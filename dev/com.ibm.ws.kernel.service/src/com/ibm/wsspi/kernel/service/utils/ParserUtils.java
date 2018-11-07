/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import com.ibm.ws.kernel.boot.classloader.URLEncodingUtils;

public class ParserUtils {

    /**
     * Encodes a URL path string. This method is suitable only for URL path
     * strings and is unsuitable for other URL components.
     *
     * @param s the string to encode
     * @return the encoded string
     */
    public static String encode(String s) {
        return URLEncodingUtils.encode(s);
    }

    /**
     * Decodes a URL-encoded path string. For example, an encoded
     * space (%20) is decoded into a normal space (' ') character.
     *
     * @param String encoded - the encoded URL string
     * @return String decoded - the decoded string.
     */
    public static String decode(String s) {
        return URLEncodingUtils.decode(s);
    }

/*
 * public static void main(String[] args) {
 * testEncodeDecode();
 * System.out.println();
 * testDecode();
 * }
 *
 * private static void testEncodeDecode() {
 * System.out.println("decode(encode()):");
 *
 * if (encode(null) == null) {
 * System.out.println("pass:    in=null");
 * } else {
 * System.out.println("FAIL:    in=null");
 * }
 *
 * String identity = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()/;:@&=+$,";
 * testEncodeDecode(identity, identity);
 * testEncodeDecode(" %", "%20%25");
 * testEncodeDecode("xX yY zZ", "xX%20yY%20zZ");
 * testEncodeDecode("\uffff", "%ef%bf%bf");
 * }
 *
 * private static void testEncodeDecode(String in, String expected) {
 * String out = encode(in);
 * if (out.equals(expected)) {
 * System.out.println("pass:    in=\"" + in + "\"");
 *
 * String out2 = decode(out);
 * if (out2.equals(in)) {
 * System.out.println("pass2:   in=\"" + out + "\"");
 * } else {
 * System.out.println("FAIL2:   in=\"" + out + "\", out=\"" + out2 + "\"");
 * }
 * } else {
 * System.out.println("FAIL:    in=\"" + in + "\"");
 * }
 * }
 *
 * private static void testDecode() {
 * System.out.println("decode():");
 *
 * if (decode(null) == null) {
 * System.out.println("pass:    in=null");
 * } else {
 * System.out.println("FAIL:    in=null");
 * }
 *
 * testDecode("not encoded", "not encoded");
 *
 * // Test basic decoding.
 * testDecode("%20", " ");
 * testDecode("%20%20", "  ");
 * testDecode("xX%20yY%20zZ", "xX yY zZ");
 *
 * // Invalid decoding.
 * testDecode("%", null);
 * testDecode("%0", null);
 * testDecode("%g0", null);
 *
 * // %80 is an invalid first byte for UTF-8.
 * testDecode("%80", null);
 * testDecode("%80x", null);
 * testDecode("%80%00", null);
 *
 * // Special null encoding for modified UTF-8.
 * testDecode("%c0%80", "\u0000");
 *
 * // Invalid overlong UTF-8, valid modified UTF-8.
 * testDecode("%c0%25", "%");
 * testDecode("%c0%a5", "%");
 * testDecode("%c0%e5", "%");
 *
 * // Invalid 2-byte encodings.
 * testDecode("%c0", null);
 * testDecode("%c000", null);
 * testDecode("%c0%", null);
 * testDecode("%c0%0", null);
 *
 * // Invalid overlong UTF-8, valid modified UTF-8.
 * testDecode("%e0%00%25", "%");
 * testDecode("%e0%80%25", "%");
 * testDecode("%e0%c0%25", "%");
 * testDecode("%e0%00%a5", "%");
 * testDecode("%e0%00%e5", "%");
 *
 * // Invalid 3-byte encodings.
 * testDecode("%e0", null);
 * testDecode("%e0%00", null);
 * testDecode("%e0%0000", null);
 * testDecode("%e0%0%00", null);
 * testDecode("%e0%00%", null);
 * testDecode("%e0%00%0", null);
 * }
 *
 * private static void testDecode(String in, String expected) {
 * try {
 * String out = decode(in);
 * if (out.equals(expected)) {
 * System.out.println("pass:    in=\"" + in + "\"");
 * } else {
 * System.out.println("FAIL:    in=\"" + in + "\", out=\"" + out + "\"");
 * }
 * } catch (RuntimeException ex) {
 * if (expected == null) {
 * System.out.println("pass:    in=\"" + in + "\", " + ex);
 * } else {
 * System.out.println("ERROR:   in=\"" + in + "\"");
 * ex.printStackTrace();
 * }
 * }
 *
 * try {
 * // Using URL.openStream() triggers decoding.
 * new java.net.URL("file:/doesnotexist/" + in).openStream();
 * } catch (java.io.FileNotFoundException ex) {
 * if (expected == null) {
 * System.out.println("FAIL2:   in=\"" + in + "\", " + ex);
 * ex.printStackTrace();
 * } else {
 * System.out.println("pass2:   in=\"" + in + "\"");
 * }
 * } catch (Exception ex) {
 * if (expected == null) {
 * System.out.println("pass2:   in=\"" + in + "\", " + ex);
 * } else {
 * System.out.println("ERROR2:  in=\"" + in + "\"");
 * ex.printStackTrace();
 * }
 * }
 * }
 */
}