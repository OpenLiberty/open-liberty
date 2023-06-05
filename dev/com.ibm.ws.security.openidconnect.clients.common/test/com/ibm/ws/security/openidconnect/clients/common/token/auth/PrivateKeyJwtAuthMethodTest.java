/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common.token.auth;

import static org.junit.Assert.fail;

import java.security.PrivateKey;
import java.util.regex.Pattern;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.ssl.KeyStoreService;

import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;
import test.common.SharedOutputManager;

public class PrivateKeyJwtAuthMethodTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String CWWKS1554E_TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR = "CWWKS1554E";
    private static final String CWWKS1555E_PRIVATE_KEY_JWT_MISSING_KEY_ALIAS_NAME = "CWWKS1555E";
    private static final String CWWKS1556E_PRIVATE_KEY_JWT_MISSING_KEYSTORE_REF = "CWWKS1556E";

    private final ConvergedClientConfig clientConfig = mockery.mock(ConvergedClientConfig.class);
    private final KeyStoreService keyStoreService = mockery.mock(KeyStoreService.class);
    private final PrivateKey privateKey = mockery.mock(PrivateKey.class);

    private final String tokenEndpointUrl = "https://localhost/op/token";
    private final String clientId = "myClientId";
    private final String clientSecret = "some super secret password";
    private final String redirectUri = "https://localhost/rp/redirect";
    private String code = "";

    Builder tokenRequestBuilder;
    PrivateKeyJwtAuthMethod authMethod;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        code = testName.getMethodName();
        tokenRequestBuilder = new Builder(tokenEndpointUrl, clientId, clientSecret, redirectUri, code);

        authMethod = new PrivateKeyJwtAuthMethod(clientConfig);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_getPrivateKeyForClientAuthentication_nullKeyAliasName() throws Exception {
        String keyAliasName = null;
        String keyStoreRef = "ref";
        try {
            PrivateKeyJwtAuthMethod.getPrivateKeyForClientAuthentication(clientId, keyAliasName, keyStoreRef, keyStoreService);
            fail("Should have thrown an exception, but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS1555E_PRIVATE_KEY_JWT_MISSING_KEY_ALIAS_NAME);
        }
    }

    @Test
    public void test_getPrivateKeyForClientAuthentication_emptyKeyAliasName() throws Exception {
        String keyAliasName = "";
        String keyStoreRef = "ref";
        try {
            PrivateKeyJwtAuthMethod.getPrivateKeyForClientAuthentication(clientId, keyAliasName, keyStoreRef, keyStoreService);
            fail("Should have thrown an exception, but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS1555E_PRIVATE_KEY_JWT_MISSING_KEY_ALIAS_NAME);
        }
    }

    @Test
    public void test_getPrivateKeyForClientAuthentication_nullKeyStoreRef() throws Exception {
        String keyAliasName = "alias";
        String keyStoreRef = null;
        try {
            PrivateKeyJwtAuthMethod.getPrivateKeyForClientAuthentication(clientId, keyAliasName, keyStoreRef, keyStoreService);
            fail("Should have thrown an exception, but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS1556E_PRIVATE_KEY_JWT_MISSING_KEYSTORE_REF);
        }
    }

    @Test
    public void test_getPrivateKeyForClientAuthentication_emptyKeyStoreRef() throws Exception {
        String keyAliasName = "alias";
        String keyStoreRef = "";
        try {
            PrivateKeyJwtAuthMethod.getPrivateKeyForClientAuthentication(clientId, keyAliasName, keyStoreRef, keyStoreService);
            fail("Should have thrown an exception, but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS1556E_PRIVATE_KEY_JWT_MISSING_KEYSTORE_REF);
        }
    }

    @Test
    public void test_getPrivateKeyForClientAuthentication() throws Exception {
        String keyAliasName = "alias";
        String keyStoreRef = "ref";
        mockery.checking(new Expectations() {
            {
                one(keyStoreService).getPrivateKeyFromKeyStore(keyStoreRef, keyAliasName, null);
            }
        });
        PrivateKeyJwtAuthMethod.getPrivateKeyForClientAuthentication(clientId, keyAliasName, keyStoreRef, keyStoreService);
    }

    @Test
    public void test_setAuthMethodSpecificSettings_gettingKeyThrowsException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getTokenEndpointAuthSigningAlgorithm();
                will(returnValue("RS256"));
                one(clientConfig).getPrivateKeyForClientAuthentication();
                will(throwException(new Exception(defaultExceptionMsg)));
                one(clientConfig).getClientId();
                will(returnValue(clientId));
                one(clientConfig).getTokenEndpointAuthMethod();
                will(returnValue(PrivateKeyJwtAuthMethod.AUTH_METHOD));
            }
        });
        try {
            authMethod.setAuthMethodSpecificSettings(tokenRequestBuilder);
            fail("Should have thrown an exception, but didn't.");
        } catch (TokenEndpointAuthMethodSettingsException e) {
            verifyException(e, CWWKS1554E_TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR + ".+" + Pattern.quote(defaultExceptionMsg));
        }
    }

    @Test
    public void test_setAuthMethodSpecificSettings() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(clientConfig).getTokenEndpointAuthSigningAlgorithm();
                will(returnValue("RS256"));
                one(clientConfig).getPrivateKeyForClientAuthentication();
                will(returnValue(privateKey));
            }
        });
        authMethod.setAuthMethodSpecificSettings(tokenRequestBuilder);
    }

}
