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
package com.clavain.utils;

import com.clavain.jobs.CheckJob;
import com.clavain.jobs.CustomJob;
import com.clavain.jobs.MuninJob;
import com.clavain.json.ScheduledJob;
import com.clavain.json.ServiceCheck;
import com.clavain.munin.MuninNode;
import com.clavain.munin.MuninPlugin;
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
import static com.clavain.utils.Database.getMuninPluginForCustomJobFromDb;
import static com.clavain.utils.Database.getServiceCheckFromDatabase;

import static com.clavain.utils.Generic.getStampFromTimeAndZone;
import static com.clavain.utils.Generic.getMuninPluginForCustomJob;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.quartz.CronScheduleBuilder;

/**
 *
 * @author enricokern
 */
public class Quartz {
    
    public static boolean scheduleServiceCheck(Integer p_cid)
    {
        ServiceCheck sc = getServiceCheckFromDatabase(p_cid);
        if(sc == null)
        {
            return false;
        }
        return scheduleServiceCheck(sc);
    }
    
    // schedule a new check
    public static boolean scheduleServiceCheck(ServiceCheck p_sc)
    {
        boolean l_retVal = false;   
        Trigger trigger = newTrigger().withIdentity("checktrigger", p_sc.getUser_id().toString() + p_sc.getCid() + System.currentTimeMillis()).startNow().withSchedule(simpleSchedule().withIntervalInMinutes(p_sc.getInterval()) .repeatForever().withMisfireHandlingInstructionFireNow()).build(); 
        JobDetail job = newJob(CheckJob.class).withIdentity(p_sc.getCid().toString(), p_sc.getUser_id().toString()).usingJobData("cid", p_sc.getCid()).build();
        try
        {
            com.clavain.muninmxcd.sched_checks.scheduleJob(job, trigger);
            l_retVal = true;
        } catch (Exception ex)
        {
            com.clavain.muninmxcd.logger.error(ex);
        }
        return l_retVal;
    }    
    
    // delete a job
    public static boolean unscheduleServiceCheck(String p_cid, String p_uid)
    {
        boolean l_retVal = false;
        JobKey jk = new JobKey(p_cid.toString(),p_uid.toString());
        try {
            com.clavain.muninmxcd.sched_checks.deleteJob(jk);
            l_retVal = true;
        } catch (SchedulerException ex) {
            com.clavain.muninmxcd.logger.error(ex.getLocalizedMessage());
        }
        
        return l_retVal;
    }    
    
    public static boolean isServiceCheckScheduled(int p_cid)
    {
        boolean retval = false;
        String match = p_cid+"";
        
        for(ScheduledJob sj : getScheduledServiceChecks())
        {
            if(sj.getJobName().equals(match))
            {
                return true;
            }
        }
        
        return retval;
    }  
    
