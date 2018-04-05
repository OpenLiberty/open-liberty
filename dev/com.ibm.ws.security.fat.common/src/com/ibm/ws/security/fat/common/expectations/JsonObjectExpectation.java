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

public class JsonObjectExpectation extends Expectation {

    protected static Class<?> thisClass = JsonObjectExpectation.class;

    public JsonObjectExpectation(String testAction, String searchKey, String searchValue, String failureMsg) {
        super(testAction, Constants.JSON_OBJECT, null, searchKey, searchValue, failureMsg);
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        // TODO
        throw new Exception("Validation for " + this.getClass().getName() + " has not been implemented yet!");
    }

}