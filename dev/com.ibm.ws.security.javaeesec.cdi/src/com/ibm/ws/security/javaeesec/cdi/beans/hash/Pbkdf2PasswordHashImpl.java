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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.security.enterprise.identitystore.Pbkdf2PasswordHash;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;

@Default
@Dependent
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
    public String generate(@Sensitive char[] password) {
        byte[] salt = generateSalt(generateSaltSize);
        byte[] outputBytes = generate(SUPPORTED_ALGORITHMS.get(generateAlgorithm), generateIterations, generateKeySize, salt, password);
        return format(SUPPORTED_ALGORITHMS.get(generateAlgorithm), generateIterations, salt, outputBytes);
    }

    /*
     * (non-Javadoc)
     */
    @Override
    public boolean verify(@Sensitive char[] password, String hashedPassword) {
        String[] items = parseData(hashedPassword);
        byte[] originalHash = Base64Coder.base64DecodeString(items[3]);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "original Hash length : " + (originalHash != null?originalHash.length:"null"));
        }
        if (originalHash == null) {
            String message = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_PASSWORDHASH_INVALID_DATA", Tr.formatMessage(tc, "JAVAEESEC_CDI_INVALID_HASH_VALUE"));
            Tr.error(tc, message);
            throw new IllegalArgumentException(message);
        }
        byte[] salt = Base64Coder.base64DecodeString(items[2]);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "original Salt length : " + (salt != null?salt.length:"null"));
        }
        if (salt == null) {
            String message = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_PASSWORDHASH_INVALID_DATA", Tr.formatMessage(tc, "JAVAEESEC_CDI_INVALID_SALT_VALUE"));
            Tr.error(tc, message);
            throw new IllegalArgumentException(message);
        }
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
    private String[] parseData(String hashedPassword) throws IllegalArgumentException {
        // <algorithm>:<iterations>:<base64(salt)>:<base64(hash)>
        String[] items = hashedPassword.split(":");
        String error = null;
        if (items.length == 4) {
            if (SUPPORTED_ALGORITHMS.contains(items[0])) {
                try {
                    Integer.parseInt(items[1]);
                    return items; // good.
                } catch (Exception e) {
                    error = Tr.formatMessage(tc, "JAVAEESEC_CDI_INVALID_ITERATION", items[1]);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid format: the iterations is not a number : " + items[1]);
                    }
                }
            } else {
                error = Tr.formatMessage(tc, "JAVAEESEC_CDI_INVALID_ALGORITHM", items[0]);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Invalid format: the hash algorithm is not supported : " + items[0]);
                }
            }
        } else {
            error = Tr.formatMessage(tc, "JAVAEESEC_CDI_INVALID_ELEMENTS", items.length);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid format: the number of the elements is not 4 but " + items.length);
            }
        }
        String message = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_PASSWORDHASH_INVALID_DATA", error);
        Tr.error(tc, message);
        throw new IllegalArgumentException(message);
    }

    /**
     * Parse the parameters. If the value is not set, set the default, if the value is invalid, throw InvalidArgumentException
     */
    protected void parseParams(Map<String, String> params) {
        generateAlgorithm = indexOf(PARAM_ALGORITHM, DEFAULT_ALGORITHM, SUPPORTED_ALGORITHMS, params.get(PARAM_ALGORITHM));
        generateIterations = parseInt(PARAM_ITERATIONS, params.get(PARAM_ITERATIONS), DEFAULT_ITERATIONS, MINIMUM_ITERATIONS);
        generateSaltSize = parseInt(PARAM_SALTSIZE, params.get(PARAM_SALTSIZE), DEFAULT_SALTSIZE, MINIMUM_SALTSIZE);
        generateKeySize = parseInt(PARAM_KEYSIZE, params.get(PARAM_KEYSIZE), DEFAULT_KEYSIZE, MINIMUM_KEYSIZE);
    }

    private int indexOf(String name, int defaultValue, List<String> list, String value) {
        int output = defaultValue;
        if (value != null) {
            int index = SUPPORTED_ALGORITHMS.indexOf(value);
            if (index >= 0) {
                output = index;
            } else {
                Tr.error(tc, "JAVAEESEC_CDI_ERROR_PASSWORDHASH_INVALID_PARAM", value, name);
                String msg = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_PASSWORDHASH_INVALID_PARAM", value, name);
                throw new IllegalArgumentException(msg);
            }
        }
        return output;
    }

    private int parseInt(String name, String value, int defaultValue, int minimumValue) {
        int output = defaultValue;
        if (value != null) {
            try {
                output = Integer.parseInt(value);
                if (output < minimumValue) {
                    Tr.error(tc, "JAVAEESEC_CDI_ERROR_PASSWORDHASH_BELOW_MINIMUM_PARAM", value, name, minimumValue);
                    String msg = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_PASSWORDHASH_BELOW_MINIMUM_PARAM", value, name, minimumValue);
                    throw new IllegalArgumentException(msg);
                }
            } catch (NumberFormatException e) {
                Tr.error(tc, "JAVAEESEC_CDI_ERROR_PASSWORDHASH_INVALID_PARAM", value, name);
                String msg = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_PASSWORDHASH_INVALID_PARAM", value, name);
                throw new IllegalArgumentException(msg);
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

    public byte[] generate(String algorithm, int iterations, int keySize, byte[] salt, @Sensitive char[] password) {
        try {
            SecretKey secretKey;
            PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, keySize * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
            secretKey = skf.generateSecret(keySpec);
            return secretKey.getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String getAlgorithmString(int index) {
        return SUPPORTED_ALGORITHMS.get(index);
    }

    protected int getAlgorithm() {
        return generateAlgorithm;
    }

    protected int getIterations() {
        return generateIterations;
    }

    protected int getSaltSize() {
        return generateSaltSize;
    }

    protected int getKeySize() {
        return generateKeySize;
    }
}
