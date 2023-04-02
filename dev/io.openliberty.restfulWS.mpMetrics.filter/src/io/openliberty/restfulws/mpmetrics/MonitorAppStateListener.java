/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import io.openliberty.microprofile.metrics50.SharedMetricRegistries;
import io.openliberty.restfulws.mpmetrics.RestfulWsMonitorFilter.RestMetricInfo;

@Component(immediate = true, property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.IGNORE,
        service = { ApplicationStateListener.class })
public class MonitorAppStateListener implements ApplicationStateListener {

    static SharedMetricRegistries sharedMetricRegistries;

    @Override
    public void applicationStarting(ApplicationInfo appInfo) {
        /*
         * When the application is starting we will create the application's metrics
         * info object to store information such as whether the application is contained
         * within an ear file or not.
         */
        String appName = appInfo.getDeploymentName();
        RestMetricInfo metricInfo = RestfulWsMonitorFilter.getMetricInfo(appName);

        /*
         * Determine if the application is packaged within an ear file. This is
         * 
         * useful since a key created in the RestfulWsMonitorFilter class will be
         * prefixed with the earname + warname or just the warname. See
         * JaxRsMonitorFilter class for more information.
         *
         */
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
        /*
         * Allow the RestfulWsMonitorFilter instance to clean up when the application is
         * stopped.
         */
        RestfulWsMonitorFilter.cleanApplication(appInfo.getDeploymentName());
    }

    @Reference
    public void setSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistryService) {
        sharedMetricRegistries = sharedMetricRegistryService;
    }

}