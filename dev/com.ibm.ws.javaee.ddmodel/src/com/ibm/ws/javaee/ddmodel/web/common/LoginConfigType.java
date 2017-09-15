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
package com.ibm.ws.javaee.ddmodel.web.common;

import com.ibm.ws.javaee.dd.web.common.FormLoginConfig;
import com.ibm.ws.javaee.dd.web.common.LoginConfig;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="login-configType">
 <xsd:sequence>
 <xsd:element name="auth-method"
 type="javaee:auth-methodType"
 minOccurs="0"/>
 <xsd:element name="realm-name"
 type="javaee:xsdTokenType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="form-login-config"
 type="javaee:form-login-configType"
 minOccurs="0"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class LoginConfigType extends DDParser.ElementContentParsable implements LoginConfig {

    @Override
    public String getAuthMethod() {
        return auth_method != null ? auth_method.getValue() : null;
    }

    @Override
    public String getRealmName() {
        return realm_name != null ? realm_name.getValue() : null;
    }

    @Override
    public FormLoginConfig getFormLoginConfig() {
        return form_login_config;
    }

    // elements
    XSDTokenType auth_method;
    XSDTokenType realm_name;
    FormLoginConfigType form_login_config;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("auth-method".equals(localName)) {
            XSDTokenType auth_method = new XSDTokenType();
            parser.parse(auth_method);
            this.auth_method = auth_method;
            return true;
        }
        if ("realm-name".equals(localName)) {
            XSDTokenType realm_name = new XSDTokenType();
            parser.parse(realm_name);
            this.realm_name = realm_name;
            return true;
        }
        if ("form-login-config".equals(localName)) {
            FormLoginConfigType form_login_config = new FormLoginConfigType();
            parser.parse(form_login_config);
            this.form_login_config = form_login_config;
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("auth-method", auth_method);
        diag.describeIfSet("realm-name", realm_name);
        diag.describeIfSet("form-login-config", form_login_config);
    }

    /*
     * <xsd:complexType name="form-login-configType">
     * <xsd:sequence>
     * <xsd:element name="form-login-page"
     * type="javaee:war-pathType">
     * </xsd:element>
     * <xsd:element name="form-error-page"
     * type="javaee:war-pathType">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class FormLoginConfigType extends DDParser.ElementContentParsable implements FormLoginConfig {

        @Override
        public String getFormLoginPage() {
            return form_login_page.getValue();
        }

        @Override
        public String getFormErrorPage() {
            return form_error_page.getValue();
        }

        // elements
        XSDTokenType form_login_page = new XSDTokenType();
        XSDTokenType form_error_page = new XSDTokenType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("form-login-page".equals(localName)) {
                parser.parse(form_login_page);
                return true;
            }
            if ("form-error-page".equals(localName)) {
                parser.parse(form_error_page);
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describe("form-login-page", form_login_page);
            diag.describe("form-error-page", form_error_page);
        }
    }
}
