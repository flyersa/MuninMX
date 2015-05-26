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
package com.clavain.executors;

import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.p;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.clavain.workers.MongoCheckWorker;

/**
 *
 * @author enricokern
 */
public class MongoCheckExecutor implements Runnable {

    @Override
    public void run() {
        try
        {
            while(true)
            {
                Thread.sleep(50);
                ExecutorService executor = Executors.newFixedThreadPool(2);
                
                executor.execute(new MongoCheckWorker());

                executor.shutdown();
                while (!executor.isTerminated()) {
                    Thread.sleep(50);
                }
                logger.info("MongoCheckWorker Set finished. Restarting MongoCheckWorkers.");
                System.gc();
            }
        } catch (Exception ex)
        {
            logger.fatal("Error in MongoCheckExecutor: " + ex.getLocalizedMessage());
        }
    }
    
}
