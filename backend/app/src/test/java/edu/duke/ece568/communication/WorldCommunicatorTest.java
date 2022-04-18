package edu.duke.ece568.communication;

import edu.duke.ece568.proto.WorldUps;
import edu.duke.ece568.utils.WorldConnect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class WorldCommunicatorTest {

    @Test
    void sendMsg() throws InterruptedException {
        // Init World and Connect to World

//        final String WORLD_HOST = "207.246.90.49";
//        final int WORLD_PORT = 12345;
//
//        WorldConnect worldConnector = new WorldConnect(WORLD_HOST, WORLD_PORT);
//        worldConnector.setupConnection();
//
//        // Setup message to be sent
//        WorldUps.UGoPickup.Builder uGoPickup = WorldUps.UGoPickup.newBuilder();
//        uGoPickup.setTruckid(1);
//        uGoPickup.setWhid(1);
//        uGoPickup.setSeqnum(3);
//
//        WorldUps.UGoPickup.Builder uGoPickup2 = WorldUps.UGoPickup.newBuilder();
//        uGoPickup2.setTruckid(2);
//        uGoPickup2.setWhid(1);
//        uGoPickup2.setSeqnum(4);
//
//        WorldUps.UGoPickup.Builder uGoPickup3 = WorldUps.UGoPickup.newBuilder();
//        uGoPickup3.setTruckid(3);
//        uGoPickup3.setWhid(1);
//        uGoPickup3.setSeqnum(5);
//
//
//        WorldUps.UGoPickup.Builder uGoPickup4 = WorldUps.UGoPickup.newBuilder();
//        uGoPickup4.setTruckid(4);
//        uGoPickup4.setWhid(1);
//        uGoPickup4.setSeqnum(6);
//
//
//        WorldUps.UGoPickup.Builder uGoPickup5 = WorldUps.UGoPickup.newBuilder();
//        uGoPickup5.setTruckid(5);
//        uGoPickup5.setWhid(1);
//        uGoPickup5.setSeqnum(7);
//
//
//        WorldUps.UGoPickup.Builder uGoPickup6 = WorldUps.UGoPickup.newBuilder();
//        uGoPickup6.setTruckid(6);
//        uGoPickup6.setWhid(1);
//        uGoPickup6.setSeqnum(8);
//
//
//        WorldCommunicator worldCommunicator = new WorldCommunicator(worldConnector.getWorld_socket(), null);
//        worldCommunicator.sendMsg(uGoPickup.build(), 1);
//        worldCommunicator.sendMsg(uGoPickup2.build(), 1);
//        worldCommunicator.sendMsg(uGoPickup3.build(), 1);
//        worldCommunicator.sendMsg(uGoPickup4.build(), 1);
//        worldCommunicator.sendMsg(uGoPickup5.build(), 1);
//        worldCommunicator.sendMsg(uGoPickup6.build(), 1);
//
//        while (true){
//            Queue<ArrayList<Object>>  sendQueue =  worldCommunicator.getSendQueue();
//            Queue<Long>  recvQueue =  worldCommunicator.getRecvQueue();
//            Queue<ArrayList<Object>>  resendQueue =  worldCommunicator.getResendQueue();
//
//            System.out.println("____________________________");
//            System.out.println("\nSEND QUEUE\n");
//            for(Object item : sendQueue){
//                System.out.println(item.toString());
//            }
//
//            System.out.println("\nRECV QUEUE\n");
//            for(Object item : recvQueue){
//                System.out.println(item.toString());
//            }
//
//            System.out.println("\nRESEND QUEUE\n");
//            for(Object item : resendQueue){
//                System.out.println(item.toString());
//            }
//            System.out.println("____________________________");
//            Thread.sleep(1000);
//        }

    }


}