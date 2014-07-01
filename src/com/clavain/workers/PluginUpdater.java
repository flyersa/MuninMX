/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.workers;

import com.clavain.munin.MuninNode;




/**
 *
 * @author enricokern
 */
public class PluginUpdater implements Runnable {

    private MuninNode p_mn_node;
    
    public PluginUpdater(MuninNode it_mn) {
        p_mn_node = it_mn;
    }

    @Override
    public void run() {
        com.clavain.muninmxcd.logger.info("* " + p_mn_node.getHostname()+ " Loading Plugins");
        if(!p_mn_node.loadPlugins())
        {
            com.clavain.muninmxcd.v_munin_nodes.remove(p_mn_node);
            com.clavain.muninmxcd.logger.warn("** Removed " + p_mn_node.getHostname() + " from nodelist because unable to fetch plugins, retrying later");      
        }
        else
        {
            com.clavain.muninmxcd.logger.info("* " + p_mn_node.getHostname()+ " got " + p_mn_node.getGraphCount() + " graphs of " + p_mn_node.getLoadedPlugins().size() + " plugins");
        }
        
    }
    
}
