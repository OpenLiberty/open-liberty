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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.security.PublicKey;

import org.apache.http.impl.client.HttpClientBuilder;
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
    private static final String RELATIVE_PEM_LOCATION = "./com/ibm/ws/security/common/jwk/impl/rsa_key.pem";
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    private final String kid = "test-key-id";

    private String configId;
    private String sslConfigurationName;
    private String jwkEndpointUrl;
    private JWKSet jwkSet;
    private SSLSupport sslSupport;
    private boolean hnvEnabled;
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
        outputMgr.resetStreams();
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
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, true);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_fullLocation() throws Exception {
        URL jwkURL = getClass().getResource(JWK_RESOURCE_NAME);
        keyLocation = jwkURL.getPath();
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, false);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_fileURL() throws Exception {
        URL jwkURL = getClass().getResource(JWK_RESOURCE_NAME);
        keyLocation = jwkURL.toString();
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, true);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_relativeLocationPEM() throws Exception {
        keyLocation = RELATIVE_PEM_LOCATION;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, false);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_publicKeyTextPEM() throws Exception {
        publickey = PemKeyUtilTest.PEM_KEY_TEXT;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, true);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_publicKeyTextInvalid() throws Exception {
        publickey = "notAValidKeyText";
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, false);

        assertNull("There must not be a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_publicKeyLocationInvalid() throws Exception {
        keyLocation = "badKeyLocation";
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, true);

        assertNull("There must not be a public key.", publicKey);
    }
    
  
    // check that when useSystemPropertiesForHttpClientConnections is passed in, client gets created with correct option
    @Test
    public void testGetPublicKeyFromJwk_useSystemProperties() throws Exception {
        keyLocation = "badKeyLocation";
        String jwkEndpointUrl2 = "http://somewheretotallybogusurl";
        MockJwKRetriever jwkRetriever = new MockJwKRetriever(configId, sslConfigurationName, jwkEndpointUrl2,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);       
        
        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null, true);
        // a "real" retriever would through an io exception due to bogus url, but the mock one doesn't.   
        
        assertTrue("getBuilder method of JwkRetriever was not invoked with useSystemProperties", jwkRetriever.jvmPropWasSet);
    }

    @Test
    public void testParseKeyText_nullArgs() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

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
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

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
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

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
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

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
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        String kty = "some unknown value";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertFalse("Should have failed to parse key text, but did not.", result);
    }

    //@Test
    public void testParseKeyText_keyTypeRSA() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        String kty = "RSA";
        String keyText = "{\"kty\":\"" + kty + "\"}";
        String location = null;
        JWKSet jwkset = null;
        String signatureAlgorithm = null;

        // TODO - figure out how to fix this
        boolean result = jwkRetriever.parseKeyText(keyText, location, jwkset, signatureAlgorithm);
        assertTrue("Should have successfully parsed key text, but did not.", result);
    }

    @Test
    public void testParseKeyText_keyTypeEC_signatureAlgorithmNull() throws Exception {
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

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
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

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
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

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
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

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
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

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
