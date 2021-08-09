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

import com.ibm.ws.javaee.dd.web.common.ErrorPage;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.XSDIntegerType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="error-codeType">
 <xsd:simpleContent>
 <xsd:restriction base="javaee:xsdPositiveIntegerType">
 <xsd:pattern value="\d{3}"/>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:restriction>
 </xsd:simpleContent>
 </xsd:complexType>
 */

/*
 <xsd:complexType name="error-pageType">
 <xsd:sequence>
 <xsd:choice minOccurs="0"
 maxOccurs="1">
 <xsd:element name="error-code"
 type="javaee:error-codeType"/>
 <xsd:element name="exception-type"
 type="javaee:fully-qualified-classType">
 </xsd:element>
 </xsd:choice>
 <xsd:element name="location"
 type="javaee:war-pathType">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class ErrorPageType extends DDParser.ElementContentParsable implements ErrorPage {

    public static class ListType extends ParsableListImplements<ErrorPageType, ErrorPage> {
        @Override
        public ErrorPageType newInstance(DDParser parser) {
            return new ErrorPageType();
        }
    }

    // elements
    // choice [0,1] {
    XSDIntegerType error_code;
    XSDTokenType exception_type;
    // } choice
    XSDTokenType location = new XSDTokenType();

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("error-code".equals(localName)) {
            XSDIntegerType error_code = new XSDIntegerType();
            parser.parse(error_code);
            this.error_code = error_code;
            return true;
        }
        if ("exception-type".equals(localName)) {
            XSDTokenType exception_type = new XSDTokenType();
            parser.parse(exception_type);
            this.exception_type = exception_type;
            return true;
        }
        if ("location".equals(localName)) {
            parser.parse(location);
            return true;
        }
        return false;
    }

    @Override
    public boolean isSetErrorCode() {
        return AnySimpleType.isSet(error_code);
    }

    @Override
    public int getErrorCode() {
        return error_code != null ? error_code.getIntValue() : 0;
    }

    @Override
    public String getExceptionType() {
        return exception_type != null ? exception_type.getValue() : null;
    }

    @Override
    public String getLocation() {
        return location.getValue();
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("error-code", error_code);
        diag.describeIfSet("exception-type", exception_type);
        diag.describe("location", location);
    }
}
