/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.security.SignatureException;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.joda.time.Duration;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import net.oauth.jsontoken.JsonToken;
import net.oauth.jsontoken.JsonTokenParser;
import net.oauth.jsontoken.crypto.HmacSHA256Verifier;
import net.oauth.jsontoken.crypto.RsaSHA256Verifier;
import net.oauth.jsontoken.crypto.SignatureAlgorithm;
import net.oauth.jsontoken.crypto.Verifier;
import net.oauth.jsontoken.discovery.VerifierProvider;
import net.oauth.jsontoken.discovery.VerifierProviders;
import net.oauth.jsontoken.SystemClock;
import java.io.FileInputStream;
import java.security.cert.Certificate;
import java.security.PublicKey;
import java.security.KeyStore;

public class JWTTokenVerifier {
    // need net.oauth.jsontoken-1.1/lib/guava-14.0.1.jar
    // need net.oauth.jsontoken-1.1/lib/gson-2.2.4.jar
    // need net.oauth.jsontoken-1.1/lib/jsontoken-1.1-r42.jar
    // need joda-time/lib/joda-time-1.6.2.jar

    String _tokenString = null;
    String _clientId = null;
    Object _key = null;

    VerifierProviders _locators = null;

    private static final Duration SKEW = Duration.standardMinutes(3);

    final String[] signAlgorithms = new String[] { "RS256", // TODO:
            "HS256" };
    String _signAlgorithm = null;

    JsonToken _jsonToken = null;
    JsonObject _header;
    JsonObject _payload;

    public JWTTokenVerifier(String clientId, Object key, String signAlgorithm,
            String tokenString) throws Exception {
        _tokenString = tokenString;
        _clientId = clientId;
        _key = key;
        _signAlgorithm = signAlgorithm;
        initLocator();
    }

    public JWTTokenVerifier(String clientId, String signAlgorithm,
            String strKeystorePathname,
            String trustPassword,
            String trustAlias,
            String tokenString) throws Exception {
        _tokenString = tokenString;
        _clientId = clientId;
        _key = getPublicKey(strKeystorePathname, trustPassword, trustAlias);
        _signAlgorithm = signAlgorithm;
        initLocator();
    }

    public static PublicKey getPublicKey(String keystoreFileName,
            String keystorePassword, String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keystoreFileName),
                keystorePassword.toCharArray());
        Certificate certificate = ks.getCertificate(alias);
        PublicKey publicKey = certificate.getPublicKey();
        return publicKey;
    }

    /**
     * parse and verify the token by FAT. The clock is current time
     * 
     * @param clock
     * @throws SignatureException
     */
    public void verifyAndDeserialize() throws SignatureException {
        verifyAndDeserialize(getSysClock());
    }

    /**
     * parse and verify the token by FAT. It can set the clock
     * 
     * @param clock
     * @throws SignatureException
     */
    public void verifyAndDeserialize(SystemClock clock)
            throws SignatureException {
        _jsonToken = new JsonTokenParser(clock, _locators,
                new FatIgnoreAudience()).verifyAndDeserialize(_tokenString);
        _header = _jsonToken.getHeader();
        _payload = _jsonToken.getPayloadAsJsonObject();
    }

    public boolean isSigned() {
        return _jsonToken != null;
    }

    public JsonObject getJwsHeader() {
        return _header;
    }

    /**
     * return Payload
     */
    public JsonObject getPayload() {
        return _payload;
    }

    JsonToken getJsonToken() {
        return _jsonToken;
    }

    public static String fromBase64ToJsonString(String source) {
        return StringUtils.newStringUtf8(Base64.decodeBase64(source));
    }

    protected void initLocator() throws Exception {
        if ("RS256".equalsIgnoreCase(_signAlgorithm)) {
            final Verifier rsaVerifier = new RsaSHA256Verifier((PublicKey) _key);
            VerifierProvider rsaLocator = new VerifierProvider() {
                @Override
                public List<Verifier> findVerifier(String signerId, String keyId) {
                    return Lists.newArrayList(rsaVerifier);
                }
            };
            _locators = new VerifierProviders();
            _locators.setVerifierProvider(SignatureAlgorithm.RS256, rsaLocator);

        } else {
            final Verifier hmacVerifier = new HmacSHA256Verifier((byte[]) _key);

            VerifierProvider hmacLocator = new VerifierProvider() {
                @Override
                public List<Verifier> findVerifier(String signerId, String keyId) {
                    return Lists.newArrayList(hmacVerifier);
                }
            };
            _locators = new VerifierProviders();
            _locators.setVerifierProvider(SignatureAlgorithm.HS256, hmacLocator);
        }

    }

    public static SystemClock getSysClock() {
        return new SystemClock(SKEW);
    }

}
