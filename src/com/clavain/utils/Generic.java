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

import com.clavain.alerts.Alert;
import com.clavain.json.ServiceCheck;
import com.clavain.munin.MuninNode;
import com.clavain.munin.MuninPlugin;
import java.util.Iterator;

import static com.clavain.muninmxcd.p;
import static com.clavain.muninmxcd.v_munin_nodes;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 *
 * @author enricokern
 */
public class Generic {
      public static ServiceCheck returnServiceCheck(Integer p_cid)
      {
          for (ServiceCheck l_sc : com.clavain.muninmxcd.v_serviceChecks) {
              if(l_sc.getCid().equals(p_cid))
              {
                  return l_sc;
              }
          }
          return null;

    }
      
    public static boolean checkIsProcessing(Integer cid)
    {
        boolean retval = false;
        
        if(com.clavain.muninmxcd.errorProcessing.size() < 1)
        {
            return false;
        }
        
        Iterator<Integer> it = com.clavain.muninmxcd.errorProcessing.iterator();

        while(it.hasNext())
        {
            Integer check = it.next();
            if(cid == check)
            {
                return true;
            }
        }
        return retval;
    }
    
    public static String getHumanReadableDateFromTimeStampWithTimezone(long ts,String Timezone)
    {
         Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone(Timezone));
         cal.setTimeInMillis(ts * 1000);
         String retval = getConvertedCalendar(cal);
         retval = retval + " " + Timezone;
         return retval;
    }
    
    public static String getConvertedCalendar(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        sdf.setTimeZone(calendar.getTimeZone());
        return sdf.format(calendar.getTime());  
    }        
    
    public static int sendPost(String p_url, String p_data) {
        int retval = 0;
        try {
            URL obj = new URL(p_url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            //add reuqest header
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", "MuninMX Collector");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
           
            String urlParameters = p_data;

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            retval = responseCode;
            com.clavain.muninmxcd.logger.info("sendPost executed to " + p_url + " Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (Exception ex) {
            com.clavain.muninmxcd.logger.warn("Error in sendPost " + ex.getLocalizedMessage());
        }
        return retval;
    }

    /**
     * return unix timestamp
     */
    public static int getUnixtime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }       

    /**
     * Return a MuninNode by Nodename or Hostname
     *
     */
    public static MuninNode getMuninNode(String p_strNodename) {
        for (MuninNode l_mn : com.clavain.muninmxcd.v_munin_nodes) {
            if (l_mn.getNodename().equals(p_strNodename) || l_mn.getHostname().equals(p_strNodename)) {
                return l_mn;
            }
        }
        return null;
    }

    /**
     * Return a MuninPlugin for custom jobs by Custom ID
     *
     * @param customId
     * @return
     */
    public static MuninPlugin getMuninPluginForCustomJob(Integer customId) {
        Iterator it = com.clavain.muninmxcd.v_cinterval_plugins.iterator();
        while (it.hasNext()) {
            MuninPlugin l_mp = (MuninPlugin) it.next();
            if (l_mp.getCustomId().equals(customId)) {
                return l_mp;
            }
        }
        com.clavain.muninmxcd.logger.error("getMuninPluginForCustomJob: Cant find plugin for custom id: " + customId);
        return null;
    }

    
    public static String getNodeHostNameForMuninNode(Integer nodeId)
    {
        MuninNode mn = getMuninNode(nodeId);
        if(mn == null)
        {
            return "";
        }
        else
        {
            String r_hostname = mn.getHostname();
            return r_hostname;
        }
    }
    
    public static ArrayList<Alert> getAlertByNidPluginAndGraph(Integer p_nid,String p_strPlugin, String p_strGraph)
    {
        ArrayList<Alert> retval = new ArrayList<>();
        for (Alert l_av : com.clavain.muninmxcd.v_alerts) {
            if (l_av.getNode_id().equals(p_nid)) {      
                if(l_av.getPluginName().trim().equals(p_strPlugin) && l_av.getGraphName().trim().equals(p_strGraph))
                {
                    retval.add(l_av);
                }
            }
        }     

        return retval;

    }
    
    /**
     * Return a MuninNode by Node ID
     *
     * @param nodeId
     * @return
     */
    public static MuninNode getMuninNode(Integer nodeId) {
        Iterator it = v_munin_nodes.iterator();
        while (it.hasNext()) {
            MuninNode l_mn = (MuninNode) it.next();
            if (l_mn.getNode_id().equals(nodeId)) {
                return l_mn;
            }
        }
        com.clavain.muninmxcd.logger.warn("getMuninNode: Cant find nodeId " + nodeId);
        return null;
    }

    public static boolean isPluginIgnored(String pluginName) {
        boolean retval = false;
        if (p.getProperty("ignore.plugins") != null) {
            StringTokenizer st = new StringTokenizer(p.getProperty("ignore.plugins"), ",");
            while (st.hasMoreElements()) {
                if (st.nextElement().toString().equals(pluginName)) {
                    return true;
                }
            }
        }
        return retval;
    }

    public static long getStampFromTimeAndZone(String time, String zone) {
        long retval = 0;
        ///usr/local/bin/php /opt/pcvd/getTime.php
        List<String> commands = new ArrayList<String>();
        commands.add("/usr/bin/php");
        commands.add("/opt/muninmx/getTime.php");
        commands.add(zone);
        commands.add(time);
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            Process p = pb.start();
            InputStream is = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            // print output
            String str_output = null;
            String str_parse = null;
            while ((str_output = reader.readLine()) != null) {
                //logger.info(str_output);
                str_parse = str_output;
            }
            com.clavain.muninmxcd.logger.info("getTime.php returned: " + str_parse);
            retval = Long.parseLong(str_parse);
        } catch (Exception ex) {
            com.clavain.muninmxcd.logger.error("Error in getStampFromTimeAndZone:" + ex.getLocalizedMessage());
        }
        return retval;
    }
}
