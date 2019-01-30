/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.utils;

/**
 *
 */
/**
 * <p>
 * This module is a utility class to work with byte arrays.
 * It performs initialization, copies and compares byte arrays.
 * </p>
 *
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 * @ibm-spi
 */
public class ByteArray implements java.io.Serializable {

    private static final long serialVersionUID = 7739289083575118864L;

    byte[] byteArray;

    int hash = 0;

    /**
     * <p>
     * Default constructor
     * </p>
     * Returns a ByteArray given a byte[]
     *
     * @param the array to convert into ByteArray
     * @return the ByteArray
     */
    public ByteArray(byte[] barray) {
        byteArray = barray;
    }

    /**
     * <p>
     * The <code>getArray</code> method returns a byte array
     * of type byte [].
     * </p>
     *
     * @return the byte[]
     */
    public byte[] getArray() {
        return byteArray;
    }

    /**
     * <p>
     * The <code>setArray</code> method sets the byte array
     * of type byte[].
     * </p>
     *
     * @param the byte[] array
     */
    public void setArray(byte[] array) {
        hash = 0;
        byteArray = array;
    }

    /**
     * <p>
     * The <code>equals</code> method determines if the
     * array object passed in is equal to another array.
     * </p>
     *
     * @param the array object
     * @return true if the two arrays are equal
     */
    @Override
    public boolean equals(Object arrayObj) {
        if (arrayObj instanceof ByteArray) {
            ByteArray anotherArrayObject = (ByteArray) arrayObj;
            return java.util.Arrays.equals(byteArray, anotherArrayObject.getArray()); //@MD18387C
        }
        return false;
    }

    /**
     * <p>
     * The <code>hashCode</code> method determines the hash of an array.
     * </p>
     *
     * @return the hash
     */
    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            int len = byteArray.length;
            for (int i = 0; i < len; i++)
                h = 31 * h + byteArray[i];
            hash = h;
        }
        return h;
    }

    /**
     * <p>
     * The <code>initialize</code> method initializes the array to a given value.
     * </p>
     *
     * @param the array to initialize
     * @param the value
     */
    public static void initialize(byte[] array, byte val) {
        if (array == null) {
            throw new IllegalArgumentException("Null Byte array");
        }
        for (int i = 0, len = array.length; i < len; i++) {
            array[i] = val;
        }
    }

    /**
     * <p>
     * The <code>copy</code> method copies data of length <code>len</code>
     * from the array <code>from</code> to the array <code>to</code>,
     * from the offset <code>offsetFrom</code> to the offset <code>offsetTo</code>.
     * </p>
     *
     * @param the input array
     * @param the offset for the input array
     * @param the length to copy
     * @param the output array
     * @param the offset for the output array
     */
    public static void copy(byte[] from, int offsetFrom, int len,
                            byte[] to, int offsetTo) {
        for (int i = 0; i < len; i++) {
            to[offsetTo + i] = from[offsetFrom + i];
        }
    }

    /**
     * The <code>compare</code> method compares the byte arrays for equality.
     *
     * @param first byte array
     * @param second byte array
     * @return true if the byte arrays are equal
     */

    public static boolean compare(byte[] a, byte[] b) {

        if (a == null || b == null || a.length != b.length) {
            return false;
        }

        return java.util.Arrays.equals(a, b);
    }

    /**
     * The <code>XOR</code> method performs the xor function of two byte arrays.
     *
     * @param first byte array
     * @param second byte array
     * @return the resulting byte array
     */
    public static byte[] XOR(byte[] a, byte b) {
        byte[] t = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            t[i] = (byte) (a[i] ^ b);
        }
        return t;
    }

}