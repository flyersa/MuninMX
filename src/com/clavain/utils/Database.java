/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.utils;

import com.clavain.munin.MuninGraph;
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
import static com.clavain.utils.Generic.getMuninNode;
import static com.clavain.utils.Quartz.scheduleCustomIntervalJob;
import java.util.Iterator;
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
    
    public static void dbScheduleAllCustomJobs()
    {
        try 
        {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();  
            ResultSet rs = stmt.executeQuery("SELECT * FROM plugins_custom_interval");
            while(rs.next())
            {
                scheduleCustomIntervalJob(rs.getInt("id"));
            }
        } catch (Exception ex)
        {
            logger.error("Startup Schedule for Custom Jobs failed." + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
    
    public static MuninPlugin getMuninPluginForCustomJobFromDb(Integer p_id)
    {
        MuninPlugin retval = new MuninPlugin();
        try
        {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();  
            ResultSet rs = stmt.executeQuery("SELECT plugins_custom_interval.*,plugins_custom_interval.query_interval AS second_interval , nodes.*, nodes.id AS nodeid, nodes.query_interval AS node_query_interval FROM  `plugins_custom_interval` LEFT JOIN nodes ON plugins_custom_interval.node_id = nodes.id WHERE plugins_custom_interval.id = "+p_id); 
            while(rs.next())
            {
                MuninNode l_node = getMuninNode(rs.getInt("nodeid"));
                if(l_node == null)
                {
                    logger.error("getMuninPluginFromCustomInterval: cannot find MuninNode with id " + rs.getInt("nodeid") + " for custom interval: " + p_id);
                    return null;
                }
                retval.set_IntervalIsSeconds(true);
                retval.set_NodeId(l_node.getNode_id());
                retval.setTo_time(rs.getString("to_time"));
                retval.setFrom_time(rs.getString("from_time"));
                retval.setTimezone(rs.getString("timezone"));
                String str_PluginName = rs.getString("pluginname").trim();
                retval.setUser_id(l_node.getUser_id());
                retval.setQuery_interval(rs.getInt("second_interval"));
                retval.setCustomId(p_id);
                // find plugin for custom interval and copy graphs and plugin informations
                Iterator it = l_node.getPluginList().iterator();
                while(it.hasNext())
                {
                    MuninPlugin l_mp = (MuninPlugin) it.next();
                    if(l_mp.getPluginName().equals(str_PluginName))
                    {
                        retval.setPluginInfo(l_mp.getPluginInfo());
                        retval.setPluginLabel((l_mp.getPluginLabel()));
                        retval.setPluginName(l_mp.getPluginName());
                        retval.setPluginTitle(l_mp.getPluginTitle());
                        retval.setStr_PluginCategory(l_mp.getStr_PluginCategory());
                        
                        // copy graph base informations
                        Iterator git = l_mp.getGraphs().iterator();
                        while(git.hasNext())
                        {
                            MuninGraph old_mg = (MuninGraph) git.next();
                            MuninGraph new_mg = new MuninGraph();
                            new_mg.setGraphDraw(old_mg.getGraphDraw());
                            new_mg.setGraphInfo(old_mg.getGraphInfo());
                            new_mg.setGraphLabel(old_mg.getGraphLabel());
                            new_mg.setGraphName(old_mg.getGraphName());
                            new_mg.setGraphType(old_mg.getGraphType());
                            new_mg.setNegative(old_mg.isNegative());
                            new_mg.setQueryInterval(rs.getInt("second_interval"));
                            new_mg.setIntervalIsSeconds(true);
                            retval.addGraph(new_mg);
                            logger.info("getMuninPluginFromCustomInterval: added graph " + new_mg.getGraphName() + " for custom interval: " + p_id);
                        }
                    }
                }
               return retval;
            }
            
        } catch (Exception ex)
        {
            logger.error("Error in getMuninPluginFromCustomInterval: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            retval = null;
        }
        return retval;
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
