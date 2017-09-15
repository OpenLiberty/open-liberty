/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel;

import java.math.BigInteger;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

public class IntegerType extends AnySimpleType {

    public BigInteger getIntegerValue() {
        return value;
    }

    public int getIntValue() {
        return value.intValue();
    }

    public long getLongValue() {
        return value.longValue();
    }

    public static IntegerType wrap(DDParser parser, String wrapped) throws ParseException {
        return new IntegerType(parser, wrapped);
    }

    private BigInteger value;

    public IntegerType() {
        super(Whitespace.collapse);
    }

    protected IntegerType(DDParser parser, String lexical) throws ParseException {
        super(Whitespace.collapse, parser, lexical);
    }

    @Override
    @FFDCIgnore(NumberFormatException.class)
    protected void setValueFromLexical(DDParser parser, String lexical) throws ParseException {
        try {
            value = new BigInteger(lexical);
        } catch (NumberFormatException e) {
            throw new ParseException(parser.invalidIntValue(lexical));
        }
    }

    @Override
    public void describe(DDParser.Diagnostics diag) {
        if (value != null) {
            diag.append("\"" + value + "\"");
        } else {
            diag.append("null");
        }
    }
}
