package com.unbelievable.utils;

import com.unbelievable.munin.MuninNode;
import java.util.Iterator;
import static com.unbelievable.muninmxcd.v_munin_nodes;
import static com.unbelievable.muninmxcd.p;
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
        for (MuninNode l_mn : v_munin_nodes) {
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
        for (MuninNode l_mn : v_munin_nodes) {
            if(l_mn.getNode_id() == nodeId)
            {
                return l_mn;
            }
        }
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
