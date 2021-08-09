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

import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.web.common.Filter;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.DescriptionGroup;
import com.ibm.ws.javaee.ddmodel.common.ParamValueType;
import com.ibm.ws.javaee.ddmodel.common.XSDBooleanType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="filterType">
 <xsd:sequence>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="filter-name"
 type="javaee:filter-nameType"/>
 <xsd:element name="filter-class"
 type="javaee:fully-qualified-classType"
 minOccurs="0"
 maxOccurs="1">
 </xsd:element>
 <xsd:element name="async-supported"
 type="javaee:true-falseType"
 minOccurs="0"/>
 <xsd:element name="init-param"
 type="javaee:param-valueType"
 minOccurs="0"
 maxOccurs="unbounded">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class FilterType extends DescriptionGroup implements Filter {

    public static class ListType extends ParsableListImplements<FilterType, Filter> {
        @Override
        public FilterType newInstance(DDParser parser) {
            return new FilterType();
        }
    }

    @Override
    public String getFilterName() {
        return filter_name.getValue();
    }

    @Override
    public String getFilterClass() {
        return filter_class != null ? filter_class.getValue() : null;
    }

    @Override
    public boolean isSetAsyncSupported() {
        return AnySimpleType.isSet(async_supported);
    }

    @Override
    public boolean isAsyncSupported() {
        return async_supported != null && async_supported.getBooleanValue();
    }

    @Override
    public List<ParamValue> getInitParams() {
        if (init_param != null) {
            return init_param.getList();
        } else {
            return Collections.emptyList();
        }
    }

    // elements
    // DescriptionGroup fields appear here in sequence
    XSDTokenType filter_name = new XSDTokenType();
    XSDTokenType filter_class;
    XSDBooleanType async_supported;
    ParamValueType.ListType init_param;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("filter-name".equals(localName)) {
            parser.parse(filter_name);
            return true;
        }
        if ("filter-class".equals(localName)) {
            XSDTokenType filter_class = new XSDTokenType();
            parser.parse(filter_class);
            this.filter_class = filter_class;
            return true;
        }
        if ("async-supported".equals(localName)) {
            XSDBooleanType async_supported = new XSDBooleanType();
            parser.parse(async_supported);
            this.async_supported = async_supported;
            return true;
        }
        if ("init-param".equals(localName)) {
            ParamValueType init_param = new ParamValueType();
            parser.parse(init_param);
            addInitParam(init_param);
            return true;
        }
        return false;
    }

    private void addInitParam(ParamValueType init_param) {
        if (this.init_param == null) {
            this.init_param = new ParamValueType.ListType();
        }
        this.init_param.add(init_param);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describe("filter-name", filter_name);
        diag.describeIfSet("filter-class", filter_class);
        diag.describeIfSet("async-supported", async_supported);
        diag.describeIfSet("init-param", init_param);
    }
}
