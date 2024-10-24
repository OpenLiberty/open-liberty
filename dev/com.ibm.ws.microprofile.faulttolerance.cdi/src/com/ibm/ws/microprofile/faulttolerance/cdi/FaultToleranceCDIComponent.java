/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.microprofile.faulttolerance.cdi;

import static java.util.Collections.unmodifiableList;
import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.ws.microprofile.faulttolerance.spi.FTAnnotationInspector;
import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * This component provides access to various services through a static methods
 */
@Component(configurationPolicy = IGNORE, immediate = true)
public class FaultToleranceCDIComponent {

    private static List<MetricRecorderProvider> metricProviders = new CopyOnWriteArrayList<MetricRecorderProvider>();

    private static FTEnablementConfig enablementConfig = null;

    private static volatile Optional<FaultToleranceCDIComponent> instance = Optional.empty();

    @Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
    private volatile List<FTAnnotationInspector> annotationInspectors;

    @Activate
    protected void activate() {
        synchronized (FaultToleranceCDIComponent.class) {
            instance = Optional.of(this);
        }
    }

    @Deactivate
    protected void deactivate() {
        synchronized (FaultToleranceCDIComponent.class) {
            if (instance.isPresent() && instance.get().equals(this)) {
                instance = Optional.empty();
            }
        }
    }

    /**
     * Get the current metric providers
     * <p>
     * The provider may change if features are updated
     *
     * @return an unmodifiable list of metric providers.
     */
    public static List<MetricRecorderProvider> getMetricProviders() {
        return Collections.unmodifiableList(metricProviders);
    }

    @Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE)
    protected void setProvider(MetricRecorderProvider provider) {
        metricProviders.add(provider);
    }

    protected void unsetProvider(MetricRecorderProvider provider) {
        metricProviders.remove(provider);
    }

    /**
     * Get the current enablement config
     */
    public static FTEnablementConfig getEnablementConfig() {
        return enablementConfig;
    }

    @Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MANDATORY)
    protected void setEnablementConfig(FTEnablementConfig config) {
        enablementConfig = config;
    }

    protected void unsetEnablementConfig(FTEnablementConfig config) {
        // Unless we're shutting down, we expect this to be called just *after* setEnablementConfig has been called with the new provider
        // so don't unset the field unconditionally.
        if (enablementConfig == config) {
            enablementConfig = null;
        }
    }

    /**
     * Get the list of annotation inspectors
     *
     * @return immutable list of annotation inspectors
     */
    public static List<FTAnnotationInspector> getAnnotationInspectors() {
        return instance.map(i -> unmodifiableList(i.annotationInspectors)).orElseGet(Collections::emptyList);
    }

}
