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
    
    
}
