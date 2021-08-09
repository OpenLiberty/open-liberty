/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.MailSession;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

public class MailSessionType extends JNDIEnvironmentRefType implements MailSession {
    public static class ListType extends ParsableListImplements<MailSessionType, MailSession> {
        @Override
        public MailSessionType newInstance(DDParser parser) {
            return new MailSessionType();
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
    public String getStoreProtocol() {
        return store_protocol != null ? store_protocol.getValue() : null;
    }

    @Override
    public String getStoreProtocolClassName() {
        return store_protocol_class != null ? store_protocol_class.getValue() : null;
    }

    @Override
    public String getTransportProtocol() {
        return transport_protocol != null ? transport_protocol.getValue() : null;
    }

    @Override
    public String getTransportProtocolClassName() {
        return transport_protocol_class != null ? transport_protocol_class.getValue() : null;
    }

    @Override
    public String getHost() {
        return host != null ? host.getValue() : null;
    }

    @Override
    public String getUser() {
        return user != null ? user.getValue() : null;
    }

    @Override
    public String getPassword() {
        return password != null ? password.getValue() : null;
    }

    @Override
    public String getFrom() {
        return from != null ? from.getValue() : null;
    }

    @Override
    public List<Property> getProperties() {
        if (property != null) {
            return property.getList();
        } else {
            return Collections.emptyList();
        }
    }

    // elements
    DescriptionType.ListType description;
    XSDTokenType store_protocol;
    XSDTokenType store_protocol_class;
    XSDTokenType transport_protocol;
    XSDTokenType transport_protocol_class;
    XSDTokenType host;
    XSDTokenType user;
    XSDTokenType password;
    XSDTokenType from;
    PropertyType.ListType property;

    public MailSessionType() {
        super("name");
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
        if ("store-protocol".equals(localName)) {
            XSDTokenType store_protocol = new XSDTokenType();
            parser.parse(store_protocol);
            this.store_protocol = store_protocol;
            return true;
        }
        if ("store-protocol-class".equals(localName)) {
            XSDTokenType store_protocol_class = new XSDTokenType();
            parser.parse(store_protocol_class);
            this.store_protocol_class = store_protocol_class;
            return true;
        }
        if ("transport-protocol".equals(localName)) {
            XSDTokenType transport_protocol = new XSDTokenType();
            parser.parse(transport_protocol);
            this.transport_protocol = transport_protocol;
            return true;
        }
        if ("transport-protocol-class".equals(localName)) {
            XSDTokenType transport_protocol_class = new XSDTokenType();
            parser.parse(transport_protocol_class);
            this.transport_protocol_class = transport_protocol_class;
            return true;
        }
        if ("host".equals(localName)) {
            XSDTokenType host = new XSDTokenType();
            parser.parse(host);
            this.host = host;
            return true;
        }
        if ("user".equals(localName)) {
            XSDTokenType user = new XSDTokenType();
            parser.parse(user);
            this.user = user;
            return true;
        }
        if ("password".equals(localName)) {
            XSDTokenType password = new XSDTokenType();
            parser.parse(password);
            this.password = password;
            return true;
        }
        if ("from".equals(localName)) {
            XSDTokenType from = new XSDTokenType();
            parser.parse(from);
            this.from = from;
            return true;
        }
        if ("property".equals(localName)) {
            PropertyType property = new PropertyType();
            parser.parse(property);
            addProperty(property);
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

    private void addProperty(PropertyType property) {
        if (this.property == null) {
            this.property = new PropertyType.ListType();
        }
        this.property.add(property);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("description", description);
        super.describe(diag);
        diag.describeIfSet("store-protocol", store_protocol);
        diag.describeIfSet("store-protocol-class", store_protocol_class);
        diag.describeIfSet("transport-protocol", transport_protocol);
        diag.describeIfSet("transport-protocol-class", transport_protocol_class);
        diag.describeIfSet("host", host);
        diag.describeIfSet("user", user);
        diag.describeIfSet("password", password);
        diag.describeIfSet("from", from);
        diag.describeIfSet("property", property);
    }
}
