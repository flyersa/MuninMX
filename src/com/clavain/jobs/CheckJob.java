/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.jobs;

import com.clavain.checks.ReturnDebugTrace;
import com.clavain.checks.ReturnServiceCheck;
import com.clavain.checks.RunDebugTrace;
import com.clavain.checks.RunServiceCheck;
import com.clavain.json.ServiceCheck;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import static com.clavain.utils.Generic.returnServiceCheck;
import static com.clavain.utils.Generic.checkIsProcessing;
import static com.clavain.utils.Generic.getUnixtime;
import static com.clavain.utils.Database.serviceCheckGotDowntime;
import com.mongodb.BasicDBObject;

/**
 *
 * @author enricokern
 */
public class CheckJob implements Job {

    
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        try {
            JobDataMap dataMap = jec.getJobDetail().getJobDataMap();
            Integer cid = dataMap.getInt("cid");
            ServiceCheck sc = returnServiceCheck(cid);
            
            // is check active?
            if(!sc.isIs_active())
            {
                com.clavain.muninmxcd.logger.info("[CheckJob] Check " + sc.getCheckname() + " with id " + sc.getCid() + " is paused. skipping run");
                return;
            }
            
            ReturnServiceCheck rsc = new RunServiceCheck(sc).execute();

            
            ReturnDebugTrace rdt = null;
            sc.setIterations(sc.getIterations() + 1);

            // do trace because of error?
            if(rsc.getReturnValue() == 2 || rsc.getReturnValue() == 3)
            {
                rdt = new RunDebugTrace(sc).execute();
            }
            
            BasicDBObject doc = new BasicDBObject();
            if(rdt != null)
            {
                com.clavain.muninmxcd.logger.info("[CheckJob] Added Trace for Check "+sc.getCheckname()+" with id " + sc.getCid());
                doc.put("cid", rdt.getCid());
                doc.put("status", rdt.getRetval());
                if(rdt.getOutput().length() > 1)
                {
                    doc.put("output",rdt.getOutput());
                }  
                doc.put("time", rdt.getChecktime());
                doc.put("user_id", rdt.getUser_id());
                doc.put("type", "trace");
                com.clavain.muninmxcd.mongo_check_queue.put(doc);
            }
            
            // ok schedule this shit for data insertion
            if(rsc != null)
            {
                doc = null;
                doc = new BasicDBObject();
                doc.put("cid", rsc.getCid());
                doc.put("status", rsc.getReturnValue());
                if(rsc.getOutput().size() > 1)
                {
                    doc.put("pdata",rsc.getOutput().get(1));
                }
                doc.put("hread", rsc.getOutput().get(0));
                doc.put("time", rsc.getChecktime());
                doc.put("user_id", rsc.getUser_id());
                doc.put("type", "check");
                com.clavain.muninmxcd.mongo_check_queue.put(doc);
                // HELL SHIT DO WE HAVE A ALERT?
                if(rsc.getReturnValue() == 2 || rsc.getReturnValue() == 3)
                {
                      // check if its already handled
                      if(!checkIsProcessing(rsc.getCid()))
                      {
                        com.clavain.muninmxcd.logger.info("[ServiceCheck] - Check " + sc.getCheckname() + " with id " + sc.getCid() + " is in error state. Starting Inspector"); 
                        // START Inspector
                        com.clavain.muninmxcd.errorProcessing.add(rsc.getCid());
                        com.clavain.muninmxcd.check_error_queue.add(rsc);
                      }
                      else
                      {
                          com.clavain.muninmxcd.logger.info("[ServiceCheck] - Check " + sc.getCheckname() + " with id " + sc.getCid() + " is in error state but already in inspection");
                      }
                }
            }
            
        } catch (Exception ex) {
            Logger.getLogger(CheckJob.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
