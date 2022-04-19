package edu.duke.ece568.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class TimeGetter {

    public static String getCurrTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.ffffff");
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        String timeStr = sdf.format(currentTime);//get current time
        return timeStr;
    }
}
