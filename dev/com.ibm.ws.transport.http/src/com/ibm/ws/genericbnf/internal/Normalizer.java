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

/**
 * Utility class for normalizing chars.
 * <p>
 * It might normalize to uppercase, lowercase, or none at all.
 */
public class Normalizer {

    /** No normalization on this matcher */
    public static final int NORMALIZE_OFF = 0;
    /** Normalization of values to uppercase */
    public static final int NORMALIZE_UPPER = 1;
    /** Normalization of values to lowercase */
    public static final int NORMALIZE_LOWER = 2;
    /** Flag on whether to use upper case when matching */
    private int normalizeFlag = NORMALIZE_OFF;

    /**
     * Constructor. The normalization flag can only be set once.
     * 
     * @param flag
     */
    public Normalizer(int flag) {
        if (NORMALIZE_OFF != flag && NORMALIZE_UPPER != flag && NORMALIZE_LOWER != flag) {
            throw new IllegalArgumentException("Unable to create Normalizer using identifier " + flag);
        }
        this.normalizeFlag = flag;
    }

    /**
     * Query the normalization flag.
     * 
     * @return int
     */
    public final int getNormalization() {
        return this.normalizeFlag;
    }

    /**
     * Normalize will convert a target String to the format that matches the
     * keys in the tree (lowercase for example).
     * 
     * @param path
     * @return String (the normalized version)
     */
    public String normalize(String path) {

        // if normalize is off, just return
        if (NORMALIZE_OFF == getNormalization() || null == path) {
            return path;
        }
        String normalizedString = path;
        int len = path.length();
        // if there's no data, just return
        if (0 != len) {
            char[] buf = new char[len];
            path.getChars(0, len, buf, 0);
            for (int index = 0; index < len; index++) {
                buf[index] = normalize(buf[index]);
            }
            normalizedString = new String(buf, 0, len);
        }

        return normalizedString;
    }

    /**
     * Normalize the given byte[] of data into a String based on the global
     * normalization "type" flag.
     * 
     * @param data
     * @return String (null String if null input);
     */
    public String normalize(byte[] data) {

        if (null == data) {
            return null;
        }
        if (0 == data.length) {
            return "";
        }
        // if normalization is off, just return
        if (NORMALIZE_OFF == getNormalization()) {
            return new String(data);
        }

        char[] buf = new char[data.length];
        for (int index = 0; index < data.length; index++) {
            buf[index] = normalize((char) data[index]);
        }

        return new String(buf, 0, buf.length);
    }

    /**
     * Take the input character and normalize based on this normalizer instance.
     * 
     * @param currentChar
     * @return char (normalized based on settings)
     */
    public char normalize(char currentChar) {
        if (NORMALIZE_UPPER == getNormalization()) {
            return toUpper(currentChar);
        }
        if (NORMALIZE_LOWER == getNormalization()) {
            return toLower(currentChar);
        }
        return currentChar;
    }

    /**
     * Utility method to convert a character to the uppercase form
     * 
     * @param c
     * @return char (uppercase version of c)
     */
    private static char toUpper(char c) {
        if (c >= 'a' && c <= 'z') {
            return (char) (c - 'a' + 'A');
        }
        return c;
    }

    /**
     * Utility method convert a character to the lowercase form
     * 
     * @param c
     * @return char (lowercase version of c)
     */
    private static char toLower(char c) {
        if (c >= 'A' && c <= 'Z') {
            return (char) (c - 'A' + 'a');
        }
        return c;
    }

    /**
     * Normalize the given byte[] of data into a String based on the input
     * format.
     * 
     * @param data
     * @param format
     * @return String (null String if null input);
     */
    static public String normalize(byte[] data, int format) {

        if (null == data) {
            return null;
        }
        if (0 == data.length) {
            return "";
        }
        // if normalization is off, just return
        if (NORMALIZE_OFF == format) {
            return new String(data);
        }

        char[] buf = new char[data.length];
        for (int index = 0; index < data.length; index++) {
            buf[index] = normalize((char) data[index], format);
        }

        return new String(buf, 0, buf.length);
    }

    /**
     * Take the input character and normalize based on the input format.
     * 
     * @param input
     * @param format
     * @return normalized character.
     */
    static public char normalize(char input, int format) {
        if (NORMALIZE_LOWER == format) {
            return toLower(input);
        }
        if (NORMALIZE_UPPER == format) {
            return toUpper(input);
        }
        return input;
    }

    /**
     * Normalize will convert a target String to the format that matches the
     * keys in the tree (lowercase for example).
     * 
     * @param path
     * @param format
     * @return String (the normalized version)
     */
    static public String normalize(String path, int format) {

        // if normalize is off, just return
        if (NORMALIZE_OFF == format || null == path) {
            return path;
        }
        String normalizedString = path;
        int len = path.length();
        // if there's no data, just return
        if (0 != len) {
            char[] buf = new char[len];
            path.getChars(0, len, buf, 0);
            for (int index = 0; index < len; index++) {
                buf[index] = normalize(buf[index], format);
            }
            normalizedString = new String(buf, 0, len);
        }

        return normalizedString;
    }

}
