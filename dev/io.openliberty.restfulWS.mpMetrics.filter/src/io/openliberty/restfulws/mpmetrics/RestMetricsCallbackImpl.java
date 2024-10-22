/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs.monitor.metrics.service.RestMetricsCallback;

import io.openliberty.microprofile.metrics.internal.monitor.computed.internal.ComputedMonitorMetricsHandler;

@Component(service = { RestMetricsCallback.class }, configurationPolicy = ConfigurationPolicy.IGNORE, property = {
        "service.vendor=IBM" })
public class RestMetricsCallbackImpl implements RestMetricsCallback {

    private static final TraceComponent tc = Tr.register(RestMetricsCallbackImpl.class);

    static final ConcurrentHashMap<String, RestMetricInfo> appMetricInfos = new ConcurrentHashMap<String, RestMetricInfo>();

    static final ConcurrentHashMap<String, Timer> timerMap = new ConcurrentHashMap<String, Timer>();

    static final String REST_REQUEST_DESCRIPTION = "The number of invocations and total response time of this RESTful "
            + "resource method since the start of the server. The metric will not record the elapsed time nor count "
            + "of a REST request if it resulted in an unmapped exception. Also tracks the highest recorded time "
            + "duration within the previous completed full minute and lowest recorded time duration within the "
            + "previous completed full minute.";

    @Override
    public void createRestMetric(String classMethodParamSignature, String statsKey) {
        Timer restTimer;

        // check if timerMap has this key - if we did, then something weird happened
        if (!timerMap.contains(statsKey)) {
            String appName = resolveAppNameFromStatsKey(statsKey);

            // App name should not be null, something went wrong.
            if (appName == null) {
                // log and return - do not continue with creation
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                            String.format(
                                    "Encountered issue when validating REST stats key: [%s]. REST metric will not be created."), statsKey);
                }
                return;
            }
            
            // Used for clean up when app is unloaded
            addKeyToMetricInfo(appName, statsKey);

            // create Metric
            String className = getClassTagValue(classMethodParamSignature);
            String methodName = getMethodTagValue(classMethodParamSignature);

            MetricRegistry baseMetricRegistry = MonitorAppStateListener.sharedMetricRegistries
                    .getOrCreate(MetricRegistry.BASE_SCOPE);
            Metadata metricMetadata = Metadata.builder().withName("REST.request")
                    .withDescription(REST_REQUEST_DESCRIPTION).build(); 
            Tag classTag = new Tag("class", className);
            Tag methodTag = new Tag("method", methodName);
            restTimer = baseMetricRegistry.timer(metricMetadata, classTag, methodTag);

            MetricID metricID = new MetricID("REST.request", classTag, methodTag);

            MonitorAppStateListener.sharedMetricRegistries.associateMetricIDToApplication(metricID, appName,
                    baseMetricRegistry);

            timerMap.put(statsKey, restTimer);

            /*
             * Need to make sure we register the unmapped exception counter as it is
             * expected whether an exception has occurred or not upon creation of the main
             * REST metric.
             */
            MetricsRestfulWsEMCallbackImpl.registerOrRetrieveRESTUnmappedExceptionMetric(className, methodName,
                    appName);

            /**
             * Need to handle the computed metrics now
             */

            // Register the computed REST.elapsedTime metric.
            ComputedMonitorMetricsHandler cmmh = MonitorAppStateListener.monitorMetricsHandler
                    .getComputedMonitorMetricsHandler();

            // Save mp app name value from the MP Config property (if available) for
            // unregistering the metric.
            String mpAppNameConfigValue = resolveMPAppNameFromMPConfig();

            cmmh.createRESTComputedMetrics("RESTStats", metricID, appName, mpAppNameConfigValue);

        } else {
            // This method is called when Mbean is created... somehow metric already exists.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, String.format("REST metric for stats key: [%s] has already been created.", statsKey));
            }
        }

    }

    @Override
    public void updateRestMetric(String classMethodParamSignature, String statsKey, Duration duration) {
        Timer restTimer;
        if (!timerMap.containsKey(statsKey)) {

            /*
             * This should have been created. Unless something went wrong. We will not
             * "create" it now since something has gone wrong from before.
             */
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, String.format(
                        "REST metric for stats key: [%s] has not been created and can not be updated.", statsKey));
            }
            return;
        }
        restTimer = timerMap.get(statsKey);
        restTimer.update(duration);
    }

    protected String resolveMPAppNameFromMPConfig() {
        Optional<String> applicationName = null;
        String mpAppName = null;

        if ((applicationName = ConfigProvider.getConfig().getOptionalValue("mp.metrics.appName", String.class))
                .isPresent() && !applicationName.get().isEmpty()) {
            mpAppName = applicationName.get();
        } else if ((applicationName = ConfigProvider.getConfig().getOptionalValue("mp.metrics.defaultAppName",
                String.class)).isPresent() && !applicationName.get().isEmpty()) {
            mpAppName = applicationName.get();
        }

        return mpAppName;
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

    static RestMetricInfo getMetricInfo(String appName) {
        RestMetricInfo rMetricInfo = appMetricInfos.get(appName);
        if (rMetricInfo == null) {
            rMetricInfo = new RestMetricInfo();
            appMetricInfos.put(appName, rMetricInfo);
        }
        return rMetricInfo;
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

                    /*
                     * NOTE: The Metric Registry already keeps track of appName -> metrics It will
                     * handle the deregistering of metrics on its own end when an application is
                     * unloaded.
                     */
                }
            }
            appMetricInfos.remove(appName);
        }
    }

    /**
     * Given the REST Stats key resolve the app name. Stats key takes the template
     * of appName[/moduleName]/class/method(params...)
     * 
     * @param statsKey the appName[/moduleName]/class/method(params...)
     * @return the derived appName or null if error occurred
     */
    private String resolveAppNameFromStatsKey(String statsKey){
        if (statsKey == null || statsKey.isBlank()) {
            return null;
        } else {
            String[] keyComponents = statsKey.split("/");
            if (keyComponents.length == 0 || keyComponents.length > 4) {
                return null;
            } else {
                // AppName is always first one
                return keyComponents[0];
            }
        }
    }

    /**
     * Retrieve the class name from the class/method(params..) signature
     * 
     * @param classMethodParamSignature
     * @return class name for the tags
     */
    private String getClassTagValue(String classMethodParamSignature) {
        return classMethodParamSignature.split("/")[0];
    }

    /**
     * Retrieve the method name from the class/method(params..) signature
     * 
     * @param classMethodParamSignature
     * @return method name for the tags
     */
    private String getMethodTagValue(String classMethodParamSignature) {
        String methodTag;

        // Example of the expected incoming String signature
        // fully.qualified.class.name/methodSignature(java.lang.String)

        methodTag = classMethodParamSignature.split("/")[1];

        // blank method
        methodTag = methodTag.replaceAll("\\(\\)", "");
        // otherwise first bracket becomes underscores
        methodTag = methodTag.replaceAll("\\(", "_");
        // second bracket is removed
        methodTag = methodTag.replaceAll("\\)", "");

        return methodTag;
    }

    static class RestMetricInfo {
        static void init() {
        }

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
