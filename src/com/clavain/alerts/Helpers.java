/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.alerts;

import static com.clavain.muninmxcd.p;
import java.sql.ResultSet;
import static com.clavain.utils.Database.dbAddAllAlertWithId;
import static com.clavain.utils.Database.connectToDatabase;
import java.sql.Connection;
/**
 *
 * @author enricokern
 */
public class Helpers {

    public static void withdrawSMSTicket(Integer user_id) {
        try {
         Connection conn = connectToDatabase(p);   
         java.sql.Statement stmt = conn.createStatement();
         stmt.executeUpdate("UPDATE users SET sms_tickets = sms_tickets -1 WHERE id = " + user_id);
         conn.close();
        } catch (Exception ex) {
            com.clavain.muninmxcd.logger.error("Error in withdrawSMS Ticket for User: " + user_id + " with: " + ex.getLocalizedMessage());
        }
    }
    
    public static void withdrawTTSTicket(Integer user_id)
    {
        try 
        {
            Connection conn = connectToDatabase(p);   
            java.sql.Statement stmt = conn.createStatement();            
            stmt.executeUpdate("UPDATE users SET tts_tickets = tts_tickets -1 WHERE id = " + user_id);
            conn.close();
        } catch (Exception ex)
        {
            com.clavain.muninmxcd.logger.error("Error in withdrawTTS Ticket for User: " + user_id + " with: " + ex.getLocalizedMessage());
        }
    }  
    
    public static int getTTSTicketCount(Integer user_id)
    {
        try 
        {
            Connection conn = connectToDatabase(p);   
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from users WHERE id = " + user_id);
            while(rs.next())
            {
                return rs.getInt("tts_tickets");
            }    
        } catch (Exception ex)
        {
            com.clavain.muninmxcd.logger.error("Error in retrieving TTSTicket Count for User: " + user_id + " with: " + ex.getLocalizedMessage());
        }
        return 0;
    }    
   
    public static int getSMSTicketCount(Integer user_id)
    {
        try 
        {
            Connection conn = connectToDatabase(p);   
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from users WHERE id = " + user_id);
            while(rs.next())
            {
                return rs.getInt("sms_tickets");
            }
        } catch (Exception ex)
        {
            com.clavain.muninmxcd.logger.error("Error in retrieving SMSTicket Count for User: " + user_id + " with: " + ex.getLocalizedMessage());
        }
        return 0;
    }      
    
    public static String getCustomerEMail(Integer user_id)
    {
        try 
        {
            Connection conn = connectToDatabase(p);   
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * from users WHERE id = " + user_id);
            while(rs.next())
            {
                return rs.getString("email");
            }
        } catch (Exception ex)
        {
            com.clavain.muninmxcd.logger.error("Error in retrieving SMSTicket Count for User: " + user_id + " with: " + ex.getLocalizedMessage());
        }
        return null;        
    }  
    
    public static void updateNotificationLog(Integer cid, Integer contact_id, String msg, String msg_type)
    {
        try 
        {
            Connection conn = connectToDatabase(p);   
            java.sql.Statement stmt = conn.createStatement();
            stmt.executeUpdate("INSERT INTO notification_log (cid,contact_id,msg,msg_type) VALUES ("+cid+","+contact_id+",'"+msg+"','"+msg_type+"')");
            conn.close();
        } catch (Exception ex)
        {
            com.clavain.muninmxcd.logger.error("Error in updateNotification Log : " + ex.getLocalizedMessage());
        }   
    }
    
    public static boolean removeAlert(Integer p_alertId)
    {
        boolean retval = false;
        for (Alert l_av : com.clavain.muninmxcd.v_alerts) {
            if(l_av.getAlert_id().equals(p_alertId))
            {
                retval = com.clavain.muninmxcd.v_alerts.remove(l_av);
            }
        }
        return retval;
    }
    
    public static boolean addAlert(Integer p_alertId)
    {
        return dbAddAllAlertWithId(p_alertId);
    }    
}
