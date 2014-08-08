/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.rca;

import com.clavain.json.User;
import com.clavain.munin.MuninGraph;
import com.clavain.munin.MuninNode;
import com.clavain.munin.MuninPlugin;
import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.p;
import static com.clavain.utils.Database.clearStringForSQL;
import static com.clavain.utils.Database.connectToDatabase;
import static com.clavain.utils.Database.dbSetRcaFinished;
import static com.clavain.utils.Database.getUserFromDatabase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.clavain.utils.Generic.getMuninNode;
import static com.clavain.utils.Generic.getUnixtime;
import static com.clavain.rca.Methods.*;
import static com.clavain.utils.Database.dbSetRcaOutput;
import java.math.BigDecimal;

/**
 *
 * @author enricokern
 */
public class Analyzer implements Runnable {

    private String rcaId;;
    private String groupname;
    private String category;
    private Integer user_id = 0;
    private String status = "initialized";
    private int singlehost = 0;
    private boolean running = false;
    private int nodes_affected = 0;
    private int nodes_processed = 0;
    private boolean finished = false;
    private int matchcount = 0;
    private transient ArrayList<MuninNode> rca_nodes = new ArrayList<>();
    private ArrayList<RcaResult> results = new ArrayList<>();
    private int starttime   = 0;
    private BigDecimal percentage = new BigDecimal("10");
    private int querydays = 4;
    private int start = 0;
    private int end = 0;
    private int day = 86400;
    private BigDecimal threshold = new BigDecimal(10.00);
    
    public Analyzer(String p_rcaId) {
        rcaId = p_rcaId;
    }

    public boolean configureFromDatabase() {
        boolean retval = false;
        try {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from rca WHERE rcaId = '" + getRcaId() + "'");
            while (rs.next()) {
                setGroupname(rs.getString("groupname"));
                setCategory(rs.getString("categoryfilter"));
                setUser_id((Integer) rs.getInt("user_id"));
                setSinglehost(rs.getInt("singlehost"));
                percentage = rs.getBigDecimal("percentage");
                querydays = rs.getInt("querydays") + 1;
                start = rs.getInt("start_time");
                end = rs.getInt("end_time");
                threshold = rs.getBigDecimal("threshold");
                retval = true;
                setStatus("Analyzer configured. Waiting for open slot...");
            }
            conn.close();
        } catch (Exception ex) {
            logger.error("[RCA] Error in configureFromDatabase for rcaId: " + getRcaId() + " - " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        return retval;
    }

    @Override
    public void run() {
        setStarttime(getUnixtime());
        setRunning(true);
        com.clavain.muninmxcd.v_analyzer.add(this);
        try {
            int maxjobs = Integer.parseInt(p.getProperty("rca.maxjobs"));

            while (com.clavain.muninmxcd.rcajobs_running > maxjobs) {
                logger.info("[RCA] Cannot start job: " + getRcaId() + " because already " + com.clavain.muninmxcd.rcajobs_running + " of " + maxjobs + " running. Trying again in 30s");
                Thread.sleep(30000);
            }

        } catch (Exception ex) {
            logger.error("[RCA] Error in sleep phase (waiting for slot) for rcaId: " + getRcaId() + " - " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }

        // ok lets go
        try {
            com.clavain.muninmxcd.rcajobs_running = com.clavain.muninmxcd.rcajobs_running + 1;
            setStatus("Analyzer job preparing - collecting matching nodes");
            User aUser = getUserFromDatabase(getUser_id());
            if (aUser == null) {
                com.clavain.muninmxcd.rcajobs_running = com.clavain.muninmxcd.rcajobs_running - 1;
                logger.fatal("[RCA] " + getRcaId() + " cannot proceed, user is null");
                setStatus(" cannot proceed, user is null");
                setRunning(false);
                return;
            }
            
            // single host? only add one node
            if(getSinglehost() > 0)
            {
                logger.info("[RCA] " + getRcaId() + " Filtering with Single Host ID: " + getSinglehost());
                MuninNode mn = getMuninNode(getSinglehost());
                if(mn == null)
                {
                    com.clavain.muninmxcd.rcajobs_running = com.clavain.muninmxcd.rcajobs_running - 1;
                    logger.fatal("[RCA] " + getRcaId() + " cannot proceed, single node ("+getSinglehost()+") is null");  
                    setStatus(" cannot proceed, single node (" + getSinglehost() + ") is null");
                    setRunning(false);
                    return;
                }    
                
                if(aUser.getUserrole().equals("admin"))
                {
                    getRca_nodes().add(mn);
                    logger.info("[RCA] " + getRcaId() + " Added single node: " + mn.getNodename());
                }
                else if(aUser.getUserrole().equals("userext"))
                {
                    if(mn.getUser_id().equals(getUser_id()))
                    {
                        getRca_nodes().add(mn);
                        logger.info("[RCA] " + getRcaId() + " Added single node: " + mn.getNodename());
                    }
                }
                else
                {
                    List<String> accessgroups = Arrays.asList(aUser.getAccessgroup().split(","));
                    if (accessgroups.contains(mn.getGroup()) && mn.getGroup().equals(getGroupname())) {
                        getRca_nodes().add(mn);
                        logger.info("[RCA] " + getRcaId() + " Added single node: " + mn.getNodename());
                     }                    
                }
            }
            // check if we filter on a group base and add node if the user got the permission todo that
            else if (getGroupname() != null) {
                logger.info("[RCA] " + getRcaId() + " Filtering with group: " + getGroupname());
                for (MuninNode l_mn : com.clavain.muninmxcd.v_munin_nodes) {
                    if (l_mn.getGroup().equals(getGroupname())) {
                        if (aUser.getUserrole().equals("admin")) {
                            getRca_nodes().add(l_mn);
                            logger.info("[RCA] " + getRcaId() + " Added group (" + getGroupname() + ") filtered node: " + l_mn.getNodename());
                        } else if (aUser.getUserrole().equals("userext")) {
                            if (l_mn.getUser_id().equals(aUser.getUser_id()) && l_mn.getGroup().equals(getGroupname())) {
                                getRca_nodes().add(l_mn);
                                logger.info("[RCA] " + getRcaId() + " Added group (" + getGroupname() + ") filtered node: " + l_mn.getNodename());
                            }
                        } else if (aUser.getUserrole().equals("user")) {
                            // check if group is in accessgroups
                            List<String> accessgroups = Arrays.asList(aUser.getAccessgroup().split(","));
                            if (accessgroups.contains(l_mn.getGroup()) && l_mn.getGroup().equals(getGroupname())) {
                                getRca_nodes().add(l_mn);
                                logger.info("[RCA] " + getRcaId() + " Added group (" + getGroupname() + ") filtered node: " + l_mn.getNodename());
                            }
                        }

                    }
                }
            } else {
                // fuck add all nodes...
                for (MuninNode l_mn : com.clavain.muninmxcd.v_munin_nodes) {
                    if (aUser.getUserrole().equals("admin")) {
                        getRca_nodes().add(l_mn);
                        logger.info("[RCA] " + getRcaId() + " Added node: " + l_mn.getNodename());
                    } else if (aUser.getUserrole().equals("userext")) {
                        if (l_mn.getUser_id().equals(aUser.getUser_id())) {
                            getRca_nodes().add(l_mn);
                            logger.info("[RCA] " + getRcaId() + " Added node: " + l_mn.getNodename());
                        }
                    } else if (aUser.getUserrole().equals("user")) {
                        // check if group is in accessgroups
                        List<String> accessgroups = Arrays.asList(aUser.getAccessgroup().split(","));
                        if (accessgroups.contains(l_mn.getGroup())) {
                            getRca_nodes().add(l_mn);
                            logger.info("[RCA] " + getRcaId() + " Added node: " + l_mn.getNodename());
                        }
                    }
                }
            }
            setNodes_affected(getRca_nodes().size());

            if(getNodes_affected() < 1)
            {
                setStatus("no nodes found for analyzing matching search query");
                logger.info("[RCA] " + getRcaId() + "no nodes found for analyzing matching search query");
                com.clavain.muninmxcd.rcajobs_running = com.clavain.muninmxcd.rcajobs_running - 1;
                setFinished(true);
                dbSetRcaFinished(getRcaId());
                setRunning(false);
                //com.clavain.muninmxcd.v_analyzer.remove(this);
                return;
            }
            
            // analyze this shit
            setStatus("Analyzer job started - processing " + getRca_nodes().size() + " nodes...");
            
            // BRAIN DAMAGE INCOMING, FOR FOR FOR FOR WHOOOOHOOO!
            for (MuninNode mn : rca_nodes) {
                pluginLoop:
                for (MuninPlugin mp : mn.getPluginList())
                {
                    if(category != null)
                    {
                        if(mp.getStr_PluginCategory() != null)
                        {
                            if(!mp.getStr_PluginCategory().equals(category))
                            {
                                continue pluginLoop;
                            }
                        }
                    }
                    graphLoop:
                    for (MuninGraph mg : mp.getGraphs())
                    {
                        status = "Analyzing " + mn.getNodename() + " - " + mp.getPluginName().toUpperCase() + "/" + mg.getGraphName();
                        //BigDecimal t = getTotalForPluginAndGraph(mp.getPluginName(), mg.getGraphName(), start, end, mn.getUser_id(), mn.getNode_id());
                        BigDecimal t = getAverageForPluginAndGraph(mp.getPluginName(), mg.getGraphName(), start, end, mn.getUser_id(), mn.getNode_id());
                        // skip if average is below threeshold
                        if(t.compareTo(threshold) == 0 || t.compareTo(threshold) == -1)                        
                        {
                            continue graphLoop;
                        }
                        int iterations = 1;
                        int p_start = start;
                        int p_end = end;
                        ArrayList<BigDecimal> values = new ArrayList<>();
                        while(iterations < this.querydays)
                        {
                            p_start = start-(day*iterations);
                            p_end = end-(day*iterations); 
                            BigDecimal adding = getAverageForPluginAndGraph(mp.getPluginName(), mg.getGraphName(), p_start, p_end, mn.getUser_id(), mn.getNode_id());
                            //logger.info("[RCA] iteration found: " + adding + " Total: "+t+" query: " + mp.getPluginName() +" / " + mg.getGraphName() + " start: " + p_start+ " end: " + p_end) ;
                            values.add(adding);
                            iterations++;
                        }
                        BigDecimal avg = returnAvgBig(values);
                        
                        BigDecimal foundPercentage;
                        if(avg.compareTo(t) == 1)
                        {
                            foundPercentage = ReversePercentageFromValues(t,avg);
                        }
                        else
                        {
                             foundPercentage = ReversePercentageFromValues(avg,t);
                        }
                        //logger.info("[RCA] avg: " + avg + " total: " + t + " foundPercentage " + foundPercentage + " query: " + mp.getPluginName() +" / " + mg.getGraphName());
                        // did we receive negative value, then convert to positive?
                        if(foundPercentage.signum() == -1)
                        {
                            foundPercentage = foundPercentage.abs();
                        }
                        
                        // match
                        if(foundPercentage.compareTo(percentage) == 0 || foundPercentage.compareTo(percentage) == 1)
                        {
                            RcaResult result = new RcaResult();
                            result.setNodeId(mn.getNode_id());
                            result.setNodeName(mn.getNodename());
                            result.setGraphName(mg.getGraphName());
                            result.setPluginName(mp.getPluginName());
                            result.setPercentage(foundPercentage);
                            result.setGraphLabel(mg.getGraphLabel());
                            result.setDaysAverage(avg);
                            result.setInputAverage(t);
                            results.add(result);
                            matchcount++;
                            status = "Added Result for node "+mn.getNodename()+" with match of "+foundPercentage+"% on " + mp.getPluginName().toUpperCase()+"/"+mg.getGraphLabel();
                            //logger.info("[RCA] " + getRcaId() + " Added Result for node "+mn.getNodename()+" with match of "+foundPercentage+"% on " + mp.getPluginName().toUpperCase()+"/"+mg.getGraphLabel());
                        }
                    }
                }
                nodes_processed++;
            }

            setFinished(true);
            
            logger.info("[RCA] " + getRcaId() + " job completed");
            
        } catch (Exception ex) {
            com.clavain.muninmxcd.rcajobs_running = com.clavain.muninmxcd.rcajobs_running - 1;
            setStatus("Error Occured - " + ex.getLocalizedMessage());
            logger.error("[RCA] Error in running thread for rcaId: " + getRcaId() + " - " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        
        com.clavain.muninmxcd.rcajobs_running = com.clavain.muninmxcd.rcajobs_running - 1;
        dbSetRcaFinished(getRcaId());
        status = "Analysis complete";
        setFinished(true);
        setRunning(false);
    }

    /**
     * @return the rcaId
     */
    public String getRcaId() {
        return rcaId;
    }

    /**
     * @param rcaId the rcaId to set
     */
    public void setRcaId(String rcaId) {
        this.rcaId = rcaId;
    }

    /**
     * @return the groupname
     */
    public String getGroupname() {
        return groupname;
    }

    /**
     * @param groupname the groupname to set
     */
    public void setGroupname(String groupname) {
        this.groupname = groupname;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
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
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the singlehost
     */
    public int getSinglehost() {
        return singlehost;
    }

    /**
     * @param singlehost the singlehost to set
     */
    public void setSinglehost(int singlehost) {
        this.singlehost = singlehost;
    }

    /**
     * @return the running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @param running the running to set
     */
    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * @return the nodes_affected
     */
    public int getNodes_affected() {
        return nodes_affected;
    }

    /**
     * @param nodes_affected the nodes_affected to set
     */
    public void setNodes_affected(int nodes_affected) {
        this.nodes_affected = nodes_affected;
    }

    /**
     * @return the finished
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * @param finished the finished to set
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
        // upload to database
        dbSetRcaOutput(this);
    }

    /**
     * @return the rca_nodes
     */
    public ArrayList<MuninNode> getRca_nodes() {
        return rca_nodes;
    }

    /**
     * @param rca_nodes the rca_nodes to set
     */
    public void setRca_nodes(ArrayList<MuninNode> rca_nodes) {
        this.rca_nodes = rca_nodes;
    }

    /**
     * @return the results
     */
    public ArrayList<RcaResult> getResults() {
        return results;
    }

    /**
     * @param results the results to set
     */
    public void setResults(ArrayList<RcaResult> results) {
        this.results = results;
    }

    /**
     * @return the starttime
     */
    public int getStarttime() {
        return starttime;
    }

    /**
     * @param starttime the starttime to set
     */
    public void setStarttime(int starttime) {
        this.starttime = starttime;
    }
}
