package com.unbelievable.utils;

import com.unbelievable.munin.MuninNode;
import java.util.Iterator;

import static com.unbelievable.muninmxcd.p;
import static com.unbelievable.muninmxcd.v_munin_nodes;
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
        for (MuninNode l_mn : com.unbelievable.muninmxcd.v_munin_nodes) {
            if(l_mn.getNodename().equals(p_strNodename) || l_mn.getHostname().equals(p_strNodename))
            {
                return l_mn;
            }
        }
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
        com.unbelievable.muninmxcd.logger.warn("getMuninNode: Cant find nodeId " + nodeId);
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
}
