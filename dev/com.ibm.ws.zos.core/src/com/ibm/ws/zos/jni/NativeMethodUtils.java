/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.jni;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * JNI utilities.
 */
public class NativeMethodUtils {

    /**
     * EBCDIC codeset name.
     */
    public final static String EBCDIC = "Cp1047";

    /**
     * Cache of ASCII-to-EBCDIC conversions.
     */
    private final static ConcurrentHashMap<String, byte[]> a2eCache = new ConcurrentHashMap<String, byte[]>();

    /**
     * Ascii-to-Ebcdic conversion.
     *
     * Append null-term, then convert bytes to EBCDIC, return byte[].
     * The null-termed ebcdic byte array is passed to native. This is to avoid
     * using JNI methods like GetStringChars or GetStringUTFChars, which return
     * Unicode (double-byte) and UTF-8 (multi-byte) char strings, respectively.
     * We need single-byte ebcdic char strings, and the most efficient way of
     * getting them (that I know of) is to pull the byte[] in Java and pass it
     * down directly. The native code can cast the byte[] to a char *.
     *
     * @param s The String to convert.
     *
     * @return A byte[] containing the String bytes in EBCDIC form, with a
     *         null-term at the end.
     *
     * @throws RuntimeException for any UnsupportedEncodingExceptions.
     */
    public static byte[] convertToEBCDIC(String s) {
        return convertToEBCDICNoTrace(s);
    }

    /**
     * Ascii-to-Ebcdic conversion.
     *
     * Append null-term, then convert bytes to EBCDIC, return byte[].
     * The null-termed ebcdic byte array is passed to native. This is to avoid
     * using JNI methods like GetStringChars or GetStringUTFChars, which return
     * Unicode (double-byte) and UTF-8 (multi-byte) char strings, respectively.
     * We need single-byte ebcdic char strings, and the most efficient way of
     * getting them (that I know of) is to pull the byte[] in Java and pass it
     * down directly. The native code can cast the byte[] to a char *.
     *
     * @param s       The String to convert.
     * @param doCache true if we should cache the resulting byte[] for use
     *                    on subsequent calls with the same string s.
     *
     * @return A byte[] containing the String bytes in EBCDIC form, with a
     *         null-term at the end.
     *
     * @throws RuntimeException for any UnsupportedEncodingExceptions.
     */
    public static byte[] convertToEBCDIC(String s, boolean doCache) {
        return convertToEBCDICNoTrace(s, doCache);
    }

    /**
     * Ascii-to-Ebcdic conversion - SANS injected entry/exit tracing.
     *
     * @param s The String to convert.
     *
     * @return A byte[] containing the String bytes in EBCDIC form, with a
     *         null-term at the end.
     *
     * @throws RuntimeException for any UnsupportedEncodingExceptions.
     */
    @Trivial
    public static byte[] convertToEBCDICNoTrace(String s) {
        return convertToEBCDICNoTrace(s, true);
    }

    /**
     * Ascii-to-Ebcdic conversion - SANS injected entry/exit tracing.
     *
     * @param s       The String to convert.
     * @param doCache true if we should cache the resulting byte[] for use
     *                    on subsequent calls with the same string s.
     *
     * @return A byte[] containing the String bytes in EBCDIC form, with a
     *         null-term at the end.
     *
     * @throws RuntimeException for any UnsupportedEncodingExceptions.
     */
    @Trivial
    public static byte[] convertToEBCDICNoTrace(String s, boolean doCache) {
        if (s == null) {
            return null;
        }
        byte[] e = (doCache == true) ? a2eCache.get(s) : null;
        if (e == null) {
            try {
                e = (s + '\0').getBytes(EBCDIC);
                if (doCache == true) {
                    a2eCache.put(s, e);
                }
            } catch (UnsupportedEncodingException uee) {
                throw new RuntimeException("code page conversion error", uee);
            }
        }
        return e;
    }

    /**
     * Ebcdic-to-Ascii conversion.
     *
     * Convert a byte[] containing EBCDIC chars to an ASCII String.
     * Any null-term chars are stripped away.
     *
     * @param e A byte[] containing EBCDIC chars.
     *
     * @return A String containing the bytes in ASCII form, without any
     *         null-term chars.
     *
     * @throws RuntimeException for any UnsupportedEncodingExceptions.
     */
    public static String convertToASCII(byte[] e) {
        String a = null;
        try {
            if (e != null) {
                // Strip off all null-term chars from the end.
                int len = e.length;
                for (; len > 0 && e[len - 1] == '\0'; --len);
                a = new String(e, 0, len, EBCDIC);
            }
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("code page conversion error", uee);
        }
        return a;
    }

    /**
     * Ebcdic-to-Ascii conversion for a list of byte[].
     *
     * For a List<byte[]> containing multiple byte[] of EBCDIC
     * chars, convert each byte[] to an ASCII String.
     *
     * @param e A List<byte[]> containing multiple byte[] of EBCDIC chars.
     *
     * @return A String containing the bytes in ASCII form, without any
     *         null-term chars.
     *
     * @throws RuntimeException for any UnsupportedEncodingExceptions.
     */
    public static List<String> convertToASCII(List<byte[]> e) {
        List<String> a = new ArrayList<String>();
        if (e != null) {
            for (byte[] egroup : e) {
                a.add(NativeMethodUtils.convertToASCII(egroup));
            }
        }
        return a;
    }

}
