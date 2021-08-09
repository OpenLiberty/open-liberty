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

public class ResponseFullExpectation extends Expectation {

    protected static Class<?> thisClass = ResponseFullExpectation.class;

    public ResponseFullExpectation(String checkType, String searchFor, String failureMsg) {
        this(null, checkType, searchFor, failureMsg);
    }

    public ResponseFullExpectation(String testAction, String checkType, String searchFor, String failureMsg) {
        super(testAction, Constants.RESPONSE_FULL, checkType, searchFor, failureMsg);
    }

    public ResponseFullExpectation(String testAction, String searchLocation, String checkType, String searchKey, String searchFor, String failureMsg) {
        super(testAction, searchLocation, checkType, searchKey, searchFor, failureMsg);
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        try {
            String responseText = getResponseTextFromContent(contentToValidate);
            validationUtils.validateStringContent(this, responseText);
        } catch (Exception e) {
            throw new Exception(failureMsg + " Failed to validate response text: " + e.getMessage());
        }
    }

    protected String getResponseTextFromContent(Object contentToValidate) throws Exception {
        if (contentToValidate instanceof String) {
            return (String) contentToValidate;
        }
        return WebResponseUtils.getResponseText(contentToValidate);
    }

}
