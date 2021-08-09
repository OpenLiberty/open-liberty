/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common.wsclient;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.QName;
import com.ibm.ws.javaee.dd.common.wsclient.Handler;
import com.ibm.ws.javaee.dd.common.wsclient.HandlerChain;
import com.ibm.ws.javaee.dd.common.wsclient.PortComponentRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.DescriptionType;
import com.ibm.ws.javaee.ddmodel.common.DisplayNameType;
import com.ibm.ws.javaee.ddmodel.common.IconType;
import com.ibm.ws.javaee.ddmodel.common.ResourceGroup;
import com.ibm.ws.javaee.ddmodel.common.XSDQNameType;
import com.ibm.ws.javaee.ddmodel.common.XSDStringType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="service-refType">
 <xsd:sequence>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="service-ref-name"
 type="javaee:jndi-nameType">
 </xsd:element>
 <xsd:element name="service-interface"
 type="javaee:fully-qualified-classType">
 </xsd:element>
 <xsd:element name="service-ref-type"
 type="javaee:fully-qualified-classType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="wsdl-file"
 type="javaee:xsdAnyURIType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="jaxrpc-mapping-file"
 type="javaee:pathType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="service-qname"
 type="javaee:xsdQNameType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="port-component-ref"
 type="javaee:port-component-refType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 <xsd:choice>
 <xsd:element name="handler"
 type="javaee:handlerType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 <xsd:element name="handler-chains"
 type="javaee:handler-chainsType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 </xsd:choice>
 <xsd:group ref="javaee:resourceGroup"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class ServiceRefType extends ResourceGroup implements ServiceRef {

    public static class ListType extends ParsableListImplements<ServiceRefType, ServiceRef> {
        @Override
        public ServiceRefType newInstance(DDParser parser) {
            return new ServiceRefType();
        }
    }

    @Override
    public List<Description> getDescriptions() {
        if (description != null) {
            return description.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<DisplayName> getDisplayNames() {
        if (display_name != null) {
            return display_name.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Icon> getIcons() {
        if (icon != null) {
            return icon.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getServiceInterfaceName() {
        return service_interface.getValue();
    }

    @Override
    public String getServiceRefTypeName() {
        return service_ref_type != null ? service_ref_type.getValue() : null;
    }

    @Override
    public String getWsdlFile() {
        return wsdl_file != null ? wsdl_file.getValue() : null;
    }

    @Override
    public String getJaxrpcMappingFile() {
        return jaxrpc_mapping_file != null ? jaxrpc_mapping_file.getValue() : null;
    }

    @Override
    public QName getServiceQname() {
        return service_qname;
    }

    @Override
    public List<PortComponentRef> getPortComponentRefs() {
        if (port_component_ref != null) {
            return port_component_ref.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Handler> getHandlers() {
        if (handler != null) {
            return handler.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<HandlerChain> getHandlerChainList() {
        if (handler_chains != null && handler_chains.handler_chain != null) {
            return handler_chains.handler_chain.getList();
        } else {
            return Collections.emptyList();
        }
    }

    // elements
    DescriptionType.ListType description;
    DisplayNameType.ListType display_name;
    IconType.ListType icon;
    XSDTokenType service_interface = new XSDTokenType();
    XSDTokenType service_ref_type;
    XSDStringType wsdl_file;
    XSDTokenType jaxrpc_mapping_file;
    XSDQNameType service_qname;
    PortComponentRefType.ListType port_component_ref;
    // choice {
    HandlerType.ListType handler;
    HandlerChainsType handler_chains;

    // } choice
    // ResourceGroup fields appear here in sequence

    public ServiceRefType() {
        super("service-ref-name");
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("description".equals(localName)) {
            DescriptionType description = new DescriptionType();
            parser.parse(description);
            addDescription(description);
            return true;
        }
        if ("display-name".equals(localName)) {
            DisplayNameType display_name = new DisplayNameType();
            parser.parse(display_name);
            addDisplayName(display_name);
            return true;
        }
        if ("icon".equals(localName)) {
            IconType icon = new IconType();
            parser.parse(icon);
            addIcon(icon);
            return true;
        }
        if ("service-interface".equals(localName)) {
            parser.parse(service_interface);
            return true;
        }
        if ("service-ref-type".equals(localName)) {
            XSDTokenType service_ref_type = new XSDTokenType();
            parser.parse(service_ref_type);
            this.service_ref_type = service_ref_type;
            return true;
        }
        if ("wsdl-file".equals(localName)) {
            XSDStringType wsdl_file = new XSDStringType();
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
        if ("service-qname".equals(localName)) {
            XSDQNameType service_qname = new XSDQNameType();
            parser.parse(service_qname);
            service_qname.resolve(parser);
            this.service_qname = service_qname;
            return true;
        }
        if ("port-component-ref".equals(localName)) {
            PortComponentRefType port_component_ref = new PortComponentRefType();
            parser.parse(port_component_ref);
            addPortComponentRef(port_component_ref);
            return true;
        }
        if ("handler".equals(localName)) {
            HandlerType handler = new HandlerType();
            parser.parse(handler);
            addHandler(handler);
            return true;
        }
        if ("handler-chains".equals(localName)) {
            HandlerChainsType handler_chains = new HandlerChainsType();
            parser.parse(handler_chains);
            this.handler_chains = handler_chains;
            return true;
        }
        return false;
    }

    private void addDescription(DescriptionType description) {
        if (this.description == null) {
            this.description = new DescriptionType.ListType();
        }
        this.description.add(description);
    }

    private void addDisplayName(DisplayNameType display_name) {
        if (this.display_name == null) {
            this.display_name = new DisplayNameType.ListType();
        }
        this.display_name.add(display_name);
    }

    private void addIcon(IconType icon) {
        if (this.icon == null) {
            this.icon = new IconType.ListType();
        }
        this.icon.add(icon);
    }

    private void addPortComponentRef(PortComponentRefType port_component_ref) {
        if (this.port_component_ref == null) {
            this.port_component_ref = new PortComponentRefType.ListType();
        }
        this.port_component_ref.add(port_component_ref);
    }

    private void addHandler(HandlerType handler) {
        if (this.handler == null) {
            this.handler = new HandlerType.ListType();
        }
        this.handler.add(handler);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("description", description);
        diag.describeIfSet("display-name", display_name);
        diag.describeIfSet("icon", icon);
        diag.describe("service-interface", service_interface);
        diag.describeIfSet("service-ref-type", service_ref_type);
        diag.describeIfSet("wsdl-file", wsdl_file);
        diag.describeIfSet("jaxrpc-mapping-file", jaxrpc_mapping_file);
        diag.describeIfSet("service-qname", service_qname);
        diag.describeIfSet("port-component-ref", port_component_ref);
        diag.describeIfSet("handler", handler);
        diag.describeIfSet("handler-chains", handler_chains);
        super.describe(diag);
    }

    /*
     * <xsd:simpleType name="protocol-bindingListType">
     * <xsd:list itemType="javaee:protocol-bindingType"/>
     * </xsd:simpleType>
     */

    /*
     * <xsd:simpleType name="protocol-bindingType">
     * <xsd:union memberTypes="xsd:anyURI javaee:protocol-URIAliasType"/>
     * </xsd:simpleType>
     */

    /*
     * <xsd:simpleType name="protocol-URIAliasType">
     * <xsd:restriction base="xsd:token">
     * <xsd:pattern value="##.+"/>
     * </xsd:restriction>
     * </xsd:simpleType>
     */

    /*
     * <xsd:simpleType name="qname-pattern">
     * <xsd:restriction base="xsd:token">
     * <xsd:pattern value="\*|([\i-[:]][\c-[:]]*:)?[\i-[:]][\c-[:]]*\*?"/>
     * </xsd:restriction>
     * </xsd:simpleType>
     */

}
