/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.munin;

import com.clavain.alerts.Alert;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import static com.clavain.muninmxcd.p;
import static com.clavain.muninmxcd.logger;
import static com.clavain.utils.Generic.getUnixtime;
import static com.clavain.muninmxcd.logMore;
import static com.clavain.utils.Generic.getAlertByNidPluginAndGraph;
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
    private String str_LineMode;
    private long   l_lastFrontendQuery;
    private transient long   l_lastMuninQuery;
    private transient Socket csMuninSocket;
    private transient boolean b_IntervalIsSeconds = false;
    private int i_nodeId;
    private transient Integer from_time;
    private transient Integer to_time;
    private transient String timezone;
    private transient Integer customId;
    private transient Integer query_interval;
    private transient Integer user_id;
    private transient String crontab = "false";
    private ArrayList<MuninGraph> v_graphs = new ArrayList<>();

    
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
        return getV_graphs();
    }

    /**
     * @param v_graphs the v_graphs to set
     */
    public void setGraphs(ArrayList<MuninGraph> v_graphs) {
        this.setV_graphs(v_graphs);
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
            setCsMuninSocket(p_socket);
            // connection available?
            if(getCsMuninSocket() != null)
            {
                if(!csMuninSocket.isConnected() || getCsMuninSocket().isClosed())
                {
                    setCsMuninSocket(null);
                    setCsMuninSocket(new Socket());
                    getCsMuninSocket().setSoTimeout(5000);
                    getCsMuninSocket().setKeepAlive(false);
                    getCsMuninSocket().setSoLinger(true, 0);
                    getCsMuninSocket().setReuseAddress(true);                     
                    getCsMuninSocket().connect(new InetSocketAddress(p_strHostname, p_iPort),5000);
                    logger.info("Reconnecting to " + p_strHostname);
                }
            }
            else
            {
                setCsMuninSocket(new Socket());
                getCsMuninSocket().setSoTimeout(5000);
                getCsMuninSocket().setKeepAlive(false);
                getCsMuninSocket().setSoLinger(true, 0);
                getCsMuninSocket().setReuseAddress(true);                  
                getCsMuninSocket().connect(new InetSocketAddress(p_strHostname, p_iPort),5000);
            }
            PrintStream os = new PrintStream( getCsMuninSocket().getOutputStream() );
            BufferedReader in = new BufferedReader(new InputStreamReader( getCsMuninSocket().getInputStream()) );
            os.println("fetch " + this.getPluginName());
            if(logMore)
            {
                logger.info(p_strHostname + " Executed:  fetch " + this.getPluginName());
            }
            String line;    
            while((line = in.readLine()) != null) {
                if(line.startsWith("."))
                {
                    if(logMore)
                    {
                        logger.info(p_strHostname + " Executed:  fetch " + this.getPluginName() + " BUT INSTEAD GOT . AS RESULT");
                    }                    
                    // close wait fix for single mode
                    if(customId != null)
                    {
                        os.println("quit");
                        os.close();
                        in.close();
                        csMuninSocket.close();
                        csMuninSocket = null;
                    }                        
                    return;
                }
                if(logMore)
                {                
                    logger.info(p_strHostname + " Received from (fetch " + this.getPluginName()+") Result: " + line);
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
                    Iterator it = this.getV_graphs().iterator();
                    while (it.hasNext())
                    {
                        MuninGraph l_mg = (MuninGraph) it.next();
                        l_mg.setQueryInterval(p_queryInterval);
                        // required for customer plugin intervals
                        if(IntervalIsSeconds() != false)
                        {
                           l_mg.setIntervalIsSeconds(true); 
                        }
                        
                        if(logMore)
                        {
                            logger.info(p_strHostname + " - equalcheck - Given: " + l_mg.getGraphName() + " required: " + l_graphName);
                        }                          
                        if(l_mg.getGraphName().equals(l_graphName))
                        {    
                            if(logMore)
                            {
                                logger.info(p_strHostname + " - equalcheck passed - Given: " + l_mg.getGraphName() + " found: " + l_graphName);
                            }                                
                            if(l_value.trim().length() < 1)
                            {
                                l_mg.setGraphValue("0");
                            }
                            else
                            {
                                // check....
                                try
                                {
                                    if(logMore)
                                    {
                                        logger.info(p_strHostname + " - graph: " + l_mg.getGraphName() + " calling setGraphValue with value: " + l_value.trim());
                                    }                                    
                                    l_mg.setGraphValue(l_value.trim());
                                    // check if we need to add alert value as well
                                    if(l_mg.isInit())
                                    {
                                        ArrayList<Alert> av = getAlertByNidPluginAndGraph(this.get_NodeId(),this.getPluginName(),l_mg.getGraphName());
                                        if(!av.isEmpty())
                                        {
                                            for (Alert l_av : av) {
                                                l_av.addAndCheckSample(getUnixtime(), l_mg.getGraphValue());
                                            }
                                        }
                                    }
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

            if(customId != null)
            {
                os.close();
                in.close();
                csMuninSocket.shutdownInput();
                csMuninSocket.shutdownOutput();
                csMuninSocket.close();
                
                csMuninSocket = null;
            }

        } catch (Exception ex)
        {
            //this.getCsMuninSocket().close();
            //try { csMuninSocket.close(); } catch (Exception e) {};
            logger.error(p_strHostname + " Unable to connect/process: " + ex.getMessage());
            ex.printStackTrace();
        }
        
    }
    
    public ArrayList<MuninGraph> returnAllGraphs()
    {
        return getV_graphs();   
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

    /**
     * @return the b_IntervalIsSeconds
     */
    public boolean IntervalIsSeconds() {
        return b_IntervalIsSeconds;
    }

    /**
     * @param b_IntervalIsSeconds the b_IntervalIsSeconds to set
     */
    public void set_IntervalIsSeconds(boolean b_IntervalIsSeconds) {
        this.b_IntervalIsSeconds = b_IntervalIsSeconds;
    }

    /**
     * @return the i_nodeId
     */
    public int get_NodeId() {
        return i_nodeId;
    }

    /**
     * @param i_nodeId the i_nodeId to set
     */
    public void set_NodeId(int i_nodeId) {
        this.i_nodeId = i_nodeId;
    }

    /**
     * @return the to_time
     */
    public Integer getTo_time() {
        return to_time;
    }

    /**
     * @param to_time the to_time to set
     */
    public void setTo_time(Integer to_time) {
        this.to_time = to_time;
    }

    /**
     * @return the timezone
     */
    public String getTimezone() {
        return timezone;
    }

    /**
     * @param timezone the timezone to set
     */
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    /**
     * @return the v_graphs
     */
    public ArrayList<MuninGraph> getV_graphs() {
        return v_graphs;
    }

    /**
     * @param v_graphs the v_graphs to set
     */
    public void setV_graphs(ArrayList<MuninGraph> v_graphs) {
        this.v_graphs = v_graphs;
    }

    /**
     * @return the from_time
     */
    public Integer getFrom_time() {
        return from_time;
    }

    /**
     * @param from_time the from_time to set
     */
    public void setFrom_time(Integer from_time) {
        this.from_time = from_time;
    }

    /**
     * @return the customId
     */
    public Integer getCustomId() {
        return customId;
    }

    /**
     * @param customId the customId to set
     */
    public void setCustomId(Integer customId) {
        this.customId = customId;
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
     * @return the query_interval
     */
    public Integer getQuery_interval() {
        return query_interval;
    }

    /**
     * @param query_interval the query_interval to set
     */
    public void setQuery_interval(Integer query_interval) {
        this.query_interval = query_interval;
    }

    /**
     * @return the csMuninSocket
     */
    public Socket getCsMuninSocket() {
        return csMuninSocket;
    }

    /**
     * @param csMuninSocket the csMuninSocket to set
     */
    public void setCsMuninSocket(Socket csMuninSocket) {
        this.csMuninSocket = csMuninSocket;
    }

    /**
     * @return the crontab
     */
    public String getCrontab() {
        return crontab;
    }

    /**
     * @param crontab the crontab to set
     */
    public void setCrontab(String crontab) {
        this.crontab = crontab;
    }

    /**
     * @return the str_LineMode
     */
    public String getStr_LineMode() {
        return str_LineMode;
    }

    /**
     * @param str_LineMode the str_LineMode to set
     */
    public void setStr_LineMode(String str_LineMode) {
        this.str_LineMode = str_LineMode;
    }
}
