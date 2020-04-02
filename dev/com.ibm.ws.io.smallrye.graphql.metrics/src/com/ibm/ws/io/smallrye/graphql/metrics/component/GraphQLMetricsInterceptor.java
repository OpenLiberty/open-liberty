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
package com.ibm.ws.io.smallrye.graphql.metrics.component;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.interceptor.Interceptor;

import com.ibm.ws.microprofile.metrics.cdi23.producer.MetricRegistryFactory;

import io.smallrye.graphql.metrics.MetricsInterceptor;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.metrics.MetricRegistry;

@Dependent
@GraphQLApi
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class GraphQLMetricsInterceptor extends MetricsInterceptor {

    GraphQLMetricsInterceptor() {
        setMetrics(MetricRegistryFactory.getVendorRegistry());
    }

    // this is overridden here to avoid the injection annotation
    @Override
    public void setMetrics(MetricRegistry metrics) {
        super.setMetrics(metrics);
    }
}
