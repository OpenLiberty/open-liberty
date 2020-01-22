/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.Constants.CheckType;
import com.ibm.ws.security.fat.common.Constants.StringCheckType;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.logging.CommonFatLoggingUtils;

public class TestValidationUtils {

    private final Class<?> thisClass = TestValidationUtils.class;

    protected final CommonFatLoggingUtils loggingUtils = new CommonFatLoggingUtils();

    /**
     * Logs the state of the test assertion and then invokes the JUnit assertTrue method to record the test "status"
     * with JUnit.
     *
     * @param caller
     *            - Routine that is requesting the check be performed
     * @param failureMsg
     *            - Message that will be recorded if the test assertion fails
     * @param testAssertion
     *            - State of the test assertion
     */
    public void assertTrueAndLog(String caller, String failureMsg, boolean testAssertion) {
        assertAndLog(caller, failureMsg, testAssertion, true);
    }

    /**
     * Logs the state of the test assertion and then invokes the JUnit assertTrue or assertFalse methods, depending on
     * the value of expectedResult, to record the test "status" with JUnit.
     *
     * @param caller
     *            - Routine that is requesting the check be performed
     * @param failureMsg
     *            - Message that will be recorded if the test assertion fails
     * @param testAssertion
     *            - State of the test assertion
     * @param expectedResult
     *            - Expected result of the test assertion
     */
    public boolean assertAndLog(String caller, String failureMsg, boolean testAssertion, boolean expectedResult) {
        Log.info(thisClass, caller, "Test assertion is: " + testAssertion);
        if (expectedResult) {
            if (!testAssertion) {
                Log.info(thisClass, caller, failureMsg);
            }
            assertTrue(failureMsg, testAssertion);
            return true;
        }
        if (testAssertion) {
            Log.info(thisClass, caller, failureMsg);
        }
        assertFalse(failureMsg, testAssertion);
        return false;
    }

    // short cut for validating when we don't have a response (ie: when we're just checking for server log messages)
    public void validateResult(Expectations expectations) throws Exception {
        validateResult(null, null, expectations);
    }

    public void validateResult(Object response, Expectations expectations) throws Exception {
        validateResult(response, null, expectations);
    }

