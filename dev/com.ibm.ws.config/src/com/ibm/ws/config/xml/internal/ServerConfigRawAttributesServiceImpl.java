/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.config.xml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.config.xml.ServerConfigRawAttributesService;

@Component(service = { ServerConfigRawAttributesService.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE)
public class ServerConfigRawAttributesServiceImpl implements ServerConfigRawAttributesService {

    private volatile SystemConfiguration systemConfiguration;

    public ServerConfigRawAttributesServiceImpl() {
    }

    @Activate
    protected void activate(BundleContext context) {
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected void setSystemConfiguration(SystemConfiguration sc) {
        this.systemConfiguration = sc;
    }

    protected void unsetSystemConfiguration(SystemConfiguration sc) {
        if (sc == this.systemConfiguration) {
            this.systemConfiguration = null;
        }
    }

    @Override
    public Map<String, Object> getRawAttributesFromElement(String elementName) {
        ServerConfiguration serverConfig = systemConfiguration.getServerConfiguration();
        ConfigurationList<SimpleElement> listDefault = serverConfig.getDefaultConfiguration().getConfigurationList(elementName);
        ConfigurationList<SimpleElement> listServerConfig = serverConfig.getConfigurationList(elementName);

        List<SimpleElement> elements = new ArrayList<SimpleElement>();

        // Reader order is: Default -> last read values in element in the server.xml (top -> down).
        elements = listDefault.collectElements(elements);
        elements = listServerConfig.collectElements(elements);

        /*
         * Need to merge the attribute values just in case there are multiple
         * elements with values.
         */
        if (!elements.isEmpty()) {
            Map<String, Object> mergedAttribute = new HashMap<String, Object>();

            for (SimpleElement se : elements) {
                mergedAttribute.putAll(se.getAttributes());
            }

            return mergedAttribute;
        } else {
            return new HashMap<String, Object>();
        }
    }

}
