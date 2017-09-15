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

import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="message-destinationType">
 <xsd:sequence>
 <xsd:group ref="javaee:descriptionGroup"/>
 <xsd:element name="message-destination-name"
 type="javaee:xsdTokenType">
 </xsd:element>
 <xsd:element name="mapped-name"
 type="javaee:xsdStringType"
 minOccurs="0">
 </xsd:element>
 <xsd:element name="lookup-name"
 type="javaee:xsdStringType"
 minOccurs="0">
 </xsd:element>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

public class MessageDestinationType extends DescriptionGroup implements MessageDestination {

    public static class ListType extends ParsableListImplements<MessageDestinationType, MessageDestination> {
        @Override
        public MessageDestinationType newInstance(DDParser parser) {
            return new MessageDestinationType();
        }
    }

    @Override
    public String getName() {
        return message_destination_name.getValue();
    }

    @Override
    public String getMappedName() {
        return mapped_name != null ? mapped_name.getValue() : null;
    }

    @Override
    public String getLookupName() {
        return lookup_name != null ? lookup_name.getValue() : null;
    }

    // elements
    // DescriptionGroup fields appear here in sequence
    XSDTokenType message_destination_name = new XSDTokenType();
    XSDStringType mapped_name;
    XSDStringType lookup_name;

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("message-destination-name".equals(localName)) {
            parser.parse(message_destination_name);
            return true;
        }
        if ("mapped-name".equals(localName)) {
            XSDStringType mapped_name = new XSDStringType();
            parser.parse(mapped_name);
            this.mapped_name = mapped_name;
            return true;
        }
        if ("lookup-name".equals(localName)) {
            XSDStringType lookup_name = new XSDStringType();
            parser.parse(lookup_name);
            this.lookup_name = lookup_name;
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        super.describe(diag);
        diag.describe("message-destination-name", message_destination_name);
        diag.describeIfSet("mapped-name", mapped_name);
        diag.describeIfSet("lookup-name", lookup_name);
    }
}
