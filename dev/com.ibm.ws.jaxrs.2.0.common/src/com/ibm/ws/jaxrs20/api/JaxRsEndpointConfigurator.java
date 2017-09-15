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
package com.ibm.ws.jaxrs20.api;

import com.ibm.ws.jaxrs20.endpoint.JaxRsPublisherContext;
import com.ibm.ws.jaxrs20.endpoint.JaxRsWebEndpoint;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;

/**
 * JaxWsEndpointConfigurator provides the abstraction for the different Web Services configurator, e.g. POJO, EJB and etc.
 */
public interface JaxRsEndpointConfigurator {

    public static final String USE_NAMESPACE_COLLABORATOR = "USE_NAMESPACE_COLLABORATOR";

    /**
     * Create the JaxWsEndpoint used to serve the requests
     * 
     * @param endpointInfo
     * @param configuration
     * @return
     */
    public JaxRsWebEndpoint createWebEndpoint(EndpointInfo endpointInfo, JaxRsPublisherContext configuration);

    /**
     * Return the endpoint property for the configurator
     * 
     * @param name property name
     * @param valueClassType property value class type
     * @return the property value or null if the property is not existed
     */
    public <T> T getEndpointProperty(String name, Class<T> valueClassType);
}
