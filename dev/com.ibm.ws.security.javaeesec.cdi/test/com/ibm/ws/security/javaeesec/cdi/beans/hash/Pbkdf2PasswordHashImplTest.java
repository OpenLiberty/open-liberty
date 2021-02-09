/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.beans.hash;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.common.internal.encoder.Base64Coder;

import test.common.SharedOutputManager;

/**
 * Password hash implementation for testing.
 */
public class Pbkdf2PasswordHashImplTest {
    // these values were copied from Pbkdf2PasswordHashImpl class
    private static final String PARAM_ALGORITHM = "Pbkdf2PasswordHash.Algorithm"; // default "PBKDF2WithHmacSHA256"
    private static final String PARAM_ITERATIONS = "Pbkdf2PasswordHash.Iterations"; // default 2048, minimum 1024
    private static final String PARAM_SALTSIZE = "Pbkdf2PasswordHash.SaltSizeBytes"; // default 32, minimum 16
    private static final String PARAM_KEYSIZE = "Pbkdf2PasswordHash.KeySizeBytes"; // default 32, minimum 16

    private static final int DEFAULT_ALGORITHM = 1; // offset in SUPPORTED_ALGORITHMS
    private static final int DEFAULT_ITERATIONS = 2048;
    private static final int DEFAULT_SALTSIZE = 32;
    private static final int DEFAULT_KEYSIZE = 32;

    private static final int MINIMUM_ITERATIONS = 1024;
    private static final int MINIMUM_SALTSIZE = 16;
    private static final int MINIMUM_KEYSIZE = 16;

    private static final int INDEX_SHA256 = 1; // offset in SUPPORTED_ALGORITHMS
    private static final int INDEX_SHA512 = 3; // offset in SUPPORTED_ALGORITHMS

    private static final String SHA224 = "PBKDF2WithHmacSHA224";
    private static final String SHA256 = "PBKDF2WithHmacSHA256";
    private static final String SHA384 = "PBKDF2WithHmacSHA384";
    private static final String SHA512 = "PBKDF2WithHmacSHA512";

