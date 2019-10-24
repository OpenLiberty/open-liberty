/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.MeterCollection;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;


/**
 * Monitor Class for RESTful Resource Methods.
 */ 
@Monitor(group = "RESTful")
@Provider
public class JaxRsMonitorFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Context
    ResourceInfo resourceInfo;
    
    // jaxRSCountByName is a MeterCollection that will hold the RESTfulStats MXBean for each RESTful
    // resoure method
    @PublishedMetric
    public MeterCollection<RESTful_Stats> jaxRsCountByName = new MeterCollection<RESTful_Stats>("RESTful",this);
    
    // appMetricInfos is a hashmap used to store information for runtime and cleanup at 
    // application stop time.
    protected ConcurrentHashMap<String,RestfulMetricInfo> appMetricInfos = new ConcurrentHashMap<String,RestfulMetricInfo>();
    
    private final ThreadLocal<Long> startTimes = new ThreadLocal<Long>();    

    /**
     * Method : filter(ContainerRequestContext)
     * 
     * @param reqCtx = Container request context
     * 
     *            This method will be called whenever a request arrives for a
     *            RESTful resource method.
     *            All it does is save the current time so that the response
     *            time can be calculated later.
     * 
     */
    @Override
    public void filter(ContainerRequestContext reqCtx) throws IOException {
    	
        startTimes.set(System.nanoTime());
  	
    }
    /**
     * Method : filter(ContainerRequestContext, ContainerResponseContext)
     * 
     * @param reqCtx = Container request context
     * @param respCtx = Container response context
     * 
     *            This method will be called whenever a response arrives from a
     *            RESTful resource method.
     *            It will calculate the cumulative response time for the resource
     *            method and store the result in the RESTful_Stats MXBean.
     * 
     */

    @Override
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        if (resourceClass != null) {
            Method resourceMethod = resourceInfo.getResourceMethod();
            String resourceMethodString = resourceMethod.toGenericString();
            
            //Spit to obtain just the fully qualified method name (without app and mod information).
            String[] methodParts = resourceMethodString.split(" ");
            String simpleMethodName = methodParts[methodParts.length -1];
                        
            ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            String appName = getAppName(cmd);
            String modName = getModName(cmd);
            String keyPrefix = createKeyPrefix(appName,modName);
            String key = keyPrefix + "/" + simpleMethodName;        

            RESTful_Stats stats = jaxRsCountByName.get(key);
            if (stats == null) {
                 stats =initJaxRsStats(key, keyPrefix, simpleMethodName);
            }
            // Save key in appMetricInfos for cleanup on application stop.
            addKeyToMetricInfo(appName,key);
            
            //Increment the request count for the resource method.
            stats.incrementCountBy(1);
            
            //Calculate and store the response time for the resource method.
            Long times = startTimes.get();
            if (times!=null) {
                long elapsed = System.nanoTime() - times;
                stats.updateRT(elapsed < 0 ? 0 : elapsed);
            }
        	
        }

    }
    
    /**
     * Method : initJaxRsStats()
     * 
     * @param key = Key for the RESTful_Stats instance in the MeterCollection
     * @param keyPrefix = Application name / module name portion of the key
     * @param _method =  Resource Method Name
     * 
     *            This method will create a RESTful_Stats object for current resource method.
     *            This method needs to be synchronized.
     * 
     *            This method gets called only at first request.
     * 
     */
    private synchronized RESTful_Stats initJaxRsStats(String key, String keyPrefix, String method) {
       RESTful_Stats nStats = this.jaxRsCountByName.get(key);
        if (nStats == null) {
             nStats = new RESTful_Stats(keyPrefix, method);
            this.jaxRsCountByName.put(key, nStats);            
        }
        return nStats;
    }
    
    private String getModName(ComponentMetaData cmd) {
    	String modName = null;
        if (cmd != null) {
            ModuleMetaData mmd = cmd.getModuleMetaData();
            if (mmd != null) {
            	modName  = mmd.getName();          	
            }
        }
        return modName;
    }


    private String getAppName(ComponentMetaData cmd) {
    	String appName = null;
        if (cmd != null) {
        	J2EEName j2name= cmd.getJ2EEName();
            if (j2name != null) {
            	appName = j2name.getApplication();
            }
        }
        return appName;
    }
   
    private String createKeyPrefix(String appName, String modName) {
    	// If the application is packaged in an ear file then the key prefix will be
    	// appname/modname.  Otherwise it will just be modName.
    	if (getMetricInfo(appName).isEar) {
    		return appName + "/" + modName;
    	} else {
    		return modName;
    	}
    }
    
    
    protected RestfulMetricInfo getMetricInfo(String appName) {
    	RestfulMetricInfo rMetricInfo = appMetricInfos.get(appName);
    	if (rMetricInfo == null) {
    		rMetricInfo = new RestfulMetricInfo();
    		appMetricInfos.put(appName, rMetricInfo);
    	}
    		return rMetricInfo;
    }
    
    // At application stop time we will need to clean up the jaxRsCountByName 
    // MeteredCollection.  To do this we will need to store the keys for every 
    // entry in this collection.
    private void addKeyToMetricInfo(String appName, String key) {
    	RestfulMetricInfo rMetricInfo = appMetricInfos.get(appName);
    	if (rMetricInfo != null) {
    		rMetricInfo.setKey(key);
     	}
    }
    
    // Clean up the resources that were created for each resource method within
    // an application
    protected void cleanApplication(String appName) {   	
    	RestfulMetricInfo rMetricInfo = appMetricInfos.get(appName);
    	if (rMetricInfo != null) {
    		HashSet<String> keys = rMetricInfo.getKeys();
    		if (!keys.isEmpty()) {
        		Iterator<String> keyIterator = keys.iterator();
        		String key = null;
        		while (keyIterator.hasNext()) {
        			key = keyIterator.next();
        			jaxRsCountByName.remove(key);      			
        		}
    			
    		}
    		appMetricInfos.remove(appName);
    	}
    }
    
    class  RestfulMetricInfo {
    	boolean isEar = false;
    	HashSet<String> keys = new HashSet<String>();
    	
    	void setIsEar() {
    		isEar = true;   	
    	}
    	
    	void setKey(String key) {
    		keys.add(key);
    	}
    	
    	HashSet<String> getKeys() {
    		return keys;
    	}
    }
}

