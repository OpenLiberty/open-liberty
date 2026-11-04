/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.zos.channel.wola.internal.natv;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * ASCII-to-EBCDIC codepage conversion utilities.
 */
@Trivial
public class CodepageUtils {

    /**
     * EBCDIC codeset name.
     */
    public final static String EBCDIC = "Cp1047";

    /**
     * ASCII codeset name
     */
    public final static String ASCII = "ISO-8859-1";

    /**
     * @return The EBCDIC bytes for the given String. If the String is null, an empty byte[] is returned.
     *
     * @throws RuntimeException if for whatever reason the getBytes call throws an UnsupportedEncodingException.
     */
    public static byte[] getEbcdicBytes(String s) {
        return getBytes(s, EBCDIC);
    }

    /**
     * @param s       The string to convert
     * @param charset The charset to use
     *
     * @return The bytes for the given String. If the String is null, an empty byte[] is returned.
     *
     * @throws RuntimeException if for whatever reason the getBytes call throws an UnsupportedEncodingException.
     */
    public static byte[] getBytes(String s, String charset) {
        try {
            String s1 = (s != null) ? s : "";
            return s1.getBytes(charset);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }

    /**
     * @return The given string, padded to the right with whitespace for a total len of n.
     */
    public static String padRight(String s, int n) {
        //return String.format("%1$-" + n + "s", firstNotNull(s, ""));  Too expensive.
        String s1 = (s != null) ? s : "";
        int len = s1.length();
        if (len >= n) {
            return s1;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(s1);
        int blanksToAdd = n - len;
        for (int x = 0; x < blanksToAdd; x++) {
            sb.append(" ");
        }

        return sb.toString();
    }

    /**
     * @return The charset-encoded bytes of the given string, blank-padded to the
     *         given length n.
     */
    public static byte[] getBytesPadded(String s, int n, String charset) {
        return getBytes(padRight(s, n).substring(0, n), charset);
    }

    /**
     * @return The EBCDIC bytes of the given string, padded out with whitespace to the given length n.
     *         Note: if the string is already longer than n bytes, then only the first n bytes are returned.
     */
    public static byte[] getEbcdicBytesPadded(String s, int n) {
        return getBytesPadded(s, n, EBCDIC);
    }

    /**
     * Convert the given string to EBCDIC and return it as a long.
     * If the string is not 8 bytes, it is padded with blanks.
     *
     * @param s - The string to convert to ebcdic bytes
     *
     * @return the EBCDIC bytes of the given string, as a long.
     */
    public static long getEbcdicBytesAsLong(String s) {
        return ByteBuffer.wrap(getEbcdicBytesPadded(s, 8)).getLong();
    }
}
