/*
 * MuninMX
 * Written by Enrico Kern, kern@clavain.com
 * www.clavain.com
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
    private BigDecimal inputAverage = new BigDecimal("0");
    private BigDecimal daysAverage = new BigDecimal("0");
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

    /**
     * @return the inputAverage
     */
    public BigDecimal getInputAverage() {
        return inputAverage;
    }

    /**
     * @param inputAverage the inputAverage to set
     */
    public void setInputAverage(BigDecimal inputAverage) {
        this.inputAverage = inputAverage;
    }

    /**
     * @return the daysAverage
     */
    public BigDecimal getDaysAverage() {
        return daysAverage;
    }

    /**
     * @param daysAverage the daysAverage to set
     */
    public void setDaysAverage(BigDecimal daysAverage) {
        this.daysAverage = daysAverage;
    }
    
}
