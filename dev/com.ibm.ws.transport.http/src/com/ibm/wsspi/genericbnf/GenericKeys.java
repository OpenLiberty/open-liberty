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
 * Generic abstract class for the various enumerated classes in the
 * Channel. Each basic key has a String name and an int ordinal to
 * match. The extended classes are responsible for maintaining the
 * static list of all the keys in the enum, along with anything extra
 * they may require.
 *
 * @ibm-private-in-use
 */
public abstract class GenericKeys implements Comparable<GenericKeys> {

    /** String version of the key's name */
    protected String name = null;
    /** byte[] version of the key's name */
    protected byte[] byteArray = null;
    /** Ordinal associated with the key */
    protected int ordinal = -1;
    /** Hashcode for this object */
    protected int hashcode;

    /**
     * Constructor is limited to the subclasses.
     *
     * @param inputName
     * @param inputOrdinal
     */
    protected GenericKeys(String inputName, int inputOrdinal) {
        this.name = inputName;
        this.byteArray = inputName.getBytes(HeaderStorage.ENGLISH_CHARSET);
        this.ordinal = inputOrdinal;
        this.hashcode = inputOrdinal + inputName.hashCode();
    }

    /**
     * Query the name of this key as a byte[].
     *
     * @return byte[]
     */
    final public byte[] getByteArray() {
        return this.byteArray;
    }

    /**
     * Query the ordinal number for this header.
     *
     * @return int
     */
    final public int getOrdinal() {
        return this.ordinal;
    }

    /**
     * Query the name for this key as a String.
     *
     * @return String
     */
    public String getName() {
        if (null == this.name && null != this.byteArray) {
            this.name = new String(this.byteArray, HeaderStorage.ENGLISH_CHARSET);
        }
        return this.name;
    }

    /**
     * For debugging purposes, convert this object to a String.
     *
     * @return String
     */
    @Override
    public String toString() {
        return "Key: " + getName() + " Ordinal: " + getOrdinal();
    }

    /**
     * Compare this key against the given value. Returns a negative integer,
     * zero, or a positive integer as this object is less than, equal to, or
     * greater than the specified GenericKeys object.
     *
     * @param inKey
     * @return int
     */
    @Override
    public int compareTo(GenericKeys inKey) {
        return (null == inKey) ? -1 : (getOrdinal() - inKey.getOrdinal());
    }

    /**
     * Check whether this object equals another.
     *
     * @param val
     * @return boolean
     */
    @Override
    public boolean equals(Object val) {

        if (this == val) {
            return true;
        }
        // instanceof handles class types and null input
        if (!(val instanceof GenericKeys)) {
            return false;
        }
        return (hashCode() == ((GenericKeys) val).hashCode());
    }

    /**
     * Allow an equality check against another enum object.
     *
     * @param val
     * @return boolean (true if ordinals match)
     */
    public boolean equals(GenericKeys val) {
        return (null == val) ? false : (hashCode() == val.hashCode());
    }

    /**
     * Hash code of this object.
     *
     * @return int
     */
    @Override
    public int hashCode() {
        return this.hashcode;
    }

}
