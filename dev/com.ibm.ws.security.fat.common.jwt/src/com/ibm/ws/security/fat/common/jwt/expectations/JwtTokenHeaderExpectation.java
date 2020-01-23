/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt.expectations;

import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants.CheckType;
import com.ibm.ws.security.fat.common.Constants.JsonCheckType;
import com.ibm.ws.security.fat.common.expectations.JsonObjectExpectation;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;

public class JwtTokenHeaderExpectation extends JsonObjectExpectation {

    public static final String SEARCH_LOCATION = "jwt-token-header";

    static final String DEFAULT_FAILURE_MSG = "An error occurred validating the JWT Header.";

    public JwtTokenHeaderExpectation(String expectedKey) {
        this(expectedKey, JsonCheckType.KEY_EXISTS, null);
    }

    public JwtTokenHeaderExpectation(String expectedKey, CheckType checkType, Object expectedValue) {
        super(expectedKey, checkType, expectedValue);
        this.searchLocation = SEARCH_LOCATION;
        this.failureMsg = DEFAULT_FAILURE_MSG;
    }

    public JwtTokenHeaderExpectation(String expectedKey, ValueType expectedValueType) {
        super(expectedKey, expectedValueType);
        this.searchLocation = SEARCH_LOCATION;
        this.failureMsg = DEFAULT_FAILURE_MSG;
    }

    public JwtTokenHeaderExpectation(String expectedKey, ValueType expectedValueType, Object expectedValue) {
        super(expectedKey, expectedValueType, expectedValue, DEFAULT_FAILURE_MSG);
        this.searchLocation = SEARCH_LOCATION;
    }

    @Override
    protected JsonObject readJsonFromContent(Object contentToValidate) throws Exception {
        String method = "readJsonFromContent - (JwtTokenHeaderExpectation)";
        JsonObject header = null;
        try {
            if (contentToValidate != null && (contentToValidate instanceof String)) {
                header = (new JwtTokenForTest((String) contentToValidate)).getJsonHeader();
            }
            Log.info(thisClass, method, "Header: " + header);
            return header;
        } catch (Exception e) {
            throw new Exception("Failed to read JSON data from the provided content. Error was: [" + e + "]. Content was: [" + contentToValidate + "]");
        }
    }

}
