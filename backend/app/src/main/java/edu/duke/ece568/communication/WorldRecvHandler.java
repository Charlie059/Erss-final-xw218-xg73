package edu.duke.ece568.communication;

import edu.duke.ece568.proto.WorldUps;
import edu.duke.ece568.utils.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static edu.duke.ece568.utils.GPBHelper.recvMsgFrom;
import static edu.duke.ece568.utils.GPBHelper.sendMsgTo;

public class WorldRecvHandler implements Runnable{

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    // Recv queue
    private volatile Queue<Long> recvQueue;

    /**
     * WorldRecvHandler should recv message and
     * @param socket
     * @param recvQueue
     */
    public WorldRecvHandler(Socket socket, Queue<Long> recvQueue){
        this.socket = socket;
        this.recvQueue = recvQueue;

        // Get in and output stream
        try {
            this.out = socket.getOutputStream();
            this.in = socket.getInputStream();
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
            Logger.getSingleton().write("RECV: " + uResponses.toString());

            {
            // There are two different purposes of recv
            // (1) recv ack to the recv queue
            // (1.1) should use thread-safe queue pass from WorldCommunicator
            // (1.2) should add the acks to the recv queue

                // Get all ACKs and add them into recvQueue
                List<Long> acksList = uResponses.getAcksList();
                if(!acksList.isEmpty()) {
                    this.recvQueue.addAll(acksList);
                }
            }

            {
            // (2) recv other message and print them out (store to DB) then reply ACKs
            // (2.1) UFinished, UDeliveryMade, UTruck, UErr have seq num
            // (2.2) should get these seq num and print them
            // (2.3) should reply ACKs to these UResponse

                // Build ACKs response list
                ArrayList<Long> responseACKList = new ArrayList<>();

                // TODO: THIS IS TESTING METHOD
                // Recv and send back ACKs to world
                recvMsgAndSendACKs(uResponses, responseACKList);

                // Send back all ACKs to World
                WorldUps.UCommands.Builder uCommands = WorldUps.UCommands.newBuilder();
                uCommands.addAllAcks(responseACKList);

                // If we have ACKs to send
                if(!responseACKList.isEmpty()){
                    sendMsgTo(uCommands.build(), this.out);
                }

            }

        }
    }



    /**
     * TODO : THIS IS TESTING METHOD
     * This function simulate reading UResponses (completions, delivered, truckstatus and error) to DB and
     * collect all ACKs, and send them to World
     * @param uResponses UResponses from World
     * @param responseACKList ACKs list need to send response
     */
    private void recvMsgAndSendACKs(WorldUps.UResponses.Builder uResponses, ArrayList<Long> responseACKList) {
        // Get possible ACK
        List<WorldUps.UFinished> completionsList = uResponses.getCompletionsList();
        List<WorldUps.UDeliveryMade> deliveredList = uResponses.getDeliveredList();
        List<WorldUps.UTruck> truckstatusList = uResponses.getTruckstatusList();
        List<WorldUps.UErr> errorList = uResponses.getErrorList();

        // print out UFinished info and add seqNum to responseACKList
        for (WorldUps.UFinished uFinished : completionsList) {
            System.out.println(uFinished.toString());
            responseACKList.add(uFinished.getSeqnum());
        }

        // print out UDeliveryMade info and add seqNum to responseACKList
        for (WorldUps.UDeliveryMade uDeliveryMade : deliveredList) {
            System.out.println(uDeliveryMade.toString());
            responseACKList.add(uDeliveryMade.getSeqnum());
        }

        // print out UTruck info and add seqNum to responseACKList
        for (WorldUps.UTruck uTruck : truckstatusList) {
            System.out.println(uTruck.toString());
            responseACKList.add(uTruck.getSeqnum());
        }

        // print out UErr info and add seqNum to responseACKList
        for (WorldUps.UErr uErr : errorList) {
            System.out.println(uErr.toString());
            responseACKList.add(uErr.getSeqnum());
        }
    }
}
