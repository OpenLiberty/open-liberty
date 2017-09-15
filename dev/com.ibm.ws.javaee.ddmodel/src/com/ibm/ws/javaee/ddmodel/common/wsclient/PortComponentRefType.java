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
package com.ibm.ws.javaee.ddmodel.common.wsclient;

import com.ibm.ws.javaee.dd.common.wsclient.Addressing;
import com.ibm.ws.javaee.dd.common.wsclient.PortComponentRef;
import com.ibm.ws.javaee.dd.common.wsclient.RespectBinding;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.XSDBooleanType;
import com.ibm.ws.javaee.ddmodel.common.XSDIntegerType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 * <xsd:complexType name="port-component-refType">
 * <xsd:sequence>
 * <xsd:element name="service-endpoint-interface"
 * type="javaee:fully-qualified-classType">
 * </xsd:element>
 * <xsd:element name="enable-mtom"
 * type="javaee:true-falseType"
 * minOccurs="0"
 * maxOccurs="1">
 * </xsd:element>
 * <xsd:element name="mtom-threshold"
 * type="javaee:xsdNonNegativeIntegerType"
 * minOccurs="0"
 * maxOccurs="1">
 * </xsd:element>
 * <xsd:element name="addressing"
 * type="javaee:addressingType"
 * minOccurs="0"
 * maxOccurs="1">
 * </xsd:element>
 * <xsd:element name="respect-binding"
 * type="javaee:respect-bindingType"
 * minOccurs="0"
 * maxOccurs="1">
 * </xsd:element>
 * <xsd:element name="port-component-link"
 * type="javaee:xsdTokenType"
 * minOccurs="0"
 * maxOccurs="1">
 * </xsd:element>
 * </xsd:sequence>
 * <xsd:attribute name="id"
 * type="xsd:ID"/>
 * </xsd:complexType>
 */
public class PortComponentRefType extends DDParser.ElementContentParsable implements PortComponentRef {

    public static class ListType extends ParsableListImplements<PortComponentRefType, PortComponentRef> {
        @Override
        public PortComponentRefType newInstance(DDParser parser) {
            return new PortComponentRefType();
        }
    }

    @Override
    public String getServiceEndpointInterfaceName() {
        return service_endpoint_interface.getValue();
    }

    @Override
    public boolean isSetEnableMtom() {
        return AnySimpleType.isSet(enable_mtom);
    }

    @Override
    public boolean isEnableMtom() {
        return enable_mtom != null && enable_mtom.getBooleanValue();
    }

    @Override
    public boolean isSetMtomThreshold() {
        return AnySimpleType.isSet(mtom_threshold);
    }

    @Override
    public int getMtomThreshold() {
        return mtom_threshold != null ? mtom_threshold.getIntValue() : 0;
    }

    @Override
    public Addressing getAddressing() {
        return addressing;
    }

    @Override
    public RespectBinding getRespectBinding() {
        return respect_binding;
    }

    @Override
    public String getPortComponentLink() {
        return port_component_link != null ? port_component_link.getValue() : null;
    }

    // elements
    XSDTokenType service_endpoint_interface = new XSDTokenType();
    XSDBooleanType enable_mtom;
    XSDIntegerType mtom_threshold;
    AddressingType addressing;
    RespectBindingType respect_binding;
    XSDTokenType port_component_link;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("service-endpoint-interface".equals(localName)) {
            parser.parse(service_endpoint_interface);
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
        if ("port-component-link".equals(localName)) {
            XSDTokenType port_component_link = new XSDTokenType();
            parser.parse(port_component_link);
            this.port_component_link = port_component_link;
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("service-endpoint-interface", service_endpoint_interface);
        diag.describeIfSet("enable-mtom", enable_mtom);
        diag.describeIfSet("mtom-threshold", mtom_threshold);
        diag.describeIfSet("addressing", addressing);
        diag.describeIfSet("respect-binding", respect_binding);
        diag.describeIfSet("port-component-link", port_component_link);
    }
}
