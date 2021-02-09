/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics.internal.publicapi;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.metrics.BaseMetrics;
import com.ibm.ws.microprofile.metrics.BaseMetricsHandler;
import com.ibm.ws.microprofile.metrics.Constants;
import com.ibm.ws.microprofile.metrics.WriterFactory;
import com.ibm.ws.microprofile.metrics.helper.Util;
import com.ibm.ws.microprofile.metrics.impl.SharedMetricRegistries;
import com.ibm.wsspi.rest.handler.RESTHandler;

@Component(service = { RESTHandler.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM",
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_CONTEXT_ROOT + "="
                                                                                                                                                   + Constants.PATH_METRICS,
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + Constants.PATH_ROOT,
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + Constants.PATH_SUB,
                                                                                                                             RESTHandler.PROPERTY_REST_HANDLER_ROOT + "=" + Constants.PATH_SUB_ATTRIBUTE })
public class PublicMetricsRESTHandler extends BaseMetricsHandler {
    private static final TraceComponent tc = Tr.register(PublicMetricsRESTHandler.class);

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        for (String registry : Constants.REGISTRY_NAMES_LIST) {
            sharedMetricRegistry.getOrCreate(registry);
        }
        Util.SHARED_METRIC_REGISTRIES = sharedMetricRegistry;
    }

    @Reference
    public void setSharedMetricRegistries(SharedMetricRegistries sharedMetricRegistry) {
        this.sharedMetricRegistry = sharedMetricRegistry;
    }

    @Reference
    public void setWriterFactory(WriterFactory writerFactory) {
    	this.writerFactory = writerFactory;
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {}
}
