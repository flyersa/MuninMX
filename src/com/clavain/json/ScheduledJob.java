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
package com.clavain.json;

/**
 *
 * @author enricokern
 */
public class ScheduledJob {
    private String jobId;
    private String userId;
    private String nextFireTime;

    /**
     * @return the jobName
     */
    public String getJobName() {
        return jobId;
    }

    /**
     * @param jobName the jobName to set
     */
    public void setJobName(String jobName) {
        this.jobId = jobName;
    }

    /**
     * @return the groupName
     */
    public String getGroupName() {
        return userId;
    }

    /**
     * @param groupName the groupName to set
     */
    public void setGroupName(String groupName) {
        this.userId = groupName;
    }

    /**
     * @return the nextFireTime
     */
    public String getNextFireTime() {
        return nextFireTime;
    }

    /**
     * @param nextFireTime the nextFireTime to set
     */
    public void setNextFireTime(String nextFireTime) {
        this.nextFireTime = nextFireTime;
    }
}
