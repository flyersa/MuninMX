
package com.unbelievable.jobs;

import com.unbelievable.munin.MuninNode;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import static com.unbelievable.utils.Generic.*;
import static com.unbelievable.muninmxcd.logger;
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
          
          runner.start();
          try
          {
          while(true)
          {
              Thread.sleep(1000);
              if(runner.isAlive())
              {
                  if(getUnixtime() > maxAge)
                  {
                    logger.warn("interrupting JobRunner for node: " + nodeId);
                    runner.interrupt();
                    
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
