package edu.duke.ece568.communication.world;



import edu.duke.ece568.proto.WorldUps;

import java.util.ArrayList;
import java.util.Queue;

public class WorldResendHandler implements Runnable{

    // Send Queue
    private volatile Queue<ArrayList<Object>> sendQueue;
    // Recv Queue
    private volatile Queue<Long> recvQueue;
    // Resend Queue
    private volatile Queue<ArrayList<Object>> resendQueue;


    public WorldResendHandler(Queue<ArrayList<Object>> sendQueue, Queue<Long> recvQueue, Queue<ArrayList<Object>> resendQueue) {
        this.sendQueue = sendQueue;
        this.recvQueue = recvQueue;
        this.resendQueue = resendQueue;
    }


    // The task of resendHandler is it will
    // check the front of resendQueue in period,
    // if it in the recvQueue, then rm it
    // else it is not in the recvQueue,
    // add it to the sendQueue and move it
    // to the end of resendQueue
    @Override
    public void run() {
        while (true){
            // Sleep 5 s
            trySleep(5000);

            if(!this.resendQueue.isEmpty()){
                // Poll the msg
                ArrayList<Object> msg =  this.resendQueue.poll();

                // Get the message type
                Integer type = (Integer) msg.get(0);

                // Based on the type, we know what the object it is
                Long seqNum = getSeqNumFromObject(msg, type);

                // Check if recvQueue not contains that ACK
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
        if(!this.recvQueue.contains(seqNum)){
            // Add it to the sendQueue
            if(!this.sendQueue.contains(msg))  this.sendQueue.add(msg);
            // Add it to the resendQueue
            if(!this.resendQueue.contains(msg)) this.resendQueue.add(msg);
        }
        else {
            // Remove all same seqNum form the recvQueue
            boolean rmResult = true;
            while (rmResult){
                rmResult = this.recvQueue.remove(seqNum);
            }
        }
    }

    /**
     * Get the sequence number from the object
     * @param msg message in the resendQueue
     * @param type type of message in the resendQueue
     * @return SeqNum of obj
     * message UCommands{
     *   repeated UGoPickup pickups = 1;
     *   repeated UGoDeliver deliveries = 2;
     *   optional uint32 simspeed = 3;
     *   optional bool disconnect = 4;
     *   repeated UQuery queries = 5;
     *   repeated int64 acks = 6;
     * }
     */
    private Long getSeqNumFromObject(ArrayList<Object> msg, Integer type) {
        Long seqNum;
        switch(type){
            case 1:
                seqNum = ((WorldUps.UGoPickup) msg.get(1)).getSeqnum();
                break;
            case 2:
                seqNum = ((WorldUps.UGoDeliver) msg.get(1)).getSeqnum();
                break;
            case 5:
                seqNum = ((WorldUps.UQuery) msg.get(1)).getSeqnum();
                break;
            default:
                seqNum = -1L;
        }
        return seqNum;
    }


    /**
     * Try to sleep
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
