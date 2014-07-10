/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.jobs;

import static com.clavain.muninmxcd.logger;
import static com.clavain.utils.Generic.getUnixtime;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author enricokern
 */
public class CustomJob implements Job {
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
          JobDataMap dataMap = jec.getJobDetail().getJobDataMap();
          Integer customId = dataMap.getInt("customId");
          
          Thread runner = new Thread(new CustomJobRunner(customId));
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
                      logger.warn("interrupting CustomJobRunner for custom interval: " + customId);
                      runner.interrupt();
                      runner.suspend();
                      runner.interrupt();
                      if(runner.isAlive())
                      {
                          logger.warn("final call, stopping CustomJobRunner after 2 unsuccessfull interrupts for custom interval " + customId);
                          runner.stop();
                          Thread.sleep(100);
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
              logger.error("Error in CustomJob: " + ex.getLocalizedMessage());
          }
    
    }
    
}