    public static ArrayList<ScheduledJob> getScheduledServiceChecks()
    {
        ArrayList<ScheduledJob> retval = new ArrayList<>();
        try {
            for (String groupName : com.clavain.muninmxcd.sched_checks.getJobGroupNames()) {

                for (JobKey jobKey : com.clavain.muninmxcd.sched_checks.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                     String jobName = jobKey.getName();
                     String jobGroup = jobKey.getGroup();

                     //get job's trigger
                     List<Trigger> triggers = (List<Trigger>) com.clavain.muninmxcd.sched_checks.getTriggersOfJob(jobKey);
                     Date nextFireTime = triggers.get(0).getNextFireTime(); 
                     ScheduledJob sj = new ScheduledJob();
                     sj.setJobName(jobName);
                     sj.setGroupName(jobGroup);
                     sj.setNextFireTime(nextFireTime.toString());
                     retval.add(sj);
                     }

               }
        } catch (SchedulerException ex) {
            com.clavain.muninmxcd.logger.error(ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        return retval;
    }        
    
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
    
   public static boolean isCustomJobScheduled(int p_cid)
    {
        boolean retval = false;
        String match = p_cid+"";
        for(ScheduledJob sj : getScheduledCustomJobs())
        {
            if(sj.getJobName().equals(match))
            {
                return true;
            }
        }
        
        return retval;
    }    
    
    
    public static boolean unscheduleCustomJob(String p_cid, String p_uid)
    {
        boolean l_retVal = false;
        JobKey jk = new JobKey(p_cid,p_uid);
        try {
            com.clavain.muninmxcd.sched_custom.deleteJob(jk);
            com.clavain.muninmxcd.v_cinterval_plugins.remove(getMuninPluginForCustomJob(Integer.parseInt(p_cid)));
            l_retVal = true;
        } catch (SchedulerException ex) {
            logger.error("Error in unscheduleCustomCheck: " + ex.getLocalizedMessage());
        }
        
        return l_retVal;
    }   
    
    // schedule a custom interval check
    public static boolean scheduleCustomIntervalJob(Integer p_cid)
    {
        boolean retval = false;
        MuninPlugin l_mp = getMuninPluginForCustomJobFromDb(p_cid);
        if(l_mp != null)
        {
           String uid = l_mp.getUser_id().toString();
           String cinterval = "";
           // build trigger
           Trigger trigger;
           // crontab trigger
           if(!l_mp.getCrontab().equals("false"))
           {
             // fixed start/end ?
             if(l_mp.getFrom_time() == 0)
             {
                trigger = newTrigger().withIdentity("trigger", uid + l_mp.getCustomId() + System.currentTimeMillis()).withSchedule(CronScheduleBuilder.cronSchedule(l_mp.getCrontab()).withMisfireHandlingInstructionFireAndProceed().inTimeZone(TimeZone.getTimeZone(l_mp.getTimezone()))).build();        
                cinterval = " every " + l_mp.getQuery_interval() + " seconds with crontab: " + l_mp.getCrontab();
             }
             else
             {
                long a = l_mp.getFrom_time();
                long b = l_mp.getTo_time();

                Date startDate = new Date(a*1000L);
                Date endDate = new Date(b*1000L);  
                trigger = newTrigger().withIdentity("trigger", uid + l_mp.getCustomId() + System.currentTimeMillis()).startAt(startDate).withSchedule(CronScheduleBuilder.cronSchedule(l_mp.getCrontab()).withMisfireHandlingInstructionFireAndProceed().inTimeZone(TimeZone.getTimeZone(l_mp.getTimezone()))).endAt(endDate).build();        
                cinterval = " every " + l_mp.getQuery_interval() + " seconds with crontab: " + l_mp.getCrontab() + " from " + startDate + " to " + endDate;
               
             }
           }
           else
           {
                // standard repeat forever trigger
                if(l_mp.getFrom_time() == 0)
                {
                    trigger = newTrigger().withIdentity("trigger", uid + l_mp.getCustomId() + System.currentTimeMillis()).startNow().withSchedule(simpleSchedule().withIntervalInSeconds(l_mp.getQuery_interval()).repeatForever().withMisfireHandlingInstructionFireNow()).build(); 
                    cinterval = " every " + l_mp.getQuery_interval() + " seconds";
                }
                // daterange trigger, ignore missfire
                else
                {
                    long a = l_mp.getFrom_time();
                    long b = l_mp.getTo_time();

                    Date startDate = new Date(a*1000L);
                    Date endDate = new Date(b*1000L);
                    long cur = (System.currentTimeMillis() / 1000L);
                    trigger = newTrigger().withIdentity("trigger", uid + l_mp.getCustomId() + System.currentTimeMillis()).startAt(startDate).withSchedule(simpleSchedule().withIntervalInSeconds(l_mp.getQuery_interval()).repeatForever().withMisfireHandlingInstructionIgnoreMisfires()).endAt(endDate).build(); 
                    cinterval = " every " + l_mp.getQuery_interval() + " seconds from " + startDate + " to " + endDate;
                }
           }
           //Trigger trigger = newTrigger().withIdentity("trigger", uid + l_mp.get_NodeId() + System.currentTimeMillis()).startNow().withSchedule(simpleSchedule().withIntervalInMinutes(l_mp.get .repeatForever().withMisfireHandlingInstructionFireNow()).build(); 
           JobDetail job = newJob(CustomJob.class).withIdentity(l_mp.getCustomId() + "", l_mp.getUser_id().toString()).usingJobData("customId", l_mp.getCustomId()).build();

           try
           {
               com.clavain.muninmxcd.sched_custom.scheduleJob(job, trigger);
               logger.info("Scheduled CustomJob for custom interval: " + l_mp.getCustomId() + " with interval " + cinterval);
               retval = true;
               com.clavain.muninmxcd.v_cinterval_plugins.add(l_mp);
           } catch (Exception ex)
           {
               logger.error("Unable to Schedule Job for custom interval:" + l_mp.getCustomId() + " with interval " + cinterval);
               logger.error(ex.getLocalizedMessage());
           }           

        }
        return retval;
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
 
    public static ArrayList<ScheduledJob> getScheduledCheckJobs()
    {
        ArrayList<ScheduledJob> retval = new ArrayList<>();
        try {
            for (String groupName : com.clavain.muninmxcd.sched_checks.getJobGroupNames()) {

                for (JobKey jobKey : com.clavain.muninmxcd.sched_checks.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                     String jobName = jobKey.getName();
                     String jobGroup = jobKey.getGroup();

                     //get job's trigger
                     List<Trigger> triggers = (List<Trigger>) com.clavain.muninmxcd.sched_checks.getTriggersOfJob(jobKey);
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
    
    public static ArrayList<ScheduledJob> getScheduledCustomJobs()
    {
        ArrayList<ScheduledJob> retval = new ArrayList<>();
        try {
            for (String groupName : com.clavain.muninmxcd.sched_custom.getJobGroupNames()) {

                for (JobKey jobKey : com.clavain.muninmxcd.sched_custom.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {

                     String jobName = jobKey.getName();
                     String jobGroup = jobKey.getGroup();

                     //get job's trigger
                     List<Trigger> triggers = (List<Trigger>) com.clavain.muninmxcd.sched_custom.getTriggersOfJob(jobKey);
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
