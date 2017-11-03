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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.security.enterprise.identitystore.Pbkdf2PasswordHash;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;

public class Pbkdf2PasswordHashImpl implements Pbkdf2PasswordHash {

    private static final TraceComponent tc = Tr.register(Pbkdf2PasswordHashImpl.class);

    /*
     * supported parameters.
     */
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

    private static final List<String> SUPPORTED_ALGORITHMS = Arrays.asList("PBKDF2WithHmacSHA224", "PBKDF2WithHmacSHA256", "PBKDF2WithHmacSHA384", "PBKDF2WithHmacSHA512");

    private int generateAlgorithm = DEFAULT_ALGORITHM; //the current algorithm (specified by as the index of SUPPORTED_ALGORITHMS)
    private int generateIterations = DEFAULT_ITERATIONS;
    private int generateSaltSize = DEFAULT_SALTSIZE;
    private int generateKeySize = DEFAULT_KEYSIZE;

    /*
     * (non-Javadoc)
     */
    @Override
    public void initialize(Map<String, String> params) {
        parseParams(params);
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public String generate(@Sensitive char[] password) throws RuntimeException {
        byte[] salt = generateSalt(generateSaltSize);
        byte[] outputBytes = generate(SUPPORTED_ALGORITHMS.get(generateAlgorithm), generateIterations, generateKeySize, salt, password);
        return format(SUPPORTED_ALGORITHMS.get(generateAlgorithm), generateIterations, salt, outputBytes);
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public boolean verify(char[] password, String hashedPassword) throws RuntimeException {
        String[] items = parseData(hashedPassword);
        byte[] originalHash = Base64Coder.base64DecodeString(items[3]);
        byte[] salt = Base64Coder.base64DecodeString(items[2]);
        byte[] calculatedHash = generate(items[0], Integer.parseInt(items[1]), originalHash.length, salt, password);

        return Arrays.equals(originalHash, calculatedHash);
    }

    /**
     * Parse the data by colon.
     * Make sure that there are three colons, and algorithm is one of supported ones
     * and the 2nd param can be converted to the integer.
     * 
     * @param hashedPassword
     * @return
     */
    private String[] parseData(String hashedPassword) throws RuntimeException {
        // <algorithm>:<iterations>:<base64(salt)>:<base64(hash)>
        String[] items = hashedPassword.split(":");
        String msg = null;
        if (items.length == 4) {
            if (SUPPORTED_ALGORITHMS.contains(items[0])) {
                try {
                    Integer.parseInt(items[1]);
                    return items; // good.
                } catch (Exception e) {
                    msg = "invalid format";
                }
            } else {
                msg = "invalid algorithm";
            }
        }
        //TODO: make sure to translate the message
        msg = "invalid format";
        throw new RuntimeException(msg);
    }

    protected void parseParams(Map<String, String> params) {
        generateAlgorithm = indexOf(SUPPORTED_ALGORITHMS, params.get(PARAM_ALGORITHM), DEFAULT_ALGORITHM);
        generateIterations = parseInt(params.get(PARAM_ITERATIONS), DEFAULT_ITERATIONS, MINIMUM_ITERATIONS);
        generateSaltSize = parseInt(params.get(PARAM_SALTSIZE), DEFAULT_SALTSIZE, MINIMUM_SALTSIZE);
        generateKeySize = parseInt(params.get(PARAM_KEYSIZE), DEFAULT_KEYSIZE, MINIMUM_KEYSIZE);
    }

    private int indexOf(List<String> list, String value, int defaultIndex) {
        int output = defaultIndex;
        if (value != null) {
            int index = SUPPORTED_ALGORITHMS.indexOf(value);
            if (index >= 0) {
                output = index;
            }
        }
        return output;
    }

    private int parseInt(String value, int defaultValue, int minimumValue) {
        int output = defaultValue;
        if (value != null) {
            try {
                output = Integer.parseInt(value);
                if (output < minimumValue) {
                    // TODO: implement error message.
                    System.out.println("param error. less than minimum value, change it to the minimum");
//                    Tr.error(tc, );
                    output = minimumValue;
                }
            } catch (NumberFormatException e) {
                // TODO: implement error message.
                System.out.println("param error.");
//                Tr.error(tc, );
            }
        }
        return output;
    }

    private byte[] generateSalt(int size) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[size];
        random.nextBytes(salt);
        return salt;
    }

    // <algorithm>:<iterations>:<base64(salt)>:<base64(hash)>
    private String format(String algorithm, int iterations, byte[] salt, byte[] value) {
        final char COLON = ':';
        StringBuffer sb = new StringBuffer(algorithm);
        sb.append(COLON).append(iterations).append(COLON).append(Base64Coder.base64EncodeToString(salt)).append(COLON).append(Base64Coder.base64EncodeToString(value));
        return sb.toString();
    }

    public byte[] generate(String algorithm, int iterations, int keySize, byte[] salt, @Sensitive char[] password) throws RuntimeException {
        SecretKey secretKey;
        try {
            PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, keySize * 16);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
            secretKey = skf.generateSecret(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        return secretKey.getEncoded();
    }

}
