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
 *
 *
 * 
 * special thanks to the unbelievable machine company GmbH
 * www.unbelievable-machine.com
 */
package com.clavain;

import com.clavain.alerts.Alert;
import com.clavain.alerts.msg.PushOverMessage;
import com.clavain.alerts.msg.ShortTextMessage;
import com.clavain.alerts.msg.TTSMessage;
import com.clavain.alerts.ratelimiter.PushOverLimiter;
import com.clavain.alerts.ratelimiter.SMSLimiter;
import com.clavain.alerts.ratelimiter.TTSLimiter;
import com.clavain.checks.ReturnDebugTrace;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.clavain.executors.MongoExecutor;
import com.clavain.executors.MongoEssentialExecutor;
import com.clavain.handlers.JettyLauncher;
import com.clavain.json.ServiceCheck;
import com.clavain.munin.MuninNode;
import com.clavain.munin.MuninPlugin;
import com.clavain.rca.Analyzer;
import com.clavain.workers.PluginUpdater;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import static com.clavain.utils.Database.*;
import static com.clavain.utils.Quartz.*;
import com.clavain.utils.SocketCheck;
import com.clavain.workers.MongoWorker;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import static com.clavain.utils.Generic.getUnixtime;
import com.clavain.workers.NewNodeWatcher;
import static com.clavain.utils.Database.dbScheduleAllCustomJobs;
import com.clavain.workers.DataRetentionWorker;
import java.io.File;
import java.io.ObjectInputStream;
import com.clavain.checks.ReturnServiceCheck;
import com.clavain.executors.MongoCheckExecutor;
import com.clavain.workers.ErrorNotifyExecutor;
import com.google.gson.Gson;
import java.util.List;
/**
 *
 * @author enricokern
 */
public class muninmxcd {
    public static Logger logger     = Logger.getRootLogger();
    public static Properties p      = null;
    public static LinkedBlockingQueue<BasicDBObject> mongo_queue = new LinkedBlockingQueue<BasicDBObject>();
    public static LinkedBlockingQueue<BasicDBObject> mongo_essential_queue = new LinkedBlockingQueue<BasicDBObject>();
    public static LinkedBlockingQueue<BasicDBObject> mongo_check_queue = new LinkedBlockingQueue<BasicDBObject>();
    public static boolean logMore   = false;
    public static MongoClient m;
    public static DB db;
    public static DBCollection col;
    public static String version    = "1.0 <Codename: Regret>";
    public static Connection conn = null;    
    public static CopyOnWriteArrayList<MuninNode> v_munin_nodes;
    public static CopyOnWriteArrayList<MuninPlugin> v_cinterval_plugins;
    public static Scheduler sched;
    public static Scheduler sched_custom;
    public static Scheduler sched_checks;
    public static CopyOnWriteArrayList<SocketCheck> v_sockets;
    // alerting
    public static LinkedBlockingQueue<PushOverMessage> notification_pushover_queue = new LinkedBlockingQueue<PushOverMessage>();
    public static LinkedBlockingQueue<ShortTextMessage> notification_sms_queue = new LinkedBlockingQueue<ShortTextMessage>();
    public static LinkedBlockingQueue<TTSMessage> notification_tts_queue = new LinkedBlockingQueue<TTSMessage>();  
    public static LinkedBlockingQueue<ReturnServiceCheck> check_error_queue = new LinkedBlockingQueue<ReturnServiceCheck>();
    public static CopyOnWriteArrayList<Alert> v_alerts = new CopyOnWriteArrayList<>();
    // RCA
    public static CopyOnWriteArrayList<Analyzer> v_analyzer = new CopyOnWriteArrayList<>();
    public static int rcajobs_running = 0;
    public static int maxnodes = 100000;
    public static int maxchecks = 100000;
    public static int socketTimeout = 30000;
    // CHECKS
    public static CopyOnWriteArrayList<ServiceCheck> v_serviceChecks = new CopyOnWriteArrayList<>();;
    public static List<Integer> errorProcessing = new CopyOnWriteArrayList<Integer>();
    public static String[] initialArgs;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
       if(args.length < 1)
       {
           System.err.println("Usage: java -jar MuninMXcd.jar <full path to config>");
           System.exit(1);
       }
        
