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
package com.ibm.ws.security.social.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.common.web.CommonWebConstants;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.TwitterLoginConfigImpl;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialLoginRequest;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.security.social.twitter.TwitterConstants;
import com.ibm.ws.security.social.twitter.TwitterTokenServices;
import com.ibm.ws.security.social.web.utils.ConfigInfoJsonBuilder;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

import test.common.SharedOutputManager;

public class EndpointServicesTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    EndpointServices services = new EndpointServices();
    EndpointServices servicesForTwitter = new EndpointServices();

    private final ReferrerURLCookieHandler referrerURLCookieHandler = mockery.mock(ReferrerURLCookieHandler.class);
    private final SocialLoginRequest socialLoginRequest = mockery.mock(SocialLoginRequest.class);
    private final SocialLoginConfig socialLoginConfig = mockery.mock(SocialLoginConfig.class);
    private final TwitterLoginConfigImpl twitterLoginConfig = mockery.mock(TwitterLoginConfigImpl.class);
    private final TwitterTokenServices twitterTokenServices = mockery.mock(TwitterTokenServices.class);
    private final Cookie cookie1 = mockery.mock(Cookie.class, "cookie1");
    private final Cookie cookie2 = mockery.mock(Cookie.class, "cookie2");

    @SuppressWarnings("unchecked")
    private final ConcurrentServiceReferenceMap<String, SocialLoginConfig> socialLoginConfigRef = mockery.mock(ConcurrentServiceReferenceMap.class);
    @SuppressWarnings("unchecked")
    private final AtomicServiceReference<SecurityService> securityServiceRef = mockery.mock(AtomicServiceReference.class);

    private final String httpsScheme = "https";
    private final String host = "some-domain.com";
    private final String url = httpsScheme + "://" + host + "/some/path";
    private final String configId = "myConfigId";
    private final String state = "abcABCxyzXYZ";
    private final String error = "my_error";
    private final String errorDescription = "Some error description.";
    private final String code = "myCode";

    private final static String key1 = "key1";
    private final static String key2 = "key2";
    private final static String value1 = "value1";
    private final static String value2 = "value2";
    private final static String accessToken = "myAccessToken";
    private final static String accessTokenSecret = "myAccessTokenSecret";
    private final static Map<String, Object> basicMap = new HashMap<String, Object>();
    static {
        basicMap.put(key1, value1);
        basicMap.put(key2, value2);
    }
    private final static Map<String, String> getAccessTokenMap = new HashMap<String, String>();
    static {
        getAccessTokenMap.put(TwitterConstants.RESULT_ACCESS_TOKEN, accessToken);
        getAccessTokenMap.put(TwitterConstants.RESULT_ACCESS_TOKEN_SECRET, accessTokenSecret);
    }
    private final static Map<String, String[]> basicParamMap = new HashMap<String, String[]>();
    static {
        basicParamMap.put(key1, new String[] { value1 });
        basicParamMap.put(key2, new String[] { value2 });
    }

    public interface MockInterface {
        void handleSocialLoginRequest() throws SocialLoginException;

        void doRedirect() throws IOException;

        void doTwitter() throws IOException;

        void handleSocialLoginAPIRequest();

        TwitterTokenServices getTwitterTokenServices();
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());

        services = new EndpointServices();
        servicesForTwitter = new EndpointServices() {
            @Override
            protected TwitterTokenServices getTwitterTokenServices() {
                return mockInterface.getTwitterTokenServices();
            }
        };

        WebAppSecurityCollaboratorImpl.setGlobalWebAppSecurityConfig(webAppSecConfig);
        mockery.checking(new Expectations() {
            {
                allowing(webAppSecConfig).createReferrerURLCookieHandler();
                will(returnValue(referrerURLCookieHandler));
            }
        });
        EndpointServices.setReferrerURLCookieHandler(referrerURLCookieHandler);
        EndpointServices.setActivatedSocialLoginConfigRef(socialLoginConfigRef);
        EndpointServices.setActivatedSecurityServiceRef(securityServiceRef);
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

    /************************************** activate **************************************/

    @Test
    public void activate() {
        try {
            services.activate(cc);

            verifyLogMessage(outputMgr, CWWKS5407I_SOCIAL_LOGIN_ENDPOINT_SERVICE_ACTIVATED);
            verifyNoLogMessage(outputMgr, MSG_BASE_ERROR_WARNING);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** handleSocialLoginRequest **************************************/

    @Test
    public void handleSocialLoginRequest_noSocLoginReqArg_missingRequiredAttribute() {
        try {
            mockery.checking(new Expectations() {
                {
                    // Attribute must be present to be a valid social login request
                    one(request).getAttribute(Constants.ATTRIBUTE_SOCIALMEDIA_REQUEST);
                    will(returnValue(null));
                    one(request).getRequestURL();
                    will(returnValue(new StringBuffer(url)));
                }
            });

            try {
                services.handleSocialLoginRequest(request, response);
                fail("Should have thrown SocialLoginException but didn't.");
            } catch (SocialLoginException e) {
                // TODO - might not be bad to revisit this NLS message
                verifyExceptionWithInserts(e, CWWKS5406E_SOCIAL_LOGIN_INVALID_URL, url);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_noSocLoginReqArg_exceptionThrown() {
        try {
            services = new EndpointServices() {
                @Override
                void handleSocialLoginRequest(HttpServletRequest request, HttpServletResponse response, SocialLoginRequest socialLoginRequest) throws SocialLoginException {
                    mockInterface.handleSocialLoginRequest();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(request).getAttribute(Constants.ATTRIBUTE_SOCIALMEDIA_REQUEST);
                    will(returnValue(socialLoginRequest));
                    one(mockInterface).handleSocialLoginRequest();
                    will(throwException(new SocialLoginException(defaultExceptionMsg, null, null)));
                }
            });

            try {
                services.handleSocialLoginRequest(request, response);
                fail("Should have thrown SocialLoginException but didn't.");
            } catch (SocialLoginException e) {
                // TODO - might consider catching this exception and displaying a dedicated NLS message?
                verifyExceptionWithInserts(e, "^" + defaultExceptionMsg + "$");
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_noSocLoginReqArg_valid() {
        try {
            services = new EndpointServices() {
                @Override
                void handleSocialLoginRequest(HttpServletRequest request, HttpServletResponse response, SocialLoginRequest socialLoginRequest) throws SocialLoginException {
                    mockInterface.handleSocialLoginRequest();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(request).getAttribute(Constants.ATTRIBUTE_SOCIALMEDIA_REQUEST);
                    will(returnValue(socialLoginRequest));
                    one(mockInterface).handleSocialLoginRequest();
                }
            });

            services.handleSocialLoginRequest(request, response);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_redirect_noConfig() {
        try {
            services = new EndpointServices() {
                @Override
                protected void doRedirect(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws IOException {
                    mockInterface.doRedirect();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(socialLoginRequest).isRedirect();
                    will(returnValue(true));
                    one(socialLoginRequest).getSocialLoginConfig();
                    will(returnValue(null));
                    one(request).getRequestURL();
                    will(returnValue(new StringBuffer(url)));
                }
            });

            try {
                services.handleSocialLoginRequest(request, response, socialLoginRequest);
                fail("Should have thrown a SocialLoginException because of a missing config but did not.");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5434E_ERROR_PROCESSING_REDIRECT + ".+" + CWWKS5433E_REDIRECT_NO_MATCHING_CONFIG + ".+\\[" + Pattern.quote(url) + "\\]");
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_redirect_exceptionThrown() {
        try {
            services = new EndpointServices() {
                @Override
                protected void doRedirect(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws IOException {
                    mockInterface.doRedirect();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(socialLoginRequest).isRedirect();
                    will(returnValue(true));
                    // Config value isn't checked, but simply passed on to doRedirect()
                    one(socialLoginRequest).getSocialLoginConfig();
                    will(returnValue(socialLoginConfig));
                    one(mockInterface).doRedirect();
                    will(throwException(new IOException(defaultExceptionMsg)));
                }
            });

            try {
                services.handleSocialLoginRequest(request, response, socialLoginRequest);
                fail("Should have thrown SocialLoginException but didn't.");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5434E_ERROR_PROCESSING_REDIRECT + ".*" + defaultExceptionMsg);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_redirect_nonTwitterConfig() {
        try {
            services = new EndpointServices() {
                @Override
                protected void doRedirect(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws IOException {
                    mockInterface.doRedirect();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(socialLoginRequest).isRedirect();
                    will(returnValue(true));
                    // Non-Twitter configs should go through normal OAuth 2.0 redirect flow
                    one(socialLoginRequest).getSocialLoginConfig();
                    will(returnValue(socialLoginConfig));
                    one(mockInterface).doRedirect();
                }
            });

            services.handleSocialLoginRequest(request, response, socialLoginRequest);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_redirect_twitterConfig_throwsException() {
        try {
            services = new EndpointServices() {
                @Override
                protected void doTwitter(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws IOException {
                    mockInterface.doTwitter();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(socialLoginRequest).isRedirect();
                    will(returnValue(true));
                    // Twitter configs should go through dedicated Twitter flow
                    one(socialLoginRequest).getSocialLoginConfig();
                    will(returnValue(twitterLoginConfig));
                    one(mockInterface).doTwitter();
                    will(throwException(new IOException(defaultExceptionMsg)));
                }
            });

            try {
                services.handleSocialLoginRequest(request, response, socialLoginRequest);
                fail("Should have thrown SocialLoginException but didn't.");
            } catch (SocialLoginException e) {
                verifyException(e, CWWKS5434E_ERROR_PROCESSING_REDIRECT + ".*" + defaultExceptionMsg);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_redirect_twitterConfig() {
        try {
            services = new EndpointServices() {
                @Override
                protected void doTwitter(HttpServletRequest request, HttpServletResponse response, SocialLoginConfig config) throws IOException {
                    mockInterface.doTwitter();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(socialLoginRequest).isRedirect();
                    will(returnValue(true));
                    // Twitter configs should go through dedicated Twitter flow
                    one(socialLoginRequest).getSocialLoginConfig();
                    will(returnValue(twitterLoginConfig));
                    one(mockInterface).doTwitter();
                }
            });

            services.handleSocialLoginRequest(request, response, socialLoginRequest);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_logout() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(socialLoginRequest).isRedirect();
                    will(returnValue(false));
                    one(socialLoginRequest).isLogout();
                    will(returnValue(true));
                    one(socialLoginRequest).getRequestUrl();
                }
            });

            services.handleSocialLoginRequest(request, response, socialLoginRequest);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_wellKnown() {
        try {
            services = new EndpointServices() {
                @Override
                protected void handleSocialLoginAPIRequest(HttpServletRequest request, HttpServletResponse response) {
                    mockInterface.handleSocialLoginAPIRequest();
                }
            };
            mockery.checking(new Expectations() {
                {
                    one(socialLoginRequest).isRedirect();
                    will(returnValue(false));
                    one(socialLoginRequest).isLogout();
                    will(returnValue(false));
                    one(socialLoginRequest).isWellknownConfig();
                    will(returnValue(true));
                    one(socialLoginRequest).getRequestUrl();
                    one(mockInterface).handleSocialLoginAPIRequest();
                }
            });

            services.handleSocialLoginRequest(request, response, socialLoginRequest);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_unknown() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(socialLoginRequest).isRedirect();
                    will(returnValue(false));
                    one(socialLoginRequest).isLogout();
                    will(returnValue(false));
                    one(socialLoginRequest).isWellknownConfig();
                    will(returnValue(false));
                    one(socialLoginRequest).isUnknown();
                    will(returnValue(true));
                    allowing(socialLoginRequest).getRequestUrl();
                    will(returnValue(url));
                }
            });

            try {
                services.handleSocialLoginRequest(request, response, socialLoginRequest);
                fail("Should have thrown SocialLoginException but didn't.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5406E_SOCIAL_LOGIN_INVALID_URL, url);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void handleSocialLoginRequest_reqTypeNotDefined() {
        try {
            // Request type doesn't match any of the expected values (should not really ever occur since we're the ones building the request type)
            mockery.checking(new Expectations() {
                {
                    one(socialLoginRequest).isRedirect();
                    will(returnValue(false));
                    one(socialLoginRequest).isLogout();
                    will(returnValue(false));
                    one(socialLoginRequest).isWellknownConfig();
                    will(returnValue(false));
                    one(socialLoginRequest).isUnknown();
                    will(returnValue(false));
                }
            });

            services.handleSocialLoginRequest(request, response, socialLoginRequest);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** getParameterMap ****************************************/

    @SuppressWarnings("static-access")
    @Test
    public void getParameterMap_noSecurityService() {
        try {
            services.setActivatedSecurityServiceRef(null);

            Map<String, Object> result = services.getParameterMap(socialLoginConfig);
            assertNotNull("Parameter map should not have been null.", result);
            assertEquals("Did not get expected number of entries in parameter map. Map was " + result, 1, result.size());
            assertNotNull("Map should have " + Constants.KEY_SOCIALLOGIN_SERVICE + " entry but did not. Map was " + result, result.get(Constants.KEY_SOCIALLOGIN_SERVICE));
            assertNull("Map should not have " + Constants.KEY_SOCIALLOGIN_SERVICE + " entry but did. Map was " + result, result.get(Constants.KEY_SECURITY_SERVICE));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getParameterMap_withSecurityService() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(securityServiceRef).getService();
                }
            });
            Map<String, Object> result = services.getParameterMap(socialLoginConfig);
            assertNotNull("Parameter map should not have been null.", result);
            assertEquals("Did not get expected number of entries in parameter map. Map was " + result, 2, result.size());
            assertNotNull("Map should have " + Constants.KEY_SOCIALLOGIN_SERVICE + " entry but did not. Map was " + result, result.get(Constants.KEY_SOCIALLOGIN_SERVICE));
            assertNotNull("Map should have " + Constants.KEY_SECURITY_SERVICE + " entry but did not. Map was " + result, result.get(Constants.KEY_SECURITY_SERVICE));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** doTwitter ****************************************/

    @Test
    public void doTwitter_getAccessToken_nullResponse() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTwitterTokenServices();
                    will(returnValue(twitterTokenServices));
                    one(twitterTokenServices).getAccessToken(request, response, twitterLoginConfig);
                    will(returnValue(null));
                }
            });
            handleErrorExpectations();

            servicesForTwitter.doTwitter(request, response, twitterLoginConfig);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doTwitter_getAccessToken_missingState() {
        try {
            // Request must have a non-empty state cookie value to be valid; set the state cookie value to null
            final Cookie[] cookies = new Cookie[] { cookie1 };
            getStateCookieExpectations(cookie1, null);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTwitterTokenServices();
                    will(returnValue(twitterTokenServices));
                    one(twitterTokenServices).getAccessToken(request, response, twitterLoginConfig);
                    will(returnValue(getAccessTokenMap));
                    allowing(request).getCookies();
                    will(returnValue(cookies));
                    one(twitterLoginConfig).getUniqueId();
                    will(returnValue(configId));
                }
            });
            handleErrorExpectations();

            servicesForTwitter.doTwitter(request, response, twitterLoginConfig);

            verifyLogMessageWithInserts(outputMgr, CWWKS5442E_TWITTER_STATE_MISSING, configId);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doTwitter_getAccessToken_emptyState() {
        try {
            // Request must have a non-empty state cookie value to be valid; set the state cookie value to be an empty string
            final Cookie[] cookies = new Cookie[] { cookie1 };
            getStateCookieExpectations(cookie1, "");

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTwitterTokenServices();
                    will(returnValue(twitterTokenServices));
                    one(twitterTokenServices).getAccessToken(request, response, twitterLoginConfig);
                    will(returnValue(getAccessTokenMap));
                    allowing(request).getCookies();
                    will(returnValue(cookies));
                    one(twitterLoginConfig).getUniqueId();
                    will(returnValue(configId));
                }
            });
            handleErrorExpectations();

            servicesForTwitter.doTwitter(request, response, twitterLoginConfig);

            verifyLogMessageWithInserts(outputMgr, CWWKS5442E_TWITTER_STATE_MISSING, configId);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doTwitter_getAccessToken_nullRequestUrlCookie() {
        try {
            // Request URL stored in cookie must be present and non-empty; set its value to null
            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };
            getStateCookieExpectations(cookie1, state);
            getRequestUrlCookieValue(cookie2, state, null);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTwitterTokenServices();
                    will(returnValue(twitterTokenServices));
                    one(twitterTokenServices).getAccessToken(request, response, twitterLoginConfig);
                    will(returnValue(getAccessTokenMap));
                    allowing(request).getCookies();
                    will(returnValue(cookies));
                    one(twitterLoginConfig).getUniqueId();
                    will(returnValue(configId));
                }
            });
            handleErrorExpectations();

            servicesForTwitter.doTwitter(request, response, twitterLoginConfig);

            verifyLogMessageWithInserts(outputMgr, CWWKS5443E_TWITTER_ORIGINAL_REQUEST_URL_MISSING_OR_EMPTY, configId);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doTwitter_getAccessToken_emptyRequestUrlCookie() {
        try {
            // Request URL stored in cookie must be present and non-empty; set its value to an empty string
            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };
            getStateCookieExpectations(cookie1, state);
            getRequestUrlCookieValue(cookie2, state, "");

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTwitterTokenServices();
                    will(returnValue(twitterTokenServices));
                    one(twitterTokenServices).getAccessToken(request, response, twitterLoginConfig);
                    will(returnValue(getAccessTokenMap));
                    allowing(request).getCookies();
                    will(returnValue(cookies));
                    one(twitterLoginConfig).getUniqueId();
                    will(returnValue(configId));
                }
            });
            handleErrorExpectations();

            servicesForTwitter.doTwitter(request, response, twitterLoginConfig);

            verifyLogMessageWithInserts(outputMgr, CWWKS5443E_TWITTER_ORIGINAL_REQUEST_URL_MISSING_OR_EMPTY, configId);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doTwitter_getAccessToken_invalidReqUrlCookie() {
        try {
            // Request URL stored in cookie must be present, non-empty, and a valid URL
            final String reqUrlValue = "Some invalid URL";
            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };
            getStateCookieExpectations(cookie1, state);
            getRequestUrlCookieValue(cookie2, state, reqUrlValue);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTwitterTokenServices();
                    will(returnValue(twitterTokenServices));
                    one(twitterTokenServices).getAccessToken(request, response, twitterLoginConfig);
                    will(returnValue(getAccessTokenMap));
                    allowing(request).getCookies();
                    will(returnValue(cookies));
                }
            });
            handleErrorExpectations();

            servicesForTwitter.doTwitter(request, response, twitterLoginConfig);

            // Spaces will remain encoded in the URL
            String encodedReqUrl = reqUrlValue.replaceAll(" ", "%20");

            verifyLogMessage(outputMgr, CWWKS5499E_REQUEST_URL_NOT_VALID + ".+" + CWWKS5496W_HTTP_URI_DOES_NOT_START_WITH_HTTP + ".+\\[" + encodedReqUrl + "\\]");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doTwitter_getAccessToken_emptyMap() {
        try {
            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };
            getStateCookieExpectations(cookie1, state);
            getRequestUrlCookieValue(cookie2, state, url);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTwitterTokenServices();
                    will(returnValue(twitterTokenServices));
                    // Result from getAccessToken() is an empty map
                    one(twitterTokenServices).getAccessToken(request, response, twitterLoginConfig);
                    will(returnValue(new HashMap<String, String>()));
                    allowing(request).getCookies();
                    will(returnValue(cookies));
                    // Cookies not added for access token and access token secret because those values aren't in the map
                    one(response).sendRedirect(url);
                }
            });

            servicesForTwitter.doTwitter(request, response, twitterLoginConfig);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doTwitter_getAccessToken_successful_redirectUrlContainsQuery() {
        try {
            final String urlWithQuery = url + "?and=query&string=vals";

            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };
            getStateCookieExpectations(cookie1, state);
            getRequestUrlCookieValue(cookie2, state, urlWithQuery);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTwitterTokenServices();
                    will(returnValue(twitterTokenServices));
                    // Good tokens included in the map
                    one(twitterTokenServices).getAccessToken(request, response, twitterLoginConfig);
                    will(returnValue(getAccessTokenMap));
                    allowing(request).getCookies();
                    will(returnValue(cookies));
                    one(referrerURLCookieHandler).createCookie(TwitterConstants.COOKIE_NAME_ACCESS_TOKEN, accessToken, request);
                    will(returnValue(cookie1));
                    one(response).addCookie(cookie1);
                    one(referrerURLCookieHandler).createCookie(TwitterConstants.COOKIE_NAME_ACCESS_TOKEN_SECRET, accessTokenSecret, request);
                    will(returnValue(cookie2));
                    one(response).addCookie(cookie2);
                    one(response).sendRedirect(urlWithQuery);
                }
            });

            servicesForTwitter.doTwitter(request, response, twitterLoginConfig);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doTwitter_getAccessToken_successful() {
        try {
            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };
            getStateCookieExpectations(cookie1, state);
            getRequestUrlCookieValue(cookie2, state, url);

            mockery.checking(new Expectations() {
                {
                    one(mockInterface).getTwitterTokenServices();
                    will(returnValue(twitterTokenServices));
                    // Good tokens included in the map
                    one(twitterTokenServices).getAccessToken(request, response, twitterLoginConfig);
                    will(returnValue(getAccessTokenMap));
                    allowing(request).getCookies();
                    will(returnValue(cookies));
                    one(referrerURLCookieHandler).createCookie(TwitterConstants.COOKIE_NAME_ACCESS_TOKEN, accessToken, request);
                    will(returnValue(cookie1));
                    one(response).addCookie(cookie1);
                    one(referrerURLCookieHandler).createCookie(TwitterConstants.COOKIE_NAME_ACCESS_TOKEN_SECRET, accessTokenSecret, request);
                    will(returnValue(cookie2));
                    one(response).addCookie(cookie2);
                    one(response).sendRedirect(url);
                }
            });

            servicesForTwitter.doTwitter(request, response, twitterLoginConfig);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** doRedirect ****************************************/

    @Test
    public void doRedirect_paramsIncludeError_missingValues() {
        try {
            // "error" parameter should result in an error response
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();
            paramMap.put(ClientConstants.STATE, new String[] { state });
            paramMap.put(ClientConstants.ERROR, new String[] {});
            paramMap.put("other", new String[] { "some other value" });

            mockery.checking(new Expectations() {
                {
                    one(request).getParameterMap();
                    will(returnValue(paramMap));
                    allowing(request).getParameter(ClientConstants.ERROR);
                    one(request).getParameter(ClientConstants.ERROR_URI);
                    one(request).getParameter(ClientConstants.ERROR_DESC);
                }
            });
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessageWithInserts(outputMgr, CWWKS5495E_REDIRECT_REQUEST_CONTAINED_ERROR);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_paramsIncludeError_includesValues() {
        try {
            // "error" parameter should result in an error response
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();
            paramMap.put(ClientConstants.STATE, new String[] { state });
            paramMap.put(ClientConstants.ERROR, new String[] { error });
            paramMap.put("other", new String[] { "some other value" });

            mockery.checking(new Expectations() {
                {
                    one(request).getParameterMap();
                    will(returnValue(paramMap));
                    allowing(request).getParameter(ClientConstants.ERROR);
                    will(returnValue(error));
                    one(request).getParameter(ClientConstants.ERROR_URI);
                    will(returnValue(url));
                    one(request).getParameter(ClientConstants.ERROR_DESC);
                    will(returnValue(errorDescription));
                }
            });
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessage(outputMgr, CWWKS5495E_REDIRECT_REQUEST_CONTAINED_ERROR);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_missingState() {
        try {
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();

            mockery.checking(new Expectations() {
                {
                    one(request).getParameterMap();
                    will(returnValue(paramMap));
                    allowing(request).getParameter(ClientConstants.ERROR);
                    will(returnValue(null));
                    // State parameter must be present and non-empty for the redirect request to be valid
                    allowing(request).getParameter(ClientConstants.STATE);
                    will(returnValue(null));
                }
            });
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessage(outputMgr, CWWKS5480E_STATE_NULL_OR_MISMATCHED);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_emptyState() {
        try {
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();

            mockery.checking(new Expectations() {
                {
                    one(request).getParameterMap();
                    will(returnValue(paramMap));
                    allowing(request).getParameter(ClientConstants.ERROR);
                    will(returnValue(null));
                    // State parameter must be present and non-empty for the redirect request to be valid
                    allowing(request).getParameter(ClientConstants.STATE);
                    will(returnValue(""));
                }
            });
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessage(outputMgr, CWWKS5480E_STATE_NULL_OR_MISMATCHED);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_noCookies() {
        try {
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();

            // No error param, state param is present, no request cookies; state cookie is missing
            getDoRedirectInitialExpectations(paramMap, state, null);
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessage(outputMgr, CWWKS5480E_STATE_NULL_OR_MISMATCHED);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_noStateCookie() {
        try {
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();
            final Cookie[] cookies = new Cookie[] { cookie1 };

            // No error param, state param is present, no state cookie
            getDoRedirectInitialExpectations(paramMap, state, cookies);
            mockery.checking(new Expectations() {
                {
                    allowing(cookie1).getName();
                    will(returnValue("someCookie"));
                    allowing(cookie1).getValue();
                    will(returnValue("Some cookie value"));
                }
            });
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessage(outputMgr, CWWKS5480E_STATE_NULL_OR_MISMATCHED);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_stateMismatch() {
        try {
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();
            final Cookie[] cookies = new Cookie[] { cookie1 };
            final String cookieValue = RandomUtils.getRandomSelection(null, "", state + "mismatch");

            // No error param, state param is present, state cookie value is either null, empty, or doesn't match the state parameter
            getDoRedirectInitialExpectations(paramMap, state, cookies);
            getStateCookieExpectations(cookie1, cookieValue);
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessage(outputMgr, CWWKS5480E_STATE_NULL_OR_MISMATCHED);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_missingReqUrlCookie() {
        try {
            // Include only the state cookie
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();
            final Cookie[] cookies = new Cookie[] { cookie1 };

            // Request URL cookie must be present and non-empty for the redirect to succeed
            getDoRedirectInitialExpectations(paramMap, state, cookies);
            getStateCookieExpectations(cookie1, state);
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessage(outputMgr, CWWKS5481E_REQUEST_URL_NULL_OR_EMPTY);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_emptyReqUrlCookie() {
        try {
            // Include request URL cookie
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();
            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };

            getDoRedirectInitialExpectations(paramMap, state, cookies);
            getStateCookieExpectations(cookie1, state);
            // Request URL cookie must be present and non-empty for the redirect to succeed
            getRequestUrlCookieValue(cookie2, state, "");
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessage(outputMgr, CWWKS5481E_REQUEST_URL_NULL_OR_EMPTY);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_invalidReqUrl() {
        try {
            // Include request URL cookie
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();
            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };

            getDoRedirectInitialExpectations(paramMap, state, cookies);
            getStateCookieExpectations(cookie1, state);
            // Request URL cookie must a valid URL for the redirect to succeed
            final String url = "some invalid URL";
            getRequestUrlCookieValue(cookie2, state, url);
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            // Spaces will remain encoded in the URL
            String encodedReqUrl = url.replaceAll(" ", "%20");

            verifyLogMessage(outputMgr, CWWKS5499E_REQUEST_URL_NOT_VALID + ".+" + CWWKS5496W_HTTP_URI_DOES_NOT_START_WITH_HTTP + ".+\\[" + encodedReqUrl + "\\]");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_missingCode() {
        try {
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();
            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };

            getDoRedirectInitialExpectations(paramMap, state, cookies);
            getStateCookieExpectations(cookie1, state);
            getRequestUrlCookieValue(cookie2, state, url);
            mockery.checking(new Expectations() {
                {
                    // Code parameter must be present in order to redirect
                    one(request).getParameter(ClientConstants.CODE);
                    will(returnValue(null));
                }
            });
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessage(outputMgr, CWWKS5494E_CODE_PARAMETER_NULL_OR_EMPTY);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_emptyCode() {
        try {
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();
            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };

            getDoRedirectInitialExpectations(paramMap, state, cookies);
            getStateCookieExpectations(cookie1, state);
            getRequestUrlCookieValue(cookie2, state, url);
            mockery.checking(new Expectations() {
                {
                    // Code parameter must be present and non-empty in order to redirect
                    one(request).getParameter(ClientConstants.CODE);
                    will(returnValue(""));
                }
            });
            handleErrorExpectations();

            services.doRedirect(request, response, socialLoginConfig);

            verifyLogMessage(outputMgr, CWWKS5494E_CODE_PARAMETER_NULL_OR_EMPTY);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void doRedirect_validCode() {
        try {
            final Map<String, String[]> paramMap = new HashMap<String, String[]>();
            final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };

            // No error param, state param present, state and redirect URL cookies present, state values match, redirect URL is valid URL
            getDoRedirectInitialExpectations(paramMap, state, cookies);
            getStateCookieExpectations(cookie1, state);
            getRequestUrlCookieValue(cookie2, state, url);
            mockery.checking(new Expectations() {
                {
                    one(request).getParameter(ClientConstants.CODE);
                    will(returnValue(code));
                    one(referrerURLCookieHandler).createCookie(ClientConstants.COOKIE_NAME_STATE_KEY, code, request);
                    will(returnValue(cookie1));
                    one(response).addCookie(cookie1);
                    one(response).sendRedirect(url);
                }
            });

            services.doRedirect(request, response, socialLoginConfig);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** handleSocialLoginAPIRequest ****************************************/

    // TODO

    /**************************************** getAllSocialLoginConfigs ****************************************/

    @SuppressWarnings("static-access")
    @Test
    public void getAllSocialLoginConfigs_noConfigRef() {
        try {
            services.socialLoginConfigRef = null;

            JSONObject result = services.getAllSocialLoginConfigs();

            assertNotNull("Result should not have been null but was.", result);
            assertTrue("Result should have been empty but was: " + result, result.isEmpty());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("static-access")
    @Test
    public void getAllSocialLoginConfigs_noConfigs() {
        try {
            services.socialLoginConfigRef = socialLoginConfigRef;

            Set<SocialLoginConfig> set = new HashSet<SocialLoginConfig>();
            final Iterator<SocialLoginConfig> iter = set.iterator();

            mockery.checking(new Expectations() {
                {
                    one(socialLoginConfigRef).getServices();
                    will(returnValue(iter));
                }
            });

            JSONObject result = services.getAllSocialLoginConfigs();

            assertNotNull("Result should not have been null but was.", result);
            assertTrue("Result should have " + ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA + " key but did not. Result was: " + result, result.containsKey(ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA));
            JSONArray mediaList = (JSONArray) result.get(ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA);
            assertEquals("Media list in result should have been empty but was: " + mediaList, 0, mediaList.size());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("static-access")
    @Test
    public void getAllSocialLoginConfigs_withConfigs() {
        try {
            services.socialLoginConfigRef = socialLoginConfigRef;

            Set<SocialLoginConfig> set = new HashSet<SocialLoginConfig>();
            set.add(socialLoginConfig);
            final Iterator<SocialLoginConfig> iter = set.iterator();

            mockery.checking(new Expectations() {
                {
                    one(socialLoginConfigRef).getServices();
                    will(returnValue(iter));
                    one(socialLoginConfig).getUniqueId();
                    will(returnValue(configId));
                    one(socialLoginConfig).getWebsite();
                    will(returnValue(null));
                    one(socialLoginConfig).getDisplayName();
                    will(returnValue(null));
                }
            });

            JSONObject result = services.getAllSocialLoginConfigs();

            assertNotNull("Result should not have been null but was.", result);
            assertTrue("Result should have " + ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA + " key but did not. Result was: " + result, result.containsKey(ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA));
            JSONArray mediaList = (JSONArray) result.get(ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA);
            assertEquals("Media list in result did not match expected size. List was: " + mediaList, 1, mediaList.size());

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @SuppressWarnings("static-access")
    @Test
    public void getAllSocialLoginConfigs_withConfigsAndOptionalData() {
        try {
            services.socialLoginConfigRef = socialLoginConfigRef;

            Set<SocialLoginConfig> set = new HashSet<SocialLoginConfig>();
            set.add(socialLoginConfig);
            final Iterator<SocialLoginConfig> iter = set.iterator();

            final String id = configId;
            final String website = "https://some-domain.com:80/context/path";
            final String displayName = "Some display name";
            mockery.checking(new Expectations() {
                {
                    one(socialLoginConfigRef).getServices();
                    will(returnValue(iter));
                    one(socialLoginConfig).getUniqueId();
                    will(returnValue(id));
                    one(socialLoginConfig).getWebsite();
                    will(returnValue(website));
                    one(socialLoginConfig).getDisplayName();
                    will(returnValue(displayName));
                }
            });

            JSONObject result = services.getAllSocialLoginConfigs();

            assertNotNull("Result should not have been null but was.", result);
            assertTrue("Result should have " + ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA + " key but did not. Result was: " + result, result.containsKey(ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA));
            JSONArray mediaList = (JSONArray) result.get(ConfigInfoJsonBuilder.KEY_ALL_SOCIAL_MEDIA);
            assertEquals("Media list in result did not match expected size. List was: " + mediaList, 1, mediaList.size());

            // Validate all information contained in the config object (id, website, display name)
            JSONObject configEntry = (JSONObject) mediaList.get(0);
            assertNull(ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_ID + " entry should have been null since its obscured value has not been initialized. Entry was: " + configEntry, configEntry.get(ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_ID));
            assertEquals(ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_WEBSITE + " entry did not match expected value. Entry was: " + configEntry, website, configEntry.get(ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_WEBSITE));
            assertEquals(ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_DISPLAY_NAME + " entry did not match expected value. Entry was: " + configEntry, displayName, configEntry.get(ConfigInfoJsonBuilder.KEY_SOCIAL_MEDIA_DISPLAY_NAME));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************************** Helper methods ****************************************/

    private void getStateCookieExpectations(Cookie cookie, String cookieValue) {
        final String cookieName = ClientConstants.COOKIE_NAME_STATE_KEY;
        getAndClearCookieExpectations(cookie, cookieName, cookieValue, true, response);
    }

    private void getRequestUrlCookieValue(Cookie cookie, String state, String cookieValue) {
        String reqUrlCookieName = ClientConstants.COOKIE_NAME_REQ_URL_PREFIX + state.hashCode();
        getAndClearCookieExpectations(cookie, reqUrlCookieName, cookieValue, true, response);
    }

    private void getDoRedirectInitialExpectations(final Map<String, String[]> paramMap, final String state, final Cookie[] cookies) {
        mockery.checking(new Expectations() {
            {
                one(request).getParameterMap();
                will(returnValue(paramMap));
                allowing(request).getParameter(ClientConstants.ERROR);
                will(returnValue(null));
                one(request).getParameter(ClientConstants.STATE);
                will(returnValue(state));
                allowing(request).getCookies();
                will(returnValue(cookies));
                one(request).getParameter(ClientConstants.STATE);
                will(returnValue(state));
            }
        });
    }

    private void getAllSocialLoginConfigsExpectations(final Iterator<SocialLoginConfig> iter) {
        mockery.checking(new Expectations() {
            {
                one(socialLoginConfigRef).getServices();
                will(returnValue(iter));
            }
        });
    }

    private void writeToResponseExpectations() throws IOException {
        addNoCacheHeadersExpectations();
        mockery.checking(new Expectations() {
            {
                one(response).setStatus(HttpServletResponse.SC_OK);
                one(response).setHeader(CommonWebConstants.HTTP_HEADER_CONTENT_TYPE, CommonWebConstants.HTTP_CONTENT_TYPE_JSON);
                one(response).getWriter();
                will(returnValue(writer));
                one(writer).write(with(any(String.class)));
                one(writer).flush();
                one(writer).close();
            }
        });
    }

    private void addNoCacheHeadersExpectations() {
        final String cacheControlHeaderVal = RandomUtils.getRandomSelection(null, "someValue");
        mockery.checking(new Expectations() {
            {
                one(response).getHeader(CommonWebConstants.HEADER_CACHE_CONTROL);
                will(returnValue(cacheControlHeaderVal));
                one(response).setHeader(CommonWebConstants.HEADER_PRAGMA, CommonWebConstants.PRAGMA_NO_CACHE);
            }
        });
        if (cacheControlHeaderVal == null) {
            mockery.checking(new Expectations() {
                {
                    one(response).setHeader(CommonWebConstants.HEADER_CACHE_CONTROL, CommonWebConstants.CACHE_CONTROL_NO_STORE);
                }
            });
        } else {
            mockery.checking(new Expectations() {
                {
                    one(response).setHeader(CommonWebConstants.HEADER_CACHE_CONTROL, cacheControlHeaderVal + ", " + CommonWebConstants.CACHE_CONTROL_NO_STORE);
                }
            });
        }
    }

}