    // end of values.
//    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.javaeesec.*=all");
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
    }

    @Test
    public void testInitializeDefault() throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        assertTrue("Hash parameters are not set as the default.", verifyFields(pphi, DEFAULT_ALGORITHM, DEFAULT_ITERATIONS, DEFAULT_SALTSIZE, DEFAULT_KEYSIZE));
    }

    @Test
    public void testInitializeSetValues() throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ALGORITHM, SHA512);
        int ITERATIONS = 9999;
        int SALTSIZE = 128;
        int KEYSIZE = 512;
        params.put(PARAM_ITERATIONS, String.valueOf(ITERATIONS));
        params.put(PARAM_SALTSIZE, String.valueOf(SALTSIZE));
        params.put(PARAM_KEYSIZE, String.valueOf(KEYSIZE));
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        assertTrue("Hash parameters are not set as the specified.", verifyFields(pphi, INDEX_SHA512, ITERATIONS, SALTSIZE, KEYSIZE));
    }

    @Test
    public void testInitializeInvalidAlgorithm() throws Exception {

        String INVALID_ALGORITHM = "PBKDF2WethHmacSHA512";
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ALGORITHM, INVALID_ALGORITHM);
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        try {
            pphi.initialize(params);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("CWWKS1933E: is not logged, or the error string is not included in the message.",
                       outputMgr.checkForStandardErr("CWWKS1933E:.*" + PARAM_ALGORITHM + ".*" + INVALID_ALGORITHM + ".*"));
            assertTrue("CWWKS1933E: message is not set in the IllegalArgumentException.", re.getMessage().contains("CWWKS1933E:"));
        }
    }

    @Test
    public void testInitializeInvalidIterations() throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        String INVALID_ITERATIONS = "NotANumber";
        params.put(PARAM_ITERATIONS, INVALID_ITERATIONS);
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        try {
            pphi.initialize(params);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("CWWKS1933E: is not logged, or the error string is not included in the message.",
                       outputMgr.checkForStandardErr("CWWKS1933E:.*" + PARAM_ITERATIONS + ".*" + INVALID_ITERATIONS + ".*"));
            assertTrue("CWWKS1933E: message is not set in the IllegalArgumentException.", re.getMessage().contains("CWWKS1933E:"));
        }
    }

    @Test
    public void testInitializeInvalidSaltSize() throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        String INVALID_SALTSIZE = "NotANumber";
        params.put(PARAM_SALTSIZE, INVALID_SALTSIZE);
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        try {
            pphi.initialize(params);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("CWWKS1933E: is not logged, or the error string is not included in the message.",
                       outputMgr.checkForStandardErr("CWWKS1933E:.*" + PARAM_SALTSIZE + ".*" + INVALID_SALTSIZE + ".*"));
            assertTrue("CWWKS1933E: message is not set in the IllegalArgumentException.", re.getMessage().contains("CWWKS1933E:"));
        }
    }

    @Test
    public void testInitializeInvalidKeySize() throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        String INVALID_KEYSIZE = "NotANumber";
        params.put(PARAM_KEYSIZE, INVALID_KEYSIZE);
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        try {
            pphi.initialize(params);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("CWWKS1933E: is not logged, or the error string is not included in the message.",
                       outputMgr.checkForStandardErr("CWWKS1933E:.*" + PARAM_KEYSIZE + ".*" + INVALID_KEYSIZE + ".*"));
            assertTrue("CWWKS1933E: message is not set in the IllegalArgumentException.", re.getMessage().contains("CWWKS1933E:"));
        }
    }

    @Test
    public void testInitializeBelowMinimumIterations() throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        String INVALID_ITERATIONS = "3";
        params.put(PARAM_ITERATIONS, INVALID_ITERATIONS);
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        try {
            pphi.initialize(params);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            // no default number check since the the value is more than a thousand of which format might be different if the locale is other than English.
            assertTrue("CWWKS1934E: is not logged, or the error string is not included in the message.",
                       outputMgr.checkForStandardErr("CWWKS1934E:.*" + INVALID_ITERATIONS + ".*" + PARAM_ITERATIONS + ".*"));
            assertTrue("CWWKS1934E: message is not set in the IllegalArgumentException.", re.getMessage().contains("CWWKS1934E:"));
        }
    }

    @Test
    public void testInitializeBelowMinimumSaltSize() throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        String INVALID_SALTSIZE = "5";
        params.put(PARAM_SALTSIZE, INVALID_SALTSIZE);
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        try {
            pphi.initialize(params);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("CWWKS1934E: is not logged, or the error string is not included in the message.",
                       outputMgr.checkForStandardErr("CWWKS1934E:.*" + INVALID_SALTSIZE + ".*" + PARAM_SALTSIZE + ".*" + MINIMUM_SALTSIZE + ".*"));
            assertTrue("CWWKS1934E: message is not set in the IllegalArgumentException.", re.getMessage().contains("CWWKS1934E:"));
        }
    }

    @Test
    public void testInitializeBelowMinimumKeySize() throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        String INVALID_KEYSIZE = "8";
        params.put(PARAM_KEYSIZE, INVALID_KEYSIZE);
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        try {
            pphi.initialize(params);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("CWWKS1934E: is not logged, or the error string is not included in the message.",
                       outputMgr.checkForStandardErr("CWWKS1934E:.*" + INVALID_KEYSIZE + ".*" + PARAM_KEYSIZE + ".*" + MINIMUM_KEYSIZE + ".*"));
            assertTrue("CWWKS1934E: message is not set in the IllegalArgumentException.", re.getMessage().contains("CWWKS1934E:"));
        }
    }

    @Test
    public void testGenerateVerifyDefault() throws Exception {
        final String PASSWORD = "testpassword";
        final char[] password = PASSWORD.toCharArray();
        Map<String, String> params = new HashMap<String, String>();
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        String output = pphi.generate(password);
        System.out.println("hashed value : " + output);
        assertTrue("The output should start with PBKDF2WithHmacSHA256:2048:", output.startsWith(SHA256 + ":" + DEFAULT_ITERATIONS + ":"));
        assertTrue("The key length does not match.", checkKeySize(DEFAULT_KEYSIZE, output));
        assertTrue("The salt length does not match.", checkSaltSize(DEFAULT_SALTSIZE, output));
        assertTrue("Hashed value should match", pphi.verify(password, output));
    }

    @Test
    public void testGenerateVerifySpecifiedMinimum() throws Exception {
        final String PASSWORD = "testpassword";
        final char[] password = PASSWORD.toCharArray();
        Map<String, String> params = new HashMap<String, String>();
        params.put(PARAM_ALGORITHM, SHA224);
        params.put(PARAM_ITERATIONS, String.valueOf(MINIMUM_ITERATIONS));
        params.put(PARAM_SALTSIZE, String.valueOf(MINIMUM_SALTSIZE));
        params.put(PARAM_KEYSIZE, String.valueOf(MINIMUM_KEYSIZE));
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        String output = pphi.generate(password);
        System.out.println("hashed value : " + output);
        assertTrue("The output should start with PBKDF2WithHmacSHA224:1024:", output.startsWith(SHA224 + ":" + MINIMUM_ITERATIONS + ":"));
        assertTrue("The key length does not match.", checkKeySize(MINIMUM_KEYSIZE, output));
        assertTrue("The salt length does not match.", checkSaltSize(MINIMUM_SALTSIZE, output));
        assertTrue("Hashed value should match", pphi.verify(password, output));
    }

    @Test
    public void testVerifyInvalidPassword() throws Exception {
        final String PASSWORD = "differentpassword";
        final char[] password = PASSWORD.toCharArray();
        final String VALID_VALUE = "PBKDF2WithHmacSHA256:2048:MufIaevF+xX4jMOz+qd8+bSUBMGhBQhV/Rq8oCUZ39k=:Hh7Bw6oW9iIFm5UKt/Fqm5yvSEPCsyUx";
        Map<String, String> params = new HashMap<String, String>();
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        assertFalse("Hashed value should not match", pphi.verify(password, VALID_VALUE));
    }

    @Test
    public void testVerifyInvalidFormat() throws Exception {
        final String PASSWORD = "testpassword";
        final char[] password = PASSWORD.toCharArray();
        final String INVALID_VALUE = "MufIaevF+xX4jMOz+qd8+bSUBMGhBQhV/Rq8oCUZ39k=:Hh7Bw6oW9iIFm5UKt/Fqm5yvSEPCsyUx";
        Map<String, String> params = new HashMap<String, String>();
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        try {
            pphi.verify(password, INVALID_VALUE);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("CWWKS1935E message is not set in IllegalArgumentException.", re.getMessage().contains("CWWKS1935E"));
        }
    }

    @Test
    public void testVerifyInvalidAlgorithm() throws Exception {
        final String PASSWORD = "testpassword";
        final char[] password = PASSWORD.toCharArray();
        final String INVALID_VALUE = "invalid:2048:MufIaevF+xX4jMOz+qd8+bSUBMGhBQhV/Rq8oCUZ39k=:Hh7Bw6oW9iIFm5UKt/Fqm5yvSEPCsyUx";
        Map<String, String> params = new HashMap<String, String>();
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        try {
            pphi.verify(password, INVALID_VALUE);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("CWWKS1935E message is not set in IllegalArgumentException.", re.getMessage().contains("CWWKS1935E"));
        }
    }

    @Test
    public void testVerifyInvalidIterations() throws Exception {
        final String PASSWORD = "testpassword";
        final char[] password = PASSWORD.toCharArray();
        final String INVALID_VALUE = "PBKDF2WithHmacSHA256:8A32G:MufIaevF+xX4jMOz+qd8+bSUBMGhBQhV/Rq8oCUZ39k=:Hh7Bw6oW9iIFm5UKt/Fqm5yvSEPCsyUx";
        Map<String, String> params = new HashMap<String, String>();
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        try {
            pphi.verify(password, INVALID_VALUE);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("CWWKS1935E message is not set in IllegalArgumentException.", re.getMessage().contains("CWWKS1935E"));
        }
    }

    @Test
    public void testVerifyInvalidSalt() throws Exception {
        final String PASSWORD = "testpassword";
        final char[] password = PASSWORD.toCharArray();
        final String INVALID_VALUE = "PBKDF2WithHmacSHA256:2048:MufIabrokenz+qd8+bS[]{}UBMGhBQhV/Rq8oCUZ39:Hh7Bw6oW9iIFm5UKt/Fqm5yvSEPCsyUx";
        Map<String, String> params = new HashMap<String, String>();
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        try {
            pphi.verify(password, INVALID_VALUE);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("Invalid cause is not set in IllegalArgumentException.", re.getMessage().contains("salt"));
        }
    }

    @Test
    public void testVerifyInvalidHash() throws Exception {
        final String PASSWORD = "testpassword";
        final char[] password = PASSWORD.toCharArray();
        final String INVALID_VALUE = "PBKDF2WithHmacSHA256:2048:vK49ahwrLEgd7R70BC+aYmuTVZc1askR+RfOvQsQZmE=:GCBujwAI2BASevN0Un0h5Ovl[])(+=_faaQxoU=";
        Map<String, String> params = new HashMap<String, String>();
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        try {
            pphi.verify(password, INVALID_VALUE);
            fail("A IllegalArgumentException should throw.");
        } catch (IllegalArgumentException re) {
            assertTrue("Invalid cause is not set in IllegalArgumentException.", re.getMessage().contains("hash"));
        }
    }

