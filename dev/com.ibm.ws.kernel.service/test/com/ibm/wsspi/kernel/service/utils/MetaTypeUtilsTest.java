/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;
import test.utils.Utils;

/**
 *
 */
public class MetaTypeUtilsTest {

    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.trace("*=event=enabled");
        outputMgr.logTo(Utils.TEST_DATA);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @Test
    public void testParseLong() {
        final String m = "testParseLong";
        try {
            long value;

            value = MetatypeUtils.parseLong(m, m, Long.valueOf(2), 0);
            assertEquals("parsed value should equal long parameter", 2, value);

            value = MetatypeUtils.parseLong(m, m, "2", 0);
            assertEquals("parsed value should equal string parameter", 2, value);

            value = MetatypeUtils.parseLong(m, m, null, 0);
            assertEquals("parsed value should equal default", 0, value);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBadLongValue() {
        final String m = "testParseBadLongValue";
        try {
            MetatypeUtils.parseLong(m, m, m, 0);
            fail("parsed value is an invalid long: exception not thrown");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBadLongObjectValue() {
        final String m = "testParseBadLongObjectValue";
        try {
            MetatypeUtils.parseLong(m, m, Boolean.FALSE, 0);
            fail("parsed value is an invalid long: exception not thrown");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testParseBoolean() {
        final String m = "testParseBoolean";
        try {
            boolean value;

            value = MetatypeUtils.parseBoolean(m, m, Boolean.FALSE, true);
            assertEquals("parsed value should equal long parameter", false, value);

            value = MetatypeUtils.parseBoolean(m, m, "false", true);
            assertEquals("parsed value should equal string parameter", false, value);

            value = MetatypeUtils.parseBoolean(m, m, null, true);
            assertEquals("parsed value should equal default", true, value);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBadBooleanValue() {
        final String m = "testParseBadBooleanValue";
        try {
            MetatypeUtils.parseBoolean(m, m, "yucky", true);
            fail("parsed value is an invalid boolean: exception not thrown");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBadBooleanObjectValue() {
        final String m = "testParseBadBooleanObjectValue";
        try {
            MetatypeUtils.parseBoolean(m, m, Long.valueOf(0), true);
            fail("parsed value is an invalid boolean: exception not thrown");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testParseStringArray() {
        final String m = "testParseStringArray";
        try {
            String[] value;
            String[] defaults = new String[0];
            String[] expected = new String[] { "1", "2" };
            Collection<String> collection = Arrays.asList(expected);

            value = MetatypeUtils.parseStringArray(m, m, "1, 2", defaults);
            assertArrayEquals("values parsed from string should equal expected string array values", expected, value);

            value = MetatypeUtils.parseStringArray(m, m, collection, defaults);
            assertArrayEquals("collection should be converted to an array with expected values", expected, value);

            value = MetatypeUtils.parseStringArray(m, m, expected, defaults);
            assertSame("Original string array parameter should be returned", expected, value);

            value = MetatypeUtils.parseStringArray(m, m, null, defaults);
            assertSame("Returned value should be default", defaults, value);

            value = MetatypeUtils.parseStringArray(m, m, m, defaults);
            assertEquals("Returned value should be an array of one", 1, value.length);
            assertEquals("First element should match string", m, value[0]);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBadStringArrayValue() {
        final String m = "testParseBadStringArrayValue";
        try {
            MetatypeUtils.parseStringArray(m, m, Boolean.FALSE, new String[0]);
            fail("parsed value is an invalid list of strings: exception not thrown");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testParseStringCollection() {
        final String m = "testParseStringCollection";
        try {
            String[] array = new String[] { "1", "2" };
            Collection<String> value;
            Collection<String> expected = Arrays.asList(array);
            Collection<String> defaults = Collections.emptyList();

            value = MetatypeUtils.parseStringCollection(m, m, "1, 2", defaults);
            assertEquals("values parsed from string should equal expected string collection values", expected, value);

            value = MetatypeUtils.parseStringCollection(m, m, array, defaults);
            assertEquals("array should be converted to collection with expected values", expected, value);

            value = MetatypeUtils.parseStringCollection(m, m, expected, defaults);
            assertSame("Original string collection parameter should be returned", expected, value);

            value = MetatypeUtils.parseStringCollection(m, m, null, defaults);
            assertSame("Returned value should be default", defaults, value);

            value = MetatypeUtils.parseStringCollection(m, m, m, defaults);
            assertEquals("Returned value should be an collection of one", 1, value.size());
            assertEquals("First element should match string", m, value.iterator().next());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBadStringCollectionValue() {
        final String m = "testParseBadStringCollectionValue";
        try {
            List<String> empty = Collections.emptyList();
            MetatypeUtils.parseStringCollection(m, m, Boolean.FALSE, empty);
            fail("parsed value is an invalid collection of strings: exception not thrown");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testParseInteger() {
        final String m = "testParseInteger";
        try {
            long value;

            value = MetatypeUtils.parseInteger(m, m, Long.valueOf(2), 0);
            assertEquals("parsed value should equal long parameter", 2, value);

            value = MetatypeUtils.parseInteger(m, m, "2", 0);
            assertEquals("parsed value should equal string parameter", 2, value);

            value = MetatypeUtils.parseInteger(m, m, null, 0);
            assertEquals("parsed value should equal default", 0, value);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBadIntegerValue() {
        final String m = "testParseBadIntegerValue";
        try {
            MetatypeUtils.parseInteger(m, m, m, 0);
            fail("parsed value is an invalid integer: exception not thrown");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseBadIntegerObjectValue() {
        final String m = "testParseBadIntegerObjectValue";
        try {
            MetatypeUtils.parseInteger(m, m, Boolean.FALSE, 0);
            fail("parsed value is an invalid integer: exception not thrown");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testParseIntegerArray() {
        final String m = "testParseIntegerArray";
        try {
            int[] value;
            int[] defaults = new int[0];
            int[] expected = new int[] { 1, 2 };
            Collection<Integer> collection = Arrays.asList(1, 2);

            value = MetatypeUtils.parseIntegerArray(m, m, "1, 2", defaults);
            assertArrayEquals("values parsed from string should equal expected string array values", expected, value);

            value = MetatypeUtils.parseIntegerArray(m, m, collection, defaults);
            assertArrayEquals("collection should be converted to an array with expected values", expected, value);

            value = MetatypeUtils.parseIntegerArray(m, m, expected, defaults);
            assertSame("Original integer array parameter should be returned", expected, value);

            value = MetatypeUtils.parseIntegerArray(m, m, null, defaults);
            assertSame("Returned value should be default", defaults, value);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testParseDuration() throws Exception {
        final String m = "testParseDuration";
        try {
            long value;

            value = MetatypeUtils.parseDuration(m, m, Long.valueOf(400), 0);
            assertEquals("parsed value should equal long parameter", 400, value);

            value = MetatypeUtils.parseDuration(m, m, "400ms", 0);
            assertEquals("parsed value should equal string parameter", 400, value);

            value = MetatypeUtils.parseDuration(m, m, null, Long.valueOf(500));
            assertEquals("parsed value should equal default", 500, value);

            value = MetatypeUtils.parseDuration(m, m, "400s", 0, TimeUnit.MINUTES);
            assertEquals("parsed value should be converted to minutes", 6, value);

            value = MetatypeUtils.parseDuration(m, m, "10m", 0, TimeUnit.SECONDS);
            assertEquals("parsed value should be converted to seconds", 600, value);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }

    }

    @Test
    public void testTimeConversion() throws Exception {
        assertEquals(Long.valueOf(24), MetatypeUtils.evaluateDuration("1d", TimeUnit.HOURS));
        assertEquals(Long.valueOf(1440), MetatypeUtils.evaluateDuration("1d", TimeUnit.MINUTES));
        assertEquals(Long.valueOf(86400), MetatypeUtils.evaluateDuration("1d", TimeUnit.SECONDS));
        assertEquals(Long.valueOf(86400000), MetatypeUtils.evaluateDuration("1d", TimeUnit.MILLISECONDS));
        assertEquals(Long.valueOf(1), MetatypeUtils.evaluateDuration("86400000ms", TimeUnit.DAYS));
        assertEquals(Long.valueOf(1), MetatypeUtils.evaluateDuration("86400s", TimeUnit.DAYS));
        assertEquals(Long.valueOf(1), MetatypeUtils.evaluateDuration("1440m", TimeUnit.DAYS));
        assertEquals(Long.valueOf(1), MetatypeUtils.evaluateDuration("24h", TimeUnit.DAYS));

        // Test converting into finer units
        assertEquals(Long.valueOf(60), MetatypeUtils.evaluateDuration("1h", TimeUnit.MINUTES));
        assertEquals(Long.valueOf(60), MetatypeUtils.evaluateDuration("1m", TimeUnit.SECONDS));
        assertEquals(Long.valueOf(24000), MetatypeUtils.evaluateDuration("24s", TimeUnit.MILLISECONDS));

        // Test up conversion and rounding
        assertEquals(Long.valueOf(2), MetatypeUtils.evaluateDuration("50h", TimeUnit.DAYS));
        assertEquals(Long.valueOf(0), MetatypeUtils.evaluateDuration("1000ms", TimeUnit.DAYS));
        assertEquals(Long.valueOf(1), MetatypeUtils.evaluateDuration("76m", TimeUnit.HOURS));
        assertEquals(Long.valueOf(5), MetatypeUtils.evaluateDuration("300042ms", TimeUnit.MINUTES));

        // Test multiple unit values and goofy whitespace
        assertEquals(Long.valueOf(90), MetatypeUtils.evaluateDuration("1h30m", TimeUnit.MINUTES));
        assertEquals(Long.valueOf(91), MetatypeUtils.evaluateDuration("60000ms      1   h  30m", TimeUnit.MINUTES));
        assertEquals(Long.valueOf(3), MetatypeUtils.evaluateDuration("1 d 48 h 59 m 59 s 999 ms", TimeUnit.DAYS));

        // Test invalid duration value
        boolean pass = false;
        try {
            MetatypeUtils.evaluateDuration("garbage", TimeUnit.MILLISECONDS);
        } catch (IllegalArgumentException ex) {
            pass = true;
        }
        assertTrue("ConfigParserException not received for invalid time value", pass);
    }

    @Test
    public void testLocale() throws Exception {
        assertEquals(5, (long) MetatypeUtils.evaluateDuration("5j", TimeUnit.DAYS)); // fr
        assertEquals(5, (long) MetatypeUtils.evaluateDuration("5g", TimeUnit.DAYS)); // it - conflicts with po "hours"

        assertEquals(5, (long) MetatypeUtils.evaluateDuration("5hod", TimeUnit.HOURS)); // cs
        assertEquals(120, (long) MetatypeUtils.evaluateDuration("5g", TimeUnit.HOURS)); // po - conflicts with it "days"

        assertEquals(5, (long) MetatypeUtils.evaluateDuration("5min", TimeUnit.MINUTES)); // cs
        assertEquals(5, (long) MetatypeUtils.evaluateDuration("5\u043c", TimeUnit.MINUTES)); // ru

        assertEquals(5, (long) MetatypeUtils.evaluateDuration("5mp", TimeUnit.SECONDS)); // hu

        assertEquals(5, (long) MetatypeUtils.evaluateDuration("5\u6beb\u79d2", TimeUnit.MILLISECONDS)); // zh_TW
    }
}
