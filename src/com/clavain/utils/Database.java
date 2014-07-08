/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.utils;

import com.clavain.munin.MuninNode;
import com.clavain.munin.MuninPlugin;
import static com.clavain.muninmxcd.logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import static com.clavain.muninmxcd.p;
/**
 *
 * @author enricokern
 */
public class Database {
    
    // Establish a connection to the database
    public static Connection connectToDatabase(Properties p)
    {
        Connection conn;
        try {
            logger.debug("Connecting to MySQL");
            conn =
               DriverManager.getConnection("jdbc:mysql://"+p.getProperty("mysql.host")+":"+p.getProperty("mysql.port")+"/"+p.getProperty("mysql.db")+"?" +
                                           "user="+p.getProperty("mysql.user")+"&password="+p.getProperty("mysql.pass")+"");

            return(conn);

        } catch (Exception ex) {
            // handle any errors
            logger.fatal("Error connecting to database: " + ex.getMessage());
            return(null);
        }
    }  
    
    public static void dbUpdatePluginForNode(Integer nodeId, MuninPlugin mp)
    {
        try {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from node_plugins WHERE node_id = "+nodeId+" AND pluginname = '"+mp.getPluginName()+"'"); 
            if(rowCount(rs) < 1)
            {
                logger.info("[Node " + nodeId + "] Adding Plugin: " + mp.getPluginName() + " to database");
                stmt.executeUpdate("INSERT INTO node_plugins (node_id,pluginname,plugintitle,plugininfo,plugincategory) VALUES ("+nodeId+",'"+mp.getPluginName()+"','"+mp.getPluginTitle()+"','"+mp.getPluginInfo()+"','"+mp.getStr_PluginCategory()+"')");
            }
            conn.close();
        } catch (Exception ex)
        {
            logger.error("Error in dbUpdatePlugin: " + ex.getLocalizedMessage());
        }
    }
    
    public static void dbUpdateLastContact(Integer nodeId)
    {
        try {
         Connection conn = connectToDatabase(p);   
         java.sql.Statement stmt = conn.createStatement();
         stmt.executeUpdate("UPDATE nodes SET last_contact = NOW() WHERE id = " + nodeId);
         conn.close();
        } catch (Exception ex)
        {
            logger.error("Error in dbUpdateLastContact: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
    
    public static void dbUpdateAllPluginsForNode(MuninNode p_mn)
    {
        logger.info("[Job: " + p_mn.getHostname() + "] Updating Database");
        // update graphs in database too
        for(MuninPlugin it_pl : p_mn.getPluginList()) {
            if(it_pl.getGraphs().size() > 0)
            {
                dbUpdatePluginForNode(p_mn.getNode_id(),it_pl);
            }
        }
        // delete now missing plugins
        dbDeleteMissingPlugins(p_mn.getNode_id(),p_mn.getPluginList());
        logger.info("[Job: " + p_mn.getHostname() + "] Databaseupdate Done");
    }
    
    public static MuninNode getMuninNodeFromDatabase(Integer nodeId)
    {
        MuninNode l_mn = new MuninNode();
        try
        {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM nodes WHERE id = " + nodeId);

            while(rs.next())
            {
                l_mn.setHostname(rs.getString("hostname"));
                l_mn.setNodename(rs.getString("hostname"));
                l_mn.setNode_id(rs.getInt("id"));
                l_mn.setPort(rs.getInt("port"));
                l_mn.setUser_id(rs.getInt("user_id"));
                l_mn.setQueryInterval(rs.getInt("query_interval"));  
                l_mn.setStr_via(rs.getString("via_host"));
                l_mn.setAuthpw(rs.getString("authpw"));
            }  
            conn.close();
        } catch (Exception ex)
        {
            logger.error("getMuninNodeFromDatabase Error: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            return null;
        }
        if(l_mn.getHostname().trim().equals("unset"))
        {
            return null;
        }
        else
        {
            return l_mn;
        }
    }
    
    public static void dbDeleteMissingPlugins(Integer nodeId,CopyOnWriteArrayList<MuninPlugin> mps)
    {
        try {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from node_plugins WHERE node_id = "+nodeId); 
            boolean found = false;
            while(rs.next())
            {
                found = false;
                for(MuninPlugin mp : mps) 
                {
                    if(mp.getPluginName().equals(rs.getString("pluginname")))
                    {
                        found = true;
                    }
                }
                if(found == false)
                {
                    logger.info("[Node " + nodeId + "] Removing Plugin: " + rs.getString("pluginname") + " from Database. Not found on munin node anymore");
                    java.sql.Statement stmtt = conn.createStatement();
                    stmtt.executeUpdate("DELETE FROM node_plugins WHERE id = "+rs.getInt("id"));
                }
            }
            conn.close();
        } catch (Exception ex)
        {
             logger.error("Error in dbDeleteMissingPlugins: " + ex.getLocalizedMessage());
        }
    }
    
    private static int rowCount(ResultSet rs) throws SQLException
    {
        int rsCount = 0;
        while(rs.next())
        {
            //do your other per row stuff 
            rsCount = rsCount + 1;
        }//end while
        return rsCount;
    }    
}
