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
package com.ibm.ws.javaee.ddmodel.common;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.web.common.WebAppType;

/*
 <xsd:complexType name="resource-refType">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="res-ref-name"
 type="javaee:jndi-nameType">
 </xsd:element>
 <xsd:element name="res-type"
 type="javaee:fully-qualified-classType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="res-auth"
 type="javaee:res-authType"
 minOccurs="0"/>
 <xsd:element name="res-sharing-scope"
 type="javaee:res-sharing-scopeType"
 minOccurs="0"/>
 <xsd:group ref="javaee:resourceGroup"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class ResourceRefType extends ResourceGroup implements ResourceRef {

    public static class ListType extends ParsableListImplements<ResourceRefType, ResourceRef> {
        @Override
        public ResourceRefType newInstance(DDParser parser) {
            return new ResourceRefType();
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
    public String getType() {
        return res_type != null ? res_type.getValue() : null;
    }

    @Override
    public int getAuthValue() {
        if (res_auth != null) {
            switch (res_auth.value) {
                case Application:
                    return AUTH_APPLICATION;
                case Container:
                    return AUTH_CONTAINER;
            }
        }
        return AUTH_UNSPECIFIED;
    }

    @Override
    public int getSharingScopeValue() {
        if (res_sharing_scope != null) {
            switch (res_sharing_scope.value) {
                case Shareable:
                    return SHARING_SCOPE_SHAREABLE;
                case Unshareable:
                    return SHARING_SCOPE_UNSHAREABLE;
            }
        }
        return SHARING_SCOPE_UNSPECIFIED;
    }

    // elements
    DescriptionType.ListType description;
    XSDTokenType res_type;
    ResAuthType res_auth;
    ResSharingScopeType res_sharing_scope;

    // ResourceGroup fields appear here in sequence

    public ResourceRefType() {
        super("res-ref-name");
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
        if ("res-type".equals(localName)) {
            XSDTokenType res_type = new XSDTokenType();
            parser.parse(res_type);
            this.res_type = res_type;
            return true;
        }
        if ("res-auth".equals(localName)) {
            ResAuthType res_auth = new ResAuthType();
            parser.parse(res_auth);
            this.res_auth = res_auth;
            return true;
        }
        if ("res-sharing-scope".equals(localName)) {
            ResSharingScopeType res_sharing_scope = new ResSharingScopeType();
            parser.parse(res_sharing_scope);
            this.res_sharing_scope = res_sharing_scope;
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

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("description", description);
        diag.describeIfSet("res-type", res_type);
        diag.describeIfSet("res-auth", res_auth);
        diag.describeIfSet("res-sharing-scope", res_sharing_scope);
        super.describe(diag);
    }

    /*
     * <xsd:complexType name="res-authType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:xsdTokenType">
     * <xsd:enumeration value="Application"/>
     * <xsd:enumeration value="Container"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static enum ResAuthEnum {
        // lexical value must be (Application|Container)
        Application,
        Container;
    }

    static enum ResAuthEnumWebAppLegacy {
        CONTAINER(ResAuthEnum.Container),
        SERVLET(ResAuthEnum.Application);

        final ResAuthEnum value;

        ResAuthEnumWebAppLegacy(ResAuthEnum value) {
            this.value = value;
        }
    }

    static class ResAuthType extends XSDTokenType {
        // content
        ResAuthEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                if (parser.getRootParsable() instanceof WebAppType && parser.version < 23) {
                    value = parseEnumValue(parser, ResAuthEnumWebAppLegacy.class).value;
                } else {
                    value = parseEnumValue(parser, ResAuthEnum.class);
                }
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }

    /*
     * <xsd:complexType name="res-sharing-scopeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:xsdTokenType">
     * <xsd:enumeration value="Shareable"/>
     * <xsd:enumeration value="Unshareable"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static enum ResSharingScopeEnum {
        // lexical value must be (Shareable|Unshareable)
        Shareable,
        Unshareable;
    }

    static class ResSharingScopeType extends XSDTokenType {
        // content
        ResSharingScopeEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, ResSharingScopeEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }
}
