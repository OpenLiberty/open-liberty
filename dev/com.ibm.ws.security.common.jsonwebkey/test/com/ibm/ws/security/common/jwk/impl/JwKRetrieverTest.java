/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.security.PublicKey;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.common.jwk.interfaces.JWK;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

public class JwKRetrieverTest extends CommonTestClass {

    private static final String JWK_RESOURCE_NAME = "jwk_test.json";
    private static final String RELATIVE_JWK_LOCATION = "./com/ibm/ws/security/common/jwk/impl/jwk_test.json";
    private static final String RELATIVE_JWK_MINIMUM_LOCATION = "./com/ibm/ws/security/common/jwk/impl/jwk_minimum_test.json";
    private static final String RELATIVE_PEM_LOCATION = "./com/ibm/ws/security/common/jwk/impl/rsa_key.pem";
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    private final String kid = "test-key-id";
    private final String x5t = "U1dkoqHSjCUk2fdBHU-qSCpQXZc=";
    private final String x5tS256 = "h2d0YhmNecvX5YUU8Yl_-qG-1J2Yy0E-UJdAhV9E2Rg=";

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
    public void testGetPublicKeyFromJwk_relativeLocation_minimumJwkRSA() throws Exception {
        keyLocation = RELATIVE_JWK_MINIMUM_LOCATION;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey1 = jwkRetriever.getPublicKeyFromJwk(null, null, false);

        assertNotNull("Should have successfully loaded the public key, but didn't.", publicKey1);

        PublicKey publicKey2 = jwkRetriever.getPublicKeyFromJwk(null, null, false);

        assertNotNull("Should have successfully re-loaded the public key, but didn't.", publicKey2);
        assertEquals("Retrieved keys did not match but should have.", publicKey1, publicKey2);
    }

    @Test
    public void testGetPublicKeyFromJwk_relativeLocation_minimumJwkRSA_withUse() throws Exception {
        keyLocation = RELATIVE_JWK_MINIMUM_LOCATION;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey1 = jwkRetriever.getPublicKeyFromJwk(null, null, null, "sig", false);

        assertNotNull("Should have successfully loaded the public key, but didn't.", publicKey1);

        PublicKey publicKey2 = jwkRetriever.getPublicKeyFromJwk(null, null, null, "sig", false);

        assertNotNull("Should have successfully re-loaded the public key, but didn't.", publicKey2);
        assertEquals("Retrieved keys did not match but should have.", publicKey1, publicKey2);
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
    public void testGetPublicKeyFromJwk_relativeLocationPEM_x5tSpecified() throws Exception {
        keyLocation = RELATIVE_PEM_LOCATION;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(null, x5t, false);

        assertNotNull("Should have found a key when a relative location to a single, valid PEM key and x5t is specified.", publicKey);
    }
    
    @Test
    public void testGetPublicKeyFromJwk_relativeLocationPEM_x5tS256Specified() throws Exception {
        keyLocation = RELATIVE_PEM_LOCATION;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(null, null, x5tS256, false);

        assertNotNull("Should have found a key when a relative location to a single, valid PEM key and x5t#S256 is specified.", publicKey);
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
    public void testGetPublicKeyFromJwk_publicKeyTextPEM_x5tSpecified() throws Exception {
        publickey = PemKeyUtilTest.PEM_KEY_TEXT;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(null, x5t, true);

        assertNotNull("Should have found a key when text for a single, valid PEM key and x5t is specified.", publicKey);
    }
    
    @Test
    public void testGetPublicKeyFromJwk_publicKeyTextPEM_x5tS256Specified() throws Exception {
        publickey = PemKeyUtilTest.PEM_KEY_TEXT;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(null, null, x5tS256, true);

        assertNotNull("Should have found a key when text for a single, valid PEM key and x5t#S256 is specified.", publicKey);
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

    @Test
    public void testParseKeyText_nullArgs() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String keyText = null;
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
    }

    @Test
    public void testParseKeyText_emptyKeyText() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String keyText = "";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
    }

    @Test
    public void testParseKeyText_noKtyEntryInText() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String keyText = "{\"entry1\":\"value1\"}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
    }

