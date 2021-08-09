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

/**
 * Basic class for the predefined headers used in messages.
 * 
 * @ibm-private-in-use
 */
public abstract class HeaderKeys extends GenericKeys {

    /** Marshalled byte[] of "Name: " */
    private byte[] marshalledBytes = null;
    /** Flag on whether the filterAdd/Remove methods are used for this Header */
    private boolean bUseFilters = false;
    /** Flag on whether debug should log the contents of the header value */
    private boolean bLogValue = true;
    /** Flag on whether this was created at startup or during runtime */
    private boolean bUndefined = false;

    /**
     * Constructor allowing the specific ordinal setting, useful
     * for error case type instances (HeaderKeys "No Match").
     * 
     * @param name
     * @param ordinal
     */
    public HeaderKeys(String name, int ordinal) {
        super(name, ordinal);
        setMarshalledBytes();
    }

    /**
     * Save this headerkey in it's marshalled form [Header: ] for faster
     * overall marshalling of the message.
     * 
     */
    protected void setMarshalledBytes() {
        int len = getName().length();
        this.marshalledBytes = new byte[len + BNFHeaders.KEY_VALUE_SEPARATOR.length];
        System.arraycopy(getByteArray(), 0, this.marshalledBytes, 0, len);
        System.arraycopy(BNFHeaders.KEY_VALUE_SEPARATOR, 0, this.marshalledBytes, len, BNFHeaders.KEY_VALUE_SEPARATOR.length);
    }

    /**
     * Query the marshalled bytes from this object.
     * 
     * @param compactForm
     * @return byte[]
     */
    @SuppressWarnings("unused")
    public byte[] getMarshalledByteArray(boolean compactForm) {
        return this.marshalledBytes;
    }

    /**
     * Method for subclasses to change the use-filters flag.
     * 
     * @param flag
     */
    protected void setUseFilters(boolean flag) {
        this.bUseFilters = flag;
    }

    /**
     * Query whether this particular header should use filters or not when
     * adding and removing instances.
     * 
     * @return boolean
     */
    public boolean useFilters() {
        return this.bUseFilters;
    }

    /**
     * Method for subclasses to change the flag on whether to log the value
     * in debug logs.
     * 
     * @param flag
     */
    protected void setShouldLogValue(boolean flag) {
        this.bLogValue = flag;
    }

    /**
     * Query whether this particular header allows the logging of the value in
     * debug logs.
     * 
     * @return boolean
     */
    public boolean shouldLogValue() {
        return this.bLogValue;
    }

    /**
     * Query whether this header was defined at startup or during runtime.
     * 
     * @return boolean - true means runtime defined object
     */
    public boolean isUndefined() {
        return this.bUndefined;
    }

    /**
     * Set the flag on whether this is undefined or not.
     * 
     * @param flag
     */
    protected void setUndefined(boolean flag) {
        this.bUndefined = flag;
    }

    /**
     * Get the enumerated instance that matches this input ordinal.
     * 
     * @param i
     * @return Object (HeaderKeys)
     * @throws IndexOutOfBoundsException
     *             if this ordinal is not valid
     */
    public abstract Object getEnumByOrdinal(int i);

    /**
     * @see com.ibm.wsspi.genericbnf.GenericKeys#toString()
     */
    public String toString() {
        return super.toString() + " undefined: " + isUndefined();
    }
}
