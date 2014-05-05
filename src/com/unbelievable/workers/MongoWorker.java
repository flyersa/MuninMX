package com.unbelievable.workers;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoOptions;

import static com.unbelievable.muninmxcd.m;
import static com.unbelievable.muninmxcd.mongo_queue;
import static com.unbelievable.muninmxcd.logMore;
import static com.unbelievable.muninmxcd.logger;
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
        
        db = m.getDB("muninmx");
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
                    col = db.getCollection(doc.getString("user_id") + "_" + doc.getString("nodeid")+"_"+doc.getString("plugin"));
                    doc.removeField("hostname");
                    doc.removeField("nodeid");
                    doc.removeField("user_id");
                    doc.removeField("plugin");
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
