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
import com.ibm.ws.javaee.ddmodel.StringType;

/*
 <xsd:simpleType name="isolation-levelType">
 <xsd:restriction base="xsd:string">
 <xsd:enumeration value="TRANSACTION_READ_UNCOMMITTED"/>
 <xsd:enumeration value="TRANSACTION_READ_COMMITTED"/>
 <xsd:enumeration value="TRANSACTION_REPEATABLE_READ"/>
 <xsd:enumeration value="TRANSACTION_SERIALIZABLE"/>
 </xsd:restriction>
 </xsd:simpleType>
 */

public class IsolationLevelType extends StringType {
    static enum IsolationLevelEnum {
        // lexical value must be (TRANSACTION_READ_UNCOMMITTED|TRANSACTION_READ_COMMITTED|TRANSACTION_REPEATABLE_READ|TRANSACTION_SERIALIZABLE)
        TRANSACTION_READ_UNCOMMITTED,
        TRANSACTION_READ_COMMITTED,
        TRANSACTION_REPEATABLE_READ,
        TRANSACTION_SERIALIZABLE;
    }

    IsolationLevelEnum value;

    @Override
    public void finish(DDParser parser) throws ParseException {
        super.finish(parser);
        if (!isNil()) {
            value = parseEnumValue(parser, IsolationLevelEnum.class);
        }
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        diag.describeEnum(value);
    }
}
