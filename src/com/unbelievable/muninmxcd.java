package com.unbelievable;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.unbelievable.munin.MuninNode;
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
    public static ArrayList<MuninNode> v_munin_nodes;
    
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
            builder.connectionsPerHost(100);
            builder.autoConnectRetry(true);
            builder.threadsAllowedToBlockForConnectionMultiplier(10);
            // speed up inserts, we dont care if we miss some if the shit hits the fan
            builder.writeConcern(WriteConcern.NONE);
            m = new MongoClient( new ServerAddress(p.getProperty("mongo.host")),  builder.build());
 
                        
            // connect to mysql
            connectToDatabase(p);
            
            // PreFilling Nodes, max 100 in concurrent
            logger.info("Loading initial MuninNode details. This can take a few minutes...");
            v_munin_nodes = new ArrayList<>();
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
                v_munin_nodes.add(mn);              
                logger.info("* " + mn.getHostname() + " queued for pluginfetch");
            }

            ExecutorService executor = Executors.newFixedThreadPool(100);
            for (MuninNode it_mn : v_munin_nodes) {  
                    executor.execute(new PluginUpdater(it_mn));
            }   
            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(100);
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
