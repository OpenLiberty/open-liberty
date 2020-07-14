/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
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
@Monitor(group = "REST")
@Provider
public class JaxRsMonitorFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Context
    ResourceInfo resourceInfo;
    
    // jaxRSCountByName is a MeterCollection that will hold the RESTStats MXBean for each RESTful
    // resource method
    @PublishedMetric
    public MeterCollection<REST_Stats> jaxRsCountByName = new MeterCollection<REST_Stats>("REST",this);
    
    // appMetricInfos is a hashmap used to store information for runtime and cleanup at 
    // application stop time.
    ConcurrentHashMap<String,RestMetricInfo> appMetricInfos = new ConcurrentHashMap<String,RestMetricInfo>();
    
    private static final String START_TIME = "Start_Time";
    
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
    	
    	// Store the start time in the ContainerRequestContext that can be accessed
    	// in the response filter method.  
        reqCtx.setProperty(START_TIME, System.nanoTime());
          	
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
     *            method and store the result in the REST_Stats MXBean.
     * 
     */
	@Override
	public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {

		long elapsedTime = 0;
		// Calculate the response time for the resource method.
		Long startTime = (Long) reqCtx.getProperty(START_TIME);
		if (startTime != null) {
			elapsedTime = System.nanoTime() - startTime.longValue();
		}

		Class<?> resourceClass = resourceInfo.getResourceClass();

		if (resourceClass != null) {
			Method resourceMethod = resourceInfo.getResourceMethod();

			Class<?>[] parameterClasses = resourceMethod.getParameterTypes();
			int i = 0;
			String parameter;
			String fullMethodName = resourceClass.getName() + "/" + resourceMethod.getName() + "(";
			for (Class<?> p : parameterClasses) {
				parameter = p.getCanonicalName();
				if (i > 0) {
					fullMethodName = fullMethodName + "_" + parameter;
				} else {
					fullMethodName = fullMethodName + parameter;
				}
				i++;
			}
			fullMethodName = fullMethodName + ")";

			ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
			String appName = getAppName(cmd);
			String modName = getModName(cmd);
			String keyPrefix = createKeyPrefix(appName, modName);
			String key = keyPrefix + "/" + fullMethodName;

			REST_Stats stats = jaxRsCountByName.get(key);
			if (stats == null) {
				stats = initJaxRsStats(key, keyPrefix, fullMethodName);
			}

			/*
			 * Explicitly checking for the Metrics Header via hard-coded header string.
			 * Don't want to add runtime/build dependency to this bundle/project because
			 * jaxrsMonitor-1.0.feature can run without metrics.
			 * 
			 */
			String metricsHeader = respCtx
			        .getHeaderString("com.ibm.ws.microprofile.metrics.monitor.MetricsJaxRsEMCallbackImpl.Exception");
			if (metricsHeader == null) {
				// Need to start new minute here.. we need to pass in the stat object so we can
				// actually update Mbean
				maybeStartNewMinute(stats);

				// Save key in appMetricInfos for cleanup on application stop.
				addKeyToMetricInfo(appName, key);

				// Increment the request count for the resource method.
				stats.incrementCountBy(1);

				// Store the response time for the resource method.
				stats.updateRT(elapsedTime < 0 ? 0 : elapsedTime);

				// Figure out min/max
				if (elapsedTime >= 0) {
					synchronized (this) {
						if (elapsedTime > stats.getMinuteLatestMaximumDuration()) {
							stats.updateMinuteLatestMaximumDuration(elapsedTime);
						}

						if (elapsedTime < stats.getMinuteLatestMinimumDuration()
						        || stats.getMinuteLatestMinimumDuration() == 0L) {
							stats.updateMinuteLatestMinimumDuration(elapsedTime);
						}
					}
				}
			}
		}
	}
    
    private void maybeStartNewMinute(REST_Stats stats) {
        long newMinute = getCurrentMinuteFromSystem();

        if (newMinute > stats.getMinuteLatest()) {
            synchronized (this) {
                if (newMinute > stats.getMinuteLatest()) {

                    // Move Latest values to Previous
                    stats.updateMinutePreviousMaximumDuration(stats.getMinuteLatestMaximumDuration());
                    stats.updateMinutePreviousMinimumDuration(stats.getMinuteLatestMinimumDuration());
                    stats.updateMinutePrevious(stats.getMinuteLatest());

                    // Rest latest minute values to 0 and update minute
                    stats.updateMinuteLatestMaximumDuration(0L);
                    stats.updateMinuteLatestMinimumDuration(0L);
                    stats.updateMinuteLatest(newMinute);
                }
            } // synch
        }
    }

    // Get the current system time in minutes, truncating. This number will increase by 1 every complete minute.
    private long getCurrentMinuteFromSystem() {
        return System.currentTimeMillis() / 60000;
    }
    
    /**
     * Method : initJaxRsStats()
     * 
     * @param key = Key for the REST_Stats instance in the MeterCollection
     * @param keyPrefix = Application name / module name portion of the key
     * @param _method =  Resource Method Name
     * 
     *            This method will create a REST_Stats object for current resource method.
     *            This method needs to be synchronized.
     * 
     *            This method gets called only at first request.
     * 
     */
    private synchronized REST_Stats initJaxRsStats(String key, String keyPrefix, String method) {
       REST_Stats nStats = this.jaxRsCountByName.get(key);
        if (nStats == null) {
             nStats = new REST_Stats(keyPrefix, method);
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
    
    
    protected RestMetricInfo getMetricInfo(String appName) {
    	RestMetricInfo rMetricInfo = appMetricInfos.get(appName);
    	if (rMetricInfo == null) {
    		rMetricInfo = new RestMetricInfo();
    		appMetricInfos.put(appName, rMetricInfo);
    	}
    	return rMetricInfo;
    }
    
    // At application stop time we will need to clean up the jaxRsCountByName 
    // MeteredCollection.  To do this we will need to store the keys for every 
    // entry in this collection.
    private void addKeyToMetricInfo(String appName, String key) {
    	RestMetricInfo rMetricInfo = appMetricInfos.get(appName);
    	if (rMetricInfo != null) {
    		rMetricInfo.setKey(key);
     	}
    }
    
    // Clean up the resources that were created for each resource method within
    // an application
    protected void cleanApplication(String appName) {   	
    	RestMetricInfo rMetricInfo = appMetricInfos.get(appName);
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
    
    class  RestMetricInfo {
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

