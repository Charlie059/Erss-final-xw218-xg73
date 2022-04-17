package edu.duke.ece568.communication;



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
                // Declare the seqNum
                Long seqNum;
                // Poll the msg
                ArrayList<Object> msg =  this.resendQueue.poll();
                assert msg != null;
                Integer type = (Integer) msg.get(0);
                // Based on the type, we know what the object it is
                seqNum = switch (type) {
                    case 1 -> ((WorldUps.UGoPickup) msg.get(1)).getSeqnum();
                    case 2 -> ((WorldUps.UGoDeliver) msg.get(1)).getSeqnum();
                    case 5 -> ((WorldUps.UQuery) msg.get(1)).getSeqnum();
                    default -> -1L;
                };

                assert seqNum != -1L;

                // Check if recvQueue not contains that ACK
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
        }
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
