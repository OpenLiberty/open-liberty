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

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.Constants.CheckType;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

public abstract class Expectation {

    private static Class<?> thisClass = Expectation.class;

    protected String testAction;
    protected String searchLocation;
    protected String checkType;
    protected CheckType expCheckType = null;
    protected String validationKey;
    protected String validationValue;
    protected String failureMsg;
    protected boolean isExpectationHandled;

    protected TestValidationUtils validationUtils = new TestValidationUtils();

    public Expectation(String testAction, String searchLocation, String checkType, String searchFor, String failureMsg) {
        this(testAction, searchLocation, checkType, null, searchFor, failureMsg);
    }

    public Expectation(String testAction, String searchLocation, String checkType, String searchKey, String searchFor, String failureMsg) {
        this.testAction = testAction;
        this.searchLocation = searchLocation;
        this.checkType = checkType;
        this.validationKey = searchKey;
        this.validationValue = searchFor;
        this.failureMsg = failureMsg;
        this.isExpectationHandled = false;
    }

    public String getAction() {
        return testAction;
    };

    public String getSearchLocation() {
        return searchLocation;
    };

    public String getCheckType() {
        return checkType;
    };

    public CheckType getExpectedCheckType() {
        return expCheckType;
    }

    public String getValidationKey() {
        return validationKey;
    };

    public String getValidationValue() {
        return validationValue;
    };

    public String getFailureMsg() {
        return failureMsg;
    };

    public boolean isExpectationHandled() {
        return isExpectationHandled;
    }

    public void setIsExpectationHandled(boolean isExpectationHandled) {
        this.isExpectationHandled = isExpectationHandled;
    }

    /**
     * Performs all of the steps necessary to verify that this particular expectation is met. If this expectation's test action is
     * set to {@code null}, validation will proceed regardless of the test action passed to this method. Otherwise, if the current
     * test action does not match the action for this expectation, validation is skipped.
     * 
     * @param currentTestAction
     * @param contentToValidate
     *            Some kind of object to validate against (typically some kind of HtmlUnit entity like a WebResponse)
     */
    public void validate(String currentTestAction, Object contentToValidate) throws Exception {
        if (!isExpectationForAction(currentTestAction)) {
            return;
        }
        Log.info(thisClass, "validate", "Checking " + this);
        validate(contentToValidate);
    }

    /**
     * Performs all of the steps necessary to verify that this particular expectation is met.
     *
     * @param contentToValidate
     *            Some kind of object to validate against (typically some kind of HtmlUnit entity like a WebResponse)
     */
    abstract protected void validate(Object contentToValidate) throws Exception;

    public boolean isExpectationForAction(String testAction) {
        if ((this.testAction != null) && (testAction == null || !testAction.equals(this.testAction))) {
            return false;
        }
        return true;
    }

    public static Expectation createResponseExpectation(String testAction, String searchForValue, String failureMsg) {
        return new ResponseFullExpectation(testAction, Constants.STRING_CONTAINS, searchForValue, failureMsg);
    }

    public static Expectation createResponseMissingValueExpectation(String action, String value) {
        return createResponseMissingValueExpectation(action, value, "Found [" + value + "] in the response and should not have.");
    }

    public static Expectation createResponseMissingValueExpectation(String action, String value, String failureMsg) {
        return new ResponseFullExpectation(action, Constants.STRING_DOES_NOT_CONTAIN, value, failureMsg);
    }

    public static Expectation createExceptionExpectation(String testAction, String searchForValue, String failureMsg) {
        return new ExceptionMessageExpectation(testAction, Constants.STRING_CONTAINS, searchForValue, failureMsg);
    }

    @Override
    public String toString() {
        return String.format("Expectation: [ Action: %s | Search In: %s | Check Type: %s | Search Key: %s | Search For: %s | Failure message: %s ]",
                testAction, searchLocation, (checkType != null) ? checkType : expCheckType, validationKey, validationValue, failureMsg);
    }
}
