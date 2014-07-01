/*
 * MuninMX
 * Copyright (c) 2014
 * www.clavain.com
 * 
 */
package com.clavain.handlers;


import org.eclipse.jetty.server.Server;
import static com.clavain.muninmxcd.logger;
import static com.clavain.muninmxcd.p;
/**
 *
 * @author enricokern
 */
public class JettyLauncher implements Runnable {

    @Override
    public void run() {
            // Jetty
        try
        {
            Server server = new Server(Integer.parseInt(p.getProperty("api.port")));
            server.setHandler(new JettyHandler());
            logger.info("Starting API Server on port " + p.getProperty("api.port"));
            server.start();
            server.join();
        } catch (Exception ex)
        {
            logger.fatal("Api Server crashed: " + ex.getLocalizedMessage());  
        }
    }
    
}
