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
package com.ibm.ws390.ola.jca;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * ASCII-to-EBCDIC codepage conversion utilities.
 */
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
        try {
            return firstNotNull(s, "").getBytes(EBCDIC);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }

    /**
     * @return The given string, padded to the right with whitespace for a total len of n.
     */
    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", firstNotNull(s, ""));
    }

    /**
     * @return The EBCDIC bytes of the given string, padded out with whitespace to the given length n.
     *         Note: if the string is already longer than n bytes, then only the first n bytes are returned.
     */
    public static byte[] getEbcdicBytesPadded(String s, int n) {
        return getEbcdicBytes(padRight(s, n).substring(0, n));
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

    /**
     * @return the first parm that's not null. If all are null, then null is returned.
     */
    protected static <T> T firstNotNull(T... objs) {
        for (T obj : objs) {
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }
}
