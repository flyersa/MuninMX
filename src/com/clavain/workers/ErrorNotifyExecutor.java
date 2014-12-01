/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.workers;

import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.p;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author enricokern
 */
public class ErrorNotifyExecutor implements Runnable {

    @Override
    public void run() {
        try
        {
            while(true)
            {
                Thread.sleep(500);
                ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(p.getProperty("errornotify.threads")));
                
                executor.execute(new ErrorNotifyWorker());

                executor.shutdown();
                while (!executor.isTerminated()) {
                    Thread.sleep(100);
                }
                logger.info("ErrorNotifyWorker Set finished. Restarting.");
                System.gc();
            }
        } catch (Exception ex)
        {
            logger.fatal("Error in ErrorNotifyWorker " + ex.getLocalizedMessage());
        }
    }
    
}
