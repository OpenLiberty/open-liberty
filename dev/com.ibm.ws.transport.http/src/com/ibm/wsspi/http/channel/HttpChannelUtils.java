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
package com.ibm.wsspi.http.channel;

import java.util.Hashtable;
import java.util.Map;

/**
 * This class incorporates all of the utility methods that the HTTP channel
 * needs to expose to application channels above.
 * 
 * @ibm-private-in-use
 */
public final class HttpChannelUtils {

    /** HEX character list */
    private static final byte[] HEX_BYTES = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a',
                                             (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };
    /** byte[] representation of a zero int */
    private static final byte[] ZERO_BYTEARRAY = { HEX_BYTES[0] };
    /** How many digits are possible with a maximum int value */
    private static final int SIZE_MAXINT = 10;

    /**
     * Private constructor as these are all static methods
     * 
     */
    private HttpChannelUtils() {
        // nothing to do
    }

    /**
     * Utility method to convert an integer (positive or negative) into the
     * byte[] representation. So "50" would return a byte[2] of "50", "-100"
     * would return a byte[3] of "-100".
     * 
     * @param value
     * @return byte[]
     */
    static public byte[] asByteArray(int value) {

        // check for 0
        if (0 == value) {
            return ZERO_BYTEARRAY;
        }

        // 2^31 is a 10 digit number so make space for the max size
        byte[] bytes = new byte[SIZE_MAXINT];
        // check for negative ints
        boolean bNegative = false;
        if (0 > value) {
            // force it positive for parsing
            bNegative = true;
            value = -value;
        }
        // now loop back through each digit in the int
        int index = SIZE_MAXINT - 1;
        for (; 0 <= index && 0 != value; index--) {
            bytes[index] = HEX_BYTES[value % 10];
            value /= 10;
        }

        // length is how ever many digits there were + a possible negative sign
        int len = (SIZE_MAXINT - 1 - index);
        if (bNegative) {
            len++;
        }

        // now copy out the "real bytes" for returning
        byte[] realBytes = new byte[len];
        for (int i = len - 1, x = SIZE_MAXINT - 1; 0 <= i; i--, x--) {
            realBytes[i] = bytes[x];
        }
        // add negative sign if we need to
        if (bNegative) {
            realBytes[0] = '-';
        }
        return realBytes;
    }

    /**
     * Take an input byte[] and return the int translation. For example, the
     * byte[] '0053' would return 53.
     * 
     * @param array
     * @return int
     * @throws NumberFormatException
     *             (if the data contains invalid digits)
     */
    static public int asIntValue(byte[] array) {

        if (null == array) {
            return -1;
        }

        int intVal = 0;
        int mark = 1;
        int digit;
        int i = array.length - 1;
        for (; 0 <= i; i--) {
            digit = array[i] - HEX_BYTES[0];
            if (0 > digit || 9 < digit) {
                // stop on any nondigit, if it's not a DASH then throw an exc
                if ('-' != array[i]) {
                    throw new NumberFormatException("Invalid digit: " + array[i]);
                }
                break;
            }
            intVal += digit * mark;
            mark *= 10;
        }

        // check for negative numbers
        if (0 <= i && array[i] == '-') {
            intVal = -intVal;
        }

        return intVal;
    }

    /**
     * Utility method to get ISO english encoded bytes from the input string.
     * 
     * @param data
     * @return byte[]
     */
    static public byte[] getEnglishBytes(String data) {
        if (null == data) {
            return null;
        }
        char[] chars = data.toCharArray();
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }

