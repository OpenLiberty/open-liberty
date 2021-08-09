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

import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.Constants;

import test.common.SharedOutputManager;

public class ResponseTitleExpectationTest extends CommonSpecificExpectationTest {

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

            ResponseTitleExpectation exp = new ResponseTitleExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_TITLE, checkType, null, searchForVal, failureMsg);

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

            ResponseTitleExpectation exp = new ResponseTitleExpectation(testAction, checkType, searchForVal, failureMsg);

            verifyExpectationValues(exp, testAction, Constants.RESPONSE_TITLE, checkType, null, searchForVal, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validate **************************************/

    @Test
    public void test_validate_unknownCheckType() {
        try {
            Expectation exp = new ResponseTitleExpectation(TEST_ACTION, "check type", SEARCH_FOR_VAL, FAILURE_MESSAGE);
            String title = "Title";
            setTitleExpectation(title);

            runNegativeValidateTestForCheckType_unknown(exp, htmlunitHtmlPage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_unsupportedResponseType() {
        try {
            Expectation exp = createBasicExpectation();
            Object content = Boolean.TRUE;

            runNegativeValidateTestForUnsupportedResponseType(exp, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullAction() {
        try {
            Expectation exp = new ResponseTitleExpectation(null, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = "Some " + SEARCH_FOR_VAL + " content";

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullResponseTitle() {
        try {
            Expectation exp = createBasicExpectation();
            setTitleExpectation(null);

            runNegativeValidateTestForNullContent(exp, htmlunitHtmlPage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_htmlunitWebResponse() {
        try {
            Expectation exp = createBasicExpectation();
            try {
                exp.validate(htmlunitWebResponse);
                fail("Should have thrown an exception saying getting the title for this object is not supported, but did not.");
            } catch (Exception e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".+" + "not supported");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_htmlunitTextPage() {
        try {
            Expectation exp = createBasicExpectation();
            runNegativeValidateTestForResponseTypeWithoutTitle(exp, htmlunitTextPage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_htmlunitXmlPage() {
        try {
            Expectation exp = createBasicExpectation();
            runNegativeValidateTestForResponseTypeWithoutTitle(exp, htmlunitXmlPage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_fails() {
        try {
            Expectation exp = new ResponseTitleExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            String title = "missing search value";
            setTitleExpectation(title);

            runNegativeValidateTestForCheckType_contains(exp, htmlunitHtmlPage, title);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeContains_passes() {
        try {
            Expectation exp = new ResponseTitleExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            String title = "title includes " + SEARCH_FOR_VAL + " in it";
            setTitleExpectation(title);

            exp.validate(htmlunitHtmlPage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_fails() {
        try {
            Expectation exp = new ResponseTitleExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            String title = "title includes " + SEARCH_FOR_VAL + " in it";
            setTitleExpectation(title);

            runNegativeValidateTestForCheckType_doesNotContain(exp, htmlunitHtmlPage, title);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotContain_passes() {
        try {
            Expectation exp = new ResponseTitleExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_CONTAIN, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object title = "title missing search val";

            exp.validate(title);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_fails() {
        try {
            Expectation exp = new ResponseTitleExpectation(TEST_ACTION, Constants.STRING_MATCHES, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object title = "title missing search val";

            runNegativeValidateTestForCheckType_matches(exp, title, title.toString());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_matchSpansMultipleLines() {
        try {
            String searchForRegex = "line1.+line2";
            Expectation exp = new ResponseTitleExpectation(TEST_ACTION, Constants.STRING_MATCHES, searchForRegex, FAILURE_MESSAGE);
            String title = "line0 \n\r line1 \n line2 \r line3 \r\n line4";
            setTitleExpectation(title);

            runNegativeValidateTestForCheckType_matches(exp, htmlunitHtmlPage, title);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeMatches_passes() {
        try {
            String searchForRegex = "line1.+line2";
            Expectation exp = new ResponseTitleExpectation(TEST_ACTION, Constants.STRING_MATCHES, searchForRegex, FAILURE_MESSAGE);
            String title = "line0 line1 \t line2 line3 \r\n line4";
            setTitleExpectation(title);

            exp.validate(htmlunitHtmlPage);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_fails() {
        try {
            Expectation exp = new ResponseTitleExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, "[a-zA-Z]+", FAILURE_MESSAGE);
            String title = "Hello, world!";
            setTitleExpectation(title);

            runNegativeValidateTestForCheckType_doesNotMatch(exp, htmlunitHtmlPage, title);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_checkTypeDoesNotMatch_passes() {
        try {
            Expectation exp = new ResponseTitleExpectation(TEST_ACTION, Constants.STRING_DOES_NOT_MATCH, "[0-9]+", FAILURE_MESSAGE);
            Object content = "Hello, world!";

            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    @Override
    protected Expectation createBasicExpectation() {
        return new ResponseTitleExpectation(TEST_ACTION, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    @Override
    protected Expectation createBasicExpectationWithNoAction() {
        return new ResponseTitleExpectation(null, Constants.STRING_CONTAINS, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    private void runNegativeValidateTestForResponseTypeWithoutTitle(Expectation exp, Object response) throws Exception {
        try {
            exp.validate(response);
            fail("Should have thrown an exception saying this response object cannot return a title, but did not.");
        } catch (Throwable e) {
            verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".+" + Pattern.quote(response.getClass().getName()) + ".+no title");
        }
    }

    private void setTitleExpectation(final String title) {
        mockery.checking(new org.jmock.Expectations() {
            {
                one(htmlunitHtmlPage).getTitleText();
                will(returnValue(title));
            }
        });
    }

}
