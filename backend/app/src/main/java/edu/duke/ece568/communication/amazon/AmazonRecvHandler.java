package edu.duke.ece568.communication.amazon;

import edu.duke.ece568.communication.world.WorldCommunicator;
import edu.duke.ece568.proto.UpsAmazon;
import edu.duke.ece568.proto.WorldUps;
import edu.duke.ece568.utils.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

import static edu.duke.ece568.utils.GPBHelper.recvMsgFrom;
import static edu.duke.ece568.utils.GPBHelper.sendMsgTo;

public class AmazonRecvHandler implements Runnable{

    private Socket amazonSocket;
    private WorldCommunicator worldCommunicator;
    private InputStream in;
    private OutputStream out;
    private HashSet<Long> handledSet; // record all seqNum which has handled before

    // Recv queue
    private volatile Queue<Long> recvQueue;

    /**
     * AmazonRecvHandler should recv message and handle any response
     * @param amazonSocket AmazonSocket
     * @param worldCommunicator worldCommunicator can send any msg to world
     * @param recvQueue RecvQueue place seqNum
     */
    public AmazonRecvHandler(Socket amazonSocket, WorldCommunicator worldCommunicator, Queue<Long> recvQueue){
        this.amazonSocket = amazonSocket;
        this.worldCommunicator = worldCommunicator;
        this.recvQueue = recvQueue;
        this.handledSet = new HashSet<>();

        // Get in and output stream
        try {
            this.out = this.amazonSocket.getOutputStream();
            this.in = this.amazonSocket.getInputStream();
        } catch (IOException e) {
            // log error
            Logger.getSingleton().write("AmazonRecvHandler: cannot get output or input stream");
        }
    }

    /**
     * This function should receive msg from world and handle msg then reply ack
     */
    @Override
    public void run() {
        while(true){
            // Build a new response
            UpsAmazon.AURequest.Builder uResponses = UpsAmazon.AURequest.newBuilder();

            // if nothing recv, it should block
            recvMsgFrom(uResponses, this.in);

            // Log message
            Logger.getSingleton().write("RECV Message from Amazon: " + uResponses);

            // (1) Record ACKs form the uResponses and add them into the recvQueue
            List<Long> acksList = uResponses.getAcksList();
            if(!acksList.isEmpty()) {
                this.recvQueue.addAll(acksList);
            }

            // (2) Recv other message and handle anything then reply ACKs to Amazon
            ArrayList<Long> responseACKList = new ArrayList<>();

            // Handle msg and send back ACKs to Amazon
            handleMsgAndSendACKs(uResponses, responseACKList);

            // Send back all ACKs to Amazon
            UpsAmazon.AUResponse.Builder uCommands = UpsAmazon.AUResponse.newBuilder();
            uCommands.addAllAcks(responseACKList);

            // If we have ACKs to send
            if(!responseACKList.isEmpty()){
                sendMsgTo(uCommands.build(), this.out);
            }

        }

    }


    /**
     * This function handle any msg and send back ACKs
     * @param aRequest AURequest from Amazon
     * @param responseACKList ACKs list need to send response
     */
    private void handleMsgAndSendACKs(UpsAmazon.AURequest.Builder aRequest, ArrayList<Long> responseACKList) {
        List<UpsAmazon.AShippingRequest> shippingRequestList = aRequest.getShippingRequestList();
        List<UpsAmazon.ATruckLoadedNotification> loadedNotificationList = aRequest.getLoadedList();
        List<UpsAmazon.AShipmentStatusUpdate> shipmentStatusUpdateList = aRequest.getShipmentStatusUpdateList();
        List<UpsAmazon.APackageDetailResponse> packageDetailList = aRequest.getPackageDetailList();

        // Handle AShippingRequest
        handleAShippingRequest(responseACKList, shippingRequestList);

        // Handle ATruckLoadedNotification
        handleATruckLoadedNotification(responseACKList, loadedNotificationList);

        // Handle AShipmentStatusUpdate
        handleAShipmentStatusUpdate(responseACKList, shipmentStatusUpdateList);

        // Get the APackageDetailResponse
        handleAPackageDetailResponse(responseACKList, packageDetailList);
    }

