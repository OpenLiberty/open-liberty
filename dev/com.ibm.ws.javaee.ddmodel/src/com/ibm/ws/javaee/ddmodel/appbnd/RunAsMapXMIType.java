/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.appbnd;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;

/**
 * Manual implementation of the runAsMap XMI element. The contents of
 * this object and {@link AuthorizationTableXMIType} are merged by
 * the {@link ApplicationBndType#finish} method to form the result of
 * the {@link com.ibm.ws.javaee.dd.appbnd.ApplicationBnd#getSecurityRoles} method.
 */
public class RunAsMapXMIType extends DDParser.ElementContentParsable {
    static class RunAsBindingXMIType extends DDParser.ElementContentParsable {
        RunAsXMIType authData;
        StringType securityRoleName;
        CrossComponentReferenceType securityRole;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("authData".equals(localName)) {
                authData = new RunAsXMIType();
                parser.parse(authData);
                return true;
            }
            if ("securityRole".equals(localName)) {
                securityRole = new CrossComponentReferenceType("securityRole", Application.class);
                parser.parse(securityRole);
                SecurityRole referent = this.securityRole.resolveReferent(parser, SecurityRole.class);
                if (referent == null) {
                    DDParser.unresolvedReference("role", securityRole.getReferenceString());
                } else {
                    securityRoleName = parser.parseString(referent.getRoleName());
                }
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("authData", authData);
            diag.describeIfSet("securityRole", securityRole);
        }
    }

    static class RunAsXMIType extends RunAsType {
        // TODO: These duplicate fields of 'RunAsType'.  More significantly,
        //       'password' changes from 'ProtectedStringType' to 'StringType'.
        StringType userid;
        StringType password;

        @Override
        public String getUserid() {
            return userid == null ? null : userid.getValue();
        }

        @Override
        public String getPassword() {
            return password == null ? null : password.getValue();
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
            if (nsURI == null) {
                if ("userId".equals(localName)) {
                    this.userid = parser.parseStringAttributeValue(index);
                    return true;
                }
                if ("password".equals(localName)) {
                    this.password = parser.parseProtectedStringAttributeValue(index);
                    return true;
                }
            }
            if ("http://www.omg.org/XMI".equals(nsURI)) {
                if ("type".equals(localName)) {
                    String type = parser.getAttributeValue(index);
                    if (type.endsWith(":BasicAuthData") && "commonbnd.xmi".equals(parser.getNamespaceURI(type.substring(0, type.length() - ":BasicAuthData".length())))) {
                        // Allowed but ignored.
                        return true;
                    }
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
            diag.describeIfSet("userId", userid);
            diag.describeIfSet("password", password);
        }
    }

    DDParser.ParsableList<RunAsBindingXMIType> runAsBindings;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("runAsBindings".equals(localName)) {
            RunAsBindingXMIType runAsBinding = new RunAsBindingXMIType();
            parser.parse(runAsBinding);
            addRunAsBinding(runAsBinding);
            return true;
        }
        return false;
    }

    private void addRunAsBinding(RunAsBindingXMIType runAsBinding) {
        if (this.runAsBindings == null) {
            this.runAsBindings = new DDParser.ParsableList<RunAsBindingXMIType>();
        }
        this.runAsBindings.add(runAsBinding);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("runAsBindings", runAsBindings);
    }
}
