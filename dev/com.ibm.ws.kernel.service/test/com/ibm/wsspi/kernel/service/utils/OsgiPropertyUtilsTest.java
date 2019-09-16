/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wsspi.kernel.service.utils.OsgiPropertyUtils;

import test.common.SharedOutputManager;
import test.utils.Utils;

/**
 *
 */
public class OsgiPropertyUtilsTest {

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

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.OsgiPropertyUtils#getProperty(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testGetProperty() {
        final String m = "testGetProperty";
        try {
            String defaultValue = "default";
            String result = OsgiPropertyUtils.getProperty(m, defaultValue);
            assertSame("Default returned for unknown property", defaultValue, result);

            System.setProperty(m, "value");
            result = OsgiPropertyUtils.getProperty(m, defaultValue);
            assertEquals("Value should be retrieved from system properties (no framework)", "value", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.OsgiPropertyUtils#getInteger(java.lang.String, int)}.
     */
    @Test
    public void testGetInteger() {
        final String m = "testGetInteger";
        try {
            int defaultValue = 1;
            int result = OsgiPropertyUtils.getInteger(m, defaultValue);
            assertEquals("Default returned for unknown property", defaultValue, result);

            System.setProperty(m, "2");
            result = OsgiPropertyUtils.getInteger(m, defaultValue);
            assertEquals("Value should be retrieved from system properties (no framework)", 2, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.OsgiPropertyUtils#getLong(java.lang.String, long)}.
     */
    @Test
    public void testGetLong() {
        final String m = "testGetLong";
        try {
            long defaultValue = 1;
            long result = OsgiPropertyUtils.getLong(m, defaultValue);
            assertEquals("Default returned for unknown property", defaultValue, result);

            System.setProperty(m, "2");
            result = OsgiPropertyUtils.getLong(m, defaultValue);
            assertEquals("Value should be retrieved from system properties (no framework)", 2, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.OsgiPropertyUtils#getBoolean(java.lang.String, boolean)}.
     */
    @Test
    public void testGetBoolean() {
        final String m = "testGetBoolean";
        try {
            System.setProperty(m, "true");
            boolean result = OsgiPropertyUtils.getBoolean(m);
            assertEquals("Value should be retrieved from system properties (no framework)", true, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

}
