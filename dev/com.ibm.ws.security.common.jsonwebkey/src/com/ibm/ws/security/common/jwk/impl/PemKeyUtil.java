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
import java.security.PublicKey;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;  // or could use 
import org.apache.commons.codec.binary.Base64;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class PemKeyUtil {
    private static final TraceComponent tc = Tr.register(PemKeyUtil.class);

    /* sample PKCS#8 public key PEM file
     -----BEGIN PUBLIC KEY-----
    MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0440JtmhlywtkMvR6tTM
    s0U6e9Ja4xXj5+q+joWdT2xCHt91Ck9+5C5WOaRTco4CPFMBxoUPi1jktW5c+Oyk
    nOIACXu6grXexarFQLjsREE+dkDVrMu75f7Gb9/lC7mrVM73118wnMP2u5MOQIoX
    OqqC1y1gaoJaLp/OjTiJGCm4uxzubzUPN5IDAFaTfK+QErhtcGeBDwWjvikGfUfX
    +WVq74DOoggLiGbB4jsT8iVXEm53JcoEY8nVr2ygr92TuU1+xLAGisjRSYJVe7V1
    tpdRG1CiyCIkqhDFfFBGhFnWlu4gKMiT0KToA9GJfOuCz67XZEAhQYizcXbn1uxa
    OQIDAQAB
    -----END PUBLIC KEY-----
     */

    protected final static String BEGIN = "-----BEGIN PUBLIC KEY-----";
    protected final static String END = "-----END PUBLIC KEY-----";
    protected final static String BEGIN_RSA = "-----BEGIN RSA PUBLIC KEY-----";
    protected final static String END_RSA = "-----END RSA PUBLIC KEY-----";
    protected final static String BEGIN_EC = "-----BEGIN ECDSA PUBLIC KEY-----";
    protected final static String END_EC = "-----END ECDSA PUBLIC KEY-----";

    protected final static String LINE_SEPARATOR_UNIX = "\n";
    protected final static String LINE_SEPARATOR_MAC = "\r";
    protected final static String LINE_SEPARATOR_WINDOW = "\r\n";
    protected final static String RSA_KEY = "RSA";
    protected final static String EC_KEY = "EC";

    private enum KeyType {
        RSA, EC, PUBLIC, UNKNOWN
    }

    public static PublicKey getPublicKey(String pkcs8pem) throws Exception {
        KeyType keyType = getKeyType(pkcs8pem);
        String pemKey = removeDelimiter(pkcs8pem);
        byte[] encodedKey = Base64.decodeBase64(pemKey);
        return generatePublicKey(encodedKey, keyType);
    }

    private static KeyType getKeyType(String pkcs8pem) {
        if (pkcs8pem == null) {
            return KeyType.UNKNOWN;
        }
        if (pkcs8pem.contains(BEGIN_RSA)) {
            return KeyType.RSA;
        }
        if (pkcs8pem.contains(BEGIN_EC)) {
            return KeyType.EC;
        }
        if (pkcs8pem.contains(BEGIN)) {
            return KeyType.PUBLIC;
        }
        return KeyType.UNKNOWN;
    }

    private static String removeDelimiter(String pem) {
        pem = pem.replaceAll(BEGIN, "");
        pem = pem.replaceAll(END, "");
        pem = pem.replaceAll(BEGIN_RSA, "");
        pem = pem.replaceAll(END_RSA, "");
        pem = pem.replaceAll(BEGIN_EC, "");
        pem = pem.replaceAll(END_EC, "");
        //Can not call System.getProperty("line.separator"), as client and server could have different OS
        pem = pem.replaceAll(LINE_SEPARATOR_UNIX, "");
        pem = pem.replaceAll(LINE_SEPARATOR_MAC, "");
        pem = pem.replaceAll(LINE_SEPARATOR_WINDOW, "");
        return pem.trim();
    }

    private static PublicKey generatePublicKey(byte[] encodedKey, KeyType keyType) throws Exception {
        if (keyType == KeyType.RSA) {
            return generateRsaPublicKey(encodedKey);
        } else if (keyType == KeyType.EC) {
            return generateEcPublicKey(encodedKey);
        } else if (keyType == KeyType.PUBLIC) {
            return generateUnspecifiedPublicKey(encodedKey);
        }
        return null;
    }

    private static PublicKey generateRsaPublicKey(byte[] encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY);
        return keyFactory.generatePublic(keySpec);
    }

    private static PublicKey generateEcPublicKey(byte[] encodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(EC_KEY);
        return keyFactory.generatePublic(keySpec);
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

}