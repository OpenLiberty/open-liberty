/* ============================================================================
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ============================================================================
 */
package com.ibm.ws.sib.jms.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Static class providing methods for encoding and decoding Unicode Strings and chars to
 * UTF8 bytes.
 */
public enum Utf8Codec {
    ;

    /**
     * Calculate the number of bytes needed to UTF8 encode a String
     *
     * @param s The String whose encoded length is wanted
     *
     * @return int The encoded length of the given String
     */
    public static int getEncodedLength(String s) {
        int count = 0;
        int strLength = s.length();
        for (int i = 0; i < strLength; i++) {
            int cp = s.codePointAt(i);
            if (cp < 0x80) {
                count += 1;
            } else if (cp < 0x800) {
                count += 2;
            } else if (cp < 0x10000) {
                count += 3;
            } else {
                count += 4;
                i++;
            }
        }
        return count;
    }

    /**
     * Encode a String into the given offset into a byte array, & return the new offset
     *
     * Note that:
     * 1. It is the caller's responsibility to provide a byte array long enough to
     * contain the entire encoded String. There is no checking of the length prior
     * to encoding, so ArrayIndexOutOfBoundsException will be thrown if the encode
     * falls off the end of the byte array.
     * 2. Any values already in the relevant portion of the byte array will be lost.
     *
     * @param buff The byte array into which the char should be encoded
     * @param offset The offset into buff at which the char should be encoded
     * @param s The String to be encoded.
     *
     * @return int The number of bytes written to the byte array.
     */
    public static int encode(byte[] buff, int offset, String s) {
        byte[] ba = encode(s);
        System.arraycopy(ba, 0, buff, offset, ba.length);
        return ba.length;
    }

    /**
     * Encode a String to UTF8 and return the resulting bytes
     *
     * @param s The String to be encoded
     *
     * @return byte[] The UTF8 encoding of the given String
     */
    public static byte[] encode(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Decodes UTF8 to a String
     */
    public static String decode(byte[] ba, int offset, int length) {
        return new String(ba, offset, length, StandardCharsets.UTF_8);
    }

    /**
     * Decodes UTF8 to a String
     */
    public static String decode(byte[] ba) {
        return decode(ba, 0, ba.length);
    }
}
