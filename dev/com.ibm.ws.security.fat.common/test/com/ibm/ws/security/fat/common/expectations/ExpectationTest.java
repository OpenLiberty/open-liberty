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
package com.ibm.ws.security.fat.common.expectations;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.Constants;

import test.common.SharedOutputManager;

public class ExpectationTest extends CommonExpectationTestClass {

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

    /************************************** createResponseExpectation **************************************/

    @Test
    public void test_createResponseExpectation_nullArgs() {
        try {
            String testAction = null;
            String searchForVal = null;
            String failureMsg = null;

            Expectation exp = Expectation.createResponseExpectation(testAction, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createResponseExpectation() {
        try {
            String testAction = "testAction";
            String searchForVal = "I'm looking for this value.";
            String failureMsg = "Some failure message.";

            Expectation exp = Expectation.createResponseExpectation(testAction, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createResponseMissingValueExpectation **************************************/

    @Test
    public void test_createResponseMissingValueExpectation_nullArgs_noFailureMsg() {
        try {
            String testAction = null;
            String searchForVal = null;

            Expectation exp = Expectation.createResponseMissingValueExpectation(testAction, searchForVal);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, searchForVal, "Found [" + searchForVal + "] in the response and should not have.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createResponseMissingValueExpectation_noFailureMsg() {
        try {
            String testAction = "testAction";
            String searchForVal = "I'm looking for this value.";

            Expectation exp = Expectation.createResponseMissingValueExpectation(testAction, searchForVal);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, searchForVal, "Found [" + searchForVal + "] in the response and should not have.");

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createResponseMissingValueExpectation_nullArgs() {
        try {
            String testAction = null;
            String searchForVal = null;
            String failureMsg = null;

            Expectation exp = Expectation.createResponseMissingValueExpectation(testAction, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createResponseMissingValueExpectation() {
        try {
            String testAction = "testAction";
            String searchForVal = "I'm looking for this value.";
            String failureMsg = "Some failure message.";

            Expectation exp = Expectation.createResponseMissingValueExpectation(testAction, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createExceptionExpectation **************************************/

    @Test
    public void test_createExceptionExpectation_nullArgs() {
        try {
            String testAction = null;
            String searchForValue = null;
            String failureMsg = null;

            Expectation exp = Expectation.createExceptionExpectation(testAction, searchForValue, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, null, searchForValue, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createExceptionExpectation() {
        try {

            String testAction = "testAction";
            String searchForValue = "I'm looking for this value.";
            String failureMsg = "Some failure message.";

            Expectation exp = Expectation.createExceptionExpectation(testAction, searchForValue, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, null, searchForValue, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
