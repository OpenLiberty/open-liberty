/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.common.wsclient;

import com.ibm.ws.javaee.dd.common.wsclient.Addressing;
import com.ibm.ws.javaee.ddmodel.AnySimpleType;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.XSDBooleanType;

/*
 * <xsd:complexType name="addressingType">
 * <xsd:sequence>
 * <xsd:element name="enabled"
 * type="javaee:true-falseType"
 * minOccurs="0"
 * maxOccurs="1"/>
 * <xsd:element name="required"
 * type="javaee:true-falseType"
 * minOccurs="0"
 * maxOccurs="1"/>
 * <xsd:element name="responses"
 * type="javaee:addressing-responsesType"
 * minOccurs="0"
 * maxOccurs="1"/>
 * </xsd:sequence>
 * </xsd:complexType>
 */
public class AddressingType extends DDParser.ElementContentParsable implements Addressing {

    @Override
    public boolean isSetEnabled() {
        return AnySimpleType.isSet(enabled);
    }

    @Override
    public boolean isEnabled() {
        return enabled != null && enabled.getBooleanValue();
    }

    @Override
    public boolean isSetRequired() {
        return AnySimpleType.isSet(required);
    }

    @Override
    public boolean isRequired() {
        return required != null && required.getBooleanValue();
    }

    @Override
    public int getAddressingResponsesTypeValue() {
        if (responses != null) {
            switch (responses.value) {
                case ANONYMOUS:
                    return ADDRESSING_RESPONSES_ANONYMOUS;
                case NON_ANONYMOUS:
                    return ADDRESSING_RESPONSES_NON_ANONYMOUS;
                case ALL:
                    return ADDRESSING_RESPONSES_ALL;
            }
        }
        return ADDRESSING_RESPONSES_UNSPECIFIED;
    }

    XSDBooleanType enabled;
    XSDBooleanType required;
    AddressingResponsesType responses;

    @Override
    public boolean handleChild(DDParser parser, String localName) throws ParseException {
        if ("enabled".equals(localName)) {
            XSDBooleanType enabled = new XSDBooleanType();
            parser.parse(enabled);
            this.enabled = enabled;
            return true;
        }
        if ("required".equals(localName)) {
            XSDBooleanType required = new XSDBooleanType();
            parser.parse(required);
            this.required = required;
            return true;
        }
        if ("responses".equals(localName)) {
            AddressingResponsesType responses = new AddressingResponsesType();
            parser.parse(responses);
            this.responses = responses;
            return true;
        }
        return false;
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeIfSet("enabled", enabled);
        diag.describeIfSet("required", required);
        diag.describeIfSet("responses", responses);
    }
}