    /**
     * Utility method to get the ISO English string from the given bytes.
     * 
     * @param data
     * @return String
     */
    static public String getEnglishString(byte[] data) {
        if (null == data) {
            return null;
        }
        char chars[] = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            chars[i] = (char) (data[i] & 0xff);
        }
        return new String(chars);
    }

    /**
     * Utility method to get the ISO English string from the given bytes.
     * 
     * @param data
     * @param start
     * @param end
     * @return String
     */
    static public String getEnglishString(byte[] data, int start, int end) {
        int len = end - start;
        if (0 >= len || null == data) {
            return null;
        }
        char chars[] = new char[len];
        for (int i = start; i < end; i++) {
            chars[i] = (char) (data[i] & 0xff);
        }
        return new String(chars);
    }

    /**
     * Utility method to get the bytes from a StringBuffer. These bytes will
     * be in whatever encoding was in the original chars put into the string
     * buffer object.
     * 
     * @param data
     * @return byte[]
     */
    static public byte[] getBytes(StringBuffer data) {
        if (null == data) {
            return null;
        }
        int len = data.length();
        char[] chars = new char[len];
        data.getChars(0, len, chars, 0);
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }

    /**
     * Utility method to get the bytes from a StringBuilder. These bytes will
     * be in whatever encoding was in the original chars put into the string
     * buffer object.
     * 
     * @param data
     * @return byte[]
     */
    static public byte[] getBytes(StringBuilder data) {
        if (null == data) {
            return null;
        }
        int len = data.length();
        char[] chars = new char[len];
        data.getChars(0, len, chars, 0);
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }

    /**
     * Parse a query parameter's name out of the input character array. This
     * will undo any URLEncoding in the data (%20 is a space for example) and
     * return the resulting string.
     * 
     * Input must be in ASCII encoding.
     * 
     * @param ch
     * @param start
     * @param end
     * @return String
     * @throws IllegalArgumentException
     */
    static private String parseName(char[] ch, int start, int end) {
        int len = 0;
        char[] name = new char[end - start];

        for (int i = start; i < end; i++) {
            switch (ch[i]) {
                case '+':
                    // translate plus symbols to spaces
                    name[len++] = ' ';
                    break;
                case '%':
                    // translate "%xx" to appropriate character (i.e. %20 is space)
                    if ((i + 2) < end) {
                        int num1 = Character.digit(ch[++i], 16);
                        if (-1 == num1) {
                            throw new IllegalArgumentException("" + ch[i]);
                        }
                        int num2 = Character.digit(ch[++i], 16);
                        if (-1 == num2) {
                            throw new IllegalArgumentException("" + ch[i]);
                        }
                        name[len++] = (char) ((num1 << 4) | num2);
                    } else {
                        // allow '%' at end of value or second to last character
                        for (; i < end; i++) {
                            name[len++] = ch[i];
                        }
                    }
                    break;
                default:
                    // regular character, just save it
                    name[len++] = ch[i];
                    break;
            }
        }
        return new String(name, 0, len);
    }

    /**
     * Parse a string of query parameters into a Map representing the values
     * stored using the name as the key.
     * 
     * @param data
     * @param encoding
     * @return Map
     * @throws NullPointerException
     *             if input string is null
     * @throws IllegalArgumentException
     *             if the string is formatted incorrectly
     */
    static public Map<String, String[]> parseQueryString(String data, String encoding) {
        if (null == data) {
            throw new NullPointerException("query data");
        }
        Map<String, String[]> map = new Hashtable<String, String[]>();
        String valArray[] = null;
        char[] chars = data.toCharArray();
        int key_start = 0;
        for (int i = 0; i < chars.length; i++) {
            // look for the key name delimiter
            if ('=' == chars[i]) {
                if (i == key_start) {
                    // missing the key name
                    throw new IllegalArgumentException("Missing key name: " + i);
                }
                String key = parseName(chars, key_start, i);
                int value_start = ++i;
                for (; i < chars.length && '&' != chars[i]; i++) {
                    // just keep looping looking for the end or &
                }
                if (i > value_start) {
                    // did find at least one char for the value
                    String value = parseName(chars, value_start, i);
                    if (map.containsKey(key)) {
                        String oldVals[] = map.get(key);
                        valArray = new String[oldVals.length + 1];
                        System.arraycopy(oldVals, 0, valArray, 0, oldVals.length);
                        valArray[oldVals.length] = value;
                    } else {
                        valArray = new String[] { value };
                    }
                    map.put(key, valArray);
                }
                key_start = i + 1;
            }
        }
        return map;
    }

    /**
     * API to trigger the HTTP bundle activation if not already loaded.
     */
    public static void activateBundle() {
        // no-op
    }

}
