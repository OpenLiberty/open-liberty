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

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.genericbnf.exception.MalformedMessageException;

/**
 * This interface adds the functionality of parsing and marshalling of the
 * headers in storage.
 * 
 * @ibm-private-in-use
 */
public interface BNFHeaders extends HeaderStorage {

    /** Static representation of a carriage return character */
    byte CR = '\r';
    /** Static representation of a linefeed character */
    byte LF = '\n';
    /** Static representation of a horizontal tab character */
    byte TAB = '\t';
    /** Static representation of a horizontal space character */
    byte SPACE = ' ';
    /** Static representation of a semicolon character */
    byte SEMICOLON = ';';
    /** Representation of a colon character */
    byte COLON = ':';
    /** EOL (CRLF) byte array for fast copying into ByteBuffers. */
    byte[] EOL = { CR, LF };
    /** 2 EOLs (CRLFCRLF) byte array for fast copying into ByteBuffers. */
    byte[] DUAL_EOL = { CR, LF, CR, LF };
    /** Key/Value separator ": " for fast copying into ByteBuffers. */
    byte[] KEY_VALUE_SEPARATOR = { COLON, SPACE };

    // ***********************************************************
    // Methods for marshalling and parsing headers
    // ***********************************************************

    /**
     * Marshall the headers that are in storage
     * 
     * @param src
     *            - existing buffers to start adding the headers into
     * @return WsByteBuffer[] of headers ready to be written.
     * @throws Exception
     */
    WsByteBuffer[] marshallHeaders(WsByteBuffer[] src);

    /**
     * Method to marshall of the headers in storage into the correct
     * [header: value] matches, and place them in the input buffers
     * in binary format.
     * 
     * @param src
     * @return WsByteBuffer[]
     */
    WsByteBuffer[] marshallBinaryHeaders(WsByteBuffer[] src);

    /**
     * Perform any work that must be executed immediately before
     * calling marshallHeaders(), such as flushing caches.
     */
    void preMarshallHeaders();

    /**
     * Perform any work that must be executed immediately after
     * calling marshallHeaders(), such as destroying unusable objects.
     */
    void postMarshallHeaders();

    /**
     * Begin parsing headers out from a given buffer. Returns boolean
     * as to whether it has found the end of the headers or not (double
     * CRLF). The input flag is whether or not to immediately parse the
     * header value into a byte[] or to delay the extraction from
     * the input buffer.
     * 
     * @param buff
     * @param bExtractValue
     * @return boolean
     * @throws MalformedMessageException
     *             if the headers are incorrectly formatted.
     */
    boolean parseHeaders(WsByteBuffer buff, boolean bExtractValue) throws MalformedMessageException;

    /**
     * This parses the binary headers and returns the boolean whether this was
     * successfully completed (true) or not (false). The headers are extracted
     * immediately i.e.(no delayed extraction of headers).
     * 
     * @param buff
     * @param keys
     * @return boolean
     * @throws MalformedMessageException
     */
    boolean parseBinaryHeaders(WsByteBuffer buff, HeaderKeys keys) throws MalformedMessageException;

    // *****************************************************************
    // Methods acting directly on the header storage object
    // *****************************************************************

    /**
     * Duplicate the existing headers into the input object. Null input
     * will trigger a NullPointerException
     * 
     * @param msg
     */
    void duplicate(BNFHeaders msg);

}
