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

import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.dd.web.common.Ordering;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.BooleanType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.TokenType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="web-fragmentType">
 <xsd:choice minOccurs="0"
 maxOccurs="unbounded">
 <xsd:element name="name"
 type="javaee:java-identifierType"/>
 <xsd:group ref="javaee:web-commonType"/>
 <xsd:element name="ordering"
 type="javaee:orderingType"/>
 </xsd:choice>
 <xsd:attributeGroup ref="javaee:web-common-attributes"/>
 </xsd:complexType>
 */

public class WebFragmentType extends WebCommonType implements WebFragment, DDParser.RootParsable {
    public WebFragmentType(String path) {
        super(path);
    }

    @Override
    public String getVersion() {
        return version.getValue();
    }

    @Override
    public boolean isSetMetadataComplete() {
        return AnySimpleType.isSet(metadata_complete);
    }

    @Override
    public boolean isMetadataComplete() {
        return metadata_complete != null && metadata_complete.getBooleanValue();
    }

    @Override
    public String getName() {
        return name != null ? name.getValue() : null;
    }

    @Override
    public Ordering getOrdering() {
        return ordering;
    }

    // attributes
    TokenType version;
    BooleanType metadata_complete;
    // elements
    XSDTokenType name;
    // WebCommonType fields appear here in sequence
    OrderingType ordering;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (nsURI == null) {
            if ("version".equals(localName)) {
                version = parser.parseTokenAttributeValue(index);
                return true;
            }
            if ("metadata-complete".equals(localName)) {
                metadata_complete = parser.parseBooleanAttributeValue(index);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("name".equals(localName)) {
            XSDTokenType name = new XSDTokenType();
            parser.parse(name);
            this.name = name;
            return true;
        }
        if ("ordering".equals(localName)) {
            OrderingType ordering = new OrderingType();
            parser.parse(ordering);
            if (this.ordering == null) {
                this.ordering = ordering;
            } else if (parser.runtimeVersion >= 70) { //EE7 clarification, can only have one ordering element.
                throw new ParseException(parser.tooManyElements("ordering"));
            }
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("version", version);
        diag.describeIfSet("metadata-complete", metadata_complete);
        diag.describeIfSet("name", name);
        super.describe(diag);
        diag.describeIfSet("ordering", ordering);
    }

    @Override
    protected String toTracingSafeString() {
        return "web-fragment";
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
}
