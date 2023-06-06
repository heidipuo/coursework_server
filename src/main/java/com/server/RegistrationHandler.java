package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

import com.sun.net.httpserver.*;

import org.json.JSONObject;
import org.json.JSONException;


public class RegistrationHandler implements HttpHandler {

    private UserAuthenticator userAuthenticator = null;

    public RegistrationHandler(UserAuthenticator userAuthenticator){
        this.userAuthenticator = userAuthenticator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        
        final Headers headers = exchange.getRequestHeaders();
        
        System.out.println("Entering registration handle");

        try {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                System.out.println("Post detected");
                
                if (headers.containsKey("Content-Type")) {
                 
                    if (headers.get("Content-Type").get(0).equalsIgnoreCase("application/json")) {
                    
                        InputStream stream = exchange.getRequestBody();
                        String newUser = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("/n"));
                        stream.close();
                        
                        if (newUser == null || newUser.length() == 0) {

                            handleResponse(exchange, "No user credentials given", 412);

                        }else {

                            try {
                                JSONObject obj = new JSONObject(newUser);
                                
                                if (obj.getString("username").length() == 0 || obj.getString("password").length() == 0 || obj.getString("email").length() == 0){
                                    
                                    handleResponse(exchange, "No appropriate user credentials", 413);

                                }else{
                                    Boolean result = userAuthenticator.addUser(obj);
                                    
                                    if(result == false) {
                                        handleResponse(exchange, "User already exists ", 405);
                                    } else {
                                        handleResponse(exchange, "User registered", 200);
                                    }
                                }   
                            }catch (JSONException e){
                                handleResponse(exchange, "User JSON faulty", 413);
                                System.out.println("JSON parse error, user JSON faulty");
                            }catch (SQLException e){
                                handleResponse(exchange, "Adding to the database failed", 413);
                            }
                        }
                    
                    }else {
                        handleResponse(exchange, "Content type is not application/json", 407);
                    }
                
                } else {  
                    handleResponse(exchange, "No Content-Type available", 411);
                }
            
            } else {
                handleResponse(exchange, "Only POST is supported", 401);
            }    
         
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
            handleResponse(exchange, "Internal serer error", 500);
        }
    }

    public void handleResponse(HttpExchange exchange, String response, int code) throws IOException {
        
        byte [] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream outputstream = exchange.getResponseBody();
        outputstream.write(bytes);
        outputstream.flush();
        outputstream.close();

    }
}
  
