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
package com.ibm.ws.javaee.ddmodel.ws;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.ws.PortComponent;
import com.ibm.ws.javaee.dd.ws.WebserviceDescription;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.DescriptionType;
import com.ibm.ws.javaee.ddmodel.common.DisplayNameType;
import com.ibm.ws.javaee.ddmodel.common.IconType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="webservice-descriptionType">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0"
 maxOccurs="1"/>
 <xsd:element name="display-name"
 type="javaee:display-nameType"
 minOccurs="0"
 maxOccurs="1"/>
 <xsd:element name="icon"
 type="javaee:iconType"
 minOccurs="0"
 maxOccurs="1"/>
 <xsd:element name="webservice-description-name"
 type="javaee:string">
 </xsd:element>
 <xsd:element name="wsdl-file"
 type="javaee:pathType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="jaxrpc-mapping-file"
 type="javaee:pathType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="port-component"
 type="javaee:port-componentType"
 minOccurs="1"
 maxOccurs="unbounded">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */
public class WebserviceDescriptionType extends DDParser.ElementContentParsable implements WebserviceDescription {

    public static class ListType extends ParsableListImplements<WebserviceDescriptionType, WebserviceDescription> {
        @Override
        public WebserviceDescriptionType newInstance(DDParser parser) {
            return new WebserviceDescriptionType();
        }
    }

    @Override
    public Description getDescription() {
        return description;
    }

    @Override
    public DisplayName getDisplayName() {
        return display_name;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public String getWebserviceDescriptionName() {

        return webservice_description_name.getValue();
    }

    @Override
    public String getWSDLFile() {
        return wsdl_file != null ? wsdl_file.getValue() : null;
    }

    @Override
    public String getJAXRPCMappingFile() {
        return jaxrpc_mapping_file != null ? jaxrpc_mapping_file.getValue() : null;
    }

    @Override
    public List<PortComponent> getPortComponents() {
        if (port_component != null) {
            return port_component.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("description".equals(localName)) {
            DescriptionType description = new DescriptionType();
            parser.parse(description);
            this.description = description;
            return true;
        }
        if ("display-name".equals(localName)) {
            DisplayNameType display_name = new DisplayNameType();
            parser.parse(display_name);
            this.display_name = display_name;
            return true;
        }
        if ("icon".equals(localName)) {
            IconType icon = new IconType();
            parser.parse(icon);
            this.icon = icon;
            return true;
        }
        if ("webservice-description-name".equals(localName)) {

            parser.parse(this.webservice_description_name);
            return true;
        }
        if ("wsdl-file".equals(localName)) {
            XSDTokenType wsdl_file = new XSDTokenType();
            parser.parse(wsdl_file);
            this.wsdl_file = wsdl_file;
            return true;
        }
        if ("jaxrpc-mapping-file".equals(localName)) {
            XSDTokenType jaxrpc_mapping_file = new XSDTokenType();
            parser.parse(jaxrpc_mapping_file);
            this.jaxrpc_mapping_file = jaxrpc_mapping_file;
            return true;
        }
        if ("port-component".equals(localName)) {
            PortComponentType portComp = new PortComponentType();
            parser.parse(portComp);
            addPortComponent(portComp);
            return true;
        }
        return false;
    }

    private void addPortComponent(PortComponentType portComp) {
        if (this.port_component == null) {
            this.port_component = new PortComponentType.ListType();
        }
        this.port_component.add(portComp);
    }

    @Override
    public void describe(Diagnostics diag) {
        diag.describeIfSet("description", description);
        diag.describeIfSet("display-name", display_name);
        diag.describeIfSet("icon", icon);
        diag.describe("webservice-description-name", webservice_description_name);
        diag.describeIfSet("wsdl-file", wsdl_file);
        diag.describeIfSet("jaxrpc-mapping-file", jaxrpc_mapping_file);
        diag.describeIfSet("port-component", port_component);
    }

    // key port-component-name-key
    Map<XSDTokenType, PortComponentType> portComponentNameToPortComponentMap;

    // elements
    DescriptionType description;
    DisplayNameType display_name;
    IconType icon;
    XSDTokenType webservice_description_name = new XSDTokenType();;
    XSDTokenType wsdl_file;
    XSDTokenType jaxrpc_mapping_file;
    PortComponentType.ListType port_component;
}
