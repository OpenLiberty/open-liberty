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
package com.ibm.ws.jaxws.ejb;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.jaxws.endpoint.JaxWsEndpointConfigurator;
import com.ibm.ws.jaxws.endpoint.JaxWsPublisherContext;
import com.ibm.ws.jaxws.endpoint.JaxWsWebEndpoint;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.EndpointType;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
public class JaxWsEJBEndpointConfigurator implements JaxWsEndpointConfigurator {

    private final AtomicServiceReference<EJBContainer> ejbContainerRef = new AtomicServiceReference<EJBContainer>("ejbContainer");

    protected void activate(ComponentContext cc) {
        ejbContainerRef.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        ejbContainerRef.deactivate(cc);
    }

    protected void setEJBContainer(ServiceReference<EJBContainer> ref) {
        ejbContainerRef.setReference(ref);
    }

    protected void unsetEJBContainer(ServiceReference<EJBContainer> ref) {
        ejbContainerRef.unsetReference(ref);
    }

    @Override
    public JaxWsWebEndpoint createWebEndpoint(EndpointInfo endpointInfo, JaxWsPublisherContext context) {
        return new EJBJaxWsWebEndpoint(endpointInfo, context, ejbContainerRef.getServiceWithException());
    }

    @Override
    public EndpointType getSupportedEndpointType() {
        return EndpointType.EJB;
    }

    @Override
    public <T> T getEndpointProperty(String name, Class<T> valueClassType) {
        if (USE_NAMESPACE_COLLABORATOR.equals(name)) {
            return valueClassType.cast(Boolean.TRUE);
        }
        return null;
    }
}
