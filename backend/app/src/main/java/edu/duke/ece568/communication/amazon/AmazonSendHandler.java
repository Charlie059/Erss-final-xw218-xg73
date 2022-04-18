package edu.duke.ece568.communication.amazon;

import edu.duke.ece568.proto.UpsAmazon;
import edu.duke.ece568.utils.Logger;
import edu.duke.ece568.utils.TimeLimitedCodeBlock;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static edu.duke.ece568.utils.GPBHelper.sendMsgTo;

public class AmazonSendHandler implements Runnable{

    private Socket socket;
    private OutputStream out;

    // Send Queue
    private volatile Queue<ArrayList<Object>> sendQueue;
    // Resend Queue
    private volatile Queue<ArrayList<Object>> resendQueue;


    /**
     * AmazonSendHandler should send message from the sendQueue and add it to the resendQueue
     * @param socket Amazon socket
     * @param sendQueue Message need to be sent
     * @param resendQueue
     */
    public AmazonSendHandler(Socket socket, Queue<ArrayList<Object>> sendQueue, Queue<ArrayList<Object>> resendQueue){
        this.socket = socket;
        this.sendQueue = sendQueue;
        this.resendQueue = resendQueue;

        // Get in and output stream
        try {
            this.out = this.socket.getOutputStream();
        } catch (IOException e) {
            // log error
            Logger.getSingleton().write("AmazonSendHandler: cannot get output or input stream");
        }
    }

    /**
     * This function should pick front of sendQueue by period, if sendQueue
     * is not empty, then send front and move msg to resendQueue then sleep
     */
    @Override
    public void run() {
        // This method will try to get the front of sendQueue and wrapper it then sends out
        while (true) {

            // If sendQueue is not empty
            if (!this.sendQueue.isEmpty()) {
                OutputStream out = this.out;

                // init new response
                UpsAmazon.AUResponse.Builder uCommand = UpsAmazon.AUResponse.newBuilder();

                // Pick the first msgWrapper and send out then add to the resendQueue
                ArrayList<Object> msgWrapper = this.sendQueue.poll();
                Integer type = (Integer) msgWrapper.get(0);
                Object msg = msgWrapper.get(1);

                // Build response
                buildResponse(uCommand, type, msg);

                // Try to send message
                trySend(out, uCommand);

                // Add the msgWrapper to resendQueue
                add2ResendQueue(msgWrapper, type);

            }
        }
    }

    /**
     * Add the msg to the resendQueue
     * @param msgWrapper msg -> type and object
     * @param type type of message
     */
    private void add2ResendQueue(ArrayList<Object> msgWrapper, Integer type) {
        if (!this.resendQueue.contains(msgWrapper)) {
            // add all types to the resendQueue
            this.resendQueue.add(msgWrapper);
        }
    }

    /**
     * Try to send message with 2 mins timeout
     * @param out
     * @param uCommand
     */
    private void trySend(OutputStream out, UpsAmazon.AUResponse.Builder uCommand) {
        // Send AUResponse to Amazon
        try {
            TimeLimitedCodeBlock.runWithTimeout(() -> {
                sendMsgTo(uCommand.build(), out);
            }, 2, TimeUnit.SECONDS);
            Logger.getSingleton().write("Send Message: \n" + uCommand.build());
        } catch (Exception ignored) {
            System.out.println("Cannot send: retry");
        }
    }

    /**
     * Build Response  with specific type
     * @param uCommand wrapper class
     * @param type Type of message
     * @param msg Obj need to wrapper
     *
     *  message AUResponse {
     *     repeated UShippingResponse shipping_response = 1;
     *     repeated UTruckArrivedNotification arrived = 2;
     *     repeated UShipmentStatusUpdate shipment_status_update = 3;
     *     repeated UPackageDetailRequest package_detail = 4;
     * 	   repeated int64 acks = 5;
     * }
     *
     */
    private void buildResponse(UpsAmazon.AUResponse.Builder uCommand, Integer type, Object msg) {
        switch (type) {
            case 1:
                uCommand.addShippingResponse((UpsAmazon.UShippingResponse) msg);
                break;
            case 2:
                uCommand.addArrived((UpsAmazon.UTruckArrivedNotification) msg);
                break;
            case 3:
                uCommand.addShipmentStatusUpdate((UpsAmazon.UShipmentStatusUpdate) msg);
                break;
            case 4:
                uCommand.addPackageDetail((UpsAmazon.UPackageDetailRequest) msg);
                break;
        }
    }
}
