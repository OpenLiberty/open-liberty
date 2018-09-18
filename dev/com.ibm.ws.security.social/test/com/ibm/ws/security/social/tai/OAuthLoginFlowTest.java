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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.common.web.CommonWebConstants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.wsspi.security.tai.TAIResult;

import test.common.SharedOutputManager;

public class OAuthLoginFlowTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private static final String AUTHZ_CODE = "1234567890";
    private final String clientId = "12345";
    private final String authorizationEndpointURL = "https://www.example.com/oauth/authorize";
    private final String redirectToRPHostAndPort = "https://localhost:8021";
    private final String uniqueId = "facebookLogin";
    private final String redirectUri = "https://localhost:8021/ibm/api/social-login/redirect/facebookLogin";
    private final String clientConfigScope = "email user_friends public_profile user_about_me";
    private final String state = "abcABCxyzXYZ";
    private final String acrValues = "acrValues\u1234\u4321 + ";

    final static String successfulTAIPrinciple = "myPrinciple";

    final static String REGEX_URL_ENCODED_CHARS = "[a-zA-Z0-9%\\+\\._-]";

    final static String USERNAME_ATTRIBUTE = "userNameAttribute";

    public interface MockInterface {
        TAIResult handleRedirectToServer() throws WebTrustAssociationFailedException;

        TAIResult handleAuthorizationCode() throws WebTrustAssociationFailedException;

        public String buildAuthorizationUrlWithQuery() throws SocialLoginException;
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    private final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class, "mockSocialLoginConfig");
    private final TAIWebUtils taiWebUtils = mockery.mock(TAIWebUtils.class);
    private final SocialWebUtils socialWebUtils = mockery.mock(SocialWebUtils.class);
    private final TAISubjectUtils taiSubjectUtils = mockery.mock(TAISubjectUtils.class);
    private final AuthorizationCodeAuthenticator authzCodeAuthenticator = mockery.mock(AuthorizationCodeAuthenticator.class);
    private final ReferrerURLCookieHandler referrerURLCookieHandler = mockery.mock(ReferrerURLCookieHandler.class);
    private final Cookie cookie = mockery.mock(Cookie.class);

    OAuthLoginFlow loginFlow = new MockOAuthLoginFlow();

    class MockOAuthLoginFlow extends OAuthLoginFlow {
        AuthorizationCodeAuthenticator getAuthorizationCodeAuthenticator(HttpServletRequest req, HttpServletResponse res, String authzCode, SocialLoginConfig clientConfig) {
            return authzCodeAuthenticator;
        }

        TAISubjectUtils getTAISubjectUtils(AuthorizationCodeAuthenticator authzCodeAuthenticator) {
            return taiSubjectUtils;
        }
    }

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());

        loginFlow = new MockOAuthLoginFlow();
        mockProtectedClassMembers(loginFlow);

        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
        mockery.checking(new Expectations() {
            {
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(referrerURLCookieHandler));
            }
        });
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

    private void mockProtectedClassMembers(OAuthLoginFlow loginFlow) {
        loginFlow.taiWebUtils = taiWebUtils;
        loginFlow.webUtils = socialWebUtils;
    }

    /****************************************** handleOAuthRequest ******************************************/

    @Test
    public void handleOAuthRequest_missingCode() throws Exception {
        OAuthLoginFlow loginFlow = new MockOAuthLoginFlow() {
            @Override
            TAIResult handleRedirectToServer(HttpServletRequest req, HttpServletResponse res, SocialLoginConfig clientConfig) throws WebTrustAssociationFailedException {
                return mockInterface.handleRedirectToServer();
            }
        };
        mockProtectedClassMembers(loginFlow);

        final TAIResult successfulTAIResult = TAIResult.create(HttpServletResponse.SC_OK, successfulTAIPrinciple);
        try {
            mockery.checking(new Expectations() {
                {
                    one(socialWebUtils).getAndClearCookie(request, response, ClientConstants.COOKIE_NAME_STATE_KEY);
                    will(returnValue(null));
                    one(mockInterface).handleRedirectToServer();
                    will(returnValue(successfulTAIResult));
                }
            });

            TAIResult result = loginFlow.handleOAuthRequest(request, response, config);
            assertSuccesfulTAIResult(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleOAuthRequest_includesCode() throws Exception {
        OAuthLoginFlow loginFlow = new MockOAuthLoginFlow() {
            @Override
            TAIResult handleAuthorizationCode(HttpServletRequest req, HttpServletResponse res, String authzCode, SocialLoginConfig clientConfig) throws WebTrustAssociationFailedException {
                return mockInterface.handleAuthorizationCode();
            }
        };
        mockProtectedClassMembers(loginFlow);

        final TAIResult successfulTAIResult = TAIResult.create(HttpServletResponse.SC_OK, successfulTAIPrinciple);
        try {
            mockery.checking(new Expectations() {
                {
                    one(socialWebUtils).getAndClearCookie(request, response, ClientConstants.COOKIE_NAME_STATE_KEY);
                    will(returnValue(AUTHZ_CODE));
                    one(mockInterface).handleAuthorizationCode();
                    will(returnValue(successfulTAIResult));
                }
            });

            TAIResult result = loginFlow.handleOAuthRequest(request, response, config);
            assertSuccesfulTAIResult(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** handleRedirectToServer ******************************************/

    @Test
    public void handleRedirectToServer_SocialLoginException() throws Exception {
        OAuthLoginFlow loginFlow = new MockOAuthLoginFlow() {
            @Override
            protected String buildAuthorizationUrlWithQuery(String state, SocialLoginConfig clientConfig, String redirect_url, String acr_values) throws SocialLoginException {
                return mockInterface.buildAuthorizationUrlWithQuery();
            }
        };
        mockProtectedClassMembers(loginFlow);

        int statusCode = HttpServletResponse.SC_FORBIDDEN;
        final TAIResult unsuccessfulResult = TAIResult.create(statusCode);
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getSession(true);
                    one(taiWebUtils).createStateCookie(request, response);
                    one(taiWebUtils).getRedirectUrl(request, config);
                    one(taiWebUtils).savePostParameters(request);
                    one(request).getParameter("acr_values");
                    one(mockInterface).buildAuthorizationUrlWithQuery();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(taiWebUtils).sendToErrorPage(with(any(HttpServletResponse.class)), with(any(TAIResult.class)));
                    will(returnValue(unsuccessfulResult));
                }
            });

            TAIResult result = loginFlow.handleRedirectToServer(request, response, config);
            assertResultStatus(statusCode, result);

            String expectedMsg = CWWKS5447E_FAILED_TO_REDIRECT_TO_AUTHZ_ENDPOINT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg);
            verifyLogMessage(outputMgr, expectedMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleRedirectToServer_IOException() throws Exception {
        OAuthLoginFlow loginFlow = new MockOAuthLoginFlow() {
            @Override
            protected String buildAuthorizationUrlWithQuery(String state, SocialLoginConfig clientConfig, String redirect_url, String acr_values) throws SocialLoginException {
                return mockInterface.buildAuthorizationUrlWithQuery();
            }
        };
        mockProtectedClassMembers(loginFlow);

        int statusCode = HttpServletResponse.SC_FORBIDDEN;
        final TAIResult unsuccessfulResult = TAIResult.create(statusCode);
        try {
            createAndAddCookieExpectations();

            mockery.checking(new Expectations() {
                {
                    one(request).getSession(true);
                    one(taiWebUtils).createStateCookie(request, response);
                    one(taiWebUtils).getRedirectUrl(request, config);
                    one(taiWebUtils).savePostParameters(request);
                    one(request).getParameter("acr_values");
                    one(mockInterface).buildAuthorizationUrlWithQuery();
                    will(returnValue(authorizationEndpointURL));
                    one(config).isClientSideRedirectSupported();
                    will(returnValue(false));
                    one(socialWebUtils).getRequestUrlWithEncodedQueryString(request);
                    will(returnValue(redirectToRPHostAndPort));
                    one(response).sendRedirect(authorizationEndpointURL);
                    will(throwException(new IOException(defaultExceptionMsg)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(taiWebUtils).sendToErrorPage(with(any(HttpServletResponse.class)), with(any(TAIResult.class)));
                    will(returnValue(unsuccessfulResult));
                }
            });

            TAIResult result = loginFlow.handleRedirectToServer(request, response, config);
            assertResultStatus(statusCode, result);

            String expectedMsg = CWWKS5447E_FAILED_TO_REDIRECT_TO_AUTHZ_ENDPOINT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg);
            verifyLogMessage(outputMgr, expectedMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleRedirectToServer_clientSideRedirectNotSupported() throws Exception {
        OAuthLoginFlow loginFlow = new MockOAuthLoginFlow() {
            @Override
            protected String buildAuthorizationUrlWithQuery(String state, SocialLoginConfig clientConfig, String redirect_url, String acr_values) throws SocialLoginException {
                return mockInterface.buildAuthorizationUrlWithQuery();
            }
        };
        mockProtectedClassMembers(loginFlow);

        try {
            createAndAddCookieExpectations();

            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).savePostParameters(request);
                    one(request).getSession(true);
                    one(taiWebUtils).createStateCookie(request, response);
                    will(returnValue(state));
                    one(taiWebUtils).getRedirectUrl(request, config);
                    one(request).getParameter("acr_values");
                    one(mockInterface).buildAuthorizationUrlWithQuery();
                    will(returnValue(authorizationEndpointURL));
                    one(config).isClientSideRedirectSupported();
                    will(returnValue(false));
                    one(socialWebUtils).getRequestUrlWithEncodedQueryString(request);
                    will(returnValue(redirectToRPHostAndPort));
                    one(response).sendRedirect(authorizationEndpointURL);
                }
            });
            // 240842 handleErrorExpectations(HttpServletResponse.SC_FORBIDDEN);

            TAIResult result = loginFlow.handleRedirectToServer(request, response, config);
            assertResultStatus(HttpServletResponse.SC_FORBIDDEN, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleRedirectToServer_clientSideRedirectSupported() throws Exception {
        OAuthLoginFlow loginFlow = new MockOAuthLoginFlow() {
            @Override
            protected String buildAuthorizationUrlWithQuery(String state, SocialLoginConfig clientConfig, String redirect_url, String acr_values) throws SocialLoginException {
                return mockInterface.buildAuthorizationUrlWithQuery();
            }
        };
        mockProtectedClassMembers(loginFlow);

        try {
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).savePostParameters(request);
                    one(request).getSession(true);
                    one(taiWebUtils).createStateCookie(request, response);
                    will(returnValue(state));
                    one(taiWebUtils).getRedirectUrl(request, config);
                    one(request).getParameter("acr_values");
                    one(mockInterface).buildAuthorizationUrlWithQuery();
                    will(returnValue(authorizationEndpointURL));
                    one(config).isClientSideRedirectSupported();
                    will(returnValue(true));
                    one(socialWebUtils).doClientSideRedirect(with(any(HttpServletResponse.class)), with(any(String.class)), with(any(String.class)));
                }
            });
            // 240842 handleErrorExpectations(HttpServletResponse.SC_FORBIDDEN);

            // Actual runtime would redirect the browser, however the unit test will continue executing the method, which will return a 403 result
            TAIResult result = loginFlow.handleRedirectToServer(request, response, config);
            assertResultStatus(HttpServletResponse.SC_FORBIDDEN, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** buildAuthorizationUrlWithQuery ******************************************/

    @Test
    public void buildAuthorizationUrlWithQuery_nullState() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });
            try {
                String result = loginFlow.buildAuthorizationUrlWithQuery(null, config, redirectUri, acrValues);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5448E_STATE_IS_NULL, uniqueId);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildAuthorizationUrlWithQuery_nullRedirectUrl() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });
            try {
                String result = loginFlow.buildAuthorizationUrlWithQuery(state, config, null, acrValues);
                fail("Should have thrown SocialLoginException but did not. Got result: " + result);
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5449E_REDIRECT_URL_IS_NULL, uniqueId);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildAuthorizationUrlWithQuery_emptyValues() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authorizationEndpointURL));
                }
            });
            authorizationEndpointQueryExpectations("code", "", "", "", false);

            String result = loginFlow.buildAuthorizationUrlWithQuery("", config, "", "");

            // Verify that only subset of ASCII characters are in result
            verifyAuthzEndpointFormat(result, authorizationEndpointURL);
            // Verify each param value matches its mocked value
            verifyPattern(result, "response_type=code&");
            verifyPattern(result, "client_id=&");
            verifyPattern(result, "state=&");
            verifyPattern(result, "redirect_uri=&");
            verifyPattern(result, "scope=&");
            verifyPattern(result, "response_mode=$");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildAuthorizationUrlWithQuery_nullClientId() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authorizationEndpointURL));
                }
            });
            authorizationEndpointQueryExpectations("code", null, null, null, false);

            String result = loginFlow.buildAuthorizationUrlWithQuery("", config, redirectUri, null);

            // Verify that only subset of ASCII characters are in result
            verifyAuthzEndpointFormat(result, authorizationEndpointURL);
            // Verify each param value matches its mocked value
            verifyPattern(result, "response_type=code&");
            verifyPattern(result, "client_id=&");
            verifyPattern(result, "state=&");
            verifyPattern(result, "redirect_uri=" + Pattern.quote(URLEncoder.encode(redirectUri, CommonWebConstants.UTF_8)) + "&");
            verifyPattern(result, "scope=$");

            verifyLogMessageWithInserts(outputMgr, CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER, ClientConstants.CLIENT_ID);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildAuthorizationUrlWithQuery_withNonce() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authorizationEndpointURL));
                }
            });
            authorizationEndpointQueryExpectations("code", null, clientId, clientConfigScope, true);

            String result = loginFlow.buildAuthorizationUrlWithQuery(state, config, redirectUri, null);

            // Verify that only subset of ASCII characters are in result
            verifyAuthzEndpointFormat(result, authorizationEndpointURL);
            // Verify each param value matches its mocked value
            verifyPattern(result, "response_type=code&");
            verifyPattern(result, "client_id=" + clientId + "&");
            verifyPattern(result, "state=" + state + "&");
            verifyPattern(result, "redirect_uri=" + REGEX_URL_ENCODED_CHARS + "+&");
            verifyPattern(result, "scope=" + REGEX_URL_ENCODED_CHARS + "+&");
            verifyPattern(result, "nonce=" + "[a-zA-Z0-9]" + "+$");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildAuthorizationUrlWithQuery_withAcr() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authorizationEndpointURL));
                }
            });
            authorizationEndpointQueryExpectations("code", null, clientId, clientConfigScope, false);

            String result = loginFlow.buildAuthorizationUrlWithQuery(state, config, redirectUri, acrValues);

            // Verify that only subset of ASCII characters are in result
            verifyAuthzEndpointFormat(result, authorizationEndpointURL);
            // Verify each param value matches its mocked value
            verifyPattern(result, "response_type=code&?");
            verifyPattern(result, "client_id=" + clientId + "&");
            verifyPattern(result, "state=" + state + "&");
            verifyPattern(result, "redirect_uri=" + REGEX_URL_ENCODED_CHARS + "+&");
            verifyPattern(result, "scope=" + REGEX_URL_ENCODED_CHARS + "+&");
            verifyPattern(result, "acr_values=" + REGEX_URL_ENCODED_CHARS + "+$");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildAuthorizationUrlWithQuery_responseTypeNotCode() throws Exception {
        try {
            final String responseType = "id_token";
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authorizationEndpointURL));
                }
            });
            authorizationEndpointQueryExpectations(responseType, null, clientId, clientConfigScope, false);
            mockery.checking(new Expectations() {
                {
                    one(config).getResource();
                    will(returnValue(null));
                }
            });

            String result = loginFlow.buildAuthorizationUrlWithQuery(state, config, redirectUri, null);

            // Verify that only subset of ASCII characters are in result
            verifyAuthzEndpointFormat(result, authorizationEndpointURL);
            // Verify each param value matches its mocked value
            verifyPattern(result, "response_type=" + responseType + "&");
            verifyPattern(result, "client_id=" + clientId + "&");
            verifyPattern(result, "state=" + state + "&");
            verifyPattern(result, "redirect_uri=" + REGEX_URL_ENCODED_CHARS + "+&");
            verifyPattern(result, "scope=" + REGEX_URL_ENCODED_CHARS + "+&");
            verifyPattern(result, "response_mode=form_post$");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void buildAuthorizationUrlWithQuery_responseTypeNotCode_withResource() throws Exception {
        try {
            final String responseType = "id_token";
            final String resource = "The quick brown fox jumps over<script>alert(100)</script> the lazy dog.";
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).getAuthorizationEndpoint(config);
                    will(returnValue(authorizationEndpointURL));
                }
            });
            authorizationEndpointQueryExpectations(responseType, null, clientId, clientConfigScope, false);
            mockery.checking(new Expectations() {
                {
                    one(config).getResource();
                    will(returnValue(resource));
                }
            });

            String result = loginFlow.buildAuthorizationUrlWithQuery(state, config, redirectUri, null);

            // Verify that only subset of ASCII characters are in result
            verifyAuthzEndpointFormat(result, authorizationEndpointURL);
            // Verify each param value matches its mocked value
            verifyPattern(result, "response_type=" + responseType + "&");
            verifyPattern(result, "client_id=" + clientId + "&");
            verifyPattern(result, "state=" + state + "&");
            verifyPattern(result, "redirect_uri=" + REGEX_URL_ENCODED_CHARS + "+&");
            verifyPattern(result, "scope=" + REGEX_URL_ENCODED_CHARS + "+&");
            verifyPattern(result, "response_mode=form_post&");
            verifyPattern(result, "&" + REGEX_URL_ENCODED_CHARS + "+$");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** handleAuthorizationCode ******************************************/

    @Test
    public void handleAuthorizationCode_generateTokensThrowsException() throws Exception {
        int statusCode = HttpServletResponse.SC_UNAUTHORIZED;
        final TAIResult unsuccessfulResult = TAIResult.create(statusCode);
        try {
            mockery.checking(new Expectations() {
                {
                    one(authzCodeAuthenticator).generateJwtAndTokenInformation();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                    one(taiWebUtils).sendToErrorPage(with(any(HttpServletResponse.class)), with(any(TAIResult.class)));
                    will(returnValue(unsuccessfulResult));
                }
            });

            TAIResult result = loginFlow.handleAuthorizationCode(request, response, AUTHZ_CODE, config);
            assertResultStatus(statusCode, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleAuthorizationCode_successful() throws Exception {
        try {
            final TAIResult successfulTAIResult = TAIResult.create(HttpServletResponse.SC_OK, successfulTAIPrinciple);

            mockery.checking(new Expectations() {
                {
                    one(authzCodeAuthenticator).generateJwtAndTokenInformation();
                    one(taiSubjectUtils).createResult(response, config);
                    will(returnValue(successfulTAIResult));
                    one(taiWebUtils).restorePostParameters(request);
                }
            });

            TAIResult result = loginFlow.handleAuthorizationCode(request, response, AUTHZ_CODE, config);
            assertSuccesfulTAIResult(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleAuthorizationCode_exceptionThrownCreatingResult() throws Exception {
        int statusCode = HttpServletResponse.SC_UNAUTHORIZED;
        final TAIResult unsuccessfulResult = TAIResult.create(statusCode);
        try {
            mockery.checking(new Expectations() {
                {
                    one(authzCodeAuthenticator).generateJwtAndTokenInformation();
                    one(taiSubjectUtils).createResult(response, config);
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                    allowing(config).getUniqueId();
                    will(returnValue(uniqueId));
                    allowing(config).getJwtRef();
                    will(returnValue(null));
                    one(taiWebUtils).sendToErrorPage(with(any(HttpServletResponse.class)), with(any(TAIResult.class)));
                    will(returnValue(unsuccessfulResult));
                }
            });

            TAIResult result = loginFlow.handleAuthorizationCode(request, response, AUTHZ_CODE, config);
            assertResultStatus(statusCode, result);

            verifyLogMessage(outputMgr, CWWKS5454E_AUTH_CODE_ERROR_CREATING_RESULT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** Helper methods ******************************************/

    private void createAndAddCookieExpectations() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(referrerURLCookieHandler).createCookie(with(any(String.class)), with(any(String.class)), with(any(HttpServletRequest.class)));
                will(returnValue(cookie));
                one(response).addCookie(cookie);
            }
        });
    }

    private void authorizationEndpointQueryExpectations(final String responseType, final String responseMode, final String clientId, final String scope, final boolean nonce) {
        mockery.checking(new Expectations() {
            {
                // Metatype enforces that responseType will always have one of a set of values
                allowing(config).getResponseType();
                will(returnValue(responseType));
                one(config).getResponseMode();
                will(returnValue(responseMode));
                one(config).getClientId();
                will(returnValue(clientId));
                one(config).getScope();
                will(returnValue(scope));
                one(config).createNonce();
                will(returnValue(nonce));
            }
        });
    }

    private void assertResultStatus(int expected, TAIResult result) {
        assertEquals("Result code did not match expected result.", expected, result.getStatus());
    }

    private void assertSuccesfulTAIResult(TAIResult result) {
        assertEquals("TAIResult code did not match expected value.", HttpServletResponse.SC_OK, result.getStatus());
        assertEquals("TAIResult principle did not match expected value.", successfulTAIPrinciple, result.getAuthenticatedPrincipal());
    }

    private void verifyAuthzEndpointFormat(String fullUrl, String authzEndpointUrl) {
        verifyPattern(fullUrl, "^" + Pattern.quote(authzEndpointUrl) + "\\?[^?]+$");

        String regexQueryString = "\\?(" + REGEX_URL_ENCODED_CHARS + "*[=]?" + REGEX_URL_ENCODED_CHARS + "*[&]?)*";
        verifyPattern(fullUrl, "^[^?]*" + regexQueryString + "$");
    }

}