    /**
     * Handle APackageDetailResponse msg
     * @param responseACKList
     * @param packageDetailList
     */
    private void handleAPackageDetailResponse(ArrayList<Long> responseACKList, List<UpsAmazon.APackageDetailResponse> packageDetailList) {
        for (UpsAmazon.APackageDetailResponse aPackageDetailResponse : packageDetailList) {
            // If that seqNum has been handled before, continue
            if(this.handledSet.contains(aPackageDetailResponse.getSeqnum())) continue;

            // Handle the message
            // (1) Recv a hashmap<with packageID, Package>
            // (2) TODO Update Database to store this info, we can just print them out for now

            responseACKList.add(aPackageDetailResponse.getSeqnum());
            this.handledSet.add(aPackageDetailResponse.getSeqnum());
        }
    }

    /**
     * Handle AShipmentStatusUpdate msg
     * @param responseACKList
     * @param shipmentStatusUpdateList
     */
    private void handleAShipmentStatusUpdate(ArrayList<Long> responseACKList, List<UpsAmazon.AShipmentStatusUpdate> shipmentStatusUpdateList) {
        for (UpsAmazon.AShipmentStatusUpdate aShipmentStatusUpdate : shipmentStatusUpdateList) {
            // If that seqNum has been handled before, continue
            if(this.handledSet.contains(aShipmentStatusUpdate.getSeqnum())) continue;

            // Handle the message
            // Get the list of AUShipmentUpdate and update Package Status and UpdateTime


            responseACKList.add(aShipmentStatusUpdate.getSeqnum());
            this.handledSet.add(aShipmentStatusUpdate.getSeqnum());
        }
    }

    /**
     * Handle ATruckLoadedNotification msg
     * @param responseACKList
     * @param loadedNotificationList
     */
    private void handleATruckLoadedNotification(ArrayList<Long> responseACKList, List<UpsAmazon.ATruckLoadedNotification> loadedNotificationList) {
        for (UpsAmazon.ATruckLoadedNotification aTruckLoadedNotification : loadedNotificationList) {
            // If that seqNum has been handled before, continue
            if(this.handledSet.contains(aTruckLoadedNotification.getSeqnum())) continue;

            // Handle the message
            // (1) Update Truck status DELIVERING
            // (1.1) update packages status to DELI, updateTime
            // (2) Send UGODeliver CMD to World by using WorldCommunicator

            responseACKList.add(aTruckLoadedNotification.getSeqnum());
            this.handledSet.add(aTruckLoadedNotification.getSeqnum());
        }
    }


    /**
     * Handle AShippingRequest message
     * @param responseACKList
     * @param shippingRequestList
     */
    private void handleAShippingRequest(ArrayList<Long> responseACKList, List<UpsAmazon.AShippingRequest> shippingRequestList) {
        for (UpsAmazon.AShippingRequest aShippingRequest : shippingRequestList) {
            // If that seqNum has been handled before, continue
            if(this.handledSet.contains(aShippingRequest.getSeqnum())) continue;

            // Handle the message
            // (1) Insert the Warehouse Info in DB (If not exist in DB)
            // (2) Assign a new Ticket in DB with WarehouseID, and set beginProcess to true
            // (3) Find one IDLE truck (What if no IDLE truck? -> Create 1000 trucks) and get it truckID and setTruck status to TRAVELING
            // (4) Based on the list of AShipment(packageID, x, y, emailAddress), create a list of UPS_Package
            // Status = PROC, CreateTime and UpdateTime, Owner_id = null, TicketId = (2)'s ticket_id ,TruckID = (3).truckID
            // (5) Send UGOPickUp CMD to World by using WorldCommunicator
            // (6) Send UShippingResponse Amazon with Truck_id and UTracking(package_id and String tracking_number)
            //      Note: package_id and String tracking_number can be same



            // Add to responseACKList for response ACKs
            responseACKList.add(aShippingRequest.getSeqnum());
            // Add seqNum to handledSet
            this.handledSet.add(aShippingRequest.getSeqnum());
        }
    }
}
