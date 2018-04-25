package com.ibm.ws.security.fat.common.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectation;

public class TestValidationUtils {

    private final Class<?> thisClass = TestValidationUtils.class;

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

    public void validateStringContent(Expectation expected, String contentToValidate) throws Exception {
        if (expected == null) {
            throw new Exception("The provided expectation is null; the specified content cannot be validated.");
        }
        String checkType = expected.getCheckType();

        if (Constants.STRING_CONTAINS.equals(checkType)) {
            validateStringContains(expected, contentToValidate);
        } else if (Constants.STRING_DOES_NOT_CONTAIN.equals(checkType)) {
            validateStringDoesNotContain(expected, contentToValidate);
        } else if (Constants.STRING_MATCHES.equals(checkType)) {
            validateRegexFound(expected, contentToValidate);
        } else if (Constants.STRING_DOES_NOT_MATCH.equals(checkType)) {
            validateRegexNotFound(expected, contentToValidate);
        } else {
            throw new Exception("String comparison type (" + checkType + ") unknown. Check that the offending test case has coded its expectations correctly.");
        }
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
