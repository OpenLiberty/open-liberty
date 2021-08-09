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

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.QName;
import com.ibm.ws.javaee.dd.common.wsclient.Handler;
import com.ibm.ws.javaee.dd.common.wsclient.HandlerChain;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.QNameType;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.TokenType;

/*
 * <xsd:complexType name="handler-chainType">
 * <xsd:sequence>
 * <xsd:choice minOccurs="0"
 * maxOccurs="1">
 * <xsd:element name="service-name-pattern"
 * type="javaee:qname-pattern"/>
 * <xsd:element name="port-name-pattern"
 * type="javaee:qname-pattern"/>
 * <xsd:element name="protocol-bindings"
 * type="javaee:protocol-bindingListType"/>
 * </xsd:choice>
 * <xsd:element name="handler"
 * type="javaee:handlerType"
 * minOccurs="1"
 * maxOccurs="unbounded"/>
 * </xsd:sequence>
 * <xsd:attribute name="id"
 * type="xsd:ID"/>
 * </xsd:complexType>
 * 
 * <xsd:simpleType name="protocol-bindingListType">
 * <xsd:list itemType="javaee:protocol-bindingType"/>
 * </xsd:simpleType>
 * 
 * <xsd:simpleType name="protocol-bindingType">
 * <xsd:union memberTypes="xsd:anyURI javaee:protocol-URIAliasType"/>
 * </xsd:simpleType>
 * 
 * <xsd:simpleType name="protocol-URIAliasType">
 * <xsd:restriction base="xsd:token">
 * <xsd:pattern value="##.+"/>
 * </xsd:restriction>
 * </xsd:simpleType>
 */
public class HandlerChainType extends DDParser.ElementContentParsable implements HandlerChain {

    public static class ListType extends ParsableListImplements<HandlerChainType, HandlerChain> {
        @Override
        public HandlerChainType newInstance(DDParser parser) {
            return new HandlerChainType();
        }
    }

    @Override
    public QName getServiceNamePattern() {
        return service_name_pattern;
    }

    @Override
    public QName getPortNamePattern() {
        return port_name_pattern;
    }

    @Override
    public List<String> getProtocolBindings() {
        if (protocol_bindings != null) {
            return protocol_bindings.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Handler> getHandlers() {
        return handler.getList();
    }

    // elements
    // choice [0,1] {
    QNameType service_name_pattern;
    QNameType port_name_pattern;
    TokenType.ListType protocol_bindings;
    // } choice
    HandlerType.ListType handler = new HandlerType.ListType();

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("service-name-pattern".equals(localName)) {
            StringType service_name_pattern = new StringType();
            parser.parse(service_name_pattern);
            this.service_name_pattern = parser.parseQName(service_name_pattern.getValue());
            this.service_name_pattern.resolve(parser);
            return true;
        }
        if ("port-name-pattern".equals(localName)) {
            StringType port_name_pattern = new StringType();
            parser.parse(port_name_pattern);
            this.port_name_pattern = parser.parseQName(port_name_pattern.getValue());
            this.port_name_pattern.resolve(parser);
            return true;
        }
        if ("protocol-bindings".equals(localName)) {
            TokenType protocol_bindings = new TokenType();
            parser.parse(protocol_bindings);
            this.protocol_bindings = protocol_bindings.split(parser, " ");
            return true;
        }
        if ("handler".equals(localName)) {
            HandlerType handler = new HandlerType();
            parser.parse(handler);
            addHandler(handler);
            return true;
        }
        return false;
    }

    private void addHandler(HandlerType handler) {
        this.handler.add(handler);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("service-name-pattern", service_name_pattern);
        diag.describeIfSet("port-name-pattern", port_name_pattern);
        diag.describeIfSet("protocol-bindings", protocol_bindings);
        diag.describe("handler", handler);
    }
}