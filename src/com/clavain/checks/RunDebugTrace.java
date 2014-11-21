/*
 * Copyright (c) 2014 by Clavain Technologies GbR.
 * http://www.clavain.com
 */
package com.clavain.checks;

import static com.clavain.muninmxcd.logger;
import com.clavain.json.ServiceCheck;
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
public class RunDebugTrace {
    private ServiceCheck sc;
    
    public RunDebugTrace(ServiceCheck p_sc)
    {
        sc = p_sc;
    }
    
    public ReturnDebugTrace execute()
    {
        List<String> commands = new ArrayList<String>();
        
        // ping?
        if(sc.getChecktype() == 1)
        {
            commands.add("mtr");
            commands.add("--report-wide");
            commands.add("--report-cycles=5");
            commands.add("--report");
            commands.add(sc.getNonearg());
        }
        // http?
        else if(sc.getChecktype() == 2)
        {
            commands.add("curl");
            commands.add("-IL");
            commands.add("--connect-timeout");
            commands.add("10");
            
            // build url
            String proto = "http://";
            String host = "";
            String url = "";
            String port = "";
            String uri = "";
            String user = "";
            for (String s: sc.getParam())
            {
                StringTokenizer st = new StringTokenizer(s,"|##|");  
                while(st.hasMoreTokens())
                {
                    String token = st.nextToken();
                    if(!token.trim().equals(""))
                    {
                        if(token.equals("-S"))
                        {
                            proto = "https://";
                        }
                        else if(token.equals("-H"))
                        {
                            host = st.nextToken();
                        }
                        else if(token.equals("-p"))
                        {
                            port = ":" + st.nextToken();
                        }
                        else if(token.equals("-a"))
                        {
                           user = st.nextToken() + "@";
                        }  
                        else if(token.equals("-u"))
                        {
                           uri = st.nextToken();
                        }                           
                    }
                }                    
            }
            url = proto + user + host + port + uri;
            commands.add(url);
        }
        else
        {
            return null;
        }
        
        try
        {
            Integer curTime = getUnixtime();
            ProcessBuilder pb = new ProcessBuilder(commands).redirectErrorStream(true);
            logger.info("Running DebugTrace: " + commands);
            Process p = pb.start();
            InputStream is = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            // print output
            String str_output = null;
            String str_parse = "";
            while ((str_output = reader.readLine()) != null) {
                //logger.info(str_output);
                str_parse = str_parse + str_output + System.lineSeparator();
            }     
            ReturnDebugTrace rdt = new ReturnDebugTrace();
            rdt.setChecktime(curTime);
            rdt.setOutput(str_parse);
            rdt.setCid(sc.getCid());
            rdt.setUser_id(sc.getUser_id());
            reader.close();
            int rv = p.waitFor();
            rdt.setRetval(rv);
            return rdt;
            
        } catch (Exception ex)
        {
            logger.error("Error in RDT Execute: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            return null;            
        }
    }
}
