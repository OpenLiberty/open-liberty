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

import javax.xml.namespace.QName;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.dd.common.wsclient.Addressing;
import com.ibm.ws.javaee.dd.common.wsclient.Handler;
import com.ibm.ws.javaee.dd.common.wsclient.HandlerChain;
import com.ibm.ws.javaee.dd.common.wsclient.RespectBinding;
import com.ibm.ws.javaee.dd.ws.PortComponent;
import com.ibm.ws.javaee.dd.ws.ServiceImplBean;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.TokenType;
import com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.DescriptionType;
import com.ibm.ws.javaee.ddmodel.common.DisplayNameType;
import com.ibm.ws.javaee.ddmodel.common.IconType;
import com.ibm.ws.javaee.ddmodel.common.XSDBooleanType;
import com.ibm.ws.javaee.ddmodel.common.XSDIntegerType;
import com.ibm.ws.javaee.ddmodel.common.XSDQNameType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;
import com.ibm.ws.javaee.ddmodel.common.wsclient.AddressingType;
import com.ibm.ws.javaee.ddmodel.common.wsclient.HandlerChainsType;
import com.ibm.ws.javaee.ddmodel.common.wsclient.HandlerType;
import com.ibm.ws.javaee.ddmodel.common.wsclient.RespectBindingType;

