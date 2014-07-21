/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.alerts.ratelimiter;

import static com.clavain.muninmxcd.logger;
import com.clavain.alerts.msg.ShortTextMessage;
import static com.clavain.alerts.Methods.sendSMSMessage;
/**
 *
 * @author enricokern
 */
public class SMSLimiter implements Runnable {

    @Override
    public void run() {
        logger.info("SMSLimiter started (API Rate Limiting)");
        while(true)
        {
            try {
                ShortTextMessage sms = com.clavain.muninmxcd.notification_sms_queue.take();
                logger.info("[SMSLimiter] Processing Message to: " + sms.getMobile() + " MSG: " + sms.getMessage());
                sendSMSMessage(sms.getMessage(), sms.getMobile());
                Thread.sleep(500);
            } catch (Exception ex)
            {
                logger.error("Error in SMSLimiter: " + ex.getLocalizedMessage());
            }
        }
    }
    
}
