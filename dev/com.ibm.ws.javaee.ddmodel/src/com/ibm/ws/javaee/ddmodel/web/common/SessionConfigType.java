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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.web.common.CookieConfig;
import com.ibm.ws.javaee.dd.web.common.SessionConfig;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableList;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.XSDBooleanType;
import com.ibm.ws.javaee.ddmodel.common.XSDIntegerType;
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
     * <xsd:element name="name"
     * type="javaee:cookie-nameType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="domain"
     * type="javaee:cookie-domainType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="path"
     * type="javaee:cookie-pathType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="comment"
     * type="javaee:cookie-commentType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="http-only"
     * type="javaee:true-falseType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="secure"
     * type="javaee:true-falseType"
     * minOccurs="0">
     * </xsd:element>
     * <xsd:element name="max-age"
     * type="javaee:xsdIntegerType"
     * minOccurs="0">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
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

        // elements
        XSDTokenType name;
        XSDTokenType domain;
        XSDTokenType path;
        XSDTokenType comment;
        XSDBooleanType http_only;
        XSDBooleanType secure;
        XSDIntegerType max_age;

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
            return false;
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
