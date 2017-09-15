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

import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/*
 <xsd:complexType name="ejb-ref-typeType">
 <xsd:simpleContent>
 <xsd:restriction base="javaee:xsdTokenType">
 <xsd:enumeration value="Entity"/>
 <xsd:enumeration value="Session"/>
 </xsd:restriction>
 </xsd:simpleContent>
 </xsd:complexType>
 */

public class EJBRefTypeType extends XSDTokenType {

    static enum EJBRefTypeEnum {
        // lexical value must be (Entity|Session)
        Entity,
        Session;
    }

    // content
    EJBRefTypeEnum value;

    @Override
    public void finish(DDParser parser) throws ParseException {
        super.finish(parser);
        if (!isNil()) {
            value = parseEnumValue(parser, EJBRefTypeEnum.class);
        }
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeEnum(value);
    }
}
