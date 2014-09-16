/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain;

import com.clavain.alerts.Alert;
import com.clavain.alerts.msg.PushOverMessage;
import com.clavain.alerts.msg.ShortTextMessage;
import com.clavain.alerts.msg.TTSMessage;
import com.clavain.alerts.ratelimiter.PushOverLimiter;
import com.clavain.alerts.ratelimiter.SMSLimiter;
import com.clavain.alerts.ratelimiter.TTSLimiter;
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
import muninmxlictool.CryptoUtils;
import muninmxlictool.License;
import java.io.File;
import java.io.ObjectInputStream;
/**
 *
 * @author enricokern
 */
public class muninmxcd {
    public static Logger logger     = Logger.getRootLogger();
    public static Properties p      = null;
    public static LinkedBlockingQueue<BasicDBObject> mongo_queue = new LinkedBlockingQueue<BasicDBObject>();
    public static LinkedBlockingQueue<BasicDBObject> mongo_essential_queue = new LinkedBlockingQueue<BasicDBObject>();
    public static boolean logMore   = false;
    public static MongoClient m;
    public static DB db;
    public static DBCollection col;
    public static String version    = "0.1 <Codename: Frog in Blender>";
    public static Connection conn = null;    
    public static CopyOnWriteArrayList<MuninNode> v_munin_nodes;
    public static CopyOnWriteArrayList<MuninPlugin> v_cinterval_plugins;
    public static Scheduler sched;
    public static Scheduler sched_custom;
    public static CopyOnWriteArrayList<SocketCheck> v_sockets;
    // alerting
    public static LinkedBlockingQueue<PushOverMessage> notification_pushover_queue = new LinkedBlockingQueue<PushOverMessage>();
    public static LinkedBlockingQueue<ShortTextMessage> notification_sms_queue = new LinkedBlockingQueue<ShortTextMessage>();
    public static LinkedBlockingQueue<TTSMessage> notification_tts_queue = new LinkedBlockingQueue<TTSMessage>();  
    public static CopyOnWriteArrayList<Alert> v_alerts = new CopyOnWriteArrayList<>();
    // RCA
    public static CopyOnWriteArrayList<Analyzer> v_analyzer = new CopyOnWriteArrayList<>();
    public static int rcajobs_running = 0;
    public static int maxnodes = 100000;
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
            p = new Properties();
            FileInputStream propInFile = null;
            propInFile = new FileInputStream(args[0]);
            p.loadFromXML(propInFile);            
            String logfile = p.getProperty("log.file");

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
           String l1 = "\"E~>l^FNm%";
           String l2 = "4|$18m";
           String logK = l1 + l2;
           File folder = new File("licenses");
           CryptoUtils crypt = new CryptoUtils();
           FileInputStream fin;
           ObjectInputStream ois;
           CopyOnWriteArrayList<License> v_lics = new CopyOnWriteArrayList<>();
           
           if(!new File("licenses/muninmx.license").exists())
           {
               System.err.println("licenses/muninmx.license missing");
               logger.fatal("licenses/muninmx.license missing");
               System.exit(1);
           }
           else
           {
            crypt.decrypt(logK, new File("licenses/muninmx.license"), new File("licenses/muninmx.license.tmp"));
            fin = new FileInputStream(new File("licenses/muninmx.license.tmp"));
            ois = new ObjectInputStream(fin);
            License lic;
            lic = (License) ois.readObject();
            new File("licenses/muninmx.license.tmp").delete();
            if(lic.getLicenseType().equals("demo"))
            {
                maxnodes =  lic.getNum_nodes();
                logger.info("---- DEMO MODUS -----");
                // expired?
                if(lic.getValid() < getUnixtime())
                {
                    logger.fatal("License Expired");
                    System.exit(1);
                }
            } 
            else
            {
                if(!lic.getLicenseType().equals("basic"))
                {
                    System.err.println("Invalid MuninMX License File");
                    logger.fatal("Invalid MuninMX License File");
                    System.exit(1);
                }
                
                logger.info("Read Master License - LicenseID: " + lic.getLicenseID() + " Valid To: " + lic.getValid() + " Num Nodes: " + lic.getNum_nodes() + " License Type: " + lic.getLicenseType());
                maxnodes = lic.getNum_nodes();
                
                // read additional licenses
                License alic;
                for (final File fileEntry : folder.listFiles()) {
                     if (!fileEntry.isDirectory()) {   
                         if(fileEntry.getName().endsWith(".license") && !fileEntry.getName().equals("muninmx.license"))
                         {
                            crypt.decrypt(logK, fileEntry, new File(fileEntry.getName()+".de"));
                            fin = new FileInputStream(fileEntry.getName()+".de");
                            ois = new ObjectInputStream(fin);
                            alic = (License) ois.readObject();
                            new File(fileEntry.getName()+".de").delete();
                            if(alic.getLicenseType().equals("basic"))
                            {
                                logger.info("Found another master license... ignoring " + fileEntry.getName());
                            }
                            else
                            {
                                if(alic.getLicenseID().equals(lic.getLicenseID()))
                                {
                                    logger.info("Read Additional License - Matching LicenseID: " + alic.getLicenseID() + " Num Nodes: " + alic.getNum_nodes() + " License Type: " + alic.getLicenseType());
                                    maxnodes = maxnodes + alic.getNum_nodes();
                                }
                                else
                                {
                                    logger.info("Additional License " + fileEntry.getName() + " does not match LicenseID. Ignoring");
                                }
                            }
                         }
                     } 
                }
                
            }
           
           }
           
           
       } catch (Exception ex)
       {
           ex.printStackTrace();
           logger.fatal("License Error: " + ex.getLocalizedMessage());
           System.exit(-1);
       }
       
       logger.info("Licensed Nodecount: " + maxnodes);
       
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
                    logger.info("Waiting 20s for new scheduling slot");
                }
                scheduleJob(it_mn);
                i++;
            }
            
            // schedule custom interval jobs
            dbScheduleAllCustomJobs();
            
            // add all alerts
            dbAddAllAlerts();
            
            // starting MongoExecutor
            new Thread(new MongoExecutor()).start();
        
            // starting MongoExecutor for Package Tracking and Essential Informations
            new Thread(new MongoEssentialExecutor()).start();            
            
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
            
            int curTime;
            int toTime;
            while(true)
            {
                Thread.sleep(5000);
                System.out.println("Mongo Queue Size: " + mongo_queue.size());
                
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
