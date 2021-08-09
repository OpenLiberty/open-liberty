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

import java.util.List;

import com.ibm.ws.javaee.dd.web.common.LocaleEncodingMapping;
import com.ibm.ws.javaee.dd.web.common.LocaleEncodingMappingList;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;

/*
 <xsd:simpleType name="localeType">
 <xsd:restriction base="xsd:string">
 <xsd:pattern value="[a-z]{2}(_|-)?([\p{L}\-\p{Nd}]{2})?"/>
 </xsd:restriction>
 </xsd:simpleType>
 */

/*
 <xsd:simpleType name="encodingType">
 <xsd:restriction base="xsd:string">
 <xsd:pattern value="[^\s]+"/>
 </xsd:restriction>
 </xsd:simpleType>
 */

/*
 <xsd:complexType name="locale-encoding-mapping-listType">
 <xsd:sequence>
 <xsd:element name="locale-encoding-mapping"
 type="javaee:locale-encoding-mappingType"
 maxOccurs="unbounded"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class LocaleEncodingMappingListType extends DDParser.ElementContentParsable implements LocaleEncodingMappingList {

    @Override
    public List<LocaleEncodingMapping> getLocaleEncodingMappings() {
        return locale_encoding_mapping.getList();
    }

    // elements
    LocaleEncodingMappingType.ListType locale_encoding_mapping = new LocaleEncodingMappingType.ListType();

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("locale-encoding-mapping".equals(localName)) {
            LocaleEncodingMappingType locale_encoding_mapping = new LocaleEncodingMappingType();
            parser.parse(locale_encoding_mapping);
            addLocaleEncodingMapping(locale_encoding_mapping);
            return true;
        }
        return false;
    }

    private void addLocaleEncodingMapping(LocaleEncodingMappingType locale_encoding_mapping) {
        this.locale_encoding_mapping.add(locale_encoding_mapping);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("locale-encoding-mapping", locale_encoding_mapping);
    }

    /*
     * <xsd:complexType name="locale-encoding-mappingType">
     * <xsd:sequence>
     * <xsd:element name="locale"
     * type="javaee:localeType"/>
     * <xsd:element name="encoding"
     * type="javaee:encodingType"/>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class LocaleEncodingMappingType extends DDParser.ElementContentParsable implements LocaleEncodingMapping {

        public static class ListType extends ParsableListImplements<LocaleEncodingMappingType, LocaleEncodingMapping> {
            @Override
            public LocaleEncodingMappingType newInstance(DDParser parser) {
                return new LocaleEncodingMappingType();
            }
        }

        @Override
        public String getLocale() {
            return locale.getValue();
        }

        @Override
        public String getEncoding() {
            return encoding.getValue();
        }

        // elements
        StringType locale;
        StringType encoding;

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("locale".equals(localName)) {
                StringType locale = new StringType();
                parser.parse(locale);
                this.locale = locale;
                return true;
            }
            if ("encoding".equals(localName)) {
                StringType encoding = new StringType();
                parser.parse(encoding);
                this.encoding = encoding;
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describe("locale", locale);
            diag.describe("encoding", encoding);
        }
    }
}
