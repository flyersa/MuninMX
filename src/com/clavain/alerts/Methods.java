/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.alerts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.clavain.muninmxcd;
import com.clavain.alerts.msg.PushOverMessage;
import com.clavain.alerts.msg.ShortTextMessage;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.GregorianCalendar;
import net.pushover.client.MessagePriority;
import net.pushover.client.PushoverClient;
import net.pushover.client.PushoverException;
import net.pushover.client.PushoverMessage;
import net.pushover.client.PushoverRestClient;
import net.pushover.client.Status;
import static com.clavain.utils.Generic.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import com.clavain.alerts.msg.TTSMessage;
import java.lang.reflect.Modifier;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.p;

/**
 *
 * @author enricokern
 */
public class Methods {

    public static void sendNotifications(ReturnServiceCheck sc, boolean flapalert, int flapcount) {
        Integer cid = sc.getCid();
        try {
            java.sql.Statement stmt = com.clavain.muninmxcd.conn.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT notifications.id as nid, contacts.* FROM `notifications` LEFT JOIN contacts ON notifications.contact_id = contacts.id WHERE check_id = " + cid);
            while (rs.next()) {
                Integer contact_id = rs.getInt("id");
                String dayField = getScheduleFieldToCheck();
                logger.info("[Notifications " + cid + "] Found " + rs.getString("contact_name"));
                if (rs.getString(dayField).equals("disabled")) {
                    logger.info("[Notifications " + cid + "] " + rs.getString("contact_name") + " disabled notifications for today - skipping contact");
                } else {
                    String splitField = rs.getString(dayField);
                    // figure out if this user got notifications enabled or disabled for the current hour and time
                    String[] hours = splitField.split(";");
                    long a = getStampFromTimeAndZone(hours[0], rs.getString("timezone"));
                    long b = getStampFromTimeAndZone(hours[1], rs.getString("timezone"));
                    long cur = (System.currentTimeMillis() / 1000L);
                    // if in range send notifications
                    if (a < cur && b > cur) {
                        String failTime = getHumanReadableDateFromTimeStampWithTimezone(sc.getDownTimeConfirmedAt(), rs.getString("timezone"));
                        String title = "ALERT: " + sc.getCheckname() + " (" + sc.getChecktype() + ")";
                        String message = "Service Downtime verified @ " + failTime + ".   Details: " + sc.getOutput().get(0);
                        if (flapalert) {
                            message = "Service flapped " + flapcount + " on different probes within the last hour";
                        }
                        String json = "";
                        if (rs.getInt("callback_active") == 1) {
                            logger.info("[Notifications " + cid + "] " + rs.getString("contact_name") + " Sending Callback");
                            sendCallback(sc, rs.getString("contact_callback"));
                            updateNotificationLog(cid, contact_id, "Callback executed to " + rs.getString("contact_callback"), "callback", false);
                        }
                        if (rs.getInt("tts_active") == 1) {
                            title = "This is a PingReports Alert: The Servicecheck: " + sc.getCheckname() + " with type: " + sc.getChecktype() + " is in alert state.";
                            logger.info("[Notifications " + cid + "] " + rs.getString("contact_name") + " Initiating TTS Call");
                            sendTTS(title, message, rs.getString("contact_mobile_nr"), rs.getInt("user_id"));
                            updateNotificationLog(cid, contact_id, "Text2Speech Call initiated to " + rs.getString("contact_mobile_nr"), "tts", false);
                        }
                        if (rs.getInt("email_active") == 1) {
                            logger.info("[Notifications " + cid + "] " + rs.getString("contact_name") + " Sending E-Mail");
                            String ENDL = System.getProperty("line.separator");
                            message = "Service Downtime verified @ " + failTime + "." + ENDL + ENDL + "Details:" + ENDL + ENDL + sc.getOutput().get(0);
                            if (flapalert) {
                                message = "Service flapped " + flapcount + " on different probes within the last hour";
                            }
                            sendMail(title, message, rs.getString("contact_email"));
                            updateNotificationLog(cid, contact_id, "E-Mail send to " + rs.getString("contact_email"), "email", false);
                        }
                        if (rs.getInt("sms_active") == 1) {
                            title = sc.getCheckname() + "(" + sc.getChecktype() + ")";
                            message = sc.getOutput().get(0);
                            if (flapalert) {
                                message = "Service flapped " + flapcount + " on different probes within the last hour";
                            }
                            logger.info("[Notifications " + cid + "] " + rs.getString("contact_name") + " Sending SMS");
                            sendSMS(title, message, rs.getString("contact_mobile_nr"), rs.getInt("user_id"));
                            updateNotificationLog(cid, contact_id, "SMS send to " + rs.getString("contact_mobile_nr"), "sms", false);
                        }
                        if (rs.getInt("pushover_active") == 1) {
                            logger.info("[Notifications " + cid + "] " + rs.getString("contact_name") + " Sending Pushover Notification");
                            sendPushover(title, message, rs.getString("pushover_key"));
                            updateNotificationLog(cid, contact_id, "PushOver Message send to " + rs.getString("pushover_key"), "pushover", false);
                        }
                        logger.info("[Notifications " + cid + "] " + rs.getString("contact_name") + " skipping. A flap notification was already send within the last hour");

                    } else {
                        logger.info("[Notifications " + cid + "] " + rs.getString("contact_name") + " disabled notifications for this timerange - skipping contact");
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Error in sendNotifications for CID " + cid + " : " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    private static void sendPushover(String title, String message, String userKey) {
        PushOverMessage pom = new PushOverMessage(userKey, title, message);
        com.clavain.muninmxcd.notification_pushover_queue.add(pom);
        //sendPushOverMessage(userKey, title, message);
    }

    private static void sendMail(String title, String message, String emailaddy) {
        try {
            Email email = new SimpleEmail();
            email.setHostName(p.getProperty("mailserver.host"));
            email.setSmtpPort(25);
            email.setAuthentication(p.getProperty("mailserver.user"), p.getProperty("mailserver.pass"));
            email.setSSLOnConnect(false);
            email.setFrom(p.getProperty("mailserver.from"));
            email.setSubject(title);
            email.setMsg(message);
            email.addTo(emailaddy);
            email.send();
        } catch (Exception ex) {
            logger.warn("Unable to send Mail: " + ex.getLocalizedMessage());
        }
    }

    // send a SMS and withdraw one token
    private static void sendSMS(String title, String message, String mobile, Integer user_id) {
        if (getSMSTicketCount(user_id) > 0) {
            message = title + " - " + message;
            ShortTextMessage sms = new ShortTextMessage(message, mobile);
            com.clavain.muninmxcd.notification_sms_queue.add(sms);
            withdrawSMSTicket(user_id);
        } else {
            String email = getCustomerEMail(user_id);
            if (email != null) {
                sendMail("MuninMX cant send SMS to you", "Hello. You receive this Mail because we tried to send you a SMS alert, but you do not have any sms tickets left. Please add more sms tickets or disable sms notifications", email);
            }
        }
    }

    // initiate TTS call and withdraw one token
    private static void sendTTS(String title, String message, String mobile, Integer user_id) {
        if (getTTSTicketCount(user_id) > 0) {
            message = title + " - " + message;
            TTSMessage tts = new TTSMessage(message, mobile);
            com.clavain.muninmxcd.notification_tts_queue.add(tts);
            withdrawTTSTicket(user_id);
        } else {
            String email = getCustomerEMail(user_id);
            if (email != null) {
                sendMail("MuninMX cant initiate text to speech calls to you", "Hello. You receive this Mail because we tried to send you a TTS alert, but you do not have any tts tickets left. Please add more tts tickets or disable tts notifications", email);
            }
        }
    }

    // send a http json callback to a url
    private static void sendCallback(ReturnServiceCheck sc, String url) {
        Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
        String json = gson.toJson(sc);
        sendPost(url, json);
    }

    private static String getScheduleFieldToCheck() {
        GregorianCalendar gc = new GregorianCalendar();
        String retval = "";

        switch (gc.get(Calendar.DAY_OF_WEEK)) {
            case 1:
                retval = "s_mon";
                break;
            case 2:
                retval = "s_tue";
                break;
            case 3:
                retval = "s_wed";
                break;
            case 4:
                retval = "s_thu";
                break;
            case 5:
                retval = "s_fri";
                break;
            case 6:
                retval = "s_sat";
                break;
            case 7:
                retval = "s_sun";
                break;
        }
        return retval;
    }

    public static boolean sendSMSMessage(String message, String mobile_nr) {
        boolean retval = false;
        // http://smsflatrate.net/schnittstelle.php?key=386594884a42f5063f18a7e8289d211a&to=00491734631526&type=4&text=ALERT:%20Notification%20Test%20(HTTPS)%20-%20HTTP%20Critical-%20OK%20String%20not%20found
        try {
            String key = p.getProperty("smsflatrate.key");
            String gw = p.getProperty("smsflatrate.gw");
            if (mobile_nr.startsWith("0049")) {
                gw = p.getProperty("smsflatrate.gwde");
            }
            String msg = URLEncoder.encode(message);
            URL url = new URL("http://smsflatrate.net/schnittstelle.php?key=" + key + "&to=" + mobile_nr + "&type=" + gw + "&text=" + msg);
            URLConnection conn = url.openConnection();
            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            String resp = "";
            while ((inputLine = br.readLine()) != null) {
                resp = inputLine;
            }
            //conn.getContent();      
            if (resp.trim().equals("100")) {
                retval = true;
            }
        } catch (Exception ex) {
            retval = false;
            logger.error("sendSMSMessage Error: " + ex.getLocalizedMessage());
        }
        return retval;
    }

    public static boolean sendTTSMessage(String message, String mobile_nr) {
        boolean retval = false;

        try {
            String key = p.getProperty("smsflatrate.key");
            String gw = p.getProperty("smsflatrate.ttsgw");

            String msg = URLEncoder.encode(message);
            URL url = new URL("http://smsflatrate.net/schnittstelle.php?key=" + key + "&to=" + mobile_nr + "&voice=Dave&repeat=2&rate=1&type=" + gw + "&text=" + msg);
            URLConnection conn = url.openConnection();
            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            String resp = "";
            while ((inputLine = br.readLine()) != null) {
                resp = inputLine;
            }
            //conn.getContent();      
            if (resp.trim().equals("100")) {
                retval = true;
            }
        } catch (Exception ex) {
            retval = false;
            logger.error("sendTTSMessage Error: " + ex.getLocalizedMessage());
        }
        return retval;
    }

    public static Status sendPushOverMessage(String userKey, String title, String Message) {
        PushoverClient client = new PushoverRestClient();
        try {

            Status result = client.pushMessage(PushoverMessage.builderWithApiToken(p.getProperty("pushover.key"))
                    .setUserId(userKey)
                    .setMessage(Message)
                    .setPriority(MessagePriority.HIGH) // HIGH|NORMAL|QUIET
                    .setTitle(title)
                    .build());
            return result;


        } catch (PushoverException ex) {
            logger.error("PushOver Error: " + ex.getLocalizedMessage());
            return null;
        }
    }
}
