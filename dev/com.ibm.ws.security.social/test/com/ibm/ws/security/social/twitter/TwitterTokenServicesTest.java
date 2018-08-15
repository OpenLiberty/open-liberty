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
package com.ibm.ws.security.social.twitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;

import test.common.SharedOutputManager;

public class TwitterTokenServicesTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    static final String OAUTH_TOKEN = "myOAuthToken";
    static final String OAUTH_TOKEN_SECRET = "myOAuthTokenSecret";
    static final String OAUTH_VERIFIER = "myOAuthVerifierValue";
    static final String ACCESS_TOKEN = "myAccessToken";
    static final String ACCESS_TOKEN_SECRET = "myAccessTokenSecret";
    static final String USER_ID = "myUserId";
    static final String SCREEN_NAME = "myScreenName";
    static final String EMAIL = "myEmail";

    final String CONSUMER_KEY = "myTwitterClient";
    final String CONSUMER_SECRET = "myTwitterConsumerSecret";
    final String CONFIG_ID = "twitterConfigId";
    final String INCOMING_REQUEST_URL = "https://localhost:8020/socialmedia/twitter";
    final String CALLBACK_URL = "https://localhost:8020/ibm/api/social-login/redirect/" + CONFIG_ID;
    final String STATE = "abcABCxyzXYZ";
    final String BASIC_URL = "https://www.example.com:80/context/path";

    final String MSG_ERROR_OBTAINING_ENDPOINT_RESULT = "CWWKS5419E";
    final String MSG_ERROR_PROCESSING_JSON_ENDPOINT_RESULT = "CWWKS5423E";
    final String MSG_REDIRECT_IOEXCEPTION = "CWWKS5420E";
    final String MSG_TOKEN_DOES_NOT_MATCH = "CWWKS5421E";
    final String MSG_REQUEST_MISSING_PARAMETER = "CWWKS5422E";
    final String MSG_RESPONSE_STATUS_MISSING = "CWWKS5423E";
    final String MSG_RESPONSE_FAILURE = "CWWKS5424E";

    static final String JSON_FAILURE_MSG = "JSON failure message. Something went wrong.";

    static Map<String, String> SUCCESSFUL_REQUEST_TOKEN_RESPONSE = new HashMap<String, String>();
    static Map<String, String> SUCCESSFUL_ACCESS_TOKEN_RESPONSE = new HashMap<String, String>();
    static Map<String, String> SUCCESSFUL_VERIFY_CREDENTIALS_RESPONSE = new HashMap<String, String>();
    static Map<String, String> UNSUCCESSFUL_RESPONSE = new HashMap<String, String>();
    static {
        SUCCESSFUL_REQUEST_TOKEN_RESPONSE.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);
        SUCCESSFUL_REQUEST_TOKEN_RESPONSE.put(TwitterConstants.RESPONSE_OAUTH_TOKEN, OAUTH_TOKEN);
        SUCCESSFUL_REQUEST_TOKEN_RESPONSE.put(TwitterConstants.RESPONSE_OAUTH_TOKEN_SECRET, OAUTH_TOKEN_SECRET);

        SUCCESSFUL_ACCESS_TOKEN_RESPONSE.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);
        SUCCESSFUL_ACCESS_TOKEN_RESPONSE.put(TwitterConstants.RESULT_ACCESS_TOKEN, ACCESS_TOKEN);
        SUCCESSFUL_ACCESS_TOKEN_RESPONSE.put(TwitterConstants.RESULT_ACCESS_TOKEN_SECRET, ACCESS_TOKEN_SECRET);
        SUCCESSFUL_ACCESS_TOKEN_RESPONSE.put(TwitterConstants.RESULT_USER_ID, USER_ID);
        SUCCESSFUL_ACCESS_TOKEN_RESPONSE.put(TwitterConstants.RESULT_SCREEN_NAME, SCREEN_NAME);

        SUCCESSFUL_VERIFY_CREDENTIALS_RESPONSE.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);
        SUCCESSFUL_VERIFY_CREDENTIALS_RESPONSE.put(TwitterConstants.RESULT_EMAIL, EMAIL);

        UNSUCCESSFUL_RESPONSE.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_ERROR);
        UNSUCCESSFUL_RESPONSE.put(TwitterConstants.RESULT_MESSAGE, JSON_FAILURE_MSG);
    }

    TwitterTokenServices tokenServices = new TwitterTokenServices() {
        @Override
        protected TwitterEndpointServices getTwitterEndpointServices() {
            return mockTwitter;
        }
    };

    TwitterTokenServices mockServices = new TwitterTokenServices() {
        @Override
        protected TwitterEndpointServices getTwitterEndpointServices() {
            return mockTwitter;
        }

        @Override
        protected boolean isSuccessfulResult(Map<String, Object> result, String endpoint) {
            return mockInterface.mockIsSuccessfulResult();
        }
    };

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    public interface MockInterface {
        public Map<String, Object> mockObtainRequestToken();

        public Map<String, Object> mockObtainAccessToken();

        public Map<String, Object> mockVerifyCredentials();

        public boolean mockIsSuccessfulResult();
    }

    @SuppressWarnings("serial")
    TwitterEndpointServices mockTwitter = new TwitterEndpointServices() {
        @Override
        public Map<String, Object> obtainRequestToken(SocialLoginConfig config, String callbackUrl) {
            return mockInterface.mockObtainRequestToken();
        }

        @Override
        public Map<String, Object> obtainAccessToken(SocialLoginConfig config, String requestToken, String verifierOrPinValue) {
            return mockInterface.mockObtainAccessToken();
        }

        @Override
        public Map<String, Object> verifyCredentials(SocialLoginConfig config, String accessToken, String accessTokenSecret) {
            return mockInterface.mockVerifyCredentials();
        }
    };

    final Cookie requestTokenCookie = mockery.mock(Cookie.class);
    final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class);
    final SocialWebUtils socialWebUtils = mockery.mock(SocialWebUtils.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());

        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);

        tokenServices = new TwitterTokenServices() {
            @Override
            protected TwitterEndpointServices getTwitterEndpointServices() {
                return mockTwitter;
            }
        };
        mockServices = new TwitterTokenServices() {
            @Override
            protected TwitterEndpointServices getTwitterEndpointServices() {
                return mockTwitter;
            }

            @Override
            protected boolean isSuccessfulResult(Map<String, Object> result, String endpoint) {
                return mockInterface.mockIsSuccessfulResult();
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();

        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(null);
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getRequestToken(...)}</li>
     * </ul>
     * Result from {@link TwitterTokenServices#obtainRequestToken(...)} call is null.
     */
    @Test
    public void testGetRequestToken_nullResult() {
        try {
            String callbackUrl = RandomUtils.getRandomSelection(null, CALLBACK_URL);
            getRequestTokenInitialExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainRequestToken();
                    will(returnValue(null));
                }
            });
            handleErrorExpectations();

            // Callback URL use gets mocked and state parameter does't come into play yet, so their values don't matter
            tokenServices.getRequestToken(request, response, callbackUrl, null, config);
            String logMsg = MSG_ERROR_OBTAINING_ENDPOINT_RESULT + ".*" + TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getRequestToken(...)}</li>
     * </ul>
     * Result from {@link TwitterTokenServices#obtainRequestToken(...)} call is empty.
     */
    @Test
    public void testGetRequestToken_emptyResult() {
        try {
            String callbackUrl = RandomUtils.getRandomSelection(null, CALLBACK_URL);
            getRequestTokenInitialExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainRequestToken();
                    will(returnValue(new HashMap<String, String>()));
                }
            });
            handleErrorExpectations();

            // Callback URL use gets mocked and state parameter does't come into play yet, so their values don't matter
            tokenServices.getRequestToken(request, response, callbackUrl, null, config);
            String logMsg = MSG_ERROR_OBTAINING_ENDPOINT_RESULT + ".*" + TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getRequestToken(...)}</li>
     * </ul>
     * Result from {@link TwitterTokenServices#obtainRequestToken(...)} call shows a failure.
     */
    @Test
    public void testGetRequestToken_failureResult() {
        try {
            String callbackUrl = RandomUtils.getRandomSelection(null, CALLBACK_URL);
            getRequestTokenInitialExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainRequestToken();
                    will(returnValue(UNSUCCESSFUL_RESPONSE));
                }
            });
            handleErrorExpectations();

            // Callback URL use gets mocked and state parameter does't come into play yet, so their values don't matter
            tokenServices.getRequestToken(request, response, callbackUrl, null, config);
            String logMsg = MSG_RESPONSE_FAILURE + ".*" + TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN + ".*" + JSON_FAILURE_MSG;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getRequestToken(...)}</li>
     * </ul>
     */
    @Test
    public void testGetRequestToken_missingAuthorizationEndpoint() {
        try {
            mockServices.webUtils = socialWebUtils;
            String callbackUrl = RandomUtils.getRandomSelection(null, CALLBACK_URL);
            final String url = RandomUtils.getRandomSelection(null, "");

            getRequestTokenInitialExpectations();
            setCookieExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainRequestToken();
                    will(returnValue(SUCCESSFUL_REQUEST_TOKEN_RESPONSE));
                    one(mockInterface).mockIsSuccessfulResult();
                    will(returnValue(true));
                    one(config).getAuthorizationEndpoint();
                    will(returnValue(url));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });
            handleErrorExpectations();

            // Callback URL use gets mocked, so its value doesn't matter
            mockServices.getRequestToken(request, response, callbackUrl, STATE, config);
            verifyLogMessage(outputMgr, CWWKS5447E_FAILED_TO_REDIRECT_TO_AUTHZ_ENDPOINT + ".*\\[" + uniqueId + "\\].*" + CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getRequestToken(...)}</li>
     * </ul>
     */
    @Test
    public void testGetRequestToken_invalidAuthorizationEndpoint() {
        try {
            mockServices.webUtils = socialWebUtils;
            String callbackUrl = RandomUtils.getRandomSelection(null, CALLBACK_URL);
            final String url = "Some invalid URL";

            getRequestTokenInitialExpectations();
            setCookieExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainRequestToken();
                    will(returnValue(SUCCESSFUL_REQUEST_TOKEN_RESPONSE));
                    one(mockInterface).mockIsSuccessfulResult();
                    will(returnValue(true));
                    one(config).getAuthorizationEndpoint();
                    will(returnValue(url));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });
            handleErrorExpectations();

            // Callback URL use gets mocked, so its value doesn't matter
            mockServices.getRequestToken(request, response, callbackUrl, STATE, config);
            verifyLogMessage(outputMgr, CWWKS5447E_FAILED_TO_REDIRECT_TO_AUTHZ_ENDPOINT + ".*\\[" + uniqueId + "\\].*" + CWWKS5417E_EXCEPTION_INITIALIZING_URL + ".*\\[" + url + "\\]");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getRequestToken(...)}</li>
     * </ul>
     * IOException is thrown while sending the redirect.
     */
    @Test
    public void testGetRequestToken_IOException() {
        try {
            mockServices.webUtils = socialWebUtils;
            String callbackUrl = RandomUtils.getRandomSelection(null, CALLBACK_URL);
            final String errorMsg = "This is an IOException message.";

            getRequestTokenInitialExpectations();
            setCookieExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainRequestToken();
                    will(returnValue(SUCCESSFUL_REQUEST_TOKEN_RESPONSE));
                    one(mockInterface).mockIsSuccessfulResult();
                    will(returnValue(true));
                    one(config).getAuthorizationEndpoint();
                    will(returnValue(BASIC_URL));
                    one(response).sendRedirect(BASIC_URL + "?" + TwitterConstants.PARAM_OAUTH_TOKEN + "=" + OAUTH_TOKEN);
                    will(throwException(new IOException(errorMsg)));
                }
            });
            handleErrorExpectations(HttpServletResponse.SC_BAD_REQUEST);

            // Callback URL use gets mocked, so its value doesn't matter
            mockServices.getRequestToken(request, response, callbackUrl, STATE, config);
            String logMsg = MSG_REDIRECT_IOEXCEPTION + ".*" + TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN + ".*" + errorMsg;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getRequestToken(...)}</li>
     * </ul>
     */
    @Test
    public void testGetRequestToken_successfulResult() {
        try {
            mockServices.webUtils = socialWebUtils;
            String callbackUrl = RandomUtils.getRandomSelection(null, CALLBACK_URL);

            getRequestTokenInitialExpectations();
            setCookieExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainRequestToken();
                    will(returnValue(SUCCESSFUL_REQUEST_TOKEN_RESPONSE));
                    one(mockInterface).mockIsSuccessfulResult();
                    will(returnValue(true));
                    one(config).getAuthorizationEndpoint();
                    will(returnValue(BASIC_URL));
                    one(response).sendRedirect(BASIC_URL + "?" + TwitterConstants.PARAM_OAUTH_TOKEN + "=" + OAUTH_TOKEN);
                }
            });

            // Callback URL use gets mocked, so its value doesn't matter
            mockServices.getRequestToken(request, response, callbackUrl, STATE, config);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getAccessToken(...)}</li>
     * </ul>
     * Request contained an empty parameter map.
     */
    @Test
    public void testGetAccessToken_emptyParameterMap() {
        try {
            getAccessTokenInitialExpectations();
            mockery.checking(new Expectations() {
                {
                    allowing(request).getParameterMap();
                    will(returnValue(new HashMap<String, String>()));
                }
            });

            Map<String, Object> result = tokenServices.getAccessToken(request, response, config);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            String logMsg = MSG_REQUEST_MISSING_PARAMETER + ".*" + TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN + ".*" + TwitterConstants.PARAM_OAUTH_TOKEN;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getAccessToken(...)}</li>
     * </ul>
     * Request is missing {@value TwitterConstants#PARAM_OAUTH_VERIFIER} parameter.
     */
    @Test
    public void testGetAccessToken_missingVerifierParameter() {
        try {
            final Map<String, String> reqParams = new HashMap<String, String>();
            reqParams.put(TwitterConstants.PARAM_OAUTH_TOKEN, OAUTH_TOKEN);

            getAccessTokenInitialExpectations();
            mockery.checking(new Expectations() {
                {
                    allowing(request).getParameterMap();
                    will(returnValue(reqParams));
                }
            });

            Map<String, Object> result = tokenServices.getAccessToken(request, response, config);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            String logMsg = MSG_REQUEST_MISSING_PARAMETER + ".*" + TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN + ".*" + TwitterConstants.PARAM_OAUTH_VERIFIER;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getAccessToken(...)}</li>
     * </ul>
     * {@value TwitterConstants#PARAM_OAUTH_TOKEN} parameter in request does not match previously obtained request token.
     */
    @Test
    public void testGetAccessToken_tokenDoesNotMatchRequestToken() {
        try {
            final String badToken = OAUTH_TOKEN + "doesNotMatch";
            final Map<String, String> reqParams = getAccessTokenParams();
            reqParams.put(TwitterConstants.PARAM_OAUTH_TOKEN, badToken);

            final Cookie[] cookies = new Cookie[] { requestTokenCookie };

            getAccessTokenInitialExpectations();
            mockery.checking(new Expectations() {
                {
                    allowing(request).getParameterMap();
                    will(returnValue(reqParams));
                    one(request).getParameter(TwitterConstants.PARAM_OAUTH_TOKEN);
                    will(returnValue(badToken));

                    // Get the request token cookie
                    allowing(request).getCookies();
                    will(returnValue(cookies));
                    allowing(requestTokenCookie).getName();
                    will(returnValue(TwitterConstants.COOKIE_NAME_REQUEST_TOKEN));
                    // Previously obtained request token should not match the token in the request parameters
                    allowing(requestTokenCookie).getValue();
                    will(returnValue(OAUTH_TOKEN));
                    // Clear out the request token cookie
                    one(requestTokenCookie).getPath();
                    one(requestTokenCookie).getSecure();
                    one(response).addCookie(with(any(Cookie.class)));
                }
            });

            Map<String, Object> result = tokenServices.getAccessToken(request, response, config);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            String logMsg = MSG_TOKEN_DOES_NOT_MATCH;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getAccessToken(...)}</li>
     * </ul>
     * Result from {@link TwitterEndpointServices#obtainAccessToken(...)} call is null.
     */
    @Test
    public void testGetAccessToken_nullResult() {
        try {
            getAccessTokenInitialExpectations();
            accessTokenGoodParamExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainAccessToken();
                    will(returnValue(null));
                }
            });

            Map<String, Object> result = tokenServices.getAccessToken(request, response, config);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            String logMsg = MSG_ERROR_OBTAINING_ENDPOINT_RESULT + ".*" + TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getAccessToken(...)}</li>
     * </ul>
     * Result from {@link TwitterEndpointServices#obtainAccessToken(...)} call is empty.
     */
    @Test
    public void testGetAccessToken_emptyResult() {
        try {
            getAccessTokenInitialExpectations();
            accessTokenGoodParamExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainAccessToken();
                    will(returnValue(new HashMap<String, String>()));
                }
            });

            Map<String, Object> result = tokenServices.getAccessToken(request, response, config);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            String logMsg = MSG_ERROR_OBTAINING_ENDPOINT_RESULT + ".*" + TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getAccessToken(...)}</li>
     * </ul>
     * Result from {@link TwitterEndpointServices#obtainAccessToken(...)} call shows a failure.
     */
    @Test
    public void testGetAccessToken_failureResult() {
        try {
            getAccessTokenInitialExpectations();
            accessTokenGoodParamExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainAccessToken();
                    will(returnValue(UNSUCCESSFUL_RESPONSE));
                }
            });

            Map<String, Object> result = tokenServices.getAccessToken(request, response, config);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            String logMsg = MSG_RESPONSE_FAILURE + ".*" + TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN + ".*" + JSON_FAILURE_MSG;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#getAccessToken(...)}</li>
     * </ul>
     * Result from {@link TwitterEndpointServices#obtainAccessToken(...)} call is successful.
     */
    @Test
    public void testGetAccessToken_successfulResult() {
        try {
            getAccessTokenInitialExpectations();
            accessTokenGoodParamExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockObtainAccessToken();
                    will(returnValue(SUCCESSFUL_ACCESS_TOKEN_RESPONSE));
                }
            });

            Map<String, Object> result = tokenServices.getAccessToken(request, response, config);
            assertEquals("Access token in result did not match expected result.", ACCESS_TOKEN, result.get(TwitterConstants.RESULT_ACCESS_TOKEN));
            assertEquals("Access token secret in result did not match expected result.", ACCESS_TOKEN_SECRET, result.get(TwitterConstants.RESULT_ACCESS_TOKEN_SECRET));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#verifyCredentials(...)}</li>
     * </ul>
     * Result from {@link TwitterEndpointServices#verifyCredentials(String, String)} call is null or empty.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testVerifyCredentials_nullOrEmptyResult() {
        try {
            getVerifyCredentialsInitialExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockVerifyCredentials();
                    will(returnValue(RandomUtils.getRandomSelection(null, new HashMap<String, String>())));
                }
            });

            Map<String, Object> result = tokenServices.verifyCredentials(response, ACCESS_TOKEN, ACCESS_TOKEN_SECRET, config);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            String logMsg = MSG_ERROR_OBTAINING_ENDPOINT_RESULT + ".*" + Pattern.quote(TwitterConstants.TWITTER_ENDPOINT_VERIFY_CREDENTIALS);
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#verifyCredentials(...)}</li>
     * </ul>
     * Result from {@link TwitterEndpointServices#verifyCredentials(...)} call shows unsuccessful result.
     */
    @Test
    public void testVerifyCredentials_failureResult() {
        try {
            getVerifyCredentialsInitialExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockVerifyCredentials();
                    will(returnValue(UNSUCCESSFUL_RESPONSE));
                }
            });

            Map<String, Object> result = tokenServices.verifyCredentials(response, ACCESS_TOKEN, ACCESS_TOKEN_SECRET, config);
            assertNull("Result was not null when it should have been. Result: " + result, result);

            String logMsg = MSG_RESPONSE_FAILURE + ".*" + Pattern.quote(TwitterConstants.TWITTER_ENDPOINT_VERIFY_CREDENTIALS) + ".*" + JSON_FAILURE_MSG;
            verifyLogMessage(outputMgr, logMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#verifyCredentials(...)}</li>
     * </ul>
     * Result from {@link TwitterEndpointServices#verifyCredentials(...)} call shows successful result.
     */
    @Test
    public void testVerifyCredentials_successfulResult() {
        try {
            getVerifyCredentialsInitialExpectations();
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).mockVerifyCredentials();
                    will(returnValue(SUCCESSFUL_VERIFY_CREDENTIALS_RESPONSE));
                }
            });

            Map<String, Object> result = tokenServices.verifyCredentials(response, ACCESS_TOKEN, ACCESS_TOKEN_SECRET, config);

            assertEquals("Access token in result did not match expected result.", ACCESS_TOKEN, result.get(ClientConstants.ACCESS_TOKEN));
            assertEquals("Email in result did not match expected value.", EMAIL, result.get(ClientConstants.EMAIL));
            // Result size should be all of the unique entries in SUCCESSFUL_VERIFY_CREDENTIALS_RESPONSE + an added entry for the access token
            assertEquals("Number of entries in the results map did not match expected value.", SUCCESSFUL_VERIFY_CREDENTIALS_RESPONSE.size() + 1, result.size());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li>{@link TwitterTokenServices#isSuccessfulResult(...)}</li>
     * </ul>
     */
    @Test
    public void testIsSuccessfulResult() {
        try {
            String endpoint = TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN;
            assertFalse(tokenServices.isSuccessfulResult(null, endpoint));
            String logMsg = MSG_ERROR_OBTAINING_ENDPOINT_RESULT + ".*" + endpoint;
            verifyLogMessage(outputMgr, logMsg);

            // Empty result
            Map<String, Object> result = new HashMap<String, Object>();
            endpoint = null;
            assertFalse(tokenServices.isSuccessfulResult(result, endpoint));
            logMsg = MSG_RESPONSE_STATUS_MISSING + ".*" + endpoint;
            verifyLogMessage(outputMgr, logMsg);

            // Response status != success, no error message
            result.put(TwitterConstants.RESULT_RESPONSE_STATUS, "Not" + TwitterConstants.RESULT_SUCCESS);
            endpoint = RandomUtils.getRandomSelection(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN);
            assertFalse(tokenServices.isSuccessfulResult(result, endpoint));
            logMsg = MSG_RESPONSE_FAILURE + ".*" + endpoint;
            verifyLogMessage(outputMgr, logMsg);

            // Response status != success, with error message
            String errorMsg = "This is what went wrong.";
            result.put(TwitterConstants.RESULT_RESPONSE_STATUS, "Not" + TwitterConstants.RESULT_SUCCESS);
            result.put(TwitterConstants.RESULT_MESSAGE, errorMsg);
            endpoint = RandomUtils.getRandomSelection(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN);
            assertFalse(tokenServices.isSuccessfulResult(result, endpoint));
            logMsg = MSG_RESPONSE_FAILURE + ".*" + endpoint + ".*" + errorMsg;
            verifyLogMessage(outputMgr, logMsg);

            // Response status == success, with error message (message should be ignored)
            result.put(TwitterConstants.RESULT_RESPONSE_STATUS, "Not" + TwitterConstants.RESULT_SUCCESS);
            result.put(TwitterConstants.RESULT_MESSAGE, errorMsg);
            result.put(TwitterConstants.RESULT_RESPONSE_STATUS, TwitterConstants.RESULT_SUCCESS);
            endpoint = RandomUtils.getRandomSelection(TwitterConstants.TWITTER_ENDPOINT_REQUEST_TOKEN, TwitterConstants.TWITTER_ENDPOINT_ACCESS_TOKEN);
            assertTrue(tokenServices.isSuccessfulResult(result, endpoint));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** Helper methods ****************************************/

    private Map<String, String> getAccessTokenParams() {
        Map<String, String> params = new HashMap<String, String>();
        params.put(TwitterConstants.PARAM_OAUTH_TOKEN, OAUTH_TOKEN);
        params.put(TwitterConstants.PARAM_OAUTH_VERIFIER, OAUTH_VERIFIER);
        return params;
    }

    private void getRequestTokenInitialExpectations() {
        getClientConfigExpectations();
    }

    private void getAccessTokenInitialExpectations() {
        getClientConfigExpectations();
    }

    private void getVerifyCredentialsInitialExpectations() {
        getClientConfigExpectations();
    }

    private void getClientConfigExpectations() {
        mockery.checking(new Expectations() {
            {
                one(config).getClientId();
                will(returnValue(CONSUMER_KEY));
                one(config).getClientSecret();
                will(returnValue(CONSUMER_SECRET));
            }
        });
    }

    private void setCookieExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(webAppSecConfig).getHttpOnlyCookies();
                will(returnValue(false));
                allowing(webAppSecConfig).getSSORequiresSSL();
                will(returnValue(false));
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecConfig)));
                one(response).addCookie(with(any(Cookie.class)));
                one(socialWebUtils).getRequestUrlWithEncodedQueryString(request);
                will(returnValue(INCOMING_REQUEST_URL));
                one(response).addCookie(with(any(Cookie.class)));
            }
        });
    }

    private void accessTokenGoodParamExpectations() {
        final Map<String, String> requestParams = getAccessTokenParams();
        // Previously obtained request token should match the one in the request parameter
        final Cookie[] cookies = new Cookie[] { requestTokenCookie };
        mockery.checking(new Expectations() {
            {
                allowing(request).getParameterMap();
                will(returnValue(requestParams));
                one(request).getParameter(TwitterConstants.PARAM_OAUTH_TOKEN);
                will(returnValue(OAUTH_TOKEN));
                one(request).getParameter(TwitterConstants.PARAM_OAUTH_VERIFIER);
                will(returnValue(OAUTH_VERIFIER));
                // Get the request token cookie
                allowing(request).getCookies();
                will(returnValue(cookies));
                allowing(requestTokenCookie).getName();
                will(returnValue(TwitterConstants.COOKIE_NAME_REQUEST_TOKEN));
                // Previously obtained request token should match the token in the request parameters
                allowing(requestTokenCookie).getValue();
                will(returnValue(OAUTH_TOKEN));

                // Clear out the request token cookie
                one(requestTokenCookie).getPath();
                one(requestTokenCookie).getSecure();
                one(response).addCookie(with(any(Cookie.class)));
            }
        });
    }

}
