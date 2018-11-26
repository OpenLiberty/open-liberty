/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocketFactory;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.UserApiConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.OAuthClientUtil;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;

public class AuthorizationCodeAuthenticatorTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private static final String AUTHZ_CODE = "1234567890";
    private static String ACCESS_TOKEN = "EAANQIE2J5nMBAErWBIFfkmu9r6yQeGoIMg39mHRJrZA7L0jbiD7GEpLSZBm96tgqvvlbQI3UIgQXSJaO6sRJGaFEZCwn5kolWgSjs5q71rrNg0GdbHk5yxrtsZAWsZBv3XV1xFmJ4reZBKA6sx5PqQJejg5RtTWKPg4jJoP0zk1AZDZD";
    private static String REFRESH_TOKEN = "67890";
    private static long EXPIRES_IN = 5177064;
    // IdToken with claims {"at_hash":"0HbzhW49bhEP2b3SVHfeGg","sub":"user1","realmName":"OpBasicRealm","uniqueSecurityName":"user1","iss":"https://localhost:8020/oidc/endpoint/OP","nonce":"v1zg5OZ9vXP5h0lEiYs1","aud":"rp","exp":1455909058,"iat":1455901858}
    private static final String ID_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo4MDIwL29pZGMvZW5kcG9pbnQvT1AiLCJub25jZSI6InYxemc1T1o5dlhQNWgwbEVpWXMxIiwiaWF0IjoxNDU1OTAxODU4LCJzdWIiOiJ1c2VyMSIsImV4cCI6MTQ1NTkwOTA1OCwiYXVkIjoicnAiLCJyZWFsbU5hbWUiOiJPcEJhc2ljUmVhbG0iLCJ1bmlxdWVTZWN1cml0eU5hbWUiOiJ1c2VyMSIsImF0X2hhc2giOiIwSGJ6aFc0OWJoRVAyYjNTVkhmZUdnIn0.VJNknPRe0BhzfMA4MpQIEeVczaHYiMzPiBYejp72zIs";

    public interface MockInterface {
        void createSslSocketFactory() throws SocialLoginException;

        void getTokensFromTokenEndpoint() throws SocialLoginException;

        Map<String, Object> getTokensUsingAuthzCode() throws SocialLoginException;

        void createJwtUserApiResponseAndIssuedJwtWithAppropriateToken() throws SocialLoginException;

        String getIdTokenFromTokens();

        void createJwtUserApiResponseAndIssuedJwtFromUserApi() throws SocialLoginException;

        String getAccessTokenFromTokens();

        void createUserApiResponseFromAccessToken() throws SocialLoginException;

        void createIssuedJwtFromUserApiResponse() throws SocialLoginException;

        void createJwtUserApiResponseAndIssuedJwtFromIdToken() throws SocialLoginException;

        void createJwtAndIssuedJwtFromIdToken() throws SocialLoginException;

        void createJwtFromIdToken() throws SocialLoginException;

        void createIssuedJwtFromIdToken() throws Exception;

        void createUserApiResponseFromIdToken();
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    final SSLSocketFactory sslSocketFactory = mockery.mock(SSLSocketFactory.class);
    final JwtToken jwtToken = mockery.mock(JwtToken.class, "jwtToken");
    final OAuthClientUtil clientUtil = mockery.mock(OAuthClientUtil.class);
    final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class, "mockSocialLoginConfig");
    final TAIWebUtils taiWebUtils = mockery.mock(TAIWebUtils.class);
    final TAIJwtUtils taiJwtUtils = mockery.mock(TAIJwtUtils.class);
    final UserApiConfig userApiConfig = mockery.mock(UserApiConfig.class);

    AuthorizationCodeAuthenticator authenticator = null;

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config);
        mockProtectedClassMembers(authenticator);
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

    private void mockProtectedClassMembers(AuthorizationCodeAuthenticator authenticator) {
        authenticator.clientUtil = clientUtil;
        authenticator.taiWebUtils = taiWebUtils;
        authenticator.taiJwtUtils = taiJwtUtils;
    }

    /****************************************** generateJwtAndTokenInformation ******************************************/

    @Test
    public void test_generateJwtAndTokenInformation_createSslSocketFactoryThrowsException() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createSslSocketFactory() throws SocialLoginException {
                mockInterface.createSslSocketFactory();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createSslSocketFactory();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                authenticator.generateJwtAndTokenInformation();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            // generateJwtAndTokenInformation() does not emit its own message when one of the methods it calls throws an exception
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_generateJwtAndTokenInformation_getTokensFromTokenEndpointThrowsException() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createSslSocketFactory() throws SocialLoginException {
                mockInterface.createSslSocketFactory();
            }

            @Override
            void getTokensFromTokenEndpoint() throws SocialLoginException {
                mockInterface.getTokensFromTokenEndpoint();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createSslSocketFactory();
                    one(mockInterface).getTokensFromTokenEndpoint();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                authenticator.generateJwtAndTokenInformation();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            // generateJwtAndTokenInformation() does not emit its own message when one of the methods it calls throws an exception
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_generateJwtAndTokenInformation_createJwtWithAppropriateTokenThrowsException() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createSslSocketFactory() throws SocialLoginException {
                mockInterface.createSslSocketFactory();
            }

            @Override
            void getTokensFromTokenEndpoint() throws SocialLoginException {
                mockInterface.getTokensFromTokenEndpoint();
            }

            @Override
            public void createJwtUserApiResponseAndIssuedJwtWithAppropriateToken() throws SocialLoginException {
                mockInterface.createJwtUserApiResponseAndIssuedJwtWithAppropriateToken();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createSslSocketFactory();
                    one(mockInterface).getTokensFromTokenEndpoint();
                    one(mockInterface).createJwtUserApiResponseAndIssuedJwtWithAppropriateToken();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                authenticator.generateJwtAndTokenInformation();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            // generateJwtAndTokenInformation() does not emit its own message when one of the methods it calls throws an exception
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_generateJwtAndTokenInformation_successful() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createSslSocketFactory() throws SocialLoginException {
                mockInterface.createSslSocketFactory();
            }

            @Override
            void getTokensFromTokenEndpoint() throws SocialLoginException {
                mockInterface.getTokensFromTokenEndpoint();
            }

            @Override
            public void createJwtUserApiResponseAndIssuedJwtWithAppropriateToken() throws SocialLoginException {
                mockInterface.createJwtUserApiResponseAndIssuedJwtWithAppropriateToken();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createSslSocketFactory();
                    one(mockInterface).getTokensFromTokenEndpoint();
                    one(mockInterface).createJwtUserApiResponseAndIssuedJwtWithAppropriateToken();
                }
            });

            authenticator.generateJwtAndTokenInformation();

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createSslSocketFactory ******************************************/

    @Test
    public void test_createSslSocketFactory_throwsException() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getSSLSocketFactory();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            final String exceptionAndLogMessage = CWWKS5450E_AUTH_CODE_ERROR_SSL_CONTEXT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg);
            try {
                authenticator.createSslSocketFactory();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, exceptionAndLogMessage);
            }

            verifyLogMessage(outputMgr, exceptionAndLogMessage);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createSslSocketFactory_successful() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getSSLSocketFactory();
                    will(returnValue(sslSocketFactory));
                }
            });

            authenticator.createSslSocketFactory();

            assertNotNull("SSL socket factory should not have been null but was.", authenticator.sslSocketFactory);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getTokensFromTokenEndpoint ******************************************/

    @Test
    public void test_getTokensFromTokenEndpoint_exceptionThrown() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            Map<String, Object> getTokensUsingAuthzCode() throws SocialLoginException {
                return mockInterface.getTokensUsingAuthzCode();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTokensUsingAuthzCode();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            try {
                authenticator.getTokensFromTokenEndpoint();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyLogMessage(outputMgr, CWWKS5451E_AUTH_CODE_ERROR_GETTING_TOKENS + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getTokensFromTokenEndpoint_nullTokens() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            Map<String, Object> getTokensUsingAuthzCode() throws SocialLoginException {
                return mockInterface.getTokensUsingAuthzCode();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTokensUsingAuthzCode();
                    will(returnValue(null));
                }
            });

            authenticator.getTokensFromTokenEndpoint();

            assertNull("Tokens should have been null but were: " + authenticator.getTokens(), authenticator.getTokens());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getTokensFromTokenEndpoint_nonEmptyTokens() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            Map<String, Object> getTokensUsingAuthzCode() throws SocialLoginException {
                return mockInterface.getTokensUsingAuthzCode();
            }
        };
        mockProtectedClassMembers(authenticator);
        final Map<String, Object> tokens = createTokens(ACCESS_TOKEN, REFRESH_TOKEN, ID_TOKEN, EXPIRES_IN);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTokensUsingAuthzCode();
                    will(returnValue(tokens));
                }
            });

            authenticator.getTokensFromTokenEndpoint();

            assertEquals("Token map did not match expected result.", tokens, authenticator.getTokens());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getTokensUsingAuthzCode ******************************************/

    @Test
    public void test_getTokensUsingAuthzCode_throwsException() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getTokenEndpoint();
                    one(config).getClientId();
                    one(config).getClientSecret();
                    one(config).getTokenEndpointAuthMethod();
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getResource();
                    one(taiWebUtils).getRedirectUrl(request, config);
                    one(clientUtil).getTokensFromAuthzCode(with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)),
                            with(any(String.class)), with(any(SSLSocketFactory.class)), with(any(boolean.class)), with(any(String.class)), with(any(String.class)), with(any(Boolean.class)));
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                Map<String, Object> result = authenticator.getTokensUsingAuthzCode();
                fail("Should have thrown exception, but instead got tokens: " + result);
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getTokensUsingAuthzCode_nullTokens() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getTokenEndpoint();
                    one(config).getClientId();
                    one(config).getClientSecret();
                    one(config).getTokenEndpointAuthMethod();
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getResource();
                    one(taiWebUtils).getRedirectUrl(request, config);
                    one(clientUtil).getTokensFromAuthzCode(with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)),
                            with(any(String.class)), with(any(SSLSocketFactory.class)), with(any(boolean.class)), with(any(String.class)), with(any(String.class)), with(any(Boolean.class)));
                    will(returnValue(null));
                }
            });

            Map<String, Object> result = authenticator.getTokensUsingAuthzCode();

            assertNull("Tokens should have been null but were: " + result, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getTokensUsingAuthzCode_nonEmptyTokens() throws Exception {
        final Map<String, Object> tokens = createTokens(ACCESS_TOKEN, REFRESH_TOKEN, ID_TOKEN, EXPIRES_IN);
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getTokenEndpoint();
                    one(config).getClientId();
                    one(config).getClientSecret();
                    one(config).getTokenEndpointAuthMethod();
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(config).getResource();
                    one(taiWebUtils).getRedirectUrl(request, config);
                    one(clientUtil).getTokensFromAuthzCode(with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)),
                            with(any(String.class)), with(any(SSLSocketFactory.class)), with(any(boolean.class)), with(any(String.class)), with(any(String.class)), with(any(Boolean.class)));
                    will(returnValue(tokens));
                }
            });

            Map<String, Object> result = authenticator.getTokensUsingAuthzCode();

            assertEquals("Token map did not match expected result.", tokens, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createJwtUserApiResponseAndIssuedJwtWithAppropriateToken ******************************************/

    @Test
    public void test_createJwtWithAppropriateToken_nullIdToken_exceptionThrown() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            String getIdTokenFromTokens() {
                return mockInterface.getIdTokenFromTokens();
            }

            @Override
            String getAccessTokenFromTokens() {
                return mockInterface.getAccessTokenFromTokens();
            }

            @Override
            void createJwtUserApiResponseAndIssuedJwtFromUserApi() throws SocialLoginException {
                mockInterface.createJwtUserApiResponseAndIssuedJwtFromUserApi();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            // Access token value doesn't really matter
            final String accessToken = RandomUtils.getRandomSelection(null, ACCESS_TOKEN);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getIdTokenFromTokens();
                    will(returnValue(null));
                    one(mockInterface).getAccessTokenFromTokens();
                    will(returnValue(accessToken));
                    one(mockInterface).createJwtUserApiResponseAndIssuedJwtFromUserApi();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                authenticator.createJwtUserApiResponseAndIssuedJwtWithAppropriateToken();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            assertEquals("Access token did not match expected value.", accessToken, authenticator.getAccessToken());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createJwtWithAppropriateToken_nullIdToken() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            String getIdTokenFromTokens() {
                return mockInterface.getIdTokenFromTokens();
            }

            @Override
            String getAccessTokenFromTokens() {
                return mockInterface.getAccessTokenFromTokens();
            }

            @Override
            void createJwtUserApiResponseAndIssuedJwtFromUserApi() throws SocialLoginException {
                mockInterface.createJwtUserApiResponseAndIssuedJwtFromUserApi();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            // Access token value doesn't really matter
            final String accessToken = RandomUtils.getRandomSelection(null, ACCESS_TOKEN);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getIdTokenFromTokens();
                    will(returnValue(null));
                    one(mockInterface).getAccessTokenFromTokens();
                    will(returnValue(accessToken));
                    one(mockInterface).createJwtUserApiResponseAndIssuedJwtFromUserApi();
                }
            });

            authenticator.createJwtUserApiResponseAndIssuedJwtWithAppropriateToken();

            assertEquals("Access token did not match expected value.", accessToken, authenticator.getAccessToken());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createJwtWithAppropriateToken_withIdToken_exceptionThrown() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            String getIdTokenFromTokens() {
                return mockInterface.getIdTokenFromTokens();
            }

            @Override
            String getAccessTokenFromTokens() {
                return mockInterface.getAccessTokenFromTokens();
            }

            @Override
            void createJwtUserApiResponseAndIssuedJwtFromIdToken(String idToken) throws SocialLoginException {
                mockInterface.createJwtUserApiResponseAndIssuedJwtFromIdToken();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            // Access token value doesn't really matter
            final String accessToken = RandomUtils.getRandomSelection(null, ACCESS_TOKEN);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getIdTokenFromTokens();
                    will(returnValue(ID_TOKEN));
                    one(mockInterface).getAccessTokenFromTokens();
                    will(returnValue(accessToken));
                    one(mockInterface).createJwtUserApiResponseAndIssuedJwtFromIdToken();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                authenticator.createJwtUserApiResponseAndIssuedJwtWithAppropriateToken();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            assertEquals("Access token did not match expected value.", accessToken, authenticator.getAccessToken());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createJwtWithAppropriateToken_withIdToken() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            String getIdTokenFromTokens() {
                return mockInterface.getIdTokenFromTokens();
            }

            @Override
            String getAccessTokenFromTokens() {
                return mockInterface.getAccessTokenFromTokens();
            }

            @Override
            void createJwtUserApiResponseAndIssuedJwtFromIdToken(String idToken) throws SocialLoginException {
                mockInterface.createJwtUserApiResponseAndIssuedJwtFromIdToken();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            // Access token value doesn't really matter
            final String accessToken = RandomUtils.getRandomSelection(null, ACCESS_TOKEN);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getIdTokenFromTokens();
                    will(returnValue(ID_TOKEN));
                    one(mockInterface).getAccessTokenFromTokens();
                    will(returnValue(accessToken));
                    one(mockInterface).createJwtUserApiResponseAndIssuedJwtFromIdToken();
                }
            });

            authenticator.createJwtUserApiResponseAndIssuedJwtWithAppropriateToken();

            assertEquals("Access token did not match expected value.", accessToken, authenticator.getAccessToken());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getIdTokenFromTokens ******************************************/

    @Test
    public void test_getIdTokenFromTokens_nullToken() throws Exception {
        try {
            String result = authenticator.getIdTokenFromTokens();
            assertNull("Result should have been null but was: [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getIdTokenFromTokens_missingIdToken() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            Map<String, Object> getTokensUsingAuthzCode() throws SocialLoginException {
                return mockInterface.getTokensUsingAuthzCode();
            }
        };
        mockProtectedClassMembers(authenticator);
        final Map<String, Object> tokens = createTokens(ACCESS_TOKEN, REFRESH_TOKEN, null, EXPIRES_IN);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTokensUsingAuthzCode();
                    will(returnValue(tokens));
                }
            });
            // Call this method to populate the tokens
            authenticator.getTokensFromTokenEndpoint();

            String result = authenticator.getIdTokenFromTokens();

            assertNull("Result should have been null but was: [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getIdTokenFromTokens_withToken() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            Map<String, Object> getTokensUsingAuthzCode() throws SocialLoginException {
                return mockInterface.getTokensUsingAuthzCode();
            }
        };
        mockProtectedClassMembers(authenticator);
        final Map<String, Object> tokens = createTokens(ACCESS_TOKEN, REFRESH_TOKEN, ID_TOKEN, EXPIRES_IN);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTokensUsingAuthzCode();
                    will(returnValue(tokens));
                }
            });
            // Call this method to populate the tokens
            authenticator.getTokensFromTokenEndpoint();

            String result = authenticator.getIdTokenFromTokens();

            assertEquals("ID token did not match expected value.", ID_TOKEN, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createJwtUserApiResponseAndIssuedJwtFromUserApi ******************************************/

    @Test
    public void test_createJwtFromUserApi_createUserApiFromTokenThrowsException() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createUserApiResponseFromAccessToken() throws SocialLoginException {
                mockInterface.createUserApiResponseFromAccessToken();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createUserApiResponseFromAccessToken();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                authenticator.createJwtUserApiResponseAndIssuedJwtFromUserApi();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createJwtFromUserApi_createUserApiFromResponseThrowsException() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createUserApiResponseFromAccessToken() throws SocialLoginException {
                mockInterface.createUserApiResponseFromAccessToken();
            }

            @Override
            void createIssuedJwtFromUserApiResponse() throws SocialLoginException {
                mockInterface.createIssuedJwtFromUserApiResponse();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createUserApiResponseFromAccessToken();
                    one(mockInterface).createIssuedJwtFromUserApiResponse();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                authenticator.createJwtUserApiResponseAndIssuedJwtFromUserApi();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createJwtFromUserApi_successful() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createUserApiResponseFromAccessToken() throws SocialLoginException {
                mockInterface.createUserApiResponseFromAccessToken();
            }

            @Override
            void createIssuedJwtFromUserApiResponse() throws SocialLoginException {
                mockInterface.createIssuedJwtFromUserApiResponse();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createUserApiResponseFromAccessToken();
                    one(mockInterface).createIssuedJwtFromUserApiResponse();
                }
            });

            authenticator.createJwtUserApiResponseAndIssuedJwtFromUserApi();

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getAccessTokenFromTokens ******************************************/

    @Test
    public void test_getAccessTokenFromTokens_nullToken() throws Exception {
        try {
            String result = authenticator.getAccessTokenFromTokens();
            assertNull("Result should have been null but was: [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getAccessTokenFromTokens_missingAccessToken() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            Map<String, Object> getTokensUsingAuthzCode() throws SocialLoginException {
                return mockInterface.getTokensUsingAuthzCode();
            }
        };
        mockProtectedClassMembers(authenticator);
        final Map<String, Object> tokens = createTokens(null, REFRESH_TOKEN, ID_TOKEN, EXPIRES_IN);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTokensUsingAuthzCode();
                    will(returnValue(tokens));
                }
            });
            // Call this method to populate the tokens
            authenticator.getTokensFromTokenEndpoint();

            String result = authenticator.getAccessTokenFromTokens();

            assertNull("Result should have been null but was: [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getAccessTokenFromTokens_withToken() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            Map<String, Object> getTokensUsingAuthzCode() throws SocialLoginException {
                return mockInterface.getTokensUsingAuthzCode();
            }
        };
        mockProtectedClassMembers(authenticator);
        final Map<String, Object> tokens = createTokens(ACCESS_TOKEN, REFRESH_TOKEN, ID_TOKEN, EXPIRES_IN);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTokensUsingAuthzCode();
                    will(returnValue(tokens));
                }
            });
            // Call this method to populate the tokens
            authenticator.getTokensFromTokenEndpoint();

            String result = authenticator.getAccessTokenFromTokens();

            assertEquals("Access token did not match expected value.", ACCESS_TOKEN, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createUserApiResponseFromAccessToken ******************************************/

    @Test
    public void test_createUserApiResponseFromAccessToken_nullApiResponse() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApis();
                    will(returnValue(null));
                    allowing(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            try {
                authenticator.createUserApiResponseFromAccessToken();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5452E_USER_API_RESPONSE_NULL_OR_EMPTY, uniqueId);
            }

            assertNull("User API response should have been null but was: [" + authenticator.getUserApiResponse() + "].", authenticator.getUserApiResponse());

            verifyLogMessageWithInserts(outputMgr, CWWKS5460W_NO_USER_API_CONFIGS_PRESENT, uniqueId);
            verifyLogMessageWithInserts(outputMgr, CWWKS5452E_USER_API_RESPONSE_NULL_OR_EMPTY, uniqueId);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createUserApiResponseFromAccessToken_emptyApiResponse() throws Exception {
        try {
            final String userApiResponse = "";
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApis();
                    will(returnValue(new UserApiConfig[] { userApiConfig }));
                    one(userApiConfig).getApi();
                    one(config).getUserApiNeedsSpecialHeader();
                    one(clientUtil).getUserApiResponse(with(any(String.class)), with(any(String.class)), with(any(SSLSocketFactory.class)), with(any(Boolean.class)), with(any(Boolean.class)), with(any(Boolean.class)));
                    will(returnValue(userApiResponse));
                    allowing(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(config).getUseSystemPropertiesForHttpClientConnections(); will(returnValue(false));
                }
            });

            try {
                authenticator.createUserApiResponseFromAccessToken();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5452E_USER_API_RESPONSE_NULL_OR_EMPTY, uniqueId);
            }

            assertEquals("User API response did not match expected value.", userApiResponse, authenticator.getUserApiResponse());

            verifyLogMessageWithInserts(outputMgr, CWWKS5452E_USER_API_RESPONSE_NULL_OR_EMPTY, uniqueId);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createUserApiResponseFromAccessToken_validApiResponse() throws Exception {
        try {
            final String userApiResponse = "Some valid response";
            mockery.checking(new Expectations() {
                {
                    one(config).getUserApis();
                    will(returnValue(new UserApiConfig[] { userApiConfig }));
                    one(userApiConfig).getApi();
                    one(config).getUserApiNeedsSpecialHeader();
                    one(config).getUseSystemPropertiesForHttpClientConnections();
                    one(clientUtil).getUserApiResponse(with(any(String.class)), with(any(String.class)), with(any(SSLSocketFactory.class)), with(any(Boolean.class)), with(any(Boolean.class)), with(any(Boolean.class)));
                    will(returnValue(userApiResponse));
                }
            });

            authenticator.createUserApiResponseFromAccessToken();

            assertEquals("User API response did not match expected value.", userApiResponse, authenticator.getUserApiResponse());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createIssuedJwtFromUserApiResponse ******************************************/

    @Test
    public void test_createIssuedJwtFromUserApiResponse_noJwtRef() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getJwtRef();
                    will(returnValue(null));
                }
            });

            authenticator.createIssuedJwtFromUserApiResponse();

            assertNull("Issued JWT should have been null but was: [" + authenticator.getIssuedJwt() + "].", authenticator.getIssuedJwt());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createIssuedJwtFromUserApiResponse_exceptionThrown() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getJwtRef();
                    one(taiJwtUtils).createJwtTokenFromJson(with(any(String.class)), with(any(SocialLoginConfig.class)), with(any(Boolean.class)));
                    will(throwException(new Exception(defaultExceptionMsg)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            final String exceptionAndLogMsg = CWWKS5453E_AUTH_CODE_FAILED_TO_CREATE_JWT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg);
            try {
                authenticator.createIssuedJwtFromUserApiResponse();
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, exceptionAndLogMsg);
            }

            assertNull("Issued JWT should have been null but was: [" + authenticator.getIssuedJwt() + "].", authenticator.getIssuedJwt());

            verifyLogMessage(outputMgr, exceptionAndLogMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createIssuedJwtFromUserApiResponse_successful() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getJwtRef();
                    one(taiJwtUtils).createJwtTokenFromJson(with(any(String.class)), with(any(SocialLoginConfig.class)), with(any(Boolean.class)));
                    will(returnValue(jwtToken));
                }
            });

            authenticator.createIssuedJwtFromUserApiResponse();

            assertNotNull("Issued JWT should not have been null but was.", authenticator.getIssuedJwt());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createJwtUserApiResponseAndIssuedJwtFromIdToken ******************************************/

    @Test
    public void test_createJwtUserApiResponseAndIssuedJwtFromIdToken_creatingJwtThrowsException() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createJwtAndIssuedJwtFromIdToken(String idToken) throws SocialLoginException {
                mockInterface.createJwtAndIssuedJwtFromIdToken();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createJwtAndIssuedJwtFromIdToken();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                authenticator.createJwtUserApiResponseAndIssuedJwtFromIdToken(null);
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            assertNull("User API should have been null but was: [" + authenticator.getUserApiResponse() + "].", authenticator.getUserApiResponse());
            assertNull("JWT should have been null but was: [" + authenticator.getJwt() + "].", authenticator.getJwt());
            assertNull("Issued JWT should have been null but was: [" + authenticator.getIssuedJwt() + "].", authenticator.getIssuedJwt());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createJwtUserApiResponseAndIssuedJwtFromIdToken_successful() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createJwtAndIssuedJwtFromIdToken(String idToken) throws SocialLoginException {
                mockInterface.createJwtAndIssuedJwtFromIdToken();
            }

            @Override
            void createUserApiResponseFromIdToken(String idToken) {
                mockInterface.createUserApiResponseFromIdToken();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createJwtAndIssuedJwtFromIdToken();
                    one(mockInterface).createUserApiResponseFromIdToken();
                }
            });

            authenticator.createJwtUserApiResponseAndIssuedJwtFromIdToken(null);

            // Calls have been mocked out, so the values will still be null
            assertNull("User API should have been null but was: [" + authenticator.getUserApiResponse() + "].", authenticator.getUserApiResponse());
            assertNull("JWT should have been null but was: [" + authenticator.getJwt() + "].", authenticator.getJwt());
            assertNull("Issued JWT should have been null but was: [" + authenticator.getIssuedJwt() + "].", authenticator.getIssuedJwt());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createJwtAndIssuedJwtFromIdToken ******************************************/

    @Test
    public void test_createJwtAndIssuedJwtFromIdToken_creatingJwtThrowsException() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createJwtFromIdToken(String idToken) throws SocialLoginException {
                mockInterface.createJwtFromIdToken();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createJwtFromIdToken();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            final String exceptionAndLogMsg = CWWKS5453E_AUTH_CODE_FAILED_TO_CREATE_JWT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg);
            try {
                authenticator.createJwtAndIssuedJwtFromIdToken(null);
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, exceptionAndLogMsg);
            }

            assertNull("User API should have been null but was: [" + authenticator.getUserApiResponse() + "].", authenticator.getUserApiResponse());
            assertNull("JWT should have been null but was: [" + authenticator.getJwt() + "].", authenticator.getJwt());
            assertNull("Issued JWT should have been null but was: [" + authenticator.getIssuedJwt() + "].", authenticator.getIssuedJwt());

            verifyLogMessage(outputMgr, exceptionAndLogMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createJwtAndIssuedJwtFromIdToken_creatingIssuedJwtThrowsException() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createJwtFromIdToken(String idToken) throws SocialLoginException {
                mockInterface.createJwtFromIdToken();
            }

            @Override
            void createIssuedJwtFromIdToken(String idToken) throws Exception {
                mockInterface.createIssuedJwtFromIdToken();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createJwtFromIdToken();
                    one(mockInterface).createIssuedJwtFromIdToken();
                    will(throwException(new Exception(defaultExceptionMsg)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            final String exceptionAndLogMsg = CWWKS5453E_AUTH_CODE_FAILED_TO_CREATE_JWT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg);
            try {
                authenticator.createJwtAndIssuedJwtFromIdToken(null);
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, exceptionAndLogMsg);
            }

            assertNull("User API should have been null but was: [" + authenticator.getUserApiResponse() + "].", authenticator.getUserApiResponse());
            assertNull("JWT should have been null but was: [" + authenticator.getJwt() + "].", authenticator.getJwt());
            assertNull("Issued JWT should have been null but was: [" + authenticator.getIssuedJwt() + "].", authenticator.getIssuedJwt());

            verifyLogMessage(outputMgr, exceptionAndLogMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createJwtAndIssuedJwtFromIdToken_successful() throws Exception {
        authenticator = new AuthorizationCodeAuthenticator(request, response, AUTHZ_CODE, config) {
            @Override
            void createJwtFromIdToken(String idToken) throws SocialLoginException {
                mockInterface.createJwtFromIdToken();
            }

            @Override
            void createIssuedJwtFromIdToken(String idToken) throws Exception {
                mockInterface.createIssuedJwtFromIdToken();
            }
        };
        mockProtectedClassMembers(authenticator);
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).createJwtFromIdToken();
                    one(mockInterface).createIssuedJwtFromIdToken();
                }
            });

            authenticator.createJwtAndIssuedJwtFromIdToken(null);

            // Calls have been mocked out, so the values will still be null
            assertNull("User API should have been null but was: [" + authenticator.getUserApiResponse() + "].", authenticator.getUserApiResponse());
            assertNull("JWT should have been null but was: [" + authenticator.getJwt() + "].", authenticator.getJwt());
            assertNull("Issued JWT should have been null but was: [" + authenticator.getIssuedJwt() + "].", authenticator.getIssuedJwt());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createJwtFromIdToken ******************************************/

    @Test
    public void test_createJwtFromIdToken_throwsException() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(taiJwtUtils).createJwtTokenFromIdToken(with(any(String.class)), with(any(String.class)));
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            // Token value shouldn't matter since the calls are mocked out, so choose a value at random
            String idToken = RandomUtils.getRandomSelection(null, ID_TOKEN);

            try {
                authenticator.createJwtFromIdToken(idToken);
                fail("Should have thrown exception but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            assertNull("JWT should have been null but was: [" + authenticator.getJwt() + "].", authenticator.getJwt());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createJwtFromIdToken_successful() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(taiJwtUtils).createJwtTokenFromIdToken(with(any(String.class)), with(any(String.class)));
                    will(returnValue(jwtToken));
                }
            });

            // Token value shouldn't matter since the calls are mocked out, so choose a value at random
            String idToken = RandomUtils.getRandomSelection(null, ID_TOKEN);

            authenticator.createJwtFromIdToken(idToken);

            assertNotNull("JWT should have not been null but was.", authenticator.getJwt());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createIssuedJwtFromIdToken ******************************************/

    @Test
    public void test_createIssuedJwtFromIdToken_nullJwtRef() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getJwtRef();
                    will(returnValue(null));
                }
            });

            // Token value shouldn't matter
            String idToken = RandomUtils.getRandomSelection(null, ID_TOKEN);

            authenticator.createIssuedJwtFromIdToken(idToken);

            assertNull("Issued JWT should have been null but was: [" + authenticator.getIssuedJwt() + "].", authenticator.getIssuedJwt());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createIssuedJwtFromIdToken_throwsException() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getJwtRef();
                    one(taiJwtUtils).createJwtTokenFromJson(with(any(String.class)), with(any(SocialLoginConfig.class)), with(any(Boolean.class)));
                    will(throwException(new Exception(defaultExceptionMsg)));
                }
            });

            // Token value shouldn't matter since the calls are mocked out, so choose a value at random
            String idToken = RandomUtils.getRandomSelection(null, ID_TOKEN);

            try {
                authenticator.createIssuedJwtFromIdToken(idToken);
                fail("Should have thrown exception but did not.");
            } catch (Exception e) {
                verifyException(e, Pattern.quote(defaultExceptionMsg));
            }

            assertNull("Issued JWT should have been null but was: [" + authenticator.getIssuedJwt() + "].", authenticator.getIssuedJwt());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createIssuedJwtFromIdToken_successful() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getJwtRef();
                    one(taiJwtUtils).createJwtTokenFromJson(with(any(String.class)), with(any(SocialLoginConfig.class)), with(any(Boolean.class)));
                    will(returnValue(jwtToken));
                }
            });

            // Token value shouldn't matter since the calls are mocked out, so choose a value at random
            String idToken = RandomUtils.getRandomSelection(null, ID_TOKEN);

            authenticator.createIssuedJwtFromIdToken(idToken);

            assertNotNull("Issued JWT should not have been null but was.", authenticator.getIssuedJwt());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createUserApiResponseFromIdToken ******************************************/

    @Test
    public void test_createUserApiResponseFromIdToken_nullToken() throws Exception {
        try {
            String idToken = null;

            authenticator.createUserApiResponseFromIdToken(idToken);

            assertNull("User API response should have been null but was: [" + authenticator.getUserApiResponse() + "].", authenticator.getUserApiResponse());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createUserApiResponseFromIdToken_invalidTokenFormat() throws Exception {
        try {
            String idToken = "some string";

            authenticator.createUserApiResponseFromIdToken(idToken);

            assertNull("User API response should have been null but was: [" + authenticator.getUserApiResponse() + "].", authenticator.getUserApiResponse());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createUserApiResponseFromIdToken_invalidToken() throws Exception { 
        try {
            String payload = "aGVsbG8g"; //hello
            String idToken = "xxx." + payload + ".zzz";
            String decodedPayload = new String(Base64Coder.base64Decode(payload.getBytes("UTF-8")), "UTF-8");

            authenticator.createUserApiResponseFromIdToken(idToken);

            String userApiResponse = authenticator.getUserApiResponse();
            System.out.println("*** decodedpayload, userapiresponse = >" + decodedPayload + "<>" + userApiResponse+"<");
            assertNotNull("User API response should not have been null but was.", userApiResponse);
            assertTrue("User API response did not match expected value.", decodedPayload.compareTo(userApiResponse)==0);            
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createUserApiResponseFromIdToken_validToken() throws Exception {
        try {
            String idToken = ID_TOKEN;

            authenticator.createUserApiResponseFromIdToken(idToken);

            String userApiResponse = authenticator.getUserApiResponse();
            assertNotNull("User API response should not have been null but was.", userApiResponse);

            String keyAndValue = "\"at_hash\":\"0HbzhW49bhEP2b3SVHfeGg\"";
            assertNotNull("User API response did not contain expected key and value [" + keyAndValue + "].", userApiResponse.contains(keyAndValue));
            keyAndValue = "\"sub\":\"user1\"";
            assertNotNull("User API response did not contain expected key and value [" + keyAndValue + "].", userApiResponse.contains(keyAndValue));
            keyAndValue = "\"realmName\":\"OpBasicRealm\"";
            assertNotNull("User API response did not contain expected key and value [" + keyAndValue + "].", userApiResponse.contains(keyAndValue));
            keyAndValue = "\"uniqueSecurityName\":\"user1\"";
            assertNotNull("User API response did not contain expected key and value [" + keyAndValue + "].", userApiResponse.contains(keyAndValue));
            keyAndValue = "\"iss\":\"https://localhost:8020/oidc/endpoint/OP\"";
            assertNotNull("User API response did not contain expected key and value [" + keyAndValue + "].", userApiResponse.contains(keyAndValue));
            keyAndValue = "\"nonce\":\"v1zg5OZ9vXP5h0lEiYs1\"";
            assertNotNull("User API response did not contain expected key and value [" + keyAndValue + "].", userApiResponse.contains(keyAndValue));
            keyAndValue = "\"aud\":\"rp\"";
            assertNotNull("User API response did not contain expected key and value [" + keyAndValue + "].", userApiResponse.contains(keyAndValue));
            keyAndValue = "\"exp\":1455909058";
            assertNotNull("User API response did not contain expected key and value [" + keyAndValue + "].", userApiResponse.contains(keyAndValue));
            keyAndValue = "\"iat\":1455901858";
            assertNotNull("User API response did not contain expected key and value [" + keyAndValue + "].", userApiResponse.contains(keyAndValue));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createExceptionAndLogMessage ******************************************/

    @Test
    public void test_createExceptionAndLogMessage_nullCause_emptyMessageKey() throws Exception {
        try {
            SocialLoginException exception = authenticator.createExceptionAndLogMessage(null, "", null);

            assertNull("Exception cause should have been null but was: [" + exception.getCause() + "].", exception.getCause());
            assertEquals("Exception message did not match expected value.", "", exception.getLocalizedMessage());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createExceptionAndLogMessage_withCause_emptyMessageKey() throws Exception {
        try {
            SocialLoginException exception = authenticator.createExceptionAndLogMessage(new Exception(defaultExceptionMsg), "", null);

            assertNotNull("Exception cause should not have been null but was.", exception.getCause());
            assertEquals("Exception cause message did not match expected value.", defaultExceptionMsg, exception.getCause().getLocalizedMessage());
            assertEquals("Exception message did not match expected value.", "", exception.getLocalizedMessage());

            // Exception cause message should not appear in the message log
            verifyNoLogMessage(outputMgr, Pattern.quote(defaultExceptionMsg));
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createExceptionAndLogMessage_keyNotInCatalog() throws Exception {
        try {
            SocialLoginException exception = authenticator.createExceptionAndLogMessage(null, defaultExceptionMsg, null);

            assertNull("Exception cause should have been null but was: [" + exception.getCause() + "].", exception.getCause());
            verifyException(exception, Pattern.quote(defaultExceptionMsg));

            // Exception message should appear in the message log
            verifyLogMessage(outputMgr, Pattern.quote(defaultExceptionMsg));
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createExceptionAndLogMessage_validKey_noInserts() throws Exception {
        try {
            SocialLoginException exception = authenticator.createExceptionAndLogMessage(null, "SOCIAL_LOGIN_ENDPOINT_SERVICE_ACTIVATED", null);

            assertNull("Exception cause should have been null but was: [" + exception.getCause() + "].", exception.getCause());
            verifyException(exception, CWWKS5407I_SOCIAL_LOGIN_ENDPOINT_SERVICE_ACTIVATED);

            verifyLogMessage(outputMgr, CWWKS5407I_SOCIAL_LOGIN_ENDPOINT_SERVICE_ACTIVATED);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createExceptionAndLogMessage_validKey_withInserts() throws Exception {
        try {
            final String insert = "Some insert";

            SocialLoginException exception = authenticator.createExceptionAndLogMessage(null, "URI_CONTAINS_INVALID_CHARS", new Object[] { insert });

            assertNull("Exception cause should have been null but was: [" + exception.getCause() + "].", exception.getCause());
            verifyExceptionWithInserts(exception, CWWKS5488W_URI_CONTAINS_INVALID_CHARS, insert);

            verifyLogMessageWithInserts(outputMgr, CWWKS5488W_URI_CONTAINS_INVALID_CHARS, insert);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** Helper methods ******************************************/

    private Map<String, Object> createTokens(String accessToken, String refreshToken, String idToken, Long expiresIn) {
        Map<String, Object> tokens = new HashMap<String, Object>();
        if (accessToken != null) {
            tokens.put(ClientConstants.ACCESS_TOKEN, accessToken);
        }
        if (refreshToken != null) {
            tokens.put(ClientConstants.REFRESH_TOKEN, refreshToken);
        }
        if (idToken != null) {
            tokens.put(ClientConstants.ID_TOKEN, idToken);
        }
        if (expiresIn != null) {
            tokens.put(ClientConstants.EXPIRES_IN, expiresIn);
        }
        return tokens;
    }

}
