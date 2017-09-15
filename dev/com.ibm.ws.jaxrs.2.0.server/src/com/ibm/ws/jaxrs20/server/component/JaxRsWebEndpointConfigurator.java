/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.server.component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.feature.Feature;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.jaxrs20.api.JaxRsEndpointConfigurator;
import com.ibm.ws.jaxrs20.endpoint.JaxRsPublisherContext;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.server.JaxRsWebEndpointImpl;

/**
 *
 */
@Component(service = { JaxRsEndpointConfigurator.class }, property = { "service.vendor=IBM" })
public class JaxRsWebEndpointConfigurator implements JaxRsEndpointConfigurator {

    private final List<Feature> features = new CopyOnWriteArrayList<Feature>();

    @Override
    public JaxRsWebEndpointImpl createWebEndpoint(EndpointInfo endpointInfo, JaxRsPublisherContext context) {
        return new JaxRsWebEndpointImpl(endpointInfo, context, new ArrayList<Feature>(features));
    }

    @Override
    public <T> T getEndpointProperty(String name, Class<T> valueClassType) {
        if (USE_NAMESPACE_COLLABORATOR.equals(name)) {
            return valueClassType.cast(Boolean.FALSE);
        }
        return null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setFeature(Feature feature) {
        features.add(feature);
    }

    protected void unsetFeature(Feature feature) {
        features.remove(feature);
    }
}
