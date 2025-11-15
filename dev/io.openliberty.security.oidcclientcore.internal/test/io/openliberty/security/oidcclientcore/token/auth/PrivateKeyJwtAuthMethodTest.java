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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jmock.Expectations;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.ibm.json.java.JSONObject;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLConfigChangeListener;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.security.common.ssl.SecuritySSLUtils;
import com.ibm.ws.security.test.common.CommonTestClass;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.ssl.SSLSupport;

import io.openliberty.security.oidcclientcore.exceptions.PrivateKeyJwtAuthException;
import io.openliberty.security.oidcclientcore.exceptions.TokenEndpointAuthMethodSettingsException;
import io.openliberty.security.oidcclientcore.token.TokenConstants;
import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;
import test.common.SharedOutputManager;

public class PrivateKeyJwtAuthMethodTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String CWWKS2430E_PRIVATE_KEY_JWT_AUTH_ERROR = "CWWKS2430E";
    private static final String CWWKS2431E_PRIVATE_KEY_JWT_MISSING_SIGNING_KEY = "CWWKS2431E";
    private static final String CWWKS2432E_TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR = "CWWKS2432E";
    private static final String CWWKS2433E_PRIVATE_KEY_JWT_MISSING_KEY_ALIAS_NAME = "CWWKS2433E";
    private static final String CWWKS2434E_PRIVATE_KEY_JWT_MISSING_KEYSTORE_REF = "CWWKS2434E";

    private static KeyPair keyPair;

    private final KeyStoreService keyStoreService = mockery.mock(KeyStoreService.class);
    private final SSLSupport sslSupport = mockery.mock(SSLSupport.class);
    private final JSSEHelper jsseHelper = mockery.mock(JSSEHelper.class);

    private final String configurationId = "myOidcClientConfig";
    private String clientId;
    private final String tokenEndpointUrl = "https://somehost/path/to/token";
    private final String clientAssertionSigningAlgorithm = "RS256";
    private final String sslRef = "referenceToSslConfig";
    private final String keyAliasName = "aliasForTheKeyToUse";
    private final String keyStoreName = "nameOfKeyStore";

    PrivateKeyJwtAuthMethod authMethod;

    Builder tokenRequestBuilder;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096);
        keyPair = keyGen.generateKeyPair();
    }

    @Before
    public void setUp() throws TokenEndpointAuthMethodSettingsException {
        clientId = testName.getMethodName();
        tokenRequestBuilder = new Builder(tokenEndpointUrl, clientId, null, null, null);

        SecuritySSLUtils sslUtils = new SecuritySSLUtils();
        sslUtils.setSslSupport(sslSupport);

        authMethod = new PrivateKeyJwtAuthMethod(configurationId, clientId, tokenEndpointUrl, clientAssertionSigningAlgorithm, null, sslRef, keyAliasName) {
            @Override
            String getX5tForPublicKey() throws Exception {
                return "x5t_" + testName.getMethodName();
            }
        };
        authMethod.setKeyStoreService(keyStoreService);
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
    public void test_constructor_missingKeyAliasNAme() throws Exception {
        try {
            authMethod = new PrivateKeyJwtAuthMethod(configurationId, clientId, tokenEndpointUrl, clientAssertionSigningAlgorithm, null, sslRef, null);
            fail("Should have thrown an exception, but didn't.");
        } catch (TokenEndpointAuthMethodSettingsException e) {
            verifyException(e, CWWKS2432E_TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR + ".+" + CWWKS2433E_PRIVATE_KEY_JWT_MISSING_KEY_ALIAS_NAME);
        }
    }

    @Test
    public void test_setAuthMethodSpecificSettings_missingKey() throws Exception {
        getPrivateKeyFromKeystoreExpectations(null);
        try {
            authMethod.setAuthMethodSpecificSettings(tokenRequestBuilder);
            fail("Should have thrown an exception, but didn't.");
        } catch (TokenEndpointAuthMethodSettingsException e) {
            verifyException(e, CWWKS2432E_TOKEN_ENDPOINT_AUTH_METHOD_SETTINGS_ERROR + ".+" + CWWKS2430E_PRIVATE_KEY_JWT_AUTH_ERROR + ".+"
                               + CWWKS2431E_PRIVATE_KEY_JWT_MISSING_SIGNING_KEY);
        }
    }

    @Test
    public void test_getPrivateKeyJwtParameters() throws Exception {
        PrivateKey privateKey = keyPair.getPrivate();
        getPrivateKeyFromKeystoreExpectations(privateKey);

        HashMap<String, String> parameters = authMethod.getPrivateKeyJwtParameters();
        assertEquals(TokenConstants.CLIENT_ASSERTION_TYPE + " paramter value did not match expected value.", TokenConstants.CLIENT_ASSERTION_TYPE_JWT_BEARER,
                     parameters.get(TokenConstants.CLIENT_ASSERTION_TYPE));
        assertTrue("Parameters did not include the required " + TokenConstants.CLIENT_ASSERTION + " parameter. Parameters were: " + parameters,
                   parameters.containsKey(TokenConstants.CLIENT_ASSERTION));
        String jwt = parameters.get(TokenConstants.CLIENT_ASSERTION);
        verifyPrivateKeyJwt(jwt);
    }

    @Test
    public void test_getPrivateKeyJwtParameters_withFips140_3() throws Exception {
        try (MockedStatic<CryptoUtils> cryptoUtilsMock = setupMockFips140_3()) {

            authMethod = new PrivateKeyJwtAuthMethod(configurationId, clientId, tokenEndpointUrl, clientAssertionSigningAlgorithm, null, sslRef, keyAliasName) {
                @Override
                String getX5tForPublicKey() throws Exception {
                    return "x5t#S256_" + testName.getMethodName();
                }
            };
            authMethod.setKeyStoreService(keyStoreService);
            PrivateKey privateKey = keyPair.getPrivate();
            getPrivateKeyFromKeystoreExpectations(privateKey);

            HashMap<String, String> parameters = authMethod.getPrivateKeyJwtParameters();
            assertEquals(TokenConstants.CLIENT_ASSERTION_TYPE + " paramter value did not match expected value.", TokenConstants.CLIENT_ASSERTION_TYPE_JWT_BEARER,
                        parameters.get(TokenConstants.CLIENT_ASSERTION_TYPE));
            assertTrue("Parameters did not include the required " + TokenConstants.CLIENT_ASSERTION + " parameter. Parameters were: " + parameters,
                    parameters.containsKey(TokenConstants.CLIENT_ASSERTION));
            String jwt = parameters.get(TokenConstants.CLIENT_ASSERTION);
            verifyPrivateKeyJwt(jwt);
        }
    }

    @Test
    public void test_createPrivateKeyJwt_missingKey() throws Exception {
        getPrivateKeyFromKeystoreExpectations(null);
        try {
            String jwt = authMethod.createPrivateKeyJwt();
            fail("Should have thrown an exception but got: " + jwt);
        } catch (PrivateKeyJwtAuthException e) {
            verifyException(e, CWWKS2430E_PRIVATE_KEY_JWT_AUTH_ERROR + ".*" + CWWKS2431E_PRIVATE_KEY_JWT_MISSING_SIGNING_KEY);
        }
    }

    @Test
    public void test_createPrivateKeyJwt() throws Exception {
        PrivateKey privateKey = keyPair.getPrivate();
        getPrivateKeyFromKeystoreExpectations(privateKey);

        String jwt = authMethod.createPrivateKeyJwt();

        verifyPrivateKeyJwt(jwt);
    }

    @Test
    public void test_createPrivateKeyJwt_withFips140_3() throws Exception {
        try (MockedStatic<CryptoUtils> cryptoUtilsMock = setupMockFips140_3()) {

            authMethod = new PrivateKeyJwtAuthMethod(configurationId, clientId, tokenEndpointUrl, clientAssertionSigningAlgorithm, null, sslRef, keyAliasName) {
                @Override
                String getX5tForPublicKey() throws Exception {
                    return "x5t#S256_" + testName.getMethodName();
                }
            };
            authMethod.setKeyStoreService(keyStoreService);

            PrivateKey privateKey = keyPair.getPrivate();
            getPrivateKeyFromKeystoreExpectations(privateKey);

            String jwt = authMethod.createPrivateKeyJwt();
            
            verifyPrivateKeyJwt(jwt);
    }
}

    @SuppressWarnings("unchecked")
    @Test
    public void test_getPrivateKeyForClientAuthentication_missingKeystoreRef() throws Exception {
        Properties sslProps = new Properties();
        mockery.checking(new Expectations() {
            {
                one(sslSupport).getJSSEHelper();
                will(returnValue(jsseHelper));
                one(jsseHelper).getProperties(with(any(String.class)), with(any(Map.class)), with(any(SSLConfigChangeListener.class)), with(any(Boolean.class)));
                will(returnValue(sslProps));
            }
        });
        try {
            authMethod.getPrivateKeyForClientAuthentication();
            fail("Should have thrown an exception, but didn't.");
        } catch (Exception e) {
            verifyException(e, CWWKS2434E_PRIVATE_KEY_JWT_MISSING_KEYSTORE_REF);
        }
    }

    @Test
    public void test_getPrivateKeyForClientAuthentication() throws Exception {
        PrivateKey privateKey = keyPair.getPrivate();
        getPrivateKeyFromKeystoreExpectations(privateKey);

        PrivateKey returnedKey = authMethod.getPrivateKeyForClientAuthentication();
        assertEquals(privateKey, returnedKey);
    }

    @Test
    public void test_getX509CertificateFromTrustStoreRef_noTrustStoreRef() throws Exception {
        X509Certificate returnedKey = authMethod.getX509CertificateFromTrustStoreRef();
        assertNull("Should not have returned a key, but did: " + returnedKey, returnedKey);
    }

    @SuppressWarnings("unchecked")
    private void getPrivateKeyFromKeystoreExpectations(PrivateKey privateKey) throws Exception {
        Properties sslProps = new Properties();
        sslProps.put(com.ibm.websphere.ssl.Constants.SSLPROP_KEY_STORE_NAME, keyStoreName);
        mockery.checking(new Expectations() {
            {
                one(sslSupport).getJSSEHelper();
                will(returnValue(jsseHelper));
                one(jsseHelper).getProperties(with(any(String.class)), with(any(Map.class)), with(any(SSLConfigChangeListener.class)), with(any(Boolean.class)));
                will(returnValue(sslProps));
                one(keyStoreService).getPrivateKeyFromKeyStore(keyStoreName, keyAliasName, null);
                will(returnValue(privateKey));
            }
        });
    }

    private void verifyPrivateKeyJwt(String jwt) throws JoseException, IOException {
        JsonWebSignature jws = (JsonWebSignature) JsonWebSignature.fromCompactSerialization(jwt);
        assertEquals("JWT's alg header did not match expected value.", clientAssertionSigningAlgorithm, jws.getAlgorithmHeaderValue());
        assertEquals("JWT's typ header did not match expected value.", "JWT", jws.getHeader("typ"));
        if (CryptoUtils.isFips140_3EnabledWithBetaGuard()){
            assertEquals("JWT's x5t#S256 header did not match expected value.", "x5t#S256_" + testName.getMethodName(), jws.getHeader("x5t#S256"));
        } else {
            assertEquals("JWT's x5t header did not match expected value.", "x5t_" + testName.getMethodName(), jws.getHeader("x5t"));
        }
        String rawPayload = jws.getUnverifiedPayload();
        JSONObject jsonPayload = JSONObject.parse(rawPayload);
        // Verify required claims
        assertEquals("JWT's iss claim did not match expected value.", clientId, jsonPayload.get("iss"));
        assertEquals("JWT's sub claim did not match expected value.", clientId, jsonPayload.get("sub"));
        assertEquals("JWT's aud claim did not match expected value.", tokenEndpointUrl, jsonPayload.get("aud"));
        long exp = (long) jsonPayload.get("exp");
        assertNotNull("Expected JWT to include an exp claim, but it did not.", exp);
        // Verify optional claims
        long iat = (long) jsonPayload.get("iat");
        assertNotNull("Expected JWT to include an iat claim, but it did not.", iat);
        assertNotNull("Expected JWT to include a jti claim, but it did not.", jsonPayload.get("jti"));
    }

    private MockedStatic<CryptoUtils> setupMockFips140_3() {
        MockedStatic<CryptoUtils> cryptoUtilsMock = Mockito.mockStatic(CryptoUtils.class);
        cryptoUtilsMock.when(CryptoUtils::isFips140_3EnabledWithBetaGuard).thenReturn(true);
        return cryptoUtilsMock;
    }
}
