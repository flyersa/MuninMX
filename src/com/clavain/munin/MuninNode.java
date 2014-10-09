/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */

package com.clavain.munin;

import com.mongodb.BasicDBObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.clavain.muninmxcd.p;
import static com.clavain.muninmxcd.logger;
import static com.clavain.utils.Generic.getUnixtime;
import static com.clavain.muninmxcd.logMore;
import static com.clavain.utils.Database.dbDeleteMissingPlugins;
import static com.clavain.utils.Database.dbUpdateAllPluginsForNode;
import static com.clavain.utils.Database.dbUpdatePluginForNode;
import static com.clavain.utils.Database.dbUpdateLastContact;
import static com.clavain.utils.Generic.isPluginIgnored;
import static com.clavain.utils.Database.dbTrackLogChangedForNode;
import static com.clavain.utils.Database.dbUpdateNodeDistVerKernel;
import static com.clavain.utils.Database.removeOldPackageTrack;
import com.clavain.utils.SocketCheck;
import java.io.ByteArrayInputStream;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author enricokern
 * 
 */
public class MuninNode 
{
  private String    str_nodename;
  private String    str_hostname    = "unset";  
  private int       i_port          = 4949;
  private String    str_group;
  private boolean   b_isRunning     = false;
  private transient long  l_lastFrontendQuery;   
  private String    str_muninVersion = "";
  private transient CopyOnWriteArrayList<MuninPlugin> v_loaded_plugins;
  private transient int       i_GraphCount    = 0;
  private int       i_lastRun        = 0;
  private Integer   node_id          = 0;
  private Integer   user_id          = 0;
  private int       queryInterval    = 0;
  private int       last_plugin_load = 0;
  private String    authpw           = "";
  private boolean   is_init = false;
  private boolean   track_pkg       = false;
  private boolean   essentials      = false;
  private int       last_pkg_update = 0;
  private int       last_essentials_update = 0;
  private transient Socket lastSocket;
  private String    str_via = "unset";

  
    public void setQueryInterval(Integer p_int)
    {
        queryInterval = p_int;
    }
    
    public int getQueryInterval()
    {
        return queryInterval;
    }
  
    public int getGraphCount()
    {
        return i_GraphCount;
    }

    /**
     * @return the str_hostname
     */
    public String getHostname()
    {
        return str_hostname;
    }

    /**
     * @param str_hostname the str_hostname to set
     */
    public void setHostname(String str_hostname)
    {
        this.str_hostname = str_hostname;
    }

    /**
     * @return the i_port
     */
    public int getPort()
    {
        return i_port;
    }

    /**
     * @param i_port the i_port to set
     */
    public void setPort(int i_port)
    {
        this.i_port = i_port;
    }

    /**
     * @return the str_nodename
     */
    public String getNodename()
    {
        return str_nodename;
    }

    /**
     * @param str_nodename the str_nodename to set
     */
    public void setNodename(String str_nodename)
    {
        this.str_nodename = str_nodename;
    }

    /**
     * @return the str_group
     */
    public String getGroup()
    {
        return str_group;
    }

    /**
     * @param str_group the str_group to set
     */
    public void setGroup(String str_group)
    {
        this.str_group = str_group;
    }
    
    /**
     * 
     * @return if the Thread is active or not 
     */
    public boolean isRunning()
    {
        return b_isRunning;
    }
    
    public void setIsRunning(boolean p_isRunning)
    {
        b_isRunning = p_isRunning;
    }

    
    /** 
     * will retrieve a list of loaded munin plugins for this host. If the node was never
     * contacted before the list is downloaded from munin-node
     * 
     * @return list of plugins
     */
    public CopyOnWriteArrayList<MuninPlugin> getPluginList()
    {
        if(getLoadedPlugins() == null)
        {
            // using logger from main class because of gson throwing stackoverflow otherwise
            logger.info("Empty Pluginlist for " + this.getHostname() + ". Loading from munin-node...");
            loadPlugins();
        }
        return getLoadedPlugins();
    }
    

