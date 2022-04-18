package edu.duke.ece568.communication.world;

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
    private OutputStream out;

    // Send Queue
    private volatile Queue<ArrayList<Object>> sendQueue;
    // Resend Queue
    private volatile Queue<ArrayList<Object>> resendQueue;

    /**
     * WorldSendHandler should send message from the sendQueue and add it to the resendQueue
     * @param socket World socket
     * @param sendQueue Message need to be sent
     * @param resendQueue
     */
    public WorldSendHandler(Socket socket, Queue<ArrayList<Object>> sendQueue, Queue<ArrayList<Object>> resendQueue){
        this.socket = socket;
        this.sendQueue = sendQueue;
        this.resendQueue = resendQueue;

        // Get in and output stream
        try {
            this.out = this.socket.getOutputStream();
        } catch (IOException e) {
            // log error
            Logger.getSingleton().write("WorldSendHandler: cannot get output or input stream");
        }
    }


    /**
     * This function should pick front of sendQueue by period, if sendQueue
     * is not empty, then send front and move msg to resendQueue then sleep
     */
    @Override
    public void run() {
        // This method will try to get the front of sendQueue and wrapper it then sends out
        while (true){

            // If sendQueue is not empty
            if(!this.sendQueue.isEmpty()){
                OutputStream out = this.out;

                // init new response
                WorldUps.UCommands.Builder uCommand = WorldUps.UCommands.newBuilder();

                // Pick the first msgWrapper and send out then add to the resendQueue
                ArrayList<Object> msgWrapper =  this.sendQueue.poll();
                Integer type = (Integer) msgWrapper.get(0);
                Object msg = msgWrapper.get(1);


                // Based on the type, we know what the object it is
                buildResponse(uCommand, type, msg);


                // Send uCommand to World
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
        if(!this.resendQueue.contains(msgWrapper)) {
            // if type is not setSimspeed and setDisconnect, add to resendQueue
            if(type != 3 && type != 4){
                this.resendQueue.add(msgWrapper);
            }
        }
    }

    /**
     * Try to send message with 2 mins timeout
     * @param out
     * @param uCommand
     */
    private void trySend(OutputStream out, WorldUps.UCommands.Builder uCommand) {
        try {
            TimeLimitedCodeBlock.runWithTimeout(() -> {
                sendMsgTo(uCommand.build(), out);
            }, 2, TimeUnit.SECONDS);
        }
        catch (Exception ignored) {
            System.out.println("Cannot send: retry");
        }
    }

    /**
     * Build Response  with specific type
     * @param uCommand wrapper class to be sent
     * @param type Type of message
     * @param msg Obj need to wrapper
     *
     * message UCommands{
     *   repeated UGoPickup pickups = 1;
     *   repeated UGoDeliver deliveries = 2;
     *   optional uint32 simspeed = 3;
     *   optional bool disconnect = 4;
     *   repeated UQuery queries = 5;
     *   repeated int64 acks = 6;
     * }
     */
    private void buildResponse(WorldUps.UCommands.Builder uCommand, Integer type, Object msg) {
        switch (type) {
            case 1:
                uCommand.addPickups((WorldUps.UGoPickup) msg);
                break;
            case 2:
                uCommand.addDeliveries((WorldUps.UGoDeliver) msg);
                break;
            case 3:
                uCommand.setSimspeed((Integer) msg);
                break;
            case 4:
                uCommand.setDisconnect((Boolean) msg);
                break;
            case 5:
                uCommand.addQueries((WorldUps.UQuery) msg);
                break;
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
