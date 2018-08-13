package com.ibm.ws.security.common.jwk.impl;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
//import java.util.Base64;  // or could use 
import org.apache.commons.codec.binary.Base64;

public class PemKeyUtil {
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
    protected final static String BEGIN = "-----BEGIN (.*)-----";
    protected final static String END = "-----END (.*)-----";
    protected final static String LINE_SEPARATOR_UNIX = "\n";
    protected final static String LINE_SEPARATOR_MAC = "\r";
    protected final static String LINE_SEPARATOR_WINDOW = "\r\n";
    protected final static String RSA_KEY = "RSA";

    public static PublicKey getPublicKey(String pkcs8pem) throws Exception {
        String pemKey = removeDelimiter(pkcs8pem);
        byte[] encodedKey = Base64.decodeBase64(pemKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY);
        return keyFactory.generatePublic(keySpec);
    }

    private static String removeDelimiter(String pem) {
        pem = pem.replaceAll(BEGIN, "");
        pem = pem.replaceAll(END, "");
        //Can not call System.getProperty("line.separator"), as client and server could have different OS
        pem = pem.replaceAll(LINE_SEPARATOR_UNIX, "");
        pem = pem.replaceAll(LINE_SEPARATOR_MAC, "");
        pem = pem.replaceAll(LINE_SEPARATOR_WINDOW, "");
        return pem.trim();
    }

}