/*
 * MuninMX
 * Written by Enrico Kern, kern@clavain.com
 * www.clavain.com
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
