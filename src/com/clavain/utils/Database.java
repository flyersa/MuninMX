/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.utils;

import com.clavain.alerts.Alert;
import com.clavain.json.User;
import com.clavain.munin.MuninGraph;
import com.clavain.munin.MuninNode;
import com.clavain.munin.MuninPlugin;
import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.m;
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
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author enricokern
 */
public class Database {

    public static String clearStringForSQL(String p_str)
    {
        if(p_str == null)
        {
            return p_str;
        }
        String retval = p_str;
        retval = retval.replaceAll("'","");  
        retval = retval.replaceAll("<",""); 
        retval = retval.replaceAll("`", "");
        retval = retval.replaceAll("´", "");
        retval = retval.replaceAll(";", "");
        return retval;
    }    
    
    // Establish a connection to the database
    public static Connection connectToDatabase(Properties p)
    {
        Connection conn;
        try {
            logger.debug("Connecting to MySQL");
            conn =
               DriverManager.getConnection("jdbc:mysql://"+p.getProperty("mysql.host")+":"+p.getProperty("mysql.port")+"/"+p.getProperty("mysql.db")+"?" +
                                           "user="+p.getProperty("mysql.user")+"&password="+p.getProperty("mysql.pass")+"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10");

            return(conn);

        } catch (Exception ex) {
            // handle any errors
            logger.fatal("Error connecting to database: " + ex.getMessage());
            return(null);
        }
    }  

  
    public static void dbSetRcaFinished(String p_rcaId)
    {
        try {
         Connection conn = connectToDatabase(p);   
         java.sql.Statement stmt = conn.createStatement();
         stmt.executeUpdate("UPDATE rca SET is_finished = 1 WHERE rcaId = '" + p_rcaId+"'");
         conn.close();
        } catch (Exception ex)
        {
            logger.error("[RCA] Error in dbSetRcaFinished: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }        
    }
    
    public static void dbUpdatePluginForNode(Integer nodeId, MuninPlugin mp)
    {
        try {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from node_plugins WHERE node_id = "+nodeId+" AND pluginname = '"+clearStringForSQL(mp.getPluginName())+"'"); 
            if(rowCount(rs) < 1)
            {
                logger.info("[Node " + nodeId + "] Adding Plugin: " + mp.getPluginName() + " to database");
                stmt.executeUpdate("INSERT INTO node_plugins (node_id,pluginname,plugintitle,plugininfo,plugincategory) VALUES ("+nodeId+",'"+clearStringForSQL(mp.getPluginName())+"','"+clearStringForSQL(mp.getPluginTitle())+"','"+clearStringForSQL(mp.getPluginInfo())+"','"+clearStringForSQL(mp.getStr_PluginCategory())+"')");
            }
            conn.close();
        } catch (Exception ex)
        {
            logger.error("Error in dbUpdatePlugin: " + ex.getLocalizedMessage());
            ex.printStackTrace();
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
        if(p_mn.getPluginList().size() > 0)
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
        else
        {
            logger.warn("[Job: " + p_mn.getHostname() + "] Databaseupdate skipped. Pluginsize is 0");
        }
    }
    
    public static User getUserFromDatabase(Integer user_id)
    {
        User luser = null;
        try
        {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE id = " + user_id);
            while(rs.next())
            {   
                luser = new User();
                luser.setAccessgroup(rs.getString("accessgroup"));
                luser.setUsername(rs.getString("username"));
                luser.setUserrole(rs.getString("userrole"));
                luser.setUser_id(rs.getInt("id"));
            }
        } catch (Exception ex)
        {
            return null;
        }
        
        return luser;
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
                l_mn.setGroup(rs.getString("groupname"));
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
    
    public static void dbAddAllAlerts()
    {
        try 
        {
            Connection conn = connectToDatabase(p);
            conn.setReadOnly(true);
            //
            java.sql.Statement stmt = conn.createStatement();  
            ResultSet rs = stmt.executeQuery("SELECT alerts.*,nodes.hostname FROM alerts LEFT JOIN nodes ON alerts.node_id = nodes.id");
            while(rs.next())
            {
                Alert av = new Alert();
                av.setAlert_id(rs.getInt("id"));
                av.setCondition(rs.getString("condition"));
                av.setGraphName(rs.getString("graphname"));
                av.setPluginName(rs.getString("pluginname"));
                av.setRaise_value(rs.getBigDecimal("raise_value"));
                av.setNum_samples(rs.getInt("num_samples"));
                av.setAlert_limit(rs.getInt("alert_limit"));
                av.setHostname(rs.getString("hostname"));
                av.setNode_id(rs.getInt("node_id"));
                com.clavain.muninmxcd.v_alerts.add(av);
            }
            logger.info("Startup for Alerts Done");
        } catch (Exception ex)
        {
            logger.error("Startup for Alerts failed. retrying in 60 seconds" + ex.getLocalizedMessage());
            try {
                Thread.sleep(60000);
                dbAddAllAlerts();
            } catch (InterruptedException ex1) {
                logger.error("Startup for Alerts restart failed");
            }
            ex.printStackTrace();
        }     
    }
    
    public static boolean dbAddAllAlertWithId(Integer p_aid)
    {
        boolean retval = false;
        try 
        {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();  
            ResultSet rs = stmt.executeQuery("SELECT alerts.*,nodes.hostname FROM alerts LEFT JOIN nodes ON alerts.node_id = nodes.id WHERE alerts.id = " + p_aid);
            while(rs.next())
            {
                Alert av = new Alert();
                av.setAlert_id(rs.getInt("id"));
                av.setCondition(rs.getString("condition"));
                av.setGraphName(rs.getString("graphname"));
                av.setPluginName(rs.getString("pluginname"));
                av.setRaise_value(rs.getBigDecimal("raise_value"));
                av.setNum_samples(rs.getInt("num_samples"));
                av.setAlert_limit(rs.getInt("alert_limit"));
                av.setHostname(rs.getString("hostname"));
                av.setNode_id(rs.getInt("node_id"));
                com.clavain.muninmxcd.v_alerts.add(av);
                retval = true;
            }
        } catch (Exception ex)
        {
            logger.error("Add Alert "+p_aid+" failed." + ex.getLocalizedMessage());
            ex.printStackTrace();
        }   
        return retval;
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
                retval.setTo_time(rs.getInt("to_time"));
                retval.setFrom_time(rs.getInt("from_time"));
                retval.setTimezone(rs.getString("timezone"));
                String str_PluginName = rs.getString("pluginname").trim();
                retval.setUser_id(l_node.getUser_id());
                retval.setQuery_interval(rs.getInt("second_interval"));
                retval.setCrontab(rs.getString("crontab"));
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
    
    
    public static void dbUpdateNodeDistVerKernel(String p_sum,String p_dist, String p_ver, String p_kernel, int p_nodeid)
    {
        try
        {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();  
            stmt.executeUpdate("UPDATE nodes SET trackpkg_sum = '"+clearStringForSQL(p_sum)+"', track_dist = '"+clearStringForSQL(p_dist)+"', track_ver = '"+clearStringForSQL(p_ver)+"', track_kernel = '"+clearStringForSQL(p_kernel)+"', track_update = NOW() WHERE id = " + p_nodeid);
            
        } catch (Exception ex)
        {
            logger.error("Error in dbUpdateNodeDistVerKernel (Node: "+p_nodeid+") - " + ex.getLocalizedMessage());
        }        
    }
    
    public static boolean dbTrackLogChangedForNode(String p_sum, int p_nodeid)
    {
        boolean retval = false;
        try
        {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();  
            ResultSet rs = stmt.executeQuery("SELECT id FROM nodes WHERE id = " + p_nodeid + " AND trackpkg_sum = '"+p_sum+"'"); 
            if(rowCount(rs) < 1)
            {
                retval = true;
            }
        } catch (Exception ex)
        {
            logger.error("Error in dbTrackLogChangedForNode (Node: "+p_nodeid+" Sum: "+p_sum+" ) - " + ex.getLocalizedMessage());
        }
        return retval;
    }
    
    public static void removeOldPackageTrack(int p_nodeid)
    {
        try {
            
            logger.info("Purging Package Logs for NodeID: " + p_nodeid);
            DB db;
            String dbName = com.clavain.muninmxcd.p.getProperty("mongo.dbessentials");
            db = m.getDB(dbName);
            DBCollection col = db.getCollection("trackpkg");   
            BasicDBObject query = new BasicDBObject();
            query.append("node", p_nodeid);
            col.remove(query);
        } catch (Exception ex)
        {
            logger.error("Error in removeOldPackageTrack: " + ex.getLocalizedMessage());
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
