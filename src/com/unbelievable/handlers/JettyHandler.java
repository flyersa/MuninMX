package com.unbelievable.handlers;

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
import static com.unbelievable.utils.Generic.*;
import static com.unbelievable.muninmxcd.v_munin_nodes;
import com.unbelievable.munin.*;


/**
 *
 * @author phantom
 */
public class JettyHandler extends AbstractHandler
{
    Logger logger = Logger.getRootLogger();
    public static HttpServletResponse response;
    public static PrintWriter writer;
    
    public void handle(String target,Request baseRequest,HttpServletRequest request,HttpServletResponse p_response) 
        throws IOException, ServletException
    {
        if(!target.equals("/favicon.ico"))
        {
            logger.info("Request from ["+baseRequest.getRemoteAddr()+"] : " + target);
        }
        response = p_response;
        response.setContentType("text/html;charset=utf-8");
        response.setHeader("Server", "MuninMXcd" + com.unbelievable.muninmxcd.version);
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        writer = response.getWriter();
        
        baseRequest.setHandled(true);
        
        List l_lTargets = new ArrayList();
        StringTokenizer st = new StringTokenizer(target,"/");
        while (st.hasMoreTokens()) {
            String l_strToken = st.nextToken();
            l_lTargets.add(l_strToken);
            //response.getWriter().println(st.nextToken());
        }
        
        // multi request?
        if(l_lTargets.size() > 1)
        {
            // listings
            if(l_lTargets.get(0).equals("list"))
            {
                // list nodes
                if(l_lTargets.get(1).equals("nodes"))
                {
                    if(l_lTargets.size() > 2)
                    {
                        listNodes(l_lTargets.get(2).toString());
                    }
                    else
                    {
                        listNodes();
                    }
                }
            }
            // query a node
            else if(l_lTargets.get(0).equals("node"))
            {
                //logger.debug(l_lTargets.size());
                MuninNode mn = null;
                if(l_lTargets.size() <= 2)
                {
                    mn = returnNode(Integer.parseInt(l_lTargets.get(1).toString()),true);   
                }
                else
                {
                    // retrieve available munin plugins for this node
                    mn = returnNode(Integer.parseInt(l_lTargets.get(1).toString()),false);    
                    if(l_lTargets.get(2).equals("plugins") && mn != null)
                    {
                        logger.info("query plugins for " + mn.getNodename());
                        writeJson(mn.getPluginList());
                    }
                    // return graphs for a given plugin, start up node if not running
                    if(l_lTargets.get(2).equals("fetch") && l_lTargets.size() == 4 && mn != null)
                    {
                        logger.debug("query plugin: " + l_lTargets.get(3) + " for node: " + mn.getNodename());
                        fetchPlugin(mn,l_lTargets.get(3).toString());
                    }
                }
            }
        }
        else
        {
           // single requests

        }
        
    }

    
    /** List Node Detail */
    private MuninNode returnNode(Integer nodeId,boolean p_bOutput)
    {
        MuninNode mn = getMuninNode(nodeId);
        if(mn == null)
        {
            if(p_bOutput)
            {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writer.println("node not found");
            }
            return null;
        }
        else
        {
            if(p_bOutput)
            { 
                writeJson(mn); 
            }
            return mn;
        }
    }
    
    /** List nodes for monitoring */
    private void listNodes() 
    {
        Iterator it = v_munin_nodes.iterator();
        List l_nodes = new ArrayList();
        while (it.hasNext())
        {
             MuninNode l_mn = (MuninNode) it.next();
             if(!l_mn.getHostname().equals("127.0.0.1"))
             {
                l_nodes.add(l_mn);
             }
        }
        writeJson(l_nodes);
    }
    
    /** List nodes for monitoring in a given group */
    private void listNodes(String p_strGroup) 
    {
        Iterator it = v_munin_nodes.iterator();
        List l_nodes = new ArrayList();
        while (it.hasNext())
        {
             MuninNode l_mn = (MuninNode) it.next();
             if(l_mn.getGroup() != null)
             {
                if(l_mn.getGroup().equals(p_strGroup))
                {
                    if(!l_mn.getHostname().equals("127.0.0.1"))
                    {
                        l_nodes.add(l_mn);
                    }
                }
             }
        }
        if(l_nodes.size() < 1)
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writer.println("no nodes available for query");
            return;
        }        
        writeJson(l_nodes);
    }    
    
    
    public static void writeJson(Object p_obj)
    {
        Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.TRANSIENT).create();
        writer.println(gson.toJson(p_obj));   
        gson = null;
    }

    
    /** 
     * will start a collection thread if not running, return all graphs for plugin
     * @param mn a MuninNode
     * @param p_strPluginName name of plugin to retrieve graphs 
     */
    private void fetchPlugin(MuninNode mn, String p_strPluginName) {
  
        // check if plugin exists
        if(mn.getLoadedPlugins() != null)
        {
            Iterator it = mn.getLoadedPlugins().iterator();
            boolean l_bPfound = false;
            while(it.hasNext())
            {
                MuninPlugin l_mp = (MuninPlugin) it.next();
                if(l_mp.getPluginName().equals(p_strPluginName))
                {
                    l_bPfound = true;
                    
                    // set query time for plugin and node
                    l_mp.setLastFrontendQuery();
                    mn.setLastFrontendQuery();
                    
                    writeJson(l_mp.returnAllGraphs());
                }
            }
            if(l_bPfound == false)
            {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                writer.println("no plugin named " + p_strPluginName+ " found on this node");    
            }
        }
        else
        {
            mn.loadPlugins();
            fetchPlugin(mn,p_strPluginName);  
        }
    }
    
       
}
