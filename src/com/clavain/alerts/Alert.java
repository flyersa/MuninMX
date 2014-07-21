/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.alerts;

import java.util.concurrent.CopyOnWriteArrayList;
import static com.clavain.alerts.Methods.sendNotifications;
import static com.clavain.utils.Generic.getNodeHostNameForMuninNode;
import static com.clavain.utils.Generic.getUnixtime;
import java.math.BigDecimal;
/**
 *
 * @author enricokern
 */
public class Alert {
    private String str_PluginName;   
    private String str_GraphName;
    private Integer node_id;
    private Integer alert_id;
    private BigDecimal raise_value = new BigDecimal("0");
    private String condition;
    private Integer num_samples;
    // last alert (unix timestamp)
    private int last_alert;
    // alert limit in minutes, do not send alert again if not at least x time passed between last alert
    private int alert_limit = 0;
    private CopyOnWriteArrayList<AlertValue> v_values = new CopyOnWriteArrayList<>();
    private String alertMsg;
    private String hostname;
    
    
    /** and a new value set and check if alert condition is raised and if we need to send notifications 
     * 
     * @param p_timestamp timestamp
     * @param p_value value of metric
     */
    public void addAndCheckSample(int p_timestamp, BigDecimal p_Strvalue)
    {
        try
        {

            boolean sendAlert = false;
            AlertValue av = new AlertValue();
            av.setTimestamp(p_timestamp);
            av.setValue(p_Strvalue);
            v_values.add(av);
            double p_value = av.getValue().doubleValue();
            // if we have higher sample count now, remove first
            if(v_values.size() > num_samples)
            {
                v_values.remove(0);
            }

            // now do the check magic
            double avg = returnAvg();
            // value equal given value? alert
            switch (condition) {
                case "eq":
                    if(p_value == raise_value.doubleValue())
                    {
                        this.setAlertMsg("Plugin: " + str_PluginName + " Graph: " + str_GraphName + " Host: " + hostname + " value is: " + p_value + " with alert condition => equal: "+raise_value);
                        sendAlert = true;
                    }
                    break;
                case "lt":
                    if(p_value < raise_value.doubleValue())
                    {
                        this.setAlertMsg("Plugin: " + str_PluginName + " Graph: " + str_GraphName + " Host: " + hostname + " value is: " + p_value + " with alert condition => less then: "+raise_value);
                        sendAlert = true;
                    }
                    break;     
                case "gt":
                    if(p_value > raise_value.doubleValue())
                    {
                        this.setAlertMsg("Plugin: " + str_PluginName + " Graph: " + str_GraphName + " Host: " + hostname + " value is: " + p_value + " with alert condition => greater then: "+raise_value);
                        sendAlert = true;
                    }
                    break; 
                case "gtavg":
                    if(avg > raise_value.doubleValue())
                    {
                        this.setAlertMsg("Plugin: " + str_PluginName + " Graph: " + str_GraphName + " Host: " + hostname + " avg value is: " + avg + " with alert condition => avg of "+num_samples+" samples greater then: "+raise_value);
                        sendAlert = true;
                    }
                    break;  
                case "ltavg":
                    if(avg < raise_value.doubleValue())
                    {           
                        this.setAlertMsg("Plugin: " + str_PluginName + " Graph: " + str_GraphName + " Host: " + hostname + " avg value is: " + avg + " with alert condition => avg of "+num_samples+" samples less then: "+raise_value);
                        sendAlert = true;
                    }
                    break;                  
            }

            // check if we need to alert
            if(sendAlert)
            {
                if(getAlert_limit() > 0)
                {
                    int curTime = getUnixtime();
                    // convert alert_time to seconds
                    int alim = getAlert_limit() * 60;
                    int lalert = last_alert + alim;
                    if(lalert < curTime )
                    {
                        setLast_alert(getUnixtime());
                        sendNotifications(this);
                        
                    }
                }
                else
                {
                    setLast_alert(getUnixtime());
                    sendNotifications(this);
                    
                }

            }
        } catch (Exception ex)
        {
            com.clavain.muninmxcd.logger.error("Error in Alert/addAndCheckSample (recv value: "+p_Strvalue+"): " + ex.getLocalizedMessage());
        }
    }
    
    private double returnAvg()
    {
       double[] numbers = new double[v_values.size()];     
       double retval = 0;
       for (AlertValue l_av : v_values) {
           retval += l_av.getValue().doubleValue();
       }
       double average = retval / v_values.size();
       return average;
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
     * @return the node_id
     */
    public Integer getNode_id() {
        return node_id;
    }

    /**
     * @param node_id the node_id to set
     */
    public void setNode_id(Integer node_id) {
        this.node_id = node_id;
    }

    /**
     * @return the alert_id
     */
    public Integer getAlert_id() {
        return alert_id;
    }

    /**
     * @param alert_id the alert_id to set
     */
    public void setAlert_id(Integer alert_id) {
        this.alert_id = alert_id;
    }

    /**
     * @return the raise_value
     */
    public Double getRaise_value() {
        return raise_value.doubleValue();
    }

    /**
     * @param raise_value the raise_value to set
     */
    public void setRaise_value(BigDecimal raise_value) {
        this.raise_value = raise_value;
    }

    /**
     * @return the condition
     */
    public String getCondition() {
        return condition;
    }

    /**
     * @param condition the condition to set
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * @return the v_values
     */
    public CopyOnWriteArrayList<AlertValue> getValueList() {
        return v_values;
    }

    /**
     * @param v_values the v_values to set
     */
    public void setValueList(CopyOnWriteArrayList<AlertValue> v_values) {
        this.v_values = v_values;
    }

    /**
     * @return the num_samples
     */
    public Integer getNum_samples() {
        return num_samples;
    }

    /**
     * @param num_samples the num_samples to set
     */
    public void setNum_samples(Integer num_samples) {
        this.num_samples = num_samples;
    }

    /**
     * @return the alertMsg
     */
    public String getAlertMsg() {
        return alertMsg;
    }

    /**
     * @param alertMsg the alertMsg to set
     */
    public void setAlertMsg(String alertMsg) {
        this.alertMsg = alertMsg;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        if(hostname == null)
        {
            this.setHostname(getNodeHostNameForMuninNode(this.node_id));
        }
        return hostname;
    }

    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * @return the last_alert
     */
    public int getLast_alert() {
        return last_alert;
    }

    /**
     * @param last_alert the last_alert to set
     */
    public void setLast_alert(int last_alert) {
        this.last_alert = last_alert;
    }

    /**
     * @return the alert_limit
     */
    public int getAlert_limit() {
        return alert_limit;
    }

    /**
     * @param alert_limit the alert_limit to set
     */
    public void setAlert_limit(int alert_limit) {
        this.alert_limit = alert_limit;
    }
}
