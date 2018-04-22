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

public class ResponseTitleExpectation extends Expectation {

    protected static Class<?> thisClass = ResponseTitleExpectation.class;

    public ResponseTitleExpectation(String testAction, String checkType, String searchFor, String failureMsg) {
        super(testAction, Constants.RESPONSE_TITLE, checkType, searchFor, failureMsg);
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        // TODO
    }

}