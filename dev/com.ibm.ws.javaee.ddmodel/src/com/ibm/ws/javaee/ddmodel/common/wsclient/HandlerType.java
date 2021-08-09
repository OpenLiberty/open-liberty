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

import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.common.QName;
import com.ibm.ws.javaee.dd.common.wsclient.Handler;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.DescriptionGroup;
import com.ibm.ws.javaee.ddmodel.common.ParamValueType;
import com.ibm.ws.javaee.ddmodel.common.XSDQNameType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="handlerType">
 <xsd:sequence>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="handler-name"
 type="javaee:xsdTokenType">
 </xsd:element>
 <xsd:element name="handler-class"
 type="javaee:fully-qualified-classType">
 </xsd:element>
 <xsd:element name="init-param"
 type="javaee:param-valueType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 <xsd:element name="soap-header"
 type="javaee:xsdQNameType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 <xsd:element name="soap-role"
 type="javaee:xsdTokenType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 <xsd:element name="port-name"
 type="javaee:xsdTokenType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class HandlerType extends DescriptionGroup implements Handler {

    public static class ListType extends ParsableListImplements<HandlerType, Handler> {
        @Override
        public HandlerType newInstance(DDParser parser) {
            return new HandlerType();
        }
    }

    @Override
    public String getHandlerName() {
        return handler_name.getValue();
    }

    @Override
    public String getHandlerClassName() {
        return handler_class.getValue();
    }

    @Override
    public List<ParamValue> getInitParams() {
        if (init_param != null) {
            return init_param.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<QName> getSoapHeaders() {
        if (soap_header != null) {
            return soap_header.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getSoapRoles() {
        if (soap_role != null) {
            return soap_role.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<String> getPortNames() {
        if (port_name != null) {
            return port_name.getList();
        } else {
            return Collections.emptyList();
        }
    }

    // elements
    // DescriptionGroup fields appear here in sequence
    XSDTokenType handler_name = new XSDTokenType();
    XSDTokenType handler_class = new XSDTokenType();
    ParamValueType.ListType init_param;
    XSDQNameType.ListType soap_header;
    XSDTokenType.ListType soap_role;
    XSDTokenType.ListType port_name;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("handler-name".equals(localName)) {
            parser.parse(handler_name);
            return true;
        }
        if ("handler-class".equals(localName)) {
            parser.parse(handler_class);
            return true;
        }
        if ("init-param".equals(localName)) {
            ParamValueType init_param = new ParamValueType();
            parser.parse(init_param);
            addInitParam(init_param);
            return true;
        }
        if ("soap-header".equals(localName)) {
            XSDQNameType soap_header = new XSDQNameType();
            parser.parse(soap_header);
            soap_header.resolve(parser);
            addSOAPHeader(soap_header);
            return true;
        }
        if ("soap-role".equals(localName)) {
            XSDTokenType soap_role = new XSDTokenType();
            parser.parse(soap_role);
            addSOAPRole(soap_role);
            return true;
        }
        if ("port-name".equals(localName)) {
            XSDTokenType port_name = new XSDTokenType();
            parser.parse(port_name);
            addPortName(port_name);
            return true;
        }
        return false;
    }

    private void addInitParam(ParamValueType init_param) {
        if (this.init_param == null) {
            this.init_param = new ParamValueType.ListType();
        }
        this.init_param.add(init_param);
    }

    private void addSOAPHeader(XSDQNameType soap_header) {
        if (this.soap_header == null) {
            this.soap_header = new XSDQNameType.ListType();
        }
        this.soap_header.add(soap_header);
    }

    private void addSOAPRole(XSDTokenType soap_role) {
        if (this.soap_role == null) {
            this.soap_role = new XSDTokenType.ListType();
        }
        this.soap_role.add(soap_role);
    }

    private void addPortName(XSDTokenType port_name) {
        if (this.port_name == null) {
            this.port_name = new XSDTokenType.ListType();
        }
        this.port_name.add(port_name);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describe("handler-name", handler_name);
        diag.describe("handler-class", handler_class);
        diag.describeIfSet("init-param", init_param);
        diag.describeIfSet("soap-header", soap_header);
        diag.describeIfSet("soap-role", soap_role);
        diag.describeIfSet("port-name", port_name);
    }
}