       try
        {
            
            initialArgs = args;
            p = new Properties();
            FileInputStream propInFile = null;
            propInFile = new FileInputStream(args[0]);
            p.loadFromXML(propInFile);            
            String logfile = p.getProperty("log.file");
            socketTimeout = Integer.parseInt(p.getProperty("socket.timeout"));
            
            PatternLayout layout = new PatternLayout("%d{ISO8601} %-5p %m%n");

            ConsoleAppender consoleAppender = new ConsoleAppender( layout );
            logger.addAppender( consoleAppender );
            FileAppender fileAppender = new FileAppender( layout, logfile, false );
            logger.addAppender( fileAppender );

            logger.info("MuninMX Collector Daemon - " + version + " starting up...");
            logger.info("Loading configuration from <"+args[0]+">"  );                    
            
            String l_strLogLevel = p.getProperty("log.level");
            // ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF:
            if(l_strLogLevel.equals("ALL"))
            {
                logger.setLevel( Level.ALL );
            } 
            else if (l_strLogLevel.equals("DEBUG"))
            {
                logger.setLevel( Level.DEBUG);
            }
            else if (l_strLogLevel.equals("INFO"))
            {
                logger.setLevel( Level.INFO);
            }
            else if (l_strLogLevel.equals("WARN"))
            {
                logger.setLevel( Level.WARN);
            }
            else if (l_strLogLevel.equals("ERROR"))
            {
                logger.setLevel( Level.ERROR);
            }            
            else if (l_strLogLevel.equals("FATAL"))
            {
                logger.setLevel( Level.FATAL);
            }            
            else
            {
                logger.setLevel( Level.OFF);
            }    
            
            if(p.getProperty("log.more") != null)
            {
                if(p.getProperty("log.more").equals("true"))
                {
                    logMore = true;
                }
            }
            
        } catch (Exception ex)
        {
            System.err.println("Failed to Init basic logging infastructure: " + ex.getLocalizedMessage());
            System.exit(1);
        }
       
       
        try
        {
            // connect to db
            logger.info("Connecting to TokuMX");
            MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
            builder.connectionsPerHost(400);
            builder.autoConnectRetry(true);
            builder.threadsAllowedToBlockForConnectionMultiplier(10);
           
            // speed up inserts, we dont care if we miss some if the shit hits the fan
            builder.writeConcern(WriteConcern.NONE);
            m = new MongoClient( new ServerAddress(p.getProperty("mongo.host")),  builder.build());
 
                        
            // connect to mysql
            connectToDatabase(p);
            
            // PreFilling Nodes, max 100 in concurrent
            logger.info("Loading initial MuninNode details. This can take a few minutes...");
            v_munin_nodes = new CopyOnWriteArrayList<>();
            v_cinterval_plugins = new CopyOnWriteArrayList<>();     
            v_sockets = new CopyOnWriteArrayList<>();
            
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM nodes");
            while(rs.next())
            {
                MuninNode mn = new MuninNode();
                mn.setHostname(rs.getString("hostname"));
                mn.setNodename(rs.getString("hostname"));
                mn.setNode_id(rs.getInt("id"));
                mn.setPort(rs.getInt("port"));
                mn.setUser_id(rs.getInt("user_id"));
                mn.setQueryInterval(rs.getInt("query_interval"));
                mn.setStr_via(rs.getString("via_host"));
                mn.setAuthpw(rs.getString("authpw"));
                mn.setGroup(rs.getString("groupname"));
                v_munin_nodes.add(mn);              
                logger.info("* " + mn.getHostname() + " queued for pluginfetch");
            }
            
            // launching quartz scheduler
            logger.info("Launching Scheduler");
            SchedulerFactory sf = new StdSchedulerFactory("quartz.properties");
            sched = sf.getScheduler();   
            sched.start();   
 
            // launching quartz scheduler for custom interval
            logger.info("Launching Custom Interval Scheduler");
            SchedulerFactory sfc = new StdSchedulerFactory("customquartz.properties");
            sched_custom = sfc.getScheduler();   
            sched_custom.start();
            
            // starting API server
            new Thread(new JettyLauncher()).start();
            
            int sleepTime = Integer.parseInt(p.getProperty("startup.sleeptime"));
            int startupIterations = Integer.parseInt(p.getProperty("startup.iterations"));
            // scheduling jobs
            int i = 0;
            for (MuninNode it_mn : v_munin_nodes) {
                if(i == startupIterations)
                {
                    Thread.sleep(sleepTime);
                    i = 0;
                    logger.info("Waiting "+sleepTime+"ms for new scheduling slot");
                }
                scheduleJob(it_mn);
                i++;
            }
            
            // schedule custom interval jobs
            dbScheduleAllCustomJobs();
            
            // add all alerts
            dbAddAllAlerts();
            
            // Service Checks
            logger.info("Launching Service Check Scheduler");
            SchedulerFactory sfsc = new StdSchedulerFactory("checksquartz.properties");
            sched_checks = sfsc.getScheduler();   
            sched_checks.start();             
            // load service checks from database
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM service_checks");
            while(rs.next())
            {
                Gson gson = new Gson();
                ServiceCheck tc = gson.fromJson(rs.getString("json"), ServiceCheck.class);
                tc.setCid(rs.getInt("id"));
                tc.setUser_id(rs.getInt("user_id"));
                v_serviceChecks.add(tc);
                logger.info("* " + tc.getCheckname() + " Service Check added");
            }            
            // queue service checks
            for (ServiceCheck it_sc : v_serviceChecks) {
                scheduleServiceCheck(it_sc);
            }            
            
            // starting MongoExecutor
            new Thread(new MongoExecutor()).start();
        
            // starting MongoExecutor for Package Tracking and Essential Informations
            new Thread(new MongoEssentialExecutor()).start();            

            // starting MongoExecutor for Service Checks
            new Thread(new MongoCheckExecutor()).start();            

            // starting newnodewatcher
            new Thread(new NewNodeWatcher()).start();
            
            // start pushover sending message
            new Thread(new PushOverLimiter()).start();
            
            // SMS Limiter
            new Thread(new SMSLimiter()).start();

            // TTS Limiter
            new Thread(new TTSLimiter()).start();            
            
            // start DataRetention Worker
            new Thread(new DataRetentionWorker()).start();
            
            // start Error Notify Inspector
            new Thread(new ErrorNotifyExecutor()).start();            
            
            int curTime;
            int toTime;
            int mb = 1024*1024;
            while(true)
            {
                Thread.sleep(5000);
                System.out.println("Mongo Queue Size: " + mongo_queue.size());
                System.out.println("Mongo Check Queue Size: " + mongo_check_queue.size());
                System.out.println("Mongo Essential Queue Size: " + mongo_essential_queue.size());

                Runtime runtime = Runtime.getRuntime();
                //Print used memory
                System.out.println("Used Memory:"
                    + (runtime.totalMemory() - runtime.freeMemory()) / mb);

                //Print free memory
                System.out.println("Free Memory:"
                    + runtime.freeMemory() / mb);

                //Print total available memory
                System.out.println("Total Memory:" + runtime.totalMemory() / mb);

                //Print Maximum available memory
                System.out.println("Max Memory:" + runtime.maxMemory() / mb);    
                System.out.println(" ");                
                
                if(p.getProperty("kill.sockets").equals("true"))
                {
                    System.out.println("Sockets: " + v_sockets.size());
                    // check for sockets that we can kill
                    curTime = getUnixtime();
                    for(SocketCheck sc : v_sockets)
                    {   
                        toTime = curTime - 120;
                        if(sc.getSocketCreated() < toTime)
                        {
                            if(!sc.getSocket().isClosed())
                            {
                                logger.info("timing out socket... from: " + sc.getHostname());
                                sc.closeSocket();
                                v_sockets.remove(sc);
                            }
                            else
                            {
                                v_sockets.remove(sc);
                            }
                        }
                    }
                }
                
            }
        } catch (Exception ex)
        {
            System.err.println("Something went wrong as fuck: " + ex.getLocalizedMessage());
            logger.fatal("Something went wrong as fuck. exiting: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);            
        }
    }
    
    
    // Establish a connection to the database
    private static boolean connectToDatabase(Properties p)
    {
        try {
            logger.info("Connecting to MySQL");
            conn =
               DriverManager.getConnection("jdbc:mysql://"+p.getProperty("mysql.host")+":"+p.getProperty("mysql.port")+"/"+p.getProperty("mysql.db")+"?" +
                                           "user="+p.getProperty("mysql.user")+"&password="+p.getProperty("mysql.pass")+"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10");

            return(true);

        } catch (Exception ex) {
            // handle any errors
            logger.fatal("Error connecting to database: " + ex.getMessage());
            return(false);
        }
    }    
}
