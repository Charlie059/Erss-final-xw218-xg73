package edu.duke.ece568.utils;


import edu.duke.ece568.proto.WorldUps;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.stream.IntStream;

import static edu.duke.ece568.utils.GPBHelper.recvMsgFrom;
import static edu.duke.ece568.utils.GPBHelper.sendMsgTo;


/**
 * The class used for connect to world
 */
public class WorldConnect {
    private final String world_host;
    private final int world_port;

    private Socket world_socket;
    private final ArrayList<WorldUps.UInitTruck> trucks;
    private InputStream in;
    private OutputStream out;
    long worldid;

    /**
     * Constructor of WorldConnect
     *
     * @param HOST IP add result
     * @param PORT 23456
     */
    public WorldConnect(String HOST, int PORT) {

        //assign host and port number
        this.world_host = HOST;
        this.world_port = PORT;
        this.trucks = new ArrayList<>();

        //socket set up
        connectToWorld_socket();

        try {
            this.out = world_socket.getOutputStream();
            this.in = world_socket.getInputStream();
        } catch (IOException e) {
            System.err.println("Error in world connector construction");
        }
    }
    public long getWorldid(){
        return this.worldid;
    }
    /**
     * Sets up the connection to the world: init trucks and send uconnect to world
     */
    public void setupConnection() {
        //TODO save truck info into database

        // Init 10 trucks with x = 0 and y = 0
        init_truck(10);

        // Init uConnect message
        WorldUps.UConnect uConnect = init_world_connect_info(this.trucks, true, 1);

        // Send uConnect to world and receive UConnected response
        WorldUps.UConnected uConnected = uconnect_world(uConnect);

        if (uConnected == null) {
            System.out.println("Error to connect world");
        } else {
            System.out.println("The result of uconnect is: " + uConnected.getResult() + " world id is: " + uConnected.getWorldid());
        }

    }

    /**
     * Get world socket
     *
     * @return world socket
     */
    public Socket getWorld_socket() {
        return this.world_socket;
    }

    /**
     * Connect to world via socket
     */
    public void connectToWorld_socket() {
        System.out.println("Connect to host: " + world_host);
        try {
            world_socket = new Socket(world_host, world_port);
            System.out.println("Successfully connect to world in socket " + world_socket.getPort());
        } catch (IOException e) {
            System.err.println("Error in connecting to world");
        }
    }

    /**
     * Init uconnect message
     *
     * @param trucks   trucks in UConnect
     * @param ifCreate indicate whether it needs to create a new world or just connect to existing world
     * @return the built UConnect message
     */
    public WorldUps.UConnect init_world_connect_info(ArrayList<WorldUps.UInitTruck> trucks, boolean ifCreate, long worldid) {
        WorldUps.UConnect.Builder uconnect = WorldUps.UConnect.newBuilder();
        uconnect.setIsAmazon(false);
        //add trucks
        uconnect.addAllTrucks(trucks);
        if (ifCreate) {
            //TODO if need to create a new world, don't need to specify the world id
        } else {
            //if just connect to an existing world, using an existing world id
            uconnect.setWorldid(worldid);
        }
        return uconnect.build();
    }


    /**
     * Sends uconnect to world and receive uconnected from world
     *
     * @param uconnect msg
     * @return server response or null for fail
     */
    public WorldUps.UConnected uconnect_world(WorldUps.UConnect uconnect) {
        // Send connect request to world
        System.out.println("Sending uconnect info to world: " + uconnect);
        sendMsgTo(uconnect, this.out);

        // Setup response and receive response from world
        WorldUps.UConnected.Builder response = WorldUps.UConnected.newBuilder();
        recvMsgFrom(response, this.in);

        // Check response success or not
        if (response.getResult().equals("connected!")) {
            worldid = response.getWorldid();
            return response.build();
        }
        else return null;
    }


    /**
     * Init num of truck with x = 0, y = 0
     *
     * @param nums of trucks
     */
    public void init_truck(int nums) {
        //TODO need to store in database?

        // Add truck to the truck list
        IntStream.range(0, nums).forEach(i -> {
            WorldUps.UInitTruck.Builder truck = WorldUps.UInitTruck.newBuilder();
            truck.setId(i + 1).setX(0).setY(0);
            this.trucks.add(truck.build());
        });
    }

}