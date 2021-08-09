/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.web.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
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

import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.test.CommonTestClass;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import test.common.SharedOutputManager;

public class SocialWebUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    private static final String COOKIE_NAME = "MyCookie";
    private static final String LOGIN_URL = "http://localhost:8010/context/path/login";

    private final ReferrerURLCookieHandler referrerURLCookieHandler = mockery.mock(ReferrerURLCookieHandler.class);
    private final Cookie cookie1 = mockery.mock(Cookie.class, "cookie1");
    private final Cookie cookie2 = mockery.mock(Cookie.class, "cookie2");
    private final MockInterface mockInterface = mockery.mock(MockInterface.class);

    SocialWebUtils utils = new SocialWebUtils();

    public interface MockInterface {
        public String getUrlEncodedQueryString();

        public String getUrlEncodedQueryStringFromParameterMap();

        public String getUrlEncodedParameterAndValues();

        void savePostParameters();

        public void deleteCookie();

        Cookie createExpiredCookie();

        void removePostParameterSessionAttributes();
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new SocialWebUtils();
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

    /****************************************** doClientSideRedirect ******************************************/

    @Test
    public void doClientSideRedirect() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(response).setStatus(HttpServletResponse.SC_OK);
                one(response).getWriter();
                will(returnValue(writer));
                one(webAppSecConfig).getSSORequiresSSL();
                one(webAppSecConfig).getSameSiteCookie();
                allowing(writer).println(with(any(String.class)));
                allowing(response).setHeader(with(any(String.class)), with(any(String.class)));
                one(response).setDateHeader("Expires", 0);
                one(response).setContentType("text/html; charset=UTF-8");
                one(writer).close();
            }
        });

        utils.doClientSideRedirect(response, COOKIE_NAME, LOGIN_URL);
    }

    /****************************************** createJavaScriptForRedirect ******************************************/

    @Test
    public void createJavaScriptForRedirect() throws Exception {
        String scriptStart = "<script type=\"text/javascript\" language=\"javascript\">";
        String scriptEnd = "</script>";
        String cookieRegex = scriptStart + ".*document\\.cookie=\"" + COOKIE_NAME + "=\"\\+encodeURI\\(loc\\)\\+\"; path=/;[^\"]*\".*" + scriptEnd;
        String windowReplaceRegex = scriptStart + ".*window\\.location\\.replace\\(\"" + LOGIN_URL + "\"\\).*" + scriptEnd;

        mockery.checking(new Expectations() {
            {
                one(webAppSecConfig).getSSORequiresSSL();
                one(webAppSecConfig).getSameSiteCookie();
            }
        });

        String result = utils.createJavaScriptForRedirect(COOKIE_NAME, LOGIN_URL);

        Pattern pattern = Pattern.compile(cookieRegex);
        Matcher m = pattern.matcher(result);
        assertTrue("JavaScript did not contain expected cookie expression. Expected: [" + cookieRegex + "]. Result was: [" + result + "]", m.find());

        pattern = Pattern.compile(windowReplaceRegex);
        m = pattern.matcher(result);
        assertTrue("JavaScript did not contain expected window replacement expression. Expected: [" + windowReplaceRegex + "]. Result was: [" + result + "]", m.find());
    }

    /****************************************** getAndClearCookie ******************************************/

    @Test
    public void getAndClearCookie() throws Exception {
        final String MY_COOKIE_NAME = "MyCookie";
        final String MY_COOKIE_VALUE = "Some cookie value";
        final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };
        mockery.checking(new Expectations() {
            {
                one(request).getCookies();
                will(returnValue(cookies));
                allowing(cookie1).getName();
                allowing(cookie2).getName();
                will(returnValue(MY_COOKIE_NAME));
                one(cookie2).getValue();
                will(returnValue(MY_COOKIE_VALUE));
                one(cookie2).getPath();
                one(cookie2).getSecure();
                will(returnValue(RandomUtils.getRandomSelection(true, false)));
                one(response).addCookie(with(any(Cookie.class)));
            }
        });

        String result = utils.getAndClearCookie(request, response, MY_COOKIE_NAME);
        assertEquals("Cookie value did not match the expected value.", MY_COOKIE_VALUE, result);
    }

    @Test
    public void getAndClearCookie_cookieDoesNotExist() throws Exception {
        final String MY_COOKIE_NAME = "MyCookie";
        final Cookie[] cookies = new Cookie[] { cookie1, cookie2 };
        mockery.checking(new Expectations() {
            {
                one(request).getCookies();
                will(returnValue(cookies));
                allowing(cookie1).getName();
                allowing(cookie2).getName();
            }
        });

        String result = utils.getAndClearCookie(request, response, MY_COOKIE_NAME);
        assertNull("The cookie was not present, so the result should have been null but wasn't. Result: " + result, result);
    }

    /****************************************** getRequestUrlWithEncodedQueryString ******************************************/

    @Test
    public void getRequestUrlWithEncodedQueryString_nullQueryString() throws Exception {
        final String url = LOGIN_URL;
        mockery.checking(new Expectations() {
            {
                one(request).getRequestURL();
                will(returnValue(new StringBuffer(url)));
                one(request).getQueryString();
                will(returnValue(null));
            }
        });

        String result = utils.getRequestUrlWithEncodedQueryString(request);
        assertEquals("Result did not match expected value.", url, result);
    }

    @Test
    public void getRequestUrlWithEncodedQueryString_withQueryString() throws Exception {
        utils = new SocialWebUtils() {
            public String getUrlEncodedQueryString(HttpServletRequest req) {
                return mockInterface.getUrlEncodedQueryString();
            }
        };
        final String url = LOGIN_URL;
        final String query = "value=test&other%20stuff";
        mockery.checking(new Expectations() {
            {
                one(request).getRequestURL();
                will(returnValue(new StringBuffer(url)));
                one(request).getQueryString();
                will(returnValue(query));
                one(mockInterface).getUrlEncodedQueryString();
                will(returnValue(query));
            }
        });

        String result = utils.getRequestUrlWithEncodedQueryString(request);
        assertEquals("Result did not match expected value.", url + "?" + query, result);
    }

    /****************************************** getUrlEncodedQueryString ******************************************/

    @Test
    public void getUrlEncodedQueryString_noParams() throws Exception {
        final Map<String, String[]> params = new HashMap<String, String[]>();
        mockery.checking(new Expectations() {
            {
                one(request).getParameterMap();
                will(returnValue(params));
            }
        });

        String result = utils.getUrlEncodedQueryString(request);
        assertTrue("Result should have been empty, but was: [" + result + "].", result.isEmpty());
    }

    @Test
    public void getUrlEncodedQueryString_withParams() throws Exception {
        utils = new SocialWebUtils() {
            public String getUrlEncodedQueryStringFromParameterMap(Map<String, String[]> params) {
                return mockInterface.getUrlEncodedQueryStringFromParameterMap();
            }
        };
        final Map<String, String[]> params = new HashMap<String, String[]>();
        final String key1 = "key1";
        final String value1 = "value1";
        params.put(key1, new String[] { value1 });
        final String encodedQuery = "key1=value1&key2=value2";
        mockery.checking(new Expectations() {
            {
                one(request).getParameterMap();
                will(returnValue(params));
                one(mockInterface).getUrlEncodedQueryStringFromParameterMap();
                will(returnValue(encodedQuery));
            }
        });

        String result = utils.getUrlEncodedQueryString(request);
        assertEquals("Result did not match expected value.", encodedQuery, result);
    }

    /****************************************** getUrlEncodedQueryStringFromParameterMap ******************************************/

    @Test
    public void getUrlEncodedQueryStringFromParameterMap_noParams() throws Exception {
        final Map<String, String[]> params = new HashMap<String, String[]>();

        String result = utils.getUrlEncodedQueryStringFromParameterMap(params);
        assertTrue("Result should have been empty but was: [" + result + "].", result.isEmpty());
    }

    @Test
    public void getUrlEncodedQueryStringFromParameterMap_oneParams() throws Exception {
        utils = new SocialWebUtils() {
            public String getUrlEncodedParameterAndValues(String key, String[] values) {
                return mockInterface.getUrlEncodedParameterAndValues();
            }
        };
        final Map<String, String[]> params = new HashMap<String, String[]>();
        final String key1 = "key1";
        final String value1 = "value1";
        params.put(key1, new String[] { value1 });
        final String encodedQuery = "key1=value1";
        mockery.checking(new Expectations() {
            {
                one(mockInterface).getUrlEncodedParameterAndValues();
                will(returnValue(encodedQuery));
            }
        });

        String result = utils.getUrlEncodedQueryStringFromParameterMap(params);
        assertEquals("Result did not match expected value.", encodedQuery, result);
    }

    @Test
    public void getUrlEncodedQueryStringFromParameterMap_multipleParams() throws Exception {
        utils = new SocialWebUtils() {
            public String getUrlEncodedParameterAndValues(String key, String[] values) {
                return mockInterface.getUrlEncodedParameterAndValues();
            }
        };
        final Map<String, String[]> params = new HashMap<String, String[]>();
        final String key1 = "key1";
        final String value1 = "value1";
        final String key2 = "key2";
        final String value2 = "value2";
        params.put(key1, new String[] { value1 });
        params.put(key2, new String[] { value2 });
        final String encodedQuery1 = "key1=value1";
        final String encodedQuery2 = "key2=value2";
        mockery.checking(new Expectations() {
            {
                one(mockInterface).getUrlEncodedParameterAndValues();
                will(returnValue(encodedQuery1));
                one(mockInterface).getUrlEncodedParameterAndValues();
                will(returnValue(encodedQuery2));
            }
        });

        String expectedResult = encodedQuery1 + "&" + encodedQuery2;

        String result = utils.getUrlEncodedQueryStringFromParameterMap(params);
        assertEquals("Result did not match expected value.", expectedResult, result);
    }

    /****************************************** getUrlEncodedParameterAndValues ******************************************/

    @Test
    public void getUrlEncodedParameterAndValues_noValue() throws Exception {
        final String key = "key";

        String result = utils.getUrlEncodedParameterAndValues(key, new String[0]);
        assertEquals("Result did not match expected value.", key, result);
    }

    @Test
    public void getUrlEncodedParameterAndValues_withOneValue() throws Exception {
        String key = "key";
        String value = "value";

        String result = utils.getUrlEncodedParameterAndValues(key, new String[] { value });
        assertEquals("Result did not match expected value.", key + "=" + value, result);
    }

    @Test
    public void getUrlEncodedParameterAndValues_withOneValue_specialChars() throws Exception {
        String key = " k e y\t\r\n ";
        String value = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";
        String encodedKey = "+k+e+y" + "%09%0D%0A+";
        String encodedVal = "%60%7E%21%40%23%24%25%5E%26*%28%29" + "-_" + "%3D%2B%5B%7B%5D%7D%5C%7C%3B%3A%27%22%2C%3C" + "." + "%3E%2F%3F";
        String expectedResult = encodedKey + "=" + encodedVal;

        String result = utils.getUrlEncodedParameterAndValues(key, new String[] { value });
        assertEquals("Result did not match expected value.", expectedResult, result);
    }

    @Test
    public void getUrlEncodedParameterAndValues_withMultipleValue() throws Exception {
        String key = "key 1";
        String value1 = "value1";
        String value2 = "What? Okay!";
        String value3 = "\"Hello, world!\"";
        String encodedKey = "key+1";

        String expectedResult = encodedKey + "=" + value1;
        expectedResult += "&" + encodedKey + "=" + "What%3F+Okay%21";
        expectedResult += "&" + encodedKey + "=" + "%22Hello%2C+world%21%22";

        String result = utils.getUrlEncodedParameterAndValues(key, new String[] { value1, value2, value3 });
        assertEquals("Result did not match expected value.", expectedResult, result);
    }

    /****************************************** getLoginHint ******************************************/

    @Test
    public void test_getLoginHint_hintInHeader() throws Exception {
        final String hintValue = "my hint value";
        utils = new SocialWebUtils() {
            String getLoginHintFromHeader(HttpServletRequest request) {
                return hintValue;
            }
        };
        try {
            String result = utils.getLoginHint(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHint_hintInCookie() throws Exception {
        final String hintValue = "my hint value";
        utils = new SocialWebUtils() {
            String getLoginHintFromHeader(HttpServletRequest request) {
                return null;
            }

            String getLoginHintFromCookie(HttpServletRequest request) {
                return hintValue;
            }
        };
        try {
            String result = utils.getLoginHint(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHint_hintInParameter() throws Exception {
        final String hintValue = "my hint value";
        utils = new SocialWebUtils() {
            String getLoginHintFromHeader(HttpServletRequest request) {
                return "";
            }

            String getLoginHintFromCookie(HttpServletRequest request) {
                return null;
            }

            String getLoginHintFromParameter(HttpServletRequest request) {
                return hintValue;
            }
        };
        try {
            String result = utils.getLoginHint(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHint_hintNotFound() throws Exception {
        utils = new SocialWebUtils() {
            String getLoginHintFromHeader(HttpServletRequest request) {
                return null;
            }

            String getLoginHintFromCookie(HttpServletRequest request) {
                return "";
            }

            String getLoginHintFromParameter(HttpServletRequest request) {
                return null;
            }
        };
        try {
            String result = utils.getLoginHint(request);
            assertNull("Login hint should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getLoginHintFromHeader ******************************************/

    @Test
    public void test_getLoginHintFromHeader_null() throws Exception {
        final String hintValue = null;
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(ClientConstants.LOGIN_HINT);
                    will(returnValue(hintValue));
                }
            });
            String result = utils.getLoginHintFromHeader(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHintFromHeader_empty() throws Exception {
        final String hintValue = "";
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(ClientConstants.LOGIN_HINT);
                    will(returnValue(hintValue));
                }
            });
            String result = utils.getLoginHintFromHeader(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHintFromHeader_nonEmpty() throws Exception {
        final String hintValue = "\t my\n\r \r header..... value ";
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getHeader(ClientConstants.LOGIN_HINT);
                    will(returnValue(hintValue));
                }
            });
            String result = utils.getLoginHintFromHeader(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getLoginHintFromCookie ******************************************/

    @Test
    public void test_getLoginHintFromCookie_nullCookies() throws Exception {
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getCookies();
                    will(returnValue(null));
                }
            });
            String result = utils.getLoginHintFromCookie(request);
            assertNull("Returned login hint should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHintFromCookie_noCookies() throws Exception {
        try {
            final Cookie[] cookies = new Cookie[0];
            mockery.checking(new Expectations() {
                {
                    one(request).getCookies();
                    will(returnValue(cookies));
                }
            });
            String result = utils.getLoginHintFromCookie(request);
            assertNull("Returned login hint should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHintFromCookie_noHintCookie() throws Exception {
        try {
            final Cookie[] cookies = new Cookie[] { cookie1 };
            mockery.checking(new Expectations() {
                {
                    one(request).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(""));
                }
            });
            String result = utils.getLoginHintFromCookie(request);
            assertNull("Returned login hint should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHintFromCookie_hintCookiePartialNameMatch() throws Exception {
        try {
            final Cookie[] cookies = new Cookie[] { cookie1 };
            mockery.checking(new Expectations() {
                {
                    one(request).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(ClientConstants.LOGIN_HINT + "a"));
                }
            });
            String result = utils.getLoginHintFromCookie(request);
            assertNull("Returned login hint should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHintFromCookie_emptyHintCookie() throws Exception {
        try {
            final Cookie[] cookies = new Cookie[] { cookie1 };
            final String hintValue = "";
            mockery.checking(new Expectations() {
                {
                    one(request).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(ClientConstants.LOGIN_HINT));
                    one(cookie1).getValue();
                    will(returnValue(hintValue));
                }
            });
            String result = utils.getLoginHintFromCookie(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHintFromCookie_nonEmptyHintCookie() throws Exception {
        try {
            final Cookie[] cookies = new Cookie[] { cookie1 };
            final String hintValue = "some;\n cookie\t ,=; value\r";
            mockery.checking(new Expectations() {
                {
                    one(request).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(ClientConstants.LOGIN_HINT));
                    one(cookie1).getValue();
                    will(returnValue(hintValue));
                }
            });
            String result = utils.getLoginHintFromCookie(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** getLoginHintFromParameter ******************************************/

    @Test
    public void test_getLoginHintFromParameter_null() throws Exception {
        final String hintValue = null;
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getParameter(ClientConstants.LOGIN_HINT);
                    will(returnValue(hintValue));
                }
            });
            String result = utils.getLoginHintFromParameter(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHintFromParameter_empty() throws Exception {
        final String hintValue = "";
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getParameter(ClientConstants.LOGIN_HINT);
                    will(returnValue(hintValue));
                }
            });
            String result = utils.getLoginHintFromParameter(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_getLoginHintFromParameter_nonEmpty() throws Exception {
        final String hintValue = "\t my\n\r \r parameter..... value ";
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getParameter(ClientConstants.LOGIN_HINT);
                    will(returnValue(hintValue));
                }
            });
            String result = utils.getLoginHintFromParameter(request);
            assertEquals("Returned login hint did not match expected value.", hintValue, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** saveRequestUrlAndParameters ******************************************/

    @Test
    public void test_saveRequestUrlAndParameters() throws Exception {
        final String reqUrlAndQuery = LOGIN_URL + "?param=value";
        utils = new SocialWebUtils() {
            protected ReferrerURLCookieHandler getCookieHandler() {
                return referrerURLCookieHandler;
            }

            public String getRequestUrlWithEncodedQueryString(HttpServletRequest req) {
                return reqUrlAndQuery;
            }

            void savePostParameters(HttpServletRequest request, HttpServletResponse response) {
                mockInterface.savePostParameters();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(referrerURLCookieHandler).createCookie(ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME, reqUrlAndQuery, request);
                    will(returnValue(cookie1));
                    one(response).addCookie(cookie1);
                    one(mockInterface).savePostParameters();
                }
            });
            utils.saveRequestUrlAndParameters(request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** removeRequestUrlAndParameters ******************************************/

    @Test
    public void test_removeRequestUrlAndParameters_postDataInCookie() throws Exception {
        utils = new SocialWebUtils() {
            protected ReferrerURLCookieHandler getCookieHandler() {
                return referrerURLCookieHandler;
            }

            boolean isPostDataSavedInCookie(WebAppSecurityConfig webAppSecConfig) {
                return true;
            }

            public void deleteCookie(HttpServletRequest request, HttpServletResponse response, String cookieName, WebAppSecurityConfig webAppSecConfig) {
                mockInterface.deleteCookie();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(referrerURLCookieHandler).invalidateReferrerURLCookie(request, response, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
                    one(mockInterface).deleteCookie();
                }
            });
            utils.removeRequestUrlAndParameters(request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_removeRequestUrlAndParameters_postDataNotInCookie() throws Exception {
        utils = new SocialWebUtils() {
            protected ReferrerURLCookieHandler getCookieHandler() {
                return referrerURLCookieHandler;
            }

            boolean isPostDataSavedInCookie(WebAppSecurityConfig webAppSecConfig) {
                return false;
            }

            void removePostParameterSessionAttributes(HttpServletRequest request) {
                mockInterface.removePostParameterSessionAttributes();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(referrerURLCookieHandler).invalidateReferrerURLCookie(request, response, ReferrerURLCookieHandler.REFERRER_URL_COOKIENAME);
                    one(mockInterface).removePostParameterSessionAttributes();
                }
            });
            utils.removeRequestUrlAndParameters(request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** deleteCookie ******************************************/

    @Test
    public void test_deleteCookie() throws Exception {
        final String cookieName = "myCookie";
        utils = new SocialWebUtils() {
            protected ReferrerURLCookieHandler getCookieHandler() {
                return referrerURLCookieHandler;
            }

            Cookie createExpiredCookie(HttpServletRequest request, String cookieName, WebAppSecurityConfig webAppSecConfig) {
                return mockInterface.createExpiredCookie();
            }
        };
        try {
            mockery.checking(new Expectations() {
                {
                    one(referrerURLCookieHandler).clearReferrerURLCookie(request, response, cookieName);
                    one(mockInterface).createExpiredCookie();
                    will(returnValue(cookie1));
                    one(response).addCookie(cookie1);
                }
            });
            utils.deleteCookie(request, response, cookieName, webAppSecConfig);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /****************************************** createExpiredCookie ******************************************/

    @Test
    public void test_createExpiredCookie_notHttpOnly_notSecure() throws Exception {
        final String cookieName = "myCookie";
        final String requestUri = "some uri value";
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getRequestURI();
                    will(returnValue(requestUri));
                    one(webAppSecConfig).getHttpOnlyCookies();
                    will(returnValue(false));
                    one(webAppSecConfig).getSSORequiresSSL();
                    will(returnValue(false));
                }
            });
            Cookie result = utils.createExpiredCookie(request, cookieName, webAppSecConfig);
            assertEquals("Name value did not match expected value.", cookieName, result.getName());
            assertEquals("Cookie value did not match expected value.", "", result.getValue());
            assertEquals("Path value did not match expected value.", requestUri, result.getPath());
            assertEquals("Max age value should have been set to 0 to create an expired cookie.", 0, result.getMaxAge());
            assertFalse("Cookie should not have had the HttpOnly flag set, but did. Cookie was [" + result + "].", result.isHttpOnly());
            assertFalse("Cookie should not have had the Secure flag set, but did. Cookie was [" + result + "].", result.getSecure());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createExpiredCookie_gttpOnly_secure() throws Exception {
        final String cookieName = "myCookie";
        final String requestUri = LOGIN_URL;
        try {
            mockery.checking(new Expectations() {
                {
                    one(request).getRequestURI();
                    will(returnValue(requestUri));
                    one(webAppSecConfig).getHttpOnlyCookies();
                    will(returnValue(true));
                    one(webAppSecConfig).getSSORequiresSSL();
                    will(returnValue(true));
                }
            });
            Cookie result = utils.createExpiredCookie(request, cookieName, webAppSecConfig);
            assertEquals("Name value did not match expected value.", cookieName, result.getName());
            assertEquals("Cookie value did not match expected value.", "", result.getValue());
            assertEquals("Path value did not match expected value.", requestUri, result.getPath());
            assertEquals("Max age value should have been set to 0 to create an expired cookie.", 0, result.getMaxAge());
            assertTrue("Cookie should have had the HttpOnly flag set, but did not. Cookie was [" + result + "].", result.isHttpOnly());
            assertTrue("Cookie should have had the Secure flag set, but did not. Cookie was [" + result + "].", result.getSecure());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}