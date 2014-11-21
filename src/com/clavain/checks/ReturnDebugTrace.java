/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.checks;

/**
 *
 * @author enricokern
 */
public class ReturnDebugTrace {
    private String output;
    private int checktime;
    private int retval;
    private int cid;
    private int user_id;

    /**
     * @return the output
     */
    public String getOutput() {
        return output;
    }

    /**
     * @param output the output to set
     */
    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * @return the checktime
     */
    public int getChecktime() {
        return checktime;
    }

    /**
     * @param checktime the checktime to set
     */
    public void setChecktime(int checktime) {
        this.checktime = checktime;
    }

    /**
     * @return the retval
     */
    public int getRetval() {
        return retval;
    }

    /**
     * @param retval the retval to set
     */
    public void setRetval(int retval) {
        this.retval = retval;
    }

    /**
     * @return the cid
     */
    public int getCid() {
        return cid;
    }

    /**
     * @param cid the cid to set
     */
    public void setCid(int cid) {
        this.cid = cid;
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
