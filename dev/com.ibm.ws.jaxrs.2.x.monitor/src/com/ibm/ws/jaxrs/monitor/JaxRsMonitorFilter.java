/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.monitor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.ws.jaxrs.monitor.RESTful_Stats;


/**
 * Monitor Class for WebContainer.
 */
@Monitor(group = "JaxRS")
@Provider
public class JaxRsMonitorFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Context
    ResourceInfo resourceInfo;
    @Context
    ServletContext ctx;
    @Context
    GenericServlet gsvlt;
    
    @PublishedMetric
    public MeterCollection<RESTful_Stats> jaxRsCountByName = new MeterCollection<RESTful_Stats>("JaxRS",this);
    
    private static final String APP_NAME_FROM_CONTEXT = "com.ibm.websphere.servlet.enterprise.application.name";

    private final ThreadLocal<Long> startTimes = new ThreadLocal<Long>();    


    @Override
    public void filter(ContainerRequestContext reqCtx) throws IOException {
    	
        startTimes.set(System.nanoTime());
  	
    }

    @Override
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        System.out.println("JaxRsMonitorFilter(response) - resourceClass=" + resourceClass);
        
        Method resourceMethod = resourceInfo.getResourceMethod();
        MultivaluedMap<String,String> headers = reqCtx.getHeaders();
        String resourceMethodString = resourceMethod.toGenericString();
        String[] methodParts = resourceMethodString.split(" ");
        String simpleMethodName = methodParts[methodParts.length -1];

        Request thisRequest = reqCtx.getRequest();
        
        String appName = (String)ctx.getAttribute(APP_NAME_FROM_CONTEXT);
        Enumeration<String> attributes = ctx.getAttributeNames();
        while (attributes.hasMoreElements()) {
        	System.out.println("Jim...Attribute = " + attributes.nextElement());
        }
        String sName = appName + "/" + simpleMethodName;        

        System.out.println("JaxRsMonitorFilter(request) - key=" + sName);
        System.out.println("JaxRsMonitorFilter(request) - appName=" + appName);
        System.out.println("JaxRsMonitorFilter(request) - resourceClass=" + resourceClass);
        System.out.println("JaxRsMonitorFilter(request) - resourceMethod=" + resourceMethodString);
        System.out.println("JaxRsMonitorFilter(request) - resourceMethod2=" + simpleMethodName);
        System.out.println("JaxRsMonitorFilter(request) - request=" + thisRequest);
        for (Entry<String, List<String>> header : headers.entrySet()) {
        	System.out.println("Key : " + header.getKey() + "Header : " + header.getValue());
        }
        
        RESTful_Stats stats = jaxRsCountByName.get(sName);
        if (stats == null) {
             stats =initJaxRsStats(appName, simpleMethodName);
        }                       
        stats.incrementCountBy(1);        

    }
    
    /**
     * Method : initJaxRsStats()
     * 
     * @param _app = Application Name
     * @param _method =  Resource Method Name
     * 
     *            This method will create JaxRsStats object for current resource method.
     *            This method needs to be synchronised.
     * 
     *            This method gets called only at first request.
     * 
     */
    public synchronized RESTful_Stats initJaxRsStats(String _app, String _method) {
        String _key = _app + "/" + _method;
       RESTful_Stats nStats = this.jaxRsCountByName.get(_key);
        if (nStats == null) {
             nStats = new RESTful_Stats(_app, _method);
            this.jaxRsCountByName.put(_key, nStats);            
        }
        return nStats;
    }

}
