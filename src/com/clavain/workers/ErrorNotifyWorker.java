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


import com.clavain.checks.ReturnServiceCheck;
import static com.clavain.muninmxcd.logger;
import static com.clavain.utils.Checks.*;
import static com.clavain.utils.Database.connectToDatabase;
import static com.clavain.utils.Generic.getUnixtime;
import com.clavain.workers.ServiceCheckInspector;
import java.sql.Connection;
import java.sql.ResultSet;


/**
 *
 * @author enricokern
 */
public class ErrorNotifyWorker implements Runnable {
    private Integer cid = 0;
    @Override
    public void run() {
        while(true)
        {
            try 
            {
                // get a check with a error and notifications enabled
                ReturnServiceCheck sc = com.clavain.muninmxcd.check_error_queue.take();
                logger.info("[ErrorNotifyWorker] Inspecting ServiceCheck: " + sc.getCid());
                cid = sc.getCid();
                // check if we have this service check in the database at all, it could have been deleted already, you never know
                if(checkExistsInDatabase(sc.getCid()))
                {
                    // initial fill notification data if not already set
                    Connection conn = connectToDatabase(com.clavain.muninmxcd.p);
                    java.sql.Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM notifications WHERE check_id = " + sc.getCid());

                    while(rs.next())
                    {
                        sc.setNotifyagain(rs.getInt("notifyagain"));
                        sc.setNotifydown(rs.getInt("notifydown"));
                        sc.setNotifyifup(rs.getInt("notifyifup"));
                        sc.setNotifyflap(rs.getInt("notifyflap"));
                        logger.info("[ErrorNotifyWorker "+sc.getCid()+"] Updated NotifyTimes");
                    }

                    // initial fill json and checkname if not already set
                    if(sc.getJson() == null)
                    {
                        stmt = conn.createStatement();
                        rs = stmt.executeQuery("SELECT service_checks.json,service_checks.cinterval,service_checks.check_name,service_checks.check_type,check_types.check_name as servicename FROM `service_checks` LEFT JOIN check_types ON service_checks.check_type = check_types.id WHERE service_checks.id = " + sc.getCid());
                        while(rs.next())
                        {
                            sc.setJson(rs.getString("json"));
                            sc.setCheckname(rs.getString("check_name"));
                            sc.setChecktype(rs.getString("servicename"));
                            sc.setInterval(rs.getInt("cinterval"));
                            logger.info("[ErrorNotifyWorker "+sc.getCid()+"] Retrieved JSON");
                        }
                    }

                    // TODO: check if we have a downtime for it, if so do nothing

                    // if its really down, add a own handler thread, otherwise remove it from inspection list and go on
                    boolean isCheckDown;
                    
                    isCheckDown = isServiceCheckDown(sc);
                    

                    if(isCheckDown)
                    {
                        sc.setDownTimeConfirmedAt(getUnixtime());
                        new Thread(new ServiceCheckInspector(sc)).start();
                    }
                    else
                    {
                        logger.info("[ErrorNotifyWorker " + sc.getCid()+"] Exiting. SERVICE CHECK IS FINE, might respawn new worker");
                        removeCheckFromProcessingList(sc.getCid());
                    }
                } 
                else
                {
                        logger.info("[ErrorNotifyWorker " + sc.getCid()+"] Exiting. CHECK IS NOT IN DATABASE");
                        removeCheckFromProcessingList(sc.getCid());                    
                }
   
                Thread.sleep(100);
            } catch (Exception ex)
            {
                removeCheckFromProcessingList(cid);
                logger.fatal("Error in ErrorNotifyWorker (Check: "+cid+") : " + ex.getLocalizedMessage());
                ex.printStackTrace();
            }
        }
    }
    
}
