/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.checks;

import java.util.List;

/**
 *
 * @author enricokern
 */
public class ReturnServiceCheck {
    private Integer returnValue;
    private List<String> output;
    private Integer user_id;
    private Integer cid;
    private Integer checktime;
    private Integer downTimeConfirmedAt;
    private Integer lastDownTimeConfirm;
    private String checktype;
    private String checkname;
    private Integer interval;
    private Integer notifydown;
    private Integer notifyagain;
    private Integer notifyifup;
    private Integer notifyflap;
    private String json;
    
    /**
     * @return the returnValue
     */
    public Integer getReturnValue() {
        return returnValue;
    }

    /**
     * @param aReturnValue the returnValue to set
     */
    public void setReturnValue(Integer aReturnValue) {
        returnValue = aReturnValue;
    }

    /**
     * @return the output
     */
    public List<String> getOutput() {
        return output;
    }

    /**
     * @param aOutput the output to set
     */
    public void setOutput(List<String> aOutput) {
        output = aOutput;
    }
    
    public ReturnServiceCheck(Integer p_iReturnValue,List<String> p_returnOutput)
    {
        returnValue = p_iReturnValue;
        output = p_returnOutput;
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
     * @return the checktime
     */
    public Integer getChecktime() {
        return checktime;
    }

    /**
     * @param checktime the checktime to set
     */
    public void setChecktime(Integer checktime) {
        this.checktime = checktime;
    }

    /**
     * @return the downTimeConfirmedAt
     */
    public Integer getDownTimeConfirmedAt() {
        return downTimeConfirmedAt;
    }

    /**
     * @param downTimeConfirmedAt the downTimeConfirmedAt to set
     */
    public void setDownTimeConfirmedAt(Integer downTimeConfirmedAt) {
        this.downTimeConfirmedAt = downTimeConfirmedAt;
    }

    /**
     * @return the lastDownTimeConfirm
     */
    public Integer getLastDownTimeConfirm() {
        return lastDownTimeConfirm;
    }

    /**
     * @param lastDownTimeConfirm the lastDownTimeConfirm to set
     */
    public void setLastDownTimeConfirm(Integer lastDownTimeConfirm) {
        this.lastDownTimeConfirm = lastDownTimeConfirm;
    }

    /**
     * @return the checktype
     */
    public String getChecktype() {
        return checktype;
    }

    /**
     * @param checktype the checktype to set
     */
    public void setChecktype(String checktype) {
        this.checktype = checktype;
    }

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
     * @return the notifydown
     */
    public Integer getNotifydown() {
        return notifydown;
    }

    /**
     * @param notifydown the notifydown to set
     */
    public void setNotifydown(Integer notifydown) {
        this.notifydown = notifydown;
    }

    /**
     * @return the notifyagain
     */
    public Integer getNotifyagain() {
        return notifyagain;
    }

    /**
     * @param notifyagain the notifyagain to set
     */
    public void setNotifyagain(Integer notifyagain) {
        this.notifyagain = notifyagain;
    }

    /**
     * @return the notifyifup
     */
    public Integer getNotifyifup() {
        return notifyifup;
    }

    /**
     * @param notifyifup the notifyifup to set
     */
    public void setNotifyifup(Integer notifyifup) {
        this.notifyifup = notifyifup;
    }

    /**
     * @return the notifyflap
     */
    public Integer getNotifyflap() {
        return notifyflap;
    }

    /**
     * @param notifyflap the notifyflap to set
     */
    public void setNotifyflap(Integer notifyflap) {
        this.notifyflap = notifyflap;
    }

    /**
     * @return the json
     */
    public String getJson() {
        return json;
    }

    /**
     * @param json the json to set
     */
    public void setJson(String json) {
        this.json = json;
    }
    
    
}
