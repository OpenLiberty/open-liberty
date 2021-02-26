/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.impl;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

// import java.util.Base64; // or could use
import org.apache.commons.codec.binary.Base64;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class PemKeyUtil {
    private static final TraceComponent tc = Tr.register(PemKeyUtil.class);

    /*
     * sample PKCS#8 public key PEM file
     * -----BEGIN PUBLIC KEY-----
     * MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0440JtmhlywtkMvR6tTM
     * s0U6e9Ja4xXj5+q+joWdT2xCHt91Ck9+5C5WOaRTco4CPFMBxoUPi1jktW5c+Oyk
     * nOIACXu6grXexarFQLjsREE+dkDVrMu75f7Gb9/lC7mrVM73118wnMP2u5MOQIoX
     * OqqC1y1gaoJaLp/OjTiJGCm4uxzubzUPN5IDAFaTfK+QErhtcGeBDwWjvikGfUfX
     * +WVq74DOoggLiGbB4jsT8iVXEm53JcoEY8nVr2ygr92TuU1+xLAGisjRSYJVe7V1
     * tpdRG1CiyCIkqhDFfFBGhFnWlu4gKMiT0KToA9GJfOuCz67XZEAhQYizcXbn1uxa
     * OQIDAQAB
     * -----END PUBLIC KEY-----
     */

    protected final static String BEGIN_PUBLIC = "-----BEGIN PUBLIC KEY-----";
    protected final static String END_PUBLIC = "-----END PUBLIC KEY-----";
    protected final static String BEGIN_PRIVATE = "-----BEGIN PRIVATE KEY-----";
    protected final static String END_PRIVATE = "-----END PRIVATE KEY-----";
    protected final static String BEGIN_RSA_PUBLIC = "-----BEGIN RSA PUBLIC KEY-----";
    protected final static String END_RSA_PUBLIC = "-----END RSA PUBLIC KEY-----";
    protected final static String BEGIN_EC_PUBLIC = "-----BEGIN ECDSA PUBLIC KEY-----";
    protected final static String END_EC_PUBLIC = "-----END ECDSA PUBLIC KEY-----";

    protected final static String LINE_SEPARATOR_UNIX = "\n";
    protected final static String LINE_SEPARATOR_MAC = "\r";
    protected final static String LINE_SEPARATOR_WINDOW = "\r\n";
    protected final static String RSA_KEY = "RSA";
    protected final static String EC_KEY = "EC";

    public enum KeyType {
        RSA_PUBLIC, EC_PUBLIC, PUBLIC, PRIVATE, UNKNOWN
    }

    public static PublicKey getPublicKey(String pkcs8pem) throws Exception {
        KeyType keyType = getKeyType(pkcs8pem);
        String pemKey = removeDelimiter(pkcs8pem);
        byte[] encodedKey = Base64.decodeBase64(pemKey);
        return generatePublicKey(encodedKey, keyType);
    }

    @Sensitive
    public static PrivateKey getPrivateKey(@Sensitive String pkcs8pem) throws Exception {
        KeyType keyType = getKeyType(pkcs8pem);
        String pemKey = removeDelimiter(pkcs8pem);
        byte[] encodedKey = Base64.decodeBase64(pemKey);
        return generatePrivateKey(encodedKey, keyType);
    }

    public static KeyType getKeyType(@Sensitive String pkcs8pem) {
        if (pkcs8pem == null) {
            return KeyType.UNKNOWN;
        }
        if (pkcs8pem.contains(BEGIN_RSA_PUBLIC)) {
            return KeyType.RSA_PUBLIC;
        }
        if (pkcs8pem.contains(BEGIN_EC_PUBLIC)) {
            return KeyType.EC_PUBLIC;
        }
        if (pkcs8pem.contains(BEGIN_PUBLIC)) {
            return KeyType.PUBLIC;
        }
        if (pkcs8pem.contains(BEGIN_PRIVATE)) {
            return KeyType.PRIVATE;
        }
        return KeyType.UNKNOWN;
    }

    private static String removeDelimiter(@Sensitive String pem) {
        pem = pem.replaceAll(BEGIN_PUBLIC, "");
        pem = pem.replaceAll(END_PUBLIC, "");
        pem = pem.replaceAll(BEGIN_PRIVATE, "");
        pem = pem.replaceAll(END_PRIVATE, "");
        pem = pem.replaceAll(BEGIN_RSA_PUBLIC, "");
        pem = pem.replaceAll(END_RSA_PUBLIC, "");
        pem = pem.replaceAll(BEGIN_EC_PUBLIC, "");
        pem = pem.replaceAll(END_EC_PUBLIC, "");
        //Can not call System.getProperty("line.separator"), as client and server could have different OS
        pem = pem.replaceAll(LINE_SEPARATOR_UNIX, "");
        pem = pem.replaceAll(LINE_SEPARATOR_MAC, "");
        pem = pem.replaceAll(LINE_SEPARATOR_WINDOW, "");
        return pem.trim();
    }

    private static PublicKey generatePublicKey(byte[] encodedKey, KeyType keyType) throws Exception {
        if (keyType == KeyType.RSA_PUBLIC) {
            return generateRsaPublicKey(encodedKey);
        } else if (keyType == KeyType.EC_PUBLIC) {
            return generateEcPublicKey(encodedKey);
        } else if (keyType == KeyType.PUBLIC) {
            return generateUnspecifiedPublicKey(encodedKey);
        }
        return null;
    }

    @Sensitive
    private static PrivateKey generatePrivateKey(@Sensitive byte[] encodedKey, KeyType keyType) throws Exception {
        return generateUnspecifiedPrivateKey(encodedKey);
    }

    private static PublicKey generateRsaPublicKey(byte[] encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY);
        return keyFactory.generatePublic(keySpec);
    }

    @Sensitive
    private static PrivateKey generateRsaPrivateKey(@Sensitive byte[] encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY);
        return keyFactory.generatePrivate(keySpec);
    }

    private static PublicKey generateEcPublicKey(byte[] encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(EC_KEY);
        return keyFactory.generatePublic(keySpec);
    }

    @Sensitive
    private static PrivateKey generateEcPrivateKey(@Sensitive byte[] encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(EC_KEY);
        return keyFactory.generatePrivate(keySpec);
    }

    @FFDCIgnore(Exception.class)
    private static PublicKey generateUnspecifiedPublicKey(byte[] encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            return generateRsaPublicKey(encodedKey);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to generate RSA public key. Will try to generate EC key instead. Exception was: " + e);
            }
            return generateEcPublicKey(encodedKey);
        }
    }

    @Sensitive
    @FFDCIgnore(Exception.class)
    private static PrivateKey generateUnspecifiedPrivateKey(@Sensitive byte[] encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            return generateRsaPrivateKey(encodedKey);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to generate RSA private key. Will try to generate EC key instead. Exception was: " + e);
            }
            return generateEcPrivateKey(encodedKey);
        }
    }

}