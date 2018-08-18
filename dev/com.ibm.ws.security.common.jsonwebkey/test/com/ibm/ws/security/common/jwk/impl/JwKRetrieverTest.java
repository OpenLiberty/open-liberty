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

import static org.junit.Assert.*;

import java.net.URL;
import java.security.PublicKey;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.wsspi.ssl.SSLSupport;

import test.common.SharedOutputManager;

public class JwKRetrieverTest {

    private static final String JWK_RESOURCE_NAME = "jwk_test.json";
    private static final String RELATIVE_JWK_LOCATION = "./com/ibm/ws/security/common/jwk/impl/jwk_test.json";
    private static final String RELATIVE_PEM_LOCATION = "./com/ibm/ws/security/common/jwk/impl/rsa_key.pem";
    private SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    @Rule
    public TestRule outputRule = outputMgr;

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final String kid = "test-key-id";

    private String configId;
    private String sslConfigurationName;
    private String jwkEndpointUrl;
    private JWKSet jwkSet;
    private SSLSupport sslSupport;
    private boolean hnvEnabled;
    private String publickey;
    private String keyLocation;

    @Before
    public void setUp() throws Exception {
        jwkSet = new JWKSet();
        sslSupport = mockery.mock(SSLSupport.class);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testGetPublicKeyFromJwk_relativeLocation() throws Exception {
        keyLocation = RELATIVE_JWK_LOCATION;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_fullLocation() throws Exception {
        URL jwkURL = getClass().getResource(JWK_RESOURCE_NAME);
        keyLocation = jwkURL.getPath();
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_fileURL() throws Exception {
        URL jwkURL = getClass().getResource(JWK_RESOURCE_NAME);
        keyLocation = jwkURL.toString();
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_relativeLocationPEM() throws Exception {
        keyLocation = RELATIVE_PEM_LOCATION;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_publicKeyTextPEM() throws Exception {
        publickey = PemKeyUtilTest.PEM_KEY_TEXT;
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null);

        assertNotNull("There must a public key.", publicKey);
    }

    @Test
    public void testGetPublicKeyFromJwk_publicKeyTextInvalid() throws Exception {
        publickey = "notAValidKeyText";
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null);

        assertNull("There must not be a public key.", publicKey);
    }
    
    @Test
    public void testGetPublicKeyFromJwk_publicKeyLocationInvalid() throws Exception {
        keyLocation = "badKeyLocation";
        JwKRetriever jwkRetriever = new JwKRetriever(configId, sslConfigurationName, jwkEndpointUrl,
                jwkSet, sslSupport, hnvEnabled, null, null, publickey, keyLocation);

        PublicKey publicKey = jwkRetriever.getPublicKeyFromJwk(kid, null);

        assertNull("There must not be a public key.", publicKey);
    }

    // TODO: Test Base64 encoded JWK
    // TODO: Test Base64 encoded JWKS 

}
