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
package com.ibm.ws.javaee.ddmodel.jsp;

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.jsp.JSPConfig;
import com.ibm.ws.javaee.dd.jsp.JSPPropertyGroup;
import com.ibm.ws.javaee.dd.jsp.Taglib;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="jsp-configType">
 <xsd:sequence>
 <xsd:element name="taglib"
 type="javaee:taglibType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="jsp-property-group"
 type="javaee:jsp-property-groupType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class JSPConfigType extends DDParser.ElementContentParsable implements JSPConfig {

    @Override
    public List<Taglib> getTaglibs() {
        if (taglib != null) {
            return taglib.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<JSPPropertyGroup> getJSPPropertyGroups() {
        if (jsp_property_group != null) {
            return jsp_property_group.getList();
        } else {
            return Collections.emptyList();
        }
    }

    // elements
    TaglibType.ListType taglib;
    JSPPropertyGroupType.ListType jsp_property_group;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    public void parseTaglib(DDParser parser) throws ParseException {
        TaglibType taglib = new TaglibType();
        parser.parse(taglib);
        addTaglib(taglib);
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("taglib".equals(localName)) {
            parseTaglib(parser);
            return true;
        }
        if ("jsp-property-group".equals(localName)) {
            JSPPropertyGroupType jsp_property_group = new JSPPropertyGroupType();
            parser.parse(jsp_property_group);
            addJSPPropertyGroup(jsp_property_group);
            return true;
        }
        return false;
    }

    private void addTaglib(TaglibType taglib) {
        if (this.taglib == null) {
            this.taglib = new TaglibType.ListType();
        }
        this.taglib.add(taglib);
    }

    private void addJSPPropertyGroup(JSPPropertyGroupType jsp_property_group) {
        if (this.jsp_property_group == null) {
            this.jsp_property_group = new JSPPropertyGroupType.ListType();
        }
        this.jsp_property_group.add(jsp_property_group);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("taglib", taglib);
        diag.describeIfSet("jsp-property-group", jsp_property_group);
    }

    /*
     * <xsd:complexType name="taglibType">
     * <xsd:sequence>
     * <xsd:element name="taglib-uri"
     * type="javaee:xsdTokenType">
     * </xsd:element>
     * <xsd:element name="taglib-location"
     * type="javaee:pathType">
     * </xsd:element>
     * </xsd:sequence>
     * <xsd:attribute name="id"
     * type="xsd:ID"/>
     * </xsd:complexType>
     */
    static class TaglibType extends DDParser.ElementContentParsable implements Taglib {

        public static class ListType extends ParsableListImplements<TaglibType, Taglib> {
            @Override
            public TaglibType newInstance(DDParser parser) {
                return new TaglibType();
            }
        }

        @Override
        public String getTaglibURI() {
            return taglib_uri.getValue();
        }

        @Override
        public String getTaglibLocation() {
            return taglib_location.getValue();
        }

        // elements
        XSDTokenType taglib_uri = new XSDTokenType();
        XSDTokenType taglib_location = new XSDTokenType();

        @Override
        public boolean isIdAllowed() {
            return true;
        }

        @Override
        public boolean handleChild(DDParser parser, String localName) throws ParseException {
            if ("taglib-uri".equals(localName)) {
                parser.parse(taglib_uri);
                return true;
            }
            if ("taglib-location".equals(localName)) {
                parser.parse(taglib_location);
                return true;
            }
            return false;
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describe("taglib-uri", taglib_uri);
            diag.describe("taglib-location", taglib_location);
        }
    }
}
