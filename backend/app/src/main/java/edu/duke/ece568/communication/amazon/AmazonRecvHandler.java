package edu.duke.ece568.communication.amazon;

import edu.duke.ece568.communication.world.WorldCommunicator;
import edu.duke.ece568.database.PostgreSQLJDBC;
import edu.duke.ece568.proto.UpsAmazon;
import edu.duke.ece568.proto.WorldUps;
import edu.duke.ece568.utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;

import static edu.duke.ece568.utils.GPBHelper.recvMsgFrom;
import static edu.duke.ece568.utils.GPBHelper.sendMsgTo;

public class AmazonRecvHandler implements Runnable{

    private Socket amazonSocket;
    private WorldCommunicator worldCommunicator;
    private AmazonCommunicator amazonCommunicator;
    private InputStream in;
    private OutputStream out;
    private HashSet<Long> handledSet; // record all seqNum which has handled before
    private WorldMsgFactory worldMsgFactory;
    private AUMsgFactory auMsgFactory;
    // Recv queue
    private volatile Queue<Long> recvQueue;

    /**
     * AmazonRecvHandler should recv message and handle any response
     * @param amazonSocket AmazonSocket
     * @param amazonCommunicator
     * @param worldCommunicator worldCommunicator can send any msg to world
     * @param recvQueue RecvQueue place seqNum
     */
    public AmazonRecvHandler(Socket amazonSocket, AmazonCommunicator amazonCommunicator, WorldCommunicator worldCommunicator, Queue<Long> recvQueue){
        this.amazonSocket = amazonSocket;
        this.amazonCommunicator = amazonCommunicator;
        this.worldCommunicator = worldCommunicator;
        this.recvQueue = recvQueue;
        this.handledSet = new HashSet<>();
        this.worldMsgFactory = new WorldMsgFactory();
        this.auMsgFactory = new AUMsgFactory();
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
            // Build a new response to recv
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
            try {
                handleMsgAndSendACKs(uResponses, responseACKList);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

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
    private void handleMsgAndSendACKs(UpsAmazon.AURequest.Builder aRequest, ArrayList<Long> responseACKList) throws SQLException {
        List<UpsAmazon.AShippingRequest> shippingRequestList = aRequest.getShippingRequestList();
        List<UpsAmazon.ATruckLoadedNotification> loadedNotificationList = aRequest.getLoadedList();
        List<UpsAmazon.AShipmentStatusUpdate> shipmentStatusUpdateList = aRequest.getShipmentStatusUpdateList();
        //List<UpsAmazon.APackageDetailResponse> packageDetailList = aRequest.getPackageDetailList();

        // Handle AShippingRequest
        handleAShippingRequest(responseACKList, shippingRequestList);

        // Handle ATruckLoadedNotification
        handleATruckLoadedNotification(responseACKList, loadedNotificationList);

        // Handle AShipmentStatusUpdate
        handleAShipmentStatusUpdate(responseACKList, shipmentStatusUpdateList);

        // Get the APackageDetailResponse
        //handleAPackageDetailResponse(responseACKList, packageDetailList);
    }

    /**
     * Handle APackageDetailResponse msg
     * @param responseACKList
     * @param packageDetailList
     */
//    private void handleAPackageDetailResponse(ArrayList<Long> responseACKList, List<UpsAmazon.APackageDetailResponse> packageDetailList) {
//        for (UpsAmazon.APackageDetailResponse aPackageDetailResponse : packageDetailList) {
//            // If that seqNum has been handled before, continue
//            if(this.handledSet.contains(aPackageDetailResponse.getSeqnum())) continue;
//
//            // Handle the message
//            // (1) Recv a hashmap<with packageID, Package>
//            // (2) TODO Update Database to store this info, we can just print them out for now
//
//            responseACKList.add(aPackageDetailResponse.getSeqnum());
//            this.handledSet.add(aPackageDetailResponse.getSeqnum());
//        }
//    }

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
            List<UpsAmazon.AUShipmentUpdate> ids = aShipmentStatusUpdate.getAuShipmentUpdateList();
            for(int i=0; i<ids.size(); i++){
                //TODO add update time and status detail
                String update_package = "UPDATE ups_package SET \"Status\" = " + ids.get(0).getStatus() + " WHERE \"PackageID\" = " + ids.get(i).getPackageId() + ";";
                Logger.getSingleton().write(update_package);
                PostgreSQLJDBC.getInstance().runSQLUpdate(update_package);
            }

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
            String update_truck = "UPDATE ups_truck SET \"Status\" = 'DELIVERING' WHERE \"TruckID\" = " + aTruckLoadedNotification.getTruckId() + ";";
            Logger.getSingleton().write(update_truck);
            PostgreSQLJDBC.getInstance().runSQLUpdate(update_truck);

            // (1.1) update packages status to DELI, updateTime() // TODO may strengthening conditions
            String update_package = "UPDATE ups_package SET \"Status\" = 'DELI', \"UpdateTime\" = '" + TimeGetter.getCurrTime() + "' WHERE \"TruckID_id\" = " + aTruckLoadedNotification.getTruckId() + ";";
            PostgreSQLJDBC.getInstance().runSQLUpdate(update_package);

            // Find Packages of the current truck TODO Same issue
            String package_query = "SELECT \"PackageID\", x, y FROM ups_package WHERE \"TruckID_id\" = " + aTruckLoadedNotification.getTruckId() + ";";
            Logger.getSingleton().write(package_query);
            ArrayList<WorldUps.UDeliveryLocation> locations = PostgreSQLJDBC.getInstance().selectPackages(package_query);

            // (2)
            //2.1 Send UGODeliver CMD to World by using WorldCommunicator
            WorldUps.UGoDeliver uGoDeliver = worldMsgFactory.generateUGoDeliver((int) aTruckLoadedNotification.getTruckId(), locations, SeqNumGenerator.getInstance().getCurrent_id());
            worldCommunicator.sendMsg(uGoDeliver, 2);

            //2.2 send package status update UShipmentStatusUpdate
            ArrayList<UpsAmazon.AUShipmentUpdate> updates = new ArrayList<>();
            for(int i=0; i<locations.size(); i++){
                updates.add(auMsgFactory.generateAUShipmentUpdate(locations.get(i).getPackageid(), "Delivering"));
            }
            UpsAmazon.UShipmentStatusUpdate uShipmentStatusUpdate = auMsgFactory.generateUShipmentStatusUpdates(updates, SeqNumGenerator.getInstance().getCurrent_id());
            this.amazonCommunicator.sendMsg(uShipmentStatusUpdate, 3);

            //send ack
            responseACKList.add(aTruckLoadedNotification.getSeqnum());
            this.handledSet.add(aTruckLoadedNotification.getSeqnum());
        }
    }


