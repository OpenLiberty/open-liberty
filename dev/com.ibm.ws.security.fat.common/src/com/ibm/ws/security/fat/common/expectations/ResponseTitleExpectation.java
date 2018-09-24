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

public class ResponseTitleExpectation extends Expectation {

    protected static Class<?> thisClass = ResponseTitleExpectation.class;

    public ResponseTitleExpectation(String checkType, String searchFor, String failureMsg) {
        this(null, checkType, searchFor, failureMsg);
    }

    public ResponseTitleExpectation(String testAction, String checkType, String searchFor, String failureMsg) {
        super(testAction, Constants.RESPONSE_TITLE, checkType, searchFor, failureMsg);
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        try {
            String responseTitle = getResponseTitle(contentToValidate);
            validationUtils.validateStringContent(this, responseTitle);
        } catch (Exception e) {
            throw new Exception(failureMsg + " Failed to validate response title: " + e.getMessage());
        }
    }

    String getResponseTitle(Object contentToValidate) throws Exception {
        if (contentToValidate instanceof String) {
            return (String) contentToValidate;
        }
        return WebResponseUtils.getResponseTitle(contentToValidate);
    }

}
