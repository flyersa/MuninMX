/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.utils;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author enricokern
 */
public class SocketCheck {
    private Socket cs;
    private int socketCreated;
    private String hostname;
    
    public SocketCheck(Socket p_cs, int p_sockettime)
    {
        setSocket(p_cs);
        setSocketCreated(p_sockettime);
    }

    public void closeSocket()
    {
        try {
            cs.close();
        } catch (IOException ex) {
            //Logger.getLogger(SocketCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * @return the cs
     */
    public Socket getSocket() {
        return cs;
    }

    /**
     * @param cs the cs to set
     */
    public void setSocket(Socket cs) {
        this.cs = cs;
    }

    /**
     * @return the socketCreated
     */
    public int getSocketCreated() {
        return socketCreated;
    }

    /**
     * @param socketCreated the socketCreated to set
     */
    public void setSocketCreated(int socketCreated) {
        this.socketCreated = socketCreated;
    }

    public void setHostname(String p_hostname) {
       hostname = p_hostname;
    }
    
    public String getHostname() {
       return hostname;
    }    
   
}
