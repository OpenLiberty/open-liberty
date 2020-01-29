/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.common.http.AuthUtils;
import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.Oauth2LoginConfigImpl;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.security.social.web.utils.SocialWebUtils;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;

import test.common.SharedOutputManager;

public class TAIWebUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    TAIWebUtils utils = null;

    private static final String HOST = "www.host.com";
    private static final String HOST_AND_PORT = "www.host.com:8080";
    private static final String SERVER_NAME = "www.some-server.com";
    private static final String SERVER_IP = "112.35.81.3";
    private static final String UNIQUE_ID = "mySocialLogin";
    private static final String REDIRECT_PATH = Oauth2LoginConfigImpl.getContextRoot() + "/redirect";
    private static final String AUTHORIZATION_ENDPOINT_URL = "https://www.example.com/oauth/authorize";

    public interface MockInterface {
        public String getHostAndPort();
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    final SocialLoginConfig config = mockery.mock(SocialLoginConfig.class);
    final ReferrerURLCookieHandler referrerURLCookieHandler = mockery.mock(ReferrerURLCookieHandler.class);
    final Cookie cookie = mockery.mock(Cookie.class);
    final Oauth2LoginConfigImpl oauth2Config = mockery.mock(Oauth2LoginConfigImpl.class);
    final SocialWebUtils socialWebUtils = mockery.mock(SocialWebUtils.class);
    final AuthUtils authUtils = mockery.mock(AuthUtils.class);

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());

        utils = new TAIWebUtils();
        utils.referrerURLCookieHandler = referrerURLCookieHandler;
        utils.socialWebUtils = socialWebUtils;
        utils.authUtils = authUtils;
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

    /************************************** getRedirectUrl **************************************/

    @Test
    public void getRedirectUrl_hostWithoutScheme() throws Exception {
        try {
            final String hostAndPort = RandomUtils.getRandomSelection(HOST, HOST_AND_PORT);
            mockery.checking(new Expectations() {
                {
                    one(config).getRedirectToRPHostAndPort();
                    will(returnValue(hostAndPort));
                    allowing(config).getUniqueId();
                    will(returnValue(UNIQUE_ID));
                }
            });
            String expected = hostAndPort + REDIRECT_PATH + "/" + UNIQUE_ID;
            String result = utils.getRedirectUrl(request, config);
            assertEquals("Redirect URI did not match expected result.", expected, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRedirectUrl_emptyRedirectHostAndPort() throws Exception {
        try {
            utils = new TAIWebUtils() {
                protected String getHostAndPort(HttpServletRequest req) {
                    return mockInterface.getHostAndPort();
                }
            };
            utils.socialWebUtils = socialWebUtils;
            final String serverName = RandomUtils.getRandomSelection(SERVER_NAME, SERVER_IP);
            mockery.checking(new Expectations() {
                {
                    one(config).getRedirectToRPHostAndPort();
                    will(returnValue(""));
                    allowing(mockInterface).getHostAndPort();
                    will(returnValue(serverName));
                    allowing(config).getUniqueId();
                    will(returnValue(UNIQUE_ID));
                }
            });
            String expected = serverName + REDIRECT_PATH + "/" + UNIQUE_ID;
            String result = utils.getRedirectUrl(request, config);
            assertEquals("Redirect URI did not match expected result.", expected, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRedirectUrl_nullRedirectHostAndPort() throws Exception {
        try {
            utils = new TAIWebUtils() {
                protected String getHostAndPort(HttpServletRequest req) {
                    return mockInterface.getHostAndPort();
                }
            };
            utils.socialWebUtils = socialWebUtils;
            final String serverName = RandomUtils.getRandomSelection(SERVER_NAME, SERVER_IP);
            mockery.checking(new Expectations() {
                {
                    one(config).getRedirectToRPHostAndPort();
                    will(returnValue(null));
                    allowing(mockInterface).getHostAndPort();
                    will(returnValue(serverName));
                    allowing(config).getUniqueId();
                    will(returnValue(UNIQUE_ID));
                }
            });
            String expected = serverName + REDIRECT_PATH + "/" + UNIQUE_ID;
            String result = utils.getRedirectUrl(request, config);
            assertEquals("Redirect URI did not match expected result.", expected, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRedirectUrl_invalidRedirectHostAndPort() throws Exception {
        try {
            utils = new TAIWebUtils() {
                protected String getHostAndPort(HttpServletRequest req) {
                    return mockInterface.getHostAndPort();
                }
            };
            utils.socialWebUtils = socialWebUtils;
            final String redirectUrl = "Some invalid URL";
            final String hostAndPort = RandomUtils.getRandomSelection(HOST, HOST_AND_PORT);
            mockery.checking(new Expectations() {
                {
                    one(config).getRedirectToRPHostAndPort();
                    will(returnValue(redirectUrl));
                    allowing(mockInterface).getHostAndPort();
                    will(returnValue(hostAndPort));
                    allowing(config).getUniqueId();
                    will(returnValue(UNIQUE_ID));
                }
            });
            String expected = hostAndPort + REDIRECT_PATH + "/" + UNIQUE_ID;
            String result = utils.getRedirectUrl(request, config);
            assertEquals("Redirect URI did not match expected result.", expected, result);

            // Invalid redirectHostAndPort value should not emit an error message to avoid filling up logs unnecessarily
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getAuthorizationEndpoint ******************************************/

    @Test
    public void getAuthorizationEndpoint_nullOrEmptyEndpoint() throws Exception {
        try {
            final String endpoint = RandomUtils.getRandomSelection(null, "");
            mockery.checking(new Expectations() {
                {
                    one(config).getAuthorizationEndpoint();
                    will(returnValue(endpoint));
                }
            });

            try {
                String result = utils.getAuthorizationEndpoint(config);
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAuthorizationEndpoint_invalidUri() throws Exception {
        try {
            final String endpoint = "some value that's not a valid URI";
            mockery.checking(new Expectations() {
                {
                    one(config).getAuthorizationEndpoint();
                    will(returnValue(endpoint));
                }
            });

            try {
                String result = utils.getAuthorizationEndpoint(config);
                fail("Should have thrown SocialLoginException but did not. Result was [" + result + "].");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5417E_EXCEPTION_INITIALIZING_URL, endpoint);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getAuthorizationEndpoint_validUri() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(config).getAuthorizationEndpoint();
                    will(returnValue(AUTHORIZATION_ENDPOINT_URL));
                }
            });

            String result = utils.getAuthorizationEndpoint(config);
            assertEquals("Authorization endpoint result did not match expected value.", AUTHORIZATION_ENDPOINT_URL, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createStateCookie ******************************************/

    @Test
    public void createStateCookie_GET() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(socialWebUtils).getLoginHint(request);
                    one(request).getMethod();
                    will(returnValue("GET"));
                }
            });
            createAndAddCookieExpectations();

            String result = utils.createStateCookie(request, response);
            assertNotNull("State cookie value should not have been null.", result);
            verifyPattern(result, "^[a-zA-Z0-9]+$");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void createStateCookie_POST_withLoginHint() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(socialWebUtils).getLoginHint(request);
                    will(returnValue(uniqueId));
                    one(request).getMethod();
                    will(returnValue("POST"));
                }
            });
            createAndAddCookieExpectations();

            String result = utils.createStateCookie(request, response);
            assertNotNull("State cookie value should not have been null.", result);
            verifyPattern(result, "^[a-zA-Z0-9]+" + uniqueId + "$");

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getHostAndPort **************************************/

    @Test
    public void getHostAndPort_secure() throws Exception {
        try {
            final String serverName = RandomUtils.getRandomSelection(SERVER_NAME, SERVER_IP);
            final int port = 9443;
            final String scheme = "https";
            mockery.checking(new Expectations() {
                {
                    one(request).getServerName();
                    will(returnValue(serverName));
                    one(request).isSecure();
                    will(returnValue(true));
                    one(request).getServerPort();
                    will(returnValue(port));
                    one(request).getScheme();
                    will(returnValue(scheme));
                    allowing(config).getUniqueId();
                    will(returnValue(UNIQUE_ID));
                }
            });
            String expected = scheme + "://" + serverName + ":" + port;
            String result = utils.getHostAndPort(request);
            assertEquals("Host did not match expected result.", expected, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getHostAndPort_insecure() throws Exception {
        try {
            final String serverName = RandomUtils.getRandomSelection(SERVER_NAME, SERVER_IP);
            mockery.checking(new Expectations() {
                {
                    one(request).getServerName();
                    will(returnValue(serverName));
                    one(request).isSecure();
                    will(returnValue(false));
                }
            });
            String expected = "https://" + serverName;
            String result = utils.getHostAndPort(request);
            assertEquals("Host did not match expected result.", expected, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getBearerAccessToken **************************************/

    @Test
    public void test_getBearerAccessToken_customHeaderName() throws Exception {
        try {
            final String customHeaderName = "My Header";
            final String expectedTokenValue = "Expected Token Value";
            mockery.checking(new Expectations() {
                {
                    one(oauth2Config).getAccessTokenHeaderName();
                    will(returnValue(customHeaderName));
                    one(authUtils).getBearerTokenFromHeader(request, customHeaderName);
                    will(returnValue(expectedTokenValue));
                }
            });

            String result = utils.getBearerAccessToken(request, oauth2Config);
            assertEquals("Returned bearer token value did not match expected result.", expectedTokenValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerAccessToken_customHeaderName_missingCustomHeader() throws Exception {
        try {
            final String customHeaderName = "My Header";
            mockery.checking(new Expectations() {
                {
                    one(oauth2Config).getAccessTokenHeaderName();
                    will(returnValue(customHeaderName));
                    one(authUtils).getBearerTokenFromHeader(request, customHeaderName);
                    will(returnValue(null));
                    one(request).getHeader(customHeaderName + "-segments");
                    will(returnValue(null));
                    one(oauth2Config).getUniqueId();
                    will(returnValue(uniqueId));
                }
            });

            String result = utils.getBearerAccessToken(request, oauth2Config);
            assertNull("Returned bearer token value should have been null but was [" + result + "].", result);

            verifyLogMessageWithInserts(outputMgr, CWWKS5376W_CUSTOM_ACCESS_TOKEN_HEADER_MISSING, Oauth2LoginConfigImpl.KEY_accessTokenHeaderName, uniqueId, customHeaderName);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerAccessToken_noConfiguredHeaderName_bearerTokenInAuthzHeader() throws Exception {
        try {
            final String expectedTokenValue = "Expected Token Value";
            mockery.checking(new Expectations() {
                {
                    one(oauth2Config).getAccessTokenHeaderName();
                    will(returnValue(null));
                    one(authUtils).getBearerTokenFromHeader(request);
                    will(returnValue(expectedTokenValue));
                }
            });

            String result = utils.getBearerAccessToken(request, oauth2Config);
            assertEquals("Returned bearer token value did not match expected result.", expectedTokenValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getBearerTokenFromCustomHeader **************************************/

    @Test
    public void test_getBearerTokenFromCustomHeader_emptyHeaderName() throws Exception {
        try {
            final String customHeaderName = "";
            final String expectedTokenValue = "Expected Token Value";
            mockery.checking(new Expectations() {
                {
                    one(authUtils).getBearerTokenFromHeader(request, customHeaderName);
                    will(returnValue(expectedTokenValue));
                }
            });

            String result = utils.getBearerTokenFromCustomHeader(request, customHeaderName);
            assertEquals("Returned bearer token value did not match expected result.", expectedTokenValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerTokenFromCustomHeader_emptyBearerValue() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            final String expectedTokenValue = "";
            mockery.checking(new Expectations() {
                {
                    one(authUtils).getBearerTokenFromHeader(request, customHeaderName);
                    will(returnValue(expectedTokenValue));
                }
            });

            String result = utils.getBearerTokenFromCustomHeader(request, customHeaderName);
            assertEquals("Returned bearer token value did not match expected result.", expectedTokenValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerTokenFromCustomHeader_normalBearerValue() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            final String expectedTokenValue = "Expected Token Value";
            mockery.checking(new Expectations() {
                {
                    one(authUtils).getBearerTokenFromHeader(request, customHeaderName);
                    will(returnValue(expectedTokenValue));
                }
            });

            String result = utils.getBearerTokenFromCustomHeader(request, customHeaderName);
            assertEquals("Returned bearer token value did not match expected result.", expectedTokenValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerTokenFromCustomHeader_missingCustomHeaders() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            mockery.checking(new Expectations() {
                {
                    one(authUtils).getBearerTokenFromHeader(request, customHeaderName);
                    will(returnValue(null));
                    one(request).getHeader(customHeaderName + "-segments");
                    will(returnValue(null));
                }
            });

            String result = utils.getBearerTokenFromCustomHeader(request, customHeaderName);
            assertNull("Returned bearer token value should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getBearerTokenFromCustomHeaderSegments **************************************/

    @Test
    public void test_getBearerTokenFromCustomHeaderSegments_nonNumberSegmentsHeader() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(customHeaderName + "-segments");
                    will(returnValue("not a number"));
                }
            });

            String result = utils.getBearerTokenFromCustomHeaderSegments(request, customHeaderName);
            assertNull("Returned bearer token value should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerTokenFromCustomHeaderSegments_oneSegmentHeader_missingValue() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(customHeaderName + "-segments");
                    will(returnValue("1"));
                    one(request).getHeader(customHeaderName + "-1");
                    will(returnValue(null));
                }
            });

            String result = utils.getBearerTokenFromCustomHeaderSegments(request, customHeaderName);
            assertNull("Returned bearer token value should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerTokenFromCustomHeaderSegments_oneSegmentHeader() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            final String expectedTokenValue = "Expected Token Value";
            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(customHeaderName + "-segments");
                    will(returnValue("1"));
                    one(request).getHeader(customHeaderName + "-1");
                    will(returnValue(expectedTokenValue));
                }
            });

            String result = utils.getBearerTokenFromCustomHeaderSegments(request, customHeaderName);
            assertEquals("Returned bearer token value did not match expected result.", expectedTokenValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** buildBearerTokenFromCustomHeaderSegments **************************************/

    @Test
    public void test_buildBearerTokenFromCustomHeaderSegments_negativeNumberOfSegments() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            int numberOfSegments = -42;

            String result = utils.buildBearerTokenFromCustomHeaderSegments(request, customHeaderName, numberOfSegments);
            assertEquals("Returned bearer token value should have been an empty string but was not.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildBearerTokenFromCustomHeaderSegments_zeroSegments() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            int numberOfSegments = 0;

            String result = utils.buildBearerTokenFromCustomHeaderSegments(request, customHeaderName, numberOfSegments);
            assertEquals("Returned bearer token value should have been an empty string but was not.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildBearerTokenFromCustomHeaderSegments_oneSegment_missingSegmentHeader() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            int numberOfSegments = 1;

            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(customHeaderName + "-1");
                    will(returnValue(null));
                }
            });

            String result = utils.buildBearerTokenFromCustomHeaderSegments(request, customHeaderName, numberOfSegments);
            assertEquals("Returned bearer token value should have been an empty string but was not.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildBearerTokenFromCustomHeaderSegments_oneSegment_segmentHeaderIsWhiteSpace() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            int numberOfSegments = 1;

            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(customHeaderName + "-1");
                    will(returnValue(" \t\r \n "));
                }
            });

            String result = utils.buildBearerTokenFromCustomHeaderSegments(request, customHeaderName, numberOfSegments);
            assertEquals("Returned bearer token value should have been an empty string but was not.", "", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildBearerTokenFromCustomHeaderSegments_oneSegment_segmentHeaderContainsWhiteSpace() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            int numberOfSegments = 1;
            final String expectedValue = "This is the expected value.";

            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(customHeaderName + "-1");
                    will(returnValue(" \t\r \n " + expectedValue + "      "));
                }
            });

            String result = utils.buildBearerTokenFromCustomHeaderSegments(request, customHeaderName, numberOfSegments);
            assertEquals("Returned bearer token value did not match the expected value.", expectedValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_buildBearerTokenFromCustomHeaderSegments_multipleSegments() throws Exception {
        try {
            final String customHeaderName = "My Custom Header";
            int numberOfSegments = 4;
            final String tokenPart1 = "First";
            final String tokenPart2 = "Second";
            final String tokenPart4 = "Fourth";
            final String expectedTokenValue = tokenPart1 + tokenPart2 + tokenPart4;
            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(customHeaderName + "-1");
                    will(returnValue(tokenPart1 + " \n\r\t"));
                    one(request).getHeader(customHeaderName + "-2");
                    will(returnValue("  " + tokenPart2));
                    one(request).getHeader(customHeaderName + "-3"); // One segment happens to be missing
                    will(returnValue(null));
                    one(request).getHeader(customHeaderName + "-4");
                    will(returnValue(tokenPart4));
                }
            });

            String result = utils.buildBearerTokenFromCustomHeaderSegments(request, customHeaderName, numberOfSegments);
            assertEquals("Returned bearer token value did not match expected result.", expectedTokenValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getBearerTokenFromAuthzHeaderOrRequestBody **************************************/

    @Test
    public void test_getBearerTokenFromAuthzHeaderOrRequestBody_bearerTokenInAuthzHeader() throws Exception {
        try {
            final String expectedTokenValue = "Expected Token Value";
            mockery.checking(new Expectations() {
                {
                    one(authUtils).getBearerTokenFromHeader(request);
                    will(returnValue(expectedTokenValue));
                }
            });

            String result = utils.getBearerTokenFromAuthzHeaderOrRequestBody(request);
            assertEquals("Returned bearer token value did not match expected result.", expectedTokenValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerTokenFromAuthzHeaderOrRequestBody_nonPostRequest() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(authUtils).getBearerTokenFromHeader(request);
                    will(returnValue(null));
                    one(request).getMethod();
                    will(returnValue("GET"));
                }
            });

            String result = utils.getBearerTokenFromAuthzHeaderOrRequestBody(request);
            assertNull("Returned bearer token value should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerTokenFromAuthzHeaderOrRequestBody_nonUrlEncodedRequest() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(authUtils).getBearerTokenFromHeader(request);
                    will(returnValue(null));
                    one(request).getMethod();
                    will(returnValue("POST"));
                    one(request).getHeader(ClientConstants.REQ_CONTENT_TYPE_NAME);
                    will(returnValue("application/json"));
                }
            });

            String result = utils.getBearerTokenFromAuthzHeaderOrRequestBody(request);
            assertNull("Returned bearer token value should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerTokenFromAuthzHeaderOrRequestBody_missingAccessTokenParameter() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(authUtils).getBearerTokenFromHeader(request);
                    will(returnValue(null));
                    one(request).getMethod();
                    will(returnValue("POST"));
                    one(request).getHeader(ClientConstants.REQ_CONTENT_TYPE_NAME);
                    will(returnValue(ClientConstants.REQ_CONTENT_TYPE_APP_FORM_URLENCODED));
                    one(request).getParameter("access_token");
                    will(returnValue(null));
                }
            });

            String result = utils.getBearerTokenFromAuthzHeaderOrRequestBody(request);
            assertNull("Returned bearer token value should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getBearerTokenFromAuthzHeaderOrRequestBody_tokenIncludedAsParameter() throws Exception {
        try {
            final String expectedTokenValue = "Expected Token Value";
            mockery.checking(new Expectations() {
                {
                    one(authUtils).getBearerTokenFromHeader(request);
                    will(returnValue(null));
                    one(request).getMethod();
                    will(returnValue("POST"));
                    one(request).getHeader(ClientConstants.REQ_CONTENT_TYPE_NAME);
                    will(returnValue(ClientConstants.REQ_CONTENT_TYPE_APP_FORM_URLENCODED));
                    one(request).getParameter("access_token");
                    will(returnValue(expectedTokenValue));
                }
            });

            String result = utils.getBearerTokenFromAuthzHeaderOrRequestBody(request);
            assertEquals("Returned bearer token value did not match expected result.", expectedTokenValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    private void createAndAddCookieExpectations() throws IOException {
        mockery.checking(new Expectations() {
            {
                one(referrerURLCookieHandler).createCookie(with(any(String.class)), with(any(String.class)), with(any(HttpServletRequest.class)));
                will(returnValue(cookie));
                one(response).addCookie(cookie);
            }
        });
    }

}