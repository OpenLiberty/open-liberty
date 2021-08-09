/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.json.java.internal;

/**
 * Class representing a JSON token.
 */
public class Token
{

    static final public Token TokenEOF    = new Token();
    static final public Token TokenBraceL = new Token();
    static final public Token TokenBraceR = new Token();
    static final public Token TokenBrackL = new Token();
    static final public Token TokenBrackR = new Token();
    static final public Token TokenColon  = new Token();
    static final public Token TokenComma  = new Token();
    static final public Token TokenTrue   = new Token();
    static final public Token TokenFalse  = new Token();
    static final public Token TokenNull   = new Token();

    private String  valueString;
    private Number  valueNumber;


    /**
     * Constructor
     */
    public Token()
    {
        super();
    }

    /**
     * Constructor
     * @param value The value of the token as a string
     */
    public Token(String value)
    {
        super();

        valueString = value;
    }

    /**
     * Constructor
     * @param value The value of the token as a number
     */
    public Token(Number value)
    {
        super();

        valueNumber = value;
    }

    /**
     * Method to obtain the string value of this token
     */
    public String getString()
    {
        return valueString;
    }

    /**
     * Method to obtain the number value of this token
     */
    public Number getNumber()
    {
        return valueNumber;
    }

    /**
     * Method to indicate if this token is string based or not.
     */
    public boolean isString()
    {
        return null != valueString;
    }

    /**
     * Method to indicate if this token is number based or not.
     */
    public boolean isNumber()
    {
        return null != valueNumber;
    }

    /**
     * Method to convert the token to a string representation.
     */
    public String toString()
    {
        if (this == TokenEOF)    return "Token: EOF";
        if (this == TokenBraceL) return "Token: {";
        if (this == TokenBraceR) return "Token: }";
        if (this == TokenBrackL) return "Token: [";
        if (this == TokenBrackR) return "Token: ]";
        if (this == TokenColon)  return "Token: :";
        if (this == TokenComma)  return "Token: ,";
        if (this == TokenTrue)   return "Token: true";
        if (this == TokenFalse)  return "Token: false";
        if (this == TokenNull)   return "Token: null";

        if (this.isNumber()) return "Token: Number - " + getNumber();
        if (this.isString()) return "Token: String - '" + getString() + "'";

        return "Token: unknown.";
    }
}
