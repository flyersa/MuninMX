/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.rca;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author enricokern
 */
public class RcaResultSet {
    private CopyOnWriteArrayList<RcaResult> v_results = new CopyOnWriteArrayList<>();
    private boolean finished = false;
    private String rcaId = "";
    private int user_id = 0;
    /**
     * @return the v_results
     */
    public CopyOnWriteArrayList<RcaResult> getV_results() {
        return v_results;
    }

    /**
     * @param v_results the v_results to set
     */
    public void setV_results(CopyOnWriteArrayList<RcaResult> v_results) {
        this.v_results = v_results;
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
     * @return the user_id
     */
    public int getUser_id() {
        return user_id;
    }

    /**
     * @param user_id the user_id to set
     */
    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
}
