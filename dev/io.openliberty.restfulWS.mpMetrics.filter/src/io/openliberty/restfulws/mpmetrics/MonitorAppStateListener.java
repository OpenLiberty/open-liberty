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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import io.openliberty.microprofile.metrics.internal.monitor.MonitorMetricsHandler;
import io.openliberty.microprofile.metrics50.SharedMetricRegistries;
import io.openliberty.restfulws.mpmetrics.RestMetricsCallbackImpl.RestMetricInfo;


@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPid = "com.ibm.ws.monitor.internal.MonitoringFrameworkExtender", configurationPolicy = ConfigurationPolicy.OPTIONAL,
        service = { ApplicationStateListener.class })
public class MonitorAppStateListener implements ApplicationStateListener {

    static SharedMetricRegistries sharedMetricRegistries;

    static MonitorMetricsHandler monitorMetricsHandler;

	@Override
    public void applicationStarting(ApplicationInfo appInfo) {
        /*
         * When the application is starting we will create the application's metrics
         * info object to store information such as whether the application is contained
         * within an ear file or not.
         */
        String appName = appInfo.getDeploymentName();
        RestMetricInfo metricInfo = RestMetricsCallbackImpl.getMetricInfo(appName);
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        String appName = appInfo.getDeploymentName();
        /*
         * Allow the RestMetricsCallbackImpl instance to clean up when the application is
         * stopped.
         */
        RestMetricsCallbackImpl.cleanApplication(appName);

        /*
         * Clean up the computed REST metric, when the application is stopped.
         */
        monitorMetricsHandler.unregisterComputedRESTMetrics(appName);
    }

    @Reference
    public void setSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistryService) {
        sharedMetricRegistries = sharedMetricRegistryService;
    }

    @Reference
    public void setMonitorMetricsHandler(MonitorMetricsHandler monitorMetricsHandlerService) {
        monitorMetricsHandler = monitorMetricsHandlerService;
    }

}