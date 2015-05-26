/*
 * MuninMX
 * Written by Enrico Kern, kern@clavain.com
 * www.clavain.com
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
