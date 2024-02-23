/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.metrics50.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;

import io.openliberty.microprofile.metrics50.SharedMetricRegistries;
import io.openliberty.microprofile.metrics50.helper.Util;
import io.openliberty.smallrye.metrics.adapters.SRMetricRegistryAdapter;

@Component(service = { ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE,
        immediate = true)
public class ApplicationListener50 implements ApplicationStateListener {
    private static final TraceComponent tc = Tr.register(ApplicationListener50.class);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {

        Set<String> scopeNamesSet = sharedMetricRegistry.getMetricRegistryScopeNames();
        if (!scopeNamesSet.contains(MetricRegistry.VENDOR_SCOPE)) {
            scopeNamesSet.add(MetricRegistry.VENDOR_SCOPE);
        } else if (!scopeNamesSet.contains(MetricRegistry.APPLICATION_SCOPE)) {
            scopeNamesSet.add(MetricRegistry.APPLICATION_SCOPE);
        } else if (!scopeNamesSet.contains(MetricRegistry.BASE_SCOPE)) {
            scopeNamesSet.add(MetricRegistry.BASE_SCOPE);
        }

        MetricRegistry[] registryArray = new MetricRegistry[scopeNamesSet.size()];

        int i = 0;
        for (String scope : scopeNamesSet) {
            registryArray[i] = sharedMetricRegistry.getOrCreate(scope);
            i++;
        }

        for (MetricRegistry registry : registryArray) {
            if (Util.SR_LEGACY_METRIC_REGISTRY_CLASS.isInstance(registry)) {
                try {
                    Object cast = Util.SR_LEGACY_METRIC_REGISTRY_CLASS.cast(registry);
                    SRMetricRegistryAdapter metricRegistry;
                    metricRegistry = new SRMetricRegistryAdapter(cast);
                    metricRegistry.unRegisterApplicationMetrics(appInfo.getDeploymentName());
                } catch (ClassCastException e) {
                    // This should never actually happen.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Incompatible Metric Registries. Coud not cast " + registry + " to "
                                + Util.SR_LEGACY_METRIC_REGISTRY_CLASS);
                    }
                }

            }
        }
    }

    @Reference
    public void getSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        this.sharedMetricRegistry = sharedMetricRegistry;
    }
}
