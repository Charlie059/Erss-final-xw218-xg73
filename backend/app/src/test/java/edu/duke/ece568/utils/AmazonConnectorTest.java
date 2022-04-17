package edu.duke.ece568.utils;

import static edu.duke.ece568.utils.GPBHelper.recvMsgFrom;
import static edu.duke.ece568.utils.GPBHelper.sendMsgTo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import edu.duke.ece568.AUMsgFactory;
import edu.duke.ece568.proto.UpsAmazon;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmazonConnectorTest {
  AUMsgFactory auMsgFactory = new AUMsgFactory(11111);

  @Test
  public void test_ups_amazon_connect() throws IOException {
    ArrayList<Long> acks = new ArrayList<Long>();
    acks.add((long)0);
    //    boolean add = acks.add(new Long("0"));
    UpsAmazon.AUResponse auResponse = auMsgFactory.generateAUResponse(acks);
    int port = 10000;
    Thread th = new Thread(){
        @Override
        public void run(){
          //amazon side
          Socket socket = null;
          try {
            socket = new Socket("localhost", 10000);
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            UpsAmazon.USendWorldID.Builder new_world_builder = UpsAmazon.USendWorldID.newBuilder();
            recvMsgFrom(new_world_builder, in);
            assertEquals(1, new_world_builder.getWorldId());
            sendMsgTo(auResponse, out);
          } catch (IOException e) {
            e.printStackTrace();
          }

        }
      };

        th.start();
        //ups side
        AmazonConnector amazonConnector = new AmazonConnector(10000, 1);
        amazonConnector.connectAmazon_socket();
        assertEquals(0, amazonConnector.processWorldMsg());
      }

}
