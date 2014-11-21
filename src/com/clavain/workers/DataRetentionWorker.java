/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.workers;

import com.clavain.munin.MuninNode;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoOptions;

import static com.clavain.muninmxcd.m;
import static com.clavain.muninmxcd.p;
import static com.clavain.muninmxcd.logMore;
import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.p;
import static com.clavain.utils.Generic.getUnixtime;
import static com.clavain.utils.Generic.getMuninNode;
import static com.clavain.muninmxcd.v_munin_nodes;
import static com.clavain.utils.Database.connectToDatabase;
import com.mongodb.DBCursor;
import java.util.Iterator;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author enricokern
 */
public class DataRetentionWorker implements Runnable {
    private DB db;
    private DBCollection col;
    
    @Override
    public void run() {
          int month = 2629743;
          logger.info("Started DataRetentionWorker");        
          try
          {
              int sleepTime = Integer.parseInt(p.getProperty("dataretention.period")) * 3600000;
              while(true)
              {
                   Thread.sleep(sleepTime);
                   logger.info("[DataRetentionWorker] Starting Retention Run");
                   Connection conn = connectToDatabase(p);
                   java.sql.Statement stmt = conn.createStatement();
                   ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE retention > 0 AND userrole != 'user'");
                   while(rs.next())
                   {
                       logger.info("[DataRetentionWorker - User Mode] Processing User: " + rs.getString("username"));
                       // get nodes from this user
                       Iterator it = v_munin_nodes.iterator();
                       List l_nodes = new ArrayList();
                       while (it.hasNext()) 
                       {
                        MuninNode l_mn = (MuninNode) it.next();
                        if(l_mn.getUser_id().equals(rs.getInt("id")))
                        {
                            logger.info("[DataRetentionWorker User Mode] probing " + l_mn.getHostname() + " from user: " + rs.getString("username"));
                            String colname = l_mn.getUser_id()+"_"+l_mn.getNode_id(); // recv
                            String colnamees = l_mn.getNode_id()+"_ess";  // time
                            int matchtime = rs.getInt("retention") * 2629743;
                            matchtime = getUnixtime() - matchtime;
                            BasicDBObject query = new BasicDBObject("recv", new BasicDBObject("$lt", matchtime));
                            String dbName = com.clavain.muninmxcd.p.getProperty("mongo.dbname");
                            db = m.getDB(dbName);
                            col = db.getCollection(colname);
                            DBCursor cursor = col.find(query);
                            if(cursor.count() > 0)
                            {
                                logger.info("[DataRetentionWorker User Mode] result for " + l_mn.getHostname() + " from user: " + rs.getString("username") + " affected for deletion: " + cursor.count() + " matchtime: lt " + matchtime);
                            }
                            col.remove(query);
                            
                            // now ESSENTIALS
                            query = new BasicDBObject("time", new BasicDBObject("$lt", matchtime));
                            dbName = com.clavain.muninmxcd.p.getProperty("mongo.dbessentials");
                            db = m.getDB(dbName);
                            col = db.getCollection(colnamees);
                            cursor = col.find(query);
                            if(cursor.count() > 0)
                            {
                                logger.info("[DataRetentionWorker User Mode] ESSENTIAL result for " + l_mn.getHostname() + " from user: " + rs.getString("username") + " affected for deletion: " + cursor.count() + " matchtime: lt " + matchtime);
                            }                            
                            col.remove(query);
                            
                          }
                       }
                   }
                   //conn.close();
                   
                   logger.info("[DataRetentionWorker User Mode] Finished Retention Run");
                   
                   logger.info("[DataRetentionWorker Custom Mode] Starting Retention Run");
                   stmt = conn.createStatement();
                   stmt.executeQuery("SELECT * FROM plugins_custom_interval WHERE retention > 0");
                   while(rs.next())
                   {  
                        logger.info("[DataRetentionWorker - Custom Mode] Processing Custom ID: " + rs.getString("id") + " Node: " + rs.getString("node_id") + " Plugin: " + rs.getString("pluginname")); 
                        MuninNode l_mn = getMuninNode(rs.getInt("node_id"));
                        if(l_mn != null)
                        {
                            String colname = l_mn.getUser_id()+"_"+l_mn.getNode_id(); // recv
                            int matchtime = rs.getInt("retention") * 86400;
                            matchtime = getUnixtime() - matchtime;
                            BasicDBObject query = new BasicDBObject("recv", new BasicDBObject("$lt", matchtime));
                            query.append("customId", rs.getInt("id"));
                            String dbName = com.clavain.muninmxcd.p.getProperty("mongo.dbname");
                            db = m.getDB(dbName);
                            col = db.getCollection(colname);
                            DBCursor cursor = col.find(query);
                            if(cursor.count() > 0)
                            {
                                logger.info("[DataRetentionWorker Custom Mode] Custom Interval (CID: "+rs.getInt("id")+") result for " + l_mn.getHostname() + " from user: " + rs.getString("username") + " affected for deletion: " + cursor.count() + " matchtime: lt " + matchtime);
                            }
                            col.remove(query);                            
                        }
                   }                 
                   logger.info("[DataRetentionWorker Custom Mode] Finished Retention Run");
                   conn.close();
              }
             
          } catch (Exception ex)
          {
              logger.error("Error in DataRetentionWorker: " + ex.getLocalizedMessage());
          }
    }
    
}
