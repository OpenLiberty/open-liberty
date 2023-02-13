/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.metrics.component;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;

@Component(configurationPolicy = IGNORE)
public class MetricsServiceComponent {

    private static SharedMetricRegistries sharedRegistries;

    public static SharedMetricRegistries getSharedMetricRegistries() {
        return sharedRegistries;
    }

    @Reference
    protected void setSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistries) {
        sharedRegistries = sharedMetricRegistries;
    }

    protected void unsetSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistries) {
        if (sharedRegistries == sharedMetricRegistries) {
            sharedRegistries = null;
        }
    }
}
