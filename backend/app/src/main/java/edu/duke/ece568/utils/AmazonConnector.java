package edu.duke.ece568.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static edu.duke.ece568.utils.GPBHelper.recvMsgFrom;
import static edu.duke.ece568.utils.GPBHelper.sendMsgTo;

public class AmazonConnector {
    private int port;//used for connect to amazon
    private ServerSocket serverSocket;
    private InputStream in;
    private OutputStream out;
    private long worldid;
    private Socket amazon_socket;

    public AmazonConnector(int port, long worldid) throws IOException {
        this.port = port;
        this.worldid = worldid;
        serverSocket = new ServerSocket(port);
    }

    /**
     * Waits fro Amazon socket connection
     * @throws IOException
     */
    public void connectAmazon_socket() throws IOException {
        System.out.println("Waiting for connection from Amazon");
        amazon_socket = serverSocket.accept();
        this.out = amazon_socket.getOutputStream();
        this.in = amazon_socket.getInputStream();
    }

    private UpsAmazon.USendWorldID init_USendWorldID(){
        UpsAmazon.USendWorldID.Builder new_world_builder = UpsAmazon.USendWorldID.newBuilder();
        new_world_builder.setSeqnum(SeqNumGenerator.getInstance().getCurrent_id());//increment overall seqnum here
        new_world_builder.setWorldId(worldid);
        return new_world_builder.build();
    }

    /**
     * Processes the world transimission between Amazon and Ups
     * Generates USendWorldID
     */
    public long processWorldMsg(){
        UpsAmazon.USendWorldID uSendWorldID = init_USendWorldID();
        sendMsgTo(uSendWorldID, out);
        System.out.println("Sending ups connect message to Amazon");
        //recv request from Amazon(ack for send world id)
        UpsAmazon.AURequest.Builder auRequest = UpsAmazon.AURequest.newBuilder();
        recvMsgFrom(auRequest, in);
        System.out.println("Receiving ack from Amazon: " + auRequest.getAcks(0));
        return auRequest.getAcks(0);
    }


    public Socket getAmazon_socket() {
        return amazon_socket;
    }
}
