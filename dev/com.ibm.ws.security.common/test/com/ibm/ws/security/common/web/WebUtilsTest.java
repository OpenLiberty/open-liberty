/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.rules.TestRule;

public class WebUtilsTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    protected final HttpServletRequest request = mock.mock(HttpServletRequest.class, "request");

    /******************************************* urlEncode *******************************************/

    @Test
    public void urlEncode_nullArg() {
        try {
            String result = WebUtils.urlEncode(null);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void urlEncode_emptyArg() {
        try {
            String result = WebUtils.urlEncode("");
            assertEquals("Result did not match expected value.", "", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void urlEncode_simpleArg() {
        try {
            String input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            String result = WebUtils.urlEncode(input);
            assertEquals("Result did not match expected value.", input, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void urlEncode_specialChars() {
        try {
            String input = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? \t\n\r";
            String expectedResult = "%60%7E%21%40%23%24%25%5E%26" + "*" + "%28%29" + "-_" + "%3D%2B%5B%7B%5D%7D%5C%7C%3B%3A%27%22%2C%3C" + "." + "%3E%2F%3F" + "+" + "%09%0A%0D";
            String result = WebUtils.urlEncode(input);
            assertEquals("Result did not match expected value.", expectedResult, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* encodeQueryString *******************************************/

    @Test
    public void encodeQueryString_nullArg() {
        try {
            String result = WebUtils.encodeQueryString(null);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void encodeQueryString_emptyArg() {
        try {
            String result = WebUtils.encodeQueryString("");
            assertEquals("Result did not match expected value.", "", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void encodeQueryString_simpleArg() {
        try {
            String input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            String result = WebUtils.encodeQueryString(input);
            assertEquals("Result did not match expected value.", input, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void encodeQueryString_specialChars() {
        try {
            // '&' and '=' are splitting characters, so each special character segment here is broken down at each of those characters
            String chars1 = "`~!@#$%^";
            String chars2 = "*()-_";
            String chars3 = "+[{]}\\|;:'\",<.>/? \t\n\r";
            String input = chars1 + "&" + chars2 + "=" + chars3;
            String chars1Encoded = "%60%7E%21%40%23%24%25%5E";
            String chars2Encoded = "*" + "%28%29" + "-_";
            String chars3Encoded = "%2B%5B%7B%5D%7D%5C%7C%3B%3A%27%22%2C%3C" + "." + "%3E%2F%3F" + "+" + "%09%0A%0D";
            String expectedResult = chars1Encoded + "&" + chars2Encoded + "=" + chars3Encoded;

            String result = WebUtils.encodeQueryString(input);
            assertEquals("Result did not match expected value.", expectedResult, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void encodeQueryString_onlyAmpersands() {
        try {
            String input = "&&&&&&";
            // Effectively empty parameters without any value will be removed
            String expectedResult = "";
            String result = WebUtils.encodeQueryString(input);
            assertEquals("Result did not match expected value.", expectedResult, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void encodeQueryString_onlyAmpersandsAndEquals() {
        try {
            String input = "=&&=&&=&&=";
            // Params with empty name/value pairs will still appear in the result
            String expectedResult = "=&=&=&=";
            String result = WebUtils.encodeQueryString(input);
            assertEquals("Result did not match expected value.", expectedResult, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* encodeCookie *******************************************/

    @Test
    public void encodeCookie_nullArg() {
        try {
            String result = WebUtils.encodeCookie(null);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void encodeCookie_emptyArg() {
        try {
            String result = WebUtils.encodeCookie("");
            assertEquals("Result did not match expected value.", "", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void encodeCookie_simpleArg() {
        try {
            String input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            String result = WebUtils.encodeCookie(input);
            assertEquals("Result did not match expected value.", input, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void encodeCookie_specialChars() {
        try {
            String input = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? \t\n\r";
            String expectedResult = "`~!@#$" + "%25" + "^&*()-_=+[{]}\\|" + "%3B" + ":'\"" + "%2C" + "<.>/? \t\n\r";

            String result = WebUtils.encodeCookie(input);
            assertEquals("Result did not match expected value.", expectedResult, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* decodeCookie *******************************************/

    @Test
    public void decodeCookie_nullArg() {
        try {
            String result = WebUtils.decodeCookie(null);
            assertNull("Result should have been null but was [" + result + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void decodeCookie_emptyArg() {
        try {
            String result = WebUtils.decodeCookie("");
            assertEquals("Result did not match expected value.", "", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void decodeCookie_simpleArg() {
        try {
            String input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            String result = WebUtils.decodeCookie(input);
            assertEquals("Result did not match expected value.", input, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void decodeCookie_specialChars() {
        try {
            String input = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? \t\n\r";

            String result = WebUtils.decodeCookie(input);
            assertEquals("Result did not match expected value.", input, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void decodeCookie_encodedSpecialChars() {
        try {
            String input = "`~!@#$" + "%25" + "^&*()-_=+[{]}\\|" + "%3B" + ":'\"" + "%2C" + "<.>/? \t\n\r";
            String expectedResult = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? \t\n\r";

            String result = WebUtils.decodeCookie(input);
            assertEquals("Result did not match expected value.", expectedResult, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void decodeCookie_fullEncodedSpecialChars() {
        try {
            // Should only decode '%', ';', and ',' characters
            String input = "%60%7E%21%40%23%24" + "%25" + "%5E%26*%28%29-_%3D%2B%5B%7B%5D%7D%5C%7C" + "%3B" + "%3A%27%22" + "%2C" + "%3C.%3E%2F%3F %09%0A%0D";
            String expectedResult = "%60%7E%21%40%23%24" + "%" + "%5E%26*%28%29-_%3D%2B%5B%7B%5D%7D%5C%7C" + ";" + "%3A%27%22" + "," + "%3C.%3E%2F%3F %09%0A%0D";

            String result = WebUtils.decodeCookie(input);
            assertEquals("Result did not match expected value.", expectedResult, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testStripSecretFromUrlNoChange() {
        assertNull(WebUtils.stripSecretFromUrl(null, null));
        assertNull(WebUtils.stripSecretFromUrl(null, "something"));

        String input = "";
        String expected = input;
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);

        input = "something";
        expected = input;
        value = WebUtils.stripSecretFromUrl(input, null);
        assertEquals(expected, value);

        input = "something";
        expected = input;
        value = WebUtils.stripSecretFromUrl(input, "");
        assertEquals(expected, value);

        input = "abc";
        expected = input;
        value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);

        input = "something_longer_than_client_secret";
        expected = input;
        value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);

        input = "&client_secret=";
        expected = input;
        value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);

        input = "&CLIENT_SECRET=x";
        expected = input;
        value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);

        input = "http://localhost";
        expected = input;
        value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);

        input = "http://localhost?";
        expected = input;
        value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);

        input = "http://localhost?client_secret";
        expected = input;
        value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);

        input = "http://localhost?client_secret=";
        expected = input;
        value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl1() {
        String input = "&client_secret=x";
        String expected = "&client_secret=*****";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl2() {
        String input = "abc&client_secret=x&client_secret=password";
        String expected = "abc&client_secret=*****&client_secret=*****";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl3() {
        String input = "abc&client_secret=x&&client_secret=password";
        String expected = "abc&client_secret=*****&&client_secret=*****";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl4() {
        String input = "abc&client_secret=password&client_secret=123&client_secret=abc&client_secret=";
        String expected = "abc&client_secret=*****&client_secret=*****&client_secret=*****&client_secret=";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl5() {
        String input = "&client_secret=&client_secret=password&client_secret=123&client_secret=abc&client_secret=";
        String expected = "&client_secret=&client_secret=*****&client_secret=*****&client_secret=*****&client_secret=";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl6() {
        String input = "client_secret=&client_secret=password&client_secret=123&client_secret=abc&client_secret=";
        String expected = "client_secret=&client_secret=*****&client_secret=*****&client_secret=*****&client_secret=";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl7() {
        String input = "client_secret=password&client_secret=password&client_secret=123&client_secret=abc&client_secret=";
        String expected = "client_secret=*****&client_secret=*****&client_secret=*****&client_secret=*****&client_secret=";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl8() {
        String input = "client_secret=password&client_secret=password&client_secret=123&client_secret=abc&client_secret=password";
        String expected = "client_secret=*****&client_secret=*****&client_secret=*****&client_secret=*****&client_secret=*****";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl_secretFirst() {
        String input = "http://localhost:8010/path?client_secret=password";
        String expected = "http://localhost:8010/path?client_secret=*****";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl_secretLast() {
        String input = "http://localhost:8010/path?something=what&client_secret=password";
        String expected = "http://localhost:8010/path?something=what&client_secret=*****";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl_secretMiddle() {
        String input = "http://localhost:8010/path?something=what&client_secret=password&a=b";
        String expected = "http://localhost:8010/path?something=what&client_secret=*****&a=b";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl_nonNormativeFirst() {
        String input = "client_secret=x";
        String expected = "client_secret=*****";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl_nonNormativeAlternative() {
        String input = "abc=x";
        String expected = "abc=*****";
        String secret = "abc";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl_secretSubsetOfNonSecret() {
        String input = "abc=x&abcd=password";
        String expected = "abc=*****&abcd=password";
        String secret = "abc";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrl_secretStringIsNonSecretValue() {
        String input = "abc=x&cde=abc";
        String expected = "abc=*****&cde=abc";
        String secret = "abc";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrlNoChange() {
        assertNull(WebUtils.stripSecretsFromUrl(null, null));
        assertNull(WebUtils.stripSecretsFromUrl(null, new String[]{"something"}));

        String input = "";
        String expected = input;
        String [] secrets = new String[] {"client_secret","secret1"};

        String value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "something";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, null);
        assertEquals(expected, value);

        input = "something";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, new String[] {});
        assertEquals(expected, value);

        input = "abc";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "something_longer_than_client_secret";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "&client_secret=";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "&secret1=";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "&CLIENT_SECRET=x";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "&SECRET1=x";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "http://localhost";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "http://localhost?";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "http://localhost?client_secret";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "http://localhost?client_secret=";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "http://localhost?secret1";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);

        input = "http://localhost?secret1=";
        expected = input;
        value = WebUtils.stripSecretsFromUrl(input, secrets);;
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl_withPassword() {
        String input = "&client_secret=x&another=1&password=a";
        String expected = "&client_secret=*****&another=1&password=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl_twoSecretsOneHitFirst() {
        String input = "&client_secret=x";
        String expected = "&client_secret=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl_twoSecretsOneHitSecond() {
        String input = "&secret1=x";
        String expected = "&secret1=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl_twoSecretsTwoHits() {
        String input = "&secret1=x&client_secret=x";
        String expected = "&secret1=*****&client_secret=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl_twoSecretsMultipleHits() {
        String input = "secret1=x&client_secret=x&client_secret=x&secret1=x&abc=secret1&client_secret=secret1";
        String expected = "secret1=*****&client_secret=*****&client_secret=*****&secret1=*****&abc=secret1&client_secret=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl_twoSecretsFirstHitsAfterQuery() {
        String input = "http://localhost:8010/path?client_secret=password";
        String expected = "http://localhost:8010/path?client_secret=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl_twoSecretsSecondHitsAfterQuery() {
        String input = "http://localhost:8010/path?secret1=password";
        String expected = "http://localhost:8010/path?secret1=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl_twoSecretsBothHitAfterQuery() {
        String input = "http://localhost:8010/path?secret1=password&client_secret=123";
        String expected = "http://localhost:8010/path?secret1=*****&client_secret=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl_specialChars() {
        String input = "&client_secret=&client_secret=password&secret1=x&client_secret=123&abc=secret1&client_secret=abc&secret1=xxxx&client_secret=xx&something-!@#%^*()askldfjghhljkshhh&abc=xxx";
        String expected = "&client_secret=&client_secret=*****&secret1=*****&client_secret=*****&abc=secret1&client_secret=*****&secret1=*****&client_secret=*****&something-!@#%^*()askldfjghhljkshhh&abc=xxx";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testGetRequestStringForTrace_NullRequest() {
        String value = WebUtils.getRequestStringForTrace(null, (String)null);
        assertEquals("[]",value);
    }

    //We're going to do our best to not hem in testGetRequestStringForTrace to a pre-determined
    //format. Just cursory checks are being done.  The heavy lifting is done by the stripSecretFromUrl
    //tests because that can be hemmed in.
    @Test
    public void testGetRequestStringForTrace_simple() {
        final String secret = "client_secret";
        final String pass = "password";
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url+"?"+secret+"="+pass);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString();
                    will(returnValue(null));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info [password]", output.contains("password"));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url)); 
    }

    @Test
    public void testGetRequestStringForTrace_secretInQuery() {
        final String secret = "client_secret";
        final String pass = "password";
        final String queryValue = secret+"="+pass;
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString();
                    will(returnValue(queryValue));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info ["+pass+"]", output.contains(pass));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
    }

    @Test
    public void testGetRequestStringForTrace_noSecretInQuery() {
        final String secret = "client_secret";
        final String queryValue = "aParameter";
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString();
                    will(returnValue(queryValue));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertTrue("Output string ["+output+"] does not contain the query value", output.contains(queryValue));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
    }

    @Test
    public void testGetRequestStringForTrace_oneSecretInQuery() {
        final String secret = "client_secret";
        final String pass = "password";
        final String q1 = "aParameter=a";
        final String q2 = "&"+secret+"="+pass;
        final String q3 = "&bParameter=b";
        final String queryValue = q1+q2+q3;
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString();
                    will(returnValue(queryValue));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info ["+pass+"]", output.contains(pass));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
        assertTrue("Output string ["+output+"] does not contain the query value ["+q1+"]", output.contains(q1));
        assertTrue("Output string ["+output+"] does not contain the query value ["+q3+"]", output.contains(q3));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret+"]", output.contains(secret));
    }

    @Test
    public void testGetRequestStringForTrace_simpleSecretOneMiddle() {
        final String secret = "client_secret";
        final String pass = "password";
        final String q1 = "aParameter=a";
        final String q2 = "&"+secret+"="+pass;
        final String q3 = "&bParameter=b";
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url+"?"+q1+q2+q3);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString();
                    will(returnValue(null));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info ["+pass+"]", output.contains(pass));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
        assertTrue("Output string ["+output+"] does not contain the query value ["+q1+"]", output.contains(q1));
        assertTrue("Output string ["+output+"] does not contain the query value ["+q3+"]", output.contains(q3));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret+"]", output.contains(secret));
    }

    @Test
    public void testGetRequestStringForTrace_simpleTwoSecrets() {
        final String secret1 = "client_secret";
        final String secret2 = "secret1";
        final String pass1 = "password";
        final String pass2 = "garbage";
        final String q1 = "aParameter=a";
        final String q2 = "&"+secret1+"="+pass1;
        final String q3 = "&bParameter=b";
        final String q4 = "&"+secret2+"="+pass2;
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url+"?"+q1+q2+q3+q4);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString();
                    will(returnValue(null));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,new String[]{secret1,secret2});
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info ["+pass1+"]", output.contains(pass1));
        assertFalse("Output string ["+output+"] contains secret info ["+pass2+"]", output.contains(pass2));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
        assertTrue("Output string ["+output+"] does not contain the query value ["+q1+"]", output.contains(q1));
        assertTrue("Output string ["+output+"] does not contain the query value ["+q3+"]", output.contains(q3));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
    }

    //pick up the query string
    @Test
    public void testGetRequestStringForTrace_twoSecretsInQuery() {
        final String secret1 = "client_secret";
        final String secret2 = "secret1";
        final String pass1 = "password";
        final String pass2 = "garbage";
        final String q1 = "aParameter=a";
        final String q2 = "&"+secret1+"="+pass1;
        final String q3 = "&bParameter=b";
        final String q4 = "&"+secret2+"="+pass2;
        final StringBuffer url = new StringBuffer("https://localhost:9080/target");
        final String queryString = q1+q2+q3+q4;

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(url));
                    allowing(request).getQueryString();
                    will(returnValue(queryString));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,new String[]{secret1,secret2});
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info ["+pass1+"]", output.contains(pass1));
        assertFalse("Output string ["+output+"] contains secret info ["+pass2+"]", output.contains(pass2));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
        assertTrue("Output string ["+output+"] does not contain the query value ["+q1+"]", output.contains(q1));
        assertTrue("Output string ["+output+"] does not contain the query value ["+q3+"]", output.contains(q3));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
    }

    //pick up the parametermap since there's no query string
    @Test
    public void testGetRequestStringForTrace_twoSecretsInParameters() {
        final String secret1 = "client_secret";
        final String secret2 = "secret1";
        final String pass1 = "password";
        final String pass2 = "garbage";
        final StringBuffer url = new StringBuffer("https://localhost:9080/target");

        final Map<String, String[]> pMap = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pass1});
            put(secret2, new String[]{pass2});
        }};

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(url));
                    allowing(request).getQueryString();
                    will(returnValue(null));
                    allowing(request).getParameterMap();
                    will(returnValue(pMap));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,new String[]{secret1,secret2});
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info ["+pass1+"]", output.contains(pass1));
        assertFalse("Output string ["+output+"] contains secret info ["+pass2+"]", output.contains(pass2));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
    }

    //the parameter map shouldn't be processed if a querystring is present (because they'll be the
    //same and its redundant, but for the purposes of this test, I had to make them different)
    @Test
    public void testGetRequestStringForTrace_doNotProcessParameterMap() {
        final String secret1 = "client_secret";
        final String secret2 = "secret1";
        final String pass1 = "password";
        final String pass2 = "garbage";
        final String q1 = "aParameter=a";
        final String q2 = "&"+secret1+"="+pass1;
        final String q3 = "&bParameter=b";
        final String q4 = "&"+secret2+"="+pass2;
        final StringBuffer url = new StringBuffer("https://localhost:9080/target");
        final String queryString = q1+q2+q3+q4;

        final Map<String, String[]> pMap = new HashMap<String, String[]>() {{
            put("param_client_secret1", new String[]{pass1});
            put("param_client_secret2", new String[]{pass2});
        }};

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(url));
                    allowing(request).getQueryString();
                    will(returnValue(queryString));
                    allowing(request).getParameterMap();
                    will(returnValue(pMap));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,new String[]{secret1,secret2});
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info ["+pass1+"]", output.contains(pass1));
        assertFalse("Output string ["+output+"] contains secret info ["+pass2+"]", output.contains(pass2));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
        assertFalse("Output string ["+output+"] contains secret info [param_client_secret1]", output.contains("param_client_secret1"));
        assertFalse("Output string ["+output+"] contains secret info [param_client_secret2]", output.contains("param_client_secret2"));
    }

    //null input
    @Test
    public void testStripSecretsFromParameters_nullInput() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String [] secrets = new String [] {secret1,secret2};

        String output = WebUtils.stripSecretsFromParameters(null, secrets);
        assertNull(output);
    }

    //empty input
    @Test
    public void testStripSecretsFromParameters_emptyInput() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String [] secrets = new String [] {secret1,secret2};

        Map<String, String[]> input = new HashMap<String, String[]>();

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertNull(output);
    }

    //null secret list
    @Test
    public void testStripSecretsFromParameters_nullSecretList() {
        final String secret1 = "client_secret";

        final String pwdStr = "password";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, null);
        assertTrue("Output string ["+output+"] does not contain the parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the string ["+pwdStr+"] (it wasn't marked secret so it should be there)", output.contains(pwdStr));
    }

    //empty secret list
    @Test
    public void testStripSecretsFromParameters_emptySecretList() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String [] secrets = new String [] {};

        final String pwdStr = "password";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertTrue("Output string ["+output+"] does not contain the parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the string ["+pwdStr+"] (it wasn't marked secret so it should be there)", output.contains(pwdStr));
    }

    //one parameter, matches first secret
    @Test
    public void testStripSecretsFromParameters_oneParameterMatchesFirstSecret() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String [] secrets = new String [] {secret1,secret2};

        final String pwdStr = "password";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
    }

    //one parameter, matches second secret
    @Test
    public void testStripSecretsFromParameters_oneParameterMatchesSecondSecret() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String [] secrets = new String [] {secret1,secret2};

        final String pwdStr = "password";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret2, new String[]{pwdStr});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
    }

    //two parameters, matches both secrets
    @Test
    public void testStripSecretsFromParameters_twoParametersAreBothSecrets() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String [] secrets = new String [] {secret1,secret2};

        final String pwdStr = "password";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
            put(secret2, new String[]{pwdStr});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
    }

    //add a non-secret
    @Test
    public void testStripSecretsFromParameters_threeParametersOneNotSecret() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String notSecret = "notSecretParameter";
        final String [] secrets = new String [] {secret1,secret2};

        final String pwdStr = "password";
        final String notSecretValue = "notSecretValue";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
            put(secret2, new String[]{pwdStr});
            put(notSecret, new String[]{notSecretValue});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret+"]", output.contains(notSecret));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue+"], (its parameter was not marked secret)", output.contains(notSecretValue));
    }

    //add a non-secret parameter with special characters in value
    @Test
    public void testStripSecretsFromParameters_specialCharsInNotSecretValue() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String notSecret = "notSecretParameter";
        final String notSecret2 = "abcParam";
        final String [] secrets = new String [] {secret1,secret2};

        final String pwdStr = "password";
        final String notSecretValue = "notSecretValue";
        final String notSecretValue2 = "!@#$%^&*()";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
            put(secret2, new String[]{pwdStr});
            put(notSecret, new String[]{notSecretValue});
            put(notSecret2, new String[]{notSecretValue2});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret+"]", output.contains(notSecret));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue+"], (its parameter was not marked secret)", output.contains(notSecretValue));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret2+"]", output.contains(notSecret2));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue2+"], (its parameter was not marked secret)", output.contains(notSecretValue2));
    }

    //add a non-secret parameter with special characters in name
    @Test
    public void testStripSecretsFromParameters_specialCharsInNotSecretName() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String notSecret = "notSecretParameter";
        final String notSecret2 = "abcParam";
        final String notSecret3 = "(*&^%$#@";
        final String [] secrets = new String [] {secret1,secret2};

        final String pwdStr = "password";
        final String notSecretValue = "notSecretValue";
        final String notSecretValue2 = "!@#$%^&*()";
        final String notSecretValue3 = "thisisvalue3";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
            put(secret2, new String[]{pwdStr});
            put(notSecret, new String[]{notSecretValue});
            put(notSecret2, new String[]{notSecretValue2});
            put(notSecret3, new String[]{notSecretValue3});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret+"]", output.contains(notSecret));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue+"], (its parameter was not marked secret)", output.contains(notSecretValue));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret2+"]", output.contains(notSecret2));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue2+"], (its parameter was not marked secret)", output.contains(notSecretValue2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret3+"]", output.contains(notSecret3));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue3+"], (its parameter was not marked secret)", output.contains(notSecretValue3));
    }

    //add a non-secret parameter with a name that is a 
    //super-set of one of the secrets
    @Test
    public void testStripSecretsFromParameters_notSecretNameIsSupersetofSecret() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String notSecret = "notSecretParameter";
        final String notSecret2 = "abcParam";
        final String notSecret3 = secret1+"1";
        final String [] secrets = new String [] {secret1,secret2};

        final String pwdStr = "password";
        final String notSecretValue = "notSecretValue";
        final String notSecretValue2 = "!@#$%^&*()";
        final String notSecretValue3 = "thisisvalue3";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
            put(secret2, new String[]{pwdStr});
            put(notSecret, new String[]{notSecretValue});
            put(notSecret2, new String[]{notSecretValue2});
            put(notSecret3, new String[]{notSecretValue3});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret+"]", output.contains(notSecret));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue+"], (its parameter was not marked secret)", output.contains(notSecretValue));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret2+"]", output.contains(notSecret2));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue2+"], (its parameter was not marked secret)", output.contains(notSecretValue2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret3+"]", output.contains(notSecret3));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue3+"], (its parameter was not marked secret)", output.contains(notSecretValue3));
    }

    //add a non-secret parameter with a name that is a different 
    //case than of one of the secrets
    @Test
    public void testStripSecretsFromParameters_secretWrongCase() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String notSecret = "notSecretParameter";
        final String notSecret2 = "abcParam";
        final String notSecret3 = secret1.toUpperCase();
        final String [] secrets = new String [] {secret1,secret2};

        final String pwdStr = "password";
        final String notSecretValue = "notSecretValue";
        final String notSecretValue2 = "!@#$%^&*()";
        final String notSecretValue3 = "thisisvalue3";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
            put(secret2, new String[]{pwdStr});
            put(notSecret, new String[]{notSecretValue});
            put(notSecret2, new String[]{notSecretValue2});
            put(notSecret3, new String[]{notSecretValue3});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret+"]", output.contains(notSecret));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue+"], (its parameter was not marked secret)", output.contains(notSecretValue));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret2+"]", output.contains(notSecret2));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue2+"], (its parameter was not marked secret)", output.contains(notSecretValue2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret3+"]", output.contains(notSecret3));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue3+"], (its parameter was not marked secret)", output.contains(notSecretValue3));
    }

    //non-secret parameter has more than one value
    @Test
    public void testStripSecretsFromParameters_notSecretMultiValue() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String notSecret = "notSecretParameter";
        final String notSecret2 = "abcParam";
        final String notSecret3 = "123Param";
        final String [] secrets = new String [] {secret1,secret2};

        final String pwdStr = "password";
        final String notSecretValue = "notSecretValue";
        final String notSecretValue2 = "!@#$%^&*()";
        final String notSecretValue3a = "value1";
        final String notSecretValue3b = "value2";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
            put(secret2, new String[]{pwdStr});
            put(notSecret, new String[]{notSecretValue});
            put(notSecret2, new String[]{notSecretValue2});
            put(notSecret3, new String[]{notSecretValue3a,notSecretValue3b});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret+"]", output.contains(notSecret));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue+"], (its parameter was not marked secret)", output.contains(notSecretValue));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret2+"]", output.contains(notSecret2));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue2+"], (its parameter was not marked secret)", output.contains(notSecretValue2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret3+"]", output.contains(notSecret3));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue3a+"], (its parameter was not marked secret)", output.contains(notSecretValue3a));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue3b+"], (its parameter was not marked secret)", output.contains(notSecretValue3b));
    }

    //secret parameter has more than one value
    @Test
    public void testStripSecretsFromParameters_secretMultiValue() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String notSecret = "notSecretParameter";
        final String notSecret2 = "abcParam";
        final String secret3 = "secret3";
        final String [] secrets = new String [] {secret1,secret2,secret3};

        final String pwdStr = "password";
        final String notSecretValue = "notSecretValue";
        final String notSecretValue2 = "!@#$%^&*()";
        final String secretValue3a = "value1";
        final String secretValue3b = "value2";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
            put(secret2, new String[]{pwdStr});
            put(notSecret, new String[]{notSecretValue});
            put(notSecret2, new String[]{notSecretValue2});
            put(secret3, new String[]{secretValue3a,secretValue3b});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertFalse("Output string ["+output+"] contains secret info ["+secretValue3a+"]", output.contains(secretValue3a));
        assertFalse("Output string ["+output+"] contains secret info ["+secretValue3b+"]", output.contains(secretValue3b));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret3+"]", output.contains(secret3));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret+"]", output.contains(notSecret));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue+"], (its parameter was not marked secret)", output.contains(notSecretValue));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret2+"]", output.contains(notSecret2));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue2+"], (its parameter was not marked secret)", output.contains(notSecretValue2));
    }


    //no secretes in parameters
    @Test
    public void testStripSecretsFromParameters_noSecrets() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String notSecret = "notSecretParameter";
        final String notSecret2 = "abcParam";
        final String notSecret3 = "123Param";
        final String [] secrets = new String [] {secret1,secret2};

        final String pwdStr = "password";
        final String notSecretValue = "notSecretValue";
        final String notSecretValue2 = "!@#$%^&*()";
        final String notSecretValue3a = "value1";
        final String notSecretValue3b = "value2";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(notSecret, new String[]{notSecretValue});
            put(notSecret2, new String[]{notSecretValue2});
            put(notSecret3, new String[]{notSecretValue3a,notSecretValue3b});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret+"]", output.contains(notSecret));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue+"], (its parameter was not marked secret)", output.contains(notSecretValue));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret2+"]", output.contains(notSecret2));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue2+"], (its parameter was not marked secret)", output.contains(notSecretValue2));
        assertTrue("Output string ["+output+"] does not contain the parameter ["+notSecret3+"]", output.contains(notSecret3));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue3a+"], (its parameter was not marked secret)", output.contains(notSecretValue3a));
        assertTrue("Output string ["+output+"] does not contain the value ["+notSecretValue3b+"], (its parameter was not marked secret)", output.contains(notSecretValue3b));
    }

    //two parameters, matches both secrets, plus password
    @Test
    public void testStripSecretsFromParameters_password() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String [] secrets = new String [] {secret1,secret2};

        final String secret3 = "password";

        final String pwdStr = "something";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
            put(secret2, new String[]{pwdStr});
            put(secret3, new String[]{pwdStr}); //automatic
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret3+"]", output.contains(secret3));
    }

    //two parameters, matches both secrets, plus Password
    @Test
    public void testStripSecretsFromParameters_Password() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String [] secrets = new String [] {secret1,secret2};

        final String secret4 = "Password";

        final String pwdStr = "something";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(secret1, new String[]{pwdStr});
            put(secret2, new String[]{pwdStr});
            put(secret4, new String[]{pwdStr}); //automatic
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret1+"]", output.contains(secret1));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret2+"]", output.contains(secret2));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+secret4+"]", output.contains(secret4));
    }


    //one parameter, matches default secret password
    @Test
    public void testStripSecretsFromParameters_oneParameterMatches_password() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String [] secrets = new String [] {secret1,secret2};

        final String password_secret = "password";
        final String pwdStr = "anything";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(password_secret, new String[]{pwdStr});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+password_secret+"]", output.contains(password_secret));
    }

    //one parameter, matches default secret password
    @Test
    public void testStripSecretsFromParameters_oneParameterMatches_Password() {
        final String secret1 = "client_secret";
        final String secret2 = "secret2";
        final String [] secrets = new String [] {secret1,secret2};

        final String password_secret = "Password";
        final String pwdStr = "anything";
        Map<String, String[]> input = new HashMap<String, String[]>() {{
            put(password_secret, new String[]{pwdStr});
        }};

        String output = WebUtils.stripSecretsFromParameters(input, secrets);
        assertFalse("Output string ["+output+"] contains secret info ["+pwdStr+"]", output.contains(pwdStr));
        assertTrue("Output string ["+output+"] does not contain the secret parameter ["+password_secret+"]", output.contains(password_secret));
    }

    @Test
    public void testGetRequestStringForTrace_secretInQuery_password() {
        final String secret = "client_secret";
        final String pass = "anything";
        final String secret_pass = "password";
        final String queryValue = secret+"="+pass+"&"+secret_pass+"="+pass;
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString();
                    will(returnValue(queryValue));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info ["+pass+"]", output.contains(pass));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
    }

    @Test
    public void testGetRequestStringForTrace_secretInQuery_Password() {
        final String secret = "client_secret";
        final String pass = "anything";
        final String secret_pass = "Password";
        final String queryValue = secret+"="+pass+"&"+secret_pass+"="+pass;
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString();
                    will(returnValue(queryValue));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info ["+pass+"]", output.contains(pass));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
    }

    @Test
    public void testGetRequestStringForTrace_simple_password() {
        final String secret = "password";
        final String pass = "anything";
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url+"?"+secret+"="+pass);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString();
                    will(returnValue(null));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,"client_secret");
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info [anything]", output.contains("anything"));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url)); 
    }

    @Test
    public void testGetRequestStringForTrace_simple_Password() {
        final String secret = "Password";
        final String pass = "anything";
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url+"?"+secret+"="+pass);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString();
                    will(returnValue(null));
                    allowing(request).getParameterMap();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,"client_secret");
        assertNotNull(output);
        assertFalse("Output string ["+output+"] contains secret info [anything]", output.contains("anything"));
        assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url)); 
    }

    @Test
    public void testGetRequestStringForTrace_oneParameterMatches_password() {
      final String secret = "password";
      final String pass = "anything";
      final String url = "https://localhost:9080/target";
      final StringBuffer urlValue = new StringBuffer(url);

      final Map<String, String[]> input = new HashMap<String, String[]>() {{
          put(secret, new String[]{pass});
      }};

      mock.checking(new Expectations() {
              {
                  allowing(request).getRequestURL();
                  will(returnValue(urlValue));
                  allowing(request).getQueryString();
                  will(returnValue(null));
                  allowing(request).getParameterMap();
                  will(returnValue(input));
              }
          });
      String output = WebUtils.getRequestStringForTrace(request,"client_secret");
      assertNotNull(output);
      assertFalse("Output string ["+output+"] contains secret info [anything]", output.contains("anything"));
      assertTrue("Output string ["+output+"] does not contains secret parameter ["+secret+"]", output.contains(secret));
      assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
    }

    @Test
    public void testGetRequestStringForTrace_oneParameterMatches_Password() {
      final String secret = "Password";
      final String pass = "anything";
      final String url = "https://localhost:9080/target";
      final StringBuffer urlValue = new StringBuffer(url);

      final Map<String, String[]> input = new HashMap<String, String[]>() {{
          put(secret, new String[]{pass});
      }};

      mock.checking(new Expectations() {
              {
                  allowing(request).getRequestURL();
                  will(returnValue(urlValue));
                  allowing(request).getQueryString();
                  will(returnValue(null));
                  allowing(request).getParameterMap();
                  will(returnValue(input));
              }
          });
      String output = WebUtils.getRequestStringForTrace(request,"client_secret");
      assertNotNull(output);
      assertFalse("Output string ["+output+"] contains secret info [anything]", output.contains("anything"));
      assertTrue("Output string ["+output+"] does not contains secret parameter ["+secret+"]", output.contains(secret));
      assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
    }


    @Test
    public void testGetRequestStringForTrace_twoParameterMatches_password() {
      final String secret = "password";
      final String pass = "anything";
      final String url = "https://localhost:9080/target";
      final StringBuffer urlValue = new StringBuffer(url);

      final Map<String, String[]> input = new HashMap<String, String[]>() {{
          put(secret, new String[]{pass});
          put("client_secret", new String[]{pass});
      }};

      mock.checking(new Expectations() {
              {
                  allowing(request).getRequestURL();
                  will(returnValue(urlValue));
                  allowing(request).getQueryString();
                  will(returnValue(null));
                  allowing(request).getParameterMap();
                  will(returnValue(input));
              }
          });
      String output = WebUtils.getRequestStringForTrace(request,"client_secret");
      assertNotNull(output);
      assertFalse("Output string ["+output+"] contains secret info [anything]", output.contains("anything"));
      assertTrue("Output string ["+output+"] does not contains secret parameter ["+secret+"]", output.contains(secret));
      assertTrue("Output string ["+output+"] does not contains secret parameter ["+"client_secret"+"]", output.contains("client_secret"));
      assertTrue("Output string ["+output+"] does not contain the request url ["+url+"]", output.contains(url));
    }

}
