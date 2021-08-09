/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.javaee.ddmodel.wsbnd.Properties;
import com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceEndpoint;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.WsBndConstants;

@Component(configurationPid = "com.ibm.ws.javaee.ddmodel.wsbnd.WebserviceEndpoint",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = "service.vendor = IBM")
public class WebserviceEndpointComponentImpl implements WebserviceEndpoint {

    private String portComponentName;
    private String address;
    private Properties properties;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               name = WebserviceEndpoint.PROPERTIES_ELEMENT_NAME,
               target = WsBndConstants.ID_UNBOUND)
    protected void setProperties(Properties value) {
        this.properties = value;
    }

    protected void unsetProperties(Properties value) {
        this.properties = null;
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        portComponentName = (String) config.get(WebserviceEndpoint.PORT_COMPONENT_NAME_ATTRIBUTE_NAME);
        address = (String) config.get(WebserviceEndpoint.ADDRESS_ATTRIBUTE_NAME);
    }

    @Deactivate
    protected void deactivate() {
        portComponentName = null;
        address = null;
    }

    @Override
    public String getPortComponentName() {
        return portComponentName;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties == null ? null : properties.getAttributes();
    }
}
