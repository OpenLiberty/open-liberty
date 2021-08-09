/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat.expectations;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatUtils;

public class JwtExpectation extends Expectation {

    protected static Class<?> thisClass = JwtExpectation.class;

    protected ValueType expectedType = null;

    public static final String DEFAULT_FAILURE_MSG = "The %s claim in the JWT did not pass validation.";
    public static final String SEARCH_LOCATION_JWT = "jwt";

    private TestValidationUtils validationUtils = new TestValidationUtils();
    private JwtFatUtils fatUtils = new JwtFatUtils();

    public JwtExpectation(String testAction, String key, String value) {
        this(testAction, Constants.STRING_MATCHES, key, ValueType.STRING, value, String.format(DEFAULT_FAILURE_MSG, key));
    }

    public JwtExpectation(String testAction, String key, ValueType expectedValueType, String value) {
        this(testAction, Constants.STRING_MATCHES, key, expectedValueType, value, String.format(DEFAULT_FAILURE_MSG, key));
    }

    public JwtExpectation(String testAction, String key, ValueType expectedValueType, String value, String failureMsg) {
        this(testAction, Constants.STRING_MATCHES, key, expectedValueType, value, failureMsg);
    }

    public JwtExpectation(String testAction, String checkType, String key, ValueType expectedValueType, String value, String failureMsg) {
        super(testAction, SEARCH_LOCATION_JWT, checkType, key, value, failureMsg);
        expectedType = expectedValueType;
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        JsonObject jwtToValidate = fatUtils.extractJwtPrincipalFromResponse(contentToValidate);
        JsonValue value = jwtToValidate.get(validationKey);
        validateValueType(value);
        validateValue(value);
    }

    void validateValueType(JsonValue value) throws Exception {
        ValueType valueType = (value == null) ? ValueType.NULL : value.getValueType();
        if (valueType != expectedType) {
            throw new Exception("Value for entry [" + validationKey + "] was expected to be " + expectedType + " but was not. Value was [" + value + "] (of type: " + valueType
                                + ").");
        }
    }

    void validateValue(JsonValue value) throws Exception {
        ValueType valueType = (value == null) ? ValueType.NULL : value.getValueType();
        if (valueType == ValueType.NULL) {
            verifyNullValue(value);
        } else {
            validateNonNullValue(value);
        }
    }

    void verifyNullValue(JsonValue value) throws Exception {
        if (value != null && value != JsonValue.NULL) {
            throw new Exception("Value for key " + validationKey + " is expected to be null but was [" + value + "].");
        }
    }

    void validateNonNullValue(JsonValue value) throws Exception {
        String stringValue = value.toString();
        if (value.getValueType() == ValueType.STRING) {
            stringValue = removeQuotesFromJsonStringValue((JsonString) value);
        }
        validationUtils.validateStringContent(this, stringValue);
    }

    String removeQuotesFromJsonStringValue(JsonString value) {
        // The toString() method for JsonString types includes double quotes around the value; we don't need those quotes
        return value.toString().replaceAll("\"(.*)\"", "$1");
    }

}
