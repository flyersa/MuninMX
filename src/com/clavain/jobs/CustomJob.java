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
