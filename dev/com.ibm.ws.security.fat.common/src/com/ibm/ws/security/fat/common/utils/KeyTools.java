package com.ibm.ws.security.fat.common.utils;

import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.jose4j.base64url.SimplePEMEncoder;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class KeyTools {

    protected static Class<?> thisClass = KeyTools.class;

    public final static String rsaPublicPrefix = "-----BEGIN PUBLIC KEY-----";
    public final static String rsaPublicSuffix = "-----END PUBLIC KEY-----";
    public final static String rsaPrivatePrefix = "-----BEGIN PRIVATE KEY-----";
    public final static String rsaPrivateSuffix = "-----END PRIVATE KEY-----";

    public static String getComplexKey(LibertyServer server, String fileName) throws Exception {
        Log.info(thisClass, "getComplexKey", "fileName: " + fileName);
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
        //        Log.info(thisClass, "getPrivateKeyFromPem", "base64: " + base64 + " end");
        byte[] decode = SimplePEMEncoder.decode(base64);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decode);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public static Key getPublicKeyFromPem(String publicKeyString) throws Exception {

        int beginIndex = publicKeyString.indexOf(rsaPublicPrefix) + rsaPublicPrefix.length();
        int endIndex = publicKeyString.indexOf(rsaPublicSuffix);

        String base64 = publicKeyString.substring(beginIndex, endIndex).trim();
        //        Log.info(thisClass, "getPublicKeyFromPem", "base64: " + base64 + " end");
        byte[] decode = SimplePEMEncoder.decode(base64);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decode);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);

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
