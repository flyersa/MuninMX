package com.unbelievable.utils;

import com.unbelievable.jobs.MuninJob;
import com.unbelievable.munin.MuninNode;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDetail;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import org.quartz.Trigger;
import static org.quartz.TriggerBuilder.newTrigger;
import static com.unbelievable.muninmxcd.logger;
import static com.unbelievable.muninmxcd.sched;
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
}
