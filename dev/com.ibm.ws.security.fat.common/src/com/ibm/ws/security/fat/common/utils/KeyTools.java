/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import componenttest.topology.impl.LibertyServer;

public class KeyTools {

    protected static Class<?> thisClass = KeyTools.class;

    public final static String rsaPublicPrefix = "-----BEGIN PUBLIC KEY-----";
    public final static String rsaPublicSuffix = "-----END PUBLIC KEY-----";
    public final static String rsaPrivatePrefix = "-----BEGIN PRIVATE KEY-----";
    public final static String rsaPrivateSuffix = "-----END PRIVATE KEY-----";

    public static String getComplexKey(LibertyServer server, String fileName) throws Exception {
        System.out.println("getComplexKey - fileName: " + fileName);
        return getKeyFromFile(server, fileName);
    }

    public String getSimpleKey(LibertyServer server, String fileName) throws Exception {
        String rawKey = getKeyFromFile(server, fileName);
        if (rawKey != null) {
            rawKey.replace(rsaPublicPrefix, "").replace(rsaPublicSuffix, "");
            rawKey.replace(rsaPrivatePrefix, "").replace(rsaPrivateSuffix, "");
        }
        return rawKey;
    }

    public static String getKeyFromFile(LibertyServer server, String fileName) throws Exception {

        String fullPathToFile = getDefaultKeyFileLoc(server) + fileName;

        CommonIOUtils cioTools = new CommonIOUtils();
        String key = cioTools.readFileAsString(fullPathToFile);

        return key;
    }

    public static String getDefaultKeyFileLoc(LibertyServer server) throws Exception {

        return server.getServerRoot() + "/";
    }

    public static PrivateKey getPrivateKeyFromPem(String privateKeyString) throws Exception {

        int beginIndex = privateKeyString.indexOf(rsaPrivatePrefix) + rsaPrivatePrefix.length();
        int endIndex = privateKeyString.indexOf(rsaPrivateSuffix);

        String base64 = privateKeyString.substring(beginIndex, endIndex).trim();
        System.out.println("getPrivateKeyFromPem - base64: " + base64 + " end");
        byte[] decode = Base64.getDecoder().decode(base64);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decode);

        KeyFactory rsKeyFactory = KeyFactory.getInstance("RSA");
        KeyFactory ecKeyFactory = KeyFactory.getInstance("EC");
        PrivateKey key = null;
        try {
            key = rsKeyFactory.generatePrivate(spec);
        } catch (InvalidKeySpecException e) {
            key = ecKeyFactory.generatePrivate(spec);
        } // don't catch failures from the final call - if that
          // fails, we should really fail the request.
        return key;

    }

    public static Key getPublicKeyFromPem(String publicKeyString) throws Exception {

        int beginIndex = publicKeyString.indexOf(rsaPublicPrefix) + rsaPublicPrefix.length();
        int endIndex = publicKeyString.indexOf(rsaPublicSuffix);

        String base64 = publicKeyString.substring(beginIndex, endIndex).trim();
        System.out.println("getPublicKeyFromPem - base64: " + base64 + " end");
        byte[] decode = Base64.getDecoder().decode(base64);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decode);

        KeyFactory rsKeyFactory = KeyFactory.getInstance("RSA");
        KeyFactory ecKeyFactory = KeyFactory.getInstance("EC");
        Key key = null;
        try {
            key = rsKeyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException e) {
            key = ecKeyFactory.generatePublic(keySpec);
        } // don't catch failures from the final call - if that
          // fails, we should really fail the request.
        return key;

    }

    public static Key getKeyFromPem(String keyString) throws Exception {

        if (keyString != null) {
            if (keyString.contains("PRIVATE")) {
                return getPrivateKeyFromPem(keyString);
            } else {
                return getPublicKeyFromPem(keyString);
            }
        }
        return null;
    }
}
