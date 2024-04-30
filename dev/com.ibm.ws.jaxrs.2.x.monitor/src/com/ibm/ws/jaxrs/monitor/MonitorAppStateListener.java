/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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

import java.util.Map;
import java.util.stream.Stream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.jaxrs.monitor.JaxRsMonitorFilter.RestMetricInfo;
import com.ibm.ws.jaxrs.monitor.metrics.service.RestMetricsCallback;

@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPid = "com.ibm.ws.monitor.internal.MonitoringFrameworkExtender", configurationPolicy = ConfigurationPolicy.OPTIONAL, service = {ApplicationStateListener.class})
public class MonitorAppStateListener implements ApplicationStateListener {

    private final static String MONITORING_GROUP_FILTER = "filter";

    private static final TraceComponent tc = Tr.register(MonitorAppStateListener.class);
    
    static RestMetricsCallback restMetricCallback;

    /*
     * By Default, without any monitor-1.0 filters on, all monitor components are enabled
     */
    private static boolean isRESTEnabled = true;


    public static boolean isRESTEnabled() {
        return isRESTEnabled;
    }

    @Override
    public void applicationStarting(ApplicationInfo appInfo) {
        // When the application is starting we will create
        // the application's metrics info object to store
        // information such as whether the application is
        // contained within an ear file or not.
        String appName = appInfo.getDeploymentName();
        RestMetricInfo metricInfo = JaxRsMonitorFilter.getMetricInfo(appName);
        
        // Determine if the application is packaged within an ear file.  This is 
        // useful since a key created in the JaxRsMonitorFilter class will be 
        // prefixed with the earname + warname or just the warname.
        // See JaxRsMonitorFilter class for more information.
        if (appInfo.getClass().getName().endsWith("EARApplicationInfoImpl")) {
            metricInfo.setIsEar();
        }
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        // Allow the JaxRsMonitorFilter instance to clean up when the application
        // is stopped.
        JaxRsMonitorFilter.cleanApplication(appInfo.getDeploymentName());
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        resolveMonitorFilter(properties);
    }

    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        resolveMonitorFilter(properties);
    }

    private void resolveMonitorFilter(Map<String, Object> properties) {
        String filter;

        if ((filter = (String) properties.get(MONITORING_GROUP_FILTER)) != null && filter.length() != 0) {
            // Original MonitoringFrameWorkExtender matches case

            if (filter.length() > 0) {
                isRESTEnabled = Stream.of(filter.split(",")).anyMatch(item -> item.equals("REST"));
            } else {
                // by default, every monitor component is enabled if length is 0
                isRESTEnabled = true;
            }
        } else if (filter == null) {
            /*
             * This bundle starts automatically with jaxrs and monitor-1.0 If `filter` is
             * null, we'll assume that there was no config and we will enable by default.
             */
            isRESTEnabled = true;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, String.format("JAX-RS filter is enabled set to: [%s]", isRESTEnabled));
        }
    }
    
    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void getMP5RestMetricsCallback(RestMetricsCallback restMetricCallback) {
    	MonitorAppStateListener.restMetricCallback = restMetricCallback;
    }
}