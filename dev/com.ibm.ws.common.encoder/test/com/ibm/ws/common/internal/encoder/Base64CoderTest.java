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
package com.ibm.ws.common.internal.encoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class Base64CoderTest {
    private final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(byte[])}.
     */
    @Test
    public void base64DecodeByteArray_null() {
        assertNull("Null String input should return null",
                   Base64Coder.base64Decode((byte[]) null));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(byte[])}.
     */
    @Test
    public void base64DecodeByteArray_zeroSizeArray() {
        assertNull("Null String input should return null",
                   Base64Coder.base64Decode(new byte[] {}));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(byte[])}.
     */
    @Test
    public void base64DecodeByteArray_1SizeArray() {
        assertNull("Null String input should return null",
                   Base64Coder.base64Decode(new byte[] { 1 }));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(byte[])}.
     */
    @Test
    public void base64DecodeByteArray_2SizeArray() {
        assertNull("Null String input should return null",
                   Base64Coder.base64Decode(new byte[] { 1, 2 }));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(byte[])}.
     */
    @Test
    public void base64DecodeByteArray_3SizeArray() {
        assertNull("Null String input should return null",
                   Base64Coder.base64Decode(new byte[] { 1, 2, 3 }));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(byte[])}.
     */
    @Test
    public void base64DecodeByteArray_4SizeArray() {
        assertNotNull("A byte array of size 4 is valid",
                      Base64Coder.base64Decode(new byte[] { 1, 2, 3, 4 }));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_null() {
        assertNull("Null String input should return null",
                   Base64Coder.base64Decode((String) null));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_1Length() {
        assertNull("An insufficiently long string is invalid and will result in null",
                   Base64Coder.base64Decode("1"));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_2Length() {
        assertNull("An insufficiently long string is invalid and will result in null",
                   Base64Coder.base64Decode("12"));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_3Length() {
        assertNull("An insufficiently long string is invalid and will result in null",
                   Base64Coder.base64Decode("123"));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_4Length() {
        assertNotNull("Strings that are multiple of 4 long are valid",
                      Base64Coder.base64Decode("1234"));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_5Length() {
        assertNull("Strings that are not a multiple of 4 long is invalid",
                   Base64Coder.base64Decode("12345"));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_6Length() {
        assertNull("Strings that are not a multiple of 4 long is invalid",
                   Base64Coder.base64Decode("123456"));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_7Length() {
        assertNull("Strings that are not a multiple of 4 long is invalid",
                   Base64Coder.base64Decode("1234567"));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_8Length() {
        assertNotNull("Strings that are multiple of 4 long are valid",
                      Base64Coder.base64Decode("12345678"));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Decode(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_longerString() {
        Base64Coder.base64Decode("invalidToken");
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#toString(byte[])}.
     */
    @Test
    public void toStringByteArray_null() {
        assertNull("Null begets null", Base64Coder.toString(null));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#toString(byte[])}.
     */
    @Test
    public void toStringByteArray_emptyArray() {
        assertEquals("An empty byte array is an empty String",
                     "", Base64Coder.toString(new byte[] {}));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#toString(byte[])}.
     */
    @Test
    public void toStringByteArray_validArray() {
        String expected = "someChars";
        byte[] array = expected.getBytes();
        assertEquals("Should be the same as the expected String",
                     expected, Base64Coder.toString(array));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Encode(byte[])}.
     */
    @Test
    public void base64Encode_nullBytes() {
        assertNull("Null byte input should return null",
                   Base64Coder.base64Encode((byte[]) null));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Encode(byte[])}.
     */
    @Test
    public void base64Encode_nullString() {
        assertNull("Null String input should return null",
                   Base64Coder.base64Encode((String) null));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Encode(byte[])}.
     */
    @Test
    public void base64Encode_emptyArray() {
        byte[] bytes = new byte[] {};
        assertEquals("Empty byte array should yield an empty encoded byte array",
                     0, Base64Coder.base64Encode(bytes).length);
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Encode(byte[])}.
     */
    @Test
    public void base64Encode_zeroValueSingleByteArray() {
        byte[] bytes = new byte[] { 0 };
        assertEquals("The smallest encoding is length 4",
                     4, Base64Coder.base64Encode(bytes).length);
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Encode(byte[])}.
     */
    @Test
    public void base64Encode_singleCharString() {
        byte[] bytes = "0".getBytes();
        assertEquals("The smallest encoding is length 4",
                     4, Base64Coder.base64Encode(bytes).length);
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Encode(byte[])}.
     */
    @Test
    public void base64Encode_twoCharString() {
        byte[] bytes = "zz".getBytes();
        assertEquals("The smallest encoding is length 4",
                     4, Base64Coder.base64Encode(bytes).length);
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Encode(byte[])}.
     */
    @Test
    public void base64Encode_longString() {
        byte[] bytes = "4TUZR3aU8II+cWveDgIB7ffDQZaKxx1VKlUBW7KsLa2AQjiB6RWBoNuoH+OIUbtntMIsS2956ZvdzSshAuPNuk7y30BhN00WclWtMY6AD7je2aecQxsGNrV/ogCAOip9EobBue4N1zU8S7yD1jEajykfN8Eo2rIqnMK/DraTV65gmlE378VS3Wy6IFHmZm9BBlaSNqPLBkyJ1Xh98PACMr8f/bF290AD75nGrrB0oXODaeoA85/hpiHpvxSNCFx+P3QDvRly5Bb16SQRhHmUhX0uegAdURKAaeX3gmu8zXQ=".getBytes();
        assertTrue("The size of the encoding is always a multiple of 4 bytes",
                   (Base64Coder.base64Encode(bytes).length % 4) == 0);
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64DecodeString(java.lang.String)}.
     */
    @Test
    public void base64DecodeString_nullString() {
        assertNull("Null String input should return null",
                   Base64Coder.base64DecodeString((String) null));
    }

    @Test
    public void base64DecodeStringMethod_validString() {
        assertNotNull("Strings that are multiple of 4 long are valid",
                      Base64Coder.base64DecodeString("12345678"));
    }

    /**
     * Test method for {@link com.ibm.ws.common.internal.encoder.Base64Coder#base64Encode(byte[])}.
     */
    @Test
    public void base64EncodeToString_longString() {
        byte [] bytes = "4TUZR3aU8II+cWveDgIB7ffDQZaKxx1VKlUBW7KsLa2AQjiB6RWBoNuoH+OIUbtntMIsS2956ZvdzSshAuPNuk7y30BhN00WclWtMY6AD7je2aecQxsGNrV/ogCAOip9EobBue4N1zU8S7yD1jEajykfN8Eo2rIqnMK/DraTV65gmlE378VS3Wy6IFHmZm9BBlaSNqPLBkyJ1Xh98PACMr8f/bF290AD75nGrrB0oXODaeoA85/hpiHpvxSNCFx+P3QDvRly5Bb16SQRhHmUhX0uegAdURKAaeX3gmu8zXQ=".getBytes();
        assertTrue("The size of the encoding is always a multiple of 4 bytes",
                   (Base64Coder.base64EncodeToString(bytes).length() % 4) == 0);
    }

}