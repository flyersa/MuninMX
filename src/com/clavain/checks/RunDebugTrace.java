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
