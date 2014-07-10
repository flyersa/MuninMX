/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.utils;

import com.clavain.munin.MuninNode;
import com.clavain.munin.MuninPlugin;
import java.util.Iterator;

import static com.clavain.muninmxcd.p;
import static com.clavain.muninmxcd.v_munin_nodes;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
/**
 *
 * @author enricokern
 */
public class Generic {

    /** return unix timestamp */
    public static int getUnixtime()
    {
        return (int) (System.currentTimeMillis() / 1000L);
    }    
    
    /** Return a MuninNode by Nodename or Hostname
     * 
     */
    public static MuninNode getMuninNode(String p_strNodename)
    {
        for (MuninNode l_mn : com.clavain.muninmxcd.v_munin_nodes) {
            if(l_mn.getNodename().equals(p_strNodename) || l_mn.getHostname().equals(p_strNodename))
            {
                return l_mn;
            }
        }
        return null;
    } 
   
    /** Return a MuninPlugin for custom jobs by Custom ID
     * 
     * @param customId
     * @return 
     */
    public static MuninPlugin getMuninPluginForCustomJob(Integer customId)
    {
        Iterator it = com.clavain.muninmxcd.v_cinterval_plugins.iterator();
        while (it.hasNext())
        {
             MuninPlugin l_mp = (MuninPlugin) it.next();
             if(l_mp.getCustomId().equals(customId))
             {
                return l_mp;
             }
        }
        com.clavain.muninmxcd.logger.error("getMuninPluginForCustomJob: Cant find plugin for custom id: " +  customId);
        return null;
    }      
    
    /** Return a MuninNode by Node ID
     * 
     * @param nodeId
     * @return 
     */
    public static MuninNode getMuninNode(Integer nodeId)
    {
        Iterator it = v_munin_nodes.iterator();
        while (it.hasNext())
        {
             MuninNode l_mn = (MuninNode) it.next();
             if(l_mn.getNode_id().equals(nodeId))
             {
                return l_mn;
             }
        }
        com.clavain.muninmxcd.logger.warn("getMuninNode: Cant find nodeId " + nodeId);
        return null;
    }  
    
    public static boolean isPluginIgnored(String pluginName)
    {
        boolean retval = false;
        if(p.getProperty("ignore.plugins") != null)
        {
            StringTokenizer st = new StringTokenizer(p.getProperty("ignore.plugins"), ",");
            while (st.hasMoreElements()) {
                if(st.nextElement().toString().equals(pluginName))
                {
                    return true;
                }
	    }            
        }
        return retval;
    }
    
       public static long getStampFromTimeAndZone(String time, String zone)
    {
        long retval = 0;
        ///usr/local/bin/php /opt/pcvd/getTime.php
        List<String> commands = new ArrayList<String>();
        commands.add("/usr/bin/php");
        commands.add("/opt/muninmx/getTime.php");
        commands.add(zone);
        commands.add(time);
        try
        {
            ProcessBuilder pb = new ProcessBuilder(commands);
            Process p = pb.start();
            InputStream is = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
           // print output
            String str_output = null;
            String str_parse = null;
            while ((str_output = reader.readLine()) != null) {
                //logger.info(str_output);
                str_parse = str_output;
            }  
            com.clavain.muninmxcd.logger.info("getTime.php returned: " + str_parse);
            retval = Long.parseLong(str_parse);
        } catch (Exception ex)
        {
            com.clavain.muninmxcd.logger.error("Error in getStampFromTimeAndZone:" + ex.getLocalizedMessage());
        }
        return retval;
    }    
}
