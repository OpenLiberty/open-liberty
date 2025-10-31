/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.test.api;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.BeforeClass;
import org.junit.Test;

public class HttpHeaderKeysTest {

    private static Class<?> clazz;
    private static Method validateHeaderName;

    @BeforeClass
    public static void reflect() throws Exception {
        clazz = Class.forName("com.ibm.wsspi.http.channel.values.HttpHeaderKeys");
        validateHeaderName = clazz.getDeclaredMethod("validateHeaderName", String.class, boolean.class);
        validateHeaderName.setAccessible(true);
    }

    private static Object callValidate(String name, boolean returnFalseForInvalid) throws Throwable {
        try {
            return validateHeaderName.invoke(null, name, returnFalseForInvalid);
        } catch (InvocationTargetException ite) {
            throw ite.getCause();
        }
    }

    /**
     * Ensures an invalid header name ending with a space throws {@link IllegalArgumentException},
     * preserves the original prefix, and includes new diagnostics (char, codes, pos, name).
     */
    @Test
    public void zeroMigration_testValidateHeaderName_originalAndNewDiagnostic() throws Throwable {
        String bad = "User-Agent "; // SPACE at end makes header name invalid
        try {
            callValidate(bad, false);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            String message = iae.getMessage();
            assertNotNull(message);

            // Legacy-compatible start:
            assertTrue("Message must start with original prefix",
                    message.startsWith("Header name contained an invalid character "));

            assertTrue("Should include offending char escape", message.contains("char=\\u0020(SPACE)"));
            assertTrue("Should include decimal code", message.contains("code=32("));
            assertTrue("Should include hex code", message.contains("(0x20)"));
            assertTrue("Should include 1-based position", message.contains(" pos="));
            assertTrue("Should include header name", message.contains("name=\"User-Agent \""));
        }
    }

    /**
     * Validates TAB within the header name is reported with the complete diagnostic format.
     */
    @Test
    public void invalidHeaderName_tabInName_hasCompactDetails() throws Throwable {
        String bad = "X\tBad";
        try {
            callValidate(bad, false);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            String message = iae.getMessage();
            assertTrue(message.startsWith("Header name contained an invalid character "));
            assertTrue(message.contains("char=\\u0009(TAB)"));
            assertTrue(message.contains("name=\"X\tBad\""));
        }
    }

    @Test
    public void returnFalseForInvalidName_suppressesException() throws Throwable {
        String bad = "Bad Name"; // SPACE invalid
        Object result = callValidate(bad, true);
        assertEquals("Expected boolean false when returnFalseForInvalidName=true", Boolean.FALSE, result);
    }

    @Test
    public void validHeaderName_withDigitPasses() throws Throwable {
        Object result = callValidate("ValidHeaderName6", false);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void isValidTchar_acceptsDigitAndRejectsSpace() throws Exception {
        Method isValidTchar = clazz.getDeclaredMethod("isValidTchar", char.class);
        assertEquals(Boolean.TRUE, isValidTchar.invoke(null, '6'));
        assertEquals(Boolean.FALSE, isValidTchar.invoke(null, ' '));
    }

}
