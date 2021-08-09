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

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.javaee.ddmodel.common.XSDTokenType;

/*
 * <xsd:complexType name="addressing-responsesType">
 * <xsd:simpleContent>
 * <xsd:restriction base="javaee:xsdTokenType">
 * <xsd:enumeration value="ANONYMOUS"/>
 * <xsd:enumeration value="NON_ANONYMOUS"/>
 * <xsd:enumeration value="ALL"/>
 * </xsd:restriction>
 * </xsd:simpleContent>
 * </xsd:complexType>
 */
public class AddressingResponsesType extends XSDTokenType {

    static enum AddressingResponsesEnum {
        // lexical value must be (ANONYMOUS|NON_ANONYMOUS|ALL)
        ANONYMOUS,
        NON_ANONYMOUS,
        ALL;
    }

    // content
    AddressingResponsesEnum value;

    @Override
    public void finish(DDParser parser) throws ParseException {
        super.finish(parser);
        if (!isNil()) {
            value = parseEnumValue(parser, AddressingResponsesEnum.class);
        }
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeEnum(value);
    }
}
