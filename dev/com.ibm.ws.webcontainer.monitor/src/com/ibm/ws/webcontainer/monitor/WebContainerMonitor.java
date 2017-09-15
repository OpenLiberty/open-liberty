/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.monitor;

import javax.servlet.GenericServlet;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.ProbeAtEntry;
import com.ibm.websphere.monitor.annotation.ProbeAtReturn;
import com.ibm.websphere.monitor.annotation.ProbeSite;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.annotation.This;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.websphere.servlet.container.WebContainer;
import com.ibm.ws.pmi.server.PmiRegistry;

/**
 * Monitor Class for WebContainer.
 */
@Monitor(group = "WebContainer")
public class WebContainerMonitor {

    private static final String APP_NAME_FROM_CONTEXT = "com.ibm.websphere.servlet.enterprise.application.name";
    private final ThreadLocal<Long> startTimes = new ThreadLocal<Long>();    

    @PublishedMetric
    public MeterCollection<ServletStats> servletCountByName = new MeterCollection<ServletStats>("Servlet",this);

    /**
     * This class is responsible for calculating and reporting of Performance Data for each Servlet.
     * Following data we collect from each Servlet.
     * 1) RequestCount
     * 2) ResponseTime.
     * We do this by injecting code into following class of WebContainer Module.
     * Class : javax.servlet.http.HttpServlet
     * Method : service(javax.servlet.ServletRequest,javax.servlet.ServletResponse)
     * atServletStart() -> Create ServletStats for this servlet, Store startTime,
     * atServletEnd() -> Use ServletStats for this servlet, get request count, response time.
     * 
     * To Support Traditional PMI, we add com.ibm.wsspi.webcontainer.pmi.WebAppMonitorListener to
     * global listener of WebContainer.
     * 
     */
    public WebContainerMonitor() {
        //Add com.ibm.wsspi.webcontainer.pmi.WebAppMonitorListener class as a Listener 
        //for Applications, Servlets.
        
        //Following line with add traditional PMI support
        //If we don't add WebAppMonitorListener to Global Listener, PMI WebAppModule wouldn't work
        if(!PmiRegistry.isDisabled()){
            WebContainer.addGlobalListener("com.ibm.ws.webcontainer.WebAppMonitorListener");
        }
    }

    @ProbeAtEntry
    @ProbeSite(clazz = "com.ibm.ws.webcontainer.servlet.ServletWrapper", method = "service", args = "javax.servlet.ServletRequest,javax.servlet.ServletResponse,com.ibm.ws.webcontainer.webapp.WebAppServletInvocationEvent")
    public void atServletStart(@This GenericServlet s) {
//        String servletName = s.getServletConfig().getServletName();
//        String appName = (String) s.getServletContext().getAttribute(APP_NAME_FROM_CONTEXT);
//        String key = appName + "." + servletName;               
//        ServletStats servletStats = servletCountByName.get(key);        
//        if (servletStats == null) {
//            initServletStats(appName, servletName);        
//        }
        //Store servlet service start time
        startTimes.set(System.nanoTime());
    }

    /**
     * Method : initServletStats()
     * 
     * @param _app = Application Name
     * @param _ser = Servlet Name
     * 
     *            This method will create ServletStats object for current servlet.
     *            This method needs to be synchronised.
     * 
     *            This method gets called only at first request.
     * 
     */
    public synchronized ServletStats initServletStats(String _app, String _ser) {
        String _key = _app + "." + _ser;
        ServletStats nStats = this.servletCountByName.get(_key);
        if (nStats == null) {
             nStats = new ServletStats(_app, _ser);
            this.servletCountByName.put(_key, nStats);            
        }
        return nStats;
    }

    @ProbeAtReturn
    @ProbeSite(clazz = "com.ibm.ws.webcontainer.servlet.ServletWrapper", method = "service", args = "javax.servlet.ServletRequest,javax.servlet.ServletResponse,com.ibm.ws.webcontainer.webapp.WebAppServletInvocationEvent")
    public void atServletEnd(@This GenericServlet s) {        
        String servletName = s.getServletConfig().getServletName();
        String appName = (String) s.getServletContext().getAttribute(APP_NAME_FROM_CONTEXT);
        String sName = appName + "." + servletName;        
        ServletStats stats = servletCountByName.get(sName);
        if (stats == null) {
             stats =initServletStats(appName, servletName);
        }                       
        stats.incrementCountBy(1);        
        Long times = startTimes.get();
        if (times!=null) {
            long elapsed = System.nanoTime() - times;
            stats.updateRT(elapsed < 0 ? 0 : elapsed);
        }
        
    }
    

    @ProbeAtEntry
    @ProbeSite(clazz = "com.ibm.ws.webcontainer.servlet.ServletWrapper", method = "destroy")
    public void atServletDestroy(@This GenericServlet s) {
        String servletName = s.getServletConfig().getServletName();
        String appName = (String) s.getServletContext().getAttribute(APP_NAME_FROM_CONTEXT);
        String sName = appName + "." + servletName;
        servletCountByName.remove(sName);
    }

}
