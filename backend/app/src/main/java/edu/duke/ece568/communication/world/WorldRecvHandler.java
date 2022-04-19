package edu.duke.ece568.communication.world;

import edu.duke.ece568.communication.amazon.AmazonCommunicator;
import edu.duke.ece568.database.PostgreSQLJDBC;
import edu.duke.ece568.proto.UpsAmazon;
import edu.duke.ece568.proto.WorldUps;
import edu.duke.ece568.utils.AUMsgFactory;
import edu.duke.ece568.utils.Logger;
import edu.duke.ece568.utils.SeqNumGenerator;
import edu.duke.ece568.utils.TimeGetter;

import javax.print.DocFlavor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import static edu.duke.ece568.utils.GPBHelper.recvMsgFrom;
import static edu.duke.ece568.utils.GPBHelper.sendMsgTo;

public class WorldRecvHandler implements Runnable{

    private Socket worldSocket;
    private AmazonCommunicator amazonCommunicator;
    private InputStream in;
    private OutputStream out;
    private AUMsgFactory auMsgFactory;
    // Recv queue
    private volatile Queue<Long> recvQueue;
    private HashSet<Long> handledSet; // record all seqNum which has handled before

    /**
     * WorldRecvHandler should recv message and handle any response
     * @param worldSocket WorldSocket
     * @param recvQueue recvQueue
     */
    public WorldRecvHandler(Socket worldSocket, AmazonCommunicator amazonCommunicator, Queue<Long> recvQueue){
        this.worldSocket = worldSocket;
        this.amazonCommunicator = amazonCommunicator;
        this.recvQueue = recvQueue;
        this.handledSet = new HashSet<>();
        this.auMsgFactory = new AUMsgFactory();

        // Get in and output stream
        try {
            this.out = this.worldSocket.getOutputStream();
            this.in = this.worldSocket.getInputStream();
        } catch (IOException e) {
            // log error
            Logger.getSingleton().write("WorldRecvHandler: cannot get output or input stream");
        }

    }

    /**
     * This function should receive msg from world and print it out then reply ack
     */
    @Override
    public void run() {
        while(true){
            // Build a new response
            WorldUps.UResponses.Builder uResponses = WorldUps.UResponses.newBuilder();

            // if nothing recv, it should block
            recvMsgFrom(uResponses, this.in);

            // Log message
            Logger.getSingleton().write("RECV Message from World: " + uResponses);


            // (1) Record ACKs form the uResponses and add them into the recvQueue
            List<Long> acksList = uResponses.getAcksList();
            if(!acksList.isEmpty()) {
                this.recvQueue.addAll(acksList);
            }


            // (2) Recv other message and handle anything then reply ACKs to World
            ArrayList<Long> responseACKList = new ArrayList<>();

            // Recv and send back ACKs to world
            handleMsgAndSendACKs(uResponses, responseACKList);

            // Send back all ACKs to World
            WorldUps.UCommands.Builder uCommands = WorldUps.UCommands.newBuilder();
            uCommands.addAllAcks(responseACKList);

            // If we have ACKs to send
            if(!responseACKList.isEmpty()){
                sendMsgTo(uCommands.build(), this.out);
            }
        }
    }



    /**
     * This function handle any msg and send back ACKs
     * @param uResponses UResponses from World
     * @param responseACKList ACKs list need to send response
     */
    private void handleMsgAndSendACKs(WorldUps.UResponses.Builder uResponses, ArrayList<Long> responseACKList) {
        // Get possible ACK
        List<WorldUps.UFinished> completionsList = uResponses.getCompletionsList();
        List<WorldUps.UDeliveryMade> deliveredList = uResponses.getDeliveredList();
        List<WorldUps.UTruck> truckstatusList = uResponses.getTruckstatusList();
        List<WorldUps.UErr> errorList = uResponses.getErrorList();

        // Handle UFinished msg
        handleUFinished(responseACKList, completionsList);

        // Handle UDeliveryMade msg
        handleUDeliveryMade(responseACKList, deliveredList);

        // Handle UTruck msg
        handleUTruck(responseACKList, truckstatusList);

        // Handle UErr msg
        handleUErr(responseACKList, errorList);
    }

    /**
     * Handle UErr msg
     * @param responseACKList
     * @param errorList
     */
    private void handleUErr(ArrayList<Long> responseACKList, List<WorldUps.UErr> errorList) {
        for (WorldUps.UErr uErr : errorList) {
            // If that seqNum has been handled before, continue
            if(this.handledSet.contains(uErr.getSeqnum())) continue;

            // Handle Error
            // Log the err String
            Logger.getSingleton().write(uErr.getErr());
            responseACKList.add(uErr.getSeqnum());
            this.handledSet.add(uErr.getSeqnum());
        }
    }

