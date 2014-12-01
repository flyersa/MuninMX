/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.checks;

import com.clavain.json.ServiceCheck;
import static com.clavain.muninmxcd.logger;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import static com.clavain.utils.Generic.getUnixtime;

/**
 *
 * @author enricokern
 */
public class RunServiceCheck {
    
    private ServiceCheck sc;
    
    public RunServiceCheck(ServiceCheck p_sc)
    {
        sc = p_sc;
    }
    
    public ReturnServiceCheck execute()
    {
        // build check
        String str_lcheck= com.clavain.muninmxcd.p.getProperty("nagios.plugins") + sc.getCommand();
        
        // build command
        List<String> commands = new ArrayList<String>();
        List<String> list_Return= new ArrayList<String>();
        
        // add global timeout script
        commands.add("/opt/muninmx/timeout.sh");
        commands.add("-t");
        commands.add("20");
        
        commands.add(str_lcheck);
        
        try
        {   
            // no parameters and only nonearg given
            if(sc.getNonearg() != null)
            {
                commands.add(sc.getNonearg());
            }
            
            // add parameters
            if(sc.getParam() != null)
            {
                for (String s: sc.getParam())
                {
                    StringTokenizer st = new StringTokenizer(s,"|##|");  
                    while(st.hasMoreTokens())
                    {
                        String token = st.nextToken();
                        logger.debug("Adding " + token);
                        if(!token.trim().equals(""))
                        {
                            commands.add(token);
                        }
                    }                    
                }
            }
            
            Integer curTime =  getUnixtime();
            ProcessBuilder pb = new ProcessBuilder(commands).redirectErrorStream(true);
            logger.info("[RunServiceCheck] Running: " + commands);
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
            // TODO: parse nagios plugin output, ignore last
            StringTokenizer st = new StringTokenizer(str_parse,"|");

            while(st.hasMoreTokens())
            {
               list_Return.add(st.nextToken().toString());
            }

            
            reader.close();
            Integer rv = p.waitFor();
            
            logger.debug(list_Return);
            //list_Return.add(str_parse);
            
            // rerun to eliminate false positives
            if(com.clavain.muninmxcd.p.getProperty("nagios.rerun").equals("true"))
            {
                if(rv > 0)
                {
                   list_Return.clear();
                   pb = new ProcessBuilder(commands).redirectErrorStream(true);
                   logger.info("[RunServiceCheck] Re-Running (because last was error and nagios.rerun is true): " + commands);
                   p = pb.start();
                   is = p.getInputStream();
                   reader = new BufferedReader(new InputStreamReader(is));
                   // print output
                   str_output = null;
                   str_parse = null;
                   while ((str_output = reader.readLine()) != null) {
                       //logger.info(str_output);
                       str_parse = str_output;
                   }  
                   // TODO: parse nagios plugin output, ignore last
                   st = new StringTokenizer(str_parse,"|");

                   while(st.hasMoreTokens())
                   {
                      list_Return.add(st.nextToken().toString());
                   }


                   reader.close();
                   rv = p.waitFor();

                   logger.debug(list_Return);
                   //list_Return.add(str_parse);               
                }
            }
            
            ReturnServiceCheck rsc = new ReturnServiceCheck(rv,list_Return);
           
            rsc.setChecktime(curTime);
            rsc.setCid(sc.getCid());
            rsc.setUser_id(sc.getUser_id());
            return rsc;
            
        } catch (Exception ex)
        {
            logger.error("[RunServiceCheck] Error in RSC Execute: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            return null;
        }
    }
}
