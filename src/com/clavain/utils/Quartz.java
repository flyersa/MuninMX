/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.utils;

import com.clavain.jobs.MuninJob;
import com.clavain.json.ScheduledJob;
import com.clavain.munin.MuninNode;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import org.quartz.Trigger;
import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.sched;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import static org.quartz.JobBuilder.*;
import org.quartz.JobKey;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.impl.matchers.GroupMatcher.groupEquals;
/**
 *
 * @author enricokern
 */
public class Quartz {
    // schedule a new check
    public static boolean scheduleJob(MuninNode mn)
    {
        boolean l_retVal = false;   
        String uid = mn.getUser_id().toString();
        Trigger trigger = newTrigger().withIdentity("trigger", uid + mn.getNode_id() + System.currentTimeMillis()).startNow().withSchedule(simpleSchedule().withIntervalInMinutes(mn.getQueryInterval()) .repeatForever().withMisfireHandlingInstructionFireNow()).build(); 
        JobDetail job = newJob(MuninJob.class).withIdentity(mn.getNode_id().toString(), mn.getUser_id().toString()).usingJobData("nodeId", mn.getNode_id()).build();
        
        try
        {
            sched.scheduleJob(job, trigger);
            logger.info("Scheduled Job for Node: " + mn.getHostname() + " with interval: " + mn.getQueryInterval() + " minutes");
            l_retVal = true;
        } catch (Exception ex)
        {
            logger.error("Unable to Schedule Job for Node: " + mn.getHostname());
            logger.error(ex);
        }
        return l_retVal;
    }

    public static boolean isScheduled(Integer p_nodeid, Integer p_user_id)
    {
        boolean l_retVal = false;
        String jobSearch = p_user_id + "." + p_nodeid; 
        try {
            // enumerate each job group
            for(String group: com.clavain.muninmxcd.sched.getJobGroupNames()) {
                // enumerate each job in group
                for(JobKey jobKey : com.clavain.muninmxcd.sched.getJobKeys((GroupMatcher<JobKey>) groupEquals(group))) {
                    if(jobKey.toString().equals(jobSearch))
                    {
                        l_retVal = true;
                    }
                }
            }
        } catch (SchedulerException ex) {
            logger.error("Error in isScheduled: " + ex.getLocalizedMessage());
        }
        return l_retVal;
    }
  
    public static boolean isJobScheduled(int p_nodeid)
    {
        boolean retval = false;
        String match = p_nodeid+"";
        for(ScheduledJob sj : getScheduledJobs())
        {
            if(sj.getJobName().equals(match))
            {
                return true;
            }
        }
        
        return retval;
    }    
    
    
    public static boolean unscheduleCheck(String p_nodeid, String p_uid)
    {
        boolean l_retVal = false;
        JobKey jk = new JobKey(p_nodeid,p_uid);
        try {
            com.clavain.muninmxcd.sched.deleteJob(jk);
            l_retVal = true;
        } catch (SchedulerException ex) {
            logger.error("Error in unscheduleCheck: " + ex.getLocalizedMessage());
        }
        
        return l_retVal;
    }    
    
    public static ArrayList<ScheduledJob> getScheduledJobs()
    {
        ArrayList<ScheduledJob> retval = new ArrayList<>();
        try {
            for (String groupName : com.clavain.muninmxcd.sched.getJobGroupNames()) {

                for (JobKey jobKey : com.clavain.muninmxcd.sched.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                     String jobName = jobKey.getName();
                     String jobGroup = jobKey.getGroup();

                     //get job's trigger
                     List<Trigger> triggers = (List<Trigger>) com.clavain.muninmxcd.sched.getTriggersOfJob(jobKey);
                     Date nextFireTime = triggers.get(0).getNextFireTime(); 
                     ScheduledJob sj = new ScheduledJob();
                     sj.setJobName(jobName);
                     sj.setGroupName(jobGroup);
                     sj.setNextFireTime(nextFireTime.toString());
                     retval.add(sj);
                     }

               }
        } catch (SchedulerException ex) {
            logger.error("Error in getScheduledJobs(): " + ex.getLocalizedMessage());
        }
        return retval;
    }      
}
