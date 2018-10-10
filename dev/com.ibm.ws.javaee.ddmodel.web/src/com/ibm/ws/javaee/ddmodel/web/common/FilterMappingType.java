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

import com.ibm.ws.javaee.dd.web.common.FilterMapping;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableList;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="filter-mappingType">
 <xsd:sequence>
 <xsd:element name="filter-name"
 type="javaee:filter-nameType"/>
 <xsd:choice minOccurs="1"
 maxOccurs="unbounded">
 <xsd:element name="url-pattern"
 type="javaee:url-patternType"/>
 <xsd:element name="servlet-name"
 type="javaee:servlet-nameType"/>
 </xsd:choice>
 <xsd:element name="dispatcher"
 type="javaee:dispatcherType"
 minOccurs="0"
 maxOccurs="5"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class FilterMappingType extends DDParser.ElementContentParsable {

    public static class ListType extends ParsableList<FilterMappingType> {
        @Override
        public FilterMappingType newInstance(DDParser parser) {
            return new FilterMappingType();
        }

        public List<FilterMapping> getList() {
            List<FilterMapping> mappingList = new ArrayList<FilterMapping>();
            for (FilterMappingType mapping : list) {
                mappingList.addAll(mapping.expandMappings());
            }
            return mappingList;
        }
    }

    // elements
    XSDTokenType filter_name = new XSDTokenType();
    List<FilterMapping> mappings;
    DispatcherType.ListType dispatcher; // min=0 max=5

    class SingleFilterMapping implements FilterMapping {
        StringType url_pattern;
        XSDTokenType servlet_name;

        @Override
        public String getFilterName() {
            return filter_name.getValue();
        }

        @Override
        public String getURLPattern() {
            return url_pattern != null ? url_pattern.getValue() : null;
        }

        @Override
        public String getServletName() {
            return servlet_name != null ? servlet_name.getValue() : null;
        }

        @Override
        public List<FilterMapping.DispatcherEnum> getDispatcherValues() {
            if (dispatcher != null) {
                return dispatcher.getList();
            } else {
                return Collections.emptyList();
            }
        }

        public void describe(DDParser.Diagnostics diag) {
            diag.describeIfSet("url-pattern", url_pattern);
            diag.describeIfSet("servlet-name", servlet_name);
        }
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("filter-name".equals(localName)) {
            parser.parse(filter_name);
            return true;
        }
        if ("url-pattern".equals(localName)) {
            SingleFilterMapping single = new SingleFilterMapping();
            StringType url_pattern = new StringType();
            parser.parse(url_pattern);
            single.url_pattern = url_pattern;
            addSingleMapping(single);
            return true;
        }
        if ("servlet-name".equals(localName)) {
            SingleFilterMapping single = new SingleFilterMapping();
            XSDTokenType servlet_name = new XSDTokenType();
            parser.parse(servlet_name);
            single.servlet_name = servlet_name;
            addSingleMapping(single);
            return true;
        }
        if ("dispatcher".equals(localName)) {
            DispatcherType dispatcher = new DispatcherType();
            parser.parse(dispatcher);
            addDispatcher(dispatcher);
            return true;
        }
        return false;
    }

    private void addSingleMapping(SingleFilterMapping single) {
        if (this.mappings == null) {
            this.mappings = new ArrayList<FilterMapping>();
        }
        this.mappings.add(single);
    }

    private void addDispatcher(DispatcherType dispatcher) {
        if (this.dispatcher == null) {
            this.dispatcher = new DispatcherType.ListType();
        }
        this.dispatcher.add(dispatcher);
    }

    public List<FilterMapping> expandMappings() {
        return mappings;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("filter-name", filter_name);
        if (mappings != null) {
            for (FilterMapping filter_mapping : mappings) {
                ((SingleFilterMapping) filter_mapping).describe(diag);
            }
        }
        diag.describeIfSet("dispatcher", dispatcher);
    }

    /*
     * <xsd:complexType name="dispatcherType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:xsdTokenType">
     * <xsd:enumeration value="FORWARD"/>
     * <xsd:enumeration value="INCLUDE"/>
     * <xsd:enumeration value="REQUEST"/>
     * <xsd:enumeration value="ASYNC"/>
     * <xsd:enumeration value="ERROR"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */
    static class DispatcherType extends XSDTokenType {

        public static class ListType extends ParsableList<DispatcherType> {
            @Override
            public DispatcherType newInstance(DDParser parser) {
                return new DispatcherType();
            }

            public java.util.List<FilterMapping.DispatcherEnum> getList() {
                List<FilterMapping.DispatcherEnum> values = new ArrayList<FilterMapping.DispatcherEnum>();
                for (DispatcherType type : list) {
                    values.add(type.value);
                }
                return values;
            }
        }

        // content
        FilterMapping.DispatcherEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, FilterMapping.DispatcherEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }
}
