/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.structures;

import com.ibm.ws.security.fat.common.TestServer;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;

public class OAuthOidcExpectation extends Expectation {

    public OAuthOidcExpectation(String testAction, String searchLocation, String checkType, String searchFor, String failureMsg) {
        super(testAction, searchLocation, checkType, searchFor, failureMsg);
    }

    public OAuthOidcExpectation(String testAction, String searchLocation, String checkType, String searchKey, String searchFor, String failureMsg) {
        super(testAction, searchLocation, checkType, searchKey, searchFor, failureMsg);
    }

    public OAuthOidcExpectation(String testAction, TestServer server, String searchLocation, String checkType, String searchKey, String searchFor, String failureMsg) {
        super(testAction, server, searchLocation, checkType, searchKey, searchFor, failureMsg);
    }

    public static Expectation createNoAccessTokenInResponseExpectation(String action) {
        return createResponseMissingValueExpectation(action, Constants.ACCESS_TOKEN_KEY);
    }

    public static Expectation createNoIdTokenInResponseExpectation(String action) {
        return createResponseMissingValueExpectation(action, Constants.ID_TOKEN_KEY);
    }

    public static Expectation addTokenInResponseExpectation(String action, String token) {
        return createResponseExpectation(action, token, "Did not find [" + token + "] in the response.");
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        // Not supported yet. Add logic here if it is required.
        throw new UnsupportedOperationException("The validate method is currently not supported.");
    }
}