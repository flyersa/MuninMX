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
import static com.clavain.muninmxcd.mongo_check_queue;
import static com.clavain.muninmxcd.logMore;
import static com.clavain.muninmxcd.logger;
import java.util.Iterator;

/**
 *
 * @author enricokern
 */
public class MongoCheckWorker implements Runnable {
    private DB db;
    private DBCollection col;
    
    @Override
    public void run() {
        
        String dbName = com.clavain.muninmxcd.p.getProperty("mongo.dbchecks");
        db = m.getDB(dbName);
        
        logger.info("Started MongoCheckWorker");

        while(true)
        { 
            try
            {
                BasicDBObject doc = mongo_check_queue.take();
                if(doc != null)
                {

                    // index
                    if(doc.getString("type").equals("check"))
                    {
                        col = db.getCollection(doc.getString("user_id")+"cid"+doc.getString("cid"));
                    }
                    else
                    {
                        col = db.getCollection(doc.getString("user_id")+"traces"+doc.getString("cid"));    
                    }
                    doc.removeField("user_id");
                    doc.removeField("type");

                    col.insert(doc);
                    if(logMore)
                    {
                        logger.info("Mongo: Wrote " + doc.getString("hread") + " / " + doc.getString("cid"));
                    }
                    //db.requestDone();
                }
                else
                {
                    Thread.sleep(50);    
                }
                
            } catch (Exception ex)
            {
                logger.fatal("Error in MongoCheckWorker: " + ex.getLocalizedMessage());
                ex.printStackTrace();
            }
            
        }
    }
    
}
