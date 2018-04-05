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
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseHeaderExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseTitleExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class TestValidationUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private static final String CONTENT_TO_VALIDATE_NULL = "Content to validate is null";
    private static final String STRING_TO_SEARCH_FOR_NULL = "String to search for is null";
    private static final String ERR_COMPARISON_TYPE_UNKNOWN = "comparison type.+" + "%s" + ".+unknown";
    private static final String ERR_STRING_NOT_FOUND = ".*Was expecting .*" + "%s" + ".+received.+\\[" + "%s" + "\\]";
    private static final String ERR_STRING_FOUND = ".*Was not expecting .*" + "%s" + ".+received.+\\[" + "%s" + "\\]";
    private static final String ERR_REGEX_NOT_FOUND = ".*Did not find.* regex.+" + "%s" + ".+ content.+\\[" + "%s" + "\\]";
    private static final String ERR_REGEX_FOUND = ".+unexpected regex.+" + "%s" + ".+ content.+\\[" + "%s" + "\\]";

    private final String action = "some test action";
    private final String checkType = "check type";
    private final String searchFor = "search for me";
    private final String failureMsg = "This is a failure message.";

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
            } catch (Exception e) {
                verifyException(e, String.format(ERR_COMPARISON_TYPE_UNKNOWN, "null"));
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
            } catch (Exception e) {
                verifyException(e, String.format(ERR_COMPARISON_TYPE_UNKNOWN, Pattern.quote(checkType)));
            }
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

            String expectedFailureMsg = Pattern.quote(failureMsg) + String.format(ERR_STRING_NOT_FOUND, searchFor, contentToValidate);
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

            String expectedFailureMsg = Pattern.quote(failureMsg) + String.format(ERR_STRING_FOUND, searchFor, contentToValidate);
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

            String expectedFailureMsg = Pattern.quote(failureMsg) + String.format(ERR_REGEX_NOT_FOUND, Pattern.quote(searchFor), contentToValidate);
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

            String expectedFailureMsg = Pattern.quote(failureMsg) + String.format(ERR_REGEX_FOUND, searchFor, contentToValidate);
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
                verifyPattern(e.getMessage(), STRING_TO_SEARCH_FOR_NULL);
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
                verifyPattern(e.getMessage(), STRING_TO_SEARCH_FOR_NULL);
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
                verifyPattern(e.getMessage(), CONTENT_TO_VALIDATE_NULL);
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
                verifyPattern(e.getMessage(), STRING_TO_SEARCH_FOR_NULL);
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
                verifyPattern(e.getMessage(), STRING_TO_SEARCH_FOR_NULL);
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
                verifyPattern(e.getMessage(), CONTENT_TO_VALIDATE_NULL);
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
                verifyPattern(e.getMessage(), STRING_TO_SEARCH_FOR_NULL);
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
                verifyPattern(e.getMessage(), STRING_TO_SEARCH_FOR_NULL);
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
                verifyPattern(e.getMessage(), CONTENT_TO_VALIDATE_NULL);
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
                verifyPattern(e.getMessage(), STRING_TO_SEARCH_FOR_NULL);
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
                verifyPattern(e.getMessage(), STRING_TO_SEARCH_FOR_NULL);
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
                verifyPattern(e.getMessage(), CONTENT_TO_VALIDATE_NULL);
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
            String expectedFailureMsg = Pattern.quote(expectation.getFailureMsg()) + String.format(ERR_STRING_NOT_FOUND, expectation.getValidationValue(), contentToValidate);
            verifyPattern(e.getMessage(), expectedFailureMsg);
        }
    }

    private void runNegativeValidateStringDoesNotContainTest(Expectation expectation, String contentToValidate) {
        try {
            utils.validateStringDoesNotContain(expectation, contentToValidate);
            fail("Should have thrown an assertion error but did not.");
        } catch (AssertionError e) {
            String expectedFailureMsg = Pattern.quote(expectation.getFailureMsg()) + String.format(ERR_STRING_FOUND, expectation.getValidationValue(), contentToValidate);
            verifyPattern(e.getMessage(), expectedFailureMsg);
        }
    }

    private void runNegativeValidateRegexFoundTest(Expectation expectation, String contentToValidate) {
        try {
            utils.validateRegexFound(expectation, contentToValidate);
            fail("Should have thrown an assertion error but did not.");
        } catch (AssertionError e) {
            verifyPattern(e.getMessage(), Pattern.quote(expectation.getFailureMsg()) + String.format(ERR_REGEX_NOT_FOUND, Pattern.quote(expectation.getValidationValue()), contentToValidate));
        }
    }

    private void runNegativeValidateRegexNotFoundTest(Expectation expectation, String contentToValidate) {
        try {
            utils.validateRegexNotFound(expectation, contentToValidate);
            fail("Should have thrown an assertion error but did not.");
        } catch (AssertionError e) {
            verifyPattern(e.getMessage(), Pattern.quote(expectation.getFailureMsg()) + String.format(ERR_REGEX_FOUND, Pattern.quote(expectation.getValidationValue()), contentToValidate));
        }
    }

}
