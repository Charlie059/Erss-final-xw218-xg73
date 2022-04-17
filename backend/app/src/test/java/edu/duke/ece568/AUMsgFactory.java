package edu.duke.ece568;

import edu.duke.ece568.proto.UpsAmazon;

import java.util.ArrayList;

/**
 * Used for generate amazon messages
 */
public class AUMsgFactory {
    private static String host = "localhost";
    private int port;

    public AUMsgFactory(int port){
        this.port = port;
    }

    public UpsAmazon.AURequest generateAURequest(ArrayList<Long> acks){
        UpsAmazon.AURequest.Builder builder = UpsAmazon.AURequest.newBuilder();
        builder.addAllAcks(acks);
        return builder.build();
    }

  
}
