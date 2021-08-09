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

import com.ibm.ws.javaee.dd.common.Icon;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.TokenType;

/*
 <xsd:complexType name="iconType">
 <xsd:sequence>
 <xsd:element name="small-icon"
 type="javaee:pathType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="large-icon"
 type="javaee:pathType"
 minOccurs="0">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute ref="xml:lang"/>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class IconType extends DDParser.ElementContentParsable implements Icon {

    public static class ListType extends ParsableListImplements<IconType, Icon> {
        @Override
        public IconType newInstance(DDParser parser) {
            return new IconType();
        }
    }

    @Override
    public String getSmallIcon() {
        return small_icon != null ? small_icon.getValue() : null;
    }

    @Override
    public String getLargeIcon() {
        return large_icon != null ? large_icon.getValue() : null;
    }

    @Override
    public String getLang() {
        return xml_lang != null ? xml_lang.getValue() : null;
    }

    // attributes
    TokenType xml_lang;
    // elements
    XSDTokenType small_icon;
    XSDTokenType large_icon;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (XMLConstants.XML_NS_URI.equals(nsURI) && "lang".equals(localName)) {
            xml_lang = parser.parseTokenAttributeValue(index);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("small-icon".equals(localName)) {
            XSDTokenType small_icon = new XSDTokenType();
            parser.parse(small_icon);
            this.small_icon = small_icon;
            return true;
        }
        if ("large-icon".equals(localName)) {
            XSDTokenType large_icon = new XSDTokenType();
            parser.parse(large_icon);
            this.large_icon = large_icon;
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("xml:lang", xml_lang);
        diag.describeIfSet("small-icon", small_icon);
        diag.describeIfSet("large-icon", large_icon);
    }
}
