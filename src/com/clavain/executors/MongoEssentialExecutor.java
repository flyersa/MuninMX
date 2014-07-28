/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.executors;

import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.p;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.clavain.workers.MongoEssentialWorker;

/**
 *
 * @author enricokern
 */
public class MongoEssentialExecutor implements Runnable {

    @Override
    public void run() {
        try
        {
            while(true)
            {
                Thread.sleep(50);
                ExecutorService executor = Executors.newFixedThreadPool(2);
                
                executor.execute(new MongoEssentialWorker());

                executor.shutdown();
                while (!executor.isTerminated()) {
                    Thread.sleep(50);
                }
                logger.info("MongoPkgWorker Set finished. Restarting MongoPkgWorkers.");
                System.gc();
            }
        } catch (Exception ex)
        {
            logger.fatal("Error in MongoPkgExecutor: " + ex.getLocalizedMessage());
        }
    }
    
}
