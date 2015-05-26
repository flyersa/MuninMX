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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoOptions;

import static com.clavain.muninmxcd.m;
import static com.clavain.muninmxcd.mongo_queue;
import static com.clavain.muninmxcd.logMore;
import static com.clavain.muninmxcd.logger;
import java.util.Iterator;

/**
 *
 * @author enricokern
 */
public class MongoWorker implements Runnable {
    private DB db;
    private DBCollection col;
    
    @Override
    public void run() {
        
        String dbName = com.clavain.muninmxcd.p.getProperty("mongo.dbname");
        db = m.getDB(dbName);
        
        logger.info("Started MongoWorker");
        String plugin = "";
        while(true)
        { 
            try
            {
                BasicDBObject doc = mongo_queue.take();
                if(doc != null)
                {
                    plugin = doc.getString("plugin");
                    // each hostname got its own collection
                    col = db.getCollection(doc.getString("user_id") + "_" + doc.getString("nodeid"));
                    doc.removeField("hostname");
                    doc.removeField("nodeid");
                    doc.removeField("user_id");
                    //doc.removeField("plugin");
                    //db.requestStart();
                    col.insert(doc);
                    if(logMore)
                    {
                        logger.info("Mongo: Wrote " + plugin + " / " + doc.getString("graph") + " / " + doc.getString("value"));
                    }
                    //db.requestDone();
                }
                else
                {
                    Thread.sleep(50);    
                }
                
            } catch (Exception ex)
            {
                logger.fatal("Error in MongoWorker: " + ex.getLocalizedMessage());
                ex.printStackTrace();
            }
            
        }
    }
    
}
