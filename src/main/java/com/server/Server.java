package com.server;


import com.sun.net.httpserver.*;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpHandler; 

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Server implements HttpHandler {
    
    int code = 200;
    String response = "";


    private Server() {
    }
    
    @Override
    public void handle(HttpExchange t) throws IOException {    
    
    System.out.println("Request handled in thread " + Thread.currentThread().getId());
    
    if (t.getRequestMethod().equalsIgnoreCase("POST")) {
      
        System.out.println("POST detected");
        handlePOSTRequest(t);
        handleResponse(t);

    } else if (t.getRequestMethod().equalsIgnoreCase("GET")) {
     
        handleGETRequest(t); 
        handleResponse(t);  

     } else {
        
        code = 401;
        response = "not supported";
        handleResponse(t);
     }
    }
    
    /**
     * Checks if the POST request contains content type and if the content type is application/json.
     * Reads request body, checks if the message is correct and converts it to JSON format.
     * Checks if there is a query and if so, executes it. Checks if the infromation in message is correct. 
     * If so, adds a new message in the database or modifies an existing message.
     * 
     * @param httpExchange
     * @throws IOException
     */
    private void handlePOSTRequest(HttpExchange httpExchange) throws IOException{

        System.out.println("Handling post request");
        JSONObject obj = null;
        
        if (httpExchange.getRequestHeaders().containsKey("Content-Type")) {
            String contentType = httpExchange.getRequestHeaders().get("Content-Type").get(0);
        
            if (contentType.equalsIgnoreCase("application/json")) {
                InputStream stream = httpExchange.getRequestBody();
                String newWarning = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("/n"));
                stream.close();
            
                if (newWarning != null && newWarning.length() > 0) {

                    try {
                        obj = new JSONObject(newWarning);
                    }catch (JSONException e){
                        System.out.println("JSON parse error");
                        response = "JSON parse error, user JSON faulty";
                        code = 413;
                    }  
                    
                    if (obj != null) {
                        if (obj.has("query")){
                           
                            try {
                                if (obj.getString("query").equals("user")) {
                                        
                                        response = handleQueryByUser(obj.getString("nickname"));
                                        code = 200;        
                            
                                }else if (obj.getString("query").equals("time")){
                                    
                                        response = handleQueryByTime(obj.getString("timestart"), obj.getString("timeend"));
                                        code = 200;        
                            
                                }else if (obj.getString("query").equals("location")){
                                
                                        response = handleQueryByLocation(obj.getDouble("uplongitude"), obj.getDouble("uplatitude"), obj.getDouble("downlongitude"), obj.getDouble("downlatitude"));
                                        code = 200;        
                                    
                                }else{
                                    response = "Query not supported";
                                    code = 413;
                                }
                            }catch (SQLException e){
                                e.printStackTrace();
                            }catch (JSONException e){
                                e.printStackTrace();
                            }

                            
                        }else{   
                            try {
                                if (obj.getString("nickname").length() == 0 || obj.getDouble("longitude") <= 0 ||   
                                    obj.getDouble("latitude") <= 0 || obj.getString("dangertype").length() == 0 || 
                                    obj.getString("sent").length() == 0){
                                            
                                    code = 413;
                                    response = "insufficient information";

                                }else if(!obj.getString("dangertype").equalsIgnoreCase("moose") && !obj.getString("dangertype").equalsIgnoreCase("reindeer") &&
                                        !obj.getString("dangertype").equalsIgnoreCase("deer") && !obj.getString("dangertype").equalsIgnoreCase("other")){ 

                                    code = 413;
                                    response = "dangertype not accepted";

                                }else{
                                    ZonedDateTime ztdSent = OffsetDateTime.parse(obj.getString("sent")).toZonedDateTime();
                                    WarningMessage newOne = new WarningMessage(obj.getString("nickname"), obj.getDouble("longitude"), 
                                        obj.getDouble("latitude"), ztdSent, obj.getString("dangertype"), httpExchange.getPrincipal().getUsername());
                                        
                                    if (obj.has("areacode")){
                                        newOne.setAreacode(obj.getString("areacode"));
                                    }      
                                    if (obj.has("phonenumber")){
                                        newOne.setPhonenumber(obj.getString("phonenumber"));
                                    }
                                    if (obj.has("updatereason")){
                                        newOne.setUpdateReason(obj.getString("updatereason"));
                                    }
                                    
                                    MessageDatabase db = MessageDatabase.getInstance();
                                        
                                    try{
                                        if (obj.has("id")) {
                                            newOne.setId(obj.getInt("id"));
                                            if(db.checkTheUsername(newOne.getId(), newOne.getUsername())) {
                                                db.modifyMessage(newOne);
                                                code = 200;
                                                response = "message modified";
                                            }else{
                                                code = 401;
                                                response = "Cannot modify another users message";
                                            }
                                            
                                        }else{
                                        db.addMessage(newOne);  
                                        code = 200;
                                        response = "warning added";  
                                        }

                                    }catch(SQLException e){
                                        e.printStackTrace();
                                    }
                                }
                            
                            }catch (JSONException e){

                                response = "Information given in wrong format";
                                code = 413;

                            }catch (DateTimeParseException e) {
                            
                                response = "Date format not correct";
                                code = 413;
                            }
                        }
                    }
                
                }else{
                    code = 407; 
                    response = "Content type is not application/json";
                }
            } else {
                code = 411;
                response = "No Content-Type available";
            }   
        }
    }

    /**
     * Gets the instance of the database. Checks if there are messages in the database. 
     * Gets the messages from the database and converts them to String in JSON array format.
     * Sets the code and response.
     * 
     * @param httpexchange 
     * @throws IOException
     */
    private void handleGETRequest(HttpExchange exchange) throws IOException {
        
        MessageDatabase db = MessageDatabase.getInstance();

        try {
            if (db.isEmptyMessage()) {
                code = 204;
                response = "-1";
            } else {
                
                ArrayList<WarningMessage> warnings = db.getMessages(); 
                JSONArray responseMessages = messagesToJSONArray(warnings);

                code = 200;
                response = responseMessages.toString();

            }
        }catch (SQLException e) {
            e.printStackTrace();
            code = 413;
            response = "Couldn't retrieve data from the database";
        }catch (JSONException e) {
            e.printStackTrace();
            code = 413;
            response = "An error while converting the data";
        }
    }

    /**
     * Sends response headers.
     * 
     * @param httpExchange
     * @throws IOException   
     */
    private void handleResponse (HttpExchange httpExchange) throws IOException {
        
        System.out.println("Handling response");
        byte [] bytes = response.getBytes("UTF-8");
        
        if (response == "-1") {
           httpExchange.sendResponseHeaders(code, -1);
        }else{
            httpExchange.sendResponseHeaders(code, bytes.length);
        }
        
        OutputStream outputStream = httpExchange.getResponseBody();
        
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();       
    }
    /**
     * Gets the instance of the database. Gets the messages by given nickname from the database and 
     * converts them to JSON array.
     * 
     * @param nickname by which the messages are searched for
     * @return response message as a String
     * @throws SQLException if database handling fails
     */
    private String handleQueryByUser (String nickname) throws SQLException {

        MessageDatabase db = MessageDatabase.getInstance();
          
        ArrayList<WarningMessage> messagesByUser = db.getMessagesByUser(nickname);  
                 
        JSONArray responseMessages = messagesToJSONArray(messagesByUser);
        
        if (responseMessages.isEmpty()){
            return "No messages found by given user";
        }else{
            return responseMessages.toString();
        }
    }
    /**
     * Gets the instance of the database. Gets the messages in the database that are within given time window and 
     * converts them to JSON array. 
     * 
     * @param startTime of wanted time window
     * @param endTime of the wanted time window
     * @return response message as a String
     * @throws SQLException if database handling fails
     */
    private String handleQueryByTime(String startTime, String endTime) throws SQLException {
        
        MessageDatabase db = MessageDatabase.getInstance();
          
        ArrayList<WarningMessage> messagesInTimeWindow = db.getMessagesTimeWindow(startTime, endTime);  
                 
        JSONArray responseMessages = messagesToJSONArray(messagesInTimeWindow);
        
        if (responseMessages.isEmpty()){
            return "No messages found within given time";
        }else{
            return responseMessages.toString();
        }
    }
    /**
     * Gets the instance of the database. Gets the messages in the database that are inside given coordinates and 
     * converts them to JSON array.
     * 
     * @param uplong value of the wanted area
     * @param uplat value of the wanted area
     * @param downlong value of the wanted area
     * @param downlat value of the wanted area
     * @return response message as a String 
     * @throws SQLException if database handling fails
     */
    private String handleQueryByLocation(double uplong, double uplat, double downlong, double downlat) throws SQLException{

        MessageDatabase db = MessageDatabase.getInstance();
          
        ArrayList<WarningMessage> messagesByLocation = db.getMessagesByLocation(uplong, uplat, downlong, downlat);  
                 
        JSONArray responseMessages = messagesToJSONArray(messagesByLocation);
        
        if (responseMessages.isEmpty()){
            return "No messages found in given location";
        }else{
            return responseMessages.toString();
        }
    }

    /**
     * Makes a JSON array of a list of WarningMessage objects.
     * 
     * @param messages needed to convert to a JSON array
     * @return response messages in JSON array form
     * @throws JSONException if converting to JSON fails
     */
    public JSONArray messagesToJSONArray (ArrayList<WarningMessage> messages) throws JSONException{

        JSONArray responseMessages = new JSONArray();
        
        for (int i = 0; i < messages.size(); i++){
            JSONObject obj = new JSONObject();
            obj.put("nickname", messages.get(i).getNickname()).
            put("longitude", messages.get(i).getLongitude()).
            put("latitude", messages.get(i).getLatitude()).
            put("sent", messages.get(i).getSent()).
            put("dangertype", messages.get(i).getDangertype()).
            put("id", messages.get(i).getId());
            
            if (null != messages.get(i).getAreacode()) {
                obj.put("areacode", messages.get(i).getAreacode());
            }
            if (null != messages.get(i).getPhonenumber()) {
                obj.put("phonenumber", messages.get(i).getPhonenumber());
            }
            if (null != messages.get(i).getUpdateReason()) {
                obj.put("updatereason", messages.get(i).getUpdateReason());
            }
            if (null !=messages.get(i).getModified()) {
                obj.put("modified", messages.get(i).getModified());
            }    
            
            responseMessages.put(obj);
        }   
        return responseMessages;
    }

   private static SSLContext serverSSLContext(String file, String password) throws Exception {

        char[] passphrase = password.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(file), passphrase);
     
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
     
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
     
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
   }

    public static void main(String[] args) throws Exception {
      
        try {
      
        HttpsServer server = HttpsServer.create(new InetSocketAddress(8001),0);
       
        UserAuthenticator userAuthenticator = new UserAuthenticator(null);
     
        HttpContext httpContext = server.createContext("/warning", new Server());
        httpContext.setAuthenticator(userAuthenticator);
        
        server.createContext("/registration", new RegistrationHandler(userAuthenticator));
        
        SSLContext sslContext = serverSSLContext(args[0], args[1]); 

        server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
            public void configure (HttpsParameters params) {
            InetSocketAddress remote = params.getClientAddress();
            SSLContext c = getSSLContext();
            SSLParameters sslparams = c.getDefaultSSLParameters();
            params.setSSLParameters(sslparams);
            }
           });

        server.setExecutor(Executors.newCachedThreadPool()); 
        server.start(); 
        } catch (Exception e) {
        e.printStackTrace();
        }
            
    }
}