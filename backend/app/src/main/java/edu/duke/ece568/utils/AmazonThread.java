package edu.duke.ece568.utils;

import java.io.IOException;

public class AmazonThread extends Thread{
    private AmazonConnector amazonConnector;
    private long worldid;
    private int port;
    public AmazonThread(int port, long worldid){
        this.port = port;
        this.worldid = worldid;
    }

    @Override
    public void run(){
        System.out.println("Start Amazon Thread");
        //first connect to amazon
        try {
            amazonConnector = new AmazonConnector(port, worldid);
            amazonConnector.connectAmazon_socket();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
