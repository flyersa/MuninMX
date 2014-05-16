
package com.unbelievable.workers;

import com.unbelievable.munin.MuninNode;
import static com.unbelievable.muninmxcd.logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import static com.unbelievable.muninmxcd.p;
import static com.unbelievable.muninmxcd.v_munin_nodes;
import static com.unbelievable.utils.Database.connectToDatabase;
import static com.unbelievable.utils.Generic.getMuninNode;
import static com.unbelievable.utils.Quartz.scheduleJob;

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
