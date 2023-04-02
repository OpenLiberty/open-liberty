/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class AutomationToolsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    AutomationTools tools = new AutomationTools();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        tools = new AutomationTools();
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

    @Test
    public void test_convertHeadersListToMap_null() {
        try {
            List<NameValuePair> headers = null;
            Map<String, String[]> result = AutomationTools.convertHeadersListToMap(headers);
            assertNull("Result should have been null but was " + result, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_convertHeadersListToMap_empty() {
        try {
            List<NameValuePair> headers = new ArrayList<>();
            Map<String, String[]> result = AutomationTools.convertHeadersListToMap(headers);
            assertNotNull("Result should not have been null but was.", result);
            assertTrue("Result should have been empty but was: " + result, result.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_convertHeadersListToMap_oneEntry() {
        try {
            List<NameValuePair> headers = new ArrayList<>();
            headers.add(new NameValuePair("header", "value"));

            Map<String, String[]> result = AutomationTools.convertHeadersListToMap(headers);

            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result did not have expected number of entries. Result was: " + result, 1, result.size());
            assertTrue("Result did not contain expected entry \"header\". Result was: " + result, result.containsKey("header"));
            String[] headerValues = result.get("header");
            assertEquals("Result value should have had one entry but did not. Result was: " + Arrays.toString(headerValues), 1, headerValues.length);
            assertEquals("Result value did not match expected value.", "value", headerValues[0]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_convertHeadersListToMap_multipleEntries_unique() {
        try {
            List<NameValuePair> headers = new ArrayList<>();
            int numUniqueEntries = 3;
            for (int i = 1; i <= numUniqueEntries; i++) {
                headers.add(new NameValuePair("header" + i, "value" + i));
            }

            Map<String, String[]> result = AutomationTools.convertHeadersListToMap(headers);

            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result did not have expected number of entries. Result was: " + result, numUniqueEntries, result.size());
            for (int i = 1; i <= numUniqueEntries; i++) {
                String expectedEntry = "header" + i;
                String expectedValue = "value" + i;
                assertTrue("Result did not contain expected entry \"" + expectedEntry + "\". Result was: " + result, result.containsKey(expectedEntry));
                String[] headerValues = result.get(expectedEntry);
                assertEquals("Entry for \"" + expectedEntry + "\" should have had one entry but did not. Values were: " + Arrays.toString(headerValues), 1, headerValues.length);
                assertEquals("Entry for \"" + expectedEntry + "\" did not match expected value.", expectedValue, headerValues[0]);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_convertHeadersListToMap_multipleValuesForHeader() {
        try {
            String headerName = "header";
            List<NameValuePair> headers = new ArrayList<>();
            int numUniqueValues = 3;
            for (int i = 1; i <= numUniqueValues; i++) {
                headers.add(new NameValuePair(headerName, "value" + i));
            }

            Map<String, String[]> result = AutomationTools.convertHeadersListToMap(headers);

            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Result did not have expected number of entries. Result was: " + result, 1, result.size());
            assertTrue("Result did not contain expected entry \"" + headerName + "\". Result was: " + result, result.containsKey(headerName));
            String[] headerValues = result.get(headerName);
            assertEquals("Entry for \"" + headerName + "\" did not have the expected number of values. Values were: " + Arrays.toString(headerValues), numUniqueValues, headerValues.length);
            for (int i = 0; i < numUniqueValues; i++) {
                String expectedValue = "value" + (i + 1);
                assertEquals("Value for entry " + i + " did not match expected value.", expectedValue, headerValues[i]);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