/*
 <xsd:key name="port-component_handler-name-key">
 <xsd:selector xpath="javaee:handler"/>
 <xsd:field xpath="javaee:handler-name"/>
 </xsd:key>

 <xsd:complexType name="port-componentType">
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
 <xsd:element name="port-component-name"
 type="javaee:string">
 </xsd:element>
 <xsd:element name="wsdl-service"
 type="javaee:xsdQNameType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="wsdl-port"
 type="javaee:xsdQNameType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="enable-mtom"
 type="javaee:true-falseType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="mtom-threshold"
 type="javaee:xsdNonNegativeIntegerType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="addressing"
 type="javaee:addressingType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="respect-binding"
 type="javaee:respect-bindingType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="protocol-binding"
 type="javaee:protocol-bindingType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="service-endpoint-interface"
 type="javaee:fully-qualified-classType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="service-impl-bean"
 type="javaee:service-impl-beanType"/>
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
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */
public class PortComponentType extends DDParser.ElementContentParsable implements
                PortComponent {

    public static class ListType extends
                    ParsableListImplements<PortComponentType, PortComponent> {
        @Override
        public PortComponentType newInstance(DDParser parser) {
            return new PortComponentType();
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

    /** {@inheritDoc} */
    @Override
    public Addressing getAddressing() {
        return addressing;
    }

    /** {@inheritDoc} */
    @Override
    public List<HandlerChain> getHandlerChains() {
        if (handler_chains != null) {
            return handler_chains.getList();
        } else {
            return Collections.emptyList();
        }
    }

    public List<Handler> getHandlers() {
        if (handler != null) {
            return handler.getList();
        } else {
            return Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getPortComponentName() {
        return port_component_name.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public String getProtocolBinding() {
        return protocol_binding != null ? protocol_binding.getValue() : null;
    }

    /** {@inheritDoc} */
    @Override
    public RespectBinding getRespectBinding() {
        return respect_binding;
    }

    /** {@inheritDoc} */
    @Override
    public String getServiceEndpointInterface() {
        return service_endpoint_interface != null ? service_endpoint_interface.getValue() : null;
    }

    /** {@inheritDoc} */
    @Override
    public ServiceImplBean getServiceImplBean() {
        return service_impl_bean;
    }

    /** {@inheritDoc} */
    @Override
    public QName getWSDLPort() {
        if (wsdl_port != null) {
            return new QName(wsdl_port.getNamespaceURI(), wsdl_port
                            .getLocalPart());
        } else
            return null;
    }

    /** {@inheritDoc} */
    @Override
    public QName getWSDLService() {
        if (wsdl_service != null) {
            return new QName(wsdl_service.getNamespaceURI(), wsdl_service
                            .getLocalPart());
        } else
            return null;
    }

    @Override
    public boolean isEnableMTOM() {
        return enable_mtom != null && enable_mtom.getBooleanValue();
    }

    @Override
    public boolean isSetEnableMTOM() {
        return AnySimpleType.isSet(enable_mtom);
    }

    @Override
    public boolean isSetMTOMThreshold() {
        return AnySimpleType.isSet(mtom_threshold);
    }

    @Override
    public int getMTOMThreshold() {

        return mtom_threshold != null ? mtom_threshold.getIntValue() : 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean handleChild(DDParser parser, String localName)
                    throws ParseException {

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
        if ("port-component-name".equals(localName)) {
            parser.parse(port_component_name);
            return true;
        }
        if ("wsdl-service".equals(localName)) {
            XSDQNameType wsdl_service = new XSDQNameType();
            parser.parse(wsdl_service);
            wsdl_service.resolve(parser);
            this.wsdl_service = wsdl_service;
            return true;
        }
        if ("wsdl-port".equals(localName)) {
            XSDQNameType wsdl_port = new XSDQNameType();
            parser.parse(wsdl_port);
            wsdl_port.resolve(parser);
            this.wsdl_port = wsdl_port;
            return true;
        }
        if ("enable-mtom".equals(localName)) {
            XSDBooleanType enable_mtom = new XSDBooleanType();
            parser.parse(enable_mtom);
            this.enable_mtom = enable_mtom;
            return true;
        }
        if ("mtom-threshold".equals(localName)) {
            XSDIntegerType mtom_threshold = new XSDIntegerType();
            parser.parse(mtom_threshold);
            this.mtom_threshold = mtom_threshold;
            return true;
        }
        if ("addressing".equals(localName)) {
            AddressingType addressing = new AddressingType();
            parser.parse(addressing);
            this.addressing = addressing;
            return true;
        }
        if ("respect-binding".equals(localName)) {
            RespectBindingType respect_binding = new RespectBindingType();
            parser.parse(respect_binding);
            this.respect_binding = respect_binding;
            return true;
        }
        if ("protocol-binding".equals(localName)) {
            TokenType protocol_binding = new TokenType();
            parser.parse(protocol_binding);
            this.protocol_binding = protocol_binding;
            return true;
        }
        if ("service-endpoint-interface".equals(localName)) {
            XSDTokenType service_endpoint_interface = new XSDTokenType();
            parser.parse(service_endpoint_interface);
            this.service_endpoint_interface = service_endpoint_interface;
            return true;
        }
        if ("service-impl-bean".equals(localName)) {
            parser.parse(service_impl_bean);
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

    /** {@inheritDoc} */
    @Override
    public boolean isIdAllowed() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected String toTracingSafeString() {
        return "port-component";
    }

    private void addHandler(HandlerType handler) {
        if (this.handler == null) {
            this.handler = new HandlerType.ListType();
        }
        this.handler.add(handler);
    }

    @Override
    public void describe(Diagnostics diag) {

        diag.describeIfSet("description", description);
        diag.describeIfSet("display-name", display_name);
        diag.describeIfSet("icon", icon);
        diag.describe("port-component-name", port_component_name);
        diag.describeIfSet("wsdl-service", wsdl_service);
        diag.describeIfSet("wsdl-port", wsdl_port);
        diag.describeIfSet("enable-mtom", enable_mtom);
        diag.describeIfSet("mtom-threshold", mtom_threshold);
        diag.describeIfSet("addressing", addressing);
        diag.describeIfSet("respect-binding", respect_binding);
        diag.describeIfSet("protocol-binding", protocol_binding);
        diag.describeIfSet("service-endpoint-interface", service_endpoint_interface);
        diag.describe("service-impl-bean", service_impl_bean);
        diag.describeIfSet("handler", handler);
        diag.describeIfSet("handler-chains", handler_chains);
    }

    // key port-component_handler-name-key
    Map<XSDTokenType, HandlerType> handlerNameToHandlerMap;

    // elements
    DescriptionType description;
    DisplayNameType display_name;
    IconType icon;
    XSDTokenType port_component_name = new XSDTokenType();
    XSDQNameType wsdl_service;
    XSDQNameType wsdl_port;
    XSDBooleanType enable_mtom;
    XSDIntegerType mtom_threshold;
    AddressingType addressing;
    RespectBindingType respect_binding;
    TokenType protocol_binding;
    XSDTokenType service_endpoint_interface;
    ServiceImplBeanType service_impl_bean = new ServiceImplBeanType();
    // choice {
    HandlerType.ListType handler;
    HandlerChainsType handler_chains;
    // } choice

}
