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
package com.ibm.ws.javaee.ddmodel.ejbext;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class SpecifiedIdentityType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.ejbext.SpecifiedIdentity {
    public SpecifiedIdentityType() {
        this(false);
    }

    public SpecifiedIdentityType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType role;
    com.ibm.ws.javaee.ddmodel.StringType description;

    @Override
    public java.lang.String getRole() {
        return role != null ? role.getValue() : null;
    }

    @Override
    public java.lang.String getDescription() {
        return description != null ? description.getValue() : null;
    }

    @Override
    public void finish(DDParser parser) throws DDParser.ParseException {
        if (role == null) {
            throw new DDParser.ParseException(parser.requiredAttributeMissing("role"));
        }
    }

    @Override
    public boolean isIdAllowed() {
        return xmi;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ((xmi ? "roleName" : "role").equals(localName)) {
                this.role = parser.parseStringAttributeValue(index);
                return true;
            }
            // "description" is the same for XML and XMI.
            if ("description".equals(localName)) {
                this.description = parser.parseStringAttributeValue(index);
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
        diag.describeIfSet(xmi ? "roleName" : "role", role);
        diag.describeIfSet("description", description);
    }
}
