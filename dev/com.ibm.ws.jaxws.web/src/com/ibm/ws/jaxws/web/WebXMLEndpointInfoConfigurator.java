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
package com.ibm.ws.jaxws.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.metadata.EndpointInfo;
import com.ibm.ws.jaxws.metadata.builder.AbstractEndpointInfoConfigurator;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoBuilderContext;
import com.ibm.ws.jaxws.metadata.builder.EndpointInfoConfigurator;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * Process the web.xml to configure EndpointInfo
 */
@Component(service = { EndpointInfoConfigurator.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = false, property = { "service.vendor=IBM" })
public class WebXMLEndpointInfoConfigurator extends AbstractEndpointInfoConfigurator {

    private final TraceComponent tc = Tr.register(WebXMLEndpointInfoConfigurator.class);

    public WebXMLEndpointInfoConfigurator() {
        super(EndpointInfoConfigurator.Phase.PRE_PROCESS_DESCRIPTOR);
    }

    @Override
    public void prepare(EndpointInfoBuilderContext context, EndpointInfo endpointInfo) throws UnableToAdaptException {
        WebAppConfig webAppConfig = context.getContainer().adapt(WebAppConfig.class);
        if (null == webAppConfig) {
            return;
        }

        if (null != endpointInfo.getServletName()) {
            endpointInfo.setConfiguredInWebXml(true);
        }
    }

    @Override
    public void config(EndpointInfoBuilderContext context, EndpointInfo endpointInfo) throws UnableToAdaptException {
        if (endpointInfo.isConfiguredInWebXml()) {
            WebAppConfig webAppConfig = context.getContainer().adapt(WebAppConfig.class);
            if (null == webAppConfig) {
                return;
            }

            IServletConfig servletConfig = webAppConfig.getServletInfo(endpointInfo.getServletName());
            List<String> addresses = getAddressesFromServletConfig(servletConfig, endpointInfo.getServletName());
            //If users explicitly configure the servlet-mapping for the endpoints, will clear up the default ones
            if (!addresses.isEmpty()) {
                endpointInfo.clearAddresses();
                endpointInfo.addAddresses(addresses);
            }
        }

    }

    private List<String> getAddressesFromServletConfig(IServletConfig servletConfig, String servletName) {
        List<String> servletMappings = servletConfig.getMappings();
        if (servletMappings == null || servletMappings.size() == 0) {
            return Collections.emptyList();
        }

        List<String> addresses = new ArrayList<String>(servletMappings.size());
        for (String mapping : servletConfig.getMappings()) {
            if (mapping.contains("*")) {
                //no need to error this: 146310
                // Tr.error(tc, "error.urlPattern", servletName);
                continue;
            }
            addresses.add(mapping);
        }
        return addresses;
    }

}