    /**
     * Will load the plugin list from munin-node
     */
    public boolean loadPlugins()
    {
        setLoadedPlugins(new CopyOnWriteArrayList<MuninPlugin>());
        String l_lastProceeded = "";
        
        
        try 
        {
            Socket cs = new Socket();
            cs.setKeepAlive(false);
            cs.setSoLinger(true, 0);
            cs.setReuseAddress(true);  
            cs.setSoTimeout(com.clavain.muninmxcd.socketTimeout);
            if(!str_via.equals("unset"))
            {
                cs.connect(new InetSocketAddress(this.getStr_via(), this.getPort()),com.clavain.muninmxcd.socketTimeout);    
            }
            else
            {
                cs.connect(new InetSocketAddress(this.getHostname(), this.getPort()),com.clavain.muninmxcd.socketTimeout);
            }
          
            if(p.getProperty("kill.sockets").equals("true"))
            {
                SocketCheck sc = new SocketCheck(cs,getUnixtime());
                sc.setHostname(this.getHostname());
                com.clavain.muninmxcd.v_sockets.add(sc);
            }
            PrintStream os = new PrintStream( cs.getOutputStream() );
            BufferedReader in = new BufferedReader(new InputStreamReader( cs.getInputStream()) );  
            
            String s = in.readLine();

            if(s != null)
            {
                // Set version
                os.println("version");
                Thread.sleep(150);
                s = in.readLine();
                
                String version = s.substring(s.indexOf(":")+1,s.length()).trim();
                this.str_muninVersion = version;
                
                if(authpw != null)
                {
                    // if authpw is set, verify
                    if(!authpw.trim().equals(""))
                    {
                        os.println("config muninmxauth");
                        Thread.sleep(150);
                        String apw = in.readLine();
                        s = in.readLine();
                        if(!apw.trim().equals(this.getAuthpw()))
                        {
                            logger.error("Invalid muninmxauth password for host: " + this.getHostname());
                            cs.close();
                            return false;
                        }
                    }
                }
                // check anyway if muninmxauth plugin is present
                else
                {
                    os.println("config muninmxauth");
                    Thread.sleep(100);
                    String apw = in.readLine();
                    if(!apw.trim().equals("# Unknown service"))
                    {
                        logger.error("no auth password given, but muninmxauth plugin present on " + this.getHostname());
                        cs.close();
                        return false;                        
                    }
                    s = in.readLine();
                }
                
                // get list of available plugins
                if(str_via.equals("unset"))
                {
                    os.println("list");
                }
                else
                {
                    os.println("list " + str_hostname);
                }

                Thread.sleep(250);
                s = in.readLine();
 
                // if response is empty and host is not via, do a list $hostname
                if(s.trim().equals("") && str_via.equals("unset"))
                {
                    logger.info("Plugin Response Empty on " + this.getHostname() + " trying to load with list $hostname");
                    os.println("list " + this.getHostname());
                    Thread.sleep(250);
                    s = in.readLine();
                }
                
                String l_tmp;
                StringTokenizer l_st = new StringTokenizer(s, " ");
                
                // create plugin
                MuninPlugin l_mp = new MuninPlugin();
                // negative support
                ArrayList<String> tmp_negatives = new ArrayList<String>();
                
                while(l_st.hasMoreTokens())
                {
                     
                    String l_strPlugin = l_st.nextToken();
                    
                    // check for track_pkg and muninmx essentials
                    if(l_strPlugin.equals("muninmx_trackpkg"))
                    {
                        this.setTrack_pkg(true);
                        continue;
                    }
                    
                    // got essentials?
                    if(l_strPlugin.equals("muninmx_essentials"))
                    {
                        this.setEssentials(true);
                        continue;
                    }                    
                    
                    if(isPluginIgnored(l_strPlugin.toUpperCase()))
                    {
                        continue;
                    }
                                      
                    l_mp.setPluginName(l_strPlugin);

                    os.println("config " + l_strPlugin);
                    
                    // create graphs for plugin
                    int l_iGraphsFound = 0;  
                    int l_iTmp         = 0;
                    MuninGraph  l_mg = new MuninGraph();                    
                    l_mg.setQueryInterval(this.getQueryInterval());
                    while ((l_tmp = in.readLine()) != null) 
                    {
                      if(l_tmp.startsWith("."))
                      {
                        break;
                      }
                      // collect graphs only for plugin
                      String l_strName;
                      String l_strType;
                      String l_strValue;

                      
                      if(!l_tmp.contains("graph_") && !l_tmp.trim().equals("") && !l_tmp.contains("host_name") && !l_tmp.contains("multigraph") && !l_tmp.trim().equals("graph no") && !l_tmp.trim().equals("# Bad exit") && !l_tmp.trim().contains("info Currently our peer") && !l_tmp.trim().startsWith("#") && !l_tmp.trim().contains("Bonding interface errors"))
                      {
                        l_lastProceeded = l_tmp;
                        l_strName  = l_tmp.substring(0,l_tmp.indexOf("."));
                        l_strType  = l_tmp.substring(l_tmp.indexOf(".")+1,l_tmp.indexOf(" "));
                        l_strValue = l_tmp.substring(l_tmp.indexOf(" ")+1,l_tmp.length());
                        //System.err.println("Name: " + l_strName + " Type: " + l_strType + " Value: " + l_strValue);
                        
                        if(l_strType.equals("label"))
                        {
                           l_iTmp++;
                          
                           if(l_iTmp > 1)
                           {
                               l_mp.addGraph(l_mg);
                               l_mg = new MuninGraph();
                               l_mg.setQueryInterval(this.getQueryInterval());
                           }
                           l_mg.setGraphName(l_strName);
                           l_mg.setGraphLabel(l_strValue);
                        }
                        else if(l_strType.equals("draw"))
                        {
                            l_mg.setGraphDraw(l_strValue);
                        }
                        else if(l_strType.equals("type"))
                        {
                            l_mg.setGraphType(l_strValue);
                        }
                        else if(l_strType.equals("info"))
                        {
                            l_mg.setGraphInfo(l_strValue);
                        }
                        else if(l_strType.equals("negative"))
                        {
                            // add to temporary negative list to set negatives later
                            tmp_negatives.add(l_strValue);
                        }

                        //System.out.println(l_strName); 
                        //System.out.println(l_strType);
                        //System.out.println(l_strValue);
                      }
                      else
                      {
                          // set plugin title
                          if(l_tmp.contains("graph_title"))
                          {
                              l_mp.setPluginTitle(l_tmp.substring(12,l_tmp.length()));
                          }
                          // set plugin info, if any
                          if(l_tmp.contains("graph_info"))
                          {
                              l_mp.setPluginInfo(l_tmp.substring(11,l_tmp.length()));
                          }
                          // set graph category
                          if(l_tmp.contains("graph_category"))
                          {
                              l_mp.setPluginCategory(l_tmp.substring(15,l_tmp.length()));
                          }
                          // set graph vlabel
                          if(l_tmp.contains("graph_vlabel"))
                          {
                              l_mp.setPluginLabel(l_tmp.substring(13,l_tmp.length()));
                          }
                      }
                      
                    }
                  
                    
                    // add to pluginlist
                    l_mp.addGraph(l_mg);
                    
                   Iterator it = l_mp.getGraphs().iterator();
                   while(it.hasNext())
                   {
                       MuninGraph l_mpNg = (MuninGraph) it.next();
                       if(tmp_negatives.contains(l_mpNg.getGraphName()))
                       {
                           l_mpNg.setNegative(true);
                       }
                   }
                    
                    // add plugin if it got valid graphs and add nodeid (req. for alerts)
                    if(l_mp.getGraphs().size() > 0)
                    {
                        l_mp.set_NodeId(this.getNode_id());
                        getLoadedPlugins().add(l_mp);
                    }
                    // flush temporary negatives
                    tmp_negatives.clear();
                    l_mp = null;
                    l_mp = new MuninPlugin();
                    //String l_strGraphTitle = s.substring(s.indexOf("graph_title") + 11,s.length());
                    //System.out.println(" - " + l_strGraphTitle);
                }
                cs.close();
                in.close();
                os.close();
                last_plugin_load = getUnixtime();
                //System.out.println(s);
            }
            else
            {
                cs.close();
                in.close();
                os.close();
                logger.warn("Error loading plugins on " + str_hostname + " ("+this.getNode_id()+"). Check connectivity or munin-node");
            }
            /*
            for (MuninPlugin l_mn : getLoadedPlugins()) {
                i_GraphCount = i_GraphCount + l_mn.getGraphs().size();
                logger.debug(l_mn.getGraphs().size() + " graphs found for plugin: " + l_mn.getPluginName().toUpperCase() + " on node: " + this.getNodename());
            }*/
        } catch (Exception ex) {
            logger.error("Error loading plugins on " + str_hostname + " ("+this.getNode_id()+") : " + ex.getMessage());
            ex.printStackTrace();
            return false;
        } 

        return true;
    }
            

