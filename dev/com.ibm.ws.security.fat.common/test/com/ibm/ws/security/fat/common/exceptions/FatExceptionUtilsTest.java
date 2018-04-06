/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.exceptions;

import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class FatExceptionUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

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

    /************************************** buildCumulativeException **************************************/

    /**
     * Tests:
     * - Null exception list provided
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_buildCumulativeException_nullExceptionList() {
        try {
            List<Exception> exceptions = null;
            Exception result = FatExceptionUtils.buildCumulativeException(exceptions);
            assertNull("Result should have been null but was: " + result, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Exception list includes only a null Exception object
     * Expects:
     * - Result should be null
     */
    @Test
    public void test_buildCumulativeException_listIncludesNullException() {
        try {
            List<Exception> exceptions = new ArrayList<Exception>();
            exceptions.add(null);

            Exception result = FatExceptionUtils.buildCumulativeException(exceptions);
            assertNull("Result should have been null but was: " + result, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Exception list includes only one Exception object
     * Expects:
     * - Exception message should indicate only one exception and include that exception's error message.
     */
    @Test
    public void test_buildCumulativeException_oneException() {
        try {
            List<Exception> exceptions = new ArrayList<Exception>();
            exceptions.add(new Exception(defaultExceptionMsg));

            Exception result = FatExceptionUtils.buildCumulativeException(exceptions);
            verifyException(result, Pattern.quote("[Exception #1]: " + defaultExceptionMsg));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Exception list includes multiple exceptions
     * Expects:
     * - Cumulative exception message should include each exception message
     */
    @Test
    public void test_buildCumulativeException_multipleExceptions() {
        try {
            List<Exception> exceptions = new ArrayList<Exception>();
            exceptions.add(new Exception("First exception"));
            exceptions.add(new Exception("Second exception"));
            exceptions.add(new Exception("Third exception"));

            Exception result = FatExceptionUtils.buildCumulativeException(exceptions);
            verifyException(result, Pattern.quote("[Exception #1]: " + "First exception" + "\n<br/>"));
            verifyException(result, Pattern.quote("[Exception #2]: " + "Second exception" + "\n<br/>"));
            verifyException(result, Pattern.quote("[Exception #3]: " + "Third exception"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
