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

import com.ibm.ws.javaee.dd.web.common.MimeMapping;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="mime-mappingType">
 <xsd:sequence>
 <xsd:element name="extension"
 type="javaee:xsdTokenType"/>
 <xsd:element name="mime-type"
 type="javaee:mime-typeType"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

/*
 <xsd:complexType name="mime-typeType">
 <xsd:simpleContent>
 <xsd:restriction base="javaee:xsdTokenType">
 <xsd:pattern value="[^\p{Cc}^\s]+/[^\p{Cc}^\s]+"/>
 </xsd:restriction>
 </xsd:simpleContent>
 </xsd:complexType>
 */

public class MimeMappingType extends DDParser.ElementContentParsable implements MimeMapping {

    public static class ListType extends ParsableListImplements<MimeMappingType, MimeMapping> {
        @Override
        public MimeMappingType newInstance(DDParser parser) {
            return new MimeMappingType();
        }
    }

    @Override
    public String getExtension() {
        return extension.getValue();
    }

    @Override
    public String getMimeType() {
        return mime_type.getValue();
    }

    // elements
    XSDTokenType extension = new XSDTokenType();
    XSDTokenType mime_type = new XSDTokenType();

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("extension".equals(localName)) {
            parser.parse(extension);
            return true;
        }
        if ("mime-type".equals(localName)) {
            parser.parse(mime_type);
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("extension", extension);
        diag.describe("mime-type", mime_type);
    }
}