    public void run() {
        b_isRunning = true;

        if(this.str_via.equals("unset"))
        {
            logger.info(getHostname() + " Monitoring job started");
        }
        else
        {
            logger.info(getHostname() + " (VIA: "+this.str_via+") Monitoring job started");
        }
        
        int iCurTime = getUnixtime();
        int iPluginRefreshTime = last_plugin_load + Integer.parseInt(p.getProperty("plugin.refreshtime"));
        try {
            // update plugins, maybe we have some new :)
            // double try to load plugins if fail
       
            if(getPluginList().size() > 0)
            {
                if(!is_init)
                {
                    logger.info("[Job: " + getHostname() + "] Updating Database");
                    // update graphs in database too
                    for(MuninPlugin it_pl : getPluginList()) {
                        if(it_pl.getGraphs().size() > 0)
                        {
                            //logger.info(it_pl.getPluginName());
                            dbUpdatePluginForNode(getNode_id(),it_pl);
                        }
                     }
                    // delete now missing plugins
                    dbDeleteMissingPlugins(getNode_id(),getPluginList());
                    logger.info("[Job: " + getHostname() + "] Databaseupdate Done");
                    is_init = true;
                }
                else
                {
                    if(iCurTime > iPluginRefreshTime )
                    {
                        logger.info("Refreshing Plugins on " + this.getHostname());
                        this.loadPlugins();
                        dbUpdateAllPluginsForNode(this);
                    }
                }
            }
            else
            {
                this.loadPlugins();
            }
                  
            
            Socket clientSocket = new Socket();
            clientSocket.setSoTimeout(com.clavain.muninmxcd.socketTimeout);
            clientSocket.setKeepAlive(false);
            clientSocket.setReuseAddress(true);   
            if(this.str_via.equals("unset"))
            {            
                clientSocket.connect(new InetSocketAddress(this.getHostname(), this.getPort()),com.clavain.muninmxcd.socketTimeout);
            }
            else
            {
                clientSocket.connect(new InetSocketAddress(this.getStr_via(), this.getPort()),com.clavain.muninmxcd.socketTimeout);
            }
            lastSocket = clientSocket;
            SocketCheck sc = new SocketCheck(clientSocket,getUnixtime());
            if(p.getProperty("kill.sockets").equals("true"))
            {  
                sc.setHostname(this.getHostname());
                com.clavain.muninmxcd.v_sockets.add(sc);
            }
            this.i_lastRun = getUnixtime();

            // track packages?
            if(this.track_pkg)
            {
                updateTrackPackages(clientSocket);
            }
            
            // gather essentials?
            if(this.essentials)
            {
                updateEssentials(clientSocket);
            }
            
            // update graphs for all plugins
            Iterator it = this.getLoadedPlugins().iterator();
            while(it.hasNext())
            {
                MuninPlugin l_mp = (MuninPlugin) it.next();
                if(logMore)
                {
                    logger.info(getHostname() + " fetching graphs for " + l_mp.getPluginName().toUpperCase());
                }
                // snmp support
                if(!str_via.equals("unset"))
                { 
                    l_mp.updateAllGraps(this.getStr_via(), this.getPort(), clientSocket, getQueryInterval());
                }  
                else
                {
                    l_mp.updateAllGraps(this.getHostname(), this.getPort(), clientSocket, getQueryInterval());
                }
                // add all graphs to insertion queue for mongodb
                queuePluginFetch(l_mp.returnAllGraphs(), l_mp.getPluginName());
            }
            clientSocket.close();
            
            if(p.getProperty("kill.sockets").equals("true"))
            {
                com.clavain.muninmxcd.v_sockets.remove(sc);
            }
            sc = null;
        } catch (Exception ex) {
           logger.fatal("Error in thread for host: " + getHostname() + " : " + ex.getLocalizedMessage());
           ex.printStackTrace();
        }
        int iRunTime = getUnixtime() - iCurTime;
        dbUpdateLastContact(this.getNode_id());
        logger.info(getHostname() + " Monitoring job stopped - runtime: " + iRunTime);
        
    }

    
    /*
     * update essentials
     */
    private void updateEssentials(Socket p_socket)
    {
        String decodestr = "";
        try {
            PrintStream os = new PrintStream( p_socket.getOutputStream() );
            BufferedReader in = new BufferedReader(new InputStreamReader( p_socket.getInputStream()) );
            os.println("config muninmx_essentials");
            
            // skip first line if starts with #
            decodestr = in.readLine();
            if(decodestr.startsWith("#"))
            {
                decodestr = in.readLine();    
            }

            if(decodestr.equals("."))
            {
                decodestr = in.readLine();    
            }             
            BasicDBObject doc = new BasicDBObject();
            doc.put("data", decodestr);
            doc.put("time",getUnixtime());
            doc.put("node", this.node_id);
            doc.put("type","essential");
            com.clavain.muninmxcd.mongo_essential_queue.add(doc);  
            logger.info("Essentials Updated for Node: " + this.getHostname() + ": received base64 (length): " + decodestr.length());
            last_essentials_update = getUnixtime();
            // read following .
            in.readLine();
        } catch (Exception ex)
        {
            logger.error("Error in updateEssentials for Node " + this.getHostname() + " : " + ex.getLocalizedMessage());
            logger.error("updateEssentials for Node " + this.getHostname() + " received: " + decodestr);
            ex.printStackTrace();     
        }
    }
    
