/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.PublicKey;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever.JwkKeyType;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

public class JwKRetrieverTest extends CommonTestClass {

    private static final String JWK_RESOURCE_NAME = "jwk_test.json";
    private static final String PEM_RESOURCE_NAME = "rsa_key.pem";
    private static final String RELATIVE_JWK_LOCATION = "./com/ibm/ws/security/common/jwk/impl/" + JWK_RESOURCE_NAME;
    private static final String RELATIVE_PEM_LOCATION = "./com/ibm/ws/security/common/jwk/impl/" + PEM_RESOURCE_NAME;
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    private final String kid = "test-key-id";

    private String configId;
    private String sslConfigurationName;
    private String jwkEndpointUrl;
    private JWKSet jwkSet;
    private SSLSupport sslSupport;
    private boolean hnvEnabled;
    private String signatureAlgorithm = "RS256";
    private String publickey;
    private String keyLocation;

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        System.out.println("Entering test: " + testName.getMethodName());
        jwkSet = new JWKSet();
        sslSupport = mockery.mock(SSLSupport.class);
    }

    @After
    public void tearDown() {
        System.out.println("Exiting test: " + testName.getMethodName());
        //        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void testGetPublicKeyFromJwk_relativeLocation() throws Exception {
        keyLocation = RELATIVE_JWK_LOCATION;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, true);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_fullLocation() throws Exception {
        URL jwkURL = getClass().getResource(JWK_RESOURCE_NAME);
        keyLocation = jwkURL.getPath();
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, false);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_fileURL() throws Exception {
        URL jwkURL = getClass().getResource(JWK_RESOURCE_NAME);
        keyLocation = jwkURL.toString();
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, true);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_relativeLocationPEM_kidSpecified() throws Exception {
        keyLocation = RELATIVE_PEM_LOCATION;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, false);

        assertNotNull("Should have found a key when a relative location to a single, valid PEM key and a kid is specified.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_relativeLocationPEM_noKidSpecified() throws Exception {
        keyLocation = RELATIVE_PEM_LOCATION;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(null, null, false);

        assertNotNull("Should have found a key when a relative location to a single, valid PEM key and no kid is specified.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_publicKeyTextPEM_kidSpecified() throws Exception {
        publickey = PemKeyUtilTest.PEM_KEY_TEXT;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, true);

        assertNotNull("Should have found a key when text for a single, valid PEM key and a kid is specified.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_publicKeyTextPEM_noKidSpecified() throws Exception {
        publickey = PemKeyUtilTest.PEM_KEY_TEXT;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(null, null, true);

        assertNotNull("Should have found a key when text for a single, valid PEM key and no kid is specified.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_publicKeyTextInvalid() throws Exception {
        publickey = "notAValidKeyText";
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, false);

        assertNull("There must not be a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_publicKeyLocationInvalid() throws Exception {
        keyLocation = "badKeyLocation";
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, true);

        assertNull("There must not be a public key.", publicKey);
    }

    // check that when useSystemPropertiesForHttpClientConnections is passed in, client gets created with correct option
    @Test
    public void testGetPublicKeyFromJwk_useSystemProperties() throws Exception {
        keyLocation = "badKeyLocation";
        String jwkEndpointUrl2 = "http://somewheretotallybogusurl";
        MockJwKRetriever jwkRetriever = new MockJwKRetriever(configId, sslConfigurationName, jwkEndpointUrl2,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, true);
        // a "real" retriever would through an io exception due to bogus url, but the mock one doesn't.   

        assertTrue("getBuilder method of JwkRetriever was not invoked with useSystemProperties", jwkRetriever.jvmPropWasSet);
    }

    // TODO
    @Test
    public void test_getJwkFromJWKSet_() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String setId = null;
        String kid = null;
        String x5t = null;
        String use = null;
        String keyText = null;
        JwkKeyType keyType = JwkKeyType.PUBLIC;

        Key result = jwkRetriever.getJwkFromJWKSet(setId, kid, x5t, use, keyText, keyType);
        assertNull("Should not have found a key, but did: " + result, result);
        // TODO
    }

    @Test
    public void testParseKeyText_nullArgs() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String keyText = null;
        String location = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
        assertKeyNotCached(jwkRetriever, keyText, location);
    }

    @Test
    public void testParseKeyText_emptyKeyText() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String keyText = "";
        String location = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
        assertKeyNotCached(jwkRetriever, keyText, location);
    }

    @Test
    public void testParseKeyText_noKtyEntryInText() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String keyText = "{\"entry1\":\"value1\"}";
        String location = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
        assertKeyNotCached(jwkRetriever, keyText, location);
    }

    @Test
    public void testParseKeyText_keyTypeNotString() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String keyText = "{\"kty\":1}";
        String location = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
        assertKeyNotCached(jwkRetriever, keyText, location);
    }

    @Test
    public void testParseKeyText_keyTypeUnknown() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "some unknown value";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
        assertKeyNotCached(jwkRetriever, keyText, location);
    }

    //@Test
    public void testParseKeyText_keyTypeRSA() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "RSA";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        String signatureAlgorithm = null;

        // TODO - figure out how to fix this
        boolean result = jwkRetriever.parseKeyText(keyText, location, signatureAlgorithm);
        assertTrue("Should have successfully parsed key text, but did not.", result);
    }

    @Test
    public void testParseKeyText_keyTypeEC_signatureAlgorithmNull() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "EC";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
        assertKeyNotCached(jwkRetriever, keyText, location);
    }

    @Test
    public void testParseKeyText_keyTypeEC_signatureAlgorithmNotES() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "EC";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        String signatureAlgorithm = "RSA256";

        boolean result = jwkRetriever.parseKeyText(keyText, location, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
        assertKeyNotCached(jwkRetriever, keyText, location);
    }

    //@Test
    public void testParseKeyText_keyTypeEC_signatureAlgorithmES() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, null, publickey, keyLocation);

        String kty = "EC";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        String signatureAlgorithm = "ES512";

        // TODO - figure out how to fix this
        boolean result = jwkRetriever.parseKeyText(keyText, location, signatureAlgorithm);
        assertTrue("Should have successfully parsed key text, but did not.", result);
    }

    @Test
    public void testCreateJwkBasedOnKty_ktyEC_jsonMissingCrvEntry() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "EC";

        JSONObject keyEntry = new JSONObject();
        keyEntry.put("kty", kty);
        String signatureAlgorithm = "ES512";

        JWK result = jwkRetriever.createJwkBasedOnKty(kty, keyEntry, signatureAlgorithm);
        assertNull("Created JWK should have been null but was not.", result);
    }

    //@Test
    public void testCreateJwkBasedOnKty_ktyEC_() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "EC";
        String crv = "crvValue";
        String x = "xValue";
        String y = "yValue";
        String use = "useValue";
        String kid = "kidValue";

        JSONObject keyEntry = new JSONObject();
        keyEntry.put("kty", kty);
        keyEntry.put("crv", crv);
        keyEntry.put("x", x);
        keyEntry.put("y", y);
        keyEntry.put("use", use);
        keyEntry.put("kid", kid);
        String signatureAlgorithm = "ES512";

        // TODO - figure out how to fix this
        JWK result = jwkRetriever.createJwkBasedOnKty(kty, keyEntry, signatureAlgorithm);
        assertNotNull("Created JWK should not have been null but was.", result);
    }

    @Test
    public void test_isPEM_nullString() {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String key = null;
        assertFalse("Key [" + key + "] should not have been considered a PEM key, but was.", jwkRetriever.isPEM(key));
    }

    @Test
    public void test_isPEM_emptyString() {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String key = "";
        assertFalse("Key [" + key + "] should not have been considered a PEM key, but was.", jwkRetriever.isPEM(key));
    }

    @Test
    public void test_isPEM_jwkStringSimple() {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String key = "{\"kty\":\"RSA\"}";
        assertFalse("Key [" + key + "] should not have been considered a PEM key, but was.", jwkRetriever.isPEM(key));
    }

    @Test
    public void test_isPEM_jwkStringFull() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String key = getJwkResourceFileContent();
        assertFalse("Key [" + key + "] should not have been considered a PEM key, but was.", jwkRetriever.isPEM(key));
    }

    @Test
    public void test_isPEM_pemString_noHeader() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String key = getPemResourceFileContent();
        key = key.replace("-----BEGIN PUBLIC KEY-----", "");
        assertFalse("Key [" + key + "] should not have been considered a PEM key, but was.", jwkRetriever.isPEM(key));
    }

    @Test
    public void test_isPEM_pemString_noFooter() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String key = getPemResourceFileContent();
        key = key.replace("-----END PUBLIC KEY-----", "");
        assertTrue("Key [" + key + "] should have been considered a PEM key, but wasn't.", jwkRetriever.isPEM(key));
    }

    @Test
    public void test_isPEM_pemString() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String key = getPemResourceFileContent();
        assertTrue("Key [" + key + "] should have been considered a PEM key, but wasn't.", jwkRetriever.isPEM(key));
    }

    @Test
    public void test_isPemSupportedAlgorithm_nullAlg() {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String alg = null;
        assertFalse("Algorithm [" + alg + "] should not have been considered a PEM supported algorithm, but it was.", jwkRetriever.isPemSupportedAlgorithm(alg));
    }

    @Test
    public void test_isPemSupportedAlgorithm_emptyAlg() {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String alg = "";
        assertFalse("Algorithm [" + alg + "] should not have been considered a PEM supported algorithm, but it was.", jwkRetriever.isPemSupportedAlgorithm(alg));
    }

    @Test
    public void test_isPemSupportedAlgorithm_hs256() {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String alg = "HS256";
        assertFalse("Algorithm [" + alg + "] should not have been considered a PEM supported algorithm, but it was.", jwkRetriever.isPemSupportedAlgorithm(alg));
    }

    @Test
    public void test_isPemSupportedAlgorithm_rs256() {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String alg = "RS256";
        assertTrue("Algorithm [" + alg + "] should have been considered a PEM supported algorithm, but wasn't.", jwkRetriever.isPemSupportedAlgorithm(alg));
    }

    @Test
    public void test_isPemSupportedAlgorithm_es512() {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String alg = "ES512";
        assertTrue("Algorithm [" + alg + "] should have been considered a PEM supported algorithm, but wasn't.", jwkRetriever.isPemSupportedAlgorithm(alg));
    }

    @Test
    public void test_parsePEMFormat_nullKey() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String keyText = null;
        String alg = "RS256";
        JWK result = jwkRetriever.parsePEMFormat(keyText, alg);
        assertNull("Should not have successfully parsed a JWK, but did: [" + result + "].", result);
    }

    @Test
    public void test_parsePEMFormat_emptyKey() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String keyText = "";
        String alg = "RS256";
        JWK result = jwkRetriever.parsePEMFormat(keyText, alg);
        assertNull("Should not have successfully parsed a JWK, but did: [" + result + "].", result);
    }

    @Test
    public void test_parsePEMFormat_notAKey() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String keyText = "This is not a key";
        String alg = "RS256";
        JWK result = jwkRetriever.parsePEMFormat(keyText, alg);
        assertNull("Should not have successfully parsed a JWK, but did: [" + result + "].", result);
    }

    @Test
    public void test_parsePEMFormat_rsaPublicKey_rs256SigAlg() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String keyText = getPemResourceFileContent();
        String alg = "RS256";
        JWK result = jwkRetriever.parsePEMFormat(keyText, alg);
        assertNotNull("Should have successfully parsed a JWK, but didn't.", result);
        assertEquals("Did not get the expected algorithm in the JWK result.", "RS256", result.getAlgorithm());
        assertEquals("Did not get the expected key type in the JWK result.", "RSA", result.getKeyType());
        assertNotNull("JWK's public key should not have been null but was.", result.getPublicKey());
        assertNull("JWK's private key should have been null but was [" + result.getPrivateKey() + "].", result.getPrivateKey());
    }

    @Test
    public void test_parsePEMFormat_rsaPublicKey_es256SigAlg() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String keyText = getPemResourceFileContent();
        String alg = "ES256";
        JWK result = jwkRetriever.parsePEMFormat(keyText, alg);
        // Should fail because the signature algorithm provided to the method doesn't match the algorithm of the key
        assertNull("Should not have successfully parsed a JWK, but did: [" + result + "].", result);
    }

    @Test
    public void test_parsePEMFormat_rsaPrivateKey_rs256SigAlg() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String keyText = "-----BEGIN PRIVATE KEY-----\n" +
                "MIIG/wIBADANBgkqhkiG9w0BAQEFAASCBukwggblAgEAAoIBgQDSQKZBQIPVKXE3\n" +
                "YfhyzNHhta5UjTASCXyPmj6EtoWT7/VELt8i5gpOmUSHtpLfCB6+VEf70O86CRpU\n" +
                "eLsBHv/H55jWD7FZ055/CMYQN5GWZs/bMa96GNVbptPw3R2OdxItO12dluAq7AXF\n" +
                "oy8LMiwdMfP8+8+umogQdtKIwrswv4RQZKs5Q5vTGGHMkcsx6wMuGV2i9L86ldfQ\n" +
                "gZv3lyxVT6ni4Pg93bIsiyieo+1b7IyeAdhKiQ2TRZI7wO6kpQk3Zg1Wy1AQdWmo\n" +
                "qRA9DwMHKPcK0L60WNqYDiap5FV8xspH9SOfoMk/hwhilCuneJ/f+UkcWbf4zDrr\n" +
                "5cBZGtNCnml0VASB0qGuWUzuAamUn1wx7B9Orxx1gIgxgHm6LhTpxDfHph/SG0ql\n" +
                "Awr2q5M68fYKZ8TCLDnv5fky3rMmFpKM2iawQioM5surEpWhhrMea/EEhmhrJMyd\n" +
                "Z+AmOFwmLuSf2J0md3fZ8cAbyt66CVT40qZwkaKp4WOqAR9Ol6sCAwEAAQKCAYAR\n" +
                "4QeCQv8y8Dw9bmpXMM2kOPPjIsJ0tjPOblpkpLTFOMGNk8LLz+4OcwAd3kutBZ6/\n" +
                "pqEca878C2wexMl6Ne2gTTR0946oSdOPj+pv1JnfxUxlolr+sf/qtEwPjm4GAGAe\n" +
                "Uo8KaJsVa06uWMsWU6TmsqnB2JP3kBKD++a5VH3gjlitEXJyAyrMD6W5ZKh3zTlP\n" +
                "AT8yLUfZroZUp8DglA1kD8stjN3vPNv657aDjwMPLxViFlwTMoZiAKeHx0dD3vi7\n" +
                "HLtdmNkrsph4XbWKfINKF3Pp57mW+p/xaYTelfAjQiHoq4sZe1o1I/Z24UKNPK+W\n" +
                "gXeBOonjvy/KKjn9sbsiP0UzhhgzTkaWWGZZRhnWfyRt1KA2EEawPj9Gh0nC63PH\n" +
                "nZm0fOVVn1lmuVdnjTWregDXr2XrjpmmrU5Mhg1/ypJCz8819+WeItWTVd4jf1JO\n" +
                "eT+SGhj1+2Fw69Pfx5rwNFXj6XmkNcyd4oFsAvyEjpRJSx1j5zJIm+AXslHsJBkC\n" +
                "gcEA+LPv4ePSdb37uI4OZ43sB5R4iaVeZshct8aLVWh52YsmUkP6oaGHAHTpB4Mt\n" +
                "DnM48GfhsDkqxKhQgcr8jXe+mUZDBV2bTHzg6HFJtNkZGZf7XgVWHGFCuBcsf4fK\n" +
                "mG3U1BWd2d9Yxz8uWJmpFs8lJbA7+OEmiJglhJWyF/PWAVdbmmkU7fGKn4j7h4zE\n" +
                "e15GSsP2FfE6CX9ywACxyycSPXFupqWKcCO2wpgva5GPOpuihpeVVD36XiOeaPoQ\n" +
                "a7rFAoHBANhr5zzCzHNzwiPSKEXz2QDVCJ3xG130IUSVCqCndrUucwBUDxVPzGYE\n" +
                "ONt4MT33K4g1wvyAUnwSezvYs7i5adPQ/IyrXVaplzf3I7cAdRNOiUChAfEBytU1\n" +
                "kJPbjCpJA9BY9lj2g08uSd0HfMB1bo6Ky8SeSwmNOcxU9tz62TNCZ58QpjAM7rX4\n" +
                "WqxYUZ1XDxTQQ4pR+Wnx0IyLNEDodvsaEB9a90HQVwpAp2cfDpDJquspQh+Pr9aZ\n" +
                "CR7sndzvrwKBwQCdfHP+uuFepbtslqgQcxJvilj58LnipCXyScuZfdsLIs5Vca6Y\n" +
                "A00VwPJ/S1WQvO3lbiLAELNAbMTuK7fZbWZIcPGnyCq7OKjDCFoAJyl5x3/pfb+V\n" +
                "oRE6uZH3/+kOtR38XuPiP0hcg2m5uTQVuX5wazTO2OQpww6e8pwgBswRdVwQX1Jd\n" +
                "ioKZm4iLwVv6MyyX05dVNbfmo8nqQ4ZSep9WQSosZM9RS4xDlVMR7s6f8kXsrLhm\n" +
                "7AhK7Is3NoKHdXECgcEAlMcQ7v9HR7Lzamal0vkAXAcXZmSCmMMkP55p8OlOvN0S\n" +
                "p84hlFgIPrV2VMG6sEuDvJ6gza51hd+6ofimDD5CVx/bDeUcRGBPmWSnQ73nvf8G\n" +
                "ccx9pi3CP3IUUuDh6YvKOGd/6saEnGFI286y3yebehhGXoMnOIjCjAp4ro8v04I5\n" +
                "8+qhJHUvgR8bTlFkwdJAhuFpHOSfnkpjQMrNgDWO3a4cGUISqkbMHzs1GboK7FQG\n" +
                "0KTLVSEVE2SfFYg6OdD3AoHBAL4USqZW4oJtE5vA+CVRBH4iyrrJ9lN0PlIeARgA\n" +
                "K7Qbmk7W/ZHZ3HS8S0seyimO/3lYjTbwaVvF9Jyy7R/ew3bYDYswwUCEpYPMO6Af\n" +
                "V1b6+MR5471v8GR2anrmFm72Img23eb5ohmsdDq4LprsIMGTkaX8PyWioXixnr8g\n" +
                "HZK4sKsD4P/KCkG6Ojy47ezOAqTMpZVXRgK0kALXN7EkelNIt56M0+CFIKtMmFo6\n" +
                "lST0s27SQfg0ylGlC4ZiCgDOVg==\n" +
                "-----END PRIVATE KEY-----";
        String alg = "RS256";
        JWK result = jwkRetriever.parsePEMFormat(keyText, alg);
        assertNotNull("Should have successfully parsed a JWK, but didn't.", result);
        assertEquals("Did not get the expected algorithm in the JWK result.", "RS256", result.getAlgorithm());
        assertEquals("Did not get the expected key type in the JWK result.", "RSA", result.getKeyType());
        assertNotNull("JWK's private key should not have been null but was.", result.getPrivateKey());
        assertNull("JWK's public key should have been null but was [" + result.getPublicKey() + "].", result.getPublicKey());
    }

    @Test
    public void test_parsePEMFormat_rsaPrivateKey_es256SigAlg() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String keyText = "-----BEGIN PRIVATE KEY-----\n" +
                "MIIG/wIBADANBgkqhkiG9w0BAQEFAASCBukwggblAgEAAoIBgQDSQKZBQIPVKXE3\n" +
                "YfhyzNHhta5UjTASCXyPmj6EtoWT7/VELt8i5gpOmUSHtpLfCB6+VEf70O86CRpU\n" +
                "eLsBHv/H55jWD7FZ055/CMYQN5GWZs/bMa96GNVbptPw3R2OdxItO12dluAq7AXF\n" +
                "oy8LMiwdMfP8+8+umogQdtKIwrswv4RQZKs5Q5vTGGHMkcsx6wMuGV2i9L86ldfQ\n" +
                "gZv3lyxVT6ni4Pg93bIsiyieo+1b7IyeAdhKiQ2TRZI7wO6kpQk3Zg1Wy1AQdWmo\n" +
                "qRA9DwMHKPcK0L60WNqYDiap5FV8xspH9SOfoMk/hwhilCuneJ/f+UkcWbf4zDrr\n" +
                "5cBZGtNCnml0VASB0qGuWUzuAamUn1wx7B9Orxx1gIgxgHm6LhTpxDfHph/SG0ql\n" +
                "Awr2q5M68fYKZ8TCLDnv5fky3rMmFpKM2iawQioM5surEpWhhrMea/EEhmhrJMyd\n" +
                "Z+AmOFwmLuSf2J0md3fZ8cAbyt66CVT40qZwkaKp4WOqAR9Ol6sCAwEAAQKCAYAR\n" +
                "4QeCQv8y8Dw9bmpXMM2kOPPjIsJ0tjPOblpkpLTFOMGNk8LLz+4OcwAd3kutBZ6/\n" +
                "pqEca878C2wexMl6Ne2gTTR0946oSdOPj+pv1JnfxUxlolr+sf/qtEwPjm4GAGAe\n" +
                "Uo8KaJsVa06uWMsWU6TmsqnB2JP3kBKD++a5VH3gjlitEXJyAyrMD6W5ZKh3zTlP\n" +
                "AT8yLUfZroZUp8DglA1kD8stjN3vPNv657aDjwMPLxViFlwTMoZiAKeHx0dD3vi7\n" +
                "HLtdmNkrsph4XbWKfINKF3Pp57mW+p/xaYTelfAjQiHoq4sZe1o1I/Z24UKNPK+W\n" +
                "gXeBOonjvy/KKjn9sbsiP0UzhhgzTkaWWGZZRhnWfyRt1KA2EEawPj9Gh0nC63PH\n" +
                "nZm0fOVVn1lmuVdnjTWregDXr2XrjpmmrU5Mhg1/ypJCz8819+WeItWTVd4jf1JO\n" +
                "eT+SGhj1+2Fw69Pfx5rwNFXj6XmkNcyd4oFsAvyEjpRJSx1j5zJIm+AXslHsJBkC\n" +
                "gcEA+LPv4ePSdb37uI4OZ43sB5R4iaVeZshct8aLVWh52YsmUkP6oaGHAHTpB4Mt\n" +
                "DnM48GfhsDkqxKhQgcr8jXe+mUZDBV2bTHzg6HFJtNkZGZf7XgVWHGFCuBcsf4fK\n" +
                "mG3U1BWd2d9Yxz8uWJmpFs8lJbA7+OEmiJglhJWyF/PWAVdbmmkU7fGKn4j7h4zE\n" +
                "e15GSsP2FfE6CX9ywACxyycSPXFupqWKcCO2wpgva5GPOpuihpeVVD36XiOeaPoQ\n" +
                "a7rFAoHBANhr5zzCzHNzwiPSKEXz2QDVCJ3xG130IUSVCqCndrUucwBUDxVPzGYE\n" +
                "ONt4MT33K4g1wvyAUnwSezvYs7i5adPQ/IyrXVaplzf3I7cAdRNOiUChAfEBytU1\n" +
                "kJPbjCpJA9BY9lj2g08uSd0HfMB1bo6Ky8SeSwmNOcxU9tz62TNCZ58QpjAM7rX4\n" +
                "WqxYUZ1XDxTQQ4pR+Wnx0IyLNEDodvsaEB9a90HQVwpAp2cfDpDJquspQh+Pr9aZ\n" +
                "CR7sndzvrwKBwQCdfHP+uuFepbtslqgQcxJvilj58LnipCXyScuZfdsLIs5Vca6Y\n" +
                "A00VwPJ/S1WQvO3lbiLAELNAbMTuK7fZbWZIcPGnyCq7OKjDCFoAJyl5x3/pfb+V\n" +
                "oRE6uZH3/+kOtR38XuPiP0hcg2m5uTQVuX5wazTO2OQpww6e8pwgBswRdVwQX1Jd\n" +
                "ioKZm4iLwVv6MyyX05dVNbfmo8nqQ4ZSep9WQSosZM9RS4xDlVMR7s6f8kXsrLhm\n" +
                "7AhK7Is3NoKHdXECgcEAlMcQ7v9HR7Lzamal0vkAXAcXZmSCmMMkP55p8OlOvN0S\n" +
                "p84hlFgIPrV2VMG6sEuDvJ6gza51hd+6ofimDD5CVx/bDeUcRGBPmWSnQ73nvf8G\n" +
                "ccx9pi3CP3IUUuDh6YvKOGd/6saEnGFI286y3yebehhGXoMnOIjCjAp4ro8v04I5\n" +
                "8+qhJHUvgR8bTlFkwdJAhuFpHOSfnkpjQMrNgDWO3a4cGUISqkbMHzs1GboK7FQG\n" +
                "0KTLVSEVE2SfFYg6OdD3AoHBAL4USqZW4oJtE5vA+CVRBH4iyrrJ9lN0PlIeARgA\n" +
                "K7Qbmk7W/ZHZ3HS8S0seyimO/3lYjTbwaVvF9Jyy7R/ew3bYDYswwUCEpYPMO6Af\n" +
                "V1b6+MR5471v8GR2anrmFm72Img23eb5ohmsdDq4LprsIMGTkaX8PyWioXixnr8g\n" +
                "HZK4sKsD4P/KCkG6Ojy47ezOAqTMpZVXRgK0kALXN7EkelNIt56M0+CFIKtMmFo6\n" +
                "lST0s27SQfg0ylGlC4ZiCgDOVg==\n" +
                "-----END PRIVATE KEY-----";
        String alg = "ES256";
        JWK result = jwkRetriever.parsePEMFormat(keyText, alg);
        // Should fail because the signature algorithm provided to the method doesn't match the algorithm of the key
        assertNull("Should not have successfully parsed a JWK, but did: [" + result + "].", result);
    }

    @Test
    public void test_parsePEMFormat_ecdsaPrivateKey_rs256SigAlg() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String keyText = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg1Wl3D6yxKVduvEFy\n" +
                "7N6TdS96s2yXSgJv6Lzt7Bbxg2+hRANCAAS28OQGy47gyHJlgz5cqtFSo9qEWUrw\n" +
                "If3WLMQPsSxnubk46+3R03ZM9vtm86EbRrcrcWZ90WJ+cxco8Bj1G24g\n" +
                "-----END PRIVATE KEY-----";
        String alg = "RS256";
        JWK result = jwkRetriever.parsePEMFormat(keyText, alg);
        // Should fail because the signature algorithm provided to the method doesn't match the algorithm of the key
        assertNull("Should not have successfully parsed a JWK, but did: [" + result + "].", result);
    }

    // TODO
    //@Test
    public void test_parsePEMFormat_ecdsaPrivateKey_es256SigAlg() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);
        String keyText = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg1Wl3D6yxKVduvEFy\n" +
                "7N6TdS96s2yXSgJv6Lzt7Bbxg2+hRANCAAS28OQGy47gyHJlgz5cqtFSo9qEWUrw\n" +
                "If3WLMQPsSxnubk46+3R03ZM9vtm86EbRrcrcWZ90WJ+cxco8Bj1G24g\n" +
                "-----END PRIVATE KEY-----";
        String alg = "ES256";
        JWK result = jwkRetriever.parsePEMFormat(keyText, alg);
        assertNotNull("Should have successfully parsed a JWK, but didn't.", result);
        assertEquals("Did not get the expected algorithm in the JWK result.", "ES256", result.getAlgorithm());
        assertEquals("Did not get the expected key type in the JWK result.", "ECDSA", result.getKeyType());
        assertNotNull("JWK's private key should not have been null but was.", result.getPrivateKey());
        assertNull("JWK's public key should have been null but was [" + result.getPublicKey() + "].", result.getPublicKey());
    }

    // TODO parseJsonObject
    // TODO parseJwkFormat
    // TODO parseJwksFormat

    // TODO: Test Base64 encoded JWK
    // TODO: Test Base64 encoded JWKS 

    String getJwkResourceFileContent() throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(getJwkResourcePath()));
    }

    String getPemResourceFileContent() throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(getPemResourcePath()));
    }

    Path getJwkResourcePath() throws URISyntaxException {
        return getResourcePath(JWK_RESOURCE_NAME);
    }

    Path getPemResourcePath() throws URISyntaxException {
        return getResourcePath(PEM_RESOURCE_NAME);
    }

    Path getResourcePath(String resourceName) throws URISyntaxException {
        return Paths.get(getClass().getResource(resourceName).toURI());
    }

    void assertKeyNotCached(JwKRetriever jwkRetriever, String keyText, String location) {
        Key cachedKey = jwkRetriever.jwkSet.getKeyBySetId(keyText, JwkKeyType.PUBLIC);
        assertNull("Should not have found a cached key, but found " + cachedKey, cachedKey);
        cachedKey = jwkRetriever.jwkSet.getKeyBySetId(location, JwkKeyType.PUBLIC);
        assertNull("Should not have found a cached key, but found " + cachedKey, cachedKey);
        cachedKey = jwkRetriever.jwkSet.getKeyBySetIdAndKeyText(keyText, keyText, JwkKeyType.PUBLIC);
        assertNull("Should not have found a cached key, but found " + cachedKey, cachedKey);
        cachedKey = jwkRetriever.jwkSet.getKeyBySetIdAndKeyText(location, keyText, JwkKeyType.PUBLIC);
        assertNull("Should not have found a cached key, but found " + cachedKey, cachedKey);
        cachedKey = jwkRetriever.jwkSet.getKeyBySetIdAndKid(keyText, null, JwkKeyType.PUBLIC);
        assertNull("Should not have found a cached key, but found " + cachedKey, cachedKey);
        cachedKey = jwkRetriever.jwkSet.getKeyBySetIdAndKid(location, null, JwkKeyType.PUBLIC);
        assertNull("Should not have found a cached key, but found " + cachedKey, cachedKey);
        cachedKey = jwkRetriever.jwkSet.getKeyBySetIdAndUse(keyText, null, JwkKeyType.PUBLIC);
        assertNull("Should not have found a cached key, but found " + cachedKey, cachedKey);
        cachedKey = jwkRetriever.jwkSet.getKeyBySetIdAndx5t(location, null, JwkKeyType.PUBLIC);
        assertNull("Should not have found a cached key, but found " + cachedKey, cachedKey);
    }

}
