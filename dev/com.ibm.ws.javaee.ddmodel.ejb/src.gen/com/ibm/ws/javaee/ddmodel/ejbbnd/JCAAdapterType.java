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

public class JCAAdapterType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.ejbbnd.JCAAdapter {
    public JCAAdapterType() {
        this(false);
    }

    public JCAAdapterType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType activation_spec_binding_name;
    com.ibm.ws.javaee.ddmodel.StringType activation_spec_auth_alias;
    com.ibm.ws.javaee.ddmodel.StringType destination_binding_name;

    @Override
    public java.lang.String getActivationSpecBindingName() {
        return activation_spec_binding_name != null ? activation_spec_binding_name.getValue() : null;
    }

    @Override
    public java.lang.String getActivationSpecAuthAlias() {
        return activation_spec_auth_alias != null ? activation_spec_auth_alias.getValue() : null;
    }

    @Override
    public java.lang.String getDestinationBindingName() {
        return destination_binding_name != null ? destination_binding_name.getValue() : null;
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        if (activation_spec_binding_name == null) {
            throw new DDParser.ParseException(parser.requiredAttributeMissing("activation-spec-binding-name"));
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ((xmi ? "activationSpecJndiName" : "activation-spec-binding-name").equals(localName)) {
                this.activation_spec_binding_name = parser.parseStringAttributeValue(index);
                return true;
            }
            if ((xmi ? "activationSpecAuthAlias" : "activation-spec-auth-alias").equals(localName)) {
                this.activation_spec_auth_alias = parser.parseStringAttributeValue(index);
                return true;
            }
            if ((xmi ? "destinationJndiName" : "destination-binding-name").equals(localName)) {
                this.destination_binding_name = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        return false;
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet(xmi ? "activationSpecJndiName" : "activation-spec-binding-name", activation_spec_binding_name);
        diag.describeIfSet(xmi ? "activationSpecAuthAlias" : "activation-spec-auth-alias", activation_spec_auth_alias);
        diag.describeIfSet(xmi ? "destinationJndiName" : "destination-binding-name", destination_binding_name);
    }
}
