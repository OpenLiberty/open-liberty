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

public class SessionType extends com.ibm.ws.javaee.ddmodel.ejbbnd.EnterpriseBeanType implements com.ibm.ws.javaee.dd.ejbbnd.Session {
    public SessionType() {
        this(false);
    }

    public SessionType(boolean xmi) {
        super(xmi);
    }

    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.ejbbnd.InterfaceType, com.ibm.ws.javaee.dd.ejbbnd.Interface> interface_;
    com.ibm.ws.javaee.ddmodel.StringType simple_binding_name;
    com.ibm.ws.javaee.ddmodel.StringType component_id;
    com.ibm.ws.javaee.ddmodel.StringType remote_home_binding_name;
    com.ibm.ws.javaee.ddmodel.StringType local_home_binding_name;

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.ejbbnd.Interface> getInterfaces() {
        if (interface_ != null) {
            return interface_.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.lang.String getSimpleBindingName() {
        return simple_binding_name != null ? simple_binding_name.getValue() : null;
    }

    @Override
    public java.lang.String getComponentID() {
        return component_id != null ? component_id.getValue() : null;
    }

    @Override
    public java.lang.String getRemoteHomeBindingName() {
        return remote_home_binding_name != null ? remote_home_binding_name.getValue() : null;
    }

    @Override
    public java.lang.String getLocalHomeBindingName() {
        return local_home_binding_name != null ? local_home_binding_name.getValue() : null;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ((xmi ? "jndiName" : "simple-binding-name").equals(localName)) {
                this.simple_binding_name = parser.parseStringAttributeValue(index);
                return true;
            }
            if (!xmi && "component-id".equals(localName)) {
                this.component_id = parser.parseStringAttributeValue(index);
                return true;
            }
            if (!xmi && "remote-home-binding-name".equals(localName)) {
                this.remote_home_binding_name = parser.parseStringAttributeValue(index);
                return true;
            }
            if (!xmi && "local-home-binding-name".equals(localName)) {
                this.local_home_binding_name = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return super.handleAttribute(parser, nsURI, localName, index);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (!xmi && "interface".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.ejbbnd.InterfaceType interface_ = new com.ibm.ws.javaee.ddmodel.ejbbnd.InterfaceType();
            parser.parse(interface_);
            this.addInterface(interface_);
            return true;
        }
        return super.handleChild(parser, localName);
    }

    void addInterface(com.ibm.ws.javaee.ddmodel.ejbbnd.InterfaceType interface_) {
        if (this.interface_ == null) {
            this.interface_ = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.ejbbnd.InterfaceType, com.ibm.ws.javaee.dd.ejbbnd.Interface>();
        }
        this.interface_.add(interface_);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet(xmi ? "jndiName" : "simple-binding-name", simple_binding_name);
        diag.describeIfSet("component-id", component_id);
        diag.describeIfSet("remote-home-binding-name", remote_home_binding_name);
        diag.describeIfSet("local-home-binding-name", local_home_binding_name);
        diag.describeIfSet("interface", interface_);
    }
}
