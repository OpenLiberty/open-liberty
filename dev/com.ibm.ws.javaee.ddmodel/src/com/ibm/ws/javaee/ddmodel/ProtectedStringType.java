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
package com.ibm.ws.javaee.ddmodel;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;

/**
 * Provides protection against tracing sensitive data.
 */
public class ProtectedStringType extends StringType {

    private ProtectedString value;

    /**
     * Wrap a string value as a protected value.
     * 
     * @param lexical The raw text of the string.
     *
     * @throws ParseException Thrown in case of a parse exception.  Not thrown
     *     by this implementation.  Declared for possible future use.  
     */
    protected ProtectedStringType(@Sensitive String lexical) throws ParseException {
        super(Whitespace.preserve);
        value = new ProtectedString(lexical.toCharArray());
    }

    @Sensitive
    @Override
    public String getValue() {
        return String.valueOf(value.getChars());
    }

    public static ProtectedStringType wrap(@Sensitive String wrapped) throws ParseException {
        return new ProtectedStringType(wrapped);
    }

    @Override
    protected void setValueFromLexical(DDParser parser, String lexical) {
        String lexicalValue = getLexicalValue();
        value = new ProtectedString(lexicalValue.toCharArray());
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
