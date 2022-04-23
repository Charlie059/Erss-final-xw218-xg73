package edu.duke.ece568.communication.world;

import edu.duke.ece568.database.PostgreSQLJDBC;
import edu.duke.ece568.proto.UpsAmazon;
import edu.duke.ece568.proto.WorldUps;
import edu.duke.ece568.utils.AUMsgFactory;
import edu.duke.ece568.utils.Logger;
import edu.duke.ece568.utils.SeqNumGenerator;
import edu.duke.ece568.utils.TimeGetter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class WorldRecvHandlerTest {

    public ArrayList<WorldUps.UDeliveryMade> generateDeliveredList(){
        ArrayList<WorldUps.UDeliveryMade> list = new ArrayList<>();
        WorldUps.UDeliveryMade.Builder deli = WorldUps.UDeliveryMade.newBuilder();
        deli.setPackageid(5);
        deli.setSeqnum(10);
        deli.setTruckid(10);

        list.add(deli.build());
        WorldUps.UDeliveryMade.Builder deli1 = WorldUps.UDeliveryMade.newBuilder();
        deli1.setPackageid(6);
        deli1.setSeqnum(11);
        deli1.setTruckid(10);
        return list;
    }

    public void mockUDeliveryMade(ArrayList<WorldUps.UDeliveryMade> deliveris){
        AUMsgFactory auMsgFactory = new AUMsgFactory();
        for(WorldUps.UDeliveryMade uDeliveryMade : deliveris){
            String update_package = "UPDATE ups_package SET \"Status\" = 'DELD', \"UpdateTime\" = '"  + TimeGetter.getCurrTime() + "' WHERE \"PackageID\" = " + uDeliveryMade.getPackageid() + "; ";
            Logger.getSingleton().write(update_package);
            PostgreSQLJDBC.getInstance().runSQLUpdate(update_package);


            UpsAmazon.AUShipmentUpdate auShipmentUpdate = auMsgFactory.generateAUShipmentUpdate(uDeliveryMade.getPackageid(), "Delivered");//package id and status
            UpsAmazon.UShipmentStatusUpdate uShipmentStatusUpdate = auMsgFactory.generateUShipmentStatusUpdate(auShipmentUpdate, SeqNumGenerator.getInstance().getCurrent_id());
            System.out.println(uShipmentStatusUpdate);
        }
    }

    @Disabled
    @Test
    public void test_delivery_made(){
        ArrayList<WorldUps.UDeliveryMade> deliveris= generateDeliveredList();
        mockUDeliveryMade(deliveris);

    }


    public WorldUps.UFinished generateUFinish(int seq, int x, int y, int truck_id, String status){
        WorldUps.UFinished.Builder fin1 = WorldUps.UFinished.newBuilder();
        fin1.setSeqnum(seq);
        fin1.setX(x);
        fin1.setY(y);
        fin1.setTruckid(truck_id);
        fin1.setStatus(status);
        return fin1.build();
    }

    public void mockUFinish(WorldUps.UFinished uFinished){
        AUMsgFactory auMsgFactory = new AUMsgFactory();
        String status = null;
        if(uFinished.getStatus().equals("arrive warehouse")){
            status = "ARRIVEWH";
        }else{
            status = "IDLE";
        }
        String update_truck = "UPDATE ups_truck SET x = " + uFinished.getX() + ", y = " + uFinished.getY() + ", \"Status\" = '" + status + "' WHERE \"TruckID\" = " + uFinished.getTruckid() + ";";
        Logger.getSingleton().write(update_truck);
        PostgreSQLJDBC.getInstance().runSQLUpdate(update_truck);

        if(uFinished.getStatus().equals("arrive warehouse")){
            //generate UTruckArrivedNotification response and send to Amazon
            UpsAmazon.UTruckArrivedNotification uTruckArrivedNotification = auMsgFactory.generateUTruckArrivedNotification(uFinished.getTruckid(), SeqNumGenerator.getInstance().getCurrent_id());

            //TODO whether need to update package, since we dont know package id
            System.out.println(uTruckArrivedNotification);
        }
        if(uFinished.getStatus().equals("idle")){
            //TODO told Amazon???
        }
    }

    @Disabled
    @Test
    public void test_ufinish(){
        WorldUps.UFinished fin1 = generateUFinish(1, 3, 4, 2, "arrive warehouse");
        WorldUps.UFinished fin2 = generateUFinish(2, 3, 4, 3, "delivered");
        mockUFinish(fin2);
    }

    public void mockQuery(WorldUps.UTruck uTruck){
        String status = uTruck.getStatus();
        String status_inDB = null;
        if(status.equals("arrive warehouse")){
            status_inDB = "ARRIVEWH";
        }
        if(status.equals("loading")){
            status_inDB = "LOADING";
        }
        if(status.equals("delivering")){
            status_inDB = "DELIVERING";
        }
        String update_truck = "UPDATE ups_truck SET x = " + uTruck.getX() + ", y = " + uTruck.getY() + ", \"Status\" = '" + status_inDB + "' WHERE \"TruckID\" = " + uTruck.getTruckid() + ";";
        Logger.getSingleton().write(update_truck);
        PostgreSQLJDBC.getInstance().runSQLUpdate(update_truck);
    }

    @Disabled
    @Test
    public void test_query(){
        WorldUps.UTruck.Builder truck1 = WorldUps.UTruck.newBuilder();
        truck1.setSeqnum(1);
        truck1.setTruckid(1);
        truck1.setX(0);
        truck1.setY(0);
        truck1.setStatus("loading");
        mockQuery(truck1.build());

    }

}