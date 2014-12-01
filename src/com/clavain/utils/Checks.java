/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.utils;

import com.clavain.checks.ReturnServiceCheck;
import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.p;
import static com.clavain.utils.Database.connectToDatabase;
import static com.clavain.utils.Database.rowCount;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 *
 * @author enricokern
 */
public class Checks {

    public static boolean checkExistsInDatabase(Integer cid)
    {
        boolean retval = false;
        try 
        {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();
            ResultSet rs;
           
            rs = stmt.executeQuery("SELECT * FROM service_checks WHERE id = " + cid);   
            
            if(rowCount(rs) > 0)
            {
                retval = true;
            }
            conn.close();
        } catch (Exception ex)
        {
            logger.error("Error in checkExistsInDatabase: " + ex.getLocalizedMessage());
            return false;
        }        
        return retval;
    }
        
    public static void addDownTimeToDb(Integer cid, Integer from, Integer to) {
        try {
            Connection conn = connectToDatabase(p);
            java.sql.Statement stmt = conn.createStatement();
            stmt.executeUpdate("INSERT INTO downtimes_durations (check_id,down_at,up_at) VALUES (" + cid + "," + from + "," + to + ")");
            conn.close();
        } catch (Exception ex) {
            logger.error("Error in  addDownTimeToDb for CID " + cid + " with: " + ex.getLocalizedMessage());
        }
    }

    public static void removeCheckFromProcessingList(Integer cid) {
        if (com.clavain.muninmxcd.errorProcessing.remove(cid)) {
            logger.info("Removed " + cid + " from errorProcessing. New Size: " + com.clavain.muninmxcd.errorProcessing.size());
        } else {
            logger.error("I cant remove " + cid + " from errorProcessing. Help me please!");
        }
    }

    public static ReturnServiceCheck RunTestCheck(String p_url, String p_data) {
        ReturnServiceCheck retval = null;
        try {
            URL obj = new URL(p_url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            //add reuqest header
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "MuninMX ErrorNotifyAgent");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            String urlParameters = p_data;

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            //retval = responseCode;
            if (responseCode != 200) {
                return null;
            }

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            Gson gson = new Gson();
            retval = gson.fromJson(response.toString(), ReturnServiceCheck.class);
        } catch (Exception ex) {
            logger.error("Error in RunTestCheck: " + ex.getLocalizedMessage());
            return null;
        }
        return retval;
    }

    public static boolean isServiceCheckDown(ReturnServiceCheck sc) {
        boolean retval = false;
        int failcount = 0;
        String url;
        ReturnServiceCheck rsc;
        String hostname;
        logger.info("[CheckDown " + sc.getCid() + "] Retesting");



        url = "http://localhost:" + p.getProperty("api.port") + "/testcheck/dummy";
        rsc = RunTestCheck(url, sc.getJson());



        rsc = RunTestCheck(url, sc.getJson());
        if (rsc != null) {
            if (rsc.getReturnValue() == 2 || rsc.getReturnValue() == 3) {
                failcount++;
                logger.info("[CheckDown " + sc.getCid() + "] Test Failed on Origin Probe with Code " + rsc.getReturnValue() + " and Output: " + rsc.getOutput().get(0));
            } else {
                logger.info("[CheckDown " + sc.getCid() + "] Test OK on Origin Probe with Code " + rsc.getReturnValue() + " and Output: " + rsc.getOutput().get(0));

            }
        } else {
            logger.error("[CheckDown " + sc.getCid() + "] Unable to retrieve TestCheck  assuming test as failed");
            failcount++;
        }

        if (failcount > 0) {
            retval = true;
        }
        return retval;
    }
}
