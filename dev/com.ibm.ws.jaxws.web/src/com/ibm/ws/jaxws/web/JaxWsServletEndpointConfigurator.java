/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.jaxws.web;

import com.ibm.ws.jaxws.endpoint.JaxWsEndpointConfigurator;
import com.ibm.ws.jaxws.endpoint.JaxWsPublisherContext;
import com.ibm.ws.jaxws.endpoint.JaxWsWebEndpoint;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.EndpointType;

/**
 *
 */
public class JaxWsServletEndpointConfigurator implements JaxWsEndpointConfigurator {

    @Override
    public JaxWsWebEndpoint createWebEndpoint(EndpointInfo endpointInfo, JaxWsPublisherContext context) {
        return new POJOJaxWsWebEndpoint(endpointInfo, context);
    }

    @Override
    public EndpointType getSupportedEndpointType() {
        return EndpointType.SERVLET;
    }

    @Override
    public <T> T getEndpointProperty(String name, Class<T> valueClassType) {
        if (USE_NAMESPACE_COLLABORATOR.equals(name)) {
            return valueClassType.cast(Boolean.FALSE);
        }
        return null;
    }

}
