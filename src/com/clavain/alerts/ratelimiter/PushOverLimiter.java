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
package com.clavain.alerts.ratelimiter;

import static com.clavain.muninmxcd.logger;
import com.clavain.alerts.msg.PushOverMessage;
import static com.clavain.alerts.Methods.sendPushOverMessage;
/**
 *
 * @author enricokern
 */
public class PushOverLimiter implements Runnable {

    @Override
    public void run() {
        logger.info("PushOverLimiter started (API Rate Limiting)");
        while(true)
        {
            try {
                PushOverMessage pom = com.clavain.muninmxcd.notification_pushover_queue.take();
                logger.info("[PushOverLimiter] Processing Message with UserKey: " + pom.getUserKey() + " MSG: " + pom.getMessage());
                sendPushOverMessage(pom.getUserKey(), pom.getTitle(), pom.getMessage());
                Thread.sleep(500);
            } catch (Exception ex)
            {
               logger.error("Error in PushOverLimiter: " + ex.getLocalizedMessage());
            }
        }
    }
    
}
