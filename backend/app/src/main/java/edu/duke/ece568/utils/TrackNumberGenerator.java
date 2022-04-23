package edu.duke.ece568.utils;

public class TrackNumberGenerator {
    private static TrackNumberGenerator counter_obj = null;
    private static int next_counter;
    private static int current_id;

    /**
     * Private Constructor
     */
    private TrackNumberGenerator(){
        next_counter = 1;
    }

    public static TrackNumberGenerator getInstance(){
        if (counter_obj == null){
            synchronized(SeqNumGenerator.class){
                if (counter_obj == null){
                    counter_obj = new TrackNumberGenerator();
                }
            }
        }
        current_id = next_counter;
        next_counter++;
        return counter_obj;
    }

    public int getCurrent_id() {
        return current_id;
    }
}
