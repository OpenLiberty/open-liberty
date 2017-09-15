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
package com.ibm.ws.javaee.ddmodel.appbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class ProfileType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.appbnd.Profile {
    com.ibm.ws.javaee.ddmodel.StringType name;
    DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appbnd.ClientProfileType, com.ibm.ws.javaee.dd.appbnd.ClientProfile> client_profile;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.appbnd.ClientProfile> getClientProfiles() {
        if (client_profile != null) {
            return client_profile.getList();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if ("name".equals(localName)) {
                this.name = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if ("client-profile".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.appbnd.ClientProfileType client_profile = new com.ibm.ws.javaee.ddmodel.appbnd.ClientProfileType();
            parser.parse(client_profile);
            this.addClientProfile(client_profile);
            return true;
        }
        return false;
    }

    void addClientProfile(com.ibm.ws.javaee.ddmodel.appbnd.ClientProfileType client_profile) {
        if (this.client_profile == null) {
            this.client_profile = new DDParser.ParsableListImplements<com.ibm.ws.javaee.ddmodel.appbnd.ClientProfileType, com.ibm.ws.javaee.dd.appbnd.ClientProfile>();
        }
        this.client_profile.add(client_profile);
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        diag.describeIfSet("name", name);
        diag.describeIfSet("client-profile", client_profile);
    }
}
