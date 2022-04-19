package edu.duke.ece568.utils;

import edu.duke.ece568.proto.WorldUps;

import java.util.ArrayList;

public class WorldMsgFactory {
    public WorldMsgFactory(){

    }

    public WorldUps.UDeliveryLocation generateUDeliveryLocation(long packageid, int x, int y){
        WorldUps.UDeliveryLocation.Builder uDeliveryLocation = WorldUps.UDeliveryLocation.newBuilder();
        uDeliveryLocation.setPackageid(packageid);
        uDeliveryLocation.setX(x);
        uDeliveryLocation.setY(y);
        return uDeliveryLocation.build();

    }
    public WorldUps.UGoDeliver generateUGoDeliver(int truckid, ArrayList<WorldUps.UDeliveryLocation> packages, long seqnum){
        WorldUps.UGoDeliver.Builder uGoDeliver = WorldUps.UGoDeliver.newBuilder();
        uGoDeliver.addAllPackages(packages);
        uGoDeliver.setSeqnum(seqnum);
        uGoDeliver.setTruckid(truckid);
        return uGoDeliver.build();
    }

    public WorldUps.UGoPickup generateUGoPickup(int truckid, int whid, long seqnum){
        WorldUps.UGoPickup.Builder uGoPickup = WorldUps.UGoPickup.newBuilder();
        uGoPickup.setSeqnum(seqnum);
        uGoPickup.setTruckid(truckid);
        uGoPickup.setWhid(whid);
        return uGoPickup.build();
    }

}
