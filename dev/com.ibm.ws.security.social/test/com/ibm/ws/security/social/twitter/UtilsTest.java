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
package com.ibm.ws.security.social.twitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

public class UtilsTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    final String UTF_8 = "UTF-8";
    final String ALPHA_NUM_REGEX = "[a-zA-Z0-9]";

    final int NONCE_ITERATIONS = 1000;

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void generateNonce() {
        try {
            // Generate N nonces. Validate lengths and ensure no two nonces are the same.
            Set<String> nonceResults = new HashSet<String>();
            for (int i = 0; i < NONCE_ITERATIONS; i++) {
                String result = Utils.generateNonce();
                nonceResults = validateNonce(result, Utils.NONCE_LENGTH, nonceResults);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void generateNonce_lengthZero() {
        try {
            String result = Utils.generateNonce(0);
            assertTrue("Nonce should have been empty (length of 0) but was not: [" + result + "]", result.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void generateNonce_negativeLength() {
        try {
            Random r = new Random();

            // Generate N nonces using a negative argument. Validate lengths and ensure no two nonces are the same.
            Set<String> nonceResults = new HashSet<String>();

            for (int i = 0; i < NONCE_ITERATIONS; i++) {
                // Provide a random negative argument. Generated nonce should default to length of NONCE_LENGTH
                int length = (r.nextInt(20) + 1) * -1;
                String result = Utils.generateNonce(length);

                nonceResults = validateNonce(result, Utils.NONCE_LENGTH, nonceResults);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void generateNonce_lengthN() {
        try {
            Random r = new Random();

            // Generate X different nonce length values, each with a max length of MAX_LENGTH
            int MAX_LENGTH = 100;
            int numDifferentLengths = 15;
            for (int lengthN = 0; lengthN < numDifferentLengths; lengthN++) {
                int length = r.nextInt(MAX_LENGTH) + 1;

                // Generate N nonces. Validate lengths and ensure no two nonces are the same.
                Set<String> nonceResults = new HashSet<String>();

                for (int i = 0; i < NONCE_ITERATIONS; i++) {
                    String result = Utils.generateNonce(length);
                    nonceResults = validateNonce(result, length, nonceResults);
                }
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void percentEncode() {
        try {
            assertEquals("", Utils.percentEncode(null));
            assertEquals("", Utils.percentEncode(""));

            // Make sure '~', ' ', and '*' characters are encoded correctly
            String testString = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? ";
            String urlEncodedString = URLEncoder.encode(testString, UTF_8);
            assertFalse(urlEncodedString.equals(Utils.percentEncode(testString)));

            urlEncodedString = urlEncodedString.replace("+", "%20");
            urlEncodedString = urlEncodedString.replace("*", "%2A");
            urlEncodedString = urlEncodedString.replace("%7E", "~");
            assertEquals(urlEncodedString, Utils.percentEncode(testString));

            testString = "https://api.twitter.com/1/statuses/update.json";
            assertEquals(URLEncoder.encode(testString, UTF_8), Utils.percentEncode(testString));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void percentEncodeSensitive() {
        try {
            assertEquals("", Utils.percentEncodeSensitive(null));
            assertEquals("", Utils.percentEncodeSensitive(""));

            // Make sure '~', ' ', and '*' characters are encoded correctly
            String testString = "`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/? ";
            String urlEncodedString = URLEncoder.encode(testString, UTF_8);
            assertFalse(urlEncodedString.equals(Utils.percentEncodeSensitive(testString)));

            // Ensure values don't show up in trace
            assertFalse("Sensitive string [" + testString + "] should not have appeared in trace but did.", outputMgr.checkForLiteralTrace(testString));
            assertFalse("Sensitive string [" + urlEncodedString + "] should not have appeared in trace but did.", outputMgr.checkForLiteralTrace(urlEncodedString));

            urlEncodedString = urlEncodedString.replace("+", "%20");
            urlEncodedString = urlEncodedString.replace("*", "%2A");
            urlEncodedString = urlEncodedString.replace("%7E", "~");
            assertEquals(urlEncodedString, Utils.percentEncodeSensitive(testString));

            assertFalse("Sensitive string [" + testString + "] should not have appeared in trace but did.", outputMgr.checkForLiteralTrace(testString));
            assertFalse("Sensitive string [" + urlEncodedString + "] should not have appeared in trace but did.", outputMgr.checkForLiteralTrace(urlEncodedString));

            testString = "https://api.twitter.com/1/statuses/update.json";
            String expected = URLEncoder.encode(testString, UTF_8);
            assertEquals(expected, Utils.percentEncodeSensitive(testString));

            assertFalse("Sensitive string [" + testString + "] should not have appeared in trace but did.", outputMgr.checkForLiteralTrace(testString));
            assertFalse("Sensitive string [" + expected + "] should not have appeared in trace but did.", outputMgr.checkForLiteralTrace(expected));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    private Set<String> validateNonce(String nonce, int expectedLength, Set<String> previousResults) {
        assertEquals("Nonce length did not match the expected value. Nonce: [" + nonce + "].", expectedLength, nonce.length());
        if (expectedLength > 6) {
            // Nonces of length 6 or less might be generated more than once with the number of iterations the tests are running
            // Nonces of any length longer than that should statistically not be generated more than once
            assertFalse("Nonce [" + nonce + "] has already been generated. Statistically, this should not happen.", previousResults.contains(nonce));
        }

        // Ensure nonce is alphanumeric
        String alphaNumRegex = ALPHA_NUM_REGEX + "{" + expectedLength + "}";
        Pattern p = Pattern.compile(alphaNumRegex);
        Matcher m = p.matcher(nonce);
        assertTrue("Nonce [" + nonce + "] did not adhere to expected pattern \"" + alphaNumRegex + "\".", m.matches());

        // Add to set of results to make sure no two nonces are the same
        previousResults.add(nonce);
        return previousResults;
    }

}
