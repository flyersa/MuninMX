/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
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
