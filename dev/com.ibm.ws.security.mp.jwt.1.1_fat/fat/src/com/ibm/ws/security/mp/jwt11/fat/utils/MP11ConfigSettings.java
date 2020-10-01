/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt11.fat.utils;

import java.net.InetAddress;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.CommonIOUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;

import componenttest.topology.impl.LibertyServer;

@SuppressWarnings("restriction")
public class MP11ConfigSettings {

    public static Class<?> thisClass = MP11ConfigSettings.class;

    public static String cert_type = MpJwtFatConstants.X509_CERT;

    // if you recreate the rsa_cert.pem file, please update the PublicKey value saved here.
    public final static String rsaPrefix = "-----BEGIN PUBLIC KEY-----";
    public final static String rsaSuffix = "-----END PUBLIC KEY-----";
    public final static String SimplePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl66sYoc5HXGHnrtGCMZ6G8zLHnAl+xhP7bOQMmqwEqtwI+yJJG3asvLhJQiizP0cMA317ekJE6VAJ2DBT8g2npqJSXK/IuVQokM4CNp0IIbD66qgVLJ4DS1jzf6GFciJAiGOHztl8ICd7/q0EvuYcwd/sUjTrwRpkLcEH2Z/FE2sh4a82UwyxZkX3ghbZ/3MFtsMjzw0cSqKPUrgGCr4ZcAWZeoye81cLybY5Vb/5/eZfkeBIDwSSssqJRmsNBFs23c+RAymtKaP7wsQw5ATEeI7pe0kiWLpqH4wtsDVyN1C/p+vZJSia0OQJ/z89b5OkmpFC6qGBGxC7eOk71wCJwIDAQAB";
    public final static String ComplexPublicKey = rsaPrefix + SimplePublicKey + rsaSuffix;
    public final static String PemFile = "rsa_key.pem";
    public final static String ComplexPemFile = "rsa_key_withCert.pem";
    public final static String BadPemFile = "bad_key.pem";
    public final static String jwksUri = "\"http://localhost:${bvt.prop.security_2_HTTP_default}/jwt/ibm/api/defaultJWT/jwk\"";
    public final static String PublicKeyNotSet = "";
    public final static String PublicKeyLocationNotSet = "";
    public final static String IssuerNotSet = "";

    String publicKeyLocation = null;
    String publicKey = ComplexPublicKey;
    String issuer = IssuerNotSet;
    String certType = MpJwtFatConstants.X509_CERT;

    /* key file names */
    public static final String rs256PubKey = "RS256public-key.pem";
    public static final String rs384PubKey = "RS384public-key.pem";
    public static final String rs512PubKey = "RS512public-key.pem";
    public static final String es256PubKey = "ES256public-key.pem";
    public static final String es384PubKey = "ES384public-key.pem";
    public static final String es512PubKey = "ES512public-key.pem";
    public static final String ps256PubKey = "PS256public-key.pem";
    public static final String ps384PubKey = "PS384public-key.pem";
    public static final String ps512PubKey = "PS512public-key.pem";

    public MP11ConfigSettings() {
    }

    public MP11ConfigSettings(String inPublicKeyLocation, String inPublicKey, String inIssuer, String inCertType) {

        publicKeyLocation = inPublicKeyLocation;
        publicKey = inPublicKey;
        issuer = inIssuer;
        certType = inCertType;
    }

    public void setPublicKeyLocation(String inPublicKeyLocation) {
        publicKeyLocation = inPublicKeyLocation;
    }

    public String getPublicKeyLocation() {
        return publicKeyLocation;
    }

    public void setPublicKey(String inPublicKey) {
        publicKeyLocation = inPublicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setIssuer(String inIssuer) {
        issuer = inIssuer;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setCertType(String inCertType) {
        certType = inCertType;
    }

    public String getCertType() {
        return certType;
    }

    public static String getComplexKeyForSigAlg(LibertyServer server, String sigAlg) throws Exception {

        return getComplexKey(server, getKeyFileNameForAlg(sigAlg));
    }

    public static String getKeyFileNameForAlg(String sigAlg) throws Exception {

        switch (sigAlg) {
            case MpJwtFatConstants.SIGALG_RS256:
                return rs256PubKey;
            case MpJwtFatConstants.SIGALG_RS384:
                return rs384PubKey;
            case MpJwtFatConstants.SIGALG_RS512:
                return rs512PubKey;
            case MpJwtFatConstants.SIGALG_ES256:
                return es256PubKey;
            case MpJwtFatConstants.SIGALG_ES384:
                return es384PubKey;
            case MpJwtFatConstants.SIGALG_ES512:
                return es512PubKey;
            case MpJwtFatConstants.SIGALG_PS256:
                return ps256PubKey;
            case MpJwtFatConstants.SIGALG_PS384:
                return ps384PubKey;
            case MpJwtFatConstants.SIGALG_PS512:
                return ps512PubKey;
            default:
                return rs256PubKey;
        }

    }

    public static String getComplexKey(LibertyServer server, String fileName) throws Exception {
        Log.info(thisClass, "getComplexKey", "fileName: " + fileName);
        return getKeyFromFile(server, fileName);
    }

    public String getSimpleKey(LibertyServer server, String fileName) throws Exception {
        String rawKey = getKeyFromFile(server, fileName);
        if (rawKey != null) {
            return rawKey.replace(rsaPrefix, "").replace(rsaSuffix, "");
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

    public static String buildDefaultIssuerString(LibertyServer server) throws Exception {

        InetAddress addr = InetAddress.getLocalHost();
        String serverHostName = addr.getHostName();
        String serverHostIp = addr.toString().split("/")[1];

        return "testIssuer, http://" + serverHostName + ":" + server.getBvtPort()
               + "/jwt/defaultJWT, http://" + serverHostIp + ":" + server.getBvtPort()
               + "/jwt/defaultJWT, https://" + serverHostName + ":" + server.getBvtSecurePort()
               + "/jwt/defaultJWT, https://" + serverHostIp + ":" + server.getBvtSecurePort() + "/jwt/defaultJWT";

    }
}