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
package com.ibm.ws.http.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 * Test the encoding utilities class.
 */
public class EncodingUtilsTest {
    private static SharedOutputManager outputMgr;
    private static EncodingUtilsImpl utils = null;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
        utils = new EncodingUtilsImpl();
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
        utils = null;
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
    }

    /**
     * Test extracting an optional charset from a content-type style header.
     */
    @Test
    public void testGetCharset() {
        try {
            assertNull(utils.getCharsetFromContentType(null));
            assertNull(utils.getCharsetFromContentType(""));
            assertNull(utils.getCharsetFromContentType("text/html"));
            assertEquals("en", utils.getCharsetFromContentType("text/html;charset=en"));
            assertNull(utils.getCharsetFromContentType("text;charset="));
            assertEquals("\"en", utils.getCharsetFromContentType("text;charset=\"en"));
            assertEquals("fr", utils.getCharsetFromContentType("text;charset=\"fr\""));
            assertEquals("de", utils.getCharsetFromContentType("text;  charset=de"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testStream", t);
        }
    }

    /**
     * Test getting an encoding from initial locale/encoding match lists.
     */
    @Test
    public void testGetEncoding() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("encoding.de", "TEST1");
            props.put("encoding.en", "TEST2");
            props.put("encoding.fr_FR", "TEST3");
            utils.modified(props);

            assertEquals("TEST1", utils.getEncodingFromLocale(new Locale("de")));
            assertEquals("TEST2", utils.getEncodingFromLocale(new Locale("en", "US")));
            assertEquals("TEST3", utils.getEncodingFromLocale(new Locale("fr", "FR")));
            assertNull(utils.getEncodingFromLocale(new Locale("es")));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testGetEncoding", t);
        }
    }

    /**
     * Test getting a converter from initial property match lists.
     */
    @Test
    public void testGetConverter() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("converter.EUC-JP", "TEST1");
            props.put("converter.Big5", "TEST2");
            props.put("converter.ISO-2022", "TEST3");
            utils.modified(props);

            assertNull(utils.getJvmConverter(null));
            assertEquals("test1", utils.getJvmConverter("euc-jp"));
            assertEquals("test2", utils.getJvmConverter("Big5"));
            assertEquals("test3", utils.getJvmConverter("Iso-2022"));
            assertEquals("input", utils.getJvmConverter("input"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testGetConverter", t);
        }
    }

    /**
     * Test the utility to extract locales from a request message.
     */
    @Test
    public void testGetLocales() {
        try {
            List<Locale> rc;
            rc = utils.getLocales(null);
            assertNotNull(rc);
            assertEquals(1, rc.size());
            assertEquals(Locale.getDefault(), rc.get(0));

            rc = utils.getLocales("en-us,en;q=0.5");
            assertNotNull(rc);
            assertEquals(2, rc.size());
            assertEquals(new Locale("en", "us", ""), rc.get(0));
            assertEquals(new Locale("en", "", ""), rc.get(1));

            rc = utils.getLocales("fr;q=0.0");
            assertNotNull(rc);
            assertEquals(1, rc.size());
            assertEquals(Locale.getDefault(), rc.get(0));

            rc = utils.getLocales("de, fr;q=0.0");
            assertNotNull(rc);
            assertEquals(1, rc.size());
            assertEquals(new Locale("de", "", ""), rc.get(0));

            rc = utils.getLocales("de, fr;q=0.0, es;q=0.75");
            assertNotNull(rc);
            assertEquals(2, rc.size());
            assertEquals(new Locale("de", "", ""), rc.get(0));
            assertEquals(new Locale("es", "", ""), rc.get(1));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testGetLocales", t);
        }
    }

    /**
     * Test the utility that checks whether charsets are supported on this
     * running JVM or not.
     */
    @Test
    public void testIsCharsetSupported() {
        try {
            assertTrue(utils.isCharsetSupported(utils.getDefaultEncoding()));
            assertFalse(utils.isCharsetSupported("bogus"));
            assertFalse(utils.isCharsetSupported(null));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testIsCharsetSupported", t);
        }
    }

    /**
     * Test the various extra utility methods.
     */
    @Test
    public void testUtils() {
        try {
            assertNull(utils.stripQuotes(null));
            assertEquals("", utils.stripQuotes(""));
            assertEquals("test1", utils.stripQuotes("test1"));
            assertEquals("test2", utils.stripQuotes("\"test2"));
            assertEquals("test3", utils.stripQuotes("test3\""));
            assertEquals("test4", utils.stripQuotes("\"test4\""));
            assertEquals("test5", utils.stripQuotes("'test5"));
            assertEquals("test6", utils.stripQuotes("test6'"));
            assertEquals("test7", utils.stripQuotes("'test7'"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testUtils", t);
        }
    }
}
