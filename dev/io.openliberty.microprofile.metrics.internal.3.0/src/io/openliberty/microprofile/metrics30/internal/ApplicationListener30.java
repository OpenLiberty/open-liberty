/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics30.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

import io.openliberty.microprofile.metrics30.internal.impl.MetricRegistry30Impl;

@Component(service = { ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ApplicationListener30 implements ApplicationStateListener {

    private SharedMetricRegistries sharedMetricRegistry;

    public static Map<String, String> contextRoot_Map = new HashMap<String, String>();

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        MetricRegistry[] registryArray = new MetricRegistry[] { sharedMetricRegistry.getOrCreate(MetricRegistry.Type.APPLICATION.getName()),
                                                                sharedMetricRegistry.getOrCreate(MetricRegistry.Type.BASE.getName()) };
        for (MetricRegistry registry : registryArray) {
            if (MetricRegistry30Impl.class.isInstance(registry)) {
                MetricRegistry30Impl impl = (MetricRegistry30Impl) registry;
                impl.unRegisterApplicationMetrics(appInfo.getDeploymentName());
            }
        }
    }

    @Reference
    public void getSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        this.sharedMetricRegistry = sharedMetricRegistry;
    }
}
