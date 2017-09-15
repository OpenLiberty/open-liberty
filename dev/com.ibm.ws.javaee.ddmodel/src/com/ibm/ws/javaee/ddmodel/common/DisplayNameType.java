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

import javax.xml.XMLConstants;

import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.TokenType;

/*
 <xsd:complexType name="display-nameType">
 <xsd:simpleContent>
 <xsd:extension base="javaee:xsdTokenType">
 <xsd:attribute ref="xml:lang"/>
 </xsd:extension>
 </xsd:simpleContent>
 </xsd:complexType>
 */

public class DisplayNameType extends XSDTokenType implements DisplayName {

    public static class ListType extends ParsableListImplements<DisplayNameType, DisplayName> {
        @Override
        public DisplayNameType newInstance(DDParser parser) {
            return new DisplayNameType();
        }
    }

    @Override
    public String getLang() {
        return xml_lang != null ? xml_lang.getValue() : null;
    }

    // attributes
    TokenType xml_lang;

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (XMLConstants.XML_NS_URI.equals(nsURI) && "lang".equals(localName)) {
            xml_lang = parser.parseTokenAttributeValue(index);
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describeIfSet("xml:lang", xml_lang);
    }
}
