/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 * Test class to exercise the functionality of the HashUtils class
 */
public class HashUtilsTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule outputRule = outputMgr;

    @Test
    public void testDigestNormal() {
        String methodName = "testDigestNormal";
        String input = "ThisIsASampleString.";
        String output = "pmrF0bjTQv2LAS+xhZVeNTn4V0tLOJq+VbXnUX93k/4=";

        try {
            assertEquals(output, HashUtils.digest(input));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDigestNull() {
        String methodName = "testDigestNull";

        try {
            assertNull(HashUtils.digest(null));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDigestEmpty() {
        String methodName = "testDigestEmpty";

        try {
            assertNull(HashUtils.digest(""));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDigestInvalidAlgorithm() {
        String methodName = "testDigestInvalidAlgorithm";
        String input = "ThisIsASampleString.";
        String invalidAlgorithm = "notExist";
        String message = "Exception instanciating MessageDigest :";
        try {
            HashUtils.digest(input, invalidAlgorithm);
            fail("An exception should be caught");
        } catch (RuntimeException re) {
            // this is normal.
            assertTrue(re.getMessage().startsWith(message));
            return;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDigestInvalidEncoding() {
        String methodName = "testDigestInvalidEncoding";
        String input = "ThisIsASampleString.";
        String invalidCharset = "UTF-3";
        String message = "Exception converting String object :";

        try {
            HashUtils.digest(input, "SHA-256", invalidCharset);
            fail("An exception should be caught");
        } catch (RuntimeException re) {
            // this is normal.
            assertTrue(re.getMessage().startsWith(message));
            return;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
