/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class MessageDrivenType extends com.ibm.ws.javaee.ddmodel.ejbbnd.EnterpriseBeanType implements com.ibm.ws.javaee.dd.ejbbnd.MessageDriven {
    public MessageDrivenType() {
        this(false);
    }

    public MessageDrivenType(boolean xmi) {
        super(xmi);
    }

    com.ibm.ws.javaee.ddmodel.ejbbnd.ListenerPortType listener_port;
    com.ibm.ws.javaee.ddmodel.ejbbnd.JCAAdapterType jca_adapter;

    @Override
    public com.ibm.ws.javaee.dd.ejbbnd.ListenerPort getListenerPort() {
        return listener_port;
    }

    @Override
    public com.ibm.ws.javaee.dd.ejbbnd.JCAAdapter getJCAAdapter() {
        return jca_adapter;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (xmi && "activationSpecJndiName".equals(localName)) {
                if (this.jca_adapter == null) {
                    this.jca_adapter = new com.ibm.ws.javaee.ddmodel.ejbbnd.JCAAdapterType(true);
                }
                this.jca_adapter.activation_spec_binding_name = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "activationSpecAuthAlias".equals(localName)) {
                if (this.jca_adapter == null) {
                    this.jca_adapter = new com.ibm.ws.javaee.ddmodel.ejbbnd.JCAAdapterType(true);
                }
                this.jca_adapter.activation_spec_auth_alias = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "destinationJndiName".equals(localName)) {
                if (this.jca_adapter == null) {
                    this.jca_adapter = new com.ibm.ws.javaee.ddmodel.ejbbnd.JCAAdapterType(true);
                }
                this.jca_adapter.destination_binding_name = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        if (xmi && "http://www.omg.org/XMI".equals(nsURI)) {
            if ("type".equals(localName)) {
                String type = parser.getAttributeValue(index);
                if (type.endsWith(":MessageDrivenBeanBinding") && "ejbbnd.xmi".equals(parser.getNamespaceURI(type.substring(0, type.length() - ":MessageDrivenBeanBinding".length())))) {
                    // Allowed but ignored.
                    return true;
                }
            }
        }
        return super.handleAttribute(parser, nsURI, localName, index);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (!xmi && "listener-port".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbbnd.ListenerPortType listener_port = new com.ibm.ws.javaee.ddmodel.ejbbnd.ListenerPortType();
            parser.parse(listener_port);
            this.listener_port = listener_port;
            return true;
        }
        if (!xmi && "jca-adapter".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbbnd.JCAAdapterType jca_adapter = new com.ibm.ws.javaee.ddmodel.ejbbnd.JCAAdapterType();
            parser.parse(jca_adapter);
            this.jca_adapter = jca_adapter;
            return true;
        }
        return super.handleChild(parser, localName);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("listener-port", listener_port);
        if (xmi) {
            if (jca_adapter != null) {
                jca_adapter.describe(diag);
            }
        } else {
            diag.describeIfSet("jca-adapter", jca_adapter);
        }
    }
}
