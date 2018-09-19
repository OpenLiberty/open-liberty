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
package com.ibm.ws.security.fat.common.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseHeaderExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseTitleExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.test.UnitTestUtils;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class TestValidationUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private final String action = "some test action";
    private final String checkType = "check type";
    private final String searchFor = "search for me";
    private final String failureMsg = "This is a failure message.";
    private final String exceptionMsg = "Some exception happened";

    TestValidationUtils utils = new TestValidationUtils();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new TestValidationUtils();
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

    /************************************** assertTrueAndLog **************************************/

    /**
     * Tests:
     * - Failure message: null
     * - Test assertion: false
     * Expects:
     * - AssertionError should be thrown with a null message
     */
    @Test
    public void test_assertTrueAndLog_nullMessage_testAssertionFalse() {
        try {
            String caller = null;
            String failureMsg = null;
            boolean testAssertion = false;
            try {
                utils.assertTrueAndLog(caller, failureMsg, testAssertion);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                assertNull("Error message should have been null but was [" + e.getMessage() + "].", e.getMessage());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Empty string
     * - Test assertion: false
     * Expects:
     * - AssertionError should be thrown with an empty message
     */
    @Test
    public void test_assertTrueAndLog_emptyMessage_testAssertionFalse() {
        try {
            String caller = null;
            String failureMsg = "";
            boolean testAssertion = false;
            try {
                utils.assertTrueAndLog(caller, failureMsg, testAssertion);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                assertEquals("Error message did not match expected value.", failureMsg, e.getMessage());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Non-empty
     * - Test assertion: false
     * Expects:
     * - AssertionError should be thrown with a message matching the failure message
     */
    @Test
    public void test_assertTrueAndLog_nonEmptyMessage_testAssertionFalse() {
        try {
            String caller = null;
            String failureMsg = "This is a failure message.";
            boolean testAssertion = false;
            try {
                utils.assertTrueAndLog(caller, failureMsg, testAssertion);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                assertEquals("Error message did not match expected value.", failureMsg, e.getMessage());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: null
     * - Test assertion: true
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_assertTrueAndLog_nullMessage_testAssertionTrue() {
        try {
            String caller = null;
            String failureMsg = null;
            boolean testAssertion = true;
            utils.assertTrueAndLog(caller, failureMsg, testAssertion);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Empty string
     * - Test assertion: true
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_assertTrueAndLog_emptyMessage_testAssertionTrue() {
        try {
            String caller = null;
            String failureMsg = "";
            boolean testAssertion = true;
            utils.assertTrueAndLog(caller, failureMsg, testAssertion);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Non-empty
     * - Test assertion: true
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_assertTrueAndLog_nonEmptyMessage_testAssertionTrue() {
        try {
            String caller = testName.getMethodName();
            String failureMsg = "This is a failure message.";
            boolean testAssertion = true;
            utils.assertTrueAndLog(caller, failureMsg, testAssertion);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** assertAndLog **************************************/

    /**
     * Tests:
     * - Failure message: Empty string
     * - Test assertion: false
     * - Expected result: false
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_assertAndLog_emptyMessage_testAssertionFalse_expectFalse() {
        try {
            String caller = testName.getMethodName();
            String failureMsg = "";
            boolean testAssertion = false;
            boolean expectedResult = false;
            utils.assertAndLog(caller, failureMsg, testAssertion, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Empty string
     * - Test assertion: false
     * - Expected result: true
     * Expects:
     * - AssertionError should be thrown with a message matching the failure message
     */
    @Test
    public void test_assertAndLog_emptyMessage_testAssertionFalse_expectTrue() {
        try {
            String caller = testName.getMethodName();
            String failureMsg = "";
            boolean testAssertion = false;
            boolean expectedResult = true;
            runNegativeAssertAndLogTest(caller, failureMsg, testAssertion, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Non-empty
     * - Test assertion: false
     * - Expected result: false
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_assertAndLog_nonEmptyMessage_testAssertionFalse_expectFalse() {
        try {
            String caller = testName.getMethodName();
            String failureMsg = "This is a failure message.";
            boolean testAssertion = false;
            boolean expectedResult = false;
            utils.assertAndLog(caller, failureMsg, testAssertion, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Non-empty
     * - Test assertion: false
     * - Expected result: true
     * Expects:
     * - AssertionError should be thrown with a message matching the failure message
     */
    @Test
    public void test_assertAndLog_nonEmptyMessage_testAssertionFalse_expectTrue() {
        try {
            String caller = testName.getMethodName();
            String failureMsg = "This is a failure message.";
            boolean testAssertion = false;
            boolean expectedResult = true;
            runNegativeAssertAndLogTest(caller, failureMsg, testAssertion, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Empty string
     * - Test assertion: true
     * - Expected result: false
     * Expects:
     * - AssertionError should be thrown with a message matching the failure message
     */
    @Test
    public void test_assertAndLog_emptyMessage_testAssertionTrue_expectFalse() {
        try {
            String caller = testName.getMethodName();
            String failureMsg = "";
            boolean testAssertion = true;
            boolean expectedResult = false;
            runNegativeAssertAndLogTest(caller, failureMsg, testAssertion, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Empty string
     * - Test assertion: true
     * - Expected result: true
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_assertAndLog_emptyMessage_testAssertionTrue_expectTrue() {
        try {
            String caller = testName.getMethodName();
            String failureMsg = "";
            boolean testAssertion = true;
            boolean expectedResult = true;
            utils.assertAndLog(caller, failureMsg, testAssertion, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Non-empty
     * - Test assertion: true
     * - Expected result: false
     * Expects:
     * - AssertionError should be thrown with a message matching the failure message
     */
    @Test
    public void test_assertAndLog_nonEmptyMessage_testAssertionTrue_expectFalse() {
        try {
            String caller = testName.getMethodName();
            String failureMsg = "This is a failure message.";
            boolean testAssertion = true;
            boolean expectedResult = false;
            runNegativeAssertAndLogTest(caller, failureMsg, testAssertion, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Failure message: Non-empty
     * - Test assertion: true
     * - Expected result: true
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_assertAndLog_nonEmptyMessage_testAssertionTrue_expectTrue() {
        try {
            String caller = testName.getMethodName();
            String failureMsg = "This is a failure message.";
            boolean testAssertion = true;
            boolean expectedResult = true;
            utils.assertAndLog(caller, failureMsg, testAssertion, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateResult **************************************/

    /**
     * Tests:
     * - Provided action is null
     * - Provided Expectation object is null
     * Expects:
     * - Message saying the expectations are null should be logged; nothing else should happen
     */
    @Test
    public void test_validateResult_noTestAction_nullExpectations() {
        try {
            Object contentToValidate = null;
            Expectations expectations = null;

            utils.validateResult(contentToValidate, expectations);

            assertRegexInTrace(outputMgr, "Expectations.+null");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided action is null
     * - Provided Expectation object is empty
     * Expects:
     * - Nothing should happen
     */
    @Test
    public void test_validateResult_noTestAction_emptyExpectations() {
        try {
            Object contentToValidate = null;
            Expectations expectations = new Expectations();

            utils.validateResult(contentToValidate, expectations);

            assertStringNotInTrace(outputMgr, "Error");
            assertRegexNotInTrace(outputMgr, "Expectations.+null");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided action is null
     * - One expectation provided
     * - Expectation validation fails
     * Expects:
     * - Validation error should be logged and exception should be re-thrown
     */
    @Test
    public void test_validateResult_noTestAction_oneExpectation_validationFails() {
        try {
            Object contentToValidate = null;
            Expectations expectations = new Expectations();
            expectations.addSuccessStatusCodesForActions(new String[] { null });

            try {
                utils.validateResult(contentToValidate, expectations);
                fail("Should have thrown an exception because of a validation failing for an expectation, but did not. Expectations were: " + expectations.getExpectations());
            } catch (Throwable e) {
                verifyException(e, String.format(UnitTestUtils.ERR_STRING_NOT_FOUND, "200", "-1"));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided action is null
     * - One expectation provided
     * - Expectation validation fails
     * Expects:
     * - Validation error should be logged and exception should be re-thrown
     */
    @Test
    public void test_validateResult_noTestAction_oneExpectation_validationSucceeds() {
        try {
            Object contentToValidate = "This is some response content";
            Expectations expectations = new Expectations();
            expectations.addExpectation(Expectation.createResponseExpectation(null, "response content", "Should have found \"response content\" in full response."));

            utils.validateResult(contentToValidate, expectations);

            assertStringNotInTrace(outputMgr, "Error");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided action is null
     * - One expectation provided with a non-null action
     * Expects:
     * - Validation should be skipped since the expectation has an associated action and the action being checked against is null
     */
    @Test
    public void test_validateResult_noTestAction_oneExpectation_nonNullExpectationAction() {
        try {
            Object contentToValidate = null;
            Expectations expectations = new Expectations();
            expectations.addSuccessStatusCodesForActions(new String[] { action });

            utils.validateResult(contentToValidate, expectations);

            assertStringNotInTrace(outputMgr, "Error");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided action is null
     * - Multiple expectations provided for various test actions
     * - Expectations for the action being validated all pass, other expectations for other actions would fail
     * Expects:
     * - Validation should succeed and no errors should be logged
     */
    @Test
    public void test_validateResult_noTestAction_multipleExpectationsForDifferentActions_allPassValidationForGivenStep() {
        try {
            Object contentToValidate = "This is some response content";
            Expectations expectations = new Expectations();
            expectations.addExpectation(Expectation.createResponseExpectation(null, "This", "Should have found \"This\" in full response."));
            expectations.addExpectation(Expectation.createResponseExpectation(null, "some", "Should have found \"some\" in full response."));
            expectations.addExpectation(Expectation.createResponseExpectation("some other action", "DO NOT FIND", failureMsg));
            expectations.addExpectation(Expectation.createResponseExpectation(action, "response content", "Did not find expected string in full response."));
            expectations.addExpectation(new ResponseStatusExpectation("yet another action", 403));
            expectations.addExpectation(Expectation.createResponseExpectation(null, "content", "Should have found \"content\" in full response."));

            utils.validateResult(contentToValidate, expectations);

            assertStringNotInTrace(outputMgr, "Error");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided Expectation object is null
     * Expects:
     * - Message saying the expectations are null should be logged; nothing else should happen
     */
    @Test
    public void test_validateResult_nullExpectations() {
        try {
            Object contentToValidate = null;
            Expectations expectations = null;

            utils.validateResult(contentToValidate, action, expectations);

            assertRegexInTrace(outputMgr, "Expectations.+null");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided Expectation object is empty
     * Expects:
     * - Nothing should happen
     */
    @Test
    public void test_validateResult_emptyExpectations() {
        try {
            Object contentToValidate = null;
            Expectations expectations = new Expectations();

            utils.validateResult(contentToValidate, action, expectations);

            assertStringNotInTrace(outputMgr, "Error");
            assertRegexNotInTrace(outputMgr, "Expectations.+null");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One expectation provided
     * - Action for expectation does not match the action provided to the validateResult method
     * Expects:
     * - Nothing should happen
     */
    @Test
    public void test_validateResult_oneExpectation_doesNotMatchAction() {
        try {
            Object contentToValidate = null;
            Expectations expectations = new Expectations();
            expectations.addSuccessStatusCodesForActions(new String[] { "expectation action does not match" });

            utils.validateResult(contentToValidate, action, expectations);

            assertStringNotInTrace(outputMgr, "Error");
            assertRegexNotInTrace(outputMgr, "Expectations.+null");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One expectation provided
     * - Expectation validation fails
     * Expects:
     * - Validation error should be logged and exception should be re-thrown
     */
    @Test
    public void test_validateResult_oneExpectation_expectationFailsValidation() {
        try {
            Object contentToValidate = null;
            Expectations expectations = new Expectations();
            expectations.addSuccessStatusCodesForActions(new String[] { action });

            try {
                utils.validateResult(contentToValidate, action, expectations);
                fail("Should have thrown an exception because of a validation failing for an expectation, but did not.");
            } catch (Throwable e) {
                verifyException(e, String.format(UnitTestUtils.ERR_STRING_NOT_FOUND, "200", "-1"));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple expectations provided for various test actions
     * - Expectations for the action being validated all pass, other expectations for other actions would fail
     * Expects:
     * - Validation should succeed and no errors should be logged
     */
    @Test
    public void test_validateResult_multipleExpectationsForDifferentActions_allPassValidationForGivenStep() {
        try {
            Object contentToValidate = "This is some response content";
            Expectations expectations = new Expectations();
            expectations.addExpectation(Expectation.createResponseExpectation(action, "some", "Should have found \"some\" in full response."));
            expectations.addExpectation(Expectation.createResponseExpectation("some other action", "DO NOT FIND", failureMsg));
            expectations.addExpectation(Expectation.createResponseExpectation(action, "response content", "Did not find expected string in full response."));
            expectations.addExpectation(new ResponseStatusExpectation("yet another action", 403));

            utils.validateResult(contentToValidate, action, expectations);

            assertStringNotInTrace(outputMgr, "Error");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple expectations provided for various test actions
     * - One expectation will fail for the specified test action
     * Expects:
     * - Validation should stop at the first expectation that fails validation and an error should be logged
     */
    @Test
    public void test_validateResult_multipleExpectations_oneFails() {
        try {
            Object contentToValidate = "This is some response content";
            Expectations expectations = new Expectations();
            expectations.addExpectation(Expectation.createResponseExpectation(action, "some", "Should have found \"some\" in full response."));
            expectations.addExpectation(Expectation.createResponseExpectation(action, "DO NOT FIND", failureMsg));
            expectations.addExpectation(Expectation.createResponseExpectation("another action", "response content", "Did not find expected string in full response."));
            expectations.addExpectation(new ResponseStatusExpectation(action, 403));

            try {
                utils.validateResult(contentToValidate, action, expectations);
                fail("Should have thrown an exception because of a validation failing for an expectation, but did not.");
            } catch (Throwable e) {
                verifyException(e, String.format(UnitTestUtils.ERR_STRING_NOT_FOUND, "DO NOT FIND", contentToValidate.toString()));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateResult **************************************/

    /**
     * Tests:
     * - Provided Expectation object is null
     * Expects:
     * - Message saying the expectations are null should be logged; nothing else should happen
     */
    @Test
    public void test_validateException_nullExpectations() {
        try {
            Exception exceptionToValidate = new Exception(exceptionMsg);
            Expectations expectations = null;

            utils.validateException(exceptionToValidate, action, expectations);

            assertRegexInTrace(outputMgr, "Expectations.+null");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Provided Expectation object is empty
     * Expects:
     * - Nothing should happen
     */
    @Test
    public void test_validateException_emptyExpectations() {
        try {
            Exception exceptionToValidate = new Exception(exceptionMsg);
            Expectations expectations = new Expectations();

            utils.validateResult(exceptionToValidate, action, expectations);

            assertStringNotInTrace(outputMgr, "Error");
            assertRegexNotInTrace(outputMgr, "Expectations.+null");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One expectation provided
     * - Action for expectation does not match the action provided to the validateResult method
     * Expects:
     * - Nothing should happen
     */
    @Test
    public void test_validateException_oneExpectation_doesNotMatchAction() {
        try {
            Exception exceptionToValidate = new Exception(exceptionMsg);
            Expectations expectations = new Expectations();
            expectations.addExpectation(Expectation.createExceptionExpectation("expectation action does not match", searchFor, failureMsg));

            utils.validateResult(exceptionToValidate, action, expectations);

            assertStringNotInTrace(outputMgr, "Error");
            assertRegexNotInTrace(outputMgr, "Expectations.+null");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - One expectation provided
     * - Expectation validation fails
     * Expects:
     * - Validation error should be logged and exception should be re-thrown
     */
    @Test
    public void test_validateException_oneExpectation_expectationFailsValidation() {
        try {
            Exception exceptionToValidate = new Exception(exceptionMsg);
            Expectations expectations = new Expectations();
            expectations.addExpectation(Expectation.createExceptionExpectation(action, searchFor, failureMsg));

            try {
                utils.validateException(exceptionToValidate, action, expectations);
                fail("Should have thrown an exception because of a validation failing for an expectation, but did not.");
            } catch (Throwable e) {
                verifyException(e, String.format(UnitTestUtils.ERR_STRING_NOT_FOUND, searchFor, exceptionMsg));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple expectations provided for various test actions
     * - Expectations for the action being validated all pass, other expectations for other actions would fail
     * Expects:
     * - Validation should succeed and no errors should be logged
     */
    @Test
    public void test_validateException_multipleExpectationsForDifferentActions_allPassValidationForGivenStep() {
        try {
            Exception exceptionToValidate = new Exception(exceptionMsg);
            Expectations expectations = new Expectations();
            expectations.addExpectation(Expectation.createExceptionExpectation(action, "Some", "Should have found \"Some\" in the exception."));
            expectations.addExpectation(Expectation.createExceptionExpectation("some other action", "DO NOT FIND", failureMsg));
            expectations.addExpectation(Expectation.createExceptionExpectation(action, "happened", "Did not find expected string in full response."));
            expectations.addExpectation(new ResponseStatusExpectation("yet another action", 403));

            utils.validateResult(exceptionToValidate, action, expectations);

            assertStringNotInTrace(outputMgr, "Error");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Multiple expectations provided for various test actions
     * - One expectation will fail for the specified test action
     * Expects:
     * - Validation should stop at the first expectation that fails validation and an error should be logged
     */
    @Test
    public void test_validateException_multipleExpectations_oneFails() {
        try {
            Exception exceptionToValidate = new Exception(exceptionMsg);
            Expectations expectations = new Expectations();
            expectations.addExpectation(Expectation.createExceptionExpectation(action, "Some", "Should have found \"Some\" in the exception."));
            expectations.addExpectation(Expectation.createExceptionExpectation(action, "DO NOT FIND", failureMsg));
            expectations.addExpectation(Expectation.createExceptionExpectation("another action", "happened", "Did not find expected string in full response."));
            expectations.addExpectation(new ResponseStatusExpectation("yet another action", 403));

            try {
                utils.validateResult(exceptionToValidate, action, expectations);
                fail("Should have thrown an exception because of a validation failing for an expectation, but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_NOT_FOUND, "DO NOT FIND", exceptionMsg));
            }

            // Shouldn't get far enough to fail for the response status check (that would have failed had we gotten there)
            assertStringNotInTrace(outputMgr, "Failed to validate response status");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateStringContent **************************************/

    /**
     * Tests:
     * - Provided Expectation object is null
     * Expects:
     * - Exception should be thrown due to the provided Exception object being null
     */
    @Test
    public void test_validateStringContent_nullExpectation() {
        try {
            String contentToValidate = "some content";
            try {
                utils.validateStringContent(null, contentToValidate);
                fail("Should have thrown an exception but did not.");
            } catch (Exception e) {
                verifyException(e, "expectation is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: null
     * Expects:
     * - Exception should be thrown due to an unknown check type
     */
    @Test
    public void test_validateStringContent_nullCheckType() {
        try {
            Expectation expectation = new ResponseStatusExpectation(action, null, searchFor, failureMsg);
            String contentToValidate = "some content";
            try {
                utils.validateStringContent(expectation, contentToValidate);
                fail("Should have thrown an exception but did not.");
            } catch (Throwable e) {
                verifyException(e, String.format(UnitTestUtils.ERR_COMPARISON_TYPE_UNKNOWN, "null"));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Unknown
     * Expects:
     * - Exception should be thrown due to an unknown check type
     */
    @Test
    public void test_validateStringContent_unknownCheckType() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "some content";
            try {
                utils.validateStringContent(expectation, contentToValidate);
                fail("Should have thrown an exception but did not.");
            } catch (Throwable e) {
                verifyException(e, String.format(UnitTestUtils.ERR_COMPARISON_TYPE_UNKNOWN, Pattern.quote(checkType)));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Null
     * - Content to validate: Not null
     * Expects:
     * - AssertionError should be thrown saying the value was expected to be null but was not
     */
    @Test
    public void test_validateStringContent_stringNull_fails() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, Constants.STRING_NULL, searchFor, failureMsg);
            String contentToValidate = "some content";

            String expectedFailureMsg = Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_NOT_NULL, contentToValidate);
            runNegativeValidateStringContentTest(expectation, contentToValidate, expectedFailureMsg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Null
     * - Content to validate: Null
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContent_stringNull_succeeds() {
        try {
            Expectation expectation = new ResponseFullExpectation(action, Constants.STRING_NULL, searchFor, failureMsg);
            String contentToValidate = null;
            utils.validateStringContent(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Not null
     * - Content to validate: Null
     * Expects:
     * - AssertionError should be thrown saying the value was expected not to be null but was
     */
    @Test
    public void test_validateStringContent_stringNotNull_fails() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, Constants.STRING_NOT_NULL, searchFor, failureMsg);
            String contentToValidate = null;

            String expectedFailureMsg = Pattern.quote(failureMsg) + UnitTestUtils.ERR_STRING_NULL;
            runNegativeValidateStringContentTest(expectation, contentToValidate, expectedFailureMsg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Not null
     * - Content to validate: Not null
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContent_stringNotNull_succeeds() {
        try {
            Expectation expectation = new ResponseFullExpectation(action, Constants.STRING_NOT_NULL, searchFor, failureMsg);
            String contentToValidate = "some content";
            utils.validateStringContent(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Equals
     * - Content to validate: Does not equal search value
     * Expects:
     * - AssertionError should be thrown saying the value did not equal the expected value
     */
    @Test
    public void test_validateStringContent_stringEquals_fails() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, Constants.STRING_EQUALS, searchFor, failureMsg);
            String contentToValidate = "some " + searchFor + " content";

            String expectedFailureMsg = Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_DOES_NOT_EQUAL, searchFor, contentToValidate);
            runNegativeValidateStringContentTest(expectation, contentToValidate, expectedFailureMsg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Equals
     * - Content to validate: Equals search value
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContent_stringEquals_succeeds() {
        try {
            Expectation expectation = new ResponseFullExpectation(action, Constants.STRING_EQUALS, searchFor, failureMsg);
            String contentToValidate = searchFor;
            utils.validateStringContent(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Contains
     * - Content to validate: Does not contain expected string
     * Expects:
     * - AssertionError should be thrown saying the string to search for was not found
     */
    @Test
    public void test_validateStringContent_stringContains_fails() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, Constants.STRING_CONTAINS, searchFor, failureMsg);
            String contentToValidate = "some content";

            String expectedFailureMsg = Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_NOT_FOUND, searchFor, contentToValidate);
            runNegativeValidateStringContentTest(expectation, contentToValidate, expectedFailureMsg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Contains
     * - Content to validate: Contains expected string
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContent_stringContains_succeeds() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, Constants.STRING_CONTAINS, searchFor, failureMsg);
            String contentToValidate = "some " + searchFor + " content";
            utils.validateStringContent(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Does not contain
     * - Content to validate: Contains expected string
     * Expects:
     * - AssertionError should be thrown saying the string to search for was found but should not have been
     */
    @Test
    public void test_validateStringContent_stringDoesNotContain_fails() {
        try {
            Expectation expectation = new ResponseFullExpectation(action, Constants.STRING_DOES_NOT_CONTAIN, searchFor, failureMsg);
            String contentToValidate = "some " + searchFor + " content";

            String expectedFailureMsg = Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_FOUND, searchFor, contentToValidate);
            runNegativeValidateStringContentTest(expectation, contentToValidate, expectedFailureMsg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Does not contain
     * - Content to validate: Does not contain expected string
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContent_stringDoesNotContain_succeeds() {
        try {
            Expectation expectation = new ResponseFullExpectation(action, Constants.STRING_DOES_NOT_CONTAIN, searchFor, failureMsg);
            String contentToValidate = "some content";
            utils.validateStringContent(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Matches
     * - Content to validate: Does not contain expected regex
     * Expects:
     * - AssertionError should be thrown saying the regex to search for was not found
     */
    @Test
    public void test_validateStringContent_stringMatches_fails() {
        try {
            Expectation expectation = new ResponseHeaderExpectation(action, Constants.STRING_MATCHES, searchFor, failureMsg);
            String contentToValidate = "some content";

            String expectedFailureMsg = Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_REGEX_NOT_FOUND, Pattern.quote(searchFor), contentToValidate);
            runNegativeValidateStringContentTest(expectation, contentToValidate, expectedFailureMsg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Matches
     * - Content to validate: Contains expected regex
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContent_stringMatches_succeeds() {
        try {
            Expectation expectation = new ResponseHeaderExpectation(action, Constants.STRING_MATCHES, searchFor, failureMsg);
            String contentToValidate = "some " + searchFor + " content";
            utils.validateStringContent(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Does not match
     * - Content to validate: Contains expected regex
     * Expects:
     * - AssertionError should be thrown saying the regex to search for was found but should not have been
     */
    @Test
    public void test_validateStringContent_stringDoesNotMatch_fails() {
        try {
            Expectation expectation = new ResponseMessageExpectation(action, Constants.STRING_DOES_NOT_MATCH, searchFor, failureMsg);
            String contentToValidate = "some " + searchFor + " content";

            String expectedFailureMsg = Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_REGEX_FOUND, searchFor, contentToValidate);
            runNegativeValidateStringContentTest(expectation, contentToValidate, expectedFailureMsg);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expectation check type: Does not match
     * - Content to validate: Does not contain expected regex
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContent_stringDoesNotMatch_succeeds() {
        try {
            Expectation expectation = new ResponseMessageExpectation(action, Constants.STRING_DOES_NOT_MATCH, searchFor, failureMsg);
            String contentToValidate = "some content";
            utils.validateStringContent(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateStringNull **************************************/

    /**
     * Tests:
     * - String to search for: null
     * - Content to validate: null
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringNull_nullSearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseStatusExpectation(action, checkType, null, failureMsg);
            String contentToValidate = null;
            utils.validateStringNull(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: null
     * - Content to validate: Not null
     * Expects:
     * - AssertionError should be thrown saying the content to validate is not null
     */
    @Test
    public void test_validateStringNull_nullSearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, checkType, null, failureMsg);
            String contentToValidate = "some content";
            try {
                utils.validateStringNull(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_NOT_NULL, contentToValidate));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: null
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringNull_nonEmptySearchString_nullContentToValidate() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = null;
            utils.validateStringNull(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Empty string
     * Expects:
     * - AssertionError should be thrown saying the content to validate is not null
     */
    @Test
    public void test_validateStringNull_nonEmptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "";
            try {
                utils.validateStringNull(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_NOT_NULL, contentToValidate));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateStringNotNull **************************************/

    /**
     * Tests:
     * - String to search for: null
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the content to validate is null but shouldn't be
     */
    @Test
    public void test_validateStringNotNull_nullSearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseUrlExpectation(action, checkType, null, failureMsg);
            String contentToValidate = null;
            try {
                utils.validateStringNotNull(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), Pattern.quote(failureMsg) + UnitTestUtils.ERR_STRING_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: null
     * - Content to validate: Not null
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringNotNull_nullSearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseFullExpectation(action, checkType, null, failureMsg);
            String contentToValidate = "some content";
            utils.validateStringNotNull(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the content to validate is null but shouldn't be
     */
    @Test
    public void test_validateStringNotNull_nonEmptySearchString_nullContentToValidate() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = null;
            try {
                utils.validateStringNotNull(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), Pattern.quote(failureMsg) + UnitTestUtils.ERR_STRING_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Empty string
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringNotNull_nonEmptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "";
            utils.validateStringNotNull(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateStringEquals **************************************/

    /**
     * Tests:
     * - String to search for: null
     * - Content to validate: null
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringEquals_nullSearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseHeaderExpectation(action, checkType, null, failureMsg);
            String contentToValidate = null;
            utils.validateStringEquals(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: null
     * - Content to validate: Non-empty
     * Expects:
     * - AssertionError should be thrown saying the content to validate was not null
     */
    @Test
    public void test_validateStringEquals_nullSearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseMessageExpectation(action, checkType, null, failureMsg);
            String contentToValidate = "some content";
            try {
                utils.validateStringEquals(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_NOT_NULL, contentToValidate));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the content to validate was null
     */
    @Test
    public void test_validateStringEquals_emptySearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseStatusExpectation(action, checkType, "", failureMsg);
            String contentToValidate = null;
            try {
                utils.validateStringEquals(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.CONTENT_TO_VALIDATE_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: Empty string
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringEquals_emptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, checkType, "", failureMsg);
            String contentToValidate = "";
            utils.validateStringEquals(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: Non-empty
     * Expects:
     * - AssertionError should be thrown saying the content to validate does not equal the search value
     */
    @Test
    public void test_validateStringEquals_emptySearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseUrlExpectation(action, checkType, "", failureMsg);
            String contentToValidate = "some content";
            try {
                utils.validateStringEquals(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_DOES_NOT_EQUAL, expectation.getValidationValue(), contentToValidate));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Empty string
     * Expects:
     * - AssertionError should be thrown saying the content to validate does not equal the search value
     */
    @Test
    public void test_validateStringEquals_nonEmptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "";
            try {
                utils.validateStringEquals(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_DOES_NOT_EQUAL, expectation.getValidationValue(), contentToValidate));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Non-empty, includes string to search for
     * Expects:
     * - AssertionError should be thrown saying the content to validate does not equal the search value
     */
    @Test
    public void test_validateStringEquals_nonEmptySearchString_contentIncludesString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = expectation.getValidationValue() + " and more";
            try {
                utils.validateStringEquals(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), Pattern.quote(failureMsg) + String.format(UnitTestUtils.ERR_STRING_DOES_NOT_EQUAL, expectation.getValidationValue(), contentToValidate));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Non-empty, equal to string to search for
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringEquals_nonEmptySearchString_contentEqualsSearchString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = expectation.getValidationValue();
            utils.validateStringEquals(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateStringContains **************************************/

    /**
     * Tests:
     * - String to search for: null
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the string to search for is null
     */
    @Test
    public void test_validateStringContains_nullSearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseStatusExpectation(action, checkType, null, failureMsg);
            String contentToValidate = null;
            try {
                utils.validateStringContains(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.STRING_TO_SEARCH_FOR_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Search string: null
     * - Content to validate: Non-empty
     * Expects:
     * - AssertionError should be thrown saying the string to search for was null
     */
    @Test
    public void test_validateStringContains_nullSearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, checkType, null, failureMsg);
            String contentToValidate = "does \n\r not \t include <br/> expected value";
            try {
                utils.validateStringContains(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.STRING_TO_SEARCH_FOR_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the content to validate was null
     */
    @Test
    public void test_validateStringContains_emptySearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseUrlExpectation(action, checkType, "", failureMsg);
            String contentToValidate = null;
            try {
                utils.validateStringContains(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.CONTENT_TO_VALIDATE_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: Empty string
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContains_emptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = new ResponseFullExpectation(action, checkType, "", failureMsg);
            String contentToValidate = "";
            utils.validateStringContains(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: Non-empty
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContains_emptySearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseHeaderExpectation(action, checkType, "", failureMsg);
            String contentToValidate = "some non-empty content";
            utils.validateStringContains(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Empty string
     * Expects:
     * - AssertionError should be thrown saying the expected string was not found
     */
    @Test
    public void test_validateStringContains_nonEmptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "";
            runNegativeValidateStringContainsTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Missing string to search for
     * Expects:
     * - AssertionError should be thrown saying the content to validate was null
     */
    @Test
    public void test_validateStringContains_nonEmptySearchString_contentMissingString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "does \n\r not \t include <br/> expected value";
            runNegativeValidateStringContainsTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Equal to string to search for
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContains_nonEmptySearchString_contentEqualsSearchString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = searchFor;
            utils.validateStringContains(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Includes string to search for
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringContains_nonEmptySearchString_contentIncludesString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "does \n\r abc" + searchFor + "def \t include <br/> expected value";
            utils.validateStringContains(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateStringDoesNotContain **************************************/

    /**
     * Tests:
     * - String to search for: null
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the string to search for is null
     */
    @Test
    public void test_validateStringDoesNotContain_nullSearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseMessageExpectation(action, checkType, null, failureMsg);
            String contentToValidate = null;
            try {
                utils.validateStringDoesNotContain(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.STRING_TO_SEARCH_FOR_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Search string: null
     * - Content to validate: Non-empty
     * Expects:
     * - AssertionError should be thrown saying the string to search for was null
     */
    @Test
    public void test_validateStringDoesNotContain_nullSearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseStatusExpectation(action, checkType, null, failureMsg);
            String contentToValidate = "some non-empty value";
            try {
                utils.validateStringDoesNotContain(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.STRING_TO_SEARCH_FOR_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the content to validate was null
     */
    @Test
    public void test_validateStringDoesNotContain_emptySearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, checkType, "", failureMsg);
            String contentToValidate = null;
            try {
                utils.validateStringDoesNotContain(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.CONTENT_TO_VALIDATE_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: Empty string
     * Expects:
     * - AssertionError should be thrown saying the string was found when it shouldn't have been
     */
    @Test
    public void test_validateStringDoesNotContain_emptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = new ResponseUrlExpectation(action, checkType, "", failureMsg);
            String contentToValidate = "";
            runNegativeValidateStringDoesNotContainTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: Non-empty
     * Expects:
     * - AssertionError should be thrown saying the string was found when it shouldn't have been
     */
    @Test
    public void test_validateStringDoesNotContain_emptySearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseFullExpectation(action, checkType, "", failureMsg);
            String contentToValidate = "this string is not empty";
            runNegativeValidateStringDoesNotContainTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Empty string
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringDoesNotContain_nonEmptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "";
            utils.validateStringDoesNotContain(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Missing string to search for
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateStringDoesNotContain_nonEmptySearchString_contentMissingString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "does \n\r not \t include <br/> expected value";
            utils.validateStringDoesNotContain(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Equal to string to search for
     * Expects:
     * - AssertionError should be thrown saying the string was found when it shouldn't have been
     */
    @Test
    public void test_validateStringDoesNotContain_nonEmptySearchString_contentEqualsSearchString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = searchFor;
            runNegativeValidateStringDoesNotContainTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Includes string to search for
     * Expects:
     * - AssertionError should be thrown saying the string was found when it shouldn't have been
     */
    @Test
    public void test_validateStringDoesNotContain_nonEmptySearchString_contentIncludesString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "does \n\r abc" + searchFor + "def \t include <br/> expected value";
            runNegativeValidateStringDoesNotContainTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateRegexFound **************************************/

    /**
     * Tests:
     * - String to search for: null
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the string to search for is null
     */
    @Test
    public void test_validateRegexFound_nullSearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseHeaderExpectation(action, checkType, null, failureMsg);
            String contentToValidate = null;
            try {
                utils.validateRegexFound(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.STRING_TO_SEARCH_FOR_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Search string: null
     * - Content to validate: Non-empty
     * Expects:
     * - AssertionError should be thrown saying the string to search for was null
     */
    @Test
    public void test_validateRegexFound_nullSearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseMessageExpectation(action, checkType, null, failureMsg);
            String contentToValidate = "some non-empty value";
            try {
                utils.validateRegexFound(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.STRING_TO_SEARCH_FOR_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the content to validate was null
     */
    @Test
    public void test_validateRegexFound_emptySearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseStatusExpectation(action, checkType, "", failureMsg);
            String contentToValidate = null;
            try {
                utils.validateRegexFound(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.CONTENT_TO_VALIDATE_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to match: Empty string
     * - Content to validate: Empty string
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateRegexFound_emptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, checkType, "", failureMsg);
            String contentToValidate = "";
            utils.validateRegexFound(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Search string: Empty string
     * - Content to validate: Non-empty
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateRegexFound_emptySearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseUrlExpectation(action, checkType, "", failureMsg);
            String contentToValidate = "does \n\r not \t include <br/> expected value";
            utils.validateRegexFound(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Invalid regex format
     * - Content to validate: Non-empty
     * Expects:
     * - PatternSyntaxException should be thrown
     */
    @Test
    public void test_validateRegexFound_searchStringInvalidRegex() {
        try {
            Expectation expectation = new ResponseFullExpectation(action, checkType, "start ?[(\\- end", failureMsg);
            String contentToValidate = "some non-empty value";
            try {
                utils.validateRegexFound(expectation, contentToValidate);
                fail("Should have thrown an exception because of invalid regex pattern, but did not.");
            } catch (PatternSyntaxException e) {
                // Expected
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Empty string
     * Expects:
     * - AssertionError should be thrown saying the expected regex was not found
     */
    @Test
    public void test_validateRegexFound_nonEmptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "";
            runNegativeValidateRegexFoundTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Does not match search string
     * Expects:
     * - AssertionError should be thrown saying the expected regex was not found
     */
    @Test
    public void test_validateRegexFound_nonEmptySearchString_contentDoesNotMatchString() {
        try {
            String searchForRegex = "[0-9]{4}";
            Expectation expectation = new ResponseHeaderExpectation(action, checkType, searchForRegex, failureMsg);
            String contentToValidate = "does 1\n\r not 12\t include 123 <br/> 123.4 expected value";
            runNegativeValidateRegexFoundTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Equal to string to match
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateRegexFound_nonEmptySearchString_contentEqualsSearchString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = searchFor;
            utils.validateRegexFound(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty, simple string
     * - Content to validate: Includes string to match
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateRegexFound_nonEmptySearchString_contentIncludesString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "does \n\r abc" + searchFor + "def \t include <br/> expected value";
            utils.validateRegexFound(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty regex
     * - Content to validate: Includes string to match
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateRegexFound_nonEmptySearchRegex_contentIncludesRegex() {
        try {
            Expectation expectation = new ResponseMessageExpectation(action, checkType, "[0-9]{4}", failureMsg);
            String contentToValidate = "does \n\r 1 abc 12 def \t 1234 include <br/> 12.345 expected value";
            utils.validateRegexFound(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validateRegexNotFound **************************************/

    /**
     * Tests:
     * - String to search for: null
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the string to search for is null
     */
    @Test
    public void test_validateRegexNotFound_nullSearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseStatusExpectation(action, checkType, null, failureMsg);
            String contentToValidate = null;
            try {
                utils.validateRegexNotFound(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.STRING_TO_SEARCH_FOR_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Search string: null
     * - Content to validate: Non-empty
     * Expects:
     * - AssertionError should be thrown saying the string to search for was null
     */
    @Test
    public void test_validateRegexNotFound_nullSearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseTitleExpectation(action, checkType, null, failureMsg);
            String contentToValidate = "some non-empty value";
            try {
                utils.validateRegexNotFound(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.STRING_TO_SEARCH_FOR_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Empty string
     * - Content to validate: null
     * Expects:
     * - AssertionError should be thrown saying the content to validate was null
     */
    @Test
    public void test_validateRegexNotFound_emptySearchString_nullContentToValidate() {
        try {
            Expectation expectation = new ResponseUrlExpectation(action, checkType, "", failureMsg);
            String contentToValidate = null;
            try {
                utils.validateRegexNotFound(expectation, contentToValidate);
                fail("Should have thrown an assertion error but did not.");
            } catch (AssertionError e) {
                verifyPattern(e.getMessage(), UnitTestUtils.CONTENT_TO_VALIDATE_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to match: Empty string
     * - Content to validate: Empty string
     * Expects:
     * - AssertionError should be thrown saying the regex was found when it should not have been
     */
    @Test
    public void test_validateRegexNotFound_emptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = new ResponseFullExpectation(action, checkType, "", failureMsg);
            String contentToValidate = "";
            runNegativeValidateRegexNotFoundTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Search string: Empty string
     * - Content to validate: Non-empty
     * Expects:
     * - AssertionError should be thrown saying the regex was found when it should not have been
     */
    @Test
    public void test_validateRegexNotFound_emptySearchString_nonEmptyContentToValidate() {
        try {
            Expectation expectation = new ResponseHeaderExpectation(action, checkType, "", failureMsg);
            String contentToValidate = "does \n\r not \t include <br/> expected value";
            runNegativeValidateRegexNotFoundTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Invalid regex format
     * - Content to validate: Non-empty
     * Expects:
     * - PatternSyntaxException should be thrown
     */
    @Test
    public void test_validateRegexNotFound_searchStringInvalidRegex() {
        try {
            Expectation expectation = new ResponseMessageExpectation(action, checkType, "start ?[(\\- end", failureMsg);
            String contentToValidate = "some non-empty value";
            try {
                utils.validateRegexNotFound(expectation, contentToValidate);
                fail("Should have thrown an exception because of invalid regex pattern, but did not.");
            } catch (PatternSyntaxException e) {
                // Expected
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Empty string
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateRegexNotFound_nonEmptySearchString_emptyContentToValidate() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "";
            utils.validateRegexNotFound(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Does not match search string
     * Expects:
     * - Assertion should succeed
     */
    @Test
    public void test_validateRegexNotFound_nonEmptySearchString_contentDoesNotMatchString() {
        try {
            String searchForRegex = "[0-9]{4}";
            Expectation expectation = new ResponseStatusExpectation(action, checkType, searchForRegex, failureMsg);
            String contentToValidate = "does 1\n\r not 12\t include 123 <br/> 123.4 expected value";
            utils.validateRegexNotFound(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty
     * - Content to validate: Equal to string to match
     * Expects:
     * - AssertionError should be thrown saying the regex was found when it should not have been
     */
    @Test
    public void test_validateRegexNotFound_nonEmptySearchString_contentEqualsSearchString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = searchFor;
            runNegativeValidateRegexNotFoundTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty, simple string
     * - Content to validate: Includes string to match
     * Expects:
     * - AssertionError should be thrown saying the regex was found when it should not have been
     */
    @Test
    public void test_validateRegexNotFound_nonEmptySearchString_contentIncludesString() {
        try {
            Expectation expectation = getBasicExpectation();
            String contentToValidate = "does \n\r abc" + searchFor + "def \t include <br/> expected value";
            runNegativeValidateRegexNotFoundTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - String to search for: Non-empty regex
     * - Content to validate: Includes string to match
     * Expects:
     * - AssertionError should be thrown saying the regex was found when it should not have been
     */
    @Test
    public void test_validateRegexNotFound_nonEmptySearchRegex_contentIncludesRegex() {
        try {
            String searchForRegex = "[0-9]{4}";
            Expectation expectation = new ResponseTitleExpectation(action, checkType, searchForRegex, failureMsg);
            String contentToValidate = "does \n\r 1 abc 12 def \t 1234 include <br/> 12.345 expected value";
            runNegativeValidateRegexNotFoundTest(expectation, contentToValidate);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    private void runNegativeAssertAndLogTest(String caller, String failureMsg, boolean testAssertion, boolean expectedResult) {
        try {
            utils.assertAndLog(caller, failureMsg, testAssertion, expectedResult);
            fail("Should have thrown an assertion error but did not.");
        } catch (AssertionError e) {
            assertEquals("Error message did not match expected value.", failureMsg, e.getMessage());
        }
    }

    private Expectation getBasicExpectation() {
        return new ResponseFullExpectation(action, checkType, searchFor, failureMsg);
    }

    private void runNegativeValidateStringContentTest(Expectation expectation, String contentToValidate, String expectedFailureMsg) throws Exception {
        try {
            utils.validateStringContent(expectation, contentToValidate);
            fail("Should have thrown an assertion error but did not.");
        } catch (AssertionError e) {
            verifyPattern(e.getMessage(), expectedFailureMsg);
        }
    }

    private void runNegativeValidateStringContainsTest(Expectation expectation, String contentToValidate) {
        try {
            utils.validateStringContains(expectation, contentToValidate);
            fail("Should have thrown an assertion error but did not.");
        } catch (AssertionError e) {
            String expectedFailureMsg = Pattern.quote(expectation.getFailureMsg()) + String.format(UnitTestUtils.ERR_STRING_NOT_FOUND, expectation.getValidationValue(), contentToValidate);
            verifyPattern(e.getMessage(), expectedFailureMsg);
        }
    }

    private void runNegativeValidateStringDoesNotContainTest(Expectation expectation, String contentToValidate) {
        try {
            utils.validateStringDoesNotContain(expectation, contentToValidate);
            fail("Should have thrown an assertion error but did not.");
        } catch (AssertionError e) {
            String expectedFailureMsg = Pattern.quote(expectation.getFailureMsg()) + String.format(UnitTestUtils.ERR_STRING_FOUND, expectation.getValidationValue(), contentToValidate);
            verifyPattern(e.getMessage(), expectedFailureMsg);
        }
    }

    private void runNegativeValidateRegexFoundTest(Expectation expectation, String contentToValidate) {
        try {
            utils.validateRegexFound(expectation, contentToValidate);
            fail("Should have thrown an assertion error but did not.");
        } catch (AssertionError e) {
            verifyPattern(e.getMessage(), Pattern.quote(expectation.getFailureMsg()) + String.format(UnitTestUtils.ERR_REGEX_NOT_FOUND, Pattern.quote(expectation.getValidationValue()), contentToValidate));
        }
    }

    private void runNegativeValidateRegexNotFoundTest(Expectation expectation, String contentToValidate) {
        try {
            utils.validateRegexNotFound(expectation, contentToValidate);
            fail("Should have thrown an assertion error but did not.");
        } catch (AssertionError e) {
            verifyPattern(e.getMessage(), Pattern.quote(expectation.getFailureMsg()) + String.format(UnitTestUtils.ERR_REGEX_FOUND, Pattern.quote(expectation.getValidationValue()), contentToValidate));
        }
    }

}
