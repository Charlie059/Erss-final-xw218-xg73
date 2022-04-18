package edu.duke.ece568.utils;

import edu.duke.ece568.database.PostgreSQLJDBC;
import edu.duke.ece568.proto.UpsAmazon;

import java.io.IOException;
import java.net.Socket;

public class AmazonThread extends Thread{
    private AmazonConnector amazonConnector;
    private long worldid;
    private int port;

    Socket socket;//used for send/recv from/to amazon
    public AmazonThread(int port, long worldid){
        this.port = port;
        this.worldid = worldid;
    }

    /**
     * Processes shipping request from Amazon
     * @param request shipping request
     */
    public void processShippingRequest(UpsAmazon.AShippingRequest request){
        //check which truck is available
        //String sql = "SELECT * FROM "
    }

    @Override
    public void run(){
        System.out.println("Start Amazon Thread");
        //first connect to amazon
        try {
            //TODO refactor
            amazonConnector = new AmazonConnector(port, worldid);
            amazonConnector.connectAmazon_socket();
            amazonConnector.processWorldMsg();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
