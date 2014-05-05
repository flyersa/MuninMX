/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.unbelievable.jobs;

import com.unbelievable.munin.MuninNode;
import static com.unbelievable.muninmxcd.logger;
import static com.unbelievable.utils.Generic.getMuninNode;

/**
 *
 * @author enricokern
 */
public class JobRunner implements Runnable {

    private int nodeId;
    
    public JobRunner(int p_nid)
    {
        nodeId = p_nid;
    }
    
    @Override
    public void run() {
        MuninNode mn = getMuninNode(nodeId);
        try{
          
            if(mn != null)
            {
              mn.run();
            }
            else
            {
              logger.error("Tried to Run job for NodeID: " + nodeId + " but this node is not in nodelist :("); 
            }            
        } catch (Exception ex)
        {
            logger.info(mn.getHostname() + "Monitoring job stopped - Terminated");
            logger.error("JobRunner exception: " + ex.getLocalizedMessage());
        }

    }
    
}
