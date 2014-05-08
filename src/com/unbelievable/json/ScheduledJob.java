
package com.unbelievable.json;

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