//-------------------- support methods ----------------------
    boolean verifyFields(Pbkdf2PasswordHashImpl pphi, int algorithm, int iterations, int saltSize, int keySize) {
        System.out.println("expected :algorithm : " + algorithm + ", iterations : " + iterations + ", saltSize : " + saltSize + ", keySize : " + keySize);
        System.out.println("actual   :algorithm : " + pphi.getAlgorithm() + ", iterations : " + pphi.getIterations() + ", saltSize : " + pphi.getSaltSize() + ", keySize : "
                           + pphi.getKeySize());

        return (pphi.getAlgorithm() == algorithm) && (pphi.getIterations() == iterations) && (pphi.getSaltSize() == saltSize) && (pphi.getKeySize() == keySize);
    }

    boolean checkKeySize(int keySize, String hashedValue) {
        return checkSize(keySize, hashedValue, 3);
    }

    boolean checkSaltSize(int keySize, String hashedValue) {
        return checkSize(keySize, hashedValue, 2);
    }

    boolean checkSize(int size, String hashedValue, int index) {
        String[] items = hashedValue.split(":");
        byte[] original = Base64Coder.base64DecodeString(items[index]);
        String name = "invalid";
        switch (index) {
            case 2:
                name = "salt";
                break;
            case 3:
                name = "key";
                break;
        }
        System.out.println(name + " length : " + original.length);
        return size == original.length;
    }
}
