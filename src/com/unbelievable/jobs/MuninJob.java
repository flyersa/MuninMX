
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
          MuninNode mn = getMuninNode(nodeId);
          if(mn != null)
          {
              mn.run();
          }
          else
          {
              logger.error("Tried to Run job for NodeID: " + nodeId + " but this node is not in nodelist :("); 
          }
    }
    
}
