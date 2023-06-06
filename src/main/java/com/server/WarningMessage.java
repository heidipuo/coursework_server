package com.server;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class WarningMessage {
    
    private String nickname;
    private double longitude;
    private double latitude;
    private ZonedDateTime sent; 
    private String dangertype;
    private String areacode = null;
    private String phonenumber = null;
    private String username;
    private String updateReason = null;
    private ZonedDateTime modified = null;
    private int id = 0;

     public WarningMessage(String nickname, double longitude, double latitude, ZonedDateTime sent, String dangertype, String username) {
        this.nickname = nickname;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sent = sent;
        this.dangertype = dangertype;
        this.username = username;
    }
    
    public WarningMessage(String nickname, double longitude, double latitude, ZonedDateTime sent, String dangertype, String areacode, String phonenumber, String username, String updatereason, ZonedDateTime modified, int id) {
        this.nickname = nickname;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sent = sent;
        this.dangertype = dangertype;
        this.areacode = areacode;
        this.phonenumber = phonenumber;
        this.username = username;
        this.updateReason = updatereason;
        this.modified = modified;
        this.id = id;
    }
    
    public String getNickname (){
        return nickname;
    }

     public double getLongitude (){
        return longitude;
    }
    
    public double getLatitude (){
        return latitude;
    }

    public ZonedDateTime getSent (){
        return sent;
    }

    public String getDangertype (){
        return dangertype;
    }

    public String getAreacode () {
        return areacode;
    }

    public String getPhonenumber () {
        return phonenumber;
    }

    public String getUsername () {
        return username;
    }

    public String getUpdateReason () {
        return updateReason;
    }

    public ZonedDateTime getModified () {
        return modified;
    }

    public int getId () {
        return id;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setLongitude(double longitude) {
        this.longitude =longitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setDangertype(String dangertype) {
        this.dangertype = dangertype;
    }
    
    public void setAreacode(String areacode) {
        this.areacode = areacode;
    } 

    public void setPhonenumber (String phonenumber) {
        this.phonenumber = phonenumber;
    }
 
    public void setUsername (String username) {
        this.username = username;
    }

    public void setUpdateReason (String updateReason) {
        this.updateReason = updateReason;
    }

    public void setModified (ZonedDateTime modified) {
        this.modified = modified;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long sentAsInt() {
        return sent.toInstant().toEpochMilli();
    }
    
    public void setSent(long epoch) {
        sent = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    }

    public long modifiedAsInt() {
        return modified.toInstant().toEpochMilli(); 
    } 
}
