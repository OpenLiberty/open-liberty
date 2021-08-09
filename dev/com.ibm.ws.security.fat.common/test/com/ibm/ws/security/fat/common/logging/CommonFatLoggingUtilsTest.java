/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.logging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseTitleExpectation;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class CommonFatLoggingUtilsTest extends CommonTestClass {

    private static final String LOG_STRING_EXPECTATION_WILL_NEVER_BE_PROCESSED = "This expectation will never be processed";

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private static final String LOG_STRING_START_REQUEST_PARTS = "Start Request Parts";
    private static final String LOG_STRING_END_REQUEST_PARTS = "End Request Parts";
    private static final String LOG_STRING_COOKIE = "Cookie: ";
    private static final String LOG_STRING_REQUEST_URL = "Request URL: ";
    private static final String LOG_STRING_REQUEST_HEADER = "Request header: ";
    private static final String LOG_STRING_REQUEST_PARAMETER = "Request parameter: ";
    private static final String LOG_STRING_ACTIONS = "Actions: ";
    private static final String LOG_STRING_ACTION = "Action: ";
    private static final String LOG_STRING_VALIDATE_AGAINST = "Validate against: ";
    private static final String LOG_STRING_RESPONSE_CLASS = "Response class: ";
    private static final String LOG_STRING_RESPONSE_STATUS_CODE = "Response (StatusCode): ";
    private static final String LOG_STRING_RESPONSE_TITLE = "Response (Title): ";
    private static final String LOG_STRING_RESPONSE_URL = "Response (Url): ";
    private static final String LOG_STRING_RESPONSE_HEADER = "Response (Header): ";
    private static final String LOG_STRING_RESPONSE_MESSAGE = "Response (Message): ";
    private static final String LOG_STRING_RESPONSE_FULL = "Response (Full): ";

    private final String SIMPLE_URL = "http://localhost";
    private final String ACTION1 = "action1";
    private final String ACTION2 = "action2";
    private final String ACTION3 = "action3";
    private final String ACTION4 = "action4";
    private final String TITLE = "Page Title";
    private final String HEADER_NAME = "headerName";
    private final String HEADER_VALUE = "headerValue";
    private final String STATUS_MESSAGE = "Status message";
    private final String FULL_RESPONSE = "<html><body>Hello, world!</body></html>";

    private final WebRequest webRequest = mockery.mock(WebRequest.class);
    private final WebClient webClient = mockery.mock(WebClient.class);
    private final CookieManager cookieManager = mockery.mock(CookieManager.class);
    private final Cookie cookie1 = mockery.mock(Cookie.class, "cookie1");
    private final Cookie cookie2 = mockery.mock(Cookie.class, "cookie2");
    private final Cookie cookie3 = mockery.mock(Cookie.class, "cookie3");
    private final com.gargoylesoftware.htmlunit.WebResponse htmlunitWebResponse = mockery.mock(com.gargoylesoftware.htmlunit.WebResponse.class);
    private final com.gargoylesoftware.htmlunit.html.HtmlPage htmlunitHtmlPage = mockery.mock(com.gargoylesoftware.htmlunit.html.HtmlPage.class);

    CommonFatLoggingUtils utils = new CommonFatLoggingUtils();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new CommonFatLoggingUtils();
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

    /************************************** printClassName **************************************/

    /**
     * Tests:
     * - Class name: null
     * Expects:
     * - Appropriate "starting test" string to be output with matching class name
     */
    @Test
    public void test_printClassName_null() {
        try {
            String className = null;

            utils.printClassName(className);

            String checkForString = "Starting test class: " + className;
            assertStringInTraceAndStandardOut(checkForString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Class name: Empty
     * Expects:
     * - Appropriate "starting test" string to be output with matching class name
     */
    @Test
    public void test_printClassName_empty() {
        try {
            String className = "";

            utils.printClassName(className);

            String checkForString = "Starting test class: " + className;
            assertStringInTraceAndStandardOut(checkForString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Class name: Non-empty
     * Expects:
     * - Appropriate "starting test" string to be output with matching class name
     */
    @Test
    public void test_printClassName_nonEmpty() {
        try {
            String className = "Some.NonEmpty ClassName";

            utils.printClassName(className);

            String checkForString = "Starting test class: " + className;
            assertStringInTraceAndStandardOut(checkForString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** printMethodName **************************************/

    /**
     * Tests:
     * - Method name: null
     * Expects:
     * - Appropriate method name will be output in trace but not standard out
     */
    @Test
    public void test_printMethodName_null() {
        try {
            String methodName = null;

            utils.printMethodName(methodName);

            String checkForString = CommonFatLoggingUtils.PRINT_DELIMITER_METHOD_NAME + " " + methodName;
            assertStringInTrace(outputMgr, checkForString);
            assertStringNotInStandardOut(outputMgr, checkForString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Method name: Empty
     * Expects:
     * - Appropriate method name will be output in trace but not standard out
     */
    @Test
    public void test_printMethodName_empty() {
        try {
            String methodName = "";

            utils.printMethodName(methodName);

            String checkForString = CommonFatLoggingUtils.PRINT_DELIMITER_METHOD_NAME + " " + methodName;
            assertStringInTrace(outputMgr, checkForString);
            assertStringNotInStandardOut(outputMgr, checkForString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Method name: Non-empty
     * Expects:
     * - Appropriate method name will be output in trace but not standard out
     */
    @Test
    public void test_printMethodName_nonEmpty() {
        try {
            String methodName = "Some.NonEmpty MethodName";

            utils.printMethodName(methodName);

            String checkForString = CommonFatLoggingUtils.PRINT_DELIMITER_METHOD_NAME + " " + methodName;
            assertStringInTrace(outputMgr, checkForString);
            assertStringNotInStandardOut(outputMgr, checkForString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Method name: null
     * - Task: null
     * Expects:
     * - Appropriate method name and task will be output in trace but not standard out
     */
    @Test
    public void test_printMethodName_nullName_nullTask() {
        try {
            String methodName = null;
            String task = null;

            utils.printMethodName(methodName, task);

            String checkForString = CommonFatLoggingUtils.PRINT_DELIMITER_METHOD_NAME + " " + task + " " + methodName;
            assertStringInTrace(outputMgr, checkForString);
            assertStringNotInStandardOut(outputMgr, checkForString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Method name: Non-empty
     * - Task: Empty
     * Expects:
     * - Appropriate method name and task will be output in trace but not standard out
     */
    @Test
    public void test_printMethodName_emptyTask() {
        try {
            String methodName = "myMethodName";
            String task = "";

            utils.printMethodName(methodName, task);

            String checkForString = CommonFatLoggingUtils.PRINT_DELIMITER_METHOD_NAME + " " + task + " " + methodName;
            assertStringInTrace(outputMgr, checkForString);
            assertStringNotInStandardOut(outputMgr, checkForString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Method name: Non-empty
     * - Task: Non-empty
     * Expects:
     * - Appropriate method name and task will be output in trace but not standard out
     */
    @Test
    public void test_printMethodName_nonEmptyTask() {
        try {
            String methodName = "myMethodName";
            String task = "Some task name";

            utils.printMethodName(methodName, task);

            String checkForString = CommonFatLoggingUtils.PRINT_DELIMITER_METHOD_NAME + " " + task + " " + methodName;
            assertStringInTrace(outputMgr, checkForString);
            assertStringNotInStandardOut(outputMgr, checkForString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** printRequestParts **************************************/

    /**
     * Tests:
     * - WebRequest: null
     * - Test name: null
     * Expects:
     * - "Start request parts" line should be printed
     * - Nothing else should be printed
     */
    @Test
    public void test_printRequestParts_nullRequestAndTestName() {
        try {
            WebRequest request = null;
            String test = null;

            utils.printRequestParts(request, test);

            printRequestPartsExpectations(null, null, null, null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - WebRequest: null
     * - Test name: Non-null
     * Expects:
     * - "Start request parts" line should be printed
     * - Nothing else should be printed
     */
    @Test
    public void test_printRequestParts_nullRequest() {
        try {
            WebRequest request = null;
            String test = "someTestName";

            utils.printRequestParts(request, test);

            printRequestPartsExpectations(null, null, null, null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - WebRequest: Non-null
     * - Test name: Non-null
     * Expects:
     * - "Start request parts", request URL, and "end request parts" lines should be printed
     */
    @Test
    public void test_printRequestParts_noWebClientArg() {
        try {
            String test = "someTestName";

            simpleRequestUrlHeaderAndParameterExpectations();

            utils.printRequestParts(webRequest, test);

            printRequestPartsExpectations(null, SIMPLE_URL, null, null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - WebClient: null
     * - No request headers
     * - No request parameters
     * Expects:
     * - "Start request parts", request URL, and "end request parts" lines should be printed
     */
    @Test
    public void test_printRequestParts_nullWebClientArg() {
        try {
            WebClient webClient = null;
            String test = "someTestName";

            simpleRequestUrlHeaderAndParameterExpectations();

            utils.printRequestParts(webClient, webRequest, test);

            printRequestPartsExpectations(null, SIMPLE_URL, null, null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - No cookies
     * - Includes request headers
     * - No request parameters
     * Expects:
     * - "Start request parts", request URL, request headers, and "end request parts" lines should be printed
     */
    @Test
    public void test_printRequestParts_noCookies_withRequestHeaders() {
        try {
            String test = "someTestName";

            final Map<String, String> headers = new HashMap<String, String>();
            headers.put("header1", "value1");

            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webClient).getCookieManager();
                    will(returnValue(cookieManager));
                    one(cookieManager).getCookies();
                    will(returnValue(null));
                    one(webRequest).getUrl();
                    will(returnValue(new URL(SIMPLE_URL)));
                    one(webRequest).getAdditionalHeaders();
                    will(returnValue(headers));
                    one(webRequest).getRequestParameters();
                    one(webRequest).getRequestBody();
                }
            });

            utils.printRequestParts(webClient, webRequest, test);

            printRequestPartsExpectations(null, SIMPLE_URL, "header1", null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Includes cookies
     * - No request headers
     * - Includes request parameters
     * Expects:
     * - "Start request parts", cookies, request URL, request parameters, and "end request parts" lines should be printed
     */
    @Test
    public void test_printRequestParts_withCookies_withRequestParameters() {
        try {
            String test = "someTestName";
            final Set<Cookie> cookies = new HashSet<Cookie>();
            cookies.add(cookie1);
            final List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
            requestParams.add(new NameValuePair("paramName", "paramValue"));

            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webClient).getCookieManager();
                    will(returnValue(cookieManager));
                    one(cookieManager).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue("cookieName"));
                    one(cookie1).getValue();
                    will(returnValue("cookieValue"));
                    one(webRequest).getUrl();
                    will(returnValue(new URL(SIMPLE_URL)));
                    one(webRequest).getAdditionalHeaders();
                    one(webRequest).getRequestParameters();
                    will(returnValue(requestParams));
                    one(webRequest).getRequestBody();
                }
            });

            utils.printRequestParts(webClient, webRequest, test);

            printRequestPartsExpectations("cookieName", SIMPLE_URL, null, "paramName");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** printAllCookies **************************************/

    /**
     * Tests:
     * - Web client: null
     * Expects:
     * - No cookies should be output
     */
    @Test
    public void test_printAllCookies_nullClient() {
        try {
            WebClient webClient = null;

            utils.printAllCookies(webClient);

            assertStringNotInStandardOut(outputMgr, LOG_STRING_COOKIE);
            assertStringNotInTrace(outputMgr, LOG_STRING_COOKIE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie set is null
     * Expects:
     * - No cookies should be output
     */
    @Test
    public void test_printAllCookies_nullCookieSet() {
        try {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webClient).getCookieManager();
                    will(returnValue(cookieManager));
                    one(cookieManager).getCookies();
                    will(returnValue(null));
                }
            });

            utils.printAllCookies(webClient);

            assertStringNotInStandardOut(outputMgr, LOG_STRING_COOKIE);
            assertStringNotInTrace(outputMgr, LOG_STRING_COOKIE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie set is empty
     * Expects:
     * - No cookies should be output
     */
    @Test
    public void test_printAllCookies_emptyCookieSet() {
        try {
            final Set<Cookie> cookies = new HashSet<Cookie>();
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webClient).getCookieManager();
                    will(returnValue(cookieManager));
                    one(cookieManager).getCookies();
                    will(returnValue(cookies));
                }
            });

            utils.printAllCookies(webClient);

            assertStringNotInStandardOut(outputMgr, LOG_STRING_COOKIE);
            assertStringNotInTrace(outputMgr, LOG_STRING_COOKIE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One cookie, cookie name and value are null
     * Expects:
     * - Appropriate cookie name and value appears in trace, but not standard out
     */
    @Test
    public void test_printAllCookies_oneCookie_nameAndValueNull() {
        try {
            final Set<Cookie> cookies = new HashSet<Cookie>();
            cookies.add(cookie1);
            final String cookieName = null;
            final String cookieValue = null;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webClient).getCookieManager();
                    will(returnValue(cookieManager));
                    one(cookieManager).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(cookieName));
                    one(cookie1).getValue();
                    will(returnValue(cookieValue));
                }
            });

            utils.printAllCookies(webClient);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_COOKIE) + cookieName + ".+: " + cookieValue);
            assertStringNotInStandardOut(outputMgr, LOG_STRING_COOKIE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple cookies
     * Expects:
     * - Appropriate cookie names and values appears in trace, but not standard out
     */
    @Test
    public void test_printAllCookies_multipleCookies() {
        try {
            final Set<Cookie> cookies = new HashSet<Cookie>();
            cookies.add(cookie1);
            cookies.add(cookie2);
            cookies.add(cookie3);
            final String cookieName = "cookie1";
            final String cookieNameWithSpace = "cookie 2";
            final String cookieNameWhitespace = " cookie 3 ";
            final String cookieValueNull = null;
            final String cookieValueEmpty = "";
            final String cookieValueNonEmpty = "My cookie value";
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webClient).getCookieManager();
                    will(returnValue(cookieManager));
                    one(cookieManager).getCookies();
                    will(returnValue(cookies));
                    one(cookie1).getName();
                    will(returnValue(cookieName));
                    one(cookie1).getValue();
                    will(returnValue(cookieValueNull));
                    one(cookie2).getName();
                    will(returnValue(cookieNameWithSpace));
                    one(cookie2).getValue();
                    will(returnValue(cookieValueEmpty));
                    one(cookie3).getName();
                    will(returnValue(cookieNameWhitespace));
                    one(cookie3).getValue();
                    will(returnValue(cookieValueNonEmpty));
                }
            });

            utils.printAllCookies(webClient);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_COOKIE) + cookieName + ".+: " + cookieValueNull);
            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_COOKIE) + cookieNameWithSpace + ".+: " + cookieValueEmpty);
            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_COOKIE) + cookieNameWhitespace + ".+: " + cookieValueNonEmpty);
            assertStringNotInStandardOut(outputMgr, LOG_STRING_COOKIE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** printRequestHeaders **************************************/

    /**
     * Tests:
     * - Null request headers
     * Expects:
     * - Nothing should be logged
     */
    @Test
    public void test_printRequestHeaders_nullHeaders() {
        try {
            String test = "someTestName";
            final Map<String, String> requestHeaders = null;

            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webRequest).getAdditionalHeaders();
                    will(returnValue(requestHeaders));
                }
            });

            utils.printRequestHeaders(webRequest, test);

            assertStringNotInTrace(outputMgr, LOG_STRING_REQUEST_PARAMETER);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - No request headers
     * Expects:
     * - Nothing should be logged
     */
    @Test
    public void test_printRequestHeaders_noHeaders() {
        try {
            String test = "someTestName";
            final Map<String, String> requestHeaders = new HashMap<String, String>();

            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webRequest).getAdditionalHeaders();
                    will(returnValue(requestHeaders));
                }
            });

            utils.printRequestHeaders(webRequest, test);

            assertStringNotInTrace(outputMgr, LOG_STRING_REQUEST_PARAMETER);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One request header
     * Expects:
     * - Header should be logged
     */
    @Test
    public void test_printRequestHeaders_oneHeader() {
        try {
            String test = "someTestName";
            final Map<String, String> requestHeaders = new HashMap<String, String>();
            requestHeaders.put("name", "value");

            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webRequest).getAdditionalHeaders();
                    will(returnValue(requestHeaders));
                }
            });

            utils.printRequestHeaders(webRequest, test);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_REQUEST_HEADER) + "name" + ".+: " + "value");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple request headers
     * Expects:
     * - All headers should be logged
     */
    @Test
    public void test_printRequestHeaders_multipleHeaders() {
        try {
            String test = "someTestName";
            final Map<String, String> requestHeaders = new HashMap<String, String>();
            requestHeaders.put(null, "nullName");
            requestHeaders.put("", "emptyName");
            requestHeaders.put("nullValue", null);
            requestHeaders.put("emptyValue", "");
            requestHeaders.put("Complex value", "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?");

            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webRequest).getAdditionalHeaders();
                    will(returnValue(requestHeaders));
                }
            });

            utils.printRequestHeaders(webRequest, test);

            for (Entry<String, String> header : requestHeaders.entrySet()) {
                String key = (header.getKey() == null) ? null : Pattern.quote(header.getKey());
                String value = (header.getValue() == null) ? null : Pattern.quote(header.getValue());
                assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_REQUEST_HEADER) + key + ".+: " + value);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** printRequestParameters **************************************/

    /**
     * Tests:
     * - Null request parameters
     * Expects:
     * - Nothing should be logged
     */
    @Test
    public void test_printRequestParameters_nullParameters() {
        try {
            String test = "someTestName";
            final List<NameValuePair> requestParams = null;

            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webRequest).getRequestParameters();
                    will(returnValue(requestParams));
                }
            });

            utils.printRequestParameters(webRequest, test);

            assertStringNotInTrace(outputMgr, LOG_STRING_REQUEST_PARAMETER);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - No request parameters
     * Expects:
     * - Nothing should be logged
     */
    @Test
    public void test_printRequestParameters_noParameters() {
        try {
            String test = "someTestName";
            final List<NameValuePair> requestParams = new ArrayList<NameValuePair>();

            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webRequest).getRequestParameters();
                    will(returnValue(requestParams));
                }
            });

            utils.printRequestParameters(webRequest, test);

            assertStringNotInTrace(outputMgr, LOG_STRING_REQUEST_PARAMETER);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One request parameter
     * Expects:
     * - Parameter should be logged
     */
    @Test
    public void test_printRequestParameters_oneParameter() {
        try {
            String test = "someTestName";
            final List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
            requestParams.add(new NameValuePair("name", "value"));

            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webRequest).getRequestParameters();
                    will(returnValue(requestParams));
                }
            });

            utils.printRequestParameters(webRequest, test);

            assertRegexInTrace(outputMgr, LOG_STRING_REQUEST_PARAMETER + "name" + ".+: " + "value");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple request parameters
     * Expects:
     * - All parameters should be logged
     */
    @Test
    public void test_printRequestParameters_multipleParameters() {
        try {
            String test = "someTestName";
            final List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
            requestParams.add(new NameValuePair(null, "nullName"));
            requestParams.add(new NameValuePair("", "emptyName"));
            requestParams.add(new NameValuePair("nullValue", null));
            requestParams.add(new NameValuePair("emptyValue", ""));
            requestParams.add(new NameValuePair("Complex value", "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?"));

            mockery.checking(new org.jmock.Expectations() {
                {
                    one(webRequest).getRequestParameters();
                    will(returnValue(requestParams));
                }
            });

            utils.printRequestParameters(webRequest, test);

            for (NameValuePair pair : requestParams) {
                String name = (pair.getName() == null) ? null : Pattern.quote(pair.getName());
                String value = (pair.getValue() == null) ? null : Pattern.quote(pair.getValue());
                assertRegexInTrace(outputMgr, LOG_STRING_REQUEST_PARAMETER + name + ".+: " + value);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** printResponseParts **************************************/

    /**
     * Tests:
     * - WebRequest: null
     * - Test name: null
     * Expects:
     * - Start of request content should be logged
     * - Nothing else should be logged
     */
    @Test
    public void test_printResponseParts_nullArgs() {
        try {
            Object response = null;
            String testName = null;

            utils.printResponseParts(response, testName);

            assertStartResponseContentLogged();
            assertStringInTrace(outputMgr, "nothing to print");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - WebRequest: null
     * - Test name: Non-null
     * Expects:
     * - Start of request content should be logged
     * - Nothing else should be logged
     */
    @Test
    public void test_printResponseParts_nullResponseObject() {
        try {
            Object response = null;

            utils.printResponseParts(response, testName.getMethodName());

            assertStartResponseContentLogged();
            assertStringInTrace(outputMgr, "nothing to print");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request: Unsupported type
     * - Test name: Non-null
     * Expects:
     * - Start of request content should be logged
     * - Error should be logged for unknown response type
     */
    @Test
    public void test_printResponseParts_unknownResponseObject() {
        try {
            Object response = "Unsupported response type";

            utils.printResponseParts(response, testName.getMethodName());

            assertResponseContentStartAndClassAreLogged(String.class.getName());
            assertErrorPrintingResponseContent();
            assertStringInTrace(outputMgr, "Unknown response type");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request: com.gargoylesoftware.htmlunit.WebResponse
     * Expects:
     * - Start of request content should be logged
     * - Status code should be logged
     * - Should log error because getting the title of a WebResponse object is not supported
     */
    @Test
    public void test_printResponseParts_htmlunitWebResponse() {
        try {
            final int statusCode = -1;
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitWebResponse).getStatusCode();
                    will(returnValue(statusCode));
                }
            });

            utils.printResponseParts(htmlunitWebResponse, testName.getMethodName());

            assertResponseContentStartAndClassAreLogged(htmlunitWebResponse.getClass().getName());
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_STATUS_CODE + statusCode);
            assertErrorPrintingResponseContent();
            assertStringInTrace(outputMgr, "Getting title not supported");

            assertStringNotInTrace(outputMgr, LOG_STRING_RESPONSE_TITLE);
            assertStringNotInTrace(outputMgr, LOG_STRING_RESPONSE_HEADER);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Not considered HTML (therefore title will not be logged)
     * - Headers, status code, and response content all mocked to be null
     * Expects:
     * - Start of request content should be logged
     * - Status code, URL, status message, and full response content should be logged
     */
    @Test
    public void test_printResponseParts_htmlunitHtmlPage_notHtml_nullContent() {
        try {
            final int statusCode = 200;
            final URL url = new URL(SIMPLE_URL);
            mockery.checking(new org.jmock.Expectations() {
                {
                    allowing(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusCode();
                    will(returnValue(statusCode));
                    one(htmlunitHtmlPage).isHtmlPage();
                    will(returnValue(false));
                    one(htmlunitHtmlPage).getUrl();
                    will(returnValue(url));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(null));
                    one(htmlunitWebResponse).getStatusMessage();
                    will(returnValue(null));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(null));
                }
            });

            utils.printResponseParts(htmlunitHtmlPage, testName.getMethodName());

            assertResponseContentStartAndClassAreLogged(htmlunitHtmlPage.getClass().getName());
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_STATUS_CODE + statusCode);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_URL + SIMPLE_URL);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_MESSAGE + null);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_FULL + null);

            assertStringNotInTrace(outputMgr, LOG_STRING_RESPONSE_TITLE);
            assertStringNotInTrace(outputMgr, LOG_STRING_RESPONSE_HEADER);
            assertStringNotInTrace(outputMgr, "Error printing response");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Includes headers
     * Expects:
     * - Start of request content should be logged
     * - Status code, title, URL, headers, status message, and full response content should be logged
     */
    @Test
    public void test_printResponseParts_htmlunitHtmlPage_withHeaders() {
        try {
            final int statusCode = 200;
            final URL url = new URL(SIMPLE_URL);
            final List<NameValuePair> headers = new ArrayList<NameValuePair>();
            headers.add(new NameValuePair(HEADER_NAME, HEADER_VALUE));
            mockery.checking(new org.jmock.Expectations() {
                {
                    allowing(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusCode();
                    will(returnValue(statusCode));
                    one(htmlunitHtmlPage).isHtmlPage();
                    will(returnValue(true));
                    one(htmlunitHtmlPage).getTitleText();
                    will(returnValue(TITLE));
                    one(htmlunitHtmlPage).getUrl();
                    will(returnValue(url));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(headers));
                    one(htmlunitWebResponse).getResponseHeaderValue(HEADER_NAME);
                    will(returnValue(HEADER_VALUE));
                    one(htmlunitWebResponse).getStatusMessage();
                    will(returnValue(STATUS_MESSAGE));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(FULL_RESPONSE));
                }
            });

            utils.printResponseParts(htmlunitHtmlPage, testName.getMethodName());

            assertResponseContentStartAndClassAreLogged(htmlunitHtmlPage.getClass().getName());
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_STATUS_CODE + statusCode);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_TITLE + TITLE);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_URL + SIMPLE_URL);
            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_RESPONSE_HEADER) + ".+" + HEADER_NAME + ".+" + HEADER_VALUE);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_MESSAGE + STATUS_MESSAGE);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_FULL + FULL_RESPONSE);

            assertStringNotInTrace(outputMgr, "Error printing response");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request: com.gargoylesoftware.htmlunit.html.HtmlPage
     * - Includes additional log message argument
     * Expects:
     * - Start of request content should be logged
     * - Status code, title, URL, headers, status message, and full response content should be logged
     * - Blank lines should be removed from the full response content
     */
    @Test
    public void test_printResponseParts_htmlunitHtmlPage_withAdditionalMessage_fullResponseHasBlankLines() {
        try {
            final int statusCode = 401;
            final URL url = new URL(SIMPLE_URL);
            String line1 = " line1";
            String line2 = "line2\t";
            String line3 = "line3";
            final String fullResponse = line1 + "\n \r\n" + line2 + "\r   \n\r \t\r" + line3 + "\n\r ";
            mockery.checking(new org.jmock.Expectations() {
                {
                    allowing(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                    one(htmlunitWebResponse).getStatusCode();
                    will(returnValue(statusCode));
                    one(htmlunitHtmlPage).isHtmlPage();
                    will(returnValue(true));
                    one(htmlunitHtmlPage).getTitleText();
                    will(returnValue(TITLE));
                    one(htmlunitHtmlPage).getUrl();
                    will(returnValue(url));
                    one(htmlunitWebResponse).getResponseHeaders();
                    will(returnValue(null));
                    one(htmlunitWebResponse).getStatusMessage();
                    will(returnValue(STATUS_MESSAGE));
                    one(htmlunitWebResponse).getContentAsString();
                    will(returnValue(fullResponse));
                }
            });
            final String extraMsg = "This is an extra log message";

            utils.printResponseParts(htmlunitHtmlPage, testName.getMethodName(), extraMsg);

            assertResponseContentStartAndClassAreLogged(htmlunitHtmlPage.getClass().getName());
            assertStringInTrace(outputMgr, extraMsg);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_STATUS_CODE + statusCode);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_TITLE + TITLE);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_URL + SIMPLE_URL);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_MESSAGE + STATUS_MESSAGE);
            assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_FULL + line1 + "\r\n" + line2 + "\r\n" + line3);

            assertStringNotInTrace(outputMgr, "Error printing response");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** stripBlankLines **************************************/

    /**
     * Tests:
     * - Text: null
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_stripBlankLines_nullText() {
        try {
            String text = null;
            String result = utils.stripBlankLines(text);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Empty string
     * Expects:
     * - Result should match the input
     */
    @Test
    public void test_stripBlankLines_emptyText() {
        try {
            String text = "";

            String result = utils.stripBlankLines(text);

            assertEquals("Result did not match the expected value.", text, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Simple string
     * Expects:
     * - Result should match the input
     */
    @Test
    public void test_stripBlankLines_simpleText() {
        try {
            String text = "simple";

            String result = utils.stripBlankLines(text);

            assertEquals("Result did not match the expected value.", text, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Simple string with spaces
     * Expects:
     * - Result should match the input
     */
    @Test
    public void test_stripBlankLines_simpleTextWithSpaces() {
        try {
            String text = "The quick brown fox jumps over the lazy dog.";

            String result = utils.stripBlankLines(text);

            assertEquals("Result did not match the expected value.", text, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Contains a newline character
     * - clearBlankLinesFromResponseFull: false
     * Expects:
     * - Result should match the input (new lines are not modified)
     */
    @Test
    public void test_stripBlankLines_oneNewLine_doNotClearBlankLines() {
        try {
            String text = "New\nLine";

            utils.setClearBlankLinesFromResponseFull(false);
            String result = utils.stripBlankLines(text);

            assertEquals("Result did not match the expected value.", text, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Contains a newline character
     * Expects:
     * - Result should have newlines replaced with a carriage return + newline
     */
    @Test
    public void test_stripBlankLines_oneNewLine() {
        try {
            String text = "New\nLine";

            String result = utils.stripBlankLines(text);

            String expectedResult = "New" + "\r\n" + "Line";
            assertEquals("Result did not match the expected value.", expectedResult, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Contains a carriage return character
     * Expects:
     * - Result should have carriage returns replaced with a carriage return + newline
     */
    @Test
    public void test_stripBlankLines_oneCarriageReturn() {
        try {
            String text = "New\rLine";

            String result = utils.stripBlankLines(text);

            String expectedResult = "New" + "\r\n" + "Line";
            assertEquals("Result did not match the expected value.", expectedResult, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Contains a newline and carriage return character
     * Expects:
     * - Result should have newline and carriage return replaced with a carriage return + newline
     */
    @Test
    public void test_stripBlankLines_oneNewLineAndCarriageReturn() {
        try {
            String text = "New\n\rLine";

            String result = utils.stripBlankLines(text);

            String expectedResult = "New" + "\r\n" + "Line";
            assertEquals("Result did not match the expected value.", expectedResult, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Contains only multiple newlines and carriage returns
     * Expects:
     * - Result should be an empty string
     */
    @Test
    public void test_stripBlankLines_onlyNewLineAndCarriageReturns() {
        try {
            String text = "\n\r\r\n\n";

            String result = utils.stripBlankLines(text);

            String expectedResult = "";
            assertEquals("Result did not match the expected value.", expectedResult, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Contains multiple newlines and carriage returns and whitespace
     * Expects:
     * - Result should be an empty string
     */
    @Test
    public void test_stripBlankLines_newLineAndCarriageReturnsWithWhitespace() {
        try {
            String text = " \n  \r\t\r\n\n ";

            String result = utils.stripBlankLines(text);

            String expectedResult = "";
            assertEquals("Result did not match the expected value.", expectedResult, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Contains an empty line in between two simple lines
     * Expects:
     * - Result should have the empty line removed
     */
    @Test
    public void test_stripBlankLines_emptyLineWithinSimpleLines() {
        try {
            String line1 = "Line with trailing space ";
            String line2 = " Line with preceding space";
            String text = line1 + "\n" + "\t \t" + "\r" + line2;

            String result = utils.stripBlankLines(text);

            String expectedResult = line1 + "\r\n" + line2;
            assertEquals("Result did not match the expected value.", expectedResult, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Contains multiple empty lines around multiple non-empty lines
     * Expects:
     * - Result should have the empty lines removed
     */
    @Test
    public void test_stripBlankLines_multipleEmptyLines() {
        try {
            String line1 = "Line with HTML break<br/> and trailing space ";
            String line2 = " Line with preceding space";
            String line3 = "\tLine with \t tabs\t";
            String text = "      \n" + line1 + "\n\r   \r\t\r\t \n \r" + line2 + "\r\n" + line3;

            String result = utils.stripBlankLines(text);

            String expectedResult = line1 + "\r\n" + line2 + "\r\n" + line3;
            assertEquals("Result did not match the expected value.", expectedResult, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** printExpectations **************************************/

    /**
     * Tests:
     * - Expectations: null
     * - Actions: Not provided
     * Expects:
     * - Exception should be thrown because expectations should not be null or empty for a test
     */
    @Test
    public void test_printExpectations_nullExpectations_nullActions() {
        try {
            Expectations expectations = null;
            try {
                utils.printExpectations(expectations);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "NO expectations");
            }
            assertStringNotInTrace(outputMgr, LOG_STRING_ACTIONS);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectations: Empty
     * - Actions: Not provided
     * Expects:
     * - Exception should be thrown because expectations should not be null or empty for a test
     */
    @Test
    public void test_printExpectations_emptyExpectations_nullActions() {
        try {
            Expectations expectations = new Expectations();
            runPrintExpectationsTest_nullOrEmptyExpectations(expectations);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectations: Empty
     * - Actions: Empty
     * Expects:
     * - Exception should be thrown because expectations should not be null or empty for a test
     */
    @Test
    public void test_printExpectations_emptyExpectations_emptyActions() {
        try {
            Expectations expectations = new Expectations();
            String[] actions = new String[0];
            runPrintExpectationsTest_nullOrEmptyExpectations(expectations, actions);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One expectation: Response status
     * - Actions: One action that matches the action specified for the provided expectation
     * Expects:
     * - Should print that we expect a 200 response for the action, but nothing else
     */
    @Test
    public void test_printExpectations_singleResponseStatusExpectation_oneMatchingAction() {
        try {
            Expectations expectations = new Expectations();
            String[] actions = new String[] { ACTION1 };
            Expectation expectation = new ResponseStatusExpectation(ACTION1, HttpServletResponse.SC_OK);
            expectations.addExpectation(expectation);

            utils.printExpectations(expectations, actions);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTIONS) + ".+" + Arrays.toString(actions));
            assertLoggedSuccessfulResponseStatusExpectationInfo(ACTION1);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One expectation: Response status
     * - Actions: One action that does NOT match the action specified for the provided expectation
     * Expects:
     * - Should print a note that the expectation will never be processed because its action is not in the provided action lists
     */
    @Test
    public void test_printExpectations_singleResponseStatusExpectation_oneNonMatchingAction() {
        try {
            Expectations expectations = new Expectations();
            String[] actions = new String[] { ACTION1 };
            Expectation expectation = new ResponseStatusExpectation(ACTION2, HttpServletResponse.SC_OK);
            expectations.addExpectation(expectation);

            utils.printExpectations(expectations, actions);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTIONS) + ".+" + Arrays.toString(actions));
            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_EXPECTATION_WILL_NEVER_BE_PROCESSED) + ".+" + ACTION2);
            assertLoggedSuccessfulResponseStatusExpectationInfo(ACTION2);
            assertRegexNotInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTION) + ACTION1);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One expectation: Checks for anything other than response status
     * - Actions: One action that matches the action specified for the provided expectation
     * Expects:
     * - Should print information about the expectation (what it'll check for, where, etc.)
     */
    @Test
    public void test_printExpectations_singleExpectation_oneMatchingAction() {
        try {
            Expectations expectations = new Expectations();
            String[] actions = new String[] { ACTION1 };
            Expectation expectation = Expectation.createResponseExpectation(ACTION1, "look for me", "We failed to find the expected value.");
            expectations.addExpectation(expectation);

            utils.printExpectations(expectations, actions);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTIONS) + ".+" + Arrays.toString(actions));
            assertLoggedExpectationInfo(expectation);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One expectation: Checks for anything other than response status
     * - Actions: One action that does NOT match the action specified for the provided expectation
     * Expects:
     * - Should print a note that the expectation will never be processed because its action is not in the provided action lists
     * - Information about the expectation should still be printed
     */
    @Test
    public void test_printExpectations_singleExpectation_oneNonMatchingAction() {
        try {
            Expectations expectations = new Expectations();
            String[] actions = new String[] { ACTION1 };
            Expectation expectation = new ResponseStatusExpectation(ACTION2, "check type", "search for this value", "Failed to find value!");
            expectations.addExpectation(expectation);

            utils.printExpectations(expectations, actions);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTIONS) + ".+" + Arrays.toString(actions));
            assertRegexNotInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTIONS) + ".+" + ACTION2);
            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_EXPECTATION_WILL_NEVER_BE_PROCESSED) + ".+" + ACTION2);
            assertLoggedExpectationInfo(expectation);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One expectation: Checks for anything other than response status
     * - Actions: Multiple actions, including one that matches the action specified for the expectation
     * Expects:
     * - Should print information about the expectation (what it'll check for, where, etc.)
     */
    @Test
    public void test_printExpectations_singleExpectation_multipleActions() {
        try {
            Expectations expectations = new Expectations();
            String[] actions = new String[] { ACTION1, ACTION2, ACTION3 };
            Expectation expectation = Expectation.createResponseExpectation(ACTION2, "look for me", "We failed to find the expected value.");
            expectations.addExpectation(expectation);

            utils.printExpectations(expectations, actions);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTIONS) + ".+" + Arrays.toString(actions));
            assertLoggedExpectationInfo(expectation);
            assertRegexNotInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTION) + ACTION1);
            assertRegexNotInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTION) + ACTION3);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple expectations
     * - Actions: One action that matches the action specified for at least one of the provided expectations
     * Expects:
     * - Expectation information should be logged for all expectations
     * - Expectation set for a different action should be flagged with the "expectation will never be processed" header in the
     * output
     */
    @Test
    public void test_printExpectations_multipleExpectations_oneAction() {
        try {
            Expectations expectations = new Expectations();
            String[] actions = new String[] { ACTION1 };
            Expectation expectation1 = new ResponseFullExpectation(ACTION1, "check type", "search for this value", "Failed to find value!");
            expectations.addExpectation(expectation1);
            Expectation expectation2 = new ResponseStatusExpectation(ACTION1, HttpServletResponse.SC_UNAUTHORIZED);
            expectations.addExpectation(expectation2);
            // Add an expectation for some other action/step
            Expectation expectationDifferentAction = Expectation.createResponseExpectation(ACTION2, "look for me", "We failed to find the expected value.");
            expectations.addExpectation(expectationDifferentAction);

            utils.printExpectations(expectations, actions);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTIONS) + ".+" + Arrays.toString(actions));
            assertLoggedExpectationInfo(expectation1);
            assertLoggedExpectationInfo(expectation2);
            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_EXPECTATION_WILL_NEVER_BE_PROCESSED) + ".+" + ACTION2);
            assertLoggedExpectationInfo(expectationDifferentAction);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple expectations
     * - Actions: One action that does NOT match the action specified for the provided expectation
     * Expects:
     * - Expectation information should be logged for all expectations
     * - Expectations set for a different action should be flagged with the "expectation will never be processed" header in the
     * output
     */
    @Test
    public void test_printExpectations_multipleExpectations_oneNonMatchingAction() {
        try {
            Expectations expectations = new Expectations();
            String[] actions = new String[] { ACTION1 };
            Expectation expectation1 = Expectation.createResponseExpectation(ACTION2, "look for me", "We failed to find the expected value.");
            expectations.addExpectation(expectation1);
            Expectation expectation2 = new ResponseStatusExpectation(ACTION2, HttpServletResponse.SC_UNAUTHORIZED);
            expectations.addExpectation(expectation2);

            utils.printExpectations(expectations, actions);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTIONS) + ".+" + Arrays.toString(actions));
            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_EXPECTATION_WILL_NEVER_BE_PROCESSED) + ".+" + ACTION2);
            assertLoggedExpectationInfo(expectation1);
            assertLoggedExpectationInfo(expectation2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple expectations
     * - Actions: Multiple, including ones that match some of the expectations
     * Expects:
     * - Expectation information should be logged for all expectations
     * - Expectations set for a different action should be flagged with the "expectation will never be processed" header in the
     * output
     */
    @Test
    public void test_printExpectations_multipleExpectations_multipleActions() {
        try {
            Expectations expectations = new Expectations();
            String[] actions = new String[] { ACTION1, ACTION2, ACTION3 };
            Expectation expectation1 = new ResponseTitleExpectation(ACTION1, "check type", "search for this value", "Failed to find value!");
            expectations.addExpectation(expectation1);
            Expectation expectation2 = new ResponseStatusExpectation(ACTION2, HttpServletResponse.SC_UNAUTHORIZED);
            expectations.addExpectation(expectation2);
            Expectation expectation3 = Expectation.createResponseExpectation(ACTION2, "look for me", "We failed to find the expected value.");
            expectations.addExpectation(expectation3);
            Expectation expectation4 = Expectation.createResponseExpectation(ACTION4, "Shouldn't look for this", "Failure message we'll never see");
            expectations.addExpectation(expectation4);

            utils.printExpectations(expectations, actions);

            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTIONS) + ".+" + Arrays.toString(actions));
            assertLoggedExpectationInfo(expectation1);
            assertLoggedExpectationInfo(expectation2);
            assertLoggedExpectationInfo(expectation3);
            assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_EXPECTATION_WILL_NEVER_BE_PROCESSED) + ".+" + ACTION4);
            assertLoggedExpectationInfo(expectation4);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** wrapInDelimiter **************************************/

    /**
     * Tests:
     * - Text: null
     * - Delimiter: null
     * Expects:
     * - Text should be wrapped in delimiter value
     */
    @Test
    public void test_wrapInDelimiter_nullText_nullDelimiter() {
        try {
            String text = null;
            String delimiter = null;

            String result = utils.wrapInDelimiter(text, delimiter);

            assertEquals("Result did not match the expected value.", delimiter + " " + text + " " + delimiter, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: null
     * - Delimiter: Empty
     * Expects:
     * - Text should be wrapped in delimiter value
     */
    @Test
    public void test_wrapInDelimiter_nullText_emptyDelimiter() {
        try {
            String text = null;
            String delimiter = "";

            String result = utils.wrapInDelimiter(text, delimiter);

            assertEquals("Result did not match the expected value.", delimiter + " " + text + " " + delimiter, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: null
     * - Delimiter: Non-empty
     * Expects:
     * - Text should be wrapped in delimiter value
     */
    @Test
    public void test_wrapInDelimiter_nullText_nonEmptyDelimiter() {
        try {
            String text = null;
            String delimiter = "***";

            String result = utils.wrapInDelimiter(text, delimiter);

            assertEquals("Result did not match the expected value.", delimiter + " " + text + " " + delimiter, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Empty
     * - Delimiter: Non-empty
     * Expects:
     * - Text should be wrapped in delimiter value
     */
    @Test
    public void test_wrapInDelimiter_emptyText_nonEmptyDelimiter() {
        try {
            String text = "";
            String delimiter = "Some delimiter";

            String result = utils.wrapInDelimiter(text, delimiter);

            assertEquals("Result did not match the expected value.", delimiter + " " + text + " " + delimiter, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Text: Non-empty
     * - Delimiter: Non-empty
     * Expects:
     * - Text should be wrapped in delimiter value
     */
    @Test
    public void test_wrapInDelimiter_nonEmptyText_nonEmptyDelimiter() {
        try {
            String text = "***";
            String delimiter = "Some delimiter";

            String result = utils.wrapInDelimiter(text, delimiter);

            assertEquals("Result did not match the expected value.", delimiter + " " + text + " " + delimiter, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** printToLogAndSystemOut **************************************/

    /**
     * Tests:
     * - Method: null
     * - Print string: null
     * Expects:
     * - Print string, alone, should appear in trace and standard out (null method name should effectively be ignored and not
     * displayed)
     */
    @Test
    public void test_printToLogAndSystemOut_nullMethod_nullPrintString() {
        try {
            String method = null;
            String printString = null;

            utils.printToLogAndSystemOut(method, printString);

            assertStringInTraceAndStandardOut(printString);
            assertStringNotInTrace(outputMgr, method + " " + printString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Method: null
     * - Print string: Non-empty
     * Expects:
     * - Print string, alone, should appear in trace and standard out (null method name should effectively be ignored and not
     * displayed)
     */
    @Test
    public void test_printToLogAndSystemOut_nullMethod_nonEmptyPrintString() {
        try {
            String method = null;
            String printString = "Some print string for test " + testName.getMethodName();

            utils.printToLogAndSystemOut(method, printString);

            assertStringInTraceAndStandardOut(printString);
            assertStringNotInTrace(outputMgr, method + " " + printString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Method: Empty
     * - Print string: Non-empty
     * Expects:
     * - Print string, alone, should appear in standard out
     * - Method name and print string should appear in trace
     */
    @Test
    public void test_printToLogAndSystemOut_emptyMethod_nonEmptyPrintString() {
        try {
            String method = "";
            String printString = "Some print string";

            utils.printToLogAndSystemOut(method, printString);

            assertStringInStandardOut(outputMgr, printString);
            assertStringInTrace(outputMgr, method + " " + printString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Method: Non-empty
     * - Print string: null
     * Expects:
     * - Print string, alone, should appear in standard out
     * - Method name and print string should appear in trace
     */
    @Test
    public void test_printToLogAndSystemOut_nonEmptyMethod_nullPrintString() {
        try {
            String method = testName.getMethodName();
            String printString = null;

            utils.printToLogAndSystemOut(method, printString);

            assertStringInStandardOut(outputMgr, printString);
            assertStringInTrace(outputMgr, method + " " + printString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Method: Non-empty
     * - Print string: Empty
     * Expects:
     * - Print string, alone, should appear in standard out
     * - Method name and print string should appear in trace
     */
    @Test
    public void test_printToLogAndSystemOut_nonEmptyMethod_emptyPrintString() {
        try {
            String method = testName.getMethodName();
            String printString = "";

            utils.printToLogAndSystemOut(method, printString);

            assertStringInStandardOut(outputMgr, printString);
            assertStringInTrace(outputMgr, method + " " + printString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Method: Non-empty
     * - Print string: Non-empty
     * Expects:
     * - Print string, alone, should appear in standard out
     * - Method name and print string should appear in trace
     */
    @Test
    public void test_printToLogAndSystemOut_nonEmptyMethod_nonEmptyPrintString() {
        try {
            String method = testName.getMethodName();
            String printString = "Some print string";

            utils.printToLogAndSystemOut(method, printString);

            assertStringInStandardOut(outputMgr, printString);
            assertStringInTrace(outputMgr, method + " " + printString);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    private void runPrintExpectationsTest_nullOrEmptyExpectations(Expectations expectations, String... actions) {
        try {
            utils.printExpectations(expectations, actions);
            fail("Should have thrown an exception but did not.");
        } catch (Exception e) {
            verifyException(e, "NO expectations");
        }
        if (actions != null) {
            assertStringInTrace(outputMgr, LOG_STRING_ACTIONS);
        } else {
            assertStringNotInTrace(outputMgr, LOG_STRING_ACTIONS);
        }
    }

    private void assertStringInTraceAndStandardOut(String checkForString) {
        assertStringInTrace(outputMgr, checkForString);
        assertStringInStandardOut(outputMgr, checkForString);
    }

    void simpleRequestUrlHeaderAndParameterExpectations() throws MalformedURLException {
        mockery.checking(new org.jmock.Expectations() {
            {
                one(webRequest).getUrl();
                will(returnValue(new URL(SIMPLE_URL)));
                one(webRequest).getAdditionalHeaders();
                one(webRequest).getRequestParameters();
                one(webRequest).getRequestBody();
            }
        });
    }

    private void printRequestPartsExpectations(String cookieName, String url, String headerName, String parameterName) {
        assertStringInTrace(outputMgr, LOG_STRING_START_REQUEST_PARTS);

        assertCookieNameLoggedIfPresent(cookieName);
        assertUrlLoggedIfPresent(url);
        assertHeaderNameLoggedIfPresent(headerName);
        assertParameterNameLoggedIfPresent(parameterName);

        if (url == null) {
            assertStringNotInTrace(outputMgr, LOG_STRING_END_REQUEST_PARTS);
        } else {
            assertStringInTrace(outputMgr, LOG_STRING_END_REQUEST_PARTS);
        }
    }

    void assertCookieNameLoggedIfPresent(String cookieName) {
        assertStringWithValueLoggedIfValueIsPresent(LOG_STRING_COOKIE, cookieName);
    }

    void assertHeaderNameLoggedIfPresent(String headerName) {
        assertStringWithValueLoggedIfValueIsPresent(LOG_STRING_REQUEST_HEADER, headerName);
    }

    void assertUrlLoggedIfPresent(String url) {
        assertStringWithValueLoggedIfValueIsPresent(LOG_STRING_REQUEST_URL, url);
    }

    void assertParameterNameLoggedIfPresent(String parameterName) {
        assertStringWithValueLoggedIfValueIsPresent(LOG_STRING_REQUEST_PARAMETER, parameterName);
    }

    void assertStringWithValueLoggedIfValueIsPresent(String stringPrefix, String expectedValue) {
        if (expectedValue == null) {
            assertStringNotInTrace(outputMgr, stringPrefix);
        } else {
            assertStringInTrace(outputMgr, stringPrefix + expectedValue);
        }
    }

    private void assertStartResponseContentLogged() {
        assertStringInTrace(outputMgr, CommonFatLoggingUtils.PRINT_DELIMITER_REQUEST_PARTS + " " + "Start Response Content");
    }

    private void assertResponseContentStartAndClassAreLogged(String className) {
        assertStartResponseContentLogged();
        assertStringInTrace(outputMgr, LOG_STRING_RESPONSE_CLASS + className);
    }

    private void assertErrorPrintingResponseContent() {
        assertStringInTrace(outputMgr, "Error printing response");
    }

    private void assertLoggedSuccessfulResponseStatusExpectationInfo(String action) {
        assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTION) + action + ".+200 response");
        assertStringNotInTrace(outputMgr, LOG_STRING_VALIDATE_AGAINST);
    }

    private void assertLoggedExpectationInfo(Expectation expectation) {
        assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_ACTION) + expectation.getAction());
        assertRegexInTrace(outputMgr, Pattern.quote(LOG_STRING_VALIDATE_AGAINST) + expectation.getSearchLocation());
        assertRegexInTrace(outputMgr, Pattern.quote(expectation.getCheckType()));
        assertRegexInTrace(outputMgr, Pattern.quote(expectation.getValidationValue()));
        assertRegexInTrace(outputMgr, Pattern.quote(expectation.getFailureMsg()));
    }

}
