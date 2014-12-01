/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.workers;

import com.clavain.checks.ReturnServiceCheck;
import static com.clavain.utils.Checks.isServiceCheckDown;
import static com.clavain.utils.Checks.removeCheckFromProcessingList;
import static com.clavain.utils.Checks.addDownTimeToDb;
import static com.clavain.alerts.Methods.*;
import static com.clavain.utils.Generic.getUnixtime;
import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.p;
/**
 *
 * @author enricokern
 */
public class ServiceCheckInspector implements Runnable {
    private ReturnServiceCheck sc;
    private int notificationruns = 0;
    
    @Override
    public void run() {
        logger.info("ServiceCheckInspector for failed check: " + sc.getCid() + " spawned. Will take care of notifications and ongoing check control");
        boolean keepMeRunning = true;
        boolean isCheckDown;
        boolean waiting = false;
        int cycle = 0;
        while(keepMeRunning)
        {
            try 
            {
                // initial notification?
                if(notificationruns == 0)
                {
                    // instant?
                    if(sc.getNotifydown() == 0)
                    {
                        sendNotifications(sc);
                        notificationruns++;
                    }
                    else
                    {
                        // convert to seconds
                        int waitTime = sc.getNotifydown() * 60;
                        // time already passed?
                        int waitTimeUnix = sc.getDownTimeConfirmedAt() + waitTime;
                        if(getUnixtime() > waitTimeUnix)
                        {
                            logger.info("[ServiceCheckInspector "+sc.getCid()+"] Waittime already passed. Sending notifications NOW");
                            sendNotifications(sc);
                            notificationruns++;                            
                        }
                        else
                        {
                            // wait for it
                            logger.info("[ServiceCheckInspector "+sc.getCid()+"] Notifydown set to: " + sc.getNotifydown() + " minutes. Waiting and rechecking...");
                            int curTime = getUnixtime();
                            while(curTime < waitTimeUnix)
                            {
                                Thread.sleep(10000);
                                curTime = getUnixtime();
                            }
                            // check if its down
                            logger.info("[ServiceCheckInspector "+sc.getCid()+"] " + sc.getNotifydown() + " Minutes for NotifyDown passed. Verifying if check is stil critical");
      
                            isCheckDown = isServiceCheckDown(sc);
                           
                            if(isCheckDown)
                            {
                                sendNotifications(sc);
                                notificationruns++;                                     
                            }
                            else
                            {
                                logger.info("[ServiceCheckInspector "+sc.getCid()+"] " + sc.getNotifydown() + " Minutes for NotifyDown passed. Check is FINE. not sending notifications");    
                                keepMeRunning = false;
                                addDownTimeToDb(sc.getCid(), sc.getDownTimeConfirmedAt(), getUnixtime());
                                removeCheckFromProcessingList(sc.getCid());
                            }
                        }
                    }
                }
                else
                {
                    // renotifications, up notifications. At least one notification was now send. Check how often we need to re-alert, or if we need to send up alerts
                    // not notifying again. just Wait until its back up
                    if(sc.getNotifyagain() == 0)
                    {
                        waiting = true;
                        while(waiting)
                        {
                            
                            isCheckDown = isServiceCheckDown(sc);
                                
                            if(!isCheckDown)
                            {
                                waiting = false;
                            }
                            else
                            {
                                // check once every minute
                                Thread.sleep(60000);
                            }
                        }
                        // exiting, check is back up
                        if(sc.getNotifyifup() > 0)
                        {
                            sendUPNotifications(sc);
                        }
                        keepMeRunning = false;
                        removeCheckFromProcessingList(sc.getCid());
                        addDownTimeToDb(sc.getCid(), sc.getDownTimeConfirmedAt(), getUnixtime());
                        logger.info("[ServiceCheckInspector "+sc.getCid()+"] " + sc.getNotifydown() + " Check is FINE now. Exiting");    
                    }
                    else
                    {
                        // notify every x cycle
                        waiting = true;
                        while(waiting)
                        {
                            
                            isCheckDown = isServiceCheckDown(sc);
                                
                            if(!isCheckDown)
                            {
                                waiting = false;
                            }
                            else
                            {
                                // notify again every x cycle
                                cycle++;
                                if(cycle == sc.getNotifyagain())
                                {
                                    logger.info("[ServiceCheckInspector "+sc.getCid()+"]  Cycle " + cycle + " ("+sc.getNotifyagain()+" set) reached and stil down. Sending Notifications");
                                    cycle = 0;
                                    sendNotifications(sc);
                                    notificationruns++;                                      
                                }
                            }
                            // Wait check time
                            if(isCheckDown)
                            {
                                Thread.sleep(sc.getInterval() * 100000);
                            }
                        }
                        
                        // exiting, check is back up
                        if(sc.getNotifyifup() > 0)
                        {
                            sendUPNotifications(sc);
                        }
                        keepMeRunning = false;
                        removeCheckFromProcessingList(sc.getCid());
                        logger.info("[ServiceCheckInspector "+sc.getCid()+"] " + sc.getNotifydown() + " Check is FINE now. Exiting");   
                        addDownTimeToDb(sc.getCid(), sc.getDownTimeConfirmedAt(), getUnixtime());
                    }
                    
                }
                Thread.sleep(5000);
            } catch (Exception ex)
            {
                logger.error("Error in ServiceCheckInspector for Check: " + sc.getCid() + " " + ex.getLocalizedMessage());
                keepMeRunning = false;
                removeCheckFromProcessingList(sc.getCid());               
                ex.printStackTrace();
            }
        }
        logger.info("[ServiceCheckInspector " + sc.getCid() + "] Exiting. All Work done.");
    }
    
    public ServiceCheckInspector(ReturnServiceCheck p_sc)
    {
        sc = p_sc;
    }
}
