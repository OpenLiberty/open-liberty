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
package com.ibm.ws.genericbnf.internal;

import java.nio.BufferUnderflowException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.genericbnf.BNFHeaders;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;

/**
 * Object to store information on a single header "name: value" instance.
 * 
 */
public final class HeaderElement implements HeaderField, Comparable<HeaderElement> {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HeaderElement.class, GenericConstants.GENERIC_TRACE_NAME, null);

    /** Initial hash value */
    private static final int initHash = 17 * 37;

    // state information for the header instance
    /** Element has been initialized */
    private static final int ELEM_INIT = 0;
    /** Element has been changed */
    private static final int ELEM_CHANGED = 1;
    /** Element has been removed */
    private static final int ELEM_REMOVED = 2;
    /** Element has been newly added */
    private static final int ELEM_ADDED = 3;

    // next/prev pointers accessed directly by BNFHeadersImpl

    /** link to next header in header instance list */
    protected HeaderElement nextInstance = null;
    /** link to next header in header sequence list */
    protected HeaderElement nextSequence = null;
    /** link to previous header in header sequence list */
    protected HeaderElement prevSequence = null;

    /** Header identifier possible as a key */
    private HeaderKeys key = null;
    /** Index counter into the BNFHeaders list of buffers */
    private int buffIndex = -1;
    /** Position in the buffer or larger byte[] that's the start of the value */
    private int offset = 0;
    /** Length of the value information stored in the buffers */
    private int valueLength = 0;
    /** value possible as a byte[] */
    private byte[] bValue = null;
    /** value possible as a String */
    private String sValue = null;
    /** BNFHeadersImpl that owns this object */
    private BNFHeadersImpl myOwner;
    /** Stored hash code since it is based on unchanging data */
    private int myHashCode = -1;
    /** Status on whether this element instance has changed, been removed, etc */
    private int status = ELEM_ADDED;
    /** Index into parse buffer list of last CRLF prior to this header */
    private int lastCRLFBufferIndex = -1;
    /** Position inside that buffer of the last CRLF */
    private int lastCRLFPosition = -1;
    /** Flag on whether that position points to a CR or LF */
    private boolean lastCRLFisCR = false;

    /**
     * Constructor for storing a header.
     * 
     * @param header
     * @param owner
     */
    HeaderElement(HeaderKeys header, BNFHeadersImpl owner) {
        this.key = header;
        this.myOwner = owner;
    }

    /**
     * @see HeaderField#getKey()
     */
    public HeaderKeys getKey() {
        return this.key;
    }

    /**
     * Set the information about the WsByteBuffer parsing. This is
     * intended to be used during the parsing of the wsbb but prior
     * to actually pulling the value out into the byte[] or String
     * storage.
     * 
     * @param index
     * @param start
     */
    protected void setParseInformation(int index, int start) {
        this.buffIndex = index;
        this.offset = start;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Set parse information " + this.buffIndex + " " + this.offset);
        }
    }

    /**
     * Set the value length after finding it during the parsing of headers
     * stage.
     * 
     * @param len
     */
    protected void setValueLength(int len) {
        this.valueLength = len;
        // Note: this is only called during the parse stage so don't set the
        // changed flag here
    }

    /**
     * Query the length of the value in storage.
     * 
     * @return int
     */
    protected int getValueLength() {
        return this.valueLength;
    }

    /**
     * Query the offset for this header value. Depending on context, this is
     * either the offset into a larger byte[] or the offset into the parse
     * buffers.
     * 
     * @return int
     */
    protected int getOffset() {
        return this.offset;
    }

    /**
     * Using the initial parse information, pull the byte[] value out of
     * the WsbyteBuffers.
     * 
     * @return boolean (true means byte[] value extracted, false if failure)
     */
    private boolean extractInitialValue() {
        if (-1 == this.buffIndex) {
            // parsing information not set yet, this should not happen
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Extract: -1 index");
            }
            return false;
        }

        this.bValue = new byte[this.valueLength];
        WsByteBuffer buff = this.myOwner.getParseBuffer(this.buffIndex);
        int savePosition = buff.position();
        // assume all of the data is in the first buffer, and only do the more
        // extensive logic if we need to
        try {
            buff.position(this.offset);
            buff.get(this.bValue, 0, this.valueLength);
            buff.position(savePosition);

        } catch (BufferUnderflowException bue) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Header value straddled buffers");
            }
            int lengthNeeded = this.valueLength;
            int arrayOffset = 0;
            // reset the first buffer for the read
            buff.position(this.offset);
            int spaceLeftInBuffer = buff.remaining();
            int index = this.buffIndex + 1;

            // loop until we break out below
            while (true) {
                // reset the position in the buffer for the get() call below
                if (lengthNeeded <= spaceLeftInBuffer) {
                    // get the rest of the data
                    buff.get(this.bValue, arrayOffset, lengthNeeded);
                    buff.position(savePosition);
                    // we're done
                    break;
                }
                // pull out the information from this particular buffer and
                // set it up for querying the next buffer
                buff.get(this.bValue, arrayOffset, spaceLeftInBuffer);
                buff.position(savePosition);
                lengthNeeded -= spaceLeftInBuffer;
                arrayOffset += spaceLeftInBuffer;
                buff = this.myOwner.getParseBuffer(index);
                index++;

                // secondary buffers... save the current position and reset to
                // zero for the next read
                savePosition = buff.position();
                buff.position(0);
                spaceLeftInBuffer = buff.limit();
            } // end of while
        } // end of catch block

        // once we've extracted, fix the length/offset stuff
        this.offset = 0;
        this.buffIndex = -1;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            this.sValue = GenericUtils.getEnglishString(this.bValue);
            Tr.debug(tc, "extractInitialValue parsed [" + getDebugValue() + "]");
        }
        return true;
    }

    /**
     * @see HeaderField#asBytes()
     */
    public byte[] asBytes() {
        if (null == this.bValue) {
            if (null != this.sValue) {
                this.bValue = GenericUtils.getEnglishBytes(this.sValue);
            } else {
                // try to get the initial "parsed" value
                if (!extractInitialValue()) {
                    // no data... invalid use of this object if this happens
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "byte[] value requested from empty structure");
                    }
                }
            }
        } else if (this.valueLength != this.bValue.length) {
            // need to extract the value from the larger array
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Extracting value from larger input array");
            }
            byte[] temp = new byte[this.valueLength];
            System.arraycopy(this.bValue, this.offset, temp, 0, temp.length);
            // reset the the stored array to the new "only the value" array
            this.bValue = temp;
            this.offset = 0;
            this.valueLength = temp.length;
        }

        return this.bValue;
    }

    /**
     * Query the raw bytes value. If the storage is based on a larger value where
     * the actual header value is a subset of the array, then the caller should
     * be checking the getOffset() and getValueLength() methods to know how much
     * of this returned array to use.
     * 
     * @return byte[], null if no value is present
     */
    protected byte[] asRawBytes() {
        if (null == this.bValue) {
            if (null != this.sValue) {
                this.bValue = GenericUtils.getEnglishBytes(this.sValue);
            } else {
                // try to extract from the parse buffers
                if (!extractInitialValue()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "no byte[] value present");
                    }
                }
            }
        }
        return this.bValue;
    }

    /**
     * @see HeaderField#asString()
     */
    public String asString() {
        if (null == this.sValue) {
            if (null == this.bValue) {
                // pull the initial value if possible
                if (!extractInitialValue()) {
                    // no data... invalid use of object if this happens
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "String value requested from empty structure");
                    }
                    return null;
                }
            }
            // does no encoding, except bitwise and'ing to get single byte chars
            this.sValue = GenericUtils.getEnglishString(this.bValue, this.offset, (this.offset + this.valueLength));
        }

        return this.sValue;
    }

    /**
     * Query the string value of this header that is sanitized for debug logs.
     * Some headers may disallow the logging of the value for security reasons.
     * 
     * @return String
     */
    protected String getDebugValue() {
        if (this.key.shouldLogValue()) {
            return asString();
        }
        // this header wants to block the contents of the value from debug
        return GenericUtils.blockContents(asString());
    }

    /**
     * @see HeaderField#asDate()
     */
    public Date asDate() throws ParseException {
        return HttpDispatcher.getDateFormatter().parseTime(asString());
    }

    /**
     * @see HeaderField#asInteger()
     */
    public int asInteger() {
        String value = asString();
        if (null != value) {
            return Integer.parseInt(value.trim());
        }
        throw new NumberFormatException("Missing value");
    }

    /**
     * @see HeaderField#asTokens(byte)
     */
    public List<byte[]> asTokens(byte delimiter) {
        List<byte[]> list = new ArrayList<byte[]>();
        byte[] input = asBytes();
        if (null == input || 0 == input.length) {
            return list;
        }
        byte[] token = null;
        int tokenStartPosition = 0;
        int tokenLength = 0;

        for (int i = 0; i < input.length; i++) {
            if (BNFHeaders.SPACE == input[i] || delimiter == input[i]) {
                if (0 < tokenLength) {
                    token = new byte[tokenLength];
                    System.arraycopy(input, tokenStartPosition, token, 0, tokenLength);
                    list.add(token);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Parsed token [" + GenericUtils.getEnglishString(token) + "]");
                    }
                }
                tokenLength = 0;
                // To account for the delimiter
                tokenStartPosition = i + 1;
            } else {
                tokenLength++;
            }
        }

        // Last token
        if (0 < tokenLength) {
            token = new byte[tokenLength];
            System.arraycopy(input, tokenStartPosition, token, 0, tokenLength);
            list.add(token);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Parsed token [" + GenericUtils.getEnglishString(token) + "]");
            }
        }

        return list;
    }

    /**
     * Set the byte array value to the given input.
     * 
     * @param input
     */
    protected void setByteArrayValue(byte[] input) {
        this.sValue = null;
        this.bValue = input;
        this.offset = 0;
        this.valueLength = input.length;
        if (ELEM_ADDED != this.status) {
            this.status = ELEM_CHANGED;
        }
    }

    /**
     * Set the byte array value of this header based on the input array but
     * starting at the input offset into that array and with the given length.
     * 
     * @param input
     * @param offset
     * @param length
     * @throws IllegalArgumentException if offset and length are incorrect
     */
    protected void setByteArrayValue(byte[] input, int offset, int length) {
        if ((offset + length) > input.length) {
            throw new IllegalArgumentException(
                            "Invalid length: " + offset + "+" + length + " > " + input.length);
        }
        this.sValue = null;
        this.bValue = input;
        this.offset = offset;
        this.valueLength = length;
        if (ELEM_ADDED != this.status) {
            this.status = ELEM_CHANGED;
        }
    }

    /**
     * Set the string value to the given input.
     * 
     * @param input
     */
    protected void setStringValue(String input) {
        this.bValue = null;
        this.sValue = input;
        this.offset = 0;
        this.valueLength = (null == input) ? 0 : input.length();
        if (ELEM_ADDED != this.status) {
            this.status = ELEM_CHANGED;
        }
    }

    /**
     * Test whether this instance equals another instance of this class.
     * 
     * @param inElem (another HeaderElement)
     * @return boolean (true if match, false if not)
     */
    public boolean equals(Object inElem) {
        if (this == inElem) {
            return true;
        }
        // instanceof handles class types and null input
        if (!(inElem instanceof HeaderElement)) {
            return false;
        }
        return this.key.equals(((HeaderElement) inElem).getKey());
    }

    /**
     * Compare whether the input element matchs the header name present in this
     * current element.
     * 
     * @param elem
     * @return boolean
     */
    protected boolean isSameName(HeaderElement elem) {
        return this.key.getOrdinal() == elem.key.getOrdinal();
    }

    /**
     * Compare whether the value of the input element matchs this local value.
     * This is a case sensitive comparison.
     * 
     * @param elem
     * @return boolean
     */
    protected boolean isSameValue(HeaderElement elem) {
        return asString().equals(elem.asString());
    }

    /**
     * Compare whether the value of the input element matchs this local value
     * using a case-insensitive comparison.
     * 
     * @param elem
     * @return boolean
     */
    protected boolean isSameValueIgnoreCase(HeaderElement elem) {
        return asString().equalsIgnoreCase(elem.asString());
    }

    /**
     * Return the hashcode of this object.
     * 
     * @return int
     */
    public int hashCode() {
        // lazily instantiate this if we need to
        if (-1 == this.myHashCode) {
            this.myHashCode = initHash + this.key.hashCode();
        }
        return this.myHashCode;
    }

    /**
     * Compare this object against the given value. Returns a negative integer,
     * zero, or a positive integer as this object is less than, equal to, or
     * greater than the specified object.
     * 
     * @param o
     * @return int
     */
    public int compareTo(HeaderElement o) {
        if (this == o) {
            return 0;
        }
        // supposed to throw a ClassCastException if incorrect input
        return hashCode() - o.hashCode();
    }

    /**
     * Query whether or not this element has been removed.
     * 
     * @return boolean
     */
    protected boolean wasRemoved() {
        return ELEM_REMOVED == this.status;
    }

    /**
     * Query whether or not this element has changed.
     * 
     * @return boolean
     */
    protected boolean wasChanged() {
        return ELEM_CHANGED == this.status;
    }

    /**
     * Query whether or not this element was freshly added.
     * 
     * @return boolean
     */
    protected boolean wasAdded() {
        return ELEM_ADDED == this.status;
    }

    /**
     * Notify this element that it has been removed from active storage.
     * 
     */
    protected void remove() {
        if (ELEM_REMOVED != this.status) {
            this.status = ELEM_REMOVED;
            this.myOwner.decrementHeaderCounter();
        }
    }

    /**
     * Once the state of this element is set, this method will start the
     * tracking effort of knowing whether it was removed or changed.
     * 
     */
    protected void startTracking() {
        this.status = ELEM_INIT;
    }

    /**
     * Set the relevant information for the CRLF position information from the
     * parsing code.
     * 
     * @param index
     * @param pos
     * @param isCR
     */
    protected void updateLastCRLFInfo(int index, int pos, boolean isCR) {
        this.lastCRLFBufferIndex = index;
        this.lastCRLFPosition = pos;
        this.lastCRLFisCR = isCR;
    }

    /**
     * Query the position of the CRLF prior to this header instance.
     * 
     * @return int
     */
    protected int getLastCRLFPosition() {
        return this.lastCRLFPosition;
    }

    /**
     * Query the buffer index of the last CRLF prior to this header instance.
     * 
     * @return int
     */
    protected int getLastCRLFBufferIndex() {
        return this.lastCRLFBufferIndex;
    }

    /**
     * Query whether or not the CRLF position points to a CR or an LF.
     * 
     * @return boolean
     */
    protected boolean isLastCRLFaCR() {
        return this.lastCRLFisCR;
    }

    /**
     * @see HeaderField#getName()
     */
    public String getName() {
        return this.key.getName();
    }

    /**
     * Perform cleanup when this object is no longer needed.
     * 
     */
    protected void destroy() {
        this.nextSequence = null;
        this.prevSequence = null;
        this.bValue = null;
        this.sValue = null;
        this.buffIndex = -1;
        this.offset = 0;
        this.valueLength = 0;
        this.myHashCode = -1;
        this.lastCRLFBufferIndex = -1;
        this.lastCRLFisCR = false;
        this.lastCRLFPosition = -1;
        this.status = ELEM_ADDED;
        this.myOwner.freeElement(this);
    }

    /**
     * Initialize this element to the header key.
     * 
     * @param name
     */
    protected void init(HeaderKeys name) {
        this.key = name;
    }

    /**
     * Debug print information on this header instance.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append('[').append(this.key.getName()).append('=');
        sb.append(getDebugValue()).append("] ").append(this.status);
        return sb.toString();
    }

}
