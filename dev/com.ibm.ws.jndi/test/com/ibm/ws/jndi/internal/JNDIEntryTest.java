/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jndi.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.websphere.crypto.PasswordUtil;

import test.common.SharedOutputManager;

public class JNDIEntryTest {

    private static final String DOUBLE_LITERAL = "1.1D";

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    /**
     * Test 1: value is encrypted, decode=true
     * Expected: value is decrypted and returned as a Double; CWWKN0011E is NOT logged.
     */
    @Test
    public void testEncryptedValue_DecodeTrue() throws Exception {
        double expected = Double.parseDouble(DOUBLE_LITERAL);
        String encrypted = PasswordUtil.encode(DOUBLE_LITERAL, "xor");

        Object result = JNDIEntry.parseLiteral(encrypted, true);

        assertEquals("Expected Double type", Double.class, result.getClass());
        assertEquals(expected, (Double) result, 0.0);
        assertFalse("CWWKN0011E should not be logged because decryption succeeded",
                outputMgr.checkForStandardErr("CWWKN0011E"));
    }

    /**
     * Test 2: value is NOT encrypted, decode=true
     * Expected: decryption step is skipped (value is not encrypted), parsed Double is returned;
     * CWWKN0011E is NOT logged.
     */
    @Test
    public void testUnencryptedValue_DecodeTrue() throws Exception {
        double expected = Double.parseDouble(DOUBLE_LITERAL);

        Object result = JNDIEntry.parseLiteral(DOUBLE_LITERAL, true);

        assertEquals("Expected Double type", Double.class, result.getClass());
        assertEquals(expected, (Double) result, 0.0);
        assertFalse("CWWKN0011E should not be logged when value is not encrypted",
                outputMgr.checkForStandardErr("CWWKN0011E"));
    }

    /**
     * Test 3: value is encrypted, decode=false
     * Expected: value is NOT decoded; the raw encrypted String is returned as-is;
     * CWWKN0011E is NOT logged.
     */
    @Test
    public void testEncryptedValue_DecodeFalse() throws Exception {
        String encrypted = PasswordUtil.encode(DOUBLE_LITERAL, "xor");

        Object result = JNDIEntry.parseLiteral(encrypted, false);

        assertEquals("Expected String type", String.class, result.getClass());
        assertEquals(encrypted, result);
        assertFalse("CWWKN0011E should not be logged when decode=false",
                outputMgr.checkForStandardErr("CWWKN0011E"));
    }

    /**
     * Test 4: value is NOT encrypted, decode=false
     * Expected: value is parsed normally and returned as a Double; CWWKN0011E is NOT logged.
     */
    @Test
    public void testUnencryptedValue_DecodeFalse() throws Exception {
        double expected = Double.parseDouble(DOUBLE_LITERAL);

        Object result = JNDIEntry.parseLiteral(DOUBLE_LITERAL, false);

        assertEquals("Expected Double type", Double.class, result.getClass());
        assertEquals(expected, (Double) result, 0.0);
        assertFalse("CWWKN0011E should not be logged when value is not encrypted",
                outputMgr.checkForStandardErr("CWWKN0011E"));
    }

    /**
     * Test 5: value appears encrypted but the payload is corrupt, decode=true
     * Expected: the raw (undecoded) String is returned as a fallback;
     * CWWKN0011E IS logged to signal the decryption failure.
     */
    @Test
    public void testEncryptedValue_DecodeTrue_DecodeFails() throws Exception {
        String corrupt = "{aes}notAesEncoded";

        Object result = JNDIEntry.parseLiteral(corrupt, true);

        assertEquals("Expected String type", String.class, result.getClass());
        assertEquals(corrupt, result);
        assertTrue("CWWKN0011E should be logged because decryption failed",
                outputMgr.checkForStandardErr("CWWKN0011E"));
    }
}