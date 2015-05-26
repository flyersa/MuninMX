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
package com.clavain.workers;

import com.clavain.munin.MuninNode;
import static com.clavain.muninmxcd.logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import static com.clavain.muninmxcd.p;
import static com.clavain.muninmxcd.v_munin_nodes;
import static com.clavain.utils.Database.connectToDatabase;
import static com.clavain.utils.Generic.getMuninNode;
import static com.clavain.utils.Quartz.scheduleJob;

/**
 *
 * @author enricokern
 */
public class NewNodeWatcher implements Runnable {

    @Override
    public void run() {
       logger.info("NewNodeWatcher (database) started");
       Connection conn;
       while(true)
       {
           try {
               Thread.sleep(60000);
               conn = connectToDatabase(p);
               java.sql.Statement stmt = conn.createStatement();
               ResultSet rs = stmt.executeQuery("SELECT * from nodes");
               while(rs.next())
               {
                   Integer nodeID = rs.getInt("id");
                   if(getMuninNode(nodeID) == null)
                   {
                       MuninNode mn = new MuninNode();
                       mn.setHostname(rs.getString("hostname"));
                       mn.setNodename(rs.getString("hostname"));
                       mn.setNode_id(rs.getInt("id"));
                       mn.setPort(rs.getInt("port"));
                       mn.setUser_id(rs.getInt("user_id"));
                       mn.setQueryInterval(rs.getInt("query_interval"));
                       mn.setStr_via(rs.getString("via_host"));
                       mn.setAuthpw(rs.getString("authpw"));
                       v_munin_nodes.add(mn); 
                       scheduleJob(mn);
                       logger.info("NewNodeWatcher found new node in database: " + mn.getHostname() + " added and job scheduled");
                   }
               }
               
               conn.close();
           } catch (Exception ex)
           {
               logger.error("Error in NewNodeWatcher: " + ex.getLocalizedMessage());
           }
       }
    }
    
}
