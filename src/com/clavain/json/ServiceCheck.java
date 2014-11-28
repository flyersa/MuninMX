/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.json;

import com.google.gson.JsonArray;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author enricokern
 * 
 * {"checkname":"ttt","interval":"5","checktype":"1","nonearg":"'127.0.0.1'"}
 */
public class ServiceCheck implements Serializable {
    private static final long serialVersionUID = -4991349865504351287L;
    private String  checkname;
    private Integer interval;
    private Integer checktype;
    private String nonearg;
    private String command;
    private String[] param;
    private Integer user_id;
    private Integer cid;
    private Integer iterations = 0;
    private boolean is_active = true;


    /**
     * @return the checkname
     */
    public String getCheckname() {
        return checkname;
    }

    /**
     * @param checkname the checkname to set
     */
    public void setCheckname(String checkname) {
        this.checkname = checkname;
    }

    /**
     * @return the interval
     */
    public Integer getInterval() {
        return interval;
    }

    /**
     * @param interval the interval to set
     */
    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    /**
     * @return the checktype
     */
    public Integer getChecktype() {
        return checktype;
    }

    /**
     * @param checktype the checktype to set
     */
    public void setChecktype(Integer checktype) {
        this.checktype = checktype;
    }

    /**
     * @return the nonearg
     */
    public String getNonearg() {
        return nonearg;
    }

    /**
     * @param nonearg the nonearg to set
     */
    public void setNonearg(String nonearg) {
        this.nonearg = nonearg;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * @return the param
     */
    public String[] getParam() {
        return param;
    }

    /**
     * @param param the param to set
     */
    public void setParam(String[] p_param) {
        param = p_param;
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
     * @return the cid
     */
    public Integer getCid() {
        return cid;
    }

    /**
     * @param cid the cid to set
     */
    public void setCid(Integer cid) {
        this.cid = cid;
    }


    /**
     * @return the iterations
     */
    public Integer getIterations() {
        return iterations;
    }

    /**
     * @param iterations the iterations to set
     */
    public void setIterations(Integer iterations) {
        this.iterations = iterations;
    }

    /**
     * @return the is_active
     */
    public boolean isIs_active() {
        return is_active;
    }

    /**
     * @param is_active the is_active to set
     */
    public void setIs_active(boolean is_active) {
        this.is_active = is_active;
    }


}