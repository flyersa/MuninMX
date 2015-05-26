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
import com.clavain.alerts.msg.ShortTextMessage;
import static com.clavain.alerts.Methods.sendTTSMessage;
import com.clavain.alerts.msg.TTSMessage;
/**
 *
 * @author enricokern
 */
public class TTSLimiter implements Runnable {

    @Override
    public void run() {
        logger.info("TTSLimiter started (API Rate Limiting)");
        while(true)
        {
            try {
                TTSMessage tts = com.clavain.muninmxcd.notification_tts_queue.take();
                logger.info("[TTSLimiter] Processing Message to: " + tts.getMobile() + " MSG: " + tts.getMessage());
                sendTTSMessage(tts.getMessage(), tts.getMobile());
                Thread.sleep(500);
            } catch (Exception ex)
            {
                logger.error("Error in TTSLimiter: " + ex.getLocalizedMessage());
            }
        }
    }
    
}
