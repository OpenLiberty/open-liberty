/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.mp.jwt.utils;

import java.net.InetAddress;

import com.ibm.ws.security.fat.common.mp.jwt.MPJwtFatConstants;

import componenttest.topology.impl.LibertyServer;

public class MP11ConfigSettings {

    public static Class<?> thisClass = MP11ConfigSettings.class;

    public static String cert_type = MPJwtFatConstants.X509_CERT;

    // if you recreate the rsa_cert.pem file, please update the PublicKey value saved here.
    public final static String rsaPrefix = "-----BEGIN PUBLIC KEY-----";
    public final static String rsaSuffix = "-----END PUBLIC KEY-----";
    public final static String SimplePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3dxGYp7LZLwUgmn6ts091rymORWnZH34OWjovHR8JQx7Evc1eFqAlDBZptk7pET9xav5568wvYUJJ1cK9CdY/frP9saV7xas49xqo8o47AgzbhfB9Qkm3RTkK3Qv2Y43CSckkAAaKoG2rZQl4e9T1i9Mi3Qh7k8Iq4iwEYtX+iSo3ZytjhX9RkPR+IlkAAK1DEae8oDdoEDvCUB3lCRig+R3hKhi2xayGt0OX13bGC/OC+h76onzDJkGrNX7YRf01D6wdhikzWkzR3NNp7ko8VhhXK+1PrSKflPIUQfL3JmwRm8iqZK9z5ivVm4LT19ZE9ZI+a8GftN1nioaStJ8pQIDAQAB";
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
    String certType = MPJwtFatConstants.X509_CERT;

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
