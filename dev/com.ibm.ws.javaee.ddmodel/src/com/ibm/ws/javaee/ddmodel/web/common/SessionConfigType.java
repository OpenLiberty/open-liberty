/*******************************************************************************
 * Copyright (c) 2011,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.common.AttributeValue;
import com.ibm.ws.javaee.dd.web.common.CookieConfig;
import com.ibm.ws.javaee.dd.web.common.SessionConfig;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableList;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.DescribableType;
import com.ibm.ws.javaee.ddmodel.common.XSDBooleanType;
import com.ibm.ws.javaee.ddmodel.common.XSDIntegerType;
import com.ibm.ws.javaee.ddmodel.common.XSDStringType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="session-configType">
 <xsd:sequence>
 <xsd:element name="session-timeout"
 type="javaee:xsdIntegerType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="cookie-config"
 type="javaee:cookie-configType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="tracking-mode"
 type="javaee:tracking-modeType"
 minOccurs="0"
 maxOccurs="3">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class SessionConfigType extends DDParser.ElementContentParsable implements SessionConfig {

    @Override
    public boolean isSetSessionTimeout() {
        return AnySimpleType.isSet(session_timeout);
    }

    @Override
    public int getSessionTimeout() {
        return session_timeout.getIntValue();
    }

    @Override
    public CookieConfig getCookieConfig() {
        return cookie_config;
    }

    @Override
    public List<SessionConfig.TrackingModeEnum> getTrackingModeValues() {
        if (tracking_mode != null) {
            return tracking_mode.getList();
        } else {
            return Collections.emptyList();
        }
    }

    // elements
    XSDIntegerType session_timeout;
    CookieConfigType cookie_config;
    TrackingModeType.ListType tracking_mode; // min="0" max="3"

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("session-timeout".equals(localName)) {
            XSDIntegerType session_timeout = new XSDIntegerType();
            parser.parse(session_timeout);
            this.session_timeout = session_timeout;
            return true;
        }
        if ("cookie-config".equals(localName)) {
            CookieConfigType cookie_config = new CookieConfigType();
            parser.parse(cookie_config);
            this.cookie_config = cookie_config;
            return true;
        }
        if ("tracking-mode".equals(localName)) {
            TrackingModeType tracking_mode = new TrackingModeType();
            parser.parse(tracking_mode);
            addTrackingMode(tracking_mode);
            return true;
        }
        return false;
    }

    private void addTrackingMode(TrackingModeType tracking_mode) {
        if (this.tracking_mode == null) {
            this.tracking_mode = new TrackingModeType.ListType();
        }
        this.tracking_mode.add(tracking_mode);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("session-timeout", session_timeout);
        diag.describeIfSet("cookie-config", cookie_config);
        diag.describeIfSet("tracking-mode", tracking_mode);
    }

    /*
     * <xsd:complexType name="cookie-configType">
     * <xsd:sequence>
     * <xsd:element name="name" type="javaee:cookie-nameType" minOccurs="0"/>
     * <xsd:element name="domain" type="javaee:cookie-domainType" minOccurs="0"/>
     * <xsd:element name="path" type="javaee:cookie-pathType" minOccurs="0"/>
     * <xsd:element name="comment" type="javaee:cookie-commentType" minOccurs="0"/>
     * <xsd:element name="http-only" type="javaee:true-falseType" minOccurs="0"/>
     * <xsd:element name="secure" type="javaee:true-falseType" minOccurs="0"/>
     * <xsd:element name="max-age" type="javaee:xsdIntegerType" minOccurs="0"/>
     * <xsd:element name="attribute" type="jakartaee:attribute-valueType" minOccurs="0" maxOccurs="unbounded"/>
     * </xsd:sequence>
     * <xsd:attribute name="id" type="xsd:ID"/>
     * </xsd:complexType>
     *
     * 'attribute' was added by Jakarta EE 10 / Servlet 6.0
     */
    static class CookieConfigType extends DDParser.ElementContentParsable implements CookieConfig {

        @Override
        public String getName() {
            return name != null ? name.getValue() : null;
        }

        @Override
        public String getDomain() {
            return domain != null ? domain.getValue() : null;
        }

        @Override
        public String getPath() {
            return path != null ? path.getValue() : null;
        }

        @Override
        public String getComment() {
            return comment != null ? comment.getValue() : null;
        }

        @Override
        public boolean isSetHTTPOnly() {
            return http_only != null;
        }

        @Override
        public boolean isHTTPOnly() {
            return http_only != null && http_only.getBooleanValue();
        }

        @Override
        public boolean isSetSecure() {
            return AnySimpleType.isSet(secure);
        }

        @Override
        public boolean isSecure() {
            return secure != null && secure.getBooleanValue();
        }

        @Override
        public boolean isSetMaxAge() {
            return AnySimpleType.isSet(max_age);
        }

        @Override
        public int getMaxAge() {
            return max_age != null ? max_age.getIntValue() : 0;
        }

        @Override
        public List<AttributeValue> getAttributes() {
            return attribute != null ? attribute.getList() : Collections.emptyList();
        }

        // elements
        XSDTokenType name;
        XSDTokenType domain;
        XSDTokenType path;
        XSDTokenType comment;
        XSDBooleanType http_only;
        XSDBooleanType secure;
        XSDIntegerType max_age;
        AttributeValueType.ListType attribute;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("name".equals(localName)) {
                XSDTokenType name = new XSDTokenType();
                parser.parse(name);
                this.name = name;
                return true;
            }
            if ("domain".equals(localName)) {
                XSDTokenType domain = new XSDTokenType();
                parser.parse(domain);
                this.domain = domain;
                return true;
            }
            if ("path".equals(localName)) {
                XSDTokenType path = new XSDTokenType();
                parser.parse(path);
                this.path = path;
                return true;
            }
            if ("comment".equals(localName)) {
                XSDTokenType comment = new XSDTokenType();
                parser.parse(comment);
                this.comment = comment;
                return true;
            }
            if ("http-only".equals(localName)) {
                XSDBooleanType http_only = new XSDBooleanType();
                parser.parse(http_only);
                this.http_only = http_only;
                return true;
            }
            if ("secure".equals(localName)) {
                XSDBooleanType secure = new XSDBooleanType();
                parser.parse(secure);
                this.secure = secure;
                return true;
            }
            if ("max-age".equals(localName)) {
                XSDIntegerType max_age = new XSDIntegerType();
                parser.parse(max_age);
                this.max_age = max_age;
                return true;
            }
            if ((parser.version >= WebApp.VERSION_6_0) && "attribute".equals(localName)) {
                AttributeValueType attribute_value = new AttributeValueType();
                parser.parse(attribute_value);
                addAttribute(parser, attribute_value);
                return true;
            }
            return false;
        }

        private void warn(DDParser parser, String msgId, Object... parms) {
            parser.warning(parser.formatMessage(msgId, parms));
        }

        private void addAttribute(DDParser parser, AttributeValueType attribute) {
            String attributeName = attribute.getAttributeName();

            // Per request from the web container, do not allow attributes which
            // either have a null name or which have duplicate names.
            //
            // An attribute with a null name cannot be used, and is not allowed
            // by the schema (although, we will parse it).
            //
            // Attributes with the same names are allowed by the schema, but
            // RFC 6265, HTTP State Management Mechanism, dated April 2011,
            // section 4.1.1, Syntax, states:
            // "To maximize compatibility with user agents, servers SHOULD NOT produce
            // two attributes with the same name in the same set-cookie-string."

            if (attributeName == null) {
                warn(parser,
                     "missing.session.config.attribute.name",
                     parser.describeEntry(), parser.getLineNumber(),
                     attribute.getAttributeValue());
                return;
            }

            if (this.attribute == null) {
                this.attribute = new AttributeValueType.ListType();
            } else {
                // Attributes with null names are never added, per the attribute name
                // test, above.
                AttributeValueType priorAttribute = this.attribute.lookup(attributeName);
                if (priorAttribute != null) {
                    warn(parser,
                         "duplicate.session.config.attribute.name",
                         parser.describeEntry(), parser.getLineNumber(),
                         attributeName, attribute.getAttributeValue(),
                         priorAttribute.getAttributeValue());

                    // 'add' will handle both adding the new attribute to
                    // the list, and updating the internal table of attributes.
                    // However, the prior attribute must still be removed
                    // from the list.
                    this.attribute.remove(priorAttribute);
                }
            }
            this.attribute.add(attribute);
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("name", name);
            diag.describeIfSet("domain", domain);
            diag.describeIfSet("path", path);
            diag.describeIfSet("comment", comment);
            diag.describeIfSet("http-only", http_only);
            diag.describeIfSet("secure", secure);
            diag.describeIfSet("max-age", max_age);
            diag.describeIfSet("attribute", attribute);
        }
    }

