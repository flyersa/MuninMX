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
import static com.clavain.muninmxcd.mongo_essential_queue;
import static com.clavain.muninmxcd.logMore;
import static com.clavain.muninmxcd.logger;
import java.util.Iterator;

/**
 *
 * @author enricokern
 */
public class MongoEssentialWorker implements Runnable {
    private static DB db;
    private static DBCollection col;
    
    @Override
    public void run() {
        
        String dbName = com.clavain.muninmxcd.p.getProperty("mongo.dbessentials");
        db = m.getDB(dbName);
        
        logger.info("Started MongoWorker for MuninMX Essentials");
        String plugin = "";
        while(true)
        { 
            try
            {
                BasicDBObject doc = mongo_essential_queue.take();
                if(doc != null)
                {
                    // if trackpkg, add package entry, if essential information, store somewhere else
                    if(doc.getString("type").equals("trackpkg"))
                    {
                        col = db.getCollection("trackpkg");     
                        doc.removeField("type");
                    }
                    else
                    {
                        col = db.getCollection(doc.getString("node")+"_ess");
                        doc.removeField("node");
                        doc.removeField("type");
                    }

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
                logger.fatal("Error in MongoEssentialWorker: " + ex.getLocalizedMessage());
                ex.printStackTrace();
            }
            
        }
    }
    
}
