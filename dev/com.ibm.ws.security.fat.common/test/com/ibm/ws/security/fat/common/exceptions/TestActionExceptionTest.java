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

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class TestActionExceptionTest extends CommonTestClass {

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

    /************************************** constructor **************************************/

    /**
     * Tests:
     * - Throws exception with all null arguments
     * Expects:
     * - Exception message should be null
     * - "Exception occurred" message logged
     */
    @Test
    public void test_constructor_nullArgs() {
        try {
            String method = null;
            String message = null;
            Throwable cause = null;

            try {
                throw new TestActionException(method, message, cause);
            } catch (TestActionException e) {
                verifyPattern(e.getMessage(), "^null$");
                assertStringInTrace(outputMgr, "Exception occurred in " + method);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided cause has no message
     * Expects:
     * - Exception message should match the provided failure message, plus the toString() of the cause
     * - "Exception occurred" message logged
     */
    @Test
    public void test_constructor_causeHasNoMessage() {
        try {
            String method = "some method";
            Throwable cause = new Exception();

            try {
                throw new TestActionException(method, defaultExceptionMsg, cause);
            } catch (TestActionException e) {
                verifyPattern(e.getMessage(), "^" + Pattern.quote(defaultExceptionMsg) + " " + Pattern.quote(cause.toString()) + "$");
                assertStringInTrace(outputMgr, "Exception occurred in " + method);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided cause includes its own message
     * Expects:
     * - Exception message should match the provided failure message, plus the toString() of the cause
     * - "Exception occurred" message logged
     */
    @Test
    public void test_constructor_causeWithMessage() {
        try {
            String method = "some method";
            String subMessage = "Another sub-message from the cause";
            Throwable cause = new Exception(subMessage);

            try {
                throw new TestActionException(method, defaultExceptionMsg, cause);
            } catch (TestActionException e) {
                verifyPattern(e.getMessage(), "^" + Pattern.quote(defaultExceptionMsg) + " " + Pattern.quote(cause.toString()) + "$");
                assertStringInTrace(outputMgr, "Exception occurred in " + method);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided cause includes its own message
     * Expects:
     * - Exception message should match the provided failure message, plus the toString() of the original cause
     * - "Exception occurred" message logged
     */
    @Test
    public void test_constructor_nestedCause() {
        try {
            String method = "some method";
            String cause1Message = "Sub-message for the first cause";
            Throwable cause1 = new Exception(cause1Message);
            String cause2Message = "Sub-message for the second cause";
            Throwable cause2 = new Exception(cause2Message, cause1);
            String cause3Message = "Sub-message for the third cause";
            Throwable cause3 = new Exception(cause3Message, cause2);

            try {
                throw new TestActionException(method, defaultExceptionMsg, cause3);
            } catch (TestActionException e) {
                verifyPattern(e.getMessage(), "^" + Pattern.quote(defaultExceptionMsg) + " " + Pattern.quote(cause1.toString()) + "$");
                assertStringInTrace(outputMgr, "Exception occurred in " + method);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
