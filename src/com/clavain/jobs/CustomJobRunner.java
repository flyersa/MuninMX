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
package com.clavain.jobs;

import com.clavain.munin.MuninNode;
import com.clavain.munin.MuninPlugin;
import static com.clavain.muninmxcd.logger;
import static com.clavain.utils.Generic.getMuninPluginForCustomJob;
import static com.clavain.utils.Generic.getMuninNode;
import static com.clavain.utils.Generic.getUnixtime;
import static com.clavain.utils.Database.getMuninPluginForCustomJobFromDb;
/**
 *
 * @author enricokern
 */
public class CustomJobRunner implements Runnable {

    private int customId;
    
    CustomJobRunner(Integer p_customId) {
        customId = p_customId;
    }

    @Override
    public void run() {
        MuninPlugin mp = getMuninPluginForCustomJob(customId);
        try
        { 
            if(mp == null)
            {
                logger.error("Tried to Run job for Custom Interval: " + customId + " but this MuninPlugin is not in customlist :("); 
                return;
            }          
            
            // get associated node
            MuninNode mn = getMuninNode(mp.get_NodeId());
            if(mn == null)
            {
                logger.error("Tried to Run job for Custom Interval: " + customId + " but cannot found referenced MuninNode with id: " + mp.get_NodeId()); 
                // maybe also dequeue this job now and remove from list?
                return;                
            }
            
            // check if we need to update because we have no graphs?
            if(mp.getGraphs().isEmpty())
            {
                logger.warn("No Graphs for Custom Interval: " + customId + " trying to refresh...");
                MuninPlugin nmp = getMuninPluginForCustomJobFromDb(customId);
                if(nmp == null)
                {
                    logger.error("Tried to refresh Custom Interval: " + customId + " but received null object :/ returning");
                    return;
                }
                if(!nmp.getGraphs().isEmpty())
                {
                    com.clavain.muninmxcd.v_cinterval_plugins.remove(mp);
                    com.clavain.muninmxcd.v_cinterval_plugins.add(nmp);
                    mp = nmp;
                    logger.info("Replaced MuninPlugin for Custom Interval: " + customId + " with newer version");
                }
                else
                {
                    logger.warn("Refresh did not return new Graphs for Custom Interval: " + customId);
                    return;
                }
            }
            
            String str_Hostname;
            if(mn.getStr_via().equals("unset"))
            {
                str_Hostname = mn.getHostname();
            }
            else
            {
                str_Hostname = mn.getStr_via();
            }
            logger.info(mp.getCustomId() + " custom interval job started");
            int iCurTime = getUnixtime();
            mp.updateAllGraps(str_Hostname, mn.getPort(), null, mp.getQuery_interval());
            mn.queuePluginFetch(mp.returnAllGraphs(), mp.getPluginName(),customId);
            int iRunTime = getUnixtime() - iCurTime;
            logger.info(mp.getCustomId() + " custom interval job stopped - runtime: " + iRunTime);
        } 
        catch (ThreadDeath td)
        {
            logger.info(mp.getCustomId()+ " custom interval job stopped - Terminated - " + td.getMessage()); 
            return;
        }
        catch (Exception ex)
        {
            logger.info(mp.getCustomId()+ " custom interval job stopped - Terminated");
            logger.error("CustomJobRunner exception: " + ex.getLocalizedMessage());
        }           
    }
    
}
