/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.http.channel.internal.HttpObjectFactory;
import com.ibm.ws.http.channel.internal.HttpTrailersImpl;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.channel.HttpTrailerGenerator;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import test.common.SharedOutputManager;

/**
 * Test the HTTP trailers class.
 */

public class HttpTrailersImplTest {
    private static SharedOutputManager outputMgr;

    /** The factory to create objects */
    private static final HttpObjectFactory factory = new HttpObjectFactory();
    /** The local reference to a test object. */
    private HttpTrailersImpl trMsg = null;
    /** Objects needed as parameters in the various test cases */
    private HttpContentLanguageGenerator lhtg;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
        this.trMsg = null;
        this.lhtg = null;
    }

    /**
     * Used to set up the test. This method is called by JUnit before each of
     * the tests are executed.
     */
    @Before
    public void setUp() {
        this.trMsg = new HttpTrailersImpl();
        this.trMsg.setFactory(factory);
        this.lhtg = new HttpContentLanguageGenerator();
    }

    /**
     * Get access to the trailer header object itself.
     *
     * @return HttpTrailersImpl
     */
    private HttpTrailersImpl getTrailers() {
        return this.trMsg;
    }

    /**
     * Get access to the Content-Language generator
     *
     * @return HttpContentLanguageGenerator
     */
    private HttpContentLanguageGenerator getLangGen() {
        return this.lhtg;
    }

    /**
     * Test api.
     */
    @Test
    public void test1() {
        try {
            getTrailers().setDeferredTrailer(
                                             HttpHeaderKeys.HDR_CONTENT_LANGUAGE, getLangGen());
            assertTrue(getTrailers().containsDeferredTrailer(
                                                             HttpHeaderKeys.HDR_CONTENT_LANGUAGE));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test1", t);
        }
    }

    /**
     * Test api.
     */
    @Test
    public void test2() {
        try {
            getTrailers().setDeferredTrailer("trailer_test1", getLangGen());
            assertTrue(getTrailers().containsDeferredTrailer("trailer_test1"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test2", t);
        }
    }

    /**
     * Test api.
     */
    @Test
    public void test3() {
        try {
            getTrailers().setDeferredTrailer(
                                             HttpHeaderKeys.HDR_CONTENT_LANGUAGE, getLangGen());
            getTrailers().setDeferredTrailer(
                                             HttpHeaderKeys.HDR_DATE, getLangGen());

            assertTrue(getTrailers().containsDeferredTrailer(
                                                             HttpHeaderKeys.HDR_CONTENT_LANGUAGE));
            assertTrue(getTrailers().containsDeferredTrailer(
                                                             HttpHeaderKeys.HDR_DATE));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test3", t);
        }
    }

    /**
     * Test api.
     */
    @Test
    public void test4() {
        try {
            getTrailers().setDeferredTrailer("trailer_test1", getLangGen());
            getTrailers().setDeferredTrailer("trailer_test2", getLangGen());
            getTrailers().setDeferredTrailer(
                                             HttpHeaderKeys.HDR_CONTENT_LANGUAGE.getName(), getLangGen());

            assertTrue(getTrailers().containsDeferredTrailer("trailer_test1"));
            assertTrue(getTrailers().containsDeferredTrailer("trailer_test2"));
            assertTrue(getTrailers().containsDeferredTrailer(
                                                             HttpHeaderKeys.HDR_CONTENT_LANGUAGE));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test4", t);
        }
    }

    /**
     * Test api.
     */
    @Test
    public void test5() {
        try {
            getTrailers().setDeferredTrailer(HttpHeaderKeys.HDR_CONTENT_LANGUAGE, getLangGen());
            getTrailers().setDeferredTrailer(HttpHeaderKeys.HDR_DATE, getLangGen());

            getTrailers().removeDeferredTrailer(HttpHeaderKeys.HDR_CONTENT_LANGUAGE);
            getTrailers().removeDeferredTrailer(HttpHeaderKeys.HDR_DATE);

            assertFalse(getTrailers().containsDeferredTrailer(
                                                              HttpHeaderKeys.HDR_CONTENT_LANGUAGE));
            assertFalse(getTrailers().containsDeferredTrailer(HttpHeaderKeys.HDR_DATE));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test5", t);
        }
    }

    /**
     * Test api.
     */
    @Test
    public void test6() {
        try {
            getTrailers().setDeferredTrailer("trailer_test1", getLangGen());
            getTrailers().setDeferredTrailer("trailer_test2", getLangGen());
            getTrailers().setDeferredTrailer(
                                             HttpHeaderKeys.HDR_CONTENT_LANGUAGE, getLangGen());

            getTrailers().removeDeferredTrailer("trailer_test1");
            getTrailers().removeDeferredTrailer("trailer_test2");
            getTrailers().removeDeferredTrailer(
                                                HttpHeaderKeys.HDR_CONTENT_LANGUAGE.getName());

            assertFalse(getTrailers().containsDeferredTrailer("trailer_test1"));
            assertFalse(getTrailers().containsDeferredTrailer("trailer_test2"));
            assertFalse(getTrailers().containsDeferredTrailer(
                                                              HttpHeaderKeys.HDR_CONTENT_LANGUAGE.getName()));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test6", t);
        }
    }

    /**
     * Test api.
     */
    @Test
    public void test7() {
        try {
            getTrailers().setDeferredTrailer("trailer_test1", getLangGen());
            getTrailers().setDeferredTrailer("trailer_test2", getLangGen());
            getTrailers().setDeferredTrailer(
                                             HttpHeaderKeys.HDR_CONTENT_LANGUAGE.getName(), getLangGen());
            getTrailers().setDeferredTrailer(HttpHeaderKeys.HDR_DATE, getLangGen());

            getTrailers().computeRemainingTrailers();

            assertTrue(getTrailers().containsHeader(HttpHeaderKeys.HDR_CONTENT_LANGUAGE));
            assertTrue(getTrailers().containsHeader(HttpHeaderKeys.HDR_DATE));
            assertTrue(getTrailers().containsHeader("trailer_test1"));
            assertTrue(getTrailers().containsHeader("trailer_test2"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test7", t);
        }
    }

    @SuppressWarnings("unused")
    private class HttpContentLanguageGenerator implements HttpTrailerGenerator {

        /**
         * Create an object which will return the current date.
         *
         */
        public HttpContentLanguageGenerator() {
            // nothing to do
        }

        /**
         * Return a HDR_CONTENT_LANGUAGE Http header value
         *
         * @param hdr
         *                    the HTTP header to generate as a trailer.
         * @param message
         *                    the message to append the trailer to.
         * @return the bytes comprising the value of the trailer.
         */
        @Override
        public byte[] generateTrailerValue(HeaderKeys hdr, HttpTrailers message) {
            return ("en-US".getBytes());
        }

        /**
         * Return a HDR_CONTENT_LANGUAGE Http header value
         *
         * @param hdr
         *                    the HTTP header to generate as a trailer.
         * @param message
         *                    the message to append the trailer to.
         * @return the bytes comprising the value of the trailer.
         */
        @Override
        public byte[] generateTrailerValue(String hdr, HttpTrailers message) {
            return ("en-US".getBytes());
        }
    }

    /**
     * This test validates that for an invalid header name we get IllegalArgumentException for
     * set and append operations. get, remove and contains methods should just no-op meaning
     * that they should NOT populate the HeaderStorage with a HeaderKeys object. If it would then
     * we would no longer get IllegalArgumentExceptions because once a HeaderKeys object is created
     * we know that it was a valid headerName.
     */
    @Test
    public void testInvalidHeaderName() {

        // loop twice to make sure that nothing gets added to make it not throw an exception
        String[] invalidHeaderNames = new String[] { "(0)", "2\n3", "4\r5" };
        String valueString = "value";
        byte[] valueBytes = valueString.getBytes();
        HttpTrailersImpl r = getTrailers();
        for (String invalidHeaderName : invalidHeaderNames) {
            byte[] invalidHeaderNameBytes = invalidHeaderName.getBytes();
            for (int i = 0; i < 2; ++i) {
                try {
                    r.appendHeader(invalidHeaderNameBytes, valueBytes);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                try {
                    r.appendHeader(invalidHeaderNameBytes, valueString);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                try {
                    r.appendHeader(invalidHeaderName, valueBytes);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                try {
                    r.appendHeader(invalidHeaderName, valueString);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                try {
                    r.appendHeader(invalidHeaderNameBytes, valueBytes, 0, invalidHeaderNameBytes.length);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                try {
                    r.appendHeader(invalidHeaderName, valueBytes, 0, invalidHeaderName.length());
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                assertFalse(r.containsHeader(invalidHeaderNameBytes));

                assertFalse(r.containsHeader(invalidHeaderName));

                assertEquals(0, r.getAllHeaderNames().size());

                assertNull(r.getHeader(invalidHeaderNameBytes).getKey());

                assertNull(r.getHeader(invalidHeaderName).getKey());

                assertEquals(0, r.getHeaders(invalidHeaderNameBytes).size());

                assertEquals(0, r.getHeaders(invalidHeaderName).size());

                assertEquals(0, r.getNumberOfHeaderInstances(invalidHeaderNameBytes));

                assertEquals(0, r.getNumberOfHeaderInstances(invalidHeaderName));

                assertEquals(0, r.getNumberOfHeaders());

                r.removeHeader(invalidHeaderNameBytes);

                r.removeHeader(invalidHeaderNameBytes, 1);

                r.removeHeader(invalidHeaderName);

                r.removeHeader(invalidHeaderName, 1);

                try {
                    r.setHeader(invalidHeaderNameBytes, valueBytes);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                try {
                    r.setHeader(invalidHeaderNameBytes, valueString);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                try {
                    r.setHeader(invalidHeaderName, valueBytes);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                try {
                    r.setHeader(invalidHeaderName, valueString);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                try {
                    r.setHeader(invalidHeaderNameBytes, valueBytes, 0, invalidHeaderNameBytes.length);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                try {
                    r.setHeader(invalidHeaderName, valueBytes, 0, invalidHeaderName.length());
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                // HttpTrailer specific APIs
                assertFalse(r.containsDeferredTrailer(invalidHeaderName));

                try {
                    r.setDeferredTrailer(invalidHeaderName, null);
                    fail("Expected IllegalArgumentException");
                } catch (IllegalArgumentException iae) {
                    // expected
                }

                r.removeDeferredTrailer(invalidHeaderName);
            }
        }
    }
}
