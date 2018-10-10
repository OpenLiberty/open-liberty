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

import com.ibm.ws.javaee.dd.web.common.ServletMapping;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.StringType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="servlet-mappingType">
 <xsd:sequence>
 <xsd:element name="servlet-name"
 type="javaee:servlet-nameType"/>
 <xsd:element name="url-pattern"
 type="javaee:url-patternType"
 minOccurs="1"
 maxOccurs="unbounded"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

/*
 <xsd:complexType name="servlet-nameType">
 <xsd:simpleContent>
 <xsd:extension base="javaee:nonEmptyStringType"/>
 </xsd:simpleContent>
 </xsd:complexType>
 */

public class ServletMappingType extends DDParser.ElementContentParsable implements ServletMapping {

    public static class ListType extends ParsableListImplements<ServletMappingType, ServletMapping> {
        @Override
        public ServletMappingType newInstance(DDParser parser) {
            return new ServletMappingType();
        }
    }

    @Override
    public String getServletName() {
        return servlet_name.getValue();
    }

    @Override
    public List<String> getURLPatterns() {
        return url_pattern.getList();
    }

    // elements
    XSDTokenType servlet_name = new XSDTokenType();
    StringType.ListType url_pattern = new StringType.ListType();

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("servlet-name".equals(localName)) {
            parser.parse(servlet_name);
            return true;
        }
        if ("url-pattern".equals(localName)) {
            StringType url_pattern = new StringType();
            parser.parse(url_pattern);
            this.url_pattern.add(url_pattern);
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("servlet-name", servlet_name);
        diag.describe("url-pattern", url_pattern);
    }
}
