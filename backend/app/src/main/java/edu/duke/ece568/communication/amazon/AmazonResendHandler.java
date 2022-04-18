package edu.duke.ece568.communication.amazon;

import edu.duke.ece568.proto.UpsAmazon;
import edu.duke.ece568.proto.WorldUps;

import java.util.ArrayList;
import java.util.Queue;

public class AmazonResendHandler implements Runnable {
    // Send Queue
    private volatile Queue<ArrayList<Object>> sendQueue;
    // Recv Queue
    private volatile Queue<Long> recvQueue;
    // Resend Queue
    private volatile Queue<ArrayList<Object>> resendQueue;

    public AmazonResendHandler(Queue<ArrayList<Object>> sendQueue, Queue<Long> recvQueue, Queue<ArrayList<Object>> resendQueue) {
        this.sendQueue = sendQueue;
        this.recvQueue = recvQueue;
        this.resendQueue = resendQueue;
    }

    @Override
    public void run() {
        while (true) {
            // Sleep 5 s
            trySleep(5000);

            // if resendQueue is not empty, check the front item
            if (!this.resendQueue.isEmpty()) {
                // Poll the msg seqNum from resendQueue
                ArrayList<Object> msg = this.resendQueue.poll();

                // Get the message type
                Integer type = (Integer) msg.get(0);

                // Get seqNum form object
                Long seqNum = getSeqNumFromObject(msg, type);

                // Handle resend
                handleResend(msg, seqNum);
            }
        }
    }

    /**
     * Handle resend: if not find in the recvQueue, resend msg
     * else: rm all the seqNum in the recvQueue
     * @param msg
     * @param seqNum
     */
    private void handleResend(ArrayList<Object> msg, Long seqNum) {
        // Check if recvQueue not contains that ACK
        if (!this.recvQueue.contains(seqNum)) {
            // Add it to the sendQueue
            if (!this.sendQueue.contains(msg)) this.sendQueue.add(msg);
            // Add it to the resendQueue
            if (!this.resendQueue.contains(msg)) this.resendQueue.add(msg);
        } else {
            // Remove all same seqNum form the recvQueue
            boolean rmResult = true;
            while (rmResult) {
                rmResult = this.recvQueue.remove(seqNum);
            }
        }
    }

    /**
     * Get the sequence number from the object
     * @param msg message in the resendQueue
     * @param type type of message in the resendQueue
     * @return SeqNum of obj
     *
     * message AUResponse {
     *     repeated UShippingResponse shipping_response = 1;
     *     repeated UTruckArrivedNotification arrived = 2;
     *     repeated UShipmentStatusUpdate shipment_status_update = 3;
     *     repeated UPackageDetailRequest package_detail = 4;
     * 	   repeated int64 acks = 5;
     * }
     */
    private Long getSeqNumFromObject(ArrayList<Object> msg, Integer type) {
        Long seqNum;
        switch (type) {
            case 1:
                seqNum = ((UpsAmazon.UShippingResponse) msg.get(1)).getSeqnum();
                break;
            case 2:
                seqNum = ((UpsAmazon.UTruckArrivedNotification) msg.get(1)).getSeqnum();
                break;
            case 3:
                seqNum = ((UpsAmazon.UShipmentStatusUpdate) msg.get(1)).getSeqnum();
                break;
            case 4:
                seqNum = ((UpsAmazon.UPackageDetailRequest) msg.get(1)).getSeqnum();
                break;
            default:
                seqNum = -1L;
        }
        return seqNum;
    }

    /**
     * Try to sleep
     *
     * @param millis timeunits
     */
    private void trySleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
