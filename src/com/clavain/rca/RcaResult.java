/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.rca;

import java.math.BigDecimal;

/**
 *
 * @author enricokern
 */
public class RcaResult {
    private String pluginName   =   "";
    private String  graphLabel   =   "";
     private String  graphName   =   "";
    private BigDecimal percentage = new BigDecimal("0");
    private String nodeName = "";
    private int nodeId = 0;

    /**
     * @return the pluginName
     */
    public String getPluginName() {
        return pluginName;
    }

    /**
     * @param pluginName the pluginName to set
     */
    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    /**
     * @return the graphName
     */
    public String getGraphName() {
        return graphName;
    }

    /**
     * @param graphName the graphName to set
     */
    public void setGraphName(String graphName) {
        this.graphName = graphName;
    }

    /**
     * @return the percentage
     */
    public BigDecimal getPercentage() {
        return percentage;
    }

    /**
     * @param percentage the percentage to set
     */
    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    /**
     * @return the nodeName
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * @param nodeName the nodeName to set
     */
    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * @return the nodeId
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * @param nodeId the nodeId to set
     */
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * @return the graphLabel
     */
    public String getGraphLabel() {
        return graphLabel;
    }

    /**
     * @param graphLabel the graphLabel to set
     */
    public void setGraphLabel(String graphLabel) {
        this.graphLabel = graphLabel;
    }
    
}
