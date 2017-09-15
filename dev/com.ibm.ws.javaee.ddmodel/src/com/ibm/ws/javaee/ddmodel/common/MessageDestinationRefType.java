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

import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParsableListImplements;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="message-destination-refType">
 <xsd:sequence>
 <xsd:element name="description"
 type="javaee:descriptionType"
 minOccurs="0"
 maxOccurs="unbounded"/>
 <xsd:element name="message-destination-ref-name"
 type="javaee:jndi-nameType">
 </xsd:element>
 <xsd:element name="message-destination-type"
 type="javaee:message-destination-typeType"
 minOccurs="0"/>
 <xsd:element name="message-destination-usage"
 type="javaee:message-destination-usageType"
 minOccurs="0"/>
 <xsd:element name="message-destination-link"
 type="javaee:message-destination-linkType"
 minOccurs="0"/>
 <xsd:group ref="javaee:resourceGroup"/>
 </xsd:sequence>
 <xsd:attribute name="id"
 type="xsd:ID"/>
 </xsd:complexType>
 */

/*
 <xsd:complexType name="message-destination-typeType">
 <xsd:simpleContent>
 <xsd:restriction base="javaee:fully-qualified-classType"/>
 </xsd:simpleContent>
 </xsd:complexType>
 */

/*
 <xsd:complexType name="message-destination-linkType">
 <xsd:simpleContent>
 <xsd:restriction base="javaee:xsdTokenType"/>
 </xsd:simpleContent>
 </xsd:complexType>
 */

public class MessageDestinationRefType extends ResourceGroup implements MessageDestinationRef {

    public static class ListType extends ParsableListImplements<MessageDestinationRefType, MessageDestinationRef> {
        @Override
        public MessageDestinationRefType newInstance(DDParser parser) {
            return new MessageDestinationRefType();
        }
    }

    @Override
    public List<Description> getDescriptions() {
        if (description != null) {
            return description.getList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getType() {
        return message_destination_type != null ? message_destination_type.getValue() : null;
    }

    @Override
    public int getUsageValue() {
        if (message_destination_usage != null) {
            switch (message_destination_usage.value) {
                case Consumes:
                    return USAGE_CONSUMES;
                case Produces:
                    return USAGE_PRODUCES;
                case ConsumesProduces:
                    return USAGE_CONSUMES_PRODUCES;
            }
        }
        return USAGE_UNSPECIFIED;
    }

    @Override
    public String getLink() {
        return message_destination_link != null ? message_destination_link.getValue() : null;
    }

    // elements
    DescriptionType.ListType description;
    XSDTokenType message_destination_type;
    MessageDestinationUsageType message_destination_usage;
    XSDTokenType message_destination_link;

    // ResourceGroup fields appear here in sequence

    public MessageDestinationRefType() {
        super("message-destination-ref-name");
    }

    @Override
    public boolean isIdAllowed() {
        return true;
    }

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if (super.handleChild(parser, localName)) {
            return true;
        }
        if ("description".equals(localName)) {
            DescriptionType description = new DescriptionType();
            parser.parse(description);
            addDescription(description);
            return true;
        }
        if ("message-destination-type".equals(localName)) {
            XSDTokenType message_destination_type = new XSDTokenType();
            parser.parse(message_destination_type);
            this.message_destination_type = message_destination_type;
            return true;
        }
        if ("message-destination-usage".equals(localName)) {
            MessageDestinationUsageType message_destination_usage = new MessageDestinationUsageType();
            parser.parse(message_destination_usage);
            this.message_destination_usage = message_destination_usage;
            return true;
        }
        if ("message-destination-link".equals(localName)) {
            XSDTokenType message_destination_link = new XSDTokenType();
            parser.parse(message_destination_link);
            this.message_destination_link = message_destination_link;
            return true;
        }
        return false;
    }

    private void addDescription(DescriptionType description) {
        if (this.description == null) {
            this.description = new DescriptionType.ListType();
        }
        this.description.add(description);
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("description", description);
        diag.describeIfSet("message-destination-type", message_destination_type);
        diag.describeIfSet("message-destination-usage", message_destination_usage);
        diag.describeIfSet("message-destination-link", message_destination_link);
        super.describe(diag);
    }

    /*
     * <xsd:complexType name="message-destination-usageType">
     * <xsd:simpleContent>
     * <xsd:restriction base="javaee:xsdTokenType">
     * <xsd:enumeration value="Consumes"/>
     * <xsd:enumeration value="Produces"/>
     * <xsd:enumeration value="ConsumesProduces"/>
     * </xsd:restriction>
     * </xsd:simpleContent>
     * </xsd:complexType>
     */

    static enum MessageDestinationUsageEnum {
        // lexical value must be (Consumes|Produces|ConsumesProduces)
        Consumes,
        Produces,
        ConsumesProduces;
    }

    static class MessageDestinationUsageType extends XSDTokenType {
        // content
        MessageDestinationUsageEnum value;

        @Override
        public void finish(DDParser parser) throws ParseException {
            super.finish(parser);
            if (!isNil()) {
                value = parseEnumValue(parser, MessageDestinationUsageEnum.class);
            }
        }

        @Override
        public void describe(DDParser.Diagnostics diag) {
            diag.describeEnum(value);
        }
    }
}
