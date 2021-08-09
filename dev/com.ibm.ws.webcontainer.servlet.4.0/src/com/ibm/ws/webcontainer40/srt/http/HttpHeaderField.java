/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.srt.http;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;

/**
 * Used by HttpPushBuilder to pass headers to the http channel.
 * The only method for getting the value that needs to be supported is asString;
 */
public class HttpHeaderField implements HeaderField {

    private final String _name;
    private final String _value;

    public HttpHeaderField(String name, String value) {
        _name = name;
        _value = value;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return _name;
    }

    /** {@inheritDoc} */
    @Override
    public HeaderKeys getKey() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String asString() {
        return _value;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] asBytes() {
        //  Use asString() to get the value of the header
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Date asDate() throws ParseException {
        //  Use asString() to get the value of the header
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int asInteger() throws NumberFormatException {
        //  Use asString() to get the value of the header
        throw new NumberFormatException();
    }

    /** {@inheritDoc} */
    @Override
    public List<byte[]> asTokens(byte delimiter) {
        //  Use asString() to get the value of the header
        return null;
    }

}
