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

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

public class ResponseStatusExpectation extends Expectation {

    protected static Class<?> thisClass = ResponseStatusExpectation.class;

    public static final String DEFAULT_FAILURE_MSG = "Did not receive the expected status code [%s] during test action [%s].";

    public ResponseStatusExpectation(String testAction, String checkType, String searchFor, String failureMsg) {
        super(testAction, Constants.RESPONSE_STATUS, checkType, searchFor, failureMsg);
    }

    public ResponseStatusExpectation(int expectedStatus) {
        this(null, expectedStatus);
    }

    public ResponseStatusExpectation(String testAction, int expectedStatus) {
        this(testAction, expectedStatus, String.format(DEFAULT_FAILURE_MSG, Integer.toString(expectedStatus), testAction));
    }

    public ResponseStatusExpectation(String testAction, int expectedStatus, String failureMsg) {
        super(testAction, Constants.RESPONSE_STATUS, Constants.STRING_EQUALS, Integer.toString(expectedStatus), failureMsg);
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        try {
            int responseStatus = getResponseStatus(contentToValidate);
            validationUtils.validateStringContent(this, Integer.toString(responseStatus));
        } catch (Exception e) {
            throw new Exception("Failed to validate response status: " + e.getMessage());
        }
    }

    int getResponseStatus(Object contentToValidate) throws Exception {
        if (contentToValidate instanceof Integer) {
            return (Integer) contentToValidate;
        }
        return WebResponseUtils.getResponseStatusCode(contentToValidate);
    }

}
