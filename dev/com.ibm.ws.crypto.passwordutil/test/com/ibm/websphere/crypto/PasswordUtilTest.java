/*******************************************************************************
 * Copyright (c) 2009, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.Test;

import test.common.SharedOutputManager;

/**
 * Tests for the password utility class.
 */
public class PasswordUtilTest {
    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    //@BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    //@AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    //@After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    /**
     * Test the utility methods.
     */
    @Test
    public void testUtil() {
        try {
            assertNull(PasswordUtil.getCryptoAlgorithm(null));
            assertNull(PasswordUtil.getCryptoAlgorithm(""));
            assertNull(PasswordUtil.getCryptoAlgorithm("{xor"));
            assertEquals("xor", PasswordUtil.getCryptoAlgorithm("{xor}CDo9Hgw="));

            assertNull(PasswordUtil.getCryptoAlgorithmTag(null));
            assertNull(PasswordUtil.getCryptoAlgorithmTag(""));
            assertNull(PasswordUtil.getCryptoAlgorithmTag("{xor"));
            assertEquals("{xor}", PasswordUtil.getCryptoAlgorithmTag("{xor}CDo9Hgw="));
            assertEquals("{xor}", PasswordUtil.getCryptoAlgorithmTag("{xor}"));

            assertTrue(PasswordUtil.isEncrypted("{xor}CDo9Hgw="));
            assertFalse(PasswordUtil.isEncrypted("CDo9Hgw="));

            assertFalse(PasswordUtil.isValidCryptoAlgorithm(null));
            assertTrue(PasswordUtil.isValidCryptoAlgorithm(""));
            assertTrue(PasswordUtil.isValidCryptoAlgorithm("xor"));

            assertFalse(PasswordUtil.isValidCryptoAlgorithmTag(null));
            assertFalse(PasswordUtil.isValidCryptoAlgorithmTag(""));
            assertFalse(PasswordUtil.isValidCryptoAlgorithmTag("xor"));
            assertTrue(PasswordUtil.isValidCryptoAlgorithmTag("{xor}"));

            assertEquals("WebAS", PasswordUtil.decode("{}WebAS"));
            try {
                PasswordUtil.decode("WebAS");
                fail();
            } catch (InvalidPasswordDecodingException e) {
                // expected exception
            }
            assertEquals("WebAS", PasswordUtil.decode("{xor}CDo9Hgw="));

            assertEquals("{xor}Lz4sLCgwLTsINis3exYxFis=", PasswordUtil.encode("passwordWith$InIt"));
            assertEquals("passwordWith$InIt", PasswordUtil.decode("{xor}Lz4sLCgwLTsINis3exYxFis="));

            try {
                PasswordUtil.encode(null);
                fail();
            } catch (InvalidPasswordEncodingException e) {
                // expected exception
            }
            try {
                PasswordUtil.encode("{xor}CDo9Hgw=");
                fail("We should not encode already encoded passwords");
            } catch (InvalidPasswordEncodingException e) {
                // expected exception
            }

            assertNull(PasswordUtil.removeCryptoAlgorithmTag(null));
            assertNull(PasswordUtil.removeCryptoAlgorithmTag(""));
            assertEquals("test", PasswordUtil.removeCryptoAlgorithmTag("{xor}test"));
            assertEquals("", PasswordUtil.removeCryptoAlgorithmTag("{xor}"));
            assertNull(PasswordUtil.passwordEncode("{test}teststring{/test}", "aes"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable("testUtil", t);
        }
    }

    @Test
    public void testAESEncoding() throws Exception {
        String encoding = PasswordUtil.encode("WebAS", "aes");
        assertTrue("The encoded password should start with {aes} " + encoding, encoding.startsWith("{aes}"));
        String encoding2 = PasswordUtil.encode("WebAS", "aes");
        assertFalse("Encoding the same password twice should result in different encodings: " + encoding + " and " + encoding2, encoding.equals(encoding2));

        assertEquals("The password was not decoded correctly", "WebAS", PasswordUtil.decode(encoding));
        assertEquals("The password was not decoded correctly", "WebAS", PasswordUtil.decode(encoding2));
        assertEquals("The password was not decoded correctly", "WebAS", PasswordUtil.decode("{aes}AGTpzRDW//VE3Jshg1fd89rxw/JMjHfFM9UdYdVNIUt2"));

        assertEquals("Did not decode password encoded with AES_V0 (AES-128) encoded password", "alternatepwd",
                     PasswordUtil.decode("{aes}AEmVKa+jOeA7pos+sSfpHNmH1MVfwg8ZoV29iDi6I0ZGcov6hSZsAxMhFr91jTSBYQ=="));
        assertEquals("Did not decode password encoded with AES_V1 (AES-256) encoded password", "alternatepwd",
                     PasswordUtil.decode("{aes}ARABGAM7S4HrIRtZWJ229TnxuKZrrPN3dsKrrQzCQE/3U5F4zp3UrDQ+Czmnvz1kaQyN7JktDzieJxelwu077ZYET2V+7/1Gi37iztr7lY0i+j4dlHOFIi5PESnZ7V8XOmdSbH9DSgkuJaXNoEqb"));
    }

    @Test
    public void testHashEncoding() throws Exception {
        try {
            PasswordUtil.decode("{hash}ATAAAAAEc2FsdEAAAAAgCvy54cIa4spWU/H9c8WIjTIK/peZ2xH6E7UWMWuz/Qc=");
            fail();
        } catch (InvalidPasswordDecodingException e) {
            // expected exception
        }
        String encoding = PasswordUtil.encode("WebAS", "hash");
        assertTrue("The encoded password should start with {hash} " + encoding, encoding.startsWith("{hash}"));
        String encoding2 = PasswordUtil.encode("WebAS", "hash");
        assertFalse("Encoding the same password twice should result in different encodings: " + encoding + " and " + encoding2, encoding.equals(encoding2));
    }

    @Test
    public void testNoTrim() throws Exception {
        String WITH_SPACE = "  WebAS   ";
        String TRIMMED = "{xor}CDo9Hgw=";
        String NO_TRIM = "{xor}f38IOj0eDH9/fw==";
        assertEquals(PasswordUtil.encode(WITH_SPACE, "xor"), TRIMMED);

        HashMap<String, String> props = new HashMap<String, String>();
        assertEquals(PasswordUtil.encode(WITH_SPACE, "xor", props), TRIMMED);

        props.put(PasswordUtil.PROPERTY_NO_TRIM, "true");
        assertEquals(PasswordUtil.encode(WITH_SPACE, "xor", props), NO_TRIM);
        try {
            assertEquals(WITH_SPACE, PasswordUtil.decode(NO_TRIM));
        } catch (InvalidPasswordDecodingException e) {
            fail();
        }

    }
}
