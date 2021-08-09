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
package com.ibm.ws.javaee.ddmodel.commonbnd;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class ResourceRefType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable implements com.ibm.ws.javaee.dd.commonbnd.ResourceRef {
    public static class DefaultAuthType extends com.ibm.ws.javaee.ddmodel.DDParser.ElementContentParsable {
        public DefaultAuthType() {
            this(false);
        }

        public DefaultAuthType(boolean xmi) {
            this.xmi = xmi;
        }

        protected final boolean xmi;
        com.ibm.ws.javaee.ddmodel.StringType userid;
        com.ibm.ws.javaee.ddmodel.StringType password;

        @Override
        public boolean isIdAllowed() {
            return xmi;
        }

        @Override
        public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
            if (nsURI == null) {
                if ((xmi ? "userId" : "userid").equals(localName)) {
                    this.userid = parser.parseStringAttributeValue(index);
                    return true;
                }
                // "password" is the same for XML and XMI.
                if ("password".equals(localName)) {
                    this.password = parser.parseStringAttributeValue(index);
                    return true;
                }
            }
            if (xmi && "http://www.omg.org/XMI".equals(nsURI)) {
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
            diag.describeIfSet(xmi ? "userId" : "userid", userid);
            diag.describeIfSet("password", password);
        }
    }

    public ResourceRefType() {
        this(false);
    }

    public ResourceRefType(boolean xmi) {
        this.xmi = xmi;
    }

    protected final boolean xmi;
    com.ibm.ws.javaee.ddmodel.StringType name;
    private com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType bindingResourceRef;
    com.ibm.ws.javaee.ddmodel.StringType binding_name;
    com.ibm.ws.javaee.ddmodel.commonbnd.AuthenticationAliasType authentication_alias;
    com.ibm.ws.javaee.ddmodel.commonbnd.CustomLoginConfigurationType custom_login_configuration;
    DefaultAuthType default_auth;

    @Override
    public java.lang.String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public java.lang.String getBindingName() {
        return binding_name != null ? binding_name.getValue() : null;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonbnd.AuthenticationAlias getAuthenticationAlias() {
        return authentication_alias;
    }

    @Override
    public com.ibm.ws.javaee.dd.commonbnd.CustomLoginConfiguration getCustomLoginConfiguration() {
        return custom_login_configuration;
    }

    @Override
    public java.lang.String getDefaultAuthUserid() {
        return default_auth != null && default_auth.userid != null ? default_auth.userid.getValue() : null;
    }

    @Override
    public java.lang.String getDefaultAuthPassword() {
        return default_auth != null && default_auth.password != null ? default_auth.password.getValue() : null;
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws DDParser.ParseException {
        if (nsURI == null) {
            if (!xmi && "name".equals(localName)) {
                this.name = parser.parseStringAttributeValue(index);
                return true;
            }
            if ((xmi ? "jndiName" : "binding-name").equals(localName)) {
                this.binding_name = parser.parseStringAttributeValue(index);
                return true;
            }
            if (xmi && "loginConfigurationName".equals(localName)) {
                if (this.custom_login_configuration == null) {
                    this.custom_login_configuration = new com.ibm.ws.javaee.ddmodel.commonbnd.CustomLoginConfigurationType(true);
                }
                this.custom_login_configuration.name = parser.parseStringAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws DDParser.ParseException {
        if (xmi && "bindingResourceRef".equals(localName)) {
            this.bindingResourceRef = new com.ibm.ws.javaee.ddmodel.CrossComponentReferenceType("bindingResourceRef", parser.crossComponentDocumentType);
            parser.parse(bindingResourceRef);
            com.ibm.ws.javaee.dd.common.ResourceRef referent = this.bindingResourceRef.resolveReferent(parser, com.ibm.ws.javaee.dd.common.ResourceRef.class);
            if (referent == null) {
                DDParser.unresolvedReference("bindingResourceRef", this.bindingResourceRef.getReferenceString());
            } else {
                this.name = parser.parseString(referent.getName());
            }
            return true;
        }
        if (!xmi && "authentication-alias".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.AuthenticationAliasType authentication_alias = new com.ibm.ws.javaee.ddmodel.commonbnd.AuthenticationAliasType();
            parser.parse(authentication_alias);
            this.authentication_alias = authentication_alias;
            return true;
        }
        if (!xmi && "custom-login-configuration".equals(localName)) {
            com.ibm.ws.javaee.ddmodel.commonbnd.CustomLoginConfigurationType custom_login_configuration = new com.ibm.ws.javaee.ddmodel.commonbnd.CustomLoginConfigurationType();
            parser.parse(custom_login_configuration);
            this.custom_login_configuration = custom_login_configuration;
            return true;
        }
        if (xmi && "properties".equals(localName)) {
            if (this.custom_login_configuration == null) {
                this.custom_login_configuration = new com.ibm.ws.javaee.ddmodel.commonbnd.CustomLoginConfigurationType(true);
            }
            com.ibm.ws.javaee.ddmodel.commonbnd.PropertyType property = new com.ibm.ws.javaee.ddmodel.commonbnd.PropertyType(xmi);
            parser.parse(property);
            this.custom_login_configuration.addProperty(property);
            return true;
        }
        if ((xmi ? "defaultAuth" : "default-auth").equals(localName)) {
            DefaultAuthType default_auth = new DefaultAuthType(xmi);
            parser.parse(default_auth);
            this.default_auth = default_auth;
            return true;
        }
        return false;
    }

    @Override
    public void describe(com.ibm.ws.javaee.ddmodel.DDParser.Diagnostics diag) {
        if (xmi) {
            diag.describeIfSet("bindingResourceRef", bindingResourceRef);
        } else {
            diag.describeIfSet("name", name);
        }
        diag.describeIfSet(xmi ? "jndiName" : "binding-name", binding_name);
        diag.describeIfSet("authentication-alias", authentication_alias);
        if (xmi) {
            if (custom_login_configuration != null) {
                custom_login_configuration.describe(diag);
            }
        } else {
            diag.describeIfSet("custom-login-configuration", custom_login_configuration);
        }
        diag.describeIfSet(xmi ? "defaultAuth" : "default-auth", default_auth);
    }
}
