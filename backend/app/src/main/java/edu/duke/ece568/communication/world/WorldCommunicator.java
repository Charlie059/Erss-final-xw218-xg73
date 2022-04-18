package edu.duke.ece568.communication.world;

import edu.duke.ece568.communication.amazon.AmazonCommunicator;
import edu.duke.ece568.utils.Logger;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * My basic idea is we have two threads, one to receive (and send ack) in while loop, one to ensure sending with asy
 * Note: WorldCommunicator should pass by a sendQueue, any object who want to send message to World should just add
 * message to the queue (like mailbox), and the WorldSendHandler(Postman) will handle this.
 */
public class WorldCommunicator {
    // WorldCommunicator should receive socket from WorldConnect class
    private Socket worldSocket;
    private volatile Queue<Long> recvQueue; // recvQueue form server
    private volatile Queue<ArrayList<Object>> sendQueue; // sendQueue to server: Object and type
    private Queue<ArrayList<Object>> resendQueue;   // Resend Queue


    // WorldCommunicator should have AmazonCommunicator
    private AmazonCommunicator amazonCommunicator;

    /**
     * Constructor of WorldCommunicator
     * @param worldSocket of world
     */
    public WorldCommunicator(Socket worldSocket, AmazonCommunicator amazonCommunicator){
        // log WorldCommunicator
        Logger.getSingleton().write("WorldCommunicator: " + worldSocket.getInetAddress().getHostAddress());

        // Create a new thread-safe recvQueue and sendQueue
        this.worldSocket = worldSocket;
        this.amazonCommunicator = amazonCommunicator;

        this.recvQueue = new ConcurrentLinkedQueue<>();
        this.sendQueue = new ConcurrentLinkedQueue<>();
        this.resendQueue = new ConcurrentLinkedQueue<>();
    }


    /**
     * Run ALL THREADS: recv, send and resend
     */
    public void runThreads(){
        // Create recv thread
        WorldRecvHandler worldRecvHandler = new WorldRecvHandler(this.worldSocket, this, this.amazonCommunicator, this.recvQueue);
        new Thread(worldRecvHandler).start();

        // Create send thread
        WorldSendHandler worldSendHandler = new WorldSendHandler(this.worldSocket, this.sendQueue, this.resendQueue);
        new Thread(worldSendHandler).start();

        // Create resend thread
        WorldResendHandler worldResendHandler = new WorldResendHandler(this.sendQueue, this.recvQueue, this.resendQueue);
        new Thread(worldResendHandler).start();
    }


    /**
     * Outer class will add msg to the sendQueue
     * @param object message to be sent eg: UGoPickup, UGoDeliver, UQuery ...
     * @param type indicates which type of object would like to send, eg: 1 for UGoPickup
     * message UCommands{
     *   repeated UGoPickup pickups = 1;
     *   repeated UGoDeliver deliveries = 2;
     *   optional uint32 simspeed = 3;
     *   optional bool disconnect = 4;
     *   repeated UQuery queries = 5;
     *   repeated int64 acks = 6;
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
}
