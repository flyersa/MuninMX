package com.unbelievable.munin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import static com.unbelievable.muninmxcd.p;
import static com.unbelievable.muninmxcd.logger;
import static com.unbelievable.utils.Generic.getUnixtime;
import static com.unbelievable.muninmxcd.logMore;
/**
 *
 * @author enricokern
 */
public class MuninPlugin {
    private String str_PluginName;
    private String str_PluginTitle;
    private String str_PluginInfo;
    private String str_PluginCategory;
    private String str_PluginLabel;
    private long   l_lastFrontendQuery;
    private long   l_lastMuninQuery;
    private transient Socket csMuninSocket;
    
    private ArrayList<MuninGraph> v_graphs = new ArrayList<MuninGraph>();;

    
    public void addGraph(MuninGraph p_graph)
    {
        getGraphs().add(p_graph);
    }
    
    /**
     * @return the str_PluginName
     */
    public String getPluginName() {
        return str_PluginName;
    }

    /**
     * @param str_PluginName the str_PluginName to set
     */
    public void setPluginName(String str_PluginName) {
        this.str_PluginName = str_PluginName;
    }

    /**
     * @return the str_GraphTitle
     */
    public String getPluginTitle() {
        return str_PluginTitle;
    }

    /**
     * @param str_GraphTitle the str_GraphTitle to set
     */
    public void setPluginTitle(String str_GraphTitle) {
        this.str_PluginTitle = str_GraphTitle;
    }

    /**
     * @return the str_GraphInfo
     */
    public String getPluginInfo() {
        return str_PluginInfo;
    }

    /**
     * @param str_PluginInfo the str_GraphInfo to set
     */
    public void setPluginInfo(String str_GraphInfo) {
        this.str_PluginInfo = str_GraphInfo;
    }

    /**
     * @return the v_graphs
     */
    public ArrayList<MuninGraph> getGraphs() {
        return v_graphs;
    }

    /**
     * @param v_graphs the v_graphs to set
     */
    public void setGraphs(ArrayList<MuninGraph> v_graphs) {
        this.v_graphs = v_graphs;
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
    
    
    /*
     * will connect to munin if no connections exists and will update
     * values for all graphs, then return all graphs with updated stats
     */
    public void updateAllGraps(String p_strHostname, int p_iPort, Socket p_socket, int p_queryInterval)
    {
        try
        {
            csMuninSocket = p_socket;
            // connection available?
            if(csMuninSocket != null)
            {
                if(!csMuninSocket.isConnected() || csMuninSocket.isClosed())
                {
                    csMuninSocket = null;
                    csMuninSocket = new Socket();
                    csMuninSocket.connect(new InetSocketAddress(p_strHostname, p_iPort),200);
                    logger.info("Reconnecting to " + p_strHostname);
                }
            }
            else
            {
                csMuninSocket = new Socket(p_strHostname, p_iPort);
               
            }
            PrintStream os = new PrintStream( csMuninSocket.getOutputStream() );
            BufferedReader in = new BufferedReader(new InputStreamReader( csMuninSocket.getInputStream()) );
            os.println("fetch " + this.getPluginName());
            String line;
            while((line = in.readLine()) != null) {
                if(line.startsWith("."))
                {
                    return;
                }
                //System.out.println(line);
                if(line.contains("value") && !line.contains("#"))
                {
                    String l_graphName = line.substring(0,line.indexOf("."));
                    String l_value      = line.substring(line.indexOf(" ")+1,line.length());
                    if(logMore)
                    {
                        logger.info(p_strHostname + " - " + l_graphName + " - " + l_value);
                    }
                    Iterator it = this.v_graphs.iterator();
                    while (it.hasNext())
                    {
                        MuninGraph l_mg = (MuninGraph) it.next();
                        l_mg.setQueryInterval(p_queryInterval);
                        if(l_mg.getGraphName().equals(l_graphName))
                        {                  
                            if(l_value.trim().length() < 1)
                            {
                                l_mg.setGraphValue("0");
                            }
                            else
                            {
                                // check....
                                try
                                {
                                    l_mg.setGraphValue(l_value.trim());
                                } catch (Exception ex)
                                {
                                    l_mg.setGraphValue("U");
                                    System.err.println(p_strHostname + " setvalue error on "+this.str_PluginName+" with " + l_value + " details: " + ex.getLocalizedMessage());
                                    ex.printStackTrace();
                                }
                            }
                        }
                        
                    }
                }
            }
            //os.close();
            //in.close();
            //csMuninSocket.close();
            //csMuninSocket = null;

        } catch (Exception ex)
        {
            //try { csMuninSocket.close(); } catch (Exception e) {};
            logger.error(p_strHostname + " Unable to connect/process: " + ex.getMessage());
            ex.printStackTrace();
        }
        
    }
    
    public ArrayList<MuninGraph> returnAllGraphs()
    {
        return v_graphs;   
    }

    void setPluginCategory(String p_strCategory) {
        setStr_PluginCategory(p_strCategory);
    }

    /**
     * @return the str_PluginLabel
     */
    public String getPluginLabel() {
        return str_PluginLabel;
    }

    /**
     * @param str_PluginLabel the str_PluginLabel to set
     */
    public void setPluginLabel(String str_PluginLabel) {
        this.str_PluginLabel = str_PluginLabel;
    }

    /**
     * @return the str_PluginCategory
     */
    public String getStr_PluginCategory() {
        return str_PluginCategory;
    }

    /**
     * @param str_PluginCategory the str_PluginCategory to set
     */
    public void setStr_PluginCategory(String str_PluginCategory) {
        this.str_PluginCategory = str_PluginCategory;
    }
}
