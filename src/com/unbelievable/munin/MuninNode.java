/*
 */
package com.unbelievable.munin;

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
import static com.unbelievable.muninmxcd.p;
import static com.unbelievable.muninmxcd.logger;
import static com.unbelievable.utils.Generic.getUnixtime;
import static com.unbelievable.muninmxcd.logMore;
import static com.unbelievable.utils.Database.dbDeleteMissingPlugins;
import static com.unbelievable.utils.Database.dbUpdatePluginForNode;
import static com.unbelievable.utils.Generic.isPluginIgnored;
import com.unbelievable.utils.SocketCheck;
import java.util.concurrent.CopyOnWriteArrayList;

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
  private long      l_lastFrontendQuery;   
  private String    str_muninVersion = "";
  private transient CopyOnWriteArrayList<MuninPlugin> v_loaded_plugins;
  private int       i_GraphCount    = 0;
  private int       i_lastRun        = 0;
  private Integer   node_id          = 0;
  private Integer   user_id          = 0;
  private int       queryInterval    = 0;
  private int       last_plugin_load = 0;
  private boolean   is_init = false;
  private transient Socket lastSocket;
  
  
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
            cs.setReuseAddress(false);  
            cs.setSoTimeout(30000);
            cs.connect(new InetSocketAddress(this.getHostname(), this.getPort()),30000);
            
          
            if(p.getProperty("kill.sockets").equals("true"))
            {
                SocketCheck sc = new SocketCheck(cs,getUnixtime());
                sc.setHostname(this.getHostname());
                com.unbelievable.muninmxcd.v_sockets.add(sc);
            }
            PrintStream os = new PrintStream( cs.getOutputStream() );
            BufferedReader in = new BufferedReader(new InputStreamReader( cs.getInputStream()) );  
            
            String s = in.readLine();

            if(s != null)
            {
                // Set version
                os.println("version");
                Thread.sleep(250);
                s = in.readLine();
                
                String version = s.substring(s.indexOf(":")+1,s.length()).trim();
                this.str_muninVersion = version;
                
                // get list of available plugins
                os.println("list");

                Thread.sleep(250);
                s = in.readLine();
 
                
                String l_tmp;
                StringTokenizer l_st = new StringTokenizer(s, " ");
                
                // create plugin
                MuninPlugin l_mp = new MuninPlugin();
                
                while(l_st.hasMoreTokens())
                {
                     
                    String l_strPlugin = l_st.nextToken();
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

                      
                      if(!l_tmp.contains("graph_") && !l_tmp.contains("multigraph") && !l_tmp.trim().equals("graph no") && !l_tmp.trim().equals("# Bad exit") && !l_tmp.trim().contains("info Currently our peer") && !l_tmp.trim().startsWith("#") && !l_tmp.trim().contains("Bonding interface errors"))
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
                    
                    // add plugin if it got valid graphs
                    if(l_mp.getGraphs().size() > 0)
                    {
                        getLoadedPlugins().add(l_mp);
                    }
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

        logger.info(getHostname() + " Monitoring job started");
        
        
        int iCurTime = getUnixtime();
        int iPluginRefreshTime = last_plugin_load + 86400;
        try {
            // update plugins, maybe we have some new :)
            // double try to load plugins if fail
       
            if(getPluginList().size() > 1)
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
                    }
                }
            }
            else
            {
                this.loadPlugins();
            }
                  
            
            Socket clientSocket = new Socket();
            clientSocket.setSoTimeout(30000);
            clientSocket.setKeepAlive(false);
            clientSocket.setReuseAddress(false);            
            clientSocket.connect(new InetSocketAddress(this.getHostname(), this.getPort()),30000);

            lastSocket = clientSocket;
            SocketCheck sc = new SocketCheck(clientSocket,getUnixtime());
            if(p.getProperty("kill.sockets").equals("true"))
            {  
                sc.setHostname(this.getHostname());
                com.unbelievable.muninmxcd.v_sockets.add(sc);
            }
            this.i_lastRun = getUnixtime();

            // update graphs for all plugins
            Iterator it = this.getLoadedPlugins().iterator();
            while(it.hasNext())
            {
                MuninPlugin l_mp = (MuninPlugin) it.next();
                if(logMore)
                {
                    logger.info(getHostname() + " fetching graphs for " + l_mp.getPluginName().toUpperCase());
                }
                l_mp.updateAllGraps(this.getHostname(), this.getPort(), clientSocket, getQueryInterval());
                // add all graphs to insertion queue for mongodb
                queuePluginFetch(l_mp.returnAllGraphs(), l_mp.getPluginName());
            }
            clientSocket.close();
            
            if(p.getProperty("kill.sockets").equals("true"))
            {
                com.unbelievable.muninmxcd.v_sockets.remove(sc);
            }
            sc = null;
        } catch (Exception ex) {
           logger.fatal("Error in thread for host: " + getHostname() + " : " + ex.getLocalizedMessage());
           ex.printStackTrace();
        }
        int iRunTime = getUnixtime() - iCurTime;
        logger.info(getHostname() + " Monitoring job stopped - runtime: " + iRunTime);
        
    }

    
    /**
     * fill insertion queue with current graph values for each plugin
     */
    private void queuePluginFetch(ArrayList<MuninGraph> p_almg, String p_strPluginName)
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
                com.unbelievable.muninmxcd.mongo_queue.add(doc);
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
}
