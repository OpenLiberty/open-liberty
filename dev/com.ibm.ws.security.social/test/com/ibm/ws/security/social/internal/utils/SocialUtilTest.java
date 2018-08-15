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
package com.ibm.ws.security.social.internal.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.test.CommonTestClass;

import test.common.SharedOutputManager;

public class SocialUtilTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.social.*=all");

    final String[] invalidQueryChars = new String[] { "#", "[", "]", "{", "}", "|", "\\", "<", ">" };

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
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** validateQueryString **************************************/

    @Test
    public void validateQueryString_null() {
        try {
            assertTrue("Null input should produce affirmative result.", SocialUtil.validateQueryString(null));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateQueryString_empty() {
        try {
            assertTrue("Empty input should produce affirmative result.", SocialUtil.validateQueryString(""));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateQueryString_simple() {
        try {
            assertTrue("Valid input should produce affirmative result.", SocialUtil.validateQueryString("?"));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateQueryString_simpleKeyValue() {
        try {
            String input = "key=value";
            assertTrue("Valid input [" + input + "] should produce affirmative result.", SocialUtil.validateQueryString(input));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateQueryString_allValidChars() {
        try {
            String input = "a-zA-Z0-9._~%!$&'()*+,;=:@/?-";
            assertTrue("Input [" + input + "] with all valid chars should produce affirmative result.", SocialUtil.validateQueryString(input));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateQueryString_invalidChar() {
        try {
            String invalidChar = RandomUtils.getRandomSelection(invalidQueryChars);
            String input = "invalid" + invalidChar + "value";
            assertFalse("Input [" + input + "] with invalid chars should produce negative result.", SocialUtil.validateQueryString(input));

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateEndpointFormat **************************************/

    @Test
    public void validateEndpointFormat_nullUrl() {
        try {
            try {
                SocialUtil.validateEndpointFormat(null);
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointFormat_emptyUrl() {
        try {
            try {
                SocialUtil.validateEndpointFormat("");
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointFormat_invalidUrlFormat() {
        try {
            final String url = "invalidURL";
            try {
                SocialUtil.validateEndpointFormat(url);
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5496W_HTTP_URI_DOES_NOT_START_WITH_HTTP, url);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointFormat_truncatedUrl() {
        try {
            final String url = "http://";
            try {
                SocialUtil.validateEndpointFormat(url);
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5417E_EXCEPTION_INITIALIZING_URL, Pattern.quote(url));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointFormat_urlWithInvalidChars() {
        try {
            final String url = "http://test|domain.com";
            try {
                SocialUtil.validateEndpointFormat(url);
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5417E_EXCEPTION_INITIALIZING_URL, Pattern.quote(url));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointFormat_nonHttpUrl() {
        try {
            final String url = "ftp://test-domain.com:80/context/path";
            try {
                SocialUtil.validateEndpointFormat(url);
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5496W_HTTP_URI_DOES_NOT_START_WITH_HTTP, Pattern.quote(url));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointFormat_nonHttpUrl_allowNonHttp() {
        try {
            final String url = "ftp://test-domain.com:80/context/path";
            // Valid non-HTTP URL should not throw an exception
            SocialUtil.validateEndpointFormat(url, false);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointFormat_validUrl() {
        try {
            final String url = "http://test-domain.com:80/context/path";
            // Valid URL should not throw an exception
            SocialUtil.validateEndpointFormat(url);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointFormat_urlWithQuery() {
        try {
            final String url = "http://test-domain.com:80/context/path?with=some";
            try {
                SocialUtil.validateEndpointFormat(url);
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5488W_URI_CONTAINS_INVALID_CHARS, Pattern.quote(url));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointFormat_urlWithFragment() {
        try {
            final String url = "http://test-domain.com:80/context/path#fragment";
            try {
                SocialUtil.validateEndpointFormat(url);
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5488W_URI_CONTAINS_INVALID_CHARS, Pattern.quote(url));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateEndpointWithQuery **************************************/

    @Test
    public void validateEndpointWithQuery_nullUrl() {
        try {
            try {
                SocialUtil.validateEndpointWithQuery(null);
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointWithQuery_emptyUrl() {
        try {
            try {
                SocialUtil.validateEndpointWithQuery("");
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5475E_NULL_OR_EMPTY_REQUEST_URL);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointWithQuery_invalidUrl() {
        try {
            final String url = "invalid URL";
            try {
                SocialUtil.validateEndpointWithQuery(url);
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5417E_EXCEPTION_INITIALIZING_URL, url);
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointWithQuery_validUrlNoQuery() {
        try {
            final String url = "http://test-domain.com:80/context/path";
            SocialUtil.validateEndpointWithQuery(url);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointWithQuery_endsWithQuery() {
        try {
            final String url = "http://test-domain.com:80/context/path?";
            SocialUtil.validateEndpointWithQuery(url);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointWithQuery_queryWithAllValidChars() {
        try {
            String validQueryChars = "a-z?A-Z?0-9?._~%!$&'()*+,;=:@/?-";
            final String url = "http://test-domain.com:80/context/path?" + validQueryChars;
            SocialUtil.validateEndpointWithQuery(url);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void validateEndpointWithQuery_invalidQuery() {
        try {
            String invalidChar = RandomUtils.getRandomSelection(invalidQueryChars);
            String validQueryChars = "query=value " + invalidChar + " not&valid";
            final String url = "http://test-domain.com:80/context/path?" + validQueryChars;
            try {
                SocialUtil.validateEndpointWithQuery(url);
                fail("Should have thrown SocialLoginException but did not.");
            } catch (SocialLoginException e) {
                verifyExceptionWithInserts(e, CWWKS5417E_EXCEPTION_INITIALIZING_URL, Pattern.quote(url));
            }

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
