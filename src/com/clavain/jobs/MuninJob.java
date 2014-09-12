/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.jobs;

import com.clavain.munin.MuninNode;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import static com.clavain.utils.Generic.*;
import static com.clavain.muninmxcd.logger;
/**
 *
 * @author enricokern
 */
public class MuninJob implements Job {

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
          JobDataMap dataMap = jec.getJobDetail().getJobDataMap();
          Integer nodeId = dataMap.getInt("nodeId");
          
          Thread runner = new Thread(new JobRunner(nodeId));
          int maxAge = getUnixtime() + 60;
          boolean keepMeRunning = true;
          
          runner.start();
          try
          {
            while(keepMeRunning)
            {
                Thread.sleep(1000);
                if(runner.isAlive())
                {
                    if(getUnixtime() > maxAge)
                    {
                      keepMeRunning = false;
                      logger.warn("interrupting JobRunner for node: " + nodeId);
                      runner.interrupt();
                      Thread.sleep(500);
                      
                      if(runner.isAlive())
                      {
                          logger.warn("final call, stopping JobRunner after 2 unsuccessfull interrupts for node " + nodeId);
                          runner.stop();
                      }
                      return;
                    }
                }
                else
                {
                    return;
                }
            }
          } catch (Exception ex)
          {
              logger.error("Error in MuninJob: " + ex.getLocalizedMessage());
          }
    
    }
    
}
