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
package com.ibm.ws.http.channel.test.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.http.channel.internal.HttpObjectFactory;
import com.ibm.ws.http.channel.internal.HttpTrailersImpl;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.http.channel.HttpTrailerGenerator;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

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
         *            the HTTP header to generate as a trailer.
         * @param message
         *            the message to append the trailer to.
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
         *            the HTTP header to generate as a trailer.
         * @param message
         *            the message to append the trailer to.
         * @return the bytes comprising the value of the trailer.
         */
        @Override
        public byte[] generateTrailerValue(String hdr, HttpTrailers message) {
            return ("en-US".getBytes());
        }
    }
}
