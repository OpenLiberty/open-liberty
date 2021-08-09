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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.ws.security.common.jwk.utils.JsonUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.security.social.twitter.TwitterConstants;
import com.ibm.ws.security.social.twitter.TwitterTokenServices;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.wsspi.security.tai.TAIResult;

import test.common.SharedOutputManager;

public class TwitterLoginFlowTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private static String ACCESS_TOKEN = "EAANQIE2J5nMBAErWBIFfkmu9r6yQeGoIMg39mHRJrZA7L0jbiD7GEpLSZBm96tgqvvlbQI3UIgQXSJaO6sRJGaFEZCwn5kolWgSjs5q71rrNg0GdbHk5yxrtsZAWsZBv3XV1xFmJ4reZBKA6sx5PqQJejg5RtTWKPg4jJoP0zk1AZDZD";
    private static String ACCESS_TOKEN_SECRET = "SOMExxxACCESSyyyTOKENzzzSECRET";
    private final String uniqueId = "twitterLogin";

    final static String successfulTAIPrinciple = "myPrinciple";

    public interface MockInterface {
        public void getTwitterRequestToken();

        public TAIResult createSubjectFromTwitterCredentials() throws WebTrustAssociationFailedException;

        TAIResult createResultFromUserApiResponse() throws WebTrustAssociationFailedException;
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    private final JwtToken jwtToken = mockery.mock(JwtToken.class);
    private final TwitterTokenServices twitterTokenServices = mockery.mock(TwitterTokenServices.class);
    private final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class);
    private final TAIWebUtils taiWebUtils = mockery.mock(TAIWebUtils.class);
    private final TAIJwtUtils taiJwtUtils = mockery.mock(TAIJwtUtils.class);
    private final SocialWebUtils socialWebUtils = mockery.mock(SocialWebUtils.class);
    private final TAISubjectUtils taiSubjectUtils = mockery.mock(TAISubjectUtils.class);

    TwitterLoginFlow loginFlow = new MockTwitterLoginFlow();

    class MockTwitterLoginFlow extends TwitterLoginFlow {
        TAISubjectUtils getTAISubjectUtils(@Sensitive String accessToken, JwtToken jwt, JwtToken issuedJwt, @Sensitive Map<String, Object> userApiResponseTokens, String userApiResponse) {
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

        loginFlow = new MockTwitterLoginFlow();
        mockProtectedClassMembers(loginFlow);
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

    private void mockProtectedClassMembers(TwitterLoginFlow loginFlow) {
        loginFlow.taiWebUtils = taiWebUtils;
        loginFlow.twitterTokenServices = twitterTokenServices;
        loginFlow.webUtils = socialWebUtils;
        loginFlow.taiJwtUtils = taiJwtUtils;
    }

    /****************************************** handleTwitterRequest ******************************************/

    @Test
    public void handleTwitterRequest_getRequestToken() {
        TwitterLoginFlow loginFlow = new MockTwitterLoginFlow() {
            @Override
            protected void getTwitterRequestToken(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) {
                mockInterface.getTwitterRequestToken();
            }
        };
        mockProtectedClassMembers(loginFlow);

        mockery.checking(new Expectations() {
            {
                one(socialWebUtils).getAndClearCookie(request, response, TwitterConstants.COOKIE_NAME_ACCESS_TOKEN);
                will(returnValue(null));
                one(socialWebUtils).getAndClearCookie(request, response, TwitterConstants.COOKIE_NAME_ACCESS_TOKEN_SECRET);
                will(returnValue(null));
                one(mockInterface).getTwitterRequestToken();
            }
        });
        try {
            TAIResult result = loginFlow.handleTwitterRequest(request, response, config);

            // 403 status is returned to end the TAI flow, not necessarily to say the user is forbidden. The end user will be redirected to Twitter anyway.
            assertResultStatus(HttpServletResponse.SC_FORBIDDEN, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleTwitterRequest_createSubject() {
        TwitterLoginFlow loginFlow = new MockTwitterLoginFlow() {
            @Override
            protected TAIResult createSubjectFromTwitterCredentials(HttpServletResponse response, SocialLoginConfig config, String accessToken, @Sensitive String accessTokenSecret) throws WebTrustAssociationFailedException {
                return mockInterface.createSubjectFromTwitterCredentials();
            }
        };
        mockProtectedClassMembers(loginFlow);

        try {
            final TAIResult successfulTAIResult = TAIResult.create(HttpServletResponse.SC_OK, successfulTAIPrinciple);
            mockery.checking(new Expectations() {
                {
                    one(socialWebUtils).getAndClearCookie(request, response, TwitterConstants.COOKIE_NAME_ACCESS_TOKEN);
                    will(returnValue(ACCESS_TOKEN));
                    one(socialWebUtils).getAndClearCookie(request, response, TwitterConstants.COOKIE_NAME_ACCESS_TOKEN_SECRET);
                    will(returnValue(ACCESS_TOKEN_SECRET));
                    one(taiWebUtils).restorePostParameters(request);
                    one(mockInterface).createSubjectFromTwitterCredentials();
                    will(returnValue(successfulTAIResult));
                }
            });

            TAIResult result = loginFlow.handleTwitterRequest(request, response, config);
            assertSuccesfulTAIResult(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getTwitterRequestToken ******************************************/

    @Test
    public void getTwitterRequestToken() {
        try {
            final String redirectUri = "https://localhost:8021/ibm/api/social-login/redirect/twitterLogin";
            final String state = "abcABCxyzXYZ";
            mockery.checking(new Expectations() {
                {
                    one(taiWebUtils).savePostParameters(request);
                    one(taiWebUtils).createStateCookie(request, response);
                    will(returnValue(state));
                    one(taiWebUtils).getRedirectUrl(request, config);
                    will(returnValue(redirectUri));
                    one(twitterTokenServices).getRequestToken(request, response, redirectUri, state, config);
                }
            });

            loginFlow.getTwitterRequestToken(request, response, config);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createSubjectFromTwitterCredentials ******************************************/

    @Test
    public void createSubjectFromTwitterCredentials_nullVerifyCredentialsResult() {
        try {
            int statusCode = HttpServletResponse.SC_UNAUTHORIZED;
            final TAIResult unsuccessfulResult = TAIResult.create(statusCode);
            mockery.checking(new Expectations() {
                {
                    one(twitterTokenServices).verifyCredentials(response, ACCESS_TOKEN, ACCESS_TOKEN_SECRET, config);
                    will(returnValue(null));
                    one(taiWebUtils).sendToErrorPage(with(any(HttpServletResponse.class)), with(any(TAIResult.class)));
                    will(returnValue(unsuccessfulResult));
                }
            });

            TAIResult result = loginFlow.createSubjectFromTwitterCredentials(response, config, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
            assertResultStatus(statusCode, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createSubjectFromTwitterCredentials_emptyVerifyCredentialsResult() {
        TwitterLoginFlow loginFlow = new MockTwitterLoginFlow() {
            @Override
            TAIResult createResultFromUserApiResponse(HttpServletResponse response, SocialLoginConfig config, Map<String, Object> result, String userApiResponse) throws WebTrustAssociationFailedException {
                return mockInterface.createResultFromUserApiResponse();
            }
        };
        mockProtectedClassMembers(loginFlow);

        try {
            int statusCode = HttpServletResponse.SC_UNAUTHORIZED;
            final TAIResult unsuccessfulResult = TAIResult.create(statusCode);
            mockery.checking(new Expectations() {
                {
                    one(twitterTokenServices).verifyCredentials(response, ACCESS_TOKEN, ACCESS_TOKEN_SECRET, config);
                    will(returnValue(new HashMap<String, Object>()));
                    one(mockInterface).createResultFromUserApiResponse();
                    will(returnValue(unsuccessfulResult));
                }
            });

            TAIResult result = loginFlow.createSubjectFromTwitterCredentials(response, config, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
            assertResultStatus(statusCode, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createResultFromUserApiResponse ******************************************/

    @Test
    public void createResultFromUserApiResponse_creatingJwtThrowsException() {
        try {
            final Map<String, Object> userApiResult = new HashMap<String, Object>();
            final String userApiString = JsonUtils.toJson(userApiResult);

            int statusCode = HttpServletResponse.SC_UNAUTHORIZED;
            final TAIResult unsuccessfulResult = TAIResult.create(statusCode);
            mockery.checking(new Expectations() {
                {
                    one(config).getJwtRef();
                    one(taiJwtUtils).createJwtTokenFromJson(userApiString, config, false);
                    will(throwException(new Exception(defaultExceptionMsg)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(taiWebUtils).sendToErrorPage(with(any(HttpServletResponse.class)), with(any(TAIResult.class)));
                    will(returnValue(unsuccessfulResult));
                }
            });

            TAIResult result = loginFlow.createResultFromUserApiResponse(response, config, userApiResult, userApiString);
            assertResultStatus(statusCode, result);

            verifyLogMessage(outputMgr, CWWKS5453E_AUTH_CODE_FAILED_TO_CREATE_JWT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createResultFromUserApiResponse_createTwitterResultThrowsSocialLoginException() {
        try {
            final Map<String, Object> userApiResult = new HashMap<String, Object>();
            final String userApiString = JsonUtils.toJson(userApiResult);

            int statusCode = HttpServletResponse.SC_UNAUTHORIZED;
            final TAIResult unsuccessfulResult = TAIResult.create(statusCode);
            mockery.checking(new Expectations() {
                {
                    allowing(config).getJwtRef();
                    will(returnValue("defaultJwt"));
                    one(taiJwtUtils).createJwtTokenFromJson(userApiString, config, false);
                    will(returnValue(jwtToken));
                    one(taiSubjectUtils).createResult(response, config);
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(taiWebUtils).sendToErrorPage(with(any(HttpServletResponse.class)), with(any(TAIResult.class)));
                    will(returnValue(unsuccessfulResult));
                }
            });

            TAIResult result = loginFlow.createResultFromUserApiResponse(response, config, userApiResult, userApiString);
            assertResultStatus(statusCode, result);

            String expectedMsg = CWWKS5437E_TWITTER_ERROR_CREATING_RESULT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg);
            verifyLogMessage(outputMgr, expectedMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createResultFromUserApiResponse_createTwitterResultThrowsException() {
        try {
            final Map<String, Object> userApiResult = new HashMap<String, Object>();
            final String userApiString = JsonUtils.toJson(userApiResult);

            int statusCode = HttpServletResponse.SC_UNAUTHORIZED;
            final TAIResult unsuccessfulResult = TAIResult.create(statusCode);
            mockery.checking(new Expectations() {
                {
                    allowing(config).getJwtRef();
                    will(returnValue("defaultJwt"));
                    one(taiJwtUtils).createJwtTokenFromJson(userApiString, config, false);
                    will(returnValue(jwtToken));
                    one(taiSubjectUtils).createResult(response, config);
                    will(throwException(new WebTrustAssociationFailedException(defaultExceptionMsg)));
                    one(config).getUniqueId();
                    will(returnValue(uniqueId));
                    one(taiWebUtils).sendToErrorPage(with(any(HttpServletResponse.class)), with(any(TAIResult.class)));
                    will(returnValue(unsuccessfulResult));
                }
            });

            TAIResult result = loginFlow.createResultFromUserApiResponse(response, config, userApiResult, userApiString);
            assertResultStatus(statusCode, result);

            String expectedMsg = CWWKS5437E_TWITTER_ERROR_CREATING_RESULT + ".+\\[" + uniqueId + "\\].+" + Pattern.quote(defaultExceptionMsg);
            verifyLogMessage(outputMgr, expectedMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createResultFromUserApiResponse() {
        try {
            final Map<String, Object> userApiResult = new HashMap<String, Object>();
            final String userApiString = JsonUtils.toJson(userApiResult);

            final TAIResult successfulTAIResult = TAIResult.create(HttpServletResponse.SC_OK, successfulTAIPrinciple);

            mockery.checking(new Expectations() {
                {
                    allowing(config).getJwtRef();
                    will(returnValue("defaultJwt"));
                    one(taiJwtUtils).createJwtTokenFromJson((new HashMap<String, Object>()).toString(), config, false);
                    will(returnValue(jwtToken));
                    one(taiSubjectUtils).createResult(response, config);
                    will(returnValue(successfulTAIResult));
                }
            });

            TAIResult result = loginFlow.createResultFromUserApiResponse(response, config, userApiResult, userApiString);
            assertSuccesfulTAIResult(result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** Helper methods ******************************************/

    private void assertResultStatus(int expected, TAIResult result) {
        assertEquals("Result code did not match expected result.", expected, result.getStatus());
    }

    private void assertSuccesfulTAIResult(TAIResult result) {
        assertEquals("TAIResult code did not match expected value.", HttpServletResponse.SC_OK, result.getStatus());
        assertEquals("TAIResult principle did not match expected value.", successfulTAIPrinciple, result.getAuthenticatedPrincipal());
    }

}
