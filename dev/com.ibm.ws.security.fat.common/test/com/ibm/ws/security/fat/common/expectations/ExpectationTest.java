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

import javax.servlet.http.HttpServletResponse;

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

    /************************************** Constructors/getters **************************************/

    @Test
    public void test_constructor_noKey_nullArgs() {
        try {
            String testAction = null;
            String searchLocation = null;
            String checkType = null;
            String searchForVal = null;
            String failureMsg = null;

            Expectation exp = new Expectation(testAction, searchLocation, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, searchLocation, checkType, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_noKey() {
        try {
            String testAction = "testAction";
            String searchLocation = "searchLocation";
            String checkType = "checkType";
            String searchForVal = "searchForVal";
            String failureMsg = "failureMsg";

            Expectation exp = new Expectation(testAction, searchLocation, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, searchLocation, checkType, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_nullArgs() {
        try {
            String testAction = null;
            String searchLocation = null;
            String checkType = null;
            String searchForKey = null;
            String searchForVal = null;
            String failureMsg = null;

            Expectation exp = new Expectation(testAction, searchLocation, checkType, searchForKey, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, searchLocation, checkType, searchForKey, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor() {
        try {
            String testAction = "testAction";
            String searchLocation = "searchLocation";
            String checkType = "checkType";
            String searchForKey = "searchForKey";
            String searchForVal = "searchForVal";
            String failureMsg = "failureMsg";

            Expectation exp = new Expectation(testAction, searchLocation, checkType, searchForKey, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, searchLocation, checkType, searchForKey, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
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

    /************************************** createResponseStatusExpectation **************************************/

    @Test
    public void test_createResponseStatusExpectation_nullAction() {
        try {
            String testAction = null;
            int statusCode = -12345;

            Expectation exp = Expectation.createResponseStatusExpectation(testAction, statusCode);

            String failureMsg = "Did not receive status code [" + statusCode + "] during test action [" + testAction + "].";
            verifyExpectationValues(exp, testAction, Constants.RESPONSE_STATUS, Constants.STRING_CONTAINS, null, Integer.toString(statusCode), failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createResponseStatusExpectation() {
        try {
            String testAction = "Some test action";
            int statusCode = HttpServletResponse.SC_BAD_GATEWAY;

            Expectation exp = Expectation.createResponseStatusExpectation(testAction, statusCode);

            String failureMsg = "Did not receive status code [" + statusCode + "] during test action [" + testAction + "].";
            verifyExpectationValues(exp, testAction, Constants.RESPONSE_STATUS, Constants.STRING_CONTAINS, null, Integer.toString(statusCode), failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** createJsonExpectation **************************************/

    @Test
    public void test_createJsonExpectation_nullArgs() {
        try {
            String testAction = null;
            String searchForKey = null;
            String searchForVal = null;
            String failureMsg = null;

            Expectation exp = Expectation.createJsonExpectation(testAction, searchForKey, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.JSON_OBJECT, null, searchForKey, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_createJsonExpectation() {
        try {
            String testAction = "Some test action";
            String searchForKey = "searchForKey";
            String searchForVal = "searchForVal";
            String failureMsg = "This is a failure message.";

            Expectation exp = Expectation.createJsonExpectation(testAction, searchForKey, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.JSON_OBJECT, null, searchForKey, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
