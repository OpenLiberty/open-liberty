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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.Constants.CheckType;
import com.ibm.ws.security.fat.common.Constants.JsonCheckType;
import com.ibm.ws.security.fat.common.Constants.ObjectCheckType;
import com.ibm.ws.security.fat.common.Constants.StringCheckType;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

public class JsonObjectExpectation extends Expectation {

    protected static Class<?> thisClass = JsonObjectExpectation.class;

    static final String DEFAULT_FAILURE_MSG = "An error occurred validating JSON data.";

    List<ValueType> jsonPrimitiveTypes = Arrays.asList(new ValueType[] { ValueType.NULL, ValueType.TRUE, ValueType.FALSE });

    protected ValueType valueType = null;

    /** For non-string expected value types, this member variable will be used as the expected value to compare against */
    protected Object expectedValue = null;

    public JsonObjectExpectation(String expectedKey) {
        this(expectedKey, JsonCheckType.KEY_EXISTS, null);
    }

    public JsonObjectExpectation(String expectedKey, CheckType checkType, Object expectedValue) {
        this(expectedKey, (ValueType) null, expectedValue);
        this.expCheckType = checkType;
    }

    public JsonObjectExpectation(String keyToLookFor, ValueType expectedValueType) {
        this(keyToLookFor, expectedValueType, null, DEFAULT_FAILURE_MSG);
        this.expCheckType = JsonCheckType.VALUE_TYPE;
    }

    public JsonObjectExpectation(String expectedKey, ValueType expectedValueType, Object expectedValue) {
        this(expectedKey, expectedValueType, expectedValue, DEFAULT_FAILURE_MSG);
        this.expCheckType = (expectedValueType == ValueType.STRING) ? StringCheckType.EQUALS : ObjectCheckType.EQUALS;
    }

    public JsonObjectExpectation(String keyToLookFor, ValueType expectedValueType, Object expectedValue, String failureMsg) {
        this(null, keyToLookFor, expectedValueType, expectedValue, failureMsg);
    }

    public JsonObjectExpectation(String testAction, String keyToLookFor, ValueType expectedValueType, Object expectedValue, String failureMsg) {
        super(testAction, Constants.JSON_OBJECT, null, keyToLookFor, null, failureMsg);
        this.expCheckType = (expectedValueType == ValueType.STRING) ? StringCheckType.EQUALS : ObjectCheckType.EQUALS;
        this.valueType = expectedValueType;
        this.expectedValue = expectedValue;
    }

