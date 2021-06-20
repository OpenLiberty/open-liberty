/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web.common;

import javax.xml.XMLConstants;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.common.AbsoluteOrdering;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.BooleanType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.TokenType;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 <xsd:complexType name="web-appType">
 <xsd:choice minOccurs="0"
 maxOccurs="unbounded">
 <xsd:element name="module-name"
 type="javaee:xsdTokenType"
 minOccurs="0"/>
 <xsd:group ref="javaee:web-commonType"/>
 <xsd:element name="absolute-ordering"
 type="javaee:absoluteOrderingType"/>
 </xsd:choice>
 <xsd:attributeGroup ref="javaee:web-common-attributes"/>
 </xsd:complexType>
 */

public class WebAppType extends WebCommonType implements WebApp, DDParser.RootParsable {
    public WebAppType(String path) {
        super(path);
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }
    
    // attributes
    TokenType version;
    BooleanType metadata_complete;

    // elements
    XSDTokenType module_name;

    // WebCommonType fields appear here in sequence
    AbsoluteOrderingType absolute_ordering;

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
    public String getModuleName() {
        return module_name != null ? module_name.getValue() : null;
    }

    @Override
    public AbsoluteOrdering getAbsoluteOrdering() {
        return absolute_ordering;
    }

    //

    /**
     * Override: Ensure that the version is assigned.
     * 
     * Use the version computed by the parser to ensure
     * the local version variable is assigned. 
     */
    @Override
    public void finish(DDParser parser) throws ParseException {
        if ( version == null ) {
            // In all cases, not just for 2.2 and 2.3, 
            // ensure that the local version variable is
            // assigned.
            //
            // Previously, only the two DTD based formats
            // might be missing a version attribute.
            // Changes to enable more descriptor deviations
            // mean that other cases might also be missing
            // a version attribute.
            version = parser.parseToken( parser.getDottedVersionText() );            
        }
        
        super.finish(parser);
    }

    @Override
    public boolean handleAttribute(DDParser parser, String nsURI, String localName, int index) throws ParseException {
        if (nsURI == null || XMLConstants.NULL_NS_URI.equals(nsURI)) {
            if (parser.version >= WebApp.VERSION_2_4 && "version".equals(localName)) {
                version = parser.parseTokenAttributeValue(index);
                return true;
            }
            if (parser.version >= WebApp.VERSION_2_4 && "metadata-complete".equals(localName)) {
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

        if ("module-name".equals(localName)) {
            XSDTokenType module_name = new XSDTokenType();
            parser.parse(module_name);
            this.module_name = module_name;
            return true;
        }

        if ("absolute-ordering".equals(localName)) {
            AbsoluteOrderingType absolute_ordering = new AbsoluteOrderingType();
            parser.parse(absolute_ordering);
            if (this.absolute_ordering == null) {
                this.absolute_ordering = absolute_ordering;
            } else if (parser.maxVersion >= WebApp.VERSION_3_1) {
                // WebApp 3.1, which corresponds to JavaEE7, clarified the
                // parsing of absolute ordering: At most one absolute ordering
                // is allowed.
                throw new ParseException(parser.tooManyElements("absolute-ordering"));
            }
            return true;
        }

        return false;
    }

    //

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describe("version", version);
        diag.describeIfSet("metadata-complete", metadata_complete);
        diag.describeIfSet("module-name", module_name);
        super.describe(diag);
        diag.describeIfSet("absolute-ordering", absolute_ordering);
    }

    @Override
    public void describe(StringBuilder sb) {
        DDParser.Diagnostics diag = new DDParser.Diagnostics(idMap, sb);
        diag.describe(toTracingSafeString(), this);
    }
    
    @Override
    protected String toTracingSafeString() {
        return "web-app";
    }
}
