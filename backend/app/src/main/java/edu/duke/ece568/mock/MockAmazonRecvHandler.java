package edu.duke.ece568.mock;

import edu.duke.ece568.proto.UpsAmazon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static edu.duke.ece568.utils.GPBHelper.recvMsgFrom;
import static edu.duke.ece568.utils.GPBHelper.sendMsgTo;

public class MockAmazonRecvHandler implements Runnable{
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public MockAmazonRecvHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = this.socket.getInputStream();
        this.out = this.socket.getOutputStream();
    }

    @Override
    public void run() {
        while(true){
            // Build a new RecvResponse to recv
            UpsAmazon.AUResponse.Builder uRequest = UpsAmazon.AUResponse.newBuilder();

            recvMsgFrom(uRequest, this.in);

            System.out.println("Recv form UPS: " + uRequest);

            // Response ACK
            ArrayList<Long> responseACKList = new ArrayList<>();
            handleMsgAndSendACKs(uRequest, responseACKList);


            // Send back all ACKs to UPS
            UpsAmazon.AURequest.Builder uCommands = UpsAmazon.AURequest.newBuilder();
            uCommands.addAllAcks(responseACKList);

            // If we have ACKs to send
            if(!responseACKList.isEmpty()){
                sendMsgTo(uCommands.build(), this.out);
                System.out.println("Response ACK " + uCommands.build());
            }
        }
    }

    private void handleMsgAndSendACKs(UpsAmazon.AUResponse.Builder aRequest, ArrayList<Long> responseACKList) {
        List<UpsAmazon.UShippingResponse> shippingResponseList = aRequest.getShippingResponseList();
        List<UpsAmazon.UTruckArrivedNotification> truckArrivedNotificationList = aRequest.getArrivedList();
        List<UpsAmazon.UShipmentStatusUpdate> shipmentStatusUpdateList = aRequest.getShipmentStatusUpdateList();
        List<UpsAmazon.UPackageDetailRequest> packageDetailList = aRequest.getPackageDetailList();

        // Handle UShippingResponse
        for (UpsAmazon.UShippingResponse aPackageDetailResponse : shippingResponseList) {
            responseACKList.add(aPackageDetailResponse.getSeqnum());
        }

        for (UpsAmazon.UTruckArrivedNotification uTruckArrivedNotification : truckArrivedNotificationList) {
            responseACKList.add(uTruckArrivedNotification.getSeqnum());
        }

        for (UpsAmazon.UShipmentStatusUpdate uShipmentStatusUpdate : shipmentStatusUpdateList) {
            responseACKList.add(uShipmentStatusUpdate.getSeqnum());
        }

        for (UpsAmazon.UPackageDetailRequest uPackageDetailRequest : packageDetailList) {
            responseACKList.add(uPackageDetailRequest.getSeqnum());
        }

    }
}
