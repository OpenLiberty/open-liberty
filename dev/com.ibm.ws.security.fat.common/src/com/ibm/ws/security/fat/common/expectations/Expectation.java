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

import com.ibm.ws.security.fat.common.Constants;

public class Expectation {

    protected String testAction;
    protected String searchLocation;
    protected String checkType;
    protected String validationKey;
    protected String validationValue;
    protected String failureMsg;
    protected boolean isExpectationHandled;

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

    public static Expectation createResponseExpectation(String testAction, String searchForValue, String failureMsg) {
        return new Expectation(testAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, searchForValue, failureMsg);
    }

    public static Expectation createResponseMissingValueExpectation(String action, String value) {
        return createResponseMissingValueExpectation(action, value, "Found [" + value + "] in the response and should not have.");
    }

    public static Expectation createResponseMissingValueExpectation(String action, String value, String failureMsg) {
        return new Expectation(action, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, value, failureMsg);
    }

    public static Expectation createResponseStatusExpectation(String testAction, int expectedStatusCode) {
        final String defaultMsg = "Did not receive status code [" + expectedStatusCode + "] during test action [" + testAction + "].";
        return new Expectation(testAction, Constants.RESPONSE_STATUS, Constants.STRING_CONTAINS, Integer.toString(expectedStatusCode), defaultMsg);
    }

    public static Expectation createJsonExpectation(String testAction, String key, String value, String failureMsg) {
        return new Expectation(testAction, Constants.JSON_OBJECT, null, key, value, failureMsg);
    }

    @Override
    public String toString() {
        return String.format("Expectation: [ Action: %s | Search In: %s | Check Type: %s | Search For: %s | Failure message: %s ]",
                testAction, searchLocation, checkType, validationValue, failureMsg);
    }
}
