/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.munin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import static com.clavain.muninmxcd.p;
import static com.clavain.utils.Generic.getUnixtime;

/**
 *
 * @author ekum
 */
public class MuninGraph {
    private String str_GraphName;
    private String str_GraphLabel;
    private String str_GraphInfo;
    private String str_GraphType;
    private String str_GraphDraw;
    private boolean b_isNegative = false;
    // stores the number from the current run, on COUNTER it is bdGraphValue-bd_LastGraphValue;
    private BigDecimal bd_GraphValue = new BigDecimal("0");
    // last graph value contains the value from the last query, it is required
    // to give out live numbers for a graph with GraphType COUNTER
    private BigDecimal bd_LastGraphValue = new BigDecimal("0");
    private BigDecimal bd_LastGraphValueCounter = new BigDecimal("0");
    private  boolean is_init = false;
    private int i_lastGraphFetch    =   0;
    private int i_lastQueued    = 0;
    private int queryInterval = 0;
    private transient boolean b_IntervalIsSeconds = false;

    public void setLastQueued(int p_time)
    {
        i_lastQueued = p_time;
    }
    
    public int getLastQueued()
    {
        return i_lastQueued;
    }
    
    /**
     * @return the last time this graph was updated
     */
    public int getLastGraphTime()
    {
        return i_lastGraphFetch;
    }
    
    /**
     * @return the str_GraphLabel
     */
    public String getGraphLabel() {
        return str_GraphLabel;
    }

    /**
     * @param str_GraphLabel the str_GraphLabel to set
     */
    public void setGraphLabel(String str_GraphLabel) {
        this.str_GraphLabel = str_GraphLabel;
    }

    /**
     * @return the str_GraphInfo
     */
    public String getGraphInfo() {
        return str_GraphInfo;
    }

    /**
     * @param str_GraphInfo the str_GraphInfo to set
     */
    public void setGraphInfo(String str_GraphInfo) {
        this.str_GraphInfo = str_GraphInfo;
    }

    /**
     * @return the str_GraphType
     */
    public String getGraphType() {
        return str_GraphType;
    }

    /**
     * @param str_GraphType the str_GraphType to set
     */
    public void setGraphType(String str_GraphType) {
        this.str_GraphType = str_GraphType;
    }

    /**
     * @return the str_GraphDraw
     */
    public String getGraphDraw() {
        return str_GraphDraw;
    }

    /**
     * @param str_GraphDraw the str_GraphDraw to set
     */
    public void setGraphDraw(String str_GraphDraw) {
        this.str_GraphDraw = str_GraphDraw;
    }

    /**
     * @return the str_GraphName
     */
    public String getGraphName() {
        return str_GraphName;
    }

    /**
     * @param str_GraphName the str_GraphName to set
     */
    public void setGraphName(String str_GraphName) {
        this.str_GraphName = str_GraphName;
    }

    /**
     * @return the str_GraphValue
     */
    public BigDecimal getGraphValue() {
        return bd_GraphValue;
    }

    /**
     * @param str_GraphValue the str_GraphValue to set
     */
    public void setGraphValue(String str_GraphValue) {
        //com.unbelievablemachine.monitoring.MuninToMongo.logger.warn("[Received GraphValue: " + str_GraphValue + " for graph: " + this.str_GraphName);
        bd_LastGraphValue = bd_GraphValue;
        // check for UNKNOWN according to munin documentation
        if(str_GraphValue.toUpperCase().equals("U"))
        {
            // set to 0, better then U
            str_GraphValue = "0";
        }
 
        
        bd_GraphValue = new BigDecimal(str_GraphValue);
        
        if(this.getGraphType() != null)
        {
            if(this.getGraphType().equals("COUNTER") || this.getGraphType().equals("DERIVE"))
            {
                if(bd_LastGraphValueCounter.equals(new BigDecimal("0")))
                {
                    bd_GraphValue = new BigDecimal("0");
                }
                else
                {
                    is_init = true;
                    
                    bd_GraphValue = bd_GraphValue.subtract(bd_LastGraphValueCounter);
                    bd_GraphValue = bd_GraphValue.divide(new BigDecimal(""+this.getQueryInterval()), 2, RoundingMode.HALF_UP);
                }
            }
            else
            {
                is_init = true;
            }
        }
        else
        {
            is_init = true;
        }
        bd_LastGraphValueCounter = new BigDecimal(str_GraphValue);
        i_lastGraphFetch = getUnixtime();
    }

    /**
     * @return the str_LastGraphValue
     */
    public BigDecimal getLastGraphValue() {
        return bd_LastGraphValue;
    }

    /**
     * @param str_LastGraphValue the str_LastGraphValue to set
     */
    public void setLastGraphValue(String str_LastGraphValue) {
        this.bd_LastGraphValue = new BigDecimal(str_LastGraphValue);
    }

    public void setLastGraphValueCounter(String str_LastGraphValueCounter) {
        this.bd_LastGraphValueCounter = new BigDecimal(str_LastGraphValueCounter);
    }    
    
    public boolean isInit()
    {
        return this.is_init;
    }

    public void setQueryInterval(int p_queryInterval) {
        this.queryInterval = p_queryInterval;
    }

    public int getQueryInterval() {
        int retval = queryInterval;
        if(this.b_IntervalIsSeconds == false)
        {
            retval = queryInterval * 60;
        }
        return retval;
    }

    /**
     * @return the b_isNegative
     */
    public boolean isNegative() {
        return b_isNegative;
    }

    /**
     * @param b_isNegative the b_isNegative to set
     */
    public void setNegative(boolean b_isNegative) {
        this.b_isNegative = b_isNegative;
    }

    void setIntervalIsSeconds(boolean b) {
       b_IntervalIsSeconds = b;
    }

}
