package edu.duke.ece568.database;

import edu.duke.ece568.proto.WorldUps;
import edu.duke.ece568.utils.Logger;
import edu.duke.ece568.utils.WorldMsgFactory;

import java.sql.*;
import java.util.ArrayList;

public class PostgreSQLJDBC {
    private static PostgreSQLJDBC postgreSQLJDBC;


    /**
     * Construct of PostgreSQLJDBC which clear the tables and build the tables
     */
    private PostgreSQLJDBC(){
        // TODO clear table if exist

        // Create tables


    }

    /**
     * Get the instance of PostgreSQLJDBC
     * @return the instance
     */
    public static PostgreSQLJDBC getInstance() {
        if (postgreSQLJDBC == null) postgreSQLJDBC = new PostgreSQLJDBC();
        return postgreSQLJDBC;
    }


    /**
     * Connect Database
     */
    private static Connection connectDB() {
        Connection c;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres",
                    "postgres", "postgres");
            return c;
        } catch (SQLException | ClassNotFoundException e) {
            Logger logger = Logger.getSingleton();
            logger.write("Cannot connect database.");
            return null;
        }
    }

    /**
     * Build all tables
     */
    private void buildTables(){
        String buildSQL = "";
        this.runSQLUpdate(buildSQL);
    }

    /**
     * Run SQL statement
     * @param sql String
     * @return true for success exe
     */
    public boolean runSQLUpdate(String sql){
        Statement statement;
        try {
            Connection c = connectDB();

            //Assert connect to db
            //assert c != null;

            statement = c.createStatement();
            statement.executeUpdate(sql);
            statement.close();
            c.close();
            return true;
        } catch (SQLException e) {
            Logger logger = Logger.getSingleton();
            logger.write(e.getMessage());
            return false;
        }
    }
    public int assignTicket(String sql){
        Statement statement;
        int ticket_id=-1;
        try {
            Connection c = connectDB();
            statement = c.createStatement();
            ResultSet result = statement.executeQuery(sql);
            if(result.next()){
                ticket_id = result.getInt("id");
            }
            result.close();
            statement.close();
            c.close();

        } catch (SQLException e) {
            Logger logger = Logger.getSingleton();
            logger.write(e.getMessage());
        }
        return ticket_id;
    }

    public int initTruck(String sql){
        Statement statement;
        int ticket_id=-1;
        try {
            Connection c = connectDB();
            statement = c.createStatement();
            ResultSet result = statement.executeQuery(sql);
            if(result.next()){
                ticket_id = result.getInt("TruckID");
            }
            result.close();
            statement.close();
            c.close();

        } catch (SQLException e) {
            Logger logger = Logger.getSingleton();
            logger.write(e.getMessage());
        }
        return ticket_id;
    }

    public int selectIdleTruck(String sql){
        Statement statement;
        int truck_id=-1;
        try {
            Connection c = connectDB();

            //Assert connect to db
            //assert c != null;

            statement = c.createStatement();
            ResultSet result = statement.executeQuery(sql);
            if(result.next()){
                truck_id = result.getInt("TruckID");
            }
            result.close();
            statement.close();
            c.close();

        } catch (SQLException e) {
            Logger logger = Logger.getSingleton();
            logger.write(e.getMessage());
        }
        return truck_id;
    }
    public ResultSet runSQLSelect(String sql){
        Statement statement;
        try {
            Connection c = connectDB();

            //Assert connect to db
            //assert c != null;

            statement = c.createStatement();
            ResultSet result = statement.executeQuery(sql);
            statement.close();
            c.close();
            return result;
        } catch (SQLException e) {
            Logger logger = Logger.getSingleton();
            logger.write(e.getMessage());
            return null;
        }
    }

    /**
     * Select packages id, x and y according to truck id
     * @param sql
     * @return
     */
    public ArrayList<WorldUps.UDeliveryLocation> selectPackages(String sql){
        Statement statement;
        try {
            Connection c = connectDB();
            statement = c.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            ArrayList<Integer> packages = new ArrayList<Integer>();
            ArrayList<Integer> x = new ArrayList<Integer>();
            ArrayList<Integer> y = new ArrayList<Integer>();
            while(rs.next()){
                x.add(rs.getInt("x"));
                y.add(rs.getInt("y"));
                packages.add(rs.getInt("PackageID"));
            }
            ArrayList<WorldUps.UDeliveryLocation> locations  = new ArrayList<>();
            for(int i=0; i<packages.size(); i++){
                WorldMsgFactory worldMsgFactory = new WorldMsgFactory();
                locations.add(worldMsgFactory.generateUDeliveryLocation(packages.get(i), x.get(i), y.get(i)));
            }
            rs.close();
            statement.close();
            c.close();
            return locations;
        } catch (SQLException e) {
            Logger logger = Logger.getSingleton();
            logger.write(e.getMessage());
            return null;
        }
    }

}
