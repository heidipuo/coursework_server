package com.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import org.apache.commons.codec.digest.Crypt;
import org.json.JSONObject;

public class MessageDatabase {
    
    private Connection dbConnection = null;
    private static MessageDatabase dbInstance = null;
    private String dbName = "WarningDB";
    
    
    public static synchronized MessageDatabase getInstance() {
        if (null == dbInstance) {
            dbInstance = new MessageDatabase();
        }
        return dbInstance;
    }

    private MessageDatabase() {
        try {
            open();
        } catch (SQLException e) {
            System.out.println("Log - SQLexception");
        }
    }

    public void open() throws SQLException {
        
        File dbFile = new File(dbName);
        if (dbFile.exists() && !dbFile.isDirectory()) {
           
            String database = "jdbc:sqlite:" + dbName;
            dbConnection = DriverManager.getConnection(database);
        }else{
            
            iniatializeDatabase();
        } 
    }

    private void iniatializeDatabase() throws SQLException{

        String database = "jdbc:sqlite:" + dbName;
        dbConnection = DriverManager.getConnection(database);

        if (null != dbConnection) {
            String createMessageTable = "create table messages (nickname varchar(50) NOT NULL, longitude double NOT NULL, latitude double NOT NULL, sent int NOT NULL, dangertype varchar(50) NOT NULL, areacode varchar(10), phonenumber varchar(20), username varchar(50) NOT NULL, updatereason varchar(50), modified int)";
            String createRegistrationTable = "create table registration (username varchar(50) NOT NULL PRIMARY KEY, password varchar(20) NOT NULL, email varchar(50) NOT NULL)";
            Statement createStatement = dbConnection.createStatement();
            createStatement.executeUpdate(createMessageTable);
            createStatement.executeUpdate(createRegistrationTable);
            createStatement.close();
            System.out.println("Database successfully created");
        }else{
            System.out.println("Database creation failed");
        }
    } 

    public void closeDB() throws SQLException {
        if (null != dbConnection) {
            dbConnection.close();
            System.out.println("Closing databse connection");
            dbConnection = null;
        }
    }
    
    /**
     * Checks if user exists in the database. Crypts the password.
     * Inserts user to the database with a hashed password, if user is a new user
     * 
     * @param user as a JSONObject
     * @throws SQLException if inserting to database fails
     */
    public boolean setUser(JSONObject user) throws SQLException {

        if(checkIfUserExists(user.getString("username"))){
            return false;
        }
       
        String hashedPassword = cryptPassword(user.getString("password"));

        String insertNewUser = "INSERT INTO registration VALUES ('" + user.getString("username") + "', '" + hashedPassword + "', '" + user.getString("email") + "')";
        Statement createStatement = dbConnection.createStatement();
        createStatement.executeUpdate(insertNewUser);
        createStatement.close();

        return true;
    }

    
    public String cryptPassword(String password) {
  
        String hashedPassword = Crypt.crypt(password); 
        return hashedPassword;
    }

    
    /**
     * Gets usernames from the database and checks if the given username is one of them
     * 
     * @param username to be checked
     * @return true if user exists in the database, false if it doesn't
     * @throws SQLException if getting users from the database fails
     */
    public boolean checkIfUserExists(String userName) throws SQLException {
        
        String checkUser = "select username from registration where username = '" + userName + "'";
        
        Statement queryStatement = dbConnection.createStatement();
        ResultSet rs = queryStatement.executeQuery(checkUser);

        if (rs.next()) {
            System.out.println("user exists");
            return true;
        }else {
            return false;
        }    
    }

     /**
     * Gets password for the given user from the database.
     * Checks if the the user exists, and if the given password crypted equals with the crypted 
     * password in database.
     * 
     * @param username to be checked
     * @param password to checked
     * @return true if the given password matches the password in the database, false if it doesn't or if there is no given username in the database
     * @throws SQLException if getting password from the database fails
     */
    public boolean authenticateUser(String userName, String password) throws SQLException {

        String getCredentials = "select password from registration where username = '" + userName + "'";
        
        Statement queryStatement = dbConnection.createStatement();
        ResultSet rs = queryStatement.executeQuery(getCredentials);

        if(rs.next() == false){
            System.out.println("user doesn't exist");
            return false;
        }else{
            
            String passwordInDB = rs.getString("password");
                
            String givenPasswordCrypted= Crypt.crypt(password, passwordInDB);

            if(passwordInDB.equals(givenPasswordCrypted)){
                System.out.println("password correct");
                return true;
            }else{
                System.out.println("password incorrect");
                return false;
            }
        }
    }

