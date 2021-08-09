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

import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="propertyType">
 <xsd:sequence>
 <xsd:element name="name"
 type="javaee:xsdStringType">
 </xsd:element>
 <xsd:element name="value"
 type="javaee:xsdStringType">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class PropertyType extends DDParser.ElementContentParsable implements Property {

    public static class ListType extends ParsableListImplements<PropertyType, Property> {
        @Override
        public PropertyType newInstance(DDParser parser) {
            return new PropertyType();
        }
    }

    @Override
    public String getName() {
        return name.getValue();
    }

    @Override
    public String getValue() {
        return value.getValue();
    }

    // elements
    XSDStringType name = new XSDStringType();
    XSDStringType value = new XSDStringType();

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("name".equals(localName)) {
            parser.parse(name);
            return true;
        }
        if ("value".equals(localName)) {
            parser.parse(value);
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("name", name);
        diag.describe("value", value);
    }
}
