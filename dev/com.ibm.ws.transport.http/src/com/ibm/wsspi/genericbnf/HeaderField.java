/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.genericbnf;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Class that encapsulates a single header value pair for a BNF message.
 */
public interface HeaderField {

    /**
     * Access the name of this header.
     * 
     * @return String
     */
    String getName();

    /**
     * Access the name of this header as an enumerated object.
     * 
     * @return HeaderKeys
     */
    HeaderKeys getKey();

    /**
     * Access the value of this header as a string. This is regular ASCII encoding
     * applied, if another encoding is wanted access the bytes directly.
     * 
     * @return String
     * @see HeaderField#asBytes()
     */
    String asString();

    /**
     * Access the value of this header as a byte[].
     * 
     * @return byte[]
     */
    byte[] asBytes();

    /**
     * Access the value of this header as a Date object.
     * 
     * @return Date
     * @throws ParseException
     *             - if it wasn't a date
     */
    Date asDate() throws ParseException;

    /**
     * Access the value of this header as an integer object.
     * 
     * @return int
     * @throws NumberFormatException
     *             - if it wasn't an number
     */
    int asInteger() throws NumberFormatException;

    /**
     * Tokenize the value of this header using the input delimiter.
     * 
     * @param delimiter
     * @return List<byte[]>
     */
    List<byte[]> asTokens(byte delimiter);

}