    /**
     * Handle AShippingRequest message
     * Receive from Amazon: a warehouse location(warehouse id, x and y); lists of shipments(package id, destination x,y)
     * @param responseACKList
     * @param shippingRequestList
     */
    private void handleAShippingRequest(ArrayList<Long> responseACKList, List<UpsAmazon.AShippingRequest> shippingRequestList) throws SQLException {
        for (UpsAmazon.AShippingRequest aShippingRequest : shippingRequestList) {
            Logger.getSingleton().write("Dealing with Shipping Request;");
            // If that seqNum has been handled before, continue
            if(this.handledSet.contains(aShippingRequest.getSeqnum())) continue;

            // Handle the message
            // (1) Insert the Warehouse Info in DB (If not exist in DB)
            String insert_warehouse = "INSERT INTO public.ups_awarehouse VALUES (" + aShippingRequest.getLocation().getWarehouseid() + ", "+ aShippingRequest.getLocation().getX() + ", " + aShippingRequest.getLocation().getY() + ");";
            Logger.getSingleton().write(insert_warehouse);
            PostgreSQLJDBC.getInstance().runSQLUpdate(insert_warehouse);

            // (2) Assign a new Ticket in DB with WarehouseID, and set beginProcess to true
            String assign_ticket = "INSERT INTO public.ups_ticket VALUES (Default, true, " + aShippingRequest.getLocation().getWarehouseid() +") RETURNING \"id\";";
            Logger.getSingleton().write(assign_ticket);
            int ticket_id = PostgreSQLJDBC.getInstance().assignTicket(assign_ticket);

            // (3) Find one IDLE truck (What if no IDLE truck? -> Create 1000 trucks) and get it truckID and setTruck status to TRAVELING
            String truck_query = "SELECT * FROM ups_truck WHERE \"Status\" = 'idle' AND \"Available\" = true FETCH FIRST ROW ONLY;";
            //ResultSet rs= PostgreSQLJDBC.getInstance().runSQLSelect(truck_query);//TODO database concurrency

            //TODO if no truck found, return err msg
            int truck_id = PostgreSQLJDBC.getInstance().selectIdleTruck(truck_query);
            if(truck_id==-1){
                String err = "Cannot assign truck for seq num " + aShippingRequest.getSeqnum() + "| Reason: No enough truck";
                Logger.getSingleton().write(err);
                return;
            }

            String truck_update = "UPDATE ups_truck SET \"Status\" = 'traveling' WHERE \"TruckID\" = " + truck_id + ";";
            PostgreSQLJDBC.getInstance().runSQLUpdate(truck_update);

            // (4) Based on the list of AShipment(packageID, x, y, emailAddress), create a list of UPS_Package
            // Status = PROC, CreateTime and UpdateTime, Owner_id = null, TicketId = (2)'s ticket_id ,TruckID = (3).truckID
            for(UpsAmazon.AShipment aShipment: aShippingRequest.getShipmentList()){
                String insert_package = "INSERT INTO public.ups_package (\"PackageID\", x, y, \"EmailAddress\", \"Status\", \"CreateTime\", \"UpdateTime\", \"TruckID_id\", \"TicketID_id\" ) VALUES (DEFAULT, " + aShipment.getDestX() + ", " + aShipment.getDestY() + ", '"+aShipment.getEmailaddress()+ "', 'PROC', '" + TimeGetter.getCurrTime() + "', '" + TimeGetter.getCurrTime() + "', " + truck_id + ", " + ticket_id +" );";
                //for(UpsAmazon.Product product : aShipment.)
                //String insert_package_detail = "INSERT INTO public.ups_item (\"ItemId\", \"ItemName\", \"Count\", \"PackageID_id\") VALUES (DEFAULT, )"
                PostgreSQLJDBC.getInstance().runSQLUpdate(insert_package);

            }

            // (7) TODO first Generate UShippingResponse CMD and wrap it to auResponse to Amazon
            ArrayList<UpsAmazon.UTracking> uTrackings = new ArrayList<>();
            for(UpsAmazon.UTracking uTracking : uTrackings){
                uTrackings.add(auMsgFactory.generateUTracking(uTracking.getPackageId(), TrackNumberGenerator.getInstance().getCurrent_id()));
            }
            UpsAmazon.UShippingResponse uShippingResponse = auMsgFactory.generateUShippingResponse(uTrackings, truck_id, SeqNumGenerator.getInstance().getCurrent_id());
            Logger.getSingleton().write("send to: " + uShippingResponse);
            amazonCommunicator.sendMsg(uShippingResponse, 1);

            // (5) Send UGOPickUp CMD to World by using WorldCommunicator to let the trucks arrive to warehouse
            WorldUps.UGoPickup uGoPickup = worldMsgFactory.generateUGoPickup(truck_id, aShippingRequest.getLocation().getWarehouseid(), SeqNumGenerator.getInstance().getCurrent_id());

            // (6) Send UShippingResponse Amazon with Truck_id and UTracking(package_id and String tracking_number)
            worldCommunicator.sendMsg(uGoPickup,1);
            //      Note: package_id and String tracking_number can be same


            // Add to responseACKList for response ACKs
            responseACKList.add(aShippingRequest.getSeqnum());
            // Add seqNum to handledSet
            this.handledSet.add(aShippingRequest.getSeqnum());
        }
    }
}
