package edu.duke.ece568.utils;

import edu.duke.ece568.proto.UpsAmazon;

import java.util.ArrayList;

public class AUMsgFactory {

    public AUMsgFactory(){

    }

    public UpsAmazon.UTruckArrivedNotification generateUTruckArrivedNotification(long truckid, long seqnum){
        UpsAmazon.UTruckArrivedNotification.Builder uTruckArrivedNotification = UpsAmazon.UTruckArrivedNotification.newBuilder();
        uTruckArrivedNotification.setTruckId(truckid);
        uTruckArrivedNotification.setSeqnum(seqnum);
        return uTruckArrivedNotification.build();
    }

    public UpsAmazon.AUShipmentUpdate generateAUShipmentUpdate(long package_id, String status){
        UpsAmazon.AUShipmentUpdate.Builder auShipmentUpdate = UpsAmazon.AUShipmentUpdate.newBuilder();
        auShipmentUpdate.setPackageId(package_id);
        auShipmentUpdate.setStatus(status);
        return auShipmentUpdate.build();
    }


    public UpsAmazon.UShipmentStatusUpdate generateUShipmentStatusUpdate(UpsAmazon.AUShipmentUpdate auShipmentUpdate, long seqnum){
        UpsAmazon.UShipmentStatusUpdate.Builder uShipmentStatusUpdate = UpsAmazon.UShipmentStatusUpdate.newBuilder();
        uShipmentStatusUpdate.setAuShipmentUpdate(0, auShipmentUpdate);
        uShipmentStatusUpdate.setSeqnum(seqnum);
        return uShipmentStatusUpdate.build();
    }
    public UpsAmazon.UTracking generateUTracking(long packagdid, long trackingnum){
        UpsAmazon.UTracking.Builder uTracking = UpsAmazon.UTracking.newBuilder();
        uTracking.setPackageId(packagdid);
        uTracking.setTrackingNumber(trackingnum);
        return uTracking.build();
    }

    public UpsAmazon.UShippingResponse generateUShippingResponse(ArrayList<UpsAmazon.UTracking> uTracking, long truckid, long seqnum){
        UpsAmazon.UShippingResponse.Builder uShippingResponse = UpsAmazon.UShippingResponse.newBuilder();
        uShippingResponse.addAllUTracking(uTracking);
        uShippingResponse.setTruckId(truckid);
        uShippingResponse.setSeqnum(seqnum);
        return uShippingResponse.build();
    }
}
