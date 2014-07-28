/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
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
