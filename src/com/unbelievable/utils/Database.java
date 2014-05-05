package com.unbelievable.utils;

import com.unbelievable.munin.MuninPlugin;
import static com.unbelievable.muninmxcd.conn;
import static com.unbelievable.muninmxcd.logger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author enricokern
 */
public class Database {
    public static void dbUpdatePluginForNode(Integer nodeId, MuninPlugin mp)
    {
        try {
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from node_plugins WHERE node_id = "+nodeId+" AND pluginname = '"+mp.getPluginName()+"'"); 
            if(rowCount(rs) < 1)
            {
                logger.info("[Node " + nodeId + "] Adding Plugin: " + mp.getPluginName() + " to database");
                stmt.executeUpdate("INSERT INTO node_plugins (node_id,pluginname,plugintitle,plugininfo,plugincategory) VALUES ("+nodeId+",'"+mp.getPluginName()+"','"+mp.getPluginTitle()+"','"+mp.getPluginInfo()+"','"+mp.getStr_PluginCategory()+"')");
            }
        } catch (Exception ex)
        {
            logger.error("Error in dbUpdatePlugin: " + ex.getLocalizedMessage());
        }
    }
    
    public static void dbDeleteMissingPlugins(Integer nodeId,CopyOnWriteArrayList<MuninPlugin> mps)
    {
        try {
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
