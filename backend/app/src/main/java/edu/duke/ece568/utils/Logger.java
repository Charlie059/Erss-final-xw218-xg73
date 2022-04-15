package edu.duke.ece568.utils;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class Logger {
    private final java.util.logging.Logger logger;
    private static Logger instance;

    /**
     * Private Constructor of Logger
     */
    private Logger(){
        this.logger = java.util.logging.Logger.getLogger("ServerLog");
        try {
            FileHandler fh = new FileHandler("server.log");
            this.logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get instance of logger
     * @return Logger object
     */
    public static Logger getSingleton() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    /**
     * Write any log to the log file
     * @param log to
     */
    public void write(String log){
        this.logger.info(log);
    }

}
