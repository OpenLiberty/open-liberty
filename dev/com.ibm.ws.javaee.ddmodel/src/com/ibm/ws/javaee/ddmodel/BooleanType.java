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
package com.ibm.ws.javaee.ddmodel;

import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

public class BooleanType extends AnySimpleType {

    public boolean getBooleanValue() {
        return value.booleanValue();
    }

    public static BooleanType wrap(DDParser parser, String wrapped) throws ParseException {
        return new BooleanType(parser, wrapped);
    }

    protected Boolean value;

    public BooleanType() {
        super(Whitespace.collapse);
    }

    protected BooleanType(DDParser parser, String lexical) throws ParseException {
        super(Whitespace.collapse, parser, lexical);
    }

    @Override
    protected void setValueFromLexical(DDParser parser, String lexical) throws ParseException {
        if ("true".equals(lexical) || "1".equals(lexical)) {
            value = Boolean.TRUE;
        } else if ("false".equals(lexical) || "0".equals(lexical)) {
            value = Boolean.FALSE;
        } else {
            throw new ParseException(parser.invalidEnumValue(lexical, "true", "1", "false", "0"));
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
