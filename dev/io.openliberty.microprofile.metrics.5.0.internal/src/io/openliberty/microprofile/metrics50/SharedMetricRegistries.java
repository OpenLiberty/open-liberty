/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.metrics50;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.metrics50.helper.Util;
import io.openliberty.microprofile.metrics50.internal.SmallryeMetricsCDIMetadata;
import io.openliberty.smallrye.metrics.adapters.SRMetricRegistryAdapter;
import io.openliberty.smallrye.metrics.adapters.SRSharedMetricRegistriesAdapter;

/**
 * A map of shared, named metric registries.
 */
@Component(service = SharedMetricRegistries.class, immediate = true)
public class SharedMetricRegistries {
    private static final TraceComponent tc = Tr.register(SharedMetricRegistries.class);

    // TODO: not used with this version of mp metrics, maybe find some place to use
    public static void clear() {
        // io.smallrye.metrics.SharedMetricRegistries.dropAll();
    }

    // TODO: not used with this version of mp metrics, maybe find some place to use
    public static void remove(String scope) {
        // io.smallrye.metrics.SharedMetricRegistries.drop(scope);
    }

    SmallryeMetricsCDIMetadata smallryeMetricsCDIMetadata;

    // Used strictly for "timing" purposes
    @Reference
    public void CDIExtensionMetadata(SmallryeMetricsCDIMetadata ref) {
        smallryeMetricsCDIMetadata = ref;
    }

    public Set<String> getMetricRegistryScopeNames() {
        /*
         * Unsuccessful activation. SmallRye and Micrometer classes were not loaded
         * properly.
         */
        if (!smallryeMetricsCDIMetadata.isSuccessfulActivation()) {
            return null;
        }

        SRSharedMetricRegistriesAdapter srsma;
        srsma = SRSharedMetricRegistriesAdapter.getInstance();
        return srsma.getRegistryScopeNames();
    }

    public void associateMetricIDToApplication(MetricID metricID, String appName, MetricRegistry registry) {
        if (Util.SR_LEGACY_METRIC_REGISTRY_CLASS.isInstance(registry)) {
            try {
                Object cast = Util.SR_LEGACY_METRIC_REGISTRY_CLASS.cast(registry);

                SRMetricRegistryAdapter metricRegistry = new SRMetricRegistryAdapter(cast);
                // Adds the metricID and the application to the application -> metricID map of
                // the registry
                metricRegistry.addNameToApplicationMap(metricID);
            } catch (ClassCastException e) {
                // This should never actually happen.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Incompatible Metric Registries. Coud not cast " + registry + " to "
                            + Util.SR_LEGACY_METRIC_REGISTRY_CLASS);
                }
            }
        }
    }

    public void setAppNameResolver() {
        /*
         * Unsuccessful activation. SmallRye and Micrometer classes were not loaded
         * properly.
         */
        if (!smallryeMetricsCDIMetadata.isSuccessfulActivation()) {
            return;
        }

        ClassLoader baoClassLoader = Util.BUNDLE_ADD_ON_CLASSLOADER;
        Object applicationNameResolverProxy = Proxy.newProxyInstance(baoClassLoader,
                new Class[] { Util.SR_APPLICATION_NAME_RESOLVER_INTERFACE }, new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("getApplicationName")) {
                            com.ibm.ws.runtime.metadata.ComponentMetaData metaData = com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl
                                    .getComponentMetaDataAccessor().getComponentMetaData();
                            if (metaData != null) {
                                com.ibm.websphere.csi.J2EEName appName = metaData.getJ2EEName();
                                if (appName != null) {
                                    return appName.getApplication();
                                }
                            }
                            return null;
                        } // end if
                        return null;
                    }
                });
        SRSharedMetricRegistriesAdapter srsma;
        srsma = SRSharedMetricRegistriesAdapter.getInstance();
        srsma.setAppNameResolver(applicationNameResolverProxy);
    }

    public MetricRegistry getOrCreate(String scope) {
        /*
         * Unsuccessful activation. SmallRye and Micrometer classes were not loaded
         * properly.
         */
        if (!smallryeMetricsCDIMetadata.isSuccessfulActivation()) {
            return null;
        }

        ClassLoader baoClassLoader = Util.BUNDLE_ADD_ON_CLASSLOADER;
        Object applicationNameResolverProxy = Proxy.newProxyInstance(baoClassLoader,
                new Class[] { Util.SR_APPLICATION_NAME_RESOLVER_INTERFACE }, new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("getApplicationName")) {
                            com.ibm.ws.runtime.metadata.ComponentMetaData metaData = com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl
                                    .getComponentMetaDataAccessor().getComponentMetaData();
                            if (metaData != null) {
                                com.ibm.websphere.csi.J2EEName appName = metaData.getJ2EEName();
                                if (appName != null) {
                                    return appName.getApplication();
                                }
                            }
                            return null;
                        } // end if
                        return null;
                    }
                });

        SRSharedMetricRegistriesAdapter srsma;
        srsma = SRSharedMetricRegistriesAdapter.getInstance();
        return srsma.getOrCreate(scope, applicationNameResolverProxy);
    }

}
