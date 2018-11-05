/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

public class ExceptionMessageExpectationTest extends CommonSpecificExpectationTest {

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

            ExceptionMessageExpectation exp = new ExceptionMessageExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_FULL, checkType, null, searchForVal, failureMsg);

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

            ExceptionMessageExpectation exp = new ExceptionMessageExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_FULL, checkType, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validate **************************************/

    @Test
    public void test_validate_unknownCheckType() {
        try {
            Expectation exp = new ExceptionMessageExpectation(TEST_ACTION, "check type", SEARCH_FOR_VAL, FAILURE_MESSAGE);
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
            Object content = new Integer(0);

            runNegativeValidateTestForUnsupportedResponseType(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullResponseText() {
        try {
            Expectation exp = createBasicExpectation();
            Object content = htmlunitWebResponse;
            setValidateTestExpectations(content, null);

            runNegativeValidateTestForNullContent(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_fails() {
        try {
            Expectation exp = new ExceptionMessageExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = "missing search value";

            runNegativeValidateTestForCheckType_contains(exp, content, content.toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_passes() {
        try {
            Expectation exp = new ExceptionMessageExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitHtmlPage;
            String responseBody = "<html>\n\r<head>\n\r<title>Hello</title>\n\r</head>\n\r<body>Hello, " + SEARCH_FOR_VAL + "!\n\r</body>\n\r</html>";
            setValidateTestExpectations(content, responseBody);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_fails() {
        try {
            Expectation exp = new ExceptionMessageExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            String responseBody = "<html>\n\r<head>\n\r<title>Hello</title>\n\r</head>\n\r<body>Hello, " + SEARCH_FOR_VAL + "!\n\r</body>\n\r</html>";
            setValidateTestExpectations(content, responseBody);

            runNegativeValidateTestForCheckType_doesNotContain(exp, content, responseBody);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_passes() {
        try {
            Expectation exp = new ExceptionMessageExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitXmlPage;
            String responseBody = "Hello, world!";
            setValidateTestExpectations(content, responseBody);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_fails() {
        try {
            Expectation exp = new ExceptionMessageExpectation(TEST_ACTION, Constants.STRING_MATCHES, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitWebResponse;
            String responseBody = "Hello, world!";
            setValidateTestExpectations(content, responseBody);

            runNegativeValidateTestForCheckType_matches(exp, content, responseBody);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_matchSpansMultipleLines() {
        try {
            String searchForRegex = "<html>.+</html>";
            Expectation exp = new ExceptionMessageExpectation(TEST_ACTION, Constants.STRING_MATCHES, searchForRegex, FAILURE_MESSAGE);
            Object content = htmlunitTextPage;
            String responseBody = "<html>\n\r<head>\n\r<title>Hello</title>\n\r</head>\n\r<body>Hello, world!\n\r</body>\n\r</html>";
            setValidateTestExpectations(content, responseBody);

            runNegativeValidateTestForCheckType_matches(exp, content, responseBody);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_passes() {
        try {
            String searchForRegex = "<html>.+</html>";
            Expectation exp = new ExceptionMessageExpectation(TEST_ACTION, Constants.STRING_MATCHES, searchForRegex, FAILURE_MESSAGE);
            Object content = htmlunitWebResponse;
            String responseBody = "<html><head><title>Hello</title></head><body>Hello, world!</body></html>";
            setValidateTestExpectations(content, responseBody);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_fails() {
        try {
            String searchForRegex = "<html>.+</html>";
            Expectation exp = new ExceptionMessageExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, searchForRegex, FAILURE_MESSAGE);
            Object content = htmlunitXmlPage;
            String responseBody = "<html><head><title>Hello</title></head><body>Hello, world!</body></html>";
            setValidateTestExpectations(content, responseBody);

            runNegativeValidateTestForCheckType_doesNotMatch(exp, content, responseBody);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_passes() {
        try {
            Expectation exp = new ExceptionMessageExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = htmlunitHtmlPage;
            String responseBody = "Hello, world!";
            setValidateTestExpectations(content, responseBody);

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    @Override
    protected Expectation createBasicExpectation() {
        return new ExceptionMessageExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    @Override
    protected Expectation createBasicExpectationWithNoAction() {
        return new ExceptionMessageExpectation(null, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    protected void setValidateTestExpectations(Object responseObject, final Object content) {
        setWebResponseExpectation(responseObject);
        mockery.checking(new org.jmock.Expectations() {
            {
                one(htmlunitWebResponse).getContentAsString();
                will(returnValue(content));
            }
        });
    }

}
