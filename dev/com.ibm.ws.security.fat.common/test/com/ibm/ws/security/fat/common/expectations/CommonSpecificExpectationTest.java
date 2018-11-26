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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.Test;

import com.ibm.ws.security.fat.common.test.UnitTestUtils;

import test.common.SharedOutputManager;

public abstract class CommonSpecificExpectationTest extends CommonExpectationTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    protected final com.gargoylesoftware.htmlunit.WebResponse htmlunitWebResponse = mockery.mock(com.gargoylesoftware.htmlunit.WebResponse.class);
    protected final com.gargoylesoftware.htmlunit.html.HtmlPage htmlunitHtmlPage = mockery.mock(com.gargoylesoftware.htmlunit.html.HtmlPage.class);
    protected final com.gargoylesoftware.htmlunit.TextPage htmlunitTextPage = mockery.mock(com.gargoylesoftware.htmlunit.TextPage.class);
    protected final com.gargoylesoftware.htmlunit.xml.XmlPage htmlunitXmlPage = mockery.mock(com.gargoylesoftware.htmlunit.xml.XmlPage.class);

    /**
     * Sublcasses must override this method to return the appropriate object type for the class under test.
     */
    protected abstract Expectation createBasicExpectation();

    /**
     * Sublcasses must override this method to return the appropriate object type for the class under test.
     */
    protected abstract Expectation createBasicExpectationWithNoAction();

    @Test
    public void test_validate_nullContentObject() {
        try {
            Expectation exp = createBasicExpectation();
            runNegativeValidateTestForNullContent(exp);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_nullCurrentTestAction() {
        try {
            Expectation exp = createBasicExpectation();

            String currentTestAction = null;
            Object content = null;
            exp.validate(currentTestAction, content);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_mismatchedCurrentTestAction() {
        try {
            Expectation exp = createBasicExpectation();

            String currentTestAction = "some other action";
            Object content = null;
            exp.validate(currentTestAction, content);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_currentTestActionDifferentCasing() {
        try {
            Expectation exp = createBasicExpectation();

            String currentTestAction = exp.getAction().toUpperCase();
            Object content = null;
            exp.validate(currentTestAction, content);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_currentTestActionSubstring() {
        try {
            Expectation exp = createBasicExpectation();

            String currentTestAction = exp.getAction().substring(1);
            Object content = null;
            exp.validate(currentTestAction, content);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_currentTestActionSuperstring() {
        try {
            Expectation exp = createBasicExpectation();

            String currentTestAction = exp.getAction() + " ";
            Object content = null;
            exp.validate(currentTestAction, content);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** isExpectationForAction **************************************/

    @Test
    public void test_isExpectationForAction_expectationActionNull_testActionNull() {
        try {
            Expectation exp = createBasicExpectationWithNoAction();
            assertTrue("Null expectation test action + null test action should be considered to match this expectation, but it did not. Expectation was " + exp, exp.isExpectationForAction(null));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpectationForAction_expectationActionNull_testActionEmpty() {
        try {
            Expectation exp = createBasicExpectationWithNoAction();
            String testAction = "";
            assertTrue("Null expectation test action + test action (" + testAction + ") should be considered to match this expectation, but it did not. Expectation was " + exp, exp.isExpectationForAction(testAction));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpectationForAction_expectationActionNull_testActionNonEmpty() {
        try {
            Expectation exp = createBasicExpectationWithNoAction();
            String testAction = ACTION1;
            assertTrue("Null expectation test action + test action (" + testAction + ") should be considered to match this expectation, but it did not. Expectation was " + exp, exp.isExpectationForAction(testAction));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpectationForAction_testActionNull() {
        try {
            Expectation exp = createBasicExpectation();
            assertFalse("Null test action should not be considered to match this expectation, but it did. Expectation was " + exp, exp.isExpectationForAction(null));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpectationForAction_testActionEmpty() {
        try {
            Expectation exp = createBasicExpectation();
            String testAction = "";
            assertFalse("Test action (" + testAction + ") should not be considered to match this expectation, but it did. Expectation was " + exp, exp.isExpectationForAction(testAction));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpectationForAction_testActionMismatch() {
        try {
            Expectation exp = createBasicExpectation();
            String testAction = exp.getAction().substring(1);
            assertFalse("Test action (" + testAction + ") should not be considered to match this expectation, but it did. Expectation was " + exp, exp.isExpectationForAction(testAction));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpectationForAction_testActionMatch() {
        try {
            Expectation exp = createBasicExpectation();
            String testAction = exp.getAction();
            assertTrue("Test action (" + testAction + ") should be considered to match this expectation, but did not. Expectation was " + exp, exp.isExpectationForAction(testAction));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    protected void setWebResponseExpectation(Object responseObject) {
        if (responseObject == htmlunitHtmlPage) {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitHtmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                }
            });
        } else if (responseObject == htmlunitTextPage) {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitTextPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                }
            });
        } else if (responseObject == htmlunitXmlPage) {
            mockery.checking(new org.jmock.Expectations() {
                {
                    one(htmlunitXmlPage).getWebResponse();
                    will(returnValue(htmlunitWebResponse));
                }
            });
        }
    }

    protected void runNegativeValidateTestForUnsupportedResponseType(Expectation exp, Object content) throws Exception {
        try {
            exp.validate(content);
            fail("Should have thrown an error because the response object type was not an expected type, but did not.");
        } catch (Throwable e) {
            verifyException(e, String.format(UnitTestUtils.ERR_UNKNOWN_RESPONSE_TYPE, Pattern.quote(content.getClass().getName())));
        }
    }

    protected void runNegativeValidateTestForNullContent(Expectation exp) throws Exception {
        runNegativeValidateTestForNullContent(exp, null);
    }

    protected void runNegativeValidateTestForNullContent(Expectation exp, Object content) throws Exception {
        try {
            exp.validate(content);
            fail("Should have thrown an error because the content type was not an expected type, but did not.");
        } catch (Throwable e) {
            verifyException(e, UnitTestUtils.CONTENT_TO_VALIDATE_NULL);
        }
    }

    protected void runNegativeValidateTestForCheckType_unknown(Expectation exp, Object content) throws Exception {
        try {
            exp.validate(content);
            fail("Should have thrown an exception because of an unknown expectation type but did not.");
        } catch (Throwable e) {
            verifyException(e, String.format(UnitTestUtils.ERR_COMPARISON_TYPE_UNKNOWN, Pattern.quote(exp.getCheckType())));
        }
    }

    protected void runNegativeValidateTestForCheckType_contains(Expectation exp, Object content, String actualContentValue) throws Exception {
        try {
            exp.validate(content);
            fail("Should have thrown an assertion error because the expected string was not found, but did not.");
        } catch (Throwable e) {
            verifyException(e, String.format(UnitTestUtils.ERR_STRING_NOT_FOUND, exp.getValidationValue(), actualContentValue));
        }
    }

    protected void runNegativeValidateTestForCheckType_doesNotContain(Expectation exp, Object content, String actualContentValue) throws Exception {
        try {
            exp.validate(content);
            fail("Should have thrown an assertion error because the string was found when it shouldn't have been, but did not.");
        } catch (Throwable e) {
            verifyException(e, String.format(UnitTestUtils.ERR_STRING_FOUND, exp.getValidationValue(), actualContentValue));
        }
    }

    protected void runNegativeValidateTestForCheckType_matches(Expectation exp, Object content, String actualContentValue) throws Exception {
        try {
            exp.validate(content);
            fail("Should have thrown an assertion error because the expected regex was not found, but did not.");
        } catch (Throwable e) {
            verifyException(e, String.format(UnitTestUtils.ERR_REGEX_NOT_FOUND, Pattern.quote(exp.getValidationValue()), actualContentValue));
        }
    }

    protected void runNegativeValidateTestForCheckType_doesNotMatch(Expectation exp, Object content, String actualContentValue) throws Exception {
        try {
            exp.validate(content);
            fail("Should have thrown an assertion error because the regex was found, but did not.");
        } catch (Throwable e) {
            verifyException(e, String.format(UnitTestUtils.ERR_REGEX_FOUND, Pattern.quote(exp.getValidationValue()), actualContentValue));
        }
    }

}
