/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.alerts;

import java.sql.ResultSet;

/**
 *
 * @author enricokern
 */
public class Helpers {

    public static void withdrawSMSTicket(Integer user_id) {
        try {
            java.sql.Statement stmt = com.clavain.muninmxcd.conn.createStatement();
            stmt.executeUpdate("UPDATE users SET sms_tickets = sms_tickets -1 WHERE id = " + user_id);
        } catch (Exception ex) {
            com.clavain.muninmxcd.logger.error("Error in withdrawSMS Ticket for User: " + user_id + " with: " + ex.getLocalizedMessage());
        }
    }
    
    public static void withdrawTTSTicket(Integer user_id)
    {
        try 
        {
            java.sql.Statement stmt = com.clavain.muninmxcd.conn.createStatement();
            stmt.executeUpdate("UPDATE users SET tts_tickets = tts_tickets -1 WHERE id = " + user_id);
        } catch (Exception ex)
        {
            com.clavain.muninmxcd.logger.error("Error in withdrawTTS Ticket for User: " + user_id + " with: " + ex.getLocalizedMessage());
        }
    }  
    
    public static int getTTSTicketCount(Integer user_id)
    {
        try 
        {
            java.sql.Statement stmt = com.clavain.muninmxcd.conn.createStatement();
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
            java.sql.Statement stmt = com.clavain.muninmxcd.conn.createStatement();
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
            java.sql.Statement stmt = com.clavain.muninmxcd.conn.createStatement();
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
            java.sql.Statement stmt = com.clavain.muninmxcd.conn.createStatement();
            stmt.executeUpdate("INSERT INTO notification_log (cid,contact_id,msg,msg_type) VALUES ("+cid+","+contact_id+",'"+msg+"','"+msg_type+"')");
        } catch (Exception ex)
        {
            com.clavain.muninmxcd.logger.error("Error in updateNotification Log : " + ex.getLocalizedMessage());
        }   
    }
    
    public static boolean removeAlert(Integer p_alertId)
    {
        boolean retval = false;
        
        return retval;
    }
}
