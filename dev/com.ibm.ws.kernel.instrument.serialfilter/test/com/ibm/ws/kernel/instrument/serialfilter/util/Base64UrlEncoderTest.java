/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.util;

import org.junit.Test;
import test.util.Conversion;

import java.nio.charset.Charset;

import static org.junit.Assert.*;

public class Base64UrlEncoderTest {
    private static final Encoder ENCODER = Base64UrlEncoder.URL_AND_FILENAME_SAFE_ENCODING;

    @Test
    public void testEmptyByteArray() {
        assertEquals("", ENCODER.encode(Conversion.fromOctal("")));}

    @Test
    public void testSingleByte() {
        assertEquals("AA==", ENCODER.encode(Conversion.fromOctal("0")));
        assertEquals("BA==", ENCODER.encode(Conversion.fromOctal("01")));
        assertEquals("_A==", ENCODER.encode(Conversion.fromOctal("77")));
        for (int i = 0; i < 0x100; i++) {
            byte[] buffer = {(byte)i};
            char c1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".charAt(i>>2);
            char c2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".charAt((i & 0x03)<<4);
            assertEquals("Input buffer = " + Conversion.toBinary(buffer), "" + c1 + c2 + "==", ENCODER.encode(buffer));
        }
    }

    @Test
    public void testDoubleByte() {
        assertEquals("AAA=", ENCODER.encode(Conversion.fromOctal("00 00 0")));
        assertEquals("_BA=", ENCODER.encode(Conversion.fromOctal("77 01 0")));
        assertEquals("__4=", ENCODER.encode(Conversion.fromOctal("77 77 7")));
        for (int i = 0; i < 0x100; i++) {
            byte[] buffer = {0, (byte)i};
            char c1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".charAt(i>>4);
            char c2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".charAt((i & 0x0F)<<2);
            assertEquals("Input buffer = " + Conversion.toBinary(buffer), "A" + c1 + c2 + "=", ENCODER.encode(buffer));
        }
    }

    @Test
    public void testTripleByte() {
        for (int i = 0; i < 0x40; i++) {
            byte[] buffer1 = {(byte) (i<<2), 0 , 0};
            byte[] buffer2 = {(byte) (i>>4), (byte) (i<<4), 0};
            byte[] buffer3 = {0, (byte) (i>>2), (byte) (i<<6)};
            byte[] buffer4 = {0, 0, (byte) i};
            char c = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".charAt(i);
            assertEquals("Input buffer = " + Conversion.toBinary(buffer1), "" + c + "AAA", ENCODER.encode(buffer1));
            assertEquals("Input buffer = " + Conversion.toBinary(buffer2), "A" + c + "AA", ENCODER.encode(buffer2));
            assertEquals("Input buffer = " + Conversion.toBinary(buffer3), "AA" + c + "A", ENCODER.encode(buffer3));
            assertEquals("Input buffer = " + Conversion.toBinary(buffer4), "AAA" + c + "", ENCODER.encode(buffer4));
        }
    }

    @Test
    public void testLeviathan() {
        // Wikipedia article on Base64 uses the following quote from John Hobbes
        assertBase64EncodingForUtf8String(
                "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz" +
                "IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg" +
                "dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu" +
                "dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo" +
                "ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4="
                ,
                "Man is distinguished, not only by his reason, but by this singular passion from " +
                "other animals, which is a lust of the mind, that by a perseverance of delight " +
                "in the continued and indefatigable generation of knowledge, exceeds the short " +
                "vehemence of any carnal pleasure."
        );
    }

    @Test
    public void testTruncation() {
        // again recommended by Wikipedia
        assertBase64EncodingForUtf8String("cGxlYXN1cmUu", "pleasure.");
        assertBase64EncodingForUtf8String("bGVhc3VyZS4=", "leasure." );
        assertBase64EncodingForUtf8String("ZWFzdXJlLg==", "easure."  );
        assertBase64EncodingForUtf8String("YXN1cmUu",     "asure."   );
        assertBase64EncodingForUtf8String("c3VyZS4=",     "sure."    );
    }

    private static void assertBase64EncodingForUtf8String(String expected, String input) {
        byte[] buffer = input.getBytes(Charset.forName("UTF8"));
        assertEquals(expected, ENCODER.encode(buffer));
        assertTrue(Base64UrlEncoder.isEncodedString(expected));
        assertFalse(Base64UrlEncoder.isEncodedString(input));
    }
}
