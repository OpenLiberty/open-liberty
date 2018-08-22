/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.microprofile.faulttolerance.spi.MetricRecorderProvider;

/**
 * This component tracks the current {@link MetricRecorderProvider} and exposes it through a static method.
 */
@Component(name = "com.ibm.ws.microprofile.faulttolerance.metrics.impl.MetricComponent", configurationPolicy = IGNORE)
public class MetricComponent {

    private static MetricRecorderProvider metricProvider = null;

    /**
     * Get the current metric provider
     * <p>
     * The provider may change if features are updated
     */
    public static MetricRecorderProvider getMetricProvider() {
        return metricProvider;
    }

    // Require a provider, update dynamically if a new one becomes available
    @Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MANDATORY)
    protected void setProvider(MetricRecorderProvider provider) {
        metricProvider = provider;
    }

    protected void unsetProvider(MetricRecorderProvider provider) {
        // Unless we're shutting down, we expect this to be called just *after* setProvider has been called with the new provider
        // so don't unset the field unconditionally.
        if (metricProvider == provider) {
            metricProvider = null;
        }
    }

}
