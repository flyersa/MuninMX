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
package com.clavain.handlers;

import com.clavain.munin.MuninNode;
import com.clavain.munin.MuninPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import static com.clavain.utils.Generic.*;
import static com.clavain.utils.Database.getMuninNodeFromDatabase;
import static com.clavain.utils.Database.dbUpdateAllPluginsForNode;
import static com.clavain.muninmxcd.v_munin_nodes;
import static com.clavain.utils.Quartz.*;
import static com.clavain.utils.Generic.convertStreamToString;
import java.net.URLDecoder;
import static com.clavain.alerts.Methods.sendPushOverMessage;
import com.clavain.checks.RunServiceCheck;
import com.clavain.json.ServiceCheck;
import static com.clavain.muninmxcd.p;
import com.clavain.rca.Analyzer;
import java.io.FileInputStream;

/**
 *
 * @author phantom
 */
public class JettyHandler extends AbstractHandler {

    Logger logger = Logger.getRootLogger();
    public static HttpServletResponse response;
    //public static PrintWriter writer;

    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse p_response)
            throws IOException, ServletException {
        if (!target.equals("/favicon.ico")) {
            logger.info("Request from [" + baseRequest.getRemoteAddr() + "] : " + target);
        }
        
        response = p_response;
        response.setContentType("text/html;charset=utf-8");
        response.setHeader("Server", "MuninMXcd" + com.clavain.muninmxcd.version);
        response.setHeader("Access-Control-Allow-Origin", "*");

        //writer = response.getWriter();

        //baseRequest.setHandled(true);

        List l_lTargets = new ArrayList();
        StringTokenizer st = new StringTokenizer(target, "/");
        while (st.hasMoreTokens()) {
            String l_strToken = st.nextToken();
            l_lTargets.add(l_strToken);
            //response.getWriter().println(st.nextToken());
        }

        // multi request?
        if (l_lTargets.size() > 1) {
            // listings
            if (l_lTargets.get(0).equals("list")) {
                // list nodes
                if (l_lTargets.get(1).equals("nodes")) {
                    if (l_lTargets.size() > 2) {
                        listNodes(l_lTargets.get(2).toString(), baseRequest);
                    } else {
                        listNodes(baseRequest);
                    }
                } // custom interval plugins
                else if (l_lTargets.get(1).equals("customintervals")) {

                    try (PrintWriter writer = response.getWriter()) {
                        writeJsonWithTransient(com.clavain.muninmxcd.v_cinterval_plugins, writer);
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                }
                else if (l_lTargets.get(1).equals("maxnodes")) {

                    try (PrintWriter writer = response.getWriter()) {
                        writeJson(com.clavain.muninmxcd.maxnodes, writer);
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                } 
                else if (l_lTargets.get(1).equals("ttsenabled")) {
                    try (PrintWriter writer = response.getWriter()) {
                        if(com.clavain.muninmxcd.p.getProperty("tts.enable").equals("true"))
                        {
                            writeJson(true, writer);
                        }
                        else
                        {
                            writeJson(false, writer);
                        }
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                }   
                else if (l_lTargets.get(1).equals("smsenabled")) {
                    try (PrintWriter writer = response.getWriter()) {
                        if(com.clavain.muninmxcd.p.getProperty("sms.enable").equals("true"))
                        {
                            writeJson(true, writer);
                        }
                        else
                        {
                            writeJson(false, writer);
                        }
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                }       
                
            } 
            // configuration stuff
            else if (l_lTargets.get(0).equals("config"))
            {
                if(l_lTargets.get(1).equals("reload"))
                {
                    boolean retval = false;
                    try
                    {
                        FileInputStream propInFile = null;
                        propInFile = new FileInputStream(com.clavain.muninmxcd.initialArgs[0]);
                        p.loadFromXML(propInFile); 
                        retval = true;
                    } catch (Exception ex)
                    {
                        logger.error("Error reloading config: " + ex.getLocalizedMessage());
                    }
                    
                    try (PrintWriter writer = response.getWriter()) {
                        writeJson(retval,writer);
                    } catch (Exception ex) {
                       baseRequest.setHandled(true);
                    } finally {
                       baseRequest.setHandled(true);
                    }                    
                }
                else if(l_lTargets.get(1).equals("list"))
                {
                    try (PrintWriter writer = response.getWriter()) {
                        writeJson(p,writer);
                    } catch (Exception ex) {
                       baseRequest.setHandled(true);
                    } finally {
                       baseRequest.setHandled(true);
                    }                        
                }
            }
            // query a node
            else if (l_lTargets.get(0).equals("node")) {
                //logger.debug(l_lTargets.size());
                MuninNode mn = null;
                if (l_lTargets.size() <= 2) {
                    mn = returnNode(Integer.parseInt(l_lTargets.get(1).toString()), true, baseRequest);
                } else {
                    // retrieve available munin plugins for this node
                    mn = returnNode(Integer.parseInt(l_lTargets.get(1).toString()), false, baseRequest);
                    if (l_lTargets.get(2).equals("plugins") && mn != null) {
                        logger.info("query plugins for " + mn.getNodename());

                        try (PrintWriter writer = response.getWriter()) {
                            writeJson(mn.getPluginList(), writer);
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }
                    } else if (l_lTargets.get(2).equals("loadplugins") && mn != null) {
                        logger.info("loading  plugins for " + mn.getNodename());
                        try (PrintWriter writer = response.getWriter()) {
                            writeJson(mn.loadPlugins(), writer);
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }

                        dbUpdateAllPluginsForNode(mn);
                    } // return graphs for a given plugin, start up node if not running
                    else if (l_lTargets.get(2).equals("updateAll") && mn != null) {
                        mn.run();
                        try (PrintWriter writer = response.getWriter()) {
                            writeJson(true, writer);
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }                        
                    }
                    else if (l_lTargets.get(2).equals("fetch") && l_lTargets.size() == 4 && mn != null) {
                        logger.debug("query plugin: " + l_lTargets.get(3) + " for node: " + mn.getNodename());
                        try (PrintWriter writer = response.getWriter()) {
                            fetchPlugin(mn, l_lTargets.get(3).toString(), baseRequest);
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }

                    }
                }
            } 
            // check commands
            else if (l_lTargets.get(0).equals("check")) {
                ServiceCheck sc = null;
                if (l_lTargets.size() <= 2) {
                    sc = returnServiceCheck(Integer.parseInt(l_lTargets.get(1).toString()));
                    try (PrintWriter writer = response.getWriter()) {
                        writeJson(sc, writer);
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                       baseRequest.setHandled(true);
                    }                                        
                } else {  
                   sc = returnServiceCheck(Integer.parseInt(l_lTargets.get(2).toString()));
                   if (l_lTargets.get(1).equals("pause") && sc != null) 
                   {
                        logger.info("Pausing Check " + sc.getCheckname());
                        sc.setIs_active(false);
                        try (PrintWriter writer = response.getWriter()) {
                            writeJson(true, writer);
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }  
                   }  
                   else if(l_lTargets.get(1).equals("continue") && sc != null)
                   {
                        logger.info("Continue Check " + sc.getCheckname());
                        sc.setIs_active(true);
                        try (PrintWriter writer = response.getWriter()) {
                            writeJson(true, writer);
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }                         
                   }
                   else if(l_lTargets.get(1).equals("dequeue") && sc != null)
                   {
                        logger.info("dequeue Check " + sc.getCheckname());
                        sc.setIs_active(false);
                        boolean uret = unscheduleServiceCheck(sc.getCid().toString(), sc.getUser_id().toString());
                        try (PrintWriter writer = response.getWriter()) {
                            if(uret)
                            {
                                com.clavain.muninmxcd.v_serviceChecks.remove(sc);
                                writeJson(true, writer);
                            }
                            else
                            {
                                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                writeJson(false, writer);    
                            }
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }                         
                   }   
                   else if(l_lTargets.get(1).equals("queue"))
                   {
                        
                        logger.info("queue Check with id " + l_lTargets.get(2).toString());
                        // check if we have space for nodes
                        boolean uret = false;
                        if(com.clavain.muninmxcd.v_serviceChecks.size() < com.clavain.muninmxcd.maxchecks)
                        {
                            uret = isServiceCheckScheduled(Integer.parseInt(l_lTargets.get(2).toString()));
                            if(uret == true)
                            {
                                logger.info("skipping queue Check with id " + l_lTargets.get(2).toString() + " already scheduled");
                            }
                            if(uret == false)
                            {
                                uret = scheduleServiceCheck(Integer.parseInt(l_lTargets.get(2).toString()));
                            }
                        }
                        else
                        {
                            logger.fatal("License Limit for ServiceChecks: " + com.clavain.muninmxcd.maxchecks + " reached with: " + com.clavain.muninmxcd.v_serviceChecks.size() + " active checks. wont queue new checks");
                            uret = false;
                        }
                        try (PrintWriter writer = response.getWriter()) {
                            if(uret)
                            {
                                writeJson(true, writer);
                            }
                            else
                            {
                                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                                writeJson(false, writer);    
                            }
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }                         
                   }                     
                }
            }
            // test check
            else if(l_lTargets.get(0).equals("testcheck"))
            {
               String json = convertStreamToString(request.getInputStream());
               if(!json.trim().equals(""))
               {
                Gson gson = new Gson();
                ServiceCheck tc = gson.fromJson(json, ServiceCheck.class);

                RunServiceCheck rsc = new RunServiceCheck(tc);
                try(PrintWriter writer = response.getWriter()) {
                writeJson(rsc.execute(),writer);
                } finally { baseRequest.setHandled(true); } 
               }
               else
               {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try(PrintWriter writer = response.getWriter()) {
                writer.println("no post data received");    
                } finally { baseRequest.setHandled(true); } 
               }
            }   
            // get joblist
            else if (l_lTargets.get(0).equals("joblist")) {
                try (PrintWriter writer = response.getWriter()) {
                    writeJson(getScheduledJobs(), writer);
                } catch (Exception ex) {
                    baseRequest.setHandled(true);
                } finally {
                    baseRequest.setHandled(true);
                }
            } 
            // get servicecheck list
            else if (l_lTargets.get(0).equals("checklist")) {
                try (PrintWriter writer = response.getWriter()) {
                    writeJson(com.clavain.muninmxcd.v_serviceChecks, writer);
                } catch (Exception ex) {
                    baseRequest.setHandled(true);
                } finally {
                    baseRequest.setHandled(true);
                }
            }             
            else if (l_lTargets.get(0).equals("alertlist")) {
                try (PrintWriter writer = response.getWriter()) {
                    writeJson(com.clavain.muninmxcd.v_alerts, writer);
                } catch (Exception ex) {
                    baseRequest.setHandled(true);
                } finally {
                    baseRequest.setHandled(true);
                }
            }                 
            // get joblist
            else if (l_lTargets.get(0).equals("customjoblist")) {

                try (PrintWriter writer = response.getWriter()) {
                    writeJsonWithTransient(getScheduledCustomJobs(), writer);
                } catch (Exception ex) {
                    baseRequest.setHandled(true);
                } finally {
                    baseRequest.setHandled(true);
                }
            } // add a new job
            else if (l_lTargets.get(0).equals("queuejob")) {
                if (l_lTargets.size() < 2) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                    try (PrintWriter writer = response.getWriter()) {
                        writer.println("no node id specified");
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                } else {
                    // check if we have space for nodes
                    if(com.clavain.muninmxcd.v_munin_nodes.size() < com.clavain.muninmxcd.maxnodes)
                    {
                        Integer nodeId = Integer.parseInt(l_lTargets.get(1).toString());
                        MuninNode l_mn = getMuninNodeFromDatabase(nodeId);
                        unscheduleCheck(l_mn.getNode_id().toString(), l_mn.getUser_id().toString());
                        com.clavain.muninmxcd.v_munin_nodes.add(l_mn);
                        if (scheduleJob(l_mn)) {
                            try (PrintWriter writer = response.getWriter()) {
                                writeJson(true, writer);
                            } catch (Exception ex) {
                                baseRequest.setHandled(true);
                            } finally {
                                baseRequest.setHandled(true);
                            }
                        } else {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                            try (PrintWriter writer = response.getWriter()) {
                                writer.println("queue error");
                            } catch (Exception ex) {
                                baseRequest.setHandled(true);
                            } finally {
                                baseRequest.setHandled(true);
                            }
                        }
                    }
                    else
                    {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        try (PrintWriter writer = response.getWriter()) {
                            writer.println("License Error. Maximum Number of nodes in license reached");
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                           baseRequest.setHandled(true);
                        }                        
                    }
                }
            } // add a new customjob
            else if (l_lTargets.get(0).equals("queuecustomjob")) {
                if (l_lTargets.size() < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

                    try (PrintWriter writer = response.getWriter()) {
                        writer.println("no custom id and/or user id specified. syntax: queuecustomjob/$customid/$userid");
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                } else {
                    Integer customId = Integer.parseInt(l_lTargets.get(1).toString());
                    Integer userId = Integer.parseInt(l_lTargets.get(2).toString());
                    unscheduleCustomJob(customId.toString(), userId.toString());
                    //com.clavain.muninmxcd.v_munin_nodes.add(l_mn);
                    if (scheduleCustomIntervalJob(customId)) {
                        try (PrintWriter writer = response.getWriter()) {
                            writeJson(true, writer);
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }

                    } else {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        try (PrintWriter writer = response.getWriter()) {
                            writer.println("queue error");
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }

                    }
                }
            } // delete a job
            else if (l_lTargets.get(0).equals("deletejob")) {
                if (l_lTargets.size() < 2) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter writer = response.getWriter()) {
                        writer.println("no node id and user_id specified");
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                } else {
                    // nodeid, userid
                    boolean ucRet = unscheduleCheck(l_lTargets.get(1).toString(), l_lTargets.get(2).toString());
                    if (!ucRet) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        try (PrintWriter writer = response.getWriter()) {
                            writer.println("unable to delete check, maybe its already deleted");
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }
                    } else {
                        Integer nodeId = Integer.parseInt(l_lTargets.get(1).toString());
                        try (PrintWriter writer = response.getWriter()) {
                            writeJson(ucRet, writer);
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }
                        com.clavain.muninmxcd.v_munin_nodes.remove(getMuninNode(nodeId));

                    }
                }
            } // delete a job
            else if (l_lTargets.get(0).equals("deletecustomjob")) {
                if (l_lTargets.size() < 3) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter writer = response.getWriter()) {
                        writer.println("no custom id and user_id specified");
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                } else {
                    boolean ucRet = unscheduleCustomJob(l_lTargets.get(1).toString(), l_lTargets.get(2).toString());
                    if (!ucRet) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        try (PrintWriter writer = response.getWriter()) {
                            writer.println("unable to delete custom job, maybe its already deleted");
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }
                    } else {
                        try (PrintWriter writer = response.getWriter()) {
                            writeJson(ucRet, writer);
                        } catch (Exception ex) {
                            baseRequest.setHandled(true);
                        } finally {
                            baseRequest.setHandled(true);
                        }

                    }
                }
            }
            // delete a alert
            else if (l_lTargets.get(0).equals("deletealert")) {
                if (l_lTargets.size() < 1) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter writer = response.getWriter()) {
                        writer.println("no alert id specified");
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                } else {
                    try(PrintWriter writer = response.getWriter()) {
                    writeJson(com.clavain.alerts.Helpers.removeAlert(Integer.parseInt(l_lTargets.get(1).toString())),writer);
                    } finally { baseRequest.setHandled(true); }     
                }  
            }
            // add a alert
            else if (l_lTargets.get(0).equals("addalert")) {
                if (l_lTargets.size() < 1) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter writer = response.getWriter()) {
                        writer.println("no alert id specified");
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                } else {
                    try(PrintWriter writer = response.getWriter()) {
                    writeJson(com.clavain.alerts.Helpers.addAlert(Integer.parseInt(l_lTargets.get(1).toString())),writer);
                    } finally { baseRequest.setHandled(true); }     
                }  
            }  
            // add a rca job
            else if (l_lTargets.get(0).equals("addrca")) {
                if (l_lTargets.size() < 1) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter writer = response.getWriter()) {
                        writer.println("no rca id specified");
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                } else {
                    try(PrintWriter writer = response.getWriter()) {
                        boolean found = false;
                        for (Analyzer l_rca : com.clavain.muninmxcd.v_analyzer) {
                            if(l_rca.getRcaId().equals(l_lTargets.get(1).toString()))
                            {
                                found = true;
                            }
                        }
                        if(!found)
                        {
                            Analyzer rca = new Analyzer(l_lTargets.get(1).toString());
                            if(rca.configureFromDatabase())
                            {
                                new Thread(rca).start();
                                writeJson(true,writer);
                            }
                            else
                            {
                                 writeJson(false,writer);
                            }
                        }
                        else
                        {
                            writeJson(true,writer);
                        }
                    } finally { baseRequest.setHandled(true); }     
                }  
            }  
            // rca status
            else if (l_lTargets.get(0).equals("rcastatus")) {
                if (l_lTargets.size() < 1) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    try (PrintWriter writer = response.getWriter()) {
                        writer.println("no rca id specified");
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                } else {
                    try(PrintWriter writer = response.getWriter()) {
                        boolean found = false;
                        for (Analyzer l_rca : com.clavain.muninmxcd.v_analyzer) {
                            if(l_rca.getRcaId().equals(l_lTargets.get(1).toString()))
                            {
                                writeJson(l_rca,writer);
                                found = true;
                            }
                        }
                        if(!found)
                        {
                            writeJson(false,writer);  
                        }
                    } finally { baseRequest.setHandled(true); }     
                }  
            }                 
            // send pushover test message
            else if(l_lTargets.get(0).equals("pushovertest"))
            {
               if(l_lTargets.size() < 4)
               {
                 response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                 try(PrintWriter writer = response.getWriter()) {
                 writer.println("userkey, title, message,");  
                 } finally { baseRequest.setHandled(true); }                         
               }
               else
               {
                    String userkey = l_lTargets.get(1).toString();
                    String title     = l_lTargets.get(2).toString();
                    String message   = l_lTargets.get(3).toString(); 
                    title = URLDecoder.decode(title,"UTF-8");
                    message = URLDecoder.decode(message,"UTF-8");
                    try(PrintWriter writer = response.getWriter()) {
                        writeJson(sendPushOverMessage(userkey, title, message),writer);
                    } finally { baseRequest.setHandled(true); 
                    }
               }
            }           
        } else {
            // single requests
        }

    }

    /**
     * List Node Detail
     */
    private MuninNode returnNode(Integer nodeId, boolean p_bOutput, Request baseRequest) {
        MuninNode mn = getMuninNode(nodeId);
        if (mn == null) {
            if (p_bOutput) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                try (PrintWriter writer = response.getWriter()) {
                    writer.println("node not found");
                } catch (Exception ex) {
                    baseRequest.setHandled(true);
                } finally {
                    baseRequest.setHandled(true);
                }
            }
            return null;
        } else {
            if (p_bOutput) {
                try (PrintWriter writer = response.getWriter()) {
                    writeJson(mn, writer);
                } catch (Exception ex) {
                    baseRequest.setHandled(true);
                } finally {
                    baseRequest.setHandled(true);
                }
            }
            return mn;
        }
    }

    /**
     * List nodes for monitoring
     */
    private void listNodes(Request baseRequest) {
        Iterator it = v_munin_nodes.iterator();
        List l_nodes = new ArrayList();
        while (it.hasNext()) {
            MuninNode l_mn = (MuninNode) it.next();
            if (!l_mn.getHostname().equals("127.0.0.1")) {
                l_nodes.add(l_mn);
            }
        }
        try (PrintWriter writer = response.getWriter()) {
            writeJson(l_nodes, writer);
        } catch (Exception ex) {
            baseRequest.setHandled(true);
        } finally {
            baseRequest.setHandled(true);
        }
    }

    /**
     * List nodes for monitoring in a given group
     */
    private void listNodes(String p_strGroup, Request baseRequest) {
        Iterator it = v_munin_nodes.iterator();
        List l_nodes = new ArrayList();
        while (it.hasNext()) {
            MuninNode l_mn = (MuninNode) it.next();
            if (l_mn.getGroup() != null) {
                if (l_mn.getGroup().equals(p_strGroup)) {
                    if (!l_mn.getHostname().equals("127.0.0.1")) {
                        l_nodes.add(l_mn);
                    }
                }
            }
        }
        if (l_nodes.size() < 1) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try (PrintWriter writer = response.getWriter()) {
                writer.println("no nodes available for query");
            } catch (Exception ex) {
                baseRequest.setHandled(true);
            } finally {
                baseRequest.setHandled(true);
            }
            return;
        } else {
            try (PrintWriter writer = response.getWriter()) {
                writeJson(l_nodes, writer);
            } catch (Exception ex) {
                baseRequest.setHandled(true);
            } finally {
                baseRequest.setHandled(true);
            }
        }

    }

    public static void writeJson(Object p_obj, PrintWriter writer) {
        Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
        writer.println(gson.toJson(p_obj));
        gson = null;
    }

    public static void writeJsonWithTransient(Object p_obj, PrintWriter writer) {
        Gson gson = new GsonBuilder().create();
        writer.println(gson.toJson(p_obj));
        gson = null;
    }

    /**
     * will start a collection thread if not running, return all graphs for
     * plugin
     *
     * @param mn a MuninNode
     * @param p_strPluginName name of plugin to retrieve graphs
     */
    private void fetchPlugin(MuninNode mn, String p_strPluginName, Request baseRequest) {

        // check if plugin exists
        if (mn.getLoadedPlugins() != null) {
            Iterator it = mn.getLoadedPlugins().iterator();
            boolean l_bPfound = false;
            while (it.hasNext()) {
                MuninPlugin l_mp = (MuninPlugin) it.next();
                if (l_mp.getPluginName().equals(p_strPluginName)) {
                    l_bPfound = true;

                    // set query time for plugin and node
                    l_mp.setLastFrontendQuery();
                    mn.setLastFrontendQuery();
                    try (PrintWriter writer = response.getWriter()) {
                        writeJson(l_mp.returnAllGraphs(), writer);
                    } catch (Exception ex) {
                        baseRequest.setHandled(true);
                    } finally {
                        baseRequest.setHandled(true);
                    }
                }
            }
            if (l_bPfound == false) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                try (PrintWriter writer = response.getWriter()) {
                    writer.println("no plugin named " + p_strPluginName + " found on this node");
                } catch (Exception ex) {
                    baseRequest.setHandled(true);
                } finally {
                    baseRequest.setHandled(true);
                }
            }
        } else {
            mn.loadPlugins();
            fetchPlugin(mn, p_strPluginName, baseRequest);
        }
    }
}
