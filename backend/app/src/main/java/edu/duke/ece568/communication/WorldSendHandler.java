package edu.duke.ece568.communication;

import edu.duke.ece568.proto.WorldUps;
import edu.duke.ece568.utils.Logger;
import edu.duke.ece568.utils.TimeLimitedCodeBlock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static edu.duke.ece568.utils.GPBHelper.sendMsgTo;

/**
 * This class handle asy communication send with World
 */
public class WorldSendHandler implements Runnable{

    private Socket socket;
    private InputStream in;
    private OutputStream out;


    // Send Queue
    private volatile Queue<ArrayList<Object>> sendQueue;
    // Recv Queue
    private volatile Queue<Long> recvQueue;
    // Resend Queue
    private volatile Queue<ArrayList<Object>> resendQueue;

    /**
     * WorldSendHandler should send message and handle if World not reply ACKs
     * @param socket World socket
     * @param sendQueue Message need to be sent
     * @param recvQueue ACKs Message received vy WorldRecvHandler
     * @param resendQueue
     */
    public WorldSendHandler(Socket socket, Queue<ArrayList<Object>> sendQueue, Queue<Long> recvQueue, Queue<ArrayList<Object>> resendQueue){
        this.socket = socket;
        this.sendQueue = sendQueue;
        this.recvQueue = recvQueue;
        this.resendQueue = resendQueue;

        // Get in and output stream
        try {
            this.out = socket.getOutputStream();
            this.in = socket.getInputStream();
        } catch (IOException e) {
            // log error
            Logger.getSingleton().write("WorldSendHandler: cannot get output or input stream");
        }

    }


    /**
     * This function should throw a new thread (WorldResendHandler)
     * This function should check sendQueue by period, if sendQueue
     * is not empty, then send front and move msg to resendQueue then sleep
     */
    @Override
    public void run() {
        // This method will try to get the front of sendQueue and wrapper it then sends out
        while (true){

            // If sendQueue is not empty
            if(!this.sendQueue.isEmpty()){

                WorldUps.UCommands.Builder uCommand = WorldUps.UCommands.newBuilder();

                // Pick the first msgWrapper and send out then add to the resendQueue
                ArrayList<Object> msgWrapper =  this.sendQueue.poll();
                assert msgWrapper != null;
                Integer type = (Integer) msgWrapper.get(0);
                Object msg = msgWrapper.get(1);

                // Based on the type, we know what the object it is
                switch (type) {
                    case 1 -> uCommand.addPickups((WorldUps.UGoPickup) msg);
                    case 2 -> uCommand.addDeliveries((WorldUps.UGoDeliver) msg);
                    case 3 -> uCommand.setSimspeed((Integer) msg);
                    case 4 -> uCommand.setDisconnect((Boolean) msg);
                    case 5 -> uCommand.addQueries((WorldUps.UQuery) msg);
                }

                OutputStream out = this.out;

                // Send uCommand to World
                try {
                    TimeLimitedCodeBlock.runWithTimeout(() -> {
                        sendMsgTo(uCommand.build(), out);
                    }, 2, TimeUnit.SECONDS);
                }
                catch (Exception ignored) {
                    System.out.println("Cannot send: retry");
                }


                // Add the msgWrapper to resendQueue
                if(!this.resendQueue.contains(msgWrapper)) {
                    this.resendQueue.add(msgWrapper);
                    System.out.println("ADD resendQueue");
                }

            }

//            // Sleep 1 s
//            trySleep(1000);

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
