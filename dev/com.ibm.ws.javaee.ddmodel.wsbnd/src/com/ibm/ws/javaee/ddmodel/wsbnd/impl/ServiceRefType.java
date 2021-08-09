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
package com.ibm.ws.javaee.ddmodel.wsbnd.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.wsbnd.Port;
import com.ibm.ws.javaee.ddmodel.wsbnd.ServiceRef;
import com.ibm.ws.javaee.ddmodel.wsbnd.internal.StringUtils;

/*
 <xsd:complexType name="serviceRefType">
 <xsd:sequence>
 <xsd:element name="port" type="ws:portType" minOccurs="0" maxOccurs="unbounded" />
 <xsd:element name="properties" minOccurs="0" type="ws:propertiesType" />
 </xsd:sequence>
 <xsd:attribute name="name" type="xsd:string" use="required" />
 <xsd:attribute name="component-name" type="xsd:string" />
 <xsd:attribute name="port-address" type="xsd:string" />
 <xsd:attribute name="wsdl-location" type="xsd:string" />
 </xsd:complexType>
 */
public class ServiceRefType extends DDParser.ElementContentParsable implements ServiceRef {
    private StringType name;
    private StringType componentName;
    private StringType portAddress;
    private StringType wsdlLocation;
    private Map<QName, PortType> portMap;
    private PropertiesType properties;

    @Override
    public String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public String getComponentName() {
        return componentName != null ? componentName.getValue() : null;
    }

    @Override
    public String getPortAddress() {
        return portAddress != null ? portAddress.getValue() : null;
    }

    @Override
    public String getWsdlLocation() {
        return wsdlLocation != null ? wsdlLocation.getValue() : null;
    }

    @Override
    public List<Port> getPorts() {
        List<Port> portList = null;
        if (null != portMap) {
            portList = new ArrayList<Port>(portMap.values());
        }
        return portList;
    }

    @Override
    public Map<String, String> getProperties() {
        return null != properties ? properties.getAttributes() : null;
    }

    /**
     * parse the name and address attributes defined in the element.
     */
    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        boolean result = false;

        if (nsURI != null) {
            return result;
        }

        if (NAME_ATTRIBUTE_NAME.equals(localName)) {
            name = parser.parseStringAttributeValue(index);
            result = true;
        } else if (COMPONENT_NAME_ATTRIBUTE_NAME.equals(localName)) {
            componentName = parser.parseStringAttributeValue(index);
            result = true;
        } else if (PORT_ADDRESS_ATTRIBUTE_NAME.equals(localName)) {
            portAddress = parser.parseStringAttributeValue(index);
            result = true;
        } else if (WSDL_LOCATION_ATTRIBUTE_NAME.equals(localName)) {
            wsdlLocation = parser.parseStringAttributeValue(index);
            result = true;
        }

        return result;

    }

    @Override
    public boolean handleChild(DDParser parser, String localName)
                    throws ParseException {

        if (PORT_ELEMENT_NAME.equals(localName)) {
            PortType port = new PortType();
            parser.parse(port);
            String portName = port.getName();
            if (StringUtils.isEmpty(portName)) {
                throw new ParseException(parser.requiredAttributeMissing("name"));
            }

            addEndpoint(port);
            return true;
        } else if (PROPERTIES_ELEMENT_NAME.equals(localName)) {
            properties = new PropertiesType();
            parser.parse(properties);
            return true;
        }

        return false;
    }

    private void addEndpoint(PortType port) {
        String nameSpace = port.getNamespace();
        String portName = port.getName();

        if (null == portMap) {
            portMap = new HashMap<QName, PortType>();
        }

        QName portQName = StringUtils.buildQName(nameSpace, portName);
        portMap.put(portQName, port);
    }

    @Override
    public void describe(Diagnostics diag) {
        diag.describeIfSet(NAME_ATTRIBUTE_NAME, name);
        diag.describeIfSet(COMPONENT_NAME_ATTRIBUTE_NAME, componentName);
        diag.describeIfSet(PORT_ADDRESS_ATTRIBUTE_NAME, portAddress);
        diag.describeIfSet(WSDL_LOCATION_ATTRIBUTE_NAME, wsdlLocation);

        diag.describeIfSet(PROPERTIES_ELEMENT_NAME, properties);

        diag.append("[" + PORT_ELEMENT_NAME + "<");
        if (null != portMap) {
            String prefix = "";
            for (PortType port : portMap.values()) {
                diag.append(prefix);
                port.describe(diag);
                prefix = ",";
            }
        }
        diag.append(">]");
    }

    //ONLY used by WsClientBinding
    public static ServiceRefType createServiceRefType(StringType name, Map<QName, PortType> portMap) {
        ServiceRefType serviceRefType = new ServiceRefType();
        serviceRefType.name = name;
        serviceRefType.portMap = portMap;

        return serviceRefType;

    }
}