    public ValueType getExpectedValueType() {
        return valueType;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    @Override
    protected void validate(Object contentToValidate) throws Exception {
        try {
            JsonObject jsonDataToValidate = readJsonFromContent(contentToValidate);
            verifyKeyExistenceMatchesExpectation(jsonDataToValidate);
            if (!isKeyExistenceOnlyCheck()) {
                validateValue(jsonDataToValidate);
            }
        } catch (Throwable e) {
            failValidation(e);
        }
    }

    private void failValidation(Throwable e) {
        e.printStackTrace();
        String failurePrefix = "";
        if (failureMsg != null) {
            if (e == null || !e.toString().contains(failureMsg)) {
                failurePrefix = failureMsg + " ";
            }
        }
        fail(failurePrefix + "Failed to validate JSON data for key [" + validationKey + "]. " + e);
    }

    private boolean isKeyExistenceOnlyCheck() {
        return expCheckType == JsonCheckType.KEY_EXISTS || expCheckType == JsonCheckType.KEY_DOES_NOT_EXIST;
    }

    /**
     * Attempts to read the response text in the provided object as a JSON string and convert it to its corresponding JsonObject
     * representation. Extending classes should override this method if they do not expect the content to be a pure JSON string.
     * For example, if the provided object is an instance of WebResponse whose response text includes other non-JSON information,
     * the extending expectation class should override this method to extract the appropriate JSON data.
     */
    protected JsonObject readJsonFromContent(Object contentToValidate) throws Exception {
        if (contentToValidate == null) {
            throw new Exception("Provided content is null so cannot be validated.");
        }
        JsonObject obj = null;
        try {
            String responseText = WebResponseUtils.getResponseText(contentToValidate);
            obj = Json.createReader(new StringReader(responseText)).readObject();
        } catch (Exception e) {
            throw new Exception("Failed to read JSON data from the provided content. The exception was [" + e + "]. The content to validate was: [" + contentToValidate + "].");
        }
        return obj;
    }

    /**
     * Verifies that the specified key is present (if expected to be present) or is not present (if not expected to be present) in
     * the JSON data.
     */
    private void verifyKeyExistenceMatchesExpectation(JsonObject jsonDataToValidate) {
        boolean jsonDataContainsKey = jsonDataToValidate.containsKey(validationKey);
        if (expCheckType == JsonCheckType.KEY_DOES_NOT_EXIST) {
            assertFalse("The provided content contains a \"" + validationKey + "\" key but should not. The content to validate was: [" + jsonDataToValidate + "].", jsonDataContainsKey);
        } else {
            assertTrue("The provided content does not contain a \"" + validationKey + "\" key but should. The content to validate was: [" + jsonDataToValidate + "].", jsonDataContainsKey);
        }
    }

    private void validateValue(JsonObject jsonDataToValidate) throws Exception {
        if (valueType == null && expCheckType == ObjectCheckType.EQUALS) {
            throw new Exception("The checkType for this expectation (" + expCheckType + ") requires that a specific ValueType also be set. Please specify what ValueType to expect for this expectation.");
        }
        if (valueType != null) {
            validateValueType(jsonDataToValidate);
        }
        if (validationValue == null && expectedValue == null) {
            Log.info(thisClass, "validateValue", "Skipping validation of the value for key [" + validationKey + "] because no expected value has been set.");
            return;
        }
        if (valueType == ValueType.STRING || (expCheckType instanceof StringCheckType)) {
            validateStringValue(jsonDataToValidate);
        } else {
            validateNonStringValue(jsonDataToValidate);
        }
    }

    private void validateValueType(JsonObject jsonDataToValidate) {
        Log.info(thisClass, "validateValueType", "Validating the ValueType of the value found for the expected key");
        JsonValue value = jsonDataToValidate.get(validationKey);
        if (value == null) {
            assertEquals("The value found for the [" + validationKey + "] key was null, but the value was not expected to be null.", JsonValue.NULL, valueType);
        } else {
            assertEquals("The ValueType of the value [" + value + "] did not match the expected type.", valueType, value.getValueType());
        }
    }

    private void validateStringValue(JsonObject jsonDataToValidate) throws Exception {
        Log.info(thisClass, "validateStringValue", "Validating the value for the expected key as a string");
        if (isStringValueExpected()) {
            validationValue = (String) expectedValue;
        }
        String stringValue = getStringValueFromJsonDataToValidate(jsonDataToValidate);
        validationUtils.validateStringContent(this, stringValue);
    }

    private boolean isStringValueExpected() {
        return validationValue == null && expectedValue != null && (expectedValue instanceof String);
    }

    private String getStringValueFromJsonDataToValidate(JsonObject jsonDataToValidate) throws Exception {
        JsonValue value = jsonDataToValidate.get(validationKey);
        if (!isValidTypeForStringValidation(value)) {
            throw new Exception("Value for key [" + validationKey + "] was not a valid ValueType for performing string validation. The value's type was [" + value.getValueType() + "] and the value was [" + value + "].");
        }
        String stringValue = null;
        if (value.getValueType() == ValueType.STRING) {
            stringValue = jsonDataToValidate.getString(validationKey);
        }
        return stringValue;
    }

    private boolean isValidTypeForStringValidation(JsonValue value) {
        return value.getValueType() == ValueType.STRING || value.getValueType() == ValueType.NULL;
    }

    protected void validateNonStringValue(JsonObject jsonDataToValidate) {
        Log.info(thisClass, "validateNonStringValue", "Validating the value for the expected key as a non-string value");
        JsonValue value = jsonDataToValidate.get(validationKey);
        if (isJsonPrimitiveType()) {
            validateJsonPrimitiveType(value);
        } else {
            validateJsonNonPrimitiveType(value, jsonDataToValidate);
        }
    }

    private boolean isJsonPrimitiveType() {
        return jsonPrimitiveTypes.contains(valueType);
    }

    /**
     * The expected value provided in the constructor (if there was one) is superfluous if we're validating a JSON primitive (i.e.
     * null, true, or false). Specifying an expected ValueType of one of those types is sufficient to validate the value, so we
     * ignore the expected value variables here.
     */
    private void validateJsonPrimitiveType(JsonValue value) {
        Log.info(thisClass, "validateJsonPrimitiveType", "Validating the value for the expected key as a JSON primitive value");
        for (ValueType primitiveType : jsonPrimitiveTypes) {
            if (valueType == primitiveType) {
                assertEquals("Value was expected to be a " + primitiveType + " ValueType, but was not. Value was [" + value + "].", primitiveType, value.getValueType());
            }
        }
    }

    private void validateJsonNonPrimitiveType(JsonValue value, JsonObject jsonDataToValidate) {
        Log.info(thisClass, "validateJsonNonPrimitiveType", "Validating the value for the expected key as a non-JSON primitive value");
        Object pojoValue = null;
        if (value.getValueType() == ValueType.ARRAY) {
            pojoValue = jsonDataToValidate.getJsonArray(validationKey);
        } else if (value.getValueType() == ValueType.NUMBER) {
            pojoValue = getNumberPojoFromJson(jsonDataToValidate);
        } else if (value.getValueType() == ValueType.OBJECT) {
            pojoValue = jsonDataToValidate.getJsonObject(validationKey);
        } else if (value.getValueType() == ValueType.STRING) {
            pojoValue = jsonDataToValidate.getString(validationKey);
        }
        assertEquals("Value for [" + validationKey + "] did not match the expected value.", expectedValue, pojoValue);
    }

    private Object getNumberPojoFromJson(JsonObject jsonDataToValidate) {
        Object pojoValue = null;
        JsonNumber numberValue = jsonDataToValidate.getJsonNumber(validationKey);
        if (numberValue.isIntegral()) {
            long longValue = numberValue.longValueExact();
            int intValue = numberValue.intValueExact();
            if (intValue != longValue) {
                // Rounding likely occurred, so use long value
                pojoValue = longValue;
            } else {
                pojoValue = intValue;
            }
        } else {
            pojoValue = numberValue.doubleValue();
        }
        return pojoValue;
    }

    @Override
    public String toString() {
        return String.format("Expectation: [ Action: %s | Search In: %s | Check Type: %s | Search Key: %s | Search for (string): %s | Search for (object): %s | Failure message: %s ]",
                testAction, searchLocation, expCheckType, validationKey, validationValue, expectedValue, failureMsg);
    }

}