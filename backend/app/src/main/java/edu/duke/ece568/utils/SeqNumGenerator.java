package edu.duke.ece568.utils;

/**
 * Generate unique non-repeated seq number
 */
public class SeqNumGenerator {
    private static SeqNumGenerator counter_obj = null;
    private static int next_counter;
    private static int current_id;

    /**
     * Private Constructor
     */
    private SeqNumGenerator(){
        next_counter = 1;
    }

    public static SeqNumGenerator getInstance(){
        if (counter_obj == null){
            synchronized(SeqNumGenerator.class){
                if (counter_obj == null){
                    counter_obj = new SeqNumGenerator();
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
