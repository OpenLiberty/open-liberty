/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.metadata.builder;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceEndpoint;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebservicesBnd;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.utils.URLUtils;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 * process custom binding file ibm-ws-bnd.xml
 */
@Component(service = { EndpointInfoConfigurator.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = false, property = { "service.vendor=IBM" })
public class WebServicesBndEndpointInfoConfigurator extends AbstractEndpointInfoConfigurator {

    /**
     * @param phase
     */
    public WebServicesBndEndpointInfoConfigurator() {
        super(EndpointInfoConfigurator.Phase.POST_PROCESS_DESCRIPTOR);
    }

    @Override
    public void prepare(EndpointInfoBuilderContext context, EndpointInfo endpointInfo) throws UnableToAdaptException {

    }

    @Override
    public void config(EndpointInfoBuilderContext context, EndpointInfo endpointInfo) throws UnableToAdaptException {
        Container container = context.getContainer();
        WebservicesBnd webservicesBnd = container.adapt(WebservicesBnd.class);

        if (webservicesBnd == null) {
            return;
        }

        // set default endpoint properties
        Map<String, String> defaultProperties = webservicesBnd.getWebserviceEndpointProperties();
        if (defaultProperties != null && !defaultProperties.isEmpty()) {
            endpointInfo.setEndpointProperties(defaultProperties);
        }

        // set endpoint address
        String portComponentName = endpointInfo.getPortComponentName();
        WebserviceEndpoint webserviceEndpoint = webservicesBnd.getWebserviceEndpoint(portComponentName);

        if (webserviceEndpoint != null) {
            String address = webserviceEndpoint.getAddress();
            address = URLUtils.normalizePath(address);
            if (address != null && !address.isEmpty()) {
                endpointInfo.clearAddresses();
                endpointInfo.addAddress(address);
            }

            // set endpoint properties specified in the endpoint element
            Map<String, String> properties = webserviceEndpoint.getProperties();
            if (properties != null && !properties.isEmpty()) {
                if (endpointInfo.getEndpointProperties() != null) {
                    endpointInfo.getEndpointProperties().putAll(properties);
                } else {
                    endpointInfo.setEndpointProperties(properties);
                }
            }
        }
    }

}