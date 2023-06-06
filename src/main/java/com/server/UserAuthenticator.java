package com.server;

import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {

    private MessageDatabase db = MessageDatabase.getInstance();

    public UserAuthenticator(String realm) {
        super("warning");
        db = MessageDatabase.getInstance();
    }
    
    /**
     * Checks is the correct password is given with a username. 
     * 
     * @param username to be checked
     * @param password given
     * @return true if user is valid, false it is not or database handling fails
     */
    @Override
    public boolean checkCredentials(String username, String password) {
        
        boolean isValidUser;
        try{
        isValidUser = db.authenticateUser(username, password);
        }catch(SQLException e){
            e.printStackTrace();
            return false;
        }
        return isValidUser;
    }

    /**
     * Tries to set the user to the database. Checks if it succeeded.
     * 
     * @param user that needs to be added to the database
     * @return true if registering the user succees, false if it fails
     * @throws SQLException
     * @throws JSONException
     */
	public boolean addUser(JSONObject user) throws SQLException, JSONException {
        
        final boolean result = db.setUser(user);     
        
        if(!result){
            System.out.println("cannot register user");
            return false;
        }
        System.out.println(user.getString("username") + " registered");
        return true;
    }
    
}   
            

