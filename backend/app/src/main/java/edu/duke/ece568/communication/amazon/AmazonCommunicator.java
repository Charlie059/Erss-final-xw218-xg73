package edu.duke.ece568.communication.amazon;

import edu.duke.ece568.communication.world.WorldCommunicator;
import edu.duke.ece568.communication.world.WorldRecvHandler;
import edu.duke.ece568.communication.world.WorldResendHandler;
import edu.duke.ece568.communication.world.WorldSendHandler;
import edu.duke.ece568.utils.Logger;
import org.checkerframework.checker.units.qual.A;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AmazonCommunicator {
    // AmazonCommunicator should receive socket from AmazonConnector class
    private Socket amazonSocket;
    private volatile Queue<Long> recvQueue; // recvQueue form server
    private volatile Queue<ArrayList<Object>> sendQueue; // sendQueue to server: Object and type
    private Queue<ArrayList<Object>> resendQueue;   // Resend Queue

    // AmazonCommunicator should have WorldCommunicator
    private WorldCommunicator worldCommunicator;

    /**
     * Constructor of WorldCommunicator
     * @param worldSocket of world
     */
    public AmazonCommunicator(Socket worldSocket){
        this.amazonSocket = worldSocket;

        // Create a new thread-safe recvQueue and sendQueue
        this.recvQueue = new ConcurrentLinkedQueue<>();
        this.sendQueue = new ConcurrentLinkedQueue<>();
        this.resendQueue = new ConcurrentLinkedQueue<>();
    }

    public void runThreads(){
        // Create recv thread
        AmazonRecvHandler amazonRecvHandler = new AmazonRecvHandler(this.amazonSocket, this.worldCommunicator, this.recvQueue);
        new Thread(amazonRecvHandler).start();

        // Create send thread
        AmazonSendHandler amazonSendHandler = new AmazonSendHandler(this.amazonSocket, this.sendQueue, this.resendQueue);
        new Thread(amazonSendHandler).start();

        // Create WorldResendHandler thread
        AmazonResendHandler amazonResendHandler = new AmazonResendHandler(this.sendQueue, this.recvQueue, this.resendQueue);
        new Thread(amazonResendHandler).start();
    }


    /**
     * Outer class will add msg to the sendQueue
     * @param object message to be sent
     * @param type indicates which type of object would like to send
     *  message AUResponse {
     * repeated UShippingResponse shipping_response = 1;
     * repeated UTruckArrivedNotification arrived = 2;
     * repeated UShipmentStatusUpdate shipment_status_update = 3;
     * repeated UPackageDetailRequest package_detail = 4;
     * repeated int64 acks = 5;
     *
     * }
     */
    public synchronized void sendMsg(Object object, Integer type){
        // Wrapper type and object
        ArrayList<Object> objectArr = new ArrayList<>();
        objectArr.add(type);
        objectArr.add(object);
        this.sendQueue.add(objectArr);
    }



    //TODO FOR TEST ONLY
    public Queue<Long> getRecvQueue() {
        return recvQueue;
    }

    public Queue<ArrayList<Object>> getSendQueue() {
        return sendQueue;
    }

    public Queue<ArrayList<Object>> getResendQueue() {
        return resendQueue;
    }


    /**
     * Set the WorldCommunicator
     * @param worldCommunicator
     */
    public void setWorldCommunicator(WorldCommunicator worldCommunicator) {
        this.worldCommunicator = worldCommunicator;
    }

}
