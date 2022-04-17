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

    public UpsAmazon.AUResponse generateAUResponse(ArrayList<Long> acks){
        UpsAmazon.AUResponse.Builder builder = UpsAmazon.AUResponse.newBuilder();
        builder.addAllAcks(acks);
        return builder.build();
    }
}