/*
 * <xsd:complexType name="attribute-valueType">
 * <xsd:sequence>
 * <xsd:element name="description" type="jakartaee:descriptionType" minOccurs="0" maxOccurs="unbounded"/>
 * <xsd:element name="attribute-name" type="jakartaee:string"/>
 * <xsd:element name="attribute-value" type="jakartaee:xsdStringType"/>
 * </xsd:sequence>
 * <xsd:attribute name="id" type="xsd:ID"/>
 * </xsd:complexType name="attribute-valueType">
 */

    static class AttributeValueType extends DescribableType implements AttributeValue {
        public static class ListType extends ParsableListImplements<AttributeValueType, AttributeValue> {
            @Override
            public AttributeValueType newInstance(DDParser parser) {
                return new AttributeValueType();
            }

            // Hard to tell if this is worth having.
            // I've added it in case the attribute lists are large.
            private Map<String, AttributeValueType> attributes;

            @Override
            public void add(AttributeValueType attributeValue) {
                super.add(attributeValue);

                if (attributes == null) {
                    attributes = new HashMap<String, AttributeValueType>();
                }
                attributes.put(attributeValue.getAttributeName(), attributeValue);
            }

            protected AttributeValueType lookup(String attributeName) {
                return ((attributes == null) ? null : attributes.get(attributeName));
            }

            protected void remove(AttributeValueType attributeValue) {
                list.remove(attributeValue);
            }
        }

        // elements
        XSDStringType attribute_name;
        XSDStringType attribute_value;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public String getAttributeName() {
            return attribute_name != null ? attribute_name.getValue() : null;
        }

        @Override
        public String getAttributeValue() {
            return attribute_value != null ? attribute_value.getValue() : null;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if (super.handleChild(parser, localName)) {
                return true;
            }
            if ("attribute-name".equals(localName)) {
                XSDStringType attribute_name = new XSDStringType(true);
                parser.parse(attribute_name);
                this.attribute_name = attribute_name;
                return true;
            } else if ("attribute-value".equals(localName)) {
                XSDStringType attribute_value = new XSDStringType(true);
                parser.parse(attribute_value);
                this.attribute_value = attribute_value;
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            super.describe(diag);
            diag.describeIfSet("attribute-name", attribute_name);
            diag.describeIfSet("attribute-value", attribute_value);
        }
    }

    /*
     * <xsd:complexType name="cookie-nameType">
     * <xsd:simpleContent>
     * <xsd:extension base="javaee:nonEmptyStringType"/>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */

    /*
     * <xsd:complexType name="cookie-domainType">
     * <xsd:simpleContent>
     * <xsd:extension base="javaee:nonEmptyStringType"/>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */

    /*
     * <xsd:complexType name="cookie-pathType">
     * <xsd:simpleContent>
     * <xsd:extension base="javaee:nonEmptyStringType"/>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */

    /*
     * <xsd:complexType name="cookie-commentType">
     * <xsd:simpleContent>
     * <xsd:extension base="javaee:nonEmptyStringType"/>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */

    /*
     * <xsd:complexType name="tracking-modeType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:xsdTokenType">
     * <xsd:enumeration value="COOKIE"/>
     * <xsd:enumeration value="URL"/>
     * <xsd:enumeration value="SSL"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class TrackingModeType extends XSDTokenType {

        static class ListType extends ParsableList<TrackingModeType> {
            @Override
            public TrackingModeType newInstance(DDParser parser) {
                return new TrackingModeType();
            }

            public List<SessionConfig.TrackingModeEnum> getList() {
                List<SessionConfig.TrackingModeEnum> values = new ArrayList<SessionConfig.TrackingModeEnum>();
                for (TrackingModeType type : list) {
                    values.add(type.value);
                }
                return values;
            }
        }

        // content
        SessionConfig.TrackingModeEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, SessionConfig.TrackingModeEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }
}
