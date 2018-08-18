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

import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.Oauth2LoginConfigImpl;
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
    final SocialWebUtils socialWebUtils = mockery.mock(SocialWebUtils.class);

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