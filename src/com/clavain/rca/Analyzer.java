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
    private boolean finished = false;
    private ArrayList<MuninNode> rca_nodes = new ArrayList<>();
    private ArrayList<RcaResult> results = new ArrayList<>();
    private int starttime   = 0;
    private int percentage = 10;
    
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
                percentage = rs.getInt("percentage");
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
                for (MuninPlugin mp : mn.getPluginList())
                {
                    for (MuninGraph mg : mp.getGraphs())
                    {
                        
                    }
                }
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
