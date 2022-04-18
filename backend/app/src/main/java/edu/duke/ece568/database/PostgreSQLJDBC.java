package edu.duke.ece568.database;

import edu.duke.ece568.utils.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
            c = DriverManager.getConnection("jdbc:postgresql://database:5432/postgres",
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
    private boolean runSQLUpdate(String sql){
        Statement statement;
        try {
            Connection c = connectDB();

            //Assert connect to db
            assert c != null;

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


}
