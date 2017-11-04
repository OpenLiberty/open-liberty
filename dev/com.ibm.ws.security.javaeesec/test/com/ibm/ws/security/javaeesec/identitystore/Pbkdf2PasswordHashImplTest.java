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
package com.ibm.ws.security.javaeesec.identitystore;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ibm.ws.common.internal.encoder.Base64Coder;

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

    private static final int INDEX_SHA512 = 3; // offset in SUPPORTED_ALGORITHMS

    private static final String SHA224 = "PBKDF2WithHmacSHA224";
    private static final String SHA256 = "PBKDF2WithHmacSHA256";
    private static final String SHA384 = "PBKDF2WithHmacSHA384";
    private static final String SHA512 = "PBKDF2WithHmacSHA512";

    // end of values.

    @Test
    public void testInitializeDefault() throws Exception {

        Map<String, String> params = new HashMap<String, String>();
        Pbkdf2PasswordHashImpl pphi = new Pbkdf2PasswordHashImpl();
        pphi.initialize(params);
        assertTrue("Hash parameters is not set as the default.", verifyFields(pphi, DEFAULT_ALGORITHM, DEFAULT_ITERATIONS, DEFAULT_SALTSIZE, DEFAULT_KEYSIZE));
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
        assertTrue("Hash parameters is not set as the default.", verifyFields(pphi, INDEX_SHA512, ITERATIONS, SALTSIZE, KEYSIZE));
    }

    @Test
    public void testGenerateDefault() throws Exception {
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
    public void testGenerateSpecifiedMinimum() throws Exception {
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

// hashed value : PBKDF2WithHmacSHA256:2048:MufIaevF+xX4jMOz+qd8+bSUBMGhBQhV/Rq8oCUZ39k=:Hh7Bw6oW9iIFm5UKt/Fqm5yvSEPCsyUx
// hashed value : PBKDF2WithHmacSHA224:1024:G0wIfJ5Az2/x6VI8BIW82w==:rfeVxh+eJ16coXQmIzEMDQ==

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
