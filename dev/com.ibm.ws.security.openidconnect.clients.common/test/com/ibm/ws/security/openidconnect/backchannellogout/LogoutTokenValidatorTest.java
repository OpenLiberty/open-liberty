/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.backchannellogout;

import static org.junit.Assert.fail;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.jwt.config.ConsumerUtils;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class LogoutTokenValidatorTest extends CommonTestClass {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.openidconnect.common.*=all=enabled");

    final String CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS = "CWWKS1536E";
    final String CWWKS1537E_OIDC_CLIENT_JWE_REQUIRED_BUT_TOKEN_NOT_JWE = "CWWKS1537E";
    final String CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR = "CWWKS1543E";
    final String CWWKS1778E_OIDC_JWT_SIGNATURE_VERIFY_MISSING_SIGNATURE_ERR = "CWWKS1778E";

    final String CONFIG_ID = "myConfigId";
    final String CLIENT_ID = "client01";

    final ConvergedClientConfig clientConfig = mockery.mock(ConvergedClientConfig.class);
    final ConsumerUtils consumerUtils = mockery.mock(ConsumerUtils.class);

    LogoutTokenValidator validator;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        validator = new LogoutTokenValidator(clientConfig);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_validateToken_nullString() throws Exception {
        String logoutTokenString = null;
        try {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    one(clientConfig).getId();
                    will(returnValue(CONFIG_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_emptyString() throws Exception {
        String logoutTokenString = "";
        try {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    one(clientConfig).getId();
                    will(returnValue(CONFIG_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_notAJwt() throws Exception {
        String logoutTokenString = "This is not a jwt.";
        try {
            mockery.checking(new Expectations() {
                {
                    one(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    one(clientConfig).getId();
                    will(returnValue(CONFIG_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_jwsRequired_jweFormat() throws Exception {
        String logoutTokenString = "aaa.bbb.ccc.ddd.eee";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    one(clientConfig).getId();
                    will(returnValue(CONFIG_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1536E_OIDC_CLIENT_JWS_REQUIRED_BUT_TOKEN_NOT_JWS);
        }
    }

    @Test
    public void test_validateToken_jweRequired_jwsFormat() throws Exception {
        String logoutTokenString = "aaa.bbb.ccc";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue("someAlias"));
                    one(clientConfig).getId();
                    will(returnValue(CONFIG_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1537E_OIDC_CLIENT_JWE_REQUIRED_BUT_TOKEN_NOT_JWE);
        }
    }

    @Test
    public void test_validateToken_jwsUnsigned_configAlgHs256() throws Exception {
        String logoutTokenString = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    allowing(clientConfig).getSignatureAlgorithm();
                    will(returnValue("HS256"));
                    one(clientConfig).getSharedKey();
                    will(returnValue("secret"));
                    one(clientConfig).getClientId();
                    will(returnValue(CLIENT_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + CWWKS1778E_OIDC_JWT_SIGNATURE_VERIFY_MISSING_SIGNATURE_ERR);
        }
    }

    @Test
    public void test_validateToken_jwsUnsigned_configAlgNone() throws Exception {
        String logoutTokenString = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    allowing(clientConfig).getSignatureAlgorithm();
                    will(returnValue("none"));
                    one(clientConfig).getClientId();
                    will(returnValue(CLIENT_ID));
                }
            });
            validator.validateToken(logoutTokenString);
        } catch (BackchannelLogoutException e) {
            fail("Caught an exception but shouldn't have: " + e);
        }
    }

    @Test
    public void test_validateToken_hs256_keyMismatch() throws Exception {
        // Signed using "secret2" as the key
        String logoutTokenString = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.e30.Uj2QmdsSiazCsMcLY2bZifMmTOVmvxNmh3j3GnslIbA";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    allowing(clientConfig).getSignatureAlgorithm();
                    will(returnValue("HS256"));
                    one(clientConfig).getSharedKey();
                    will(returnValue("secret"));
                    one(clientConfig).getClientId();
                    will(returnValue(CLIENT_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + "Invalid JWS Signature");
        }
    }

    @Test
    public void test_validateToken_hs256_emptyClaims() throws Exception {
        String logoutTokenString = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.e30.DMCAvRgzrcf5w0Z879BsqzcrnDFKBY_GN6c3qKOUFtQ";
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(clientConfig).getKeyManagementKeyAlias();
                    will(returnValue(null));
                    allowing(clientConfig).getSignatureAlgorithm();
                    will(returnValue("HS256"));
                    one(clientConfig).getSharedKey();
                    will(returnValue("secret"));
                    one(clientConfig).getClientId();
                    will(returnValue(CLIENT_ID));
                }
            });
            validator.validateToken(logoutTokenString);
            // TODO
            //            fail("Should have thrown an exception but didn't.");
        } catch (BackchannelLogoutException e) {
            //            verifyException(e, CWWKS1543E_BACKCHANNEL_LOGOUT_TOKEN_ERROR + ".*" + "Invalid JWS Signature");
        }
    }

}
