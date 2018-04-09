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
        String secret = "secret";
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
    public void testStripSecretFromUrl9() {
        String input = "http://localhost:8010/path?client_secret=password";
        String expected = "http://localhost:8010/path?client_secret=*****";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrlA() {
        String input = "http://localhost:8010/path?something=what&client_secret=password";
        String expected = "http://localhost:8010/path?something=what&client_secret=*****";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrlB() {
        String input = "http://localhost:8010/path?something=what&client_secret=password&a=b";
        String expected = "http://localhost:8010/path?something=what&client_secret=*****&a=b";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrlC() {
        String input = "client_secret=x";
        String expected = "client_secret=*****";
        String secret = "client_secret";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrlD() {
        String input = "abc=x";
        String expected = "abc=*****";
        String secret = "abc";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrlE() {
        String input = "abc=x&abcd=password";
        String expected = "abc=*****&abcd=password";
        String secret = "abc";
        String value = WebUtils.stripSecretFromUrl(input, secret);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretFromUrlF() {
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
    public void testStripSecretsFromUrl1() {
        String input = "&client_secret=x";
        String expected = "&client_secret=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl2() {
        String input = "&secret1=x";
        String expected = "&secret1=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl3() {
        String input = "&secret1=x&client_secret=x";
        String expected = "&secret1=*****&client_secret=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl4() {
        String input = "secret1=x&client_secret=x&client_secret=x&secret1=x&abc=secret1&client_secret=secret1";
        String expected = "secret1=*****&client_secret=*****&client_secret=*****&secret1=*****&abc=secret1&client_secret=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl5() {
        String input = "http://localhost:8010/path?client_secret=password";
        String expected = "http://localhost:8010/path?client_secret=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl6() {
        String input = "http://localhost:8010/path?secret1=password";
        String expected = "http://localhost:8010/path?secret1=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl7() {
        String input = "http://localhost:8010/path?secret1=password&client_secret=123";
        String expected = "http://localhost:8010/path?secret1=*****&client_secret=*****";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testStripSecretsFromUrl8() {
        String input = "&client_secret=&client_secret=password&secret1=x&client_secret=123&abc=secret1&client_secret=abc&secret1=xxxx&client_secret=xx&something-!@#%^*()askldfjghhljkshhh&abc=xxx";
        String expected = "&client_secret=&client_secret=*****&secret1=*****&client_secret=*****&abc=secret1&client_secret=*****&secret1=*****&client_secret=*****&something-!@#%^*()askldfjghhljkshhh&abc=xxx";
        String [] secrets = new String [] {"client_secret","secret1"};
        String value = WebUtils.stripSecretsFromUrl(input, secrets);
        assertEquals(expected, value);
    }

    @Test
    public void testGetRequestStringForTrace1() {
        String value = WebUtils.getRequestStringForTrace(null, (String)null);
        assertEquals("[]",value);
    }

    //We're going to do our best to not hem in testGetRequestStringForTrace to a pre-determined
    //format. Just cursory checks are being done.  The heavy lifting is done by the stripSecretFromUrl
    //tests because that can be hemmed in.
    @Test
    public void testGetRequestStringForTrace2() {
        final String secret = "client_secret";
        final String pass = "password";
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url+"?"+secret+"="+pass);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString
                        ();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertFalse(output.contains("password"));
        assertTrue(output.contains(url));
    }

    @Test
    public void testGetRequestStringForTrace3() {
        final String secret = "client_secret";
        final String pass = "password";
        final String queryValue = secret+"="+pass;
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString
                        ();
                    will(returnValue(queryValue));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertFalse(output.contains(pass));
        assertTrue(output.contains(url));
    }

    @Test
    public void testGetRequestStringForTrace4() {
        final String secret = "client_secret";
        final String queryValue = "aParameter";
        final String url = "https://localhost:9080/target";
        final StringBuffer urlValue = new StringBuffer(url);

        mock.checking(new Expectations() {
                {
                    allowing(request).getRequestURL();
                    will(returnValue(urlValue));
                    allowing(request).getQueryString
                        ();
                    will(returnValue(queryValue));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertTrue(output.contains(queryValue));
        assertTrue(output.contains(url));
    }

    @Test
    public void testGetRequestStringForTrace5() {
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
                    allowing(request).getQueryString
                        ();
                    will(returnValue(queryValue));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertFalse(output.contains(pass));
        assertTrue(output.contains(url));
        assertTrue(output.contains(q1));
        assertTrue(output.contains(q3));
        assertTrue(output.contains(secret));
    }

    @Test
    public void testGetRequestStringForTrace6() {
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
                    allowing(request).getQueryString
                        ();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,secret);
        assertNotNull(output);
        assertFalse(output.contains(pass));
        assertTrue(output.contains(url));
        assertTrue(output.contains(q1));
        assertTrue(output.contains(q3));
        assertTrue(output.contains(secret));
    }

    @Test
    public void testGetRequestStringForTrace7() {
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
                    allowing(request).getQueryString
                        ();
                    will(returnValue(null));
                }
            });
        String output = WebUtils.getRequestStringForTrace(request,new String[]{secret1,secret2});
        assertNotNull(output);
        assertFalse(output.contains(pass1));
        assertFalse(output.contains(pass2));
        assertTrue(output.contains(url));
        assertTrue(output.contains(q1));
        assertTrue(output.contains(q3));
        assertTrue(output.contains(secret1));
        assertTrue(output.contains(secret2));
    }
}
