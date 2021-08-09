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

public class ExceptionMessageExpectation extends Expectation {

    protected static Class<?> thisClass = ExceptionMessageExpectation.class;

    public ExceptionMessageExpectation(String testAction, String checkType, String searchFor, String failureMsg) {
        super(testAction, Constants.RESPONSE_FULL, checkType, searchFor, failureMsg);
    }

    public ExceptionMessageExpectation(String testAction, String searchFor, String failureMsg) {
        super(testAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, searchFor, failureMsg);
    }

    @Override
    protected void validate(Object exception) throws Exception {
        try {
            String messageText = getErrorMessageFromException(exception);
            validationUtils.validateStringContent(this, messageText);
        } catch (Exception e) {
            throw new Exception(failureMsg + " Failed to validate exception message: " + e.getMessage());
        }
    }

    String getErrorMessageFromException(Object exception) throws Exception {
        if (exception instanceof Exception) {
            return ((Exception) exception).getMessage();
        }
        return WebResponseUtils.getResponseText(exception);
    }

}
