/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.rca;

import com.clavain.json.User;
import com.clavain.munin.MuninNode;
import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.p;
import static com.clavain.utils.Database.clearStringForSQL;
import static com.clavain.utils.Database.connectToDatabase;
import static com.clavain.utils.Database.dbUpdateRcaStatus;
import static com.clavain.utils.Database.getUserFromDatabase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author enricokern
 */
public class Analyzer implements Runnable {

    private int rcaId = 0;
    private String groupname;
    private String category;
    private Integer user_id = 0;
    public ArrayList<MuninNode> rca_nodes = new ArrayList<>();

    public Analyzer(int p_rcaId) {
        rcaId = p_rcaId;
    }

    public boolean configureFromDatabase() {
        boolean retval = false;
        try {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from rca WHERE rcaId = '" + rcaId + "'");
            while (rs.next()) {
                groupname = rs.getString("groupname");
                category = rs.getString("categoryfilter");
                user_id = rs.getInt("user_id");
                retval = true;
                dbUpdateRcaStatus(rcaId, "Analyzer configured. Waiting for open slot...");
            }
            conn.close();
        } catch (Exception ex) {
            logger.error("[RCA] Error in configureFromDatabase for rcaId: " + rcaId + " - " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        return retval;
    }

    @Override
    public void run() {
        try {
            int maxjobs = Integer.parseInt(p.getProperty("rca.maxjobs"));

            while (com.clavain.muninmxcd.rcajobs_running > maxjobs) {
                logger.info("[RCA] Cannot start job: " + rcaId + " because already " + com.clavain.muninmxcd.rcajobs_running + " of " + maxjobs + " running. Trying again in 30s");
                Thread.sleep(30000);
            }

        } catch (Exception ex) {
            logger.error("[RCA] Error in sleep phase (waiting for slot) for rcaId: " + rcaId + " - " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }

        // ok lets go
        try {
            com.clavain.muninmxcd.rcajobs_running = com.clavain.muninmxcd.rcajobs_running + 1;
            dbUpdateRcaStatus(rcaId, "Analyzer job preparing - collecting matching nodes");
            User aUser = getUserFromDatabase(user_id);
            if (aUser == null) {
                com.clavain.muninmxcd.rcajobs_running = com.clavain.muninmxcd.rcajobs_running - 1;
                logger.fatal("[RCA] " + rcaId + " cannot proceed, user is null");
                return;
            }
            // check if we filter on a group base and add node if the user got the permission todo that
            if (groupname != null) {
                logger.info("[RCA] " + rcaId + " Filtering with group: " + groupname);
                for (MuninNode l_mn : com.clavain.muninmxcd.v_munin_nodes) {
                    if (l_mn.getGroup().equals(groupname)) {
                        if (aUser.getUserrole().equals("admin")) {
                            rca_nodes.add(l_mn);
                            logger.info("[RCA] " + rcaId + " Added group (" + groupname + ") filtered node: " + l_mn.getNodename());
                        } else if (aUser.getUserrole().equals("userext")) {
                            if (l_mn.getUser_id().equals(aUser.getUser_id()) && l_mn.getGroup().equals(groupname)) {
                                rca_nodes.add(l_mn);
                                logger.info("[RCA] " + rcaId + " Added group (" + groupname + ") filtered node: " + l_mn.getNodename());
                            }
                        } else if (aUser.getUserrole().equals("user")) {
                            // check if group is in accessgroups
                            List<String> accessgroups = Arrays.asList(aUser.getAccessgroup().split(","));
                            if (accessgroups.contains(l_mn.getGroup()) && l_mn.getGroup().equals(groupname)) {
                                rca_nodes.add(l_mn);
                                logger.info("[RCA] " + rcaId + " Added group (" + groupname + ") filtered node: " + l_mn.getNodename());
                            }
                        }

                    }
                }
            } else {
                // fuck add all nodes...
                for (MuninNode l_mn : com.clavain.muninmxcd.v_munin_nodes) {
                    if (aUser.getUserrole().equals("admin")) {
                        rca_nodes.add(l_mn);
                        logger.info("[RCA] " + rcaId + " Added node: " + l_mn.getNodename());
                    } else if (aUser.getUserrole().equals("userext")) {
                        if (l_mn.getUser_id().equals(aUser.getUser_id())) {
                            rca_nodes.add(l_mn);
                            logger.info("[RCA] " + rcaId + " Added node: " + l_mn.getNodename());
                        }
                    } else if (aUser.getUserrole().equals("user")) {
                        // check if group is in accessgroups
                        List<String> accessgroups = Arrays.asList(aUser.getAccessgroup().split(","));
                        if (accessgroups.contains(l_mn.getGroup())) {
                            rca_nodes.add(l_mn);
                            logger.info("[RCA] " + rcaId + " Added node: " + l_mn.getNodename());
                        }
                    }
                }
            }

            // analyze this shit
            dbUpdateRcaStatus(rcaId, "Analyzer job started - processing " + rca_nodes.size() + " nodes...");
            
            
            
            // job completed remove
            com.clavain.muninmxcd.rcajobs_running = com.clavain.muninmxcd.rcajobs_running - 1;
            logger.info("[RCA] " + rcaId + " job completed");
            
        } catch (Exception ex) {
            com.clavain.muninmxcd.rcajobs_running = com.clavain.muninmxcd.rcajobs_running - 1;
            logger.error("[RCA] Error in running thread for rcaId: " + rcaId + " - " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
}
