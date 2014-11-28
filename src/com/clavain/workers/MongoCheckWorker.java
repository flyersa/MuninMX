/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
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