    /*
     * update track package log
     */
    private void updateTrackPackages(Socket p_socket)
    {
        // only try to update this once per hour
        int curTime = getUnixtime();
        int lalert = this.last_pkg_update + 3600;
        if(lalert > curTime )
        {  
            return;
        } 
        
        String decodestr = "";
        try 
        {
            logger.info("TrackPackages - fetching " + this.str_hostname);
            PrintStream os = new PrintStream( p_socket.getOutputStream() );
            BufferedReader in = new BufferedReader(new InputStreamReader( p_socket.getInputStream()) );
            os.println("config muninmx_trackpkg");
            // skip first line if starts with #
            decodestr = in.readLine();
            if(decodestr.startsWith("#"))
            {
                decodestr = in.readLine();    
            }

            if(decodestr.equals("."))
            {
                decodestr = in.readLine();    
            }   
            byte[] decode = Base64.decodeBase64(decodestr);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decode); 
            GZIPInputStream gzipInputStream;
            gzipInputStream = new GZIPInputStream(byteArrayInputStream);

            InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader, 4);

            String read;
            String sum = bufferedReader.readLine();
            if(sum == null) 
            {  
                logger.error("TrackPackages - sum is null for: " + this.str_hostname);
                return; 
            }  
            String dist = bufferedReader.readLine();
            String ver  = bufferedReader.readLine();
            String kernel = bufferedReader.readLine();
            if(dbTrackLogChangedForNode(sum, this.node_id))
            {
                logger.info("TrackPackages - packages changed, updating " + this.str_hostname);
                // purge old logs
                removeOldPackageTrack(this.node_id);
                
                dbUpdateNodeDistVerKernel(sum,dist, ver, kernel, this.node_id);      
                int i = 0;
                while ((read = bufferedReader.readLine()) != null) 
                {
                     BasicDBObject doc = new BasicDBObject();
                     doc.put("package", read);
                     doc.put("time",getUnixtime());
                     doc.put("node", this.node_id);
                     doc.put("type","trackpkg");
                     com.clavain.muninmxcd.mongo_essential_queue.add(doc);
                     i++;
                } 
                logger.info("TrackPackages Updated for Node: " + this.getHostname() + " ("+dist+" " + ver + " " + kernel + "). tracking " + i + " packages"); 
            }
            else
            {
                logger.info("TrackPackages - sum not changed since last run for Node: " + this.getHostname());
            }
            this.last_pkg_update = getUnixtime();

        } catch (Exception ex)
        {
            logger.error("Error in updateTrackPackages for Node " + this.getHostname() + " : " + ex.getLocalizedMessage());
            logger.error("updateTrackPackages for Node " + this.getHostname() + " received: " + decodestr);
            ex.printStackTrace();
        }
            
    }
    
    /**
     * fill insertion queue with current graph values for each plugin
     */
    public void queuePluginFetch(ArrayList<MuninGraph> p_almg, String p_strPluginName)
    {
        Iterator<MuninGraph> it = p_almg.iterator();
        while(it.hasNext())
        {
            MuninGraph mg = it.next();
            // prepare document object
            BasicDBObject doc = new BasicDBObject();
            doc.put("hostname", this.getHostname());
            doc.put("plugin", p_strPluginName);
            doc.put("graph", mg.getGraphName());
            doc.put("value", mg.getGraphValue().toString());
            doc.put("recv", mg.getLastGraphTime());
            doc.put("user_id", this.getUser_id());
            doc.put("nodeid", this.getNode_id());
            
            // only queue if plugin is initialized or it is a if_err plugin
            if(mg.isInit() || p_strPluginName.startsWith("if_err") || p_strPluginName.equals("swap"))
            {
                com.clavain.muninmxcd.mongo_queue.add(doc);
                mg.setLastQueued(getUnixtime());
                
                logger.debug("Queued: " + this.getHostname() + " (" + p_strPluginName + " / " + mg.getGraphName() + ") Value: " + mg.getGraphValue());
                if(logMore)
                {
                    logger.info("Queued: " + this.getHostname() + " (" + p_strPluginName + " / " + mg.getGraphName() + ") Value: " + mg.getGraphValue());   
                }                
            }

        }
    }
    
    /**
     * @return the v_loaded_plugins
     */
    public CopyOnWriteArrayList<MuninPlugin> getLoadedPlugins() {
        return v_loaded_plugins;
    }

    /**
     * @param v_loaded_plugins the v_loaded_plugins to set
     */
    public void setLoadedPlugins(CopyOnWriteArrayList<MuninPlugin> v_loaded_plugins) {
        this.v_loaded_plugins = v_loaded_plugins;
    }

    /**
     * @return the l_lastFrontendQuery
     */
    public long getLastFrontendQuery() {
        return l_lastFrontendQuery;
    }

    /**
     * @param l_lastFrontendQuery the l_lastFrontendQuery to set
     */
    public void setLastFrontendQuery(long l_lastFrontendQuery) {
        this.l_lastFrontendQuery = l_lastFrontendQuery;
    }
    
    /**
     * Sets lastFrontendQuery to current unixtime
     */
    public void setLastFrontendQuery()
    {
        l_lastFrontendQuery = System.currentTimeMillis() / 1000L;
    }

    private void cleanUpGraphs() 
    {
        Iterator it = this.getLoadedPlugins().iterator();
        while(it.hasNext())
        {
            MuninPlugin l_mp = (MuninPlugin) it.next();
            Iterator itg = l_mp.getGraphs().iterator();
            while(itg.hasNext())
            {
                MuninGraph l_mg = (MuninGraph) itg.next();
                l_mg.setGraphValue("0");
                l_mg.setLastGraphValue("0");
                l_mg.setLastGraphValueCounter("0");
            }
        }
    }

    /**
     * @return the node_id
     */
    public Integer getNode_id() {
        return node_id;
    }

    /**
     * @param node_id the node_id to set
     */
    public void setNode_id(Integer node_id) {
        this.node_id = node_id;
    }

    /**
     * @return the user_id
     */
    public Integer getUser_id() {
        return user_id;
    }

    /**
     * @param user_id the user_id to set
     */
    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    /**
     * @return the lastSocket
     */
    public Socket getLastSocket() {
        return lastSocket;
    }

    /**
     * @return the str_via
     */
    public String getStr_via() {
        return str_via;
    }

    /**
     * @param str_via the str_via to set
     */
    public void setStr_via(String str_via) {
        this.str_via = str_via;
    }

    /**
     * @return the authpw
     */
    public String getAuthpw() {
        return authpw;
    }

    /**
     * @param authpw the authpw to set
     */
    public void setAuthpw(String authpw) {
        this.authpw = authpw;
    }

    /**
     * @return the track_pkg
     */
    public boolean isTrack_pkg() {
        return track_pkg;
    }

    /**
     * @param track_pkg the track_pkg to set
     */
    public void setTrack_pkg(boolean track_pkg) {
        this.track_pkg = track_pkg;
    }

    /**
     * @return the essentials
     */
    public boolean isEssentials() {
        return essentials;
    }

    /**
     * @param essentials the essentials to set
     */
    public void setEssentials(boolean essentials) {
        this.essentials = essentials;
    }
}