    /**
     * Handle Utrack info(truck x, truck y, truck id, truck status)
     * @param responseACKList
     * @param truckstatusList
     */
    private void handleUTruck(ArrayList<Long> responseACKList, List<WorldUps.UTruck> truckstatusList) {
        for (WorldUps.UTruck uTruck : truckstatusList) {
            // If that seqNum has been handled before, continue
            if(this.handledSet.contains(uTruck.getSeqnum())) continue;

            // Handle the message
            // Maybe update Truck info
            //1. Update Truck info
            String update_truck = "UPDATE ups_truck SET x = " + uTruck.getX() + " y = " + uTruck.getY() + " Status = '" + uTruck.getStatus() + "' WHERE Truck_id = " + uTruck.getTruckid();
            Logger.getSingleton().write(update_truck);
            PostgreSQLJDBC.getInstance().runSQLUpdate(update_truck);
            responseACKList.add(uTruck.getSeqnum());
            this.handledSet.add(uTruck.getSeqnum());
        }
    }

    /**
     * Handle UDeliveryMade msg
     * @param responseACKList
     * @param deliveredList
     */
    private void handleUDeliveryMade(ArrayList<Long> responseACKList, List<WorldUps.UDeliveryMade> deliveredList) {
        for (WorldUps.UDeliveryMade uDeliveryMade : deliveredList) {
            // If that seqNum has been handled before, continue
            if(this.handledSet.contains(uDeliveryMade.getSeqnum())) continue;

            // Handle the message
            // (1) Update packageInfo: status to Delivered, UpdateTime

            String update_package = "UPDATE ups_package SET Status = 'Delivered', UpdateTime = '"  + TimeGetter.getCurrTime() + "'WHERE PackageID = " + uDeliveryMade.getPackageid() + "; ";
            Logger.getSingleton().write(update_package);
            PostgreSQLJDBC.getInstance().runSQLUpdate(update_package);

            // (2) Send a UShipmentStatusUpdate to Amazon by using AmazonCommunicator
            UpsAmazon.AUShipmentUpdate auShipmentUpdate = auMsgFactory.generateAUShipmentUpdate(uDeliveryMade.getPackageid(), "Delivered");//package id and status
            UpsAmazon.UShipmentStatusUpdate uShipmentStatusUpdate = auMsgFactory.generateUShipmentStatusUpdate(auShipmentUpdate, SeqNumGenerator.getInstance().getCurrent_id());
            amazonCommunicator.sendMsg(uShipmentStatusUpdate, 3);


            responseACKList.add(uDeliveryMade.getSeqnum());
            this.handledSet.add(uDeliveryMade.getSeqnum());
        }
    }

    /**
     * Handle UFinished Msg
     * @param responseACKList
     * @param completionsList
     */
    private void handleUFinished(ArrayList<Long> responseACKList, List<WorldUps.UFinished> completionsList) {
        for (WorldUps.UFinished uFinished : completionsList) {
            // If that seqNum has been handled before, continue
            if(this.handledSet.contains(uFinished.getSeqnum())) continue;

            // Handle the message
            // Two possible situations here:
            // 1. The truck is finished all delivery -> status:  idle
            // 2. The truck is arrived at warehouse -> status:  arrive warehouse

            // (1) if UFinished's status is idle:
            // (1.1) update Truck info: x y status
            // (2) if UFinished's status is arrive warehouse:
            // (2.1) update Truck info: x y status
            // (2.2) may update package status to Pickup and updateTime -> Done by Amazon???

            // (2.3) Send UTruckArrivedNotification(seqnum and truck id) to Amazon by using AmazonCommunicator

            //update truck location and status and write it to logger
            String update_truck = "UPDATE UPS_TRUCK SET x = " + uFinished.getX() + " , y = " + uFinished.getY() + " Status = '" + uFinished.getStatus() + "' WHERE TruckID = " + uFinished.getTruckid() + ";";
            Logger.getSingleton().write(update_truck);
            PostgreSQLJDBC.getInstance().runSQLUpdate(update_truck);

            if(uFinished.getStatus().equals("arrive warehouse")){
                //generate UTruckArrivedNotification response and send to Amazon
                UpsAmazon.UTruckArrivedNotification uTruckArrivedNotification = auMsgFactory.generateUTruckArrivedNotification(uFinished.getTruckid(), SeqNumGenerator.getInstance().getCurrent_id());
                amazonCommunicator.sendMsg(uTruckArrivedNotification, 2);

                //TODO whether need to update package, since we dont know package id
            }
            if(uFinished.getStatus().equals("idle")){
                //TODO told Amazon???
            }

            responseACKList.add(uFinished.getSeqnum());
            this.handledSet.add(uFinished.getSeqnum());
        }
    }
}
