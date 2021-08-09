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

public class ResponseStatusExpectationTest extends CommonSpecificExpectationTest {

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
    public void test_constructor_nullArgs() {
        try {
            String testAction = null;
            String checkType = null;
            String searchForVal = null;
            String failureMsg = null;

            ResponseStatusExpectation exp = new ResponseStatusExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_STATUS, checkType, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor() {
        try {
            String testAction = "testAction";
            String checkType = "checkType";
            String searchForVal = "searchForVal";
            String failureMsg = "failureMsg";

            ResponseStatusExpectation exp = new ResponseStatusExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_STATUS, checkType, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_intStatus_noFailureMsg() {
        try {
            int expectedStatus = -1234;

            ResponseStatusExpectation exp = new ResponseStatusExpectation(TEST_ACTION, expectedStatus);

            String expectedErrorMsg = String.format(ResponseStatusExpectation.DEFAULT_FAILURE_MSG, Integer.toString(expectedStatus), TEST_ACTION);
            verifyExpectationValues(exp, TEST_ACTION, Constants.RESPONSE_STATUS, Constants.STRING_EQUALS, null, Integer.toString(expectedStatus), expectedErrorMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_intStatus_withFailureMsg() {
        try {
            int expectedStatus = 500;
            String failureMsg = "failureMsg";

            ResponseStatusExpectation exp = new ResponseStatusExpectation(TEST_ACTION, expectedStatus, failureMsg);

            verifyExpectationValues(exp, TEST_ACTION, Constants.RESPONSE_STATUS, Constants.STRING_EQUALS, null, Integer.toString(expectedStatus), failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validate **************************************/

    @Override
    @Test
    public void test_validate_nullContentObject() {
        try {
            Expectation exp = createBasicExpectation();
            runNegativeValidateTestForCheckType_contains(exp, null, "-1");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_unknownCheckType() {
        try {
            Expectation exp = new ResponseStatusExpectation(TEST_ACTION, "check type", SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitWebResponse;
            setValidateTestExpectations(content, 200);

            runNegativeValidateTestForCheckType_unknown(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_unsupportedResponseType() {
        try {
            Expectation exp = createBasicExpectation();
            Object content = "Some content";

            runNegativeValidateTestForUnsupportedResponseType(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullAction() {
        try {
            Expectation exp = new ResponseStatusExpectation(null, Constants.STRING_CONTAINS, "200", FAILURE_MESSAGE);
            Object content = 200;

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_fails() {
        try {
            Expectation exp = new ResponseStatusExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            int statusCode = 200;
            setValidateTestExpectations(content, statusCode);

            runNegativeValidateTestForCheckType_contains(exp, content, Integer.toString(statusCode));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_passes() {
        try {
            Expectation exp = new ResponseStatusExpectation(TEST_ACTION, Constants.STRING_CONTAINS, "200", FAILURE_MESSAGE);
            Object content = htmlunitXmlPage;
            int statusCode = 200;
            setValidateTestExpectations(content, statusCode);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_fails() {
        try {
            Expectation exp = new ResponseStatusExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, "403", FAILURE_MESSAGE);
            Object content = 403;

            runNegativeValidateTestForCheckType_doesNotContain(exp, content, "403");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_passes() {
        try {
            Expectation exp = new ResponseStatusExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, "200", FAILURE_MESSAGE);
            Object content = htmlunitHtmlPage;
            int statusCode = 401;
            setValidateTestExpectations(content, statusCode);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_fails() {
        try {
            Expectation exp = new ResponseStatusExpectation(TEST_ACTION, Constants.STRING_MATCHES, "[0-9]{4}", FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            int statusCode = 200;
            setValidateTestExpectations(content, statusCode);

            runNegativeValidateTestForCheckType_matches(exp, content, Integer.toString(statusCode));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_passes() {
        try {
            Expectation exp = new ResponseStatusExpectation(TEST_ACTION, Constants.STRING_MATCHES, "[0-9]{3}", FAILURE_MESSAGE);
            Object content = htmlunitXmlPage;
            int statusCode = 403;
            setValidateTestExpectations(content, statusCode);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_fails() {
        try {
            Expectation exp = new ResponseStatusExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, "[0-9]{3}", FAILURE_MESSAGE);
            Object content = htmlunitWebResponse;
            int statusCode = 200;
            setValidateTestExpectations(content, statusCode);

            runNegativeValidateTestForCheckType_doesNotMatch(exp, content, Integer.toString(statusCode));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_passes() {
        try {
            Expectation exp = new ResponseStatusExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitHtmlPage;
            int statusCode = 999;
            setValidateTestExpectations(content, statusCode);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    @Override
    protected Expectation createBasicExpectation() {
        return new ResponseStatusExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    @Override
    protected Expectation createBasicExpectationWithNoAction() {
        return new ResponseStatusExpectation(null, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    protected void setValidateTestExpectations(Object responseObject, final Object content) {
        setWebResponseExpectation(responseObject);
        mockery.checking(new org.jmock.Expectations() {
            {
                one(htmlunitWebResponse).getStatusCode();
                will(returnValue((int) content));
            }
        });
    }

}
