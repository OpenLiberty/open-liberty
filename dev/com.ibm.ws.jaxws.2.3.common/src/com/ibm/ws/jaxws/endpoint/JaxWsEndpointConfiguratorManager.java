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
package com.ibm.ws.jaxws.endpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.metadata.EndpointType;

/**
 *
 */
public class JaxWsEndpointConfiguratorManager {

    private static final TraceComponent tc = Tr.register(JaxWsEndpointConfiguratorManager.class);

    public Map<EndpointType, JaxWsEndpointConfigurator> endpointTypeJaxWsEndpointConfiguratorMap = new ConcurrentHashMap<EndpointType, JaxWsEndpointConfigurator>();

    public void registerJaxWsEndpointConfigurator(JaxWsEndpointConfigurator configurator) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Register JaxWsEndpointConfigurator support " + configurator.getSupportedEndpointType());
        }
        endpointTypeJaxWsEndpointConfiguratorMap.put(configurator.getSupportedEndpointType(), configurator);
    }

    public void unregisterJaxWsEndpointConfigurator(JaxWsEndpointConfigurator configurator) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "unregister JaxWsEndpointConfigurator support " + configurator.getSupportedEndpointType());
        }
        endpointTypeJaxWsEndpointConfiguratorMap.remove(configurator.getSupportedEndpointType());
    }

    public JaxWsEndpointConfigurator getJaxWsEndpointConfigurator(EndpointType endpointType) {
        return endpointTypeJaxWsEndpointConfiguratorMap.get(endpointType);
    }
}
