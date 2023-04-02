/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.monitor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Monitor Class for RESTful Resource Methods.
 */ 
@Monitor(group = "REST")
@Provider
public class JaxRsMonitorFilter implements ContainerRequestFilter, ContainerResponseFilter {
	
    private static final TraceComponent tc = Tr.register(JaxRsMonitorFilter.class);

    @Context
    ResourceInfo resourceInfo;
    
    // jaxRSCountByName is a MeterCollection that will hold the RESTStats MXBean for each RESTful
    // resource method
    @PublishedMetric
    public final MeterCollection<REST_Stats> jaxRsCountByName = new MeterCollection<REST_Stats>("REST",this);
    
    // appMetricInfos is a hashmap used to store information for runtime and cleanup at 
    // application stop time.
    static final ConcurrentHashMap<String,RestMetricInfo> appMetricInfos = new ConcurrentHashMap<String,RestMetricInfo>();
    static final Set<JaxRsMonitorFilter> instances = new HashSet<>();
    private static final String START_TIME = "Start_Time";
    private static final String CMD = "CMD";

    @PostConstruct
    public void postConstruct() {
    	instances.add(this);
    }

    @PreDestroy
    public void preDestroy() {
    	instances.remove(this);
    }

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
        reqCtx.setProperty(CMD, ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData());
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
		
		// Check that the ComponentMetaData has been set on the request context.  This will happen when 
		// the ContainerRequestFilter.filter() method is invoked.  Situations, such as an improper jwt will cause the 
		// request filter to not be called and we will therefore not record any statistics.
		ComponentMetaData cmd = (ComponentMetaData) reqCtx.getProperty(CMD);
		if (cmd == null) {
	        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	            Tr.debug(tc, "ContainerRequestContext.filter() has not been invoked for " + reqCtx.getUriInfo().getPath());
	        }
			return;
		}

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
			
			//Check for MP Metrics 30 header;
			if (metricsHeader == null)
				metricsHeader = respCtx.getHeaderString("io.openliberty.microprofile.metrics.internal.monitor.MetricsJaxRsEMCallbackImpl.Exception");

			//Check for MP Metrics 31 header;
			if (metricsHeader == null)
				metricsHeader = respCtx.getHeaderString("io.openliberty.restfulws.mpmetrics.MetricsRestfulWsEMCallbackImpl.Exception");

			// Save key in appMetricInfos for cleanup on application stop.
			addKeyToMetricInfo(appName, key);
			
			if (metricsHeader == null) {
				// Need to start new minute here.. we need to pass in the stat object so we can
				// actually update Mbean
				maybeStartNewMinute(stats);


				// Increment the request count for the resource method.
				stats.incrementCountBy(1);

				// Store the response time for the resource method.
				stats.updateRT(elapsedTime < 0 ? 0 : elapsedTime);

				// Figure out min/max
				if (elapsedTime >= 0) {
					long minuteLatestMaximumDuration = stats.getMinuteLatestMaximumDuration();
					while (elapsedTime > minuteLatestMaximumDuration) {
						if (stats.compareAndUpdateMinuteLatestMaximumDuration(minuteLatestMaximumDuration, elapsedTime)) {
								break;
						}
						minuteLatestMaximumDuration = stats.getMinuteLatestMaximumDuration();
					}

					long minuteLatestMinimumDuration = stats.getMinuteLatestMinimumDuration();
					if (!(elapsedTime == 0L && minuteLatestMinimumDuration == 0L)) {
						while (elapsedTime < minuteLatestMinimumDuration || minuteLatestMinimumDuration == 0L) {
							if (stats.compareAndUpdateMinuteLatestMinimumDuration(minuteLatestMinimumDuration, elapsedTime)) {
								break;
							}
							minuteLatestMinimumDuration = stats.getMinuteLatestMinimumDuration();
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
    
    static RestMetricInfo getMetricInfo(String appName) {
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
    static void cleanApplication(String appName) {   	
    	RestMetricInfo rMetricInfo = appMetricInfos.get(appName);
    	if (rMetricInfo != null) {
    		HashSet<String> keys = rMetricInfo.getKeys();
    		if (!keys.isEmpty()) {
        		Iterator<String> keyIterator = keys.iterator();
        		String key = null;
        		while (keyIterator.hasNext()) {
        			key = keyIterator.next();
        			for (JaxRsMonitorFilter filter : instances)
        				filter.jaxRsCountByName.remove(key);      			
        		}
    			
    		}
    		appMetricInfos.remove(appName);
    	}
    }
    
    static class RestMetricInfo {
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