    public void validateResult(Object response, String currentAction, Expectations expectations) throws Exception {
        String thisMethod = "validateResult";
        loggingUtils.printMethodName(thisMethod, "Start of");

        Log.info(thisClass, thisMethod, "currentAction is: " + currentAction);
        if (expectations == null) {
            Log.info(thisClass, thisMethod, "Expectations are null");
            return;
        }
        try {
            for (Expectation expectation : expectations.getExpectations()) {
                expectation.validate(currentAction, response);
            }
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e, "Error validating response");
            throw e;
        }
        loggingUtils.printMethodName(thisMethod, "End of");
    }

    public void validateException(Exception exception, String currentAction, Expectations expectations) throws Exception {
        String thisMethod = "validateException";
        loggingUtils.printMethodName(thisMethod, "Start of");

        Log.info(thisClass, thisMethod, "currentAction is: " + currentAction);
        if (expectations == null) {
            Log.info(thisClass, thisMethod, "Expectations are null");
            return;
        }
        try {
            for (Expectation expectation : expectations.getExpectations()) {
                expectation.validate(currentAction, exception);
            }
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e, "Error validating exception");
            throw e;
        }
        loggingUtils.printMethodName(thisMethod, "End of");
    }

    public void validateStringContent(Expectation expected, String contentToValidate) throws Exception {
        if (expected == null) {
            throw new Exception("The provided expectation is null; the specified content cannot be validated.");
        }
        String checkType = expected.getCheckType();
        CheckType checkTypeEnum = expected.getExpectedCheckType();

        if (Constants.STRING_NULL.equals(checkType) || checkTypeEnum == StringCheckType.NULL) {
            validateStringNull(expected, contentToValidate);
        } else if (Constants.STRING_NOT_NULL.equals(checkType) || checkTypeEnum == StringCheckType.NOT_NULL) {
            validateStringNotNull(expected, contentToValidate);
        } else if (Constants.STRING_EQUALS.equals(checkType) || checkTypeEnum == StringCheckType.EQUALS) {
            validateStringEquals(expected, contentToValidate);
        } else if (Constants.STRING_CONTAINS.equals(checkType) || checkTypeEnum == StringCheckType.CONTAINS) {
            validateStringContains(expected, contentToValidate);
        } else if (Constants.STRING_DOES_NOT_CONTAIN.equals(checkType) || checkTypeEnum == StringCheckType.DOES_NOT_CONTAIN) {
            validateStringDoesNotContain(expected, contentToValidate);
        } else if (Constants.STRING_MATCHES.equals(checkType) || checkTypeEnum == StringCheckType.CONTAINS_REGEX) {
            validateRegexFound(expected, contentToValidate);
        } else if (Constants.STRING_DOES_NOT_MATCH.equals(checkType) || checkTypeEnum == StringCheckType.DOES_NOT_CONTAIN_REGEX) {
            validateRegexNotFound(expected, contentToValidate);
        } else {
            throw new Exception("String comparison type (" + checkType + ") unknown. Check that the offending test case has coded its expectations correctly.");
        }
    }

    void validateStringNull(Expectation expected, String contentToValidate) {
        boolean assertion = contentToValidate == null;

        String failureSubMsg = "Expected value to be null, but received [" + contentToValidate + "]";
        assertTrueAndLog("validateStringNull", expected.getFailureMsg() + " " + failureSubMsg, assertion);
    }

    void validateStringNotNull(Expectation expected, String contentToValidate) {
        boolean assertion = contentToValidate != null;

        String failureSubMsg = "Expected value not to be null, but it was.";
        assertTrueAndLog("validateStringNotNull", expected.getFailureMsg() + " " + failureSubMsg, assertion);
    }

    void validateStringEquals(Expectation expected, String contentToValidate) {
        String searchFor = expected.getValidationValue();
        if (searchFor == null) {
            validateStringNull(expected, contentToValidate);
            return;
        }
        assertContentNotNull(contentToValidate, searchFor);

        boolean assertion = searchFor.equals(contentToValidate);

        String failureSubMsg = "Was expecting content to equal [" + searchFor + "] but received [" + contentToValidate + "]";
        assertTrueAndLog("validateStringEquals", expected.getFailureMsg() + " " + failureSubMsg, assertion);
    }

    void validateStringContains(Expectation expected, String contentToValidate) {
        String searchFor = expected.getValidationValue();
        assertContentNotNull(contentToValidate, searchFor);

        boolean assertion = contentToValidate.contains(searchFor);

        String failureSubMsg = "Was expecting to find [" + searchFor + "] but received [" + contentToValidate + "]";
        assertTrueAndLog("validateStringContains", expected.getFailureMsg() + " " + failureSubMsg, assertion);
    }

    void validateStringDoesNotContain(Expectation expected, String contentToValidate) {
        String searchFor = expected.getValidationValue();
        assertContentNotNull(contentToValidate, searchFor);

        boolean assertion = !contentToValidate.contains(searchFor);

        String failureSubMsg = "Was not expecting to find [" + searchFor + "] but received [" + contentToValidate + "]";
        assertTrueAndLog("validateStringDoesNotContain", expected.getFailureMsg() + " " + failureSubMsg, assertion);
    }

    void validateRegexFound(Expectation expected, String contentToValidate) {
        String searchFor = expected.getValidationValue();
        assertContentNotNull(contentToValidate, searchFor);
        Pattern pattern = Pattern.compile(searchFor);
        Matcher m = pattern.matcher(contentToValidate);

        boolean assertion = m.find();

        String failureSubMsg = "Did not find expected regex [" + searchFor + "] in content: [" + contentToValidate + "]";
        assertTrueAndLog("validateRegexFound", expected.getFailureMsg() + " " + failureSubMsg, assertion);
    }

    void validateRegexNotFound(Expectation expected, String contentToValidate) {
        String searchFor = expected.getValidationValue();
        assertContentNotNull(contentToValidate, searchFor);
        Pattern pattern = Pattern.compile(searchFor);
        Matcher m = pattern.matcher(contentToValidate);

        boolean assertion = !(m.find());

        String failureSubMsg = "Found unexpected regex [" + searchFor + "] in content: [" + contentToValidate + "]";
        assertTrueAndLog("validateRegexNotFound", expected.getFailureMsg() + " " + failureSubMsg, assertion);
    }

    private void assertContentNotNull(String contentToValidate, String searchFor) {
        if (contentToValidate == null && searchFor != null) {
            fail("Content to validate is null but the search value (" + searchFor + ") is not.");
        }
        if (searchFor == null) {
            fail("String to search for is null, so the expectation cannot be satisfied.");
        }
    }

}
