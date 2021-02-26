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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.javaee.ddmodel.wsbnd.Port;
import com.ibm.ws.javaee.ddmodel.wsbnd.Properties;
import com.ibm.ws.javaee.ddmodel.wsbnd.ServiceRef;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.WsBndConstants;

@Component(configurationPid = "com.ibm.ws.javaee.ddmodel.wsbnd.ServiceRef",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = "service.vendor = IBM")
public class ServiceRefComponentImpl implements ServiceRef {

    private String name;
    private String componentName;
    private String portAddress;
    private String wsdlLocation;
    private Properties properties;
    private final List<Port> ports = new ArrayList<Port>();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               name = ServiceRef.PROPERTIES_ELEMENT_NAME,
               target = WsBndConstants.ID_UNBOUND)
    protected void setProperties(Properties value) {
        this.properties = value;
    }

    protected void unsetProperties(Properties value) {
        this.properties = null;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               name = ServiceRef.PORT_ELEMENT_NAME,
               target = WsBndConstants.ID_UNBOUND)
    protected void setPort(Port port) {
        this.ports.add(port);
    }

    protected void unsetPort(Port port) {
        this.ports.remove(port);
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        name = (String) config.get(ServiceRef.NAME_ATTRIBUTE_NAME);
        componentName = (String) config.get(ServiceRef.COMPONENT_NAME_ATTRIBUTE_NAME);
        wsdlLocation = (String) config.get(ServiceRef.WSDL_LOCATION_ATTRIBUTE_NAME);
        portAddress = (String) config.get(ServiceRef.PORT_ADDRESS_ATTRIBUTE_NAME);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getComponentName() {
        return componentName;
    }

    @Override
    public String getPortAddress() {
        return portAddress;
    }

    @Override
    public String getWsdlLocation() {
        return wsdlLocation;
    }

    @Override
    public List<Port> getPorts() {
        return ports;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties == null ? null : properties.getAttributes();
    }
}
