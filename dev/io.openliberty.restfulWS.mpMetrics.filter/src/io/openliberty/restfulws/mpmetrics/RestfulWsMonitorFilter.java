/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package io.openliberty.restfulws.mpmetrics;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.microprofile.metrics.internal.monitor.computed.internal.ComputedMonitorMetricsHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RestfulWsMonitorFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final TraceComponent tc = Tr.register(RestfulWsMonitorFilter.class);

    static final String REST_REQUEST_DESCRIPTION = "The number of invocations and total response time of this RESTful "
            + "resource method since the start of the server. The metric will not record the elapsed time nor count "
            + "of a REST request if it resulted in an unmapped exception. Also tracks the highest recorded time "
            + "duration within the previous completed full minute and lowest recorded time duration within the "
            + "previous completed full minute.";

    static {
    	/*
    	 * Eagerly load the inner classes so that they are not loaded while calculating the amount of time a method took.
    	 * The first request coming through the filter() logic will end up being way off due to the loading of the inner classes. 
    	 */
    	TimerContext.init();
    	RestMetricInfo.init();
    }

    @Context
    ResourceInfo resourceInfo;

    static final ConcurrentHashMap<String, RestMetricInfo> appMetricInfos = new ConcurrentHashMap<String, RestMetricInfo>();

    static final ConcurrentHashMap<String, Timer> timerMap = new ConcurrentHashMap<String, Timer>();

    static final Set<RestfulWsMonitorFilter> instances = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final RestMonitorKeyCache monitorKeyCache = new RestMonitorKeyCache();
    private static final String TIMER_CONTEXT = "TIMER_CONTEXT";

    private static class TimerContext {
    	static void init() {}

        final String timerKey;
        final long startTime;
        TimerContext(String timerKey, long startTime) {
            this.timerKey = timerKey;
            this.startTime = startTime;
        }

        @Override
        public String toString() {
            return "TimerContext [timerKey=" + timerKey + ", startTime=" + startTime + "]";
        }
    }

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
     *               This method will be called whenever a request arrives for a
     *               RESTful resource method. All it does is save the current time
     *               so that the response time can be calculated later.
     * 
     */
    @Override
    public void filter(ContainerRequestContext reqCtx) throws IOException {
        Class<?> resourceClass = resourceInfo.getResourceClass();

        if (resourceClass != null) {
            Method resourceMethod = resourceInfo.getResourceMethod();
            Timer restTimer;
            String key = monitorKeyCache.getMonitorKey(resourceClass, resourceMethod);

            if (key == null) {
                String className = resourceClass.getName();
                String methodName = resourceMethod.getName();

                Class<?>[] parameterClasses = resourceMethod.getParameterTypes();
                int i = 0;
                String parameter;
                String fullMethodName = resourceClass.getName() + "/" + resourceMethod.getName() + "(";
                for (Class<?> p : parameterClasses) {
                    parameter = p.getCanonicalName();
                    if (i > 0) {
                        methodName = methodName + "_" + parameter;
                        fullMethodName = fullMethodName + "_" + parameter;
                    } else {
                        methodName = methodName + "_" + parameter;
                        fullMethodName = fullMethodName + parameter;
                    }
                    i++;
                }
                fullMethodName = fullMethodName + ")";

                ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
                String appName = getAppName(cmd);
                String modName = getModName(cmd);
                String keyPrefix = createKeyPrefix(appName, modName);

                // Used as our key for the map
                key = keyPrefix + "/" + fullMethodName;

                // Save key in appMetricInfos for cleanup on application stop.
                addKeyToMetricInfo(appName, key);

                if ((restTimer = timerMap.get(key)) == null) {
                    MetricRegistry baseMetricRegistry = MonitorAppStateListener.sharedMetricRegistries
                            .getOrCreate(MetricRegistry.BASE_SCOPE);
                    Metadata metricMetadata = Metadata.builder().withName("REST.request")
                            .withDescription(REST_REQUEST_DESCRIPTION).build();
                    Tag classTag = new Tag("class", className);
                    Tag methodTag = new Tag("method", methodName);
                    restTimer = baseMetricRegistry.timer(metricMetadata, classTag, methodTag);

                    MetricID metricID = new MetricID("REST.request", classTag, methodTag);
                    MonitorAppStateListener.sharedMetricRegistries.associateMetricIDToApplication(metricID, keyPrefix,
                            baseMetricRegistry);

                    timerMap.put(key, restTimer);

                    /*
                     * Need to make sure we register the unmapped exception counter as it is
                     * expected whether an exception has occurred or not.
                     */
                    MetricsRestfulWsEMCallbackImpl.registerOrRetrieveRESTUnmappedExceptionMetric(className, methodName,
                            appName);

                    // Register the computed REST.elapsedTime metric.
                    ComputedMonitorMetricsHandler cmmh = MonitorAppStateListener.monitorMetricsHandler.getComputedMonitorMetricsHandler();

                    //Save mp app name value from the MP Config property (if available) for unregistering the metric.
                    String mpAppNameConfigValue = resolveMPAppNameFromMPConfig();

                    cmmh.createRESTComputedMetrics("RESTStats", metricID, appName, mpAppNameConfigValue);
                }

                monitorKeyCache.putMonitorKey(resourceClass, resourceMethod, key);
            }

            /*
             * Store the start time and key information in the ContainerRequestContext that can be accessed in
             * the response filter method.
             */
            reqCtx.setProperty(TIMER_CONTEXT, new TimerContext(key, System.nanoTime()));
        }
    }

    /**
     * Method : filter(ContainerRequestContext, ContainerResponseContext)
     * 
     * @param reqCtx  = Container request context
     * @param respCtx = Container response context
     * 
     *                This method will be called whenever a response arrives from a
     *                RESTful resource method. It will calculate the cumulative
     *                response time for the resource method and store the result in
     *                the REST_Stats MXBean.
     * 
     */
    @Override
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {
        // Check that the TimerContext has been set on the request context.  This will happen when 
        // the ContainerRequestFilter.filter() method is invoked.  Situations, such as an improper jwt will cause the 
        // request filter to not be called and we will therefore not record any statistics.
        TimerContext timerContext = (TimerContext) reqCtx.getProperty(TIMER_CONTEXT);
        if (timerContext == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ContainerRequestContext.filter() has not been invoked for " + reqCtx.getUriInfo().getPath());
            }
            return;
        }

        // Calculate the response time for the resource method.
        long elapsedTime = System.nanoTime() - timerContext.startTime;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Elapsed time for " + timerContext.timerKey + " is " + elapsedTime + " ns");
        }

        /*
         * Explicitly checking for the Metrics Header via hard-coded header string.
         */
        String metricsHeader = respCtx
                .getHeaderString("io.openliberty.restfulws.mpmetrics.MetricsRestfulWsEMCallbackImpl.Exception");

        /*
         * Check if exception header was present, if not continue setting value for
         * Timer
         */
        if (metricsHeader == null) {
            Timer restTimer = timerMap.get(timerContext.timerKey);
            restTimer.update(Duration.ofNanos(elapsedTime));
        }
    }

    private String getModName(ComponentMetaData cmd) {
        String modName = null;
        if (cmd != null) {
            ModuleMetaData mmd = cmd.getModuleMetaData();
            if (mmd != null) {
                modName = mmd.getName();
            }
        }
        return modName;
    }

    private String getAppName(ComponentMetaData cmd) {
        String appName = null;
        if (cmd != null) {
            J2EEName j2name = cmd.getJ2EEName();
            if (j2name != null) {
                appName = j2name.getApplication();
            }
        }
        return appName;
    }

    private String createKeyPrefix(String appName, String modName) {
        /*
         * If the application is packaged in an ear file then the key prefix will be
         * appname/modname. Otherwise it will just be modName.
         */
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

    /*
     * At application stop time we will need to clean up the timer map. To do this
     * we will need to store the keys for every entry in this collection.
     */
    private void addKeyToMetricInfo(String appName, String key) {
        RestMetricInfo rMetricInfo = appMetricInfos.get(appName);
        if (rMetricInfo != null) {
            rMetricInfo.setKey(key);
        }
    }

    /*
     * Clean up the resources that were created for each resource method within an
     * application
     */
    static void cleanApplication(String appName) {
        RestMetricInfo rMetricInfo = appMetricInfos.get(appName);
        if (rMetricInfo != null) {
            HashSet<String> keys = rMetricInfo.getKeys();
            if (!keys.isEmpty()) {
                Iterator<String> keyIterator = keys.iterator();
                String key = null;
                while (keyIterator.hasNext()) {
                    key = keyIterator.next();
                    timerMap.remove(key);
                }

            }
            appMetricInfos.remove(appName);
        }
    }

    protected String resolveMPAppNameFromMPConfig() {
        Optional<String> applicationName = null;
        String mpAppName = null;

        if ((applicationName = ConfigProvider.getConfig().getOptionalValue("mp.metrics.appName", String.class)).isPresent() 
                && !applicationName.get().isEmpty()) {
            mpAppName = applicationName.get();
        }
        else if ((applicationName = ConfigProvider.getConfig().getOptionalValue("mp.metrics.defaultAppName", String.class)).isPresent() 
                && !applicationName.get().isEmpty()) {
            mpAppName = applicationName.get();
        }

        return mpAppName;
    }

    static class RestMetricInfo {
    	static void init() {}

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
