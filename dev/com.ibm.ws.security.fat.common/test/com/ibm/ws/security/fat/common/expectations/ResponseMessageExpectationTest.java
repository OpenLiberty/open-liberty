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

public class ResponseMessageExpectationTest extends CommonSpecificExpectationTest {

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

            ResponseMessageExpectation exp = new ResponseMessageExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_MESSAGE, checkType, null, searchForVal, failureMsg);

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

            ResponseMessageExpectation exp = new ResponseMessageExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_MESSAGE, checkType, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validate **************************************/

    @Test
    public void test_validate_unknownCheckType() {
        try {
            Expectation exp = new ResponseMessageExpectation(TEST_ACTION, "check type", SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = "Some content";

            runNegativeValidateTestForCheckType_unknown(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_unsupportedResponseType() {
        try {
            Expectation exp = createBasicExpectation();
            Object content = new Long(0);

            runNegativeValidateTestForUnsupportedResponseType(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullAction() {
        try {
            Expectation exp = new ResponseMessageExpectation(null, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = "Some " + SEARCH_FOR_VAL + " content";

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullResponseMessage() {
        try {
            Expectation exp = createBasicExpectation();
            Object content = htmlunitXmlPage;
            setValidateTestExpectations(content, null);

            runNegativeValidateTestForNullContent(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_fails() {
        try {
            Expectation exp = new ResponseMessageExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitWebResponse;
            String statusMessage = "Some status message";
            setValidateTestExpectations(content, statusMessage);

            runNegativeValidateTestForCheckType_contains(exp, content, statusMessage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_passes() {
        try {
            Expectation exp = new ResponseMessageExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            String statusMessage = SEARCH_FOR_VAL + " status message";
            setValidateTestExpectations(content, statusMessage);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_fails() {
        try {
            Expectation exp = new ResponseMessageExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = SEARCH_FOR_VAL + " status message";

            runNegativeValidateTestForCheckType_doesNotContain(exp, content, content.toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_passes() {
        try {
            Expectation exp = new ResponseMessageExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitHtmlPage;
            String statusMessage = "Hello, world!";
            setValidateTestExpectations(content, statusMessage);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_fails() {
        try {
            Expectation exp = new ResponseMessageExpectation(TEST_ACTION, Constants.STRING_MATCHES, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = "Some status message";

            runNegativeValidateTestForCheckType_matches(exp, content, content.toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_matchSpansMultipleLines() {
        try {
            String searchForRegex = "line1.+line2";
            Expectation exp = new ResponseMessageExpectation(TEST_ACTION, Constants.STRING_MATCHES, searchForRegex, FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            String statusMessage = "line1 \n\r line2";
            setValidateTestExpectations(content, statusMessage);

            runNegativeValidateTestForCheckType_matches(exp, content, statusMessage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_passes() {
        try {
            String searchForRegex = "line1.+line2";
            Expectation exp = new ResponseMessageExpectation(TEST_ACTION, Constants.STRING_MATCHES, searchForRegex, FAILURE_MESSAGE);
            Object content = htmlunitXmlPage;
            String statusMessage = "line1 \t stuff line2 other";
            setValidateTestExpectations(content, statusMessage);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_fails() {
        try {
            Expectation exp = new ResponseMessageExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitHtmlPage;
            String statusMessage = "Hello, " + SEARCH_FOR_VAL + " world!";
            setValidateTestExpectations(content, statusMessage);

            runNegativeValidateTestForCheckType_doesNotMatch(exp, content, statusMessage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_passes() {
        try {
            Expectation exp = new ResponseMessageExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitWebResponse;
            String statusMessage = "Hello, world!";
            setValidateTestExpectations(content, statusMessage);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    @Override
    protected Expectation createBasicExpectation() {
        return new ResponseMessageExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    @Override
    protected Expectation createBasicExpectationWithNoAction() {
        return new ResponseMessageExpectation(null, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    protected void setValidateTestExpectations(Object responseObject, final Object content) {
        setWebResponseExpectation(responseObject);
        mockery.checking(new org.jmock.Expectations() {
            {
                one(htmlunitWebResponse).getStatusMessage();
                will(returnValue((String) content));
            }
        });
    }

}
