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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

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

}