    @Test
    public void testParseKeyText_keyTypeNotString() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String keyText = "{\"kty\":1}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
    }

    @Test
    public void testParseKeyText_keyTypeUnknown() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "some unknown value";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
    }

    @Test
    public void testParseKeyText_keyTypeRSA_missingN() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "RSA";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertFalse("Should not have successfully parsed key text, but did.", result);
    }

    @Test
    public void testParseKeyText_keyTypeRSA_missingE() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "RSA";
        String keyText = "{\"kty\":\"" + kty + "\",\"n\":\"rkCYJj7QPIURA+T0arwFkBWK/8PemAW/gppsY5p+uqwASoFNnHLOiUpS6k3NJRcb0QEu2MHjt7IKZ/mya4NgoAMfM+lm0+QmDhY1XFUrmKj0WQhp/Oc6X48kX2zDmu00GXjO3H2446IofTnBeWxIpClpH+aQ0rcCZlLOu/O/CDIHz30qpe4NT4MlkYUeKNltUBctNQP7VMJw4iPHCdsXlIfpVqzONWIdbsFTsk1r3ynrReOeIbP4JA2/sI03LdSS0XxMVYe7zwIb9dHmWlOjMcejNTEh4fRdNnwQYbU3aWhj55gNYpDxUvwazwN52Rm9XoTsv+pi0pj3SK0PeE3s1w==\"}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertFalse("Should not have successfully parsed key text, but did.", result);
    }

    @Test
    public void testParseKeyText_keyTypeRSA_minimum() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "RSA";
        String keyText = "{\"kty\":\"" + kty + "\",\"n\":\"rkCYJj7QPIURA+T0arwFkBWK/8PemAW/gppsY5p+uqwASoFNnHLOiUpS6k3NJRcb0QEu2MHjt7IKZ/mya4NgoAMfM+lm0+QmDhY1XFUrmKj0WQhp/Oc6X48kX2zDmu00GXjO3H2446IofTnBeWxIpClpH+aQ0rcCZlLOu/O/CDIHz30qpe4NT4MlkYUeKNltUBctNQP7VMJw4iPHCdsXlIfpVqzONWIdbsFTsk1r3ynrReOeIbP4JA2/sI03LdSS0XxMVYe7zwIb9dHmWlOjMcejNTEh4fRdNnwQYbU3aWhj55gNYpDxUvwazwN52Rm9XoTsv+pi0pj3SK0PeE3s1w==\",\"e\":\"AQAB\"}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertTrue("Should have successfully parsed key text, but did not.", result);
    }

    @Test
    public void testParseKeyText_keyTypeEC_signatureAlgorithmNull() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "EC";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
    }

    @Test
    public void testParseKeyText_keyTypeEC_signatureAlgorithmNotES() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, signatureAlgorithm, publickey, keyLocation);

        String kty = "EC";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = "RSA256";

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
    }

    //@Test
    public void testParseKeyText_keyTypeEC_signatureAlgorithmES() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, null, publickey, keyLocation);

        String kty = "EC";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = "ES512";

        // TODO - figure out how to fix this
        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
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

    @Test
    public void testParseJsonObject_nullString() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(jwkSet);

        String jsonString = null;

        JSONObject result = jwkRetriever.parseJsonObject(jsonString);
        assertNull("Created JSON object should have been null but was not.", result);
    }

    @Test
    public void testParseJsonObject_emptyString() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(jwkSet);

        String jsonString = "";

        JSONObject result = jwkRetriever.parseJsonObject(jsonString);
        assertNull("Created JSON object should have been null but was not.", result);
    }

    @Test
    public void testParseJsonObject_nonJsonString() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(jwkSet);

        String jsonString = "non json";

        JSONObject result = jwkRetriever.parseJsonObject(jsonString);
        assertNull("Created JSON object should have been null but was not.", result);
    }

    @Test
    public void testParseJsonObject_emptyJsonObject() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(jwkSet);

        String jsonString = "{}";

        JSONObject result = jwkRetriever.parseJsonObject(jsonString);
        assertNotNull("Created JSON object should not have been null but was.", result);
        assertTrue("Created JSON object should have been empty but wasn't: " + result, result.isEmpty());
    }

    @Test
    public void testParseJsonObject_simpleJsonObject() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(jwkSet);

        String jsonString = "{\"kid\":\"abc123\"}";

        JSONObject result = jwkRetriever.parseJsonObject(jsonString);
        assertNotNull("Created JSON object should not have been null but was.", result);
        assertFalse("Created JSON object should not have been empty but was.", result.isEmpty());
        assertTrue("Created JSON object did not contain an expected 'kid' entry. Result was: " + result, result.containsKey("kid"));
    }

    @Test
    public void testParseJsonObject_simpleJsonObject_leadingWhitespace() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(jwkSet);

        String jsonString = " {\"kid\":\"abc123\"}";

        JSONObject result = jwkRetriever.parseJsonObject(jsonString);
        assertNotNull("Created JSON object should not have been null but was.", result);
        assertFalse("Created JSON object should not have been empty but was.", result.isEmpty());
        assertTrue("Created JSON object did not contain an expected 'kid' entry. Result was: " + result, result.containsKey("kid"));
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

    // TODO: Test Base64 encoded JWK
    // TODO: Test Base64 encoded JWKS

}