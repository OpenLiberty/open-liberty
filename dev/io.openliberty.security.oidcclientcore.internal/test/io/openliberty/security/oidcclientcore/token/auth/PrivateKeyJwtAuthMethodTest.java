/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.token.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import org.jose4j.jws.JsonWebSignature;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.exceptions.PrivateKeyJwtAuthException;
import test.common.SharedOutputManager;

public class PrivateKeyJwtAuthMethodTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String CWWKS2430E_PRIVATE_KEY_JWT_AUTH_ERROR = "CWWKS2430E";
    private static final String CWWKS2431E_PRIVATE_KEY_JWT_MISSING_SIGNING_KEY = "CWWKS2431E";

    private static KeyPair keyPair;

    private String clientId;
    private final String tokenEndpointUrl = "https://somehost/path/to/token";
    private final String clientAssertionSigningAlgorithm = "RS256";

    PrivateKeyJwtAuthMethod authMethod;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        keyPair = keyGen.generateKeyPair();
    }

    @Before
    public void setUp() {
        clientId = testName.getMethodName();
        authMethod = new PrivateKeyJwtAuthMethod(clientId, tokenEndpointUrl, clientAssertionSigningAlgorithm, keyPair.getPrivate());
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_createPrivateKeyJwt_missingKey() {
        authMethod = new PrivateKeyJwtAuthMethod(clientId, tokenEndpointUrl, clientAssertionSigningAlgorithm, null);
        try {
            String jwt = authMethod.createPrivateKeyJwt();
            fail("Should have thrown an exception but got: " + jwt);
        } catch (PrivateKeyJwtAuthException e) {
            verifyException(e, CWWKS2430E_PRIVATE_KEY_JWT_AUTH_ERROR + ".+" + CWWKS2431E_PRIVATE_KEY_JWT_MISSING_SIGNING_KEY);
        }
    }

    @Test
    public void test_createPrivateKeyJwt_algorithmNotValidForKeyType() {
        authMethod = new PrivateKeyJwtAuthMethod(clientId, tokenEndpointUrl, "HS256", keyPair.getPrivate());

        try {
            String jwt = authMethod.createPrivateKeyJwt();
            fail("Should have thrown an exception but got: " + jwt);
        } catch (PrivateKeyJwtAuthException e) {
            verifyException(e, CWWKS2430E_PRIVATE_KEY_JWT_AUTH_ERROR);
        }
    }

    @Test
    public void test_createPrivateKeyJwt() throws Exception {
        authMethod = new PrivateKeyJwtAuthMethod(clientId, tokenEndpointUrl, clientAssertionSigningAlgorithm, keyPair.getPrivate());

        String jwt = authMethod.createPrivateKeyJwt();

        JsonWebSignature jws = (JsonWebSignature) JsonWebSignature.fromCompactSerialization(jwt);
        assertEquals("JWT's alg header did not match expected value.", clientAssertionSigningAlgorithm, jws.getAlgorithmHeaderValue());
        assertEquals("JWT's typ header did not match expected value.", "JWT", jws.getHeader("typ"));
        // TODO - Decide how to correctly verify this value
        assertNotNull("Expected JWT to include an x5t header, but it did not.", jws.getX509CertSha1ThumbprintHeaderValue());

        String rawPayload = jws.getUnverifiedPayload();
        JSONObject jsonPayload = JSONObject.parse(rawPayload);
        // Verify required claims
        assertEquals("JWT's iss claim did not match expected value.", clientId, jsonPayload.get("iss"));
        assertEquals("JWT's iss claim did not match expected value.", clientId, jsonPayload.get("sub"));
        assertEquals("JWT's iss claim did not match expected value.", tokenEndpointUrl, jsonPayload.get("aud"));
        long exp = (long) jsonPayload.get("exp");
        assertNotNull("Expected JWT to include an exp claim, but it did not.", exp);
        // Verify optional claims
        long iat = (long) jsonPayload.get("iat");
        assertNotNull("Expected JWT to include an iat claim, but it did not.", iat);
        assertNotNull("Expected JWT to include a jti claim, but it did not.", jsonPayload.get("jti"));
    }

}
