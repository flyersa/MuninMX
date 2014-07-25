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
import static com.clavain.muninmxcd.mongo_queue;
import static com.clavain.muninmxcd.logMore;
import static com.clavain.muninmxcd.logger;
import java.util.Iterator;

/**
 *
 * @author enricokern
 */
public class MongoWorker implements Runnable {
    private static DB db;
    private static DBCollection col;
    
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
