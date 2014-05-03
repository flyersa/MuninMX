/*
 * MuninToMongo
 * Copyright (c) 2013 by the unbelievable machine company GmbH
 * www.unbelievable-machine.com
 * 
 */
package com.unbelievable.workers;

import com.unbelievable.munin.MuninNode;




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
        if(!p_mn_node.loadPlugins())
        {
            com.unbelievable.muninmxcd.v_munin_nodes.remove(p_mn_node);
            com.unbelievable.muninmxcd.logger.warn("** Removed " + p_mn_node.getHostname() + " from nodelist because unable to fetch plugins, retrying later");      
        }
        else
        {
            com.unbelievable.muninmxcd.logger.info("* " + p_mn_node.getHostname()+ " got " + p_mn_node.getGraphCount() + " graphs of " + p_mn_node.getLoadedPlugins().size() + " plugins");
        }
        
    }
    
}
