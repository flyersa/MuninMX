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
          int maxjobage = Integer.parseInt(com.clavain.muninmxcd.p.getProperty("job.maxruntime","60"));
          int maxAge = getUnixtime() + maxjobage;
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
