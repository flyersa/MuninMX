package com.unbelievable;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.unbelievable.executors.MongoExecutor;
import com.unbelievable.handlers.JettyLauncher;
import com.unbelievable.munin.MuninNode;
import com.unbelievable.munin.MuninPlugin;
import com.unbelievable.workers.PluginUpdater;
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
import static com.unbelievable.utils.Database.*;
import static com.unbelievable.utils.Quartz.*;
import com.unbelievable.utils.SocketCheck;
import com.unbelievable.workers.MongoWorker;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import static com.unbelievable.utils.Generic.getUnixtime;
/**
 *
 * @author enricokern
 */
public class muninmxcd {
    public static Logger logger     = Logger.getRootLogger();
    public static Properties p      = null;
    public static LinkedBlockingQueue<BasicDBObject> mongo_queue = new LinkedBlockingQueue<BasicDBObject>();
    public static boolean logMore   = false;
    public static MongoClient m;
    public static DB db;
    public static DBCollection col;
    public static String version    = "0.1 <Codename: Frog in Blender>";
    public static Connection conn = null;    
    public static CopyOnWriteArrayList<MuninNode> v_munin_nodes;
    public static Scheduler sched;
    public static CopyOnWriteArrayList<SocketCheck> v_sockets;
    
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
                v_munin_nodes.add(mn);              
                logger.info("* " + mn.getHostname() + " queued for pluginfetch");
            }
            
            /*
            ExecutorService executor = Executors.newFixedThreadPool(250);
            
            for (MuninNode it_mn : v_munin_nodes) {  
                    executor.execute(new PluginUpdater(it_mn));
            }   
            logger.info("Shuting down Updater Executor");
            executor.shutdown();
            
            while (!executor.isTerminated()) {
                Thread.sleep(100);
            }
            logger.info("Debug Mode. Exiting");
            System.exit(0);      
            /*
             * 
             */
            // launching quartz scheduler
            logger.info("Launching Scheduler");
            SchedulerFactory sf = new StdSchedulerFactory("quartz.properties");
            sched = sf.getScheduler();   
            sched.start();   
            
                        // starting API server
            new Thread(new JettyLauncher()).start();
            
            // scheduling jobs
            int i = 0;
            for (MuninNode it_mn : v_munin_nodes) {
                if(i == 200)
                {
                    Thread.sleep(15000);
                    i = 0;
                    logger.info("Waiting 15s for new scheduling slot");
                }
                scheduleJob(it_mn);
                i++;
            }
            
            // starting MongoExecutor
            new Thread(new MongoExecutor()).start();
            

            
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
                                           "user="+p.getProperty("mysql.user")+"&password="+p.getProperty("mysql.pass")+"");

            return(true);

        } catch (Exception ex) {
            // handle any errors
            logger.fatal("Error connecting to database: " + ex.getMessage());
            return(false);
        }
    }    
}
