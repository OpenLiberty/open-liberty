/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.test.CommonTestClass;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import test.common.SharedOutputManager;

public class JavaScriptUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    public static final String CWWKS6106E_JAVASCRIPT_REDIRECT_URL_NOT_VALID = "CWWKS6106E";

    static final String REGEX_JAVASCRIPT_START = "<script\\s+type=\"text/javascript\"\\s+language=\"javascript\">";
    static final String REGEX_JAVASCRIPT_END = "</script>";
    static final String DOCUMENT_COOKIE_START = "document.cookie=\"";
    static final String DOCUMENT_COOKIE_END = "\";";
    static final String WINDOW_LOCATION_REPLACE_START = "window.location.replace(\"";
    static final String WINDOW_LOCATION_REPLACE_END = "\");";
    static final String VALID_URL = "https://localhost:8010/myApp";

    final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    final WebAppSecurityConfig webAppConfig = mockery.mock(WebAppSecurityConfig.class);

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    public interface MockInterface {
        String getHtmlCookieString();
    }

    JavaScriptUtils utils = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new JavaScriptUtils() {
            @Override
            WebAppSecurityConfig getWebAppSecurityConfig() {
                return webAppConfig;
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
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** getJavaScriptHtmlTagStart **************************************/

    /**
     * Tests:
     * - Format for opening HTML tag for JavaSript block
     * Expects:
     * - Tag must match the appropriate format
     */
    @Test
    public void test_getJavaScriptHtmlTagStart() throws Exception {
        try {
            String result = utils.getJavaScriptHtmlTagStart();

            Pattern expectedPattern = Pattern.compile(REGEX_JAVASCRIPT_START, Pattern.CASE_INSENSITIVE);
            verifyPatternMatches(result, expectedPattern, "Opening JavaScript HTML tag did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getJavaScriptHtmlTagEnd **************************************/

    /**
     * Tests:
     * - Format for closing HTML tag for JavaSript block
     * Expects:
     * - Tag must match the appropriate format
     */
    @Test
    public void test_getJavaScriptHtmlTagEnd() throws Exception {
        try {
            String result = utils.getJavaScriptHtmlTagEnd();

            Pattern expectedPattern = Pattern.compile(REGEX_JAVASCRIPT_END, Pattern.CASE_INSENSITIVE);
            verifyPatternMatches(result, expectedPattern, "Closing JavaScript HTML tag did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getJavaScriptHtmlCookieString **************************************/

    /**
     * Tests:
     * - Cookie name: Null
     * - Cookie value: Null
     * Expects:
     * - Result should be a document.cookie string set to an empty string
     */
    @Test
    public void test_getJavaScriptHtmlCookieString_nullName_nullValue() throws Exception {
        try {
            utils = getUtilsWithHtmlCookieStringMethodMocked();

            final String mockedCookieString = "";
            setMockedCookieStringExpectation(mockedCookieString);

            String name = null;
            String value = null;

            String result = utils.getJavaScriptHtmlCookieString(name, value);

            verifyCaseInsensitiveQuotedPatternMatches(result, DOCUMENT_COOKIE_START + mockedCookieString + DOCUMENT_COOKIE_END, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Empty string
     * - Cookie value: Empty string
     * Expects:
     * - Result should be a document.cookie string set to an empty string
     */
    @Test
    public void test_getJavaScriptHtmlCookieString_emptyName_emptyValue() throws Exception {
        try {
            utils = getUtilsWithHtmlCookieStringMethodMocked();

            final String mockedCookieString = "";
            setMockedCookieStringExpectation(mockedCookieString);

            String name = "";
            String value = "";

            String result = utils.getJavaScriptHtmlCookieString(name, value);

            verifyCaseInsensitiveQuotedPatternMatches(result, DOCUMENT_COOKIE_START + mockedCookieString + DOCUMENT_COOKIE_END, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Non-empty string
     * - Cookie value: Non-empty string
     * Expects:
     * - Result should be a document.cookie string set to the provided name + "=" + value
     */
    @Test
    public void test_getJavaScriptHtmlCookieString_nonEmptyName_nonEmptyValue() throws Exception {
        try {
            utils = getUtilsWithHtmlCookieStringMethodMocked();

            String name = "some value";
            String value = "some value";

            final String mockedCookieString = name + "=" + value;
            setMockedCookieStringExpectation(mockedCookieString);

            String result = utils.getJavaScriptHtmlCookieString(name, value);

            verifyCaseInsensitiveQuotedPatternMatches(result, DOCUMENT_COOKIE_START + mockedCookieString + DOCUMENT_COOKIE_END, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie property map: Null
     * Expects:
     * - Result should be a document.cookie string set to the provided name + "=" + value
     */
    @Test
    public void test_getJavaScriptHtmlCookieString_nullCookieSettings() throws Exception {
        try {
            utils = getUtilsWithHtmlCookieStringMethodMocked();

            String name = "some value";
            String value = "some value";
            Map<String, String> cookieProps = null;

            final String mockedCookieString = name + "=" + value;
            setMockedCookieStringExpectation(mockedCookieString);

            String result = utils.getJavaScriptHtmlCookieString(name, value, cookieProps);

            verifyCaseInsensitiveQuotedPatternMatches(result, DOCUMENT_COOKIE_START + mockedCookieString + DOCUMENT_COOKIE_END, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie property map: Empty
     * Expects:
     * - Result should be a document.cookie string set to the provided name + "=" + value
     */
    @Test
    public void test_getJavaScriptHtmlCookieString_emptyCookieSettings() throws Exception {
        try {
            utils = getUtilsWithHtmlCookieStringMethodMocked();

            String name = "some value";
            String value = "some value";
            Map<String, String> cookieProps = new HashMap<String, String>();

            final String mockedCookieString = name + "=" + value;
            setMockedCookieStringExpectation(mockedCookieString);

            String result = utils.getJavaScriptHtmlCookieString(name, value, cookieProps);

            verifyCaseInsensitiveQuotedPatternMatches(result, DOCUMENT_COOKIE_START + mockedCookieString + DOCUMENT_COOKIE_END, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie property map: Non-empty
     * Expects:
     * - Result should be a document.cookie string set to the provided name + "=" + value + all extra properties
     */
    @Test
    public void test_getJavaScriptHtmlCookieString_withCookieSettings() throws Exception {
        try {
            utils = getUtilsWithHtmlCookieStringMethodMocked();

            String name = "some value";
            String value = "some value";
            Map<String, String> cookieProps = new HashMap<String, String>();
            cookieProps.put("key1", "value1");
            cookieProps.put("key2", "value2");

            final String mockedCookieString = name + "=" + value + "; key1=value1; key2=value2";
            setMockedCookieStringExpectation(mockedCookieString);

            String result = utils.getJavaScriptHtmlCookieString(name, value, cookieProps);

            verifyCaseInsensitiveQuotedPatternMatches(result, DOCUMENT_COOKIE_START + mockedCookieString + DOCUMENT_COOKIE_END, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getHtmlCookieString **************************************/

    /**
     * Tests:
     * - Cookie name: Null
     * - Cookie value: Null
     * Expects:
     * - Result should be an empty string
     */
    @Test
    public void test_getHtmlCookieString_nullName_nullValue() throws Exception {
        try {
            String name = null;
            String value = null;

            String result = utils.getHtmlCookieString(name, value);

            assertEquals("Result should have been an empty string since the cookie name is null.", "", result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Null
     * - Cookie value: Empty string
     * Expects:
     * - Result should be an empty string
     */
    @Test
    public void test_getHtmlCookieString_nullName_emptyValue() throws Exception {
        try {
            String name = null;
            String value = "";

            String result = utils.getHtmlCookieString(name, value);

            assertEquals("Result should have been an empty string since the cookie name is null.", "", result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Null
     * - Cookie value: Non-empty string
     * Expects:
     * - Result should be an empty string
     */
    @Test
    public void test_getHtmlCookieString_nullName() throws Exception {
        try {
            String name = null;
            String value = "some value";

            String result = utils.getHtmlCookieString(name, value);

            assertEquals("Result should have been an empty string since the cookie name is null.", "", result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Empty string
     * - Cookie value: Null
     * Expects:
     * - Result should be an empty string
     */
    @Test
    public void test_getHtmlCookieString_emptyName_nullValue() throws Exception {
        try {
            String name = "";
            String value = null;

            String result = utils.getHtmlCookieString(name, value);

            assertEquals("Result should have been an empty string since the cookie name is an empty string.", "", result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Empty string
     * - Cookie value: Empty string
     * Expects:
     * - Result should be an empty string
     */
    @Test
    public void test_getHtmlCookieString_emptyName_emptyValue() throws Exception {
        try {
            String name = "";
            String value = "";

            String result = utils.getHtmlCookieString(name, value);

            assertEquals("Result should have been an empty string since the cookie name is an empty string.", "", result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Empty string
     * - Cookie value: Non-empty string
     * Expects:
     * - Result should be an empty string
     */
    @Test
    public void test_getHtmlCookieString_emptyName() throws Exception {
        try {
            String name = "";
            String value = "some value";

            String result = utils.getHtmlCookieString(name, value);

            assertEquals("Result should have been an empty string since the cookie name is an empty string.", "", result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Non-empty string
     * - Cookie value: Null
     * Expects:
     * - Cookie name should be only string present
     */
    @Test
    public void test_getHtmlCookieString_nullValue() throws Exception {
        try {
            String name = "some value";
            String value = null;

            String result = utils.getHtmlCookieString(name, value);

            String expectedCookieString = name + ";";
            verifyCaseInsensitiveQuotedPatternMatches(result, expectedCookieString, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Non-empty string
     * - Cookie value: Empty string
     * Expects:
     * - Cookie name + "=" should be only string present
     */
    @Test
    public void test_getHtmlCookieString_emptyValue() throws Exception {
        try {
            String name = "some value";
            String value = "";

            String result = utils.getHtmlCookieString(name, value);

            String expectedCookieString = name + "=" + ";";
            verifyCaseInsensitiveQuotedPatternMatches(result, expectedCookieString, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Non-empty string
     * - Cookie value: Non-empty string
     * Expects:
     * - Cookie name + "=" + value should be only string present
     */
    @Test
    public void test_getHtmlCookieString_nonEmptyValue() throws Exception {
        try {
            String name = "some value";
            String value = "some value";

            String result = utils.getHtmlCookieString(name, value);

            String expectedCookieString = name + "=" + value + ";";
            verifyCaseInsensitiveQuotedPatternMatches(result, expectedCookieString, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie name: Contains HTML special characters
     * - Cookie value: Contains HTML special characters
     * Expects:
     * - Cookie name + "=" + value should be only string present, with appropriate HTML encoding
     */
    @Test
    public void test_getHtmlCookieString_htmlSpecialChars() throws Exception {
        try {
            String name = ">\"name,";
            String value = "&value<";

            String result = utils.getHtmlCookieString(name, value);

            String htmlEncodedName = "&gt;&quot;name,";
            String htmlEncodedValue = "&amp;value&lt;";
            String expectedCookieString = htmlEncodedName + "=" + htmlEncodedValue + ";";
            verifyCaseInsensitiveQuotedPatternMatches(result, expectedCookieString, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie property map: Null
     * Expects:
     * - Cookie name + "=" + value should be only string present, with appropriate HTML encoding
     */
    @Test
    public void test_getHtmlCookieString_nullPropertyMap() throws Exception {
        try {
            String name = "some name";
            String value = "a value";
            Map<String, String> cookieProps = null;

            String result = utils.getHtmlCookieString(name, value, cookieProps);

            String expectedCookieString = name + "=" + value + ";";
            verifyCaseInsensitiveQuotedPatternMatches(result, expectedCookieString, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie property map: Empty
     * Expects:
     * - Cookie name + "=" + value should be only string present, with appropriate HTML encoding
     */
    @Test
    public void test_getHtmlCookieString_emptyPropertyMap() throws Exception {
        try {
            String name = "some name";
            String value = "a value";
            Map<String, String> cookieProps = new HashMap<String, String>();

            String result = utils.getHtmlCookieString(name, value, cookieProps);

            String expectedCookieString = name + "=" + value + ";";
            verifyCaseInsensitiveQuotedPatternMatches(result, expectedCookieString, "Cookie string did not match expected pattern.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Cookie property map: Contains multiple values
     * Expects:
     * - Cookie name + "=" + value + all provided properties should be present, with appropriate HTML encoding
     */
    @Test
    public void test_getHtmlCookieString_nonEmptyPropertyMap() throws Exception {
        try {
            String name = "some name";
            String value = "a value";
            Map<String, String> cookieProps = new HashMap<String, String>();
            cookieProps.put(">\"name,", "&value<");
            cookieProps.put(null, "null_key_value");
            cookieProps.put("", "empty_key_value");
            cookieProps.put(";", "semi_colon_value");
            cookieProps.put("path", "https://localhost:43/");
            cookieProps.put("NullValue", null);
            cookieProps.put("EmptyValue", "");

            String result = utils.getHtmlCookieString(name, value, cookieProps);

            String expectedCookieNameAndValue = name + "=" + value + ";";
            verifyPattern(result, Pattern.quote(expectedCookieNameAndValue), "Expected cookie name and value did not appear in the result.");
            verifyCookiePropertyStrings(result, cookieProps);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getJavaScriptForRedirect **************************************/

    /**
     * Tests:
     * - Request URL cookie name: Null
     * - Redirect URL: Null
     * Expects:
     * - Exception should be thrown with CWWKS6106E message saying the redirect URL is invalid
     */
    @Test
    public void test_getJavaScriptForRedirect_nullRedirectUrl() throws Exception {
        try {
            String requestUrlCookieName = null;
            String redirectUrl = null;
            try {
                String result = utils.getJavaScriptForRedirect(requestUrlCookieName, redirectUrl);
                fail("Should have thrown an exception but did not. Result was: [" + result + "].");
            } catch (Exception e) {
                assertInvalidRedirectUrlException(e, redirectUrl);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request URL cookie name: Null
     * - Redirect URL: Empty
     * Expects:
     * - Exception should be thrown with CWWKS6106E message saying the redirect URL is invalid
     */
    @Test
    public void test_getJavaScriptForRedirect_emptyRedirectUrl() throws Exception {
        try {
            String requestUrlCookieName = null;
            String redirectUrl = "";
            try {
                String result = utils.getJavaScriptForRedirect(requestUrlCookieName, redirectUrl);
                fail("Should have thrown an exception but did not. Result was: [" + result + "].");
            } catch (Exception e) {
                assertInvalidRedirectUrlException(e, redirectUrl);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request URL cookie name: Null
     * - Redirect URL: Invalid format
     * Expects:
     * - Exception should be thrown with CWWKS6106E message saying the redirect URL is invalid
     */
    @Test
    public void test_getJavaScriptForRedirect_invalidRedirectUrl() throws Exception {
        try {
            String requestUrlCookieName = null;
            String redirectUrl = "some invalid URL";
            try {
                String result = utils.getJavaScriptForRedirect(requestUrlCookieName, redirectUrl);
                fail("Should have thrown an exception but did not. Result was: [" + result + "].");
            } catch (Exception e) {
                assertInvalidRedirectUrlException(e, redirectUrl);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request URL cookie name: Null
     * - Redirect URL: Valid URL
     * Expects:
     * - Valid JavaScript HTML block should be returned
     * - Block should not set any cookies since the cookie name is null
     * - Block should contain browser redirect statement
     */
    @Test
    public void test_getJavaScriptForRedirect_nullCookieName() throws Exception {
        try {
            String requestUrlCookieName = null;
            String redirectUrl = VALID_URL;

            String result = utils.getJavaScriptForRedirect(requestUrlCookieName, redirectUrl);

            verifyValidJavaScriptForRedirectBlock(result, requestUrlCookieName, redirectUrl);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request URL cookie name: Empty string
     * - Redirect URL: Valid URL
     * Expects:
     * - Valid JavaScript HTML block should be returned
     * - Block should not set any cookies since the cookie name is empty
     * - Block should contain browser redirect statement
     */
    @Test
    public void test_getJavaScriptForRedirect_emptyCookieName() throws Exception {
        try {
            String requestUrlCookieName = "";
            String redirectUrl = VALID_URL;

            String result = utils.getJavaScriptForRedirect(requestUrlCookieName, redirectUrl);

            verifyValidJavaScriptForRedirectBlock(result, requestUrlCookieName, redirectUrl);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request URL cookie name: Non-empty string
     * - Redirect URL: Valid URL
     * Expects:
     * - Valid JavaScript HTML block should be returned
     * - Block should set the cookie with the specified name and appropriate value
     * - Block should contain browser redirect statement
     */
    @Test
    public void test_getJavaScriptForRedirect_nonEmptyCookieName() throws Exception {
        try {
            String requestUrlCookieName = "some cookie name";
            String redirectUrl = VALID_URL;

            mockery.checking(new Expectations() {
                {
                    one(webAppConfig).getSSORequiresSSL();
                    will(returnValue(false));
                }
            });

            String result = utils.getJavaScriptForRedirect(requestUrlCookieName, redirectUrl);

            verifyValidJavaScriptForRedirectBlock(result, requestUrlCookieName, redirectUrl);

            Map<String, String> cookieProps = new HashMap<String, String>();
            cookieProps.put("path", "/");
            verifyCookiePropertyStrings(result, cookieProps);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Request URL cookie name: Non-empty string
     * - Redirect URL: Valid URL
     * Expects:
     * - Valid JavaScript HTML block should be returned
     * - Block should set the cookie with the specified name and appropriate value
     * - Block should contain browser redirect statement
     */
    @Test
    public void test_getJavaScriptForRedirect_nonEmptyCookieName_secureCookie() throws Exception {
        try {
            String requestUrlCookieName = "some cookie name";
            String redirectUrl = VALID_URL;

            mockery.checking(new Expectations() {
                {
                    one(webAppConfig).getSSORequiresSSL();
                    will(returnValue(true));
                }
            });

            String result = utils.getJavaScriptForRedirect(requestUrlCookieName, redirectUrl);

            verifyValidJavaScriptForRedirectBlock(result, requestUrlCookieName, redirectUrl);

            Map<String, String> cookieProps = new HashMap<String, String>();
            cookieProps.put("path", "/");
            cookieProps.put("secure", null);
            verifyCookiePropertyStrings(result, cookieProps);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    private JavaScriptUtils getUtilsWithHtmlCookieStringMethodMocked() {
        return new JavaScriptUtils() {
            @Override
            public String getHtmlCookieString(String name, String value, Map<String, String> cookieProps) {
                return mockInterface.getHtmlCookieString();
            }
        };
    }

    private void setMockedCookieStringExpectation(final String mockedCookieString) {
        mockery.checking(new Expectations() {
            {
                one(mockInterface).getHtmlCookieString();
                will(returnValue(mockedCookieString));
            }
        });
    }

    private void verifyCaseInsensitiveQuotedPatternMatches(String result, String patternString, String failureMsg) {
        Pattern expectedPattern = getCaseInsensitiveQuotedPattern(patternString);
        verifyPatternMatches(result, expectedPattern, failureMsg);
    }

    private Pattern getCaseInsensitiveQuotedPattern(String value) {
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE);
    }

    private void verifyPatternMatches(String input, Pattern pattern, String failureMsg) {
        assertNotNull(failureMsg + " Value should not have been null but was. Expected pattern [" + pattern.toString() + "].", input);
        assertTrue(failureMsg + " Expected [" + pattern.toString() + "]. Value was [" + input + "].", pattern.matcher(input).matches());
    }

    private void verifyPatternExists(String input, Pattern pattern, String failureMsg) {
        assertNotNull(failureMsg + " Value should not have been null but was. Expected pattern [" + pattern.toString() + "].", input);
        assertTrue(failureMsg + " Expected [" + pattern.toString() + "]. Value was [" + input + "].", pattern.matcher(input).find());
    }

    private void verifyCookiePropertyStrings(String cookieString, Map<String, String> cookieProperties) {
        for (Entry<String, String> property : cookieProperties.entrySet()) {
            String key = property.getKey();
            String value = property.getValue();
            if (key == null) {
                assertCookieMissingPropertyWithNullKey(cookieString, value);
            } else if (key.isEmpty()) {
                assertCookieMissingPropertyWithEmptyKey(cookieString, value);
            } else if (value == null) {
                assertCookiePropertyWithNullValueIsPresent(cookieString, key);
            } else {
                assertCookiePropertyIsPresent(cookieString, key, value);
            }
        }
    }

    private void assertCookieMissingPropertyWithNullKey(String cookieString, String propertyValue) {
        assertFalse("Result should not have contained cookie property that had null key, but did. Cookie string was [" + cookieString + "].", cookieString.contains(propertyValue));
    }

    private void assertCookieMissingPropertyWithEmptyKey(String cookieString, String propertyValue) {
        assertFalse("Result should not have contained cookie property that had empty key, but did. Cookie string was [" + cookieString + "].", cookieString.contains(propertyValue));
    }

    private void assertCookiePropertyWithNullValueIsPresent(String cookieString, String propertyKey) {
        String expectedPattern = Pattern.quote(propertyKey + ";");
        verifyPattern(cookieString, expectedPattern, "Cookie property [" + propertyKey + "] with null value did not appear as expected.");
    }

    private void assertCookiePropertyIsPresent(String cookieString, String propertyKey, String propertyValue) {
        String expectedPattern = Pattern.quote(WebUtils.htmlEncode(propertyKey) + "=" + WebUtils.htmlEncode(propertyValue) + ";");
        verifyPattern(cookieString, expectedPattern, "Cookie property [" + propertyKey + "] did not appear as expected.");
    }

    private void assertInvalidRedirectUrlException(Exception e, String redirectUrl) {
        verifyExceptionWithInserts(e, CWWKS6106E_JAVASCRIPT_REDIRECT_URL_NOT_VALID, ((redirectUrl == null) ? "null" : Pattern.quote(redirectUrl)));
    }

    private void verifyValidJavaScriptForRedirectBlock(String result, String cookieName, String redirectUrl) {
        assertIsValidJavaScriptHtmlBlock(result);
        assertContainsAppropriateCookieString(result, cookieName);
        assertContainsWindowReplaceString(result, redirectUrl);
    }

    private void assertIsValidJavaScriptHtmlBlock(String result) {
        assertNotNull("Result was null but should not have been.", result);
        Pattern expectedOverallPattern = Pattern.compile(REGEX_JAVASCRIPT_START + ".+" + REGEX_JAVASCRIPT_END, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        verifyPatternMatches(result, expectedOverallPattern, "Result should have been enclosed in JavaScript HTML tags.");
    }

    private void assertContainsAppropriateCookieString(String result, String cookieName) {
        if (cookieName == null || cookieName.isEmpty()) {
            assertFalse("With a null or empty cookie name, no cookie should have been set.", result.contains(DOCUMENT_COOKIE_START));
        } else {
            verifyCookieSettingStringExists(result);
            verifyRequestUrlCookie(result, cookieName);
        }
    }

    private void assertContainsWindowReplaceString(String result, String redirectUrl) {
        String windowReplaceRegex = Pattern.quote(WINDOW_LOCATION_REPLACE_START + redirectUrl + WINDOW_LOCATION_REPLACE_END);
        verifyPattern(result, windowReplaceRegex, "Result did not contain the expected pattern to redirect the browser.");
    }

    private void verifyCookieSettingStringExists(String text) {
        String overallCookieRegex = Pattern.quote(DOCUMENT_COOKIE_START) + ".+" + Pattern.quote(DOCUMENT_COOKIE_END) + "[^.]";
        Pattern overallCookiePattern = Pattern.compile(overallCookieRegex, Pattern.CASE_INSENSITIVE);
        verifyPatternExists(text, overallCookiePattern, "Did not find an appropriate string in the result for setting the cookie.");
    }

    private void verifyRequestUrlCookie(String result, String cookieName) {
        Pattern cookieNameAndValuePattern = getExpectedCookieNameAndValuePattern(cookieName);
        verifyPatternExists(result, cookieNameAndValuePattern, "Cookie name/and or value did not match the expected value.");
    }

    private Pattern getExpectedCookieNameAndValuePattern(String cookieName) {
        String expectedCookieVal = Pattern.quote("\"+encodeURI(") + "[a-zA-Z0-9]+" + Pattern.quote(")+\"");
        return Pattern.compile(Pattern.quote(DOCUMENT_COOKIE_START + cookieName + "=") + expectedCookieVal + Pattern.quote(";"));
    }

}