    /**
     * Inserts warning message values to the database.
     * 
     * @param warningmessage to be inserted to the database
     * @throws SQLException if inserting values to the database fails
     */
    public void addMessage(WarningMessage newMessage) throws SQLException {      
                 
        String ps = "INSERT INTO messages VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement createStatement = dbConnection.prepareStatement(ps);

        createStatement.setString(1, newMessage.getNickname());
        createStatement.setDouble(2, newMessage.getLongitude());
        createStatement.setDouble(3, newMessage.getLatitude());
        createStatement.setLong(4, newMessage.sentAsInt());
        createStatement.setString(5, newMessage.getDangertype());
        createStatement.setString(6, newMessage.getAreacode());
        createStatement.setString(7, newMessage.getPhonenumber());
        createStatement.setString(8, newMessage.getUsername());
        createStatement.setString(9, newMessage.getUpdateReason());
        
        if(newMessage.getModified() != null){
            createStatement.setLong(10, newMessage.modifiedAsInt());
        }else{
            createStatement.setNull(10, Types.BIGINT);
        }
        
        createStatement.executeUpdate();
        createStatement.close();
    }

    /**
     * Checks if there are any warning messages in the database.
     * 
     * @return true if there are no messages in the database, and false if there is at least one.
     * @throws SQLException if getting messages from the database fails
     */
    public boolean isEmptyMessage () throws SQLException{

        String getMessages = "select * from messages";

        Statement createQuery = dbConnection.createStatement();
        ResultSet rs = createQuery.executeQuery(getMessages);

        if (rs.next()) {    
            return false;
        }else{
            return true;
        }
    }
    /**
     * Gets message values from the database and converts it to list of WarningMessage objects
     * 
     * @return list of warning messages from the database
     * @throws SQLException if getting messages from the database fails
     */
    public ArrayList<WarningMessage> getMessages () throws SQLException {

        ArrayList<WarningMessage> messages = new ArrayList<>();

        String getMessages = "select *, rowid from messages";

        Statement createQuery = dbConnection.createStatement();
        ResultSet rs = createQuery.executeQuery(getMessages);

        while (rs.next()) {
            
            messages.add(setMessage(rs));
        }

        return messages;
    }
    
    /**
     * Gets messages with given nickname from the database and converts them to a list of WarningMessage objects
     * 
     * @param nickname to be checked
     * @return list of messages where nickname is the given nickname
     * @throws SQLException if getting messages from the database fails
     */
    public ArrayList<WarningMessage> getMessagesByUser (String nickname) throws SQLException{
        
        ArrayList<WarningMessage> messages = new ArrayList<>();
        
        String getMessages = "select *, rowid from messages where nickname = '" + nickname +"'";

        Statement createQuery = dbConnection.createStatement();
        ResultSet rs =createQuery.executeQuery(getMessages);

        while (rs.next()) {
            messages.add(setMessage(rs));
        }

        return messages;
    }

     /**
     * Gets messages from the database and checks which of them are sent within given time window.
     * Converts them to a list of WarningMessage objects.
     * 
     * @param startime of the wanted time window
     * @param endtime of the wanted time window
     * @return list of messages that are sent within given time window
     * @throws SQLException if getting messages from the database fails
     */
    public ArrayList<WarningMessage> getMessagesTimeWindow (String startTime, String endTime) throws SQLException {

        ArrayList<WarningMessage> messages = new ArrayList<>();
        long startEpoch =  OffsetDateTime.parse(startTime).toZonedDateTime().toInstant().toEpochMilli();
        long endEpoch = OffsetDateTime.parse(endTime).toZonedDateTime().toInstant().toEpochMilli();

        String getMessages = "select *, rowid from messages";

        Statement createQuery = dbConnection.createStatement();
        ResultSet rs =createQuery.executeQuery(getMessages);

        while (rs.next()) {
            if (startEpoch < rs.getLong("sent") && rs.getLong("sent") < endEpoch) {
               
                messages.add(setMessage(rs));
            }
        }
        return messages;
    }
    
    /**
     * Gets messages from the database and checks which of them are inside the given coordinates.
     * Converts them to a list of WarningMessage objects.
     * 
     * @param the up most longitude value of the wanted area
     * @param the up most latitude value of the wanted area
     * @param the down most longitude value of the wanted area
     * @param  the down most latitude value of the wanted area
     * @return list of messages that are located inside of the given coordinates
     * @throws SQLException if getting messages from the database fails
     */
    public ArrayList<WarningMessage> getMessagesByLocation (double uplong, double uplat, double downlong, double downlat) throws SQLException {
        
        ArrayList<WarningMessage> messages = new ArrayList<>();

        String getMessages = "select *, rowid from messages";

        Statement createQuery = dbConnection.createStatement();
        ResultSet rs =createQuery.executeQuery(getMessages);

        while (rs.next()) {
            if (uplong <= rs.getDouble("longitude") && rs.getDouble("longitude") <= downlong && 
                 downlat <= rs.getDouble("latitude") && rs.getDouble("latitude") <= uplat ) {
               
                messages.add(setMessage(rs));
            }
        }
        return messages;
    }

   /**
     * Converts messages to WarningMessage objects.
     * 
     * @param ResultSet of the messages gotten from the database
     * @return message as a WarningMessage object
     * @throws SQLException if getting values from ResultSet fails
     */
    public WarningMessage setMessage (ResultSet rs) throws SQLException{
            
                ZonedDateTime sentZTD = ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("sent")), ZoneOffset.UTC);
                ZonedDateTime modifiedZTD;
                
                if (rs.getLong("modified") == 0) {
                    modifiedZTD = null;
                }else{
                    modifiedZTD = ZonedDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("modified")), ZoneOffset.UTC);
                }
                
                WarningMessage message = new WarningMessage(rs.getString("nickname"), rs.getDouble("longitude"), rs.getDouble("latitude"),
                        sentZTD , rs.getString("dangertype"), rs.getString("areacode"), rs.getString("phonenumber"), rs.getString("username"), 
                        rs.getString("updatereason"), modifiedZTD, rs.getInt("rowid")); 
                
                return message;
        }
 
    /**
     * Gets username of the message with given id from the database. 
     * Checks if the username retrieved is same as the username of the user sending the modification message.
     * 
     * @param id of the message to be modified
     * @param username of the user sending the modification message
     * @return true if user sending the modificating message matches the user who created the message, false if it doesn't
     * @throws SQLException if getting values from the database fails
     */    
    public boolean checkTheUsername (int rowid, String username) throws SQLException{

        String getMessages = "select username from messages where rowid = '" + rowid + "'";

        Statement createQuery = dbConnection.createStatement();
        ResultSet rs =createQuery.executeQuery(getMessages);

        if (rs.getString("username").equals(username)) {
            return true;
        }else{
            return false;
        }
    }

     /**
     * Update the warning message in the database.
     * 
     * @param message with new values to be updated
     * @throws SQLException if updating values to the database fails
     */    
    public void modifyMessage(WarningMessage message) throws SQLException{

        String updateValues = "UPDATE messages SET nickname = ?, longitude = ?, latitude = ?, dangertype = ?, areacode = ?, phonenumber = ?, updatereason = ?, modified = ? WHERE rowid = ?";
        PreparedStatement updateStatement = dbConnection.prepareStatement(updateValues);
        
        updateStatement.setString(1, message.getNickname());
        updateStatement.setDouble(2, message.getLongitude());
        updateStatement.setDouble(3, message.getLatitude());
        updateStatement.setString(4, message.getDangertype());
        updateStatement.setString(5, message.getAreacode());
        updateStatement.setString(6, message.getPhonenumber());
        updateStatement.setString(7, message.getUpdateReason());
        long nowEpoch = ZonedDateTime.now().toInstant().toEpochMilli();
        updateStatement.setLong(8, nowEpoch);
        updateStatement.setInt(9, message.getId());
        
        updateStatement.executeUpdate();
        updateStatement.close();        

        
        Statement createQuery = dbConnection.createStatement();
        ResultSet rs =createQuery.executeQuery("select * from messages");

        while (rs.next()) {
           System.out.println(rs.getString("nickname"));
        }
    }
}
