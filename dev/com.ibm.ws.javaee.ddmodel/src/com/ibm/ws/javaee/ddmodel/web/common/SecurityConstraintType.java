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

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.web.common.AuthConstraint;
import com.ibm.ws.javaee.dd.web.common.SecurityConstraint;
import com.ibm.ws.javaee.dd.web.common.UserDataConstraint;
import com.ibm.ws.javaee.dd.web.common.WebResourceCollection;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.common.DescribableType;
import com.ibm.ws.javaee.ddmodel.common.DisplayNameType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="security-constraintType">
 <xsd:sequence>
 <xsd:element name="display-name"
 type="javaee:display-nameType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="web-resource-collection"
 type="javaee:web-resource-collectionType"
 maxOccurs="unbounded"/>
 <xsd:element name="auth-constraint"
 type="javaee:auth-constraintType"
 minOccurs="0"/>
 <xsd:element name="user-data-constraint"
 type="javaee:user-data-constraintType"
 minOccurs="0"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class SecurityConstraintType extends DDParser.ElementContentParsable implements SecurityConstraint {

    public static class ListType extends ParsableListImplements<SecurityConstraintType, SecurityConstraint> {
        @Override
        public SecurityConstraintType newInstance(DDParser parser) {
            return new SecurityConstraintType();
        }
    }

    @Override
    public List<DisplayName> getDisplayNames() {
        if (display_name != null) {
            return display_name.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<WebResourceCollection> getWebResourceCollections() {
        return web_resource_collection.getList();
    }

    @Override
    public AuthConstraint getAuthConstraint() {
        return auth_constraint;
    }

    @Override
    public UserDataConstraint getUserDataConstraint() {
        return user_data_constraint;
    }

    // elements
    DisplayNameType.ListType display_name;
    WebResourceCollectionType.ListType web_resource_collection = new WebResourceCollectionType.ListType();
    AuthConstraintType auth_constraint;
    UserDataConstraintType user_data_constraint;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("display-name".equals(localName)) {
            DisplayNameType display_name = new DisplayNameType();
            parser.parse(display_name);
            addDisplayName(display_name);
            return true;
        }
        if ("web-resource-collection".equals(localName)) {
            WebResourceCollectionType web_resource_collection = new WebResourceCollectionType();
            parser.parse(web_resource_collection);
            this.web_resource_collection.add(web_resource_collection);
            return true;
        }
        if ("auth-constraint".equals(localName)) {
            AuthConstraintType auth_constraint = new AuthConstraintType();
            parser.parse(auth_constraint);
            this.auth_constraint = auth_constraint;
            return true;
        }
        if ("user-data-constraint".equals(localName)) {
            UserDataConstraintType user_data_constraint = new UserDataConstraintType();
            parser.parse(user_data_constraint);
            this.user_data_constraint = user_data_constraint;
            return true;
        }
        return false;
    }

    private void addDisplayName(DisplayNameType display_name) {
        if (this.display_name == null) {
            this.display_name = new DisplayNameType.ListType();
        }
        this.display_name.add(display_name);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("display-name", display_name);
        diag.describe("web-resource-collection", web_resource_collection);
        diag.describeIfSet("auth-constraint", auth_constraint);
        diag.describeIfSet("user-data-constraint", user_data_constraint);
    }

    /*
     * <xsd:complexType name="auth-constraintType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="role-name"
     * type="javaee:role-nameType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class AuthConstraintType extends DescribableType implements AuthConstraint {

        // elements
        XSDTokenType.ListType role_name;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("role-name".equals(localName)) {
                XSDTokenType role_name = new XSDTokenType();
                parser.parse(role_name);
                addRoleName(role_name);
                return true;
            }
            return false;
        }

        private void addRoleName(XSDTokenType role_name) {
            if (this.role_name == null) {
                this.role_name = new XSDTokenType.ListType();
            }
            this.role_name.add(role_name);
        }

        @Override
        public List<String> getRoleNames() {
            if (role_name != null) {
                return role_name.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("role-name", role_name);
        }
    }

    /*
     * <xsd:complexType name="transport-guaranteeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:xsdTokenType">
     * <xsd:enumeration value="NONE"/>
     * <xsd:enumeration value="INTEGRAL"/>
     * <xsd:enumeration value="CONFIDENTIAL"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static enum TransportGuaranteeEnum {
        // lexical value must be (NONE|INTEGRAL|CONFIDENTIAL)
        NONE,
        INTEGRAL,
        CONFIDENTIAL;
    }

    static class TransportGuaranteeType extends XSDTokenType {
        // content
        TransportGuaranteeEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, TransportGuaranteeEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    /*
     * <xsd:complexType name="user-data-constraintType">
     * <xsd:sequence>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="transport-guarantee"
     * type="javaee:transport-guaranteeType"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class UserDataConstraintType extends DescribableType implements UserDataConstraint {

        @Override
        public int getTransportGuarantee() {
            switch (transport_guarantee.value) {
                default: // case NONE:
                    return TRANSPORT_GUARANTEE_NONE;
                case INTEGRAL:
                    return TRANSPORT_GUARANTEE_INTEGRAL;
                case CONFIDENTIAL:
                    return TRANSPORT_GUARANTEE_CONFIDENTIAL;
            }
        }

        // elements
        TransportGuaranteeType transport_guarantee;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("transport-guarantee".equals(localName)) {
                TransportGuaranteeType transport_guarantee = new TransportGuaranteeType();
                parser.parse(transport_guarantee);
                this.transport_guarantee = transport_guarantee;
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            super.describe(diag);
            diag.describe("transport-guarantee", transport_guarantee);
        }
    }

    /*
     * <xsd:complexType name="web-resource-collectionType">
     * <xsd:sequence>
     * <xsd:element name="web-resource-name"
     * type="javaee:xsdTokenType">
     * </xsd:element>
     * <xsd:element name="description"
     * type="javaee:descriptionType"
     * minOccurs="0"
     * maxOccurs="unbounded"/>
     * <xsd:element name="url-pattern"
     * type="javaee:url-patternType"
     * maxOccurs="unbounded"/>
     * <xsd:choice minOccurs="0"
     * maxOccurs="1">
     * <xsd:element name="http-method"
     * type="javaee:http-methodType"
     * minOccurs="1"
     * maxOccurs="unbounded">
     * </xsd:element>
     * <xsd:element name="http-method-omission"
     * type="javaee:http-methodType"
     * minOccurs="1"
     * maxOccurs="unbounded">
     * </xsd:element>
     * </xsd:choice>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class WebResourceCollectionType extends DescribableType implements WebResourceCollection {

        public static class ListType extends ParsableListImplements<WebResourceCollectionType, WebResourceCollection> {
            @Override
            public WebResourceCollectionType newInstance(DDParser parser) {
                return new WebResourceCollectionType();
            }
        }

        @Override
        public String getWebResourceName() {
            return web_resource_name.getValue();
        }

        @Override
        public List<String> getURLPatterns() {
            if (url_pattern != null) {
                return url_pattern.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<String> getHTTPMethods() {
            if (http_method != null) {
                return http_method.getList();
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<String> getHTTPMethodOmissions() {
            if (http_method_omission != null) {
                return http_method_omission.getList();
            } else {
                return Collections.emptyList();
            }
        }

        // elements
        XSDTokenType web_resource_name = new XSDTokenType();
        StringType.ListType url_pattern = new StringType.ListType();
        // choice [0,1] {
        StringType.ListType http_method;
        StringType.ListType http_method_omission;

        // } choice

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("web-resource-name".equals(localName)) {
                parser.parse(web_resource_name);
                return true;
            }
            if ("url-pattern".equals(localName)) {
                StringType url_pattern = new StringType();
                parser.parse(url_pattern);
                this.url_pattern.add(url_pattern);
                return true;
            }
            if ("http-method".equals(localName)) {
                StringType http_method = new StringType();
                parser.parse(http_method);
                addHTTPMethod(http_method);
                return true;
            }
            if ("http-method-omission".equals(localName)) {
                StringType http_method_omission = new StringType();
                parser.parse(http_method_omission);
                addHTTPMethodOmission(http_method_omission);
                return true;
            }
            return false;
        }

        private void addHTTPMethod(StringType http_method) {
            if (this.http_method == null) {
                this.http_method = new StringType.ListType();
            }
            this.http_method.add(http_method);
        }

        private void addHTTPMethodOmission(StringType http_method_omission) {
            if (this.http_method_omission == null) {
                this.http_method_omission = new StringType.ListType();
            }
            this.http_method_omission.add(http_method_omission);
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describe("web-resource-name", web_resource_name);
            diag.describe("url-pattern", url_pattern);
            diag.describeIfSet("http-method", http_method);
            diag.describeIfSet("http-method-omission", http_method_omission);
        }
    }
}
