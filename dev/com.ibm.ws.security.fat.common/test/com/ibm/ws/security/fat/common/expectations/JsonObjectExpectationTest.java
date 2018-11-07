/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.Constants.CheckType;
import com.ibm.ws.security.fat.common.Constants.JsonCheckType;
import com.ibm.ws.security.fat.common.Constants.ObjectCheckType;
import com.ibm.ws.security.fat.common.Constants.StringCheckType;
import com.ibm.ws.security.fat.common.test.UnitTestUtils;

import test.common.SharedOutputManager;

public class JsonObjectExpectationTest extends CommonSpecificExpectationTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private static final String FAILURE_REGEX_FAILED_TO_READ_JSON = ".*Failed to read JSON data.*";
    private static final String FAILURE_REGEX_CONTENT_MISSING_KEY = ".+content does not contain.*";
    private static final String FAILURE_REGEX_VALUE_TYPE_DID_NOT_MATCH_THE_EXPECTED_TYPE = ".*ValueType.*did not match the expected type.*";
    private static final String FAILURE_REGEX_VALUE_DID_NOT_MATCH_EXPECTED_VALUE = ".*Value for.*" + "%s" + ".*did not match the expected value.*" + "expected:<" + "%s" + ">.*was:<" + "%s" + ">";

    private static final String SEARCH_KEY = "searchKey";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** Constructors **************************************/

    @Test
    public void test_constructor_key() {
        try {
            String searchKey = SEARCH_KEY;

            JsonObjectExpectation exp = new JsonObjectExpectation(searchKey);

            verifyJsonObjectExpectationValues(exp, searchKey, null, JsonCheckType.KEY_EXISTS, null, null, JsonObjectExpectation.DEFAULT_FAILURE_MSG);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_key_valueType() {
        try {
            String searchKey = "searchKey";
            ValueType valueType = ValueType.ARRAY;

            JsonObjectExpectation exp = new JsonObjectExpectation(searchKey, valueType);

            verifyJsonObjectExpectationValues(exp, searchKey, valueType, JsonCheckType.VALUE_TYPE, null, null, JsonObjectExpectation.DEFAULT_FAILURE_MSG);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_key_valueType_expectedVal() {
        try {
            String searchKey = "searchKey";
            ValueType valueType = ValueType.STRING;
            Object expectedValue = 123L;

            JsonObjectExpectation exp = new JsonObjectExpectation(searchKey, valueType, expectedValue);

            verifyJsonObjectExpectationValues(exp, searchKey, valueType, StringCheckType.EQUALS, null, expectedValue, JsonObjectExpectation.DEFAULT_FAILURE_MSG);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_key_checkType_expectedVal() {
        try {
            String searchKey = "searchKey";
            CheckType checkType = StringCheckType.DOES_NOT_CONTAIN;
            Object expectedValue = 123L;

            JsonObjectExpectation exp = new JsonObjectExpectation(searchKey, checkType, expectedValue);

            verifyJsonObjectExpectationValues(exp, searchKey, null, checkType, null, expectedValue, JsonObjectExpectation.DEFAULT_FAILURE_MSG);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_key_valueType_expectedVal_failureMsg() {
        try {
            String searchKey = "searchKey";
            ValueType valueType = ValueType.NULL;
            Object expectedValue = 123L;
            String failureMsg = "failureMsg";

            JsonObjectExpectation exp = new JsonObjectExpectation(searchKey, valueType, expectedValue, failureMsg);

            // Because the ValueType is not a string, the default CheckType will be ObjectCheckType.EQUALS
            verifyJsonObjectExpectationValues(exp, searchKey, valueType, ObjectCheckType.EQUALS, null, expectedValue, failureMsg);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validate **************************************/

    /**
     * Tests:
     * - Object to validate: null
     * Expects:
     * - Should fail because by default the validate() method will try to interpret the content object as a JSON string
     */
    @Override
    @Test
    public void test_validate_nullContentObject() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.NUMBER, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            try {
                exp.validate(null);
                fail("Should have thrown an exception because the content is not a string, but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*content is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Object to validate: Non-string
     * Expects:
     * - Should fail because by default the validate() method will try to interpret the content object as a JSON string
     */
    @Test
    public void test_validate_contentObjectNotAString() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.OBJECT, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            Object content = 123;
            try {
                exp.validate(content);
                fail("Should have thrown an exception because the content is not a string, but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*" + String.format(UnitTestUtils.ERR_UNKNOWN_RESPONSE_TYPE, Pattern.quote(content.getClass().getName())));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Object to validate: Empty string
     * Expects:
     * - Should fail because the content is not a valid JSON string
     */
    @Test
    public void test_validate_contentObjectEmptyString() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.TRUE, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            String content = "";
            try {
                exp.validate(content);
                fail("Should have thrown an exception because the content is not a valid JSON string, but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + FAILURE_REGEX_FAILED_TO_READ_JSON + "JsonParsingException");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Object to validate: Non-empty, non-JSON string
     * Expects:
     * - Should fail because the content is not a valid JSON string
     */
    @Test
    public void test_validate_contentObjectNonJsonString() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.STRING, SEARCH_FOR_VAL, FAILURE_MESSAGE);
            String content = "The quick brown fox jumps over the lazy dog.";
            try {
                exp.validate(content);
                fail("Should have thrown an exception because the content is not a valid JSON string, but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + FAILURE_REGEX_FAILED_TO_READ_JSON + "JsonParsingException");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Object to validate: Empty JSON object
     * Expects:
     * - Should fail because the content is missing the expected key
     */
    @Test
    public void test_validate_contentObjectEmptyJsonObject() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY);
            String content = "{}";
            try {
                exp.validate(content);
                fail("Should have thrown an error because the JSON object read from the content does not include the expected key.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + FAILURE_REGEX_CONTENT_MISSING_KEY + exp.getValidationKey());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Object to validate: String starting with valid JSON object, but also includes non-JSON content and other JSON data
     * - Beginning JSON object does not include expected key
     * Expects:
     * - The JSON object at the beginning of the content should be the only information parsed
     * - Should fail because the interpreted JSON is missing the expected key
     */
    @Test
    public void test_validate_contentObjectStartsWithValidJsonString_firstObjectMissingExpectedKey() {
        try {
            String keyToLookFor = SEARCH_KEY;
            JsonObjectExpectation exp = new JsonObjectExpectation(keyToLookFor, ObjectCheckType.EQUALS, SEARCH_FOR_VAL);
            String content = "{\"The\": \"quick\"} brown fox jumps {\"" + keyToLookFor + "\": \"" + SEARCH_FOR_VAL + "\"} the lazy dog.";
            try {
                exp.validate(content);
                fail("Should have thrown an error because the JSON object read from the content does not include the expected key.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + FAILURE_REGEX_CONTENT_MISSING_KEY + exp.getValidationKey());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Object to validate: String starting with valid JSON object, but also includes non-JSON content and other JSON data
     * - Beginning JSON object includes expected key, but value does not pass validation
     * Expects:
     * - The JSON object at the beginning of the content should be the only information parsed
     * - Validation of the corresponding value should fail
     */
    @Test
    public void test_validate_contentObjectIncludesMultipleValidJsonStrings_firstObjectIncludesExpectedKey_wrongValue() {
        try {
            String keyToLookFor = SEARCH_KEY;
            String badValue = "quick";
            JsonObjectExpectation exp = new JsonObjectExpectation(keyToLookFor, StringCheckType.EQUALS, SEARCH_FOR_VAL);
            String content = "{\"" + keyToLookFor + "\": \"" + badValue + "\"} brown fox {\"jumps\": \"over\"} the lazy dog.";
            try {
                exp.validate(content);
                fail("Should have thrown an error because the JSON object does not equal the expected value for the specified key.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(UnitTestUtils.ERR_STRING_DOES_NOT_EQUAL, SEARCH_FOR_VAL, badValue));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Object to validate: String starting with valid JSON object, but also includes non-JSON content and other JSON data
     * - Beginning JSON object includes expected key and value passes validation
     * Expects:
     * - The JSON object at the beginning of the content should be the only information parsed
     * - Validation of the corresponding value should succeed
     */
    @Test
    public void test_validate_contentObjectIncludesMultipleValidJsonStrings_firstObjectIncludesExpectedKey_correctValue() {
        try {
            String keyToLookFor = "The";
            JsonObjectExpectation exp = new JsonObjectExpectation(keyToLookFor, StringCheckType.EQUALS, "quick");
            String content = "{\"" + keyToLookFor + "\": \"quick\"} brown fox {\"jumps\": \"over\"} the lazy dog.";
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Object to validate: JSON object missing expected key
     * Expects:
     * - Should fail because the content is missing the expected key
     */
    @Test
    public void test_validate_missingExpectedKey() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.DOES_NOT_CONTAIN_REGEX, SEARCH_FOR_VAL);
            String content = "{\"test\":1}";
            try {
                exp.validate(content);
                fail("Should have thrown an error because the JSON object read from the content does not include the expected key.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + FAILURE_REGEX_CONTENT_MISSING_KEY + exp.getValidationKey() + "\"");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - CheckType: Some string check type
     * - Value is not a string type
     * Expects:
     * - Validation should fail because the value should be a string type if we're expecting to validate a string
     */
    @Test
    public void test_validate_string_nonStringValue() {
        try {
            String keyToLookFor = SEARCH_KEY;
            JsonObjectExpectation exp = new JsonObjectExpectation(keyToLookFor, StringCheckType.CONTAINS, "should be ignored");
            int value = 1123581321;
            String content = createContentJsonString(keyToLookFor, value);
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*Value for key.*" + keyToLookFor + ".*not a valid ValueType for.*string validation");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_null_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.NULL, "should be ignored");
            String value = "badValue";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(UnitTestUtils.ERR_STRING_NOT_NULL, value));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_null_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.NULL, "should be ignored");
            String value = null;
            String content = createContentJsonString(SEARCH_KEY, value);
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_notNull_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.NOT_NULL, "should be ignored");
            String value = null;
            String content = createContentJsonString(SEARCH_KEY, value);
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + UnitTestUtils.ERR_STRING_NULL);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_notNull_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.NOT_NULL, "should be ignored");
            String value = "some value";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_equals_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.EQUALS, SEARCH_FOR_VAL);
            String value = "badValue";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(UnitTestUtils.ERR_STRING_DOES_NOT_EQUAL, SEARCH_FOR_VAL, value));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_equals_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.EQUALS, SEARCH_FOR_VAL);
            String value = SEARCH_FOR_VAL;
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_contains_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.CONTAINS, SEARCH_FOR_VAL);
            String value = "badValue";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(UnitTestUtils.ERR_STRING_NOT_FOUND, SEARCH_FOR_VAL, value));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_contains_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.CONTAINS, SEARCH_FOR_VAL);
            String value = "value is " + SEARCH_FOR_VAL + " in here somewhere";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_doesNotContain_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.DOES_NOT_CONTAIN, SEARCH_FOR_VAL);
            String value = "value is " + SEARCH_FOR_VAL + " in here somewhere";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(UnitTestUtils.ERR_STRING_FOUND, SEARCH_FOR_VAL, value));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_doesNotContain_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.DOES_NOT_CONTAIN, SEARCH_FOR_VAL);
            String value = "badValue";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_containsRegex_fails() {
        try {
            String searchForRegex = "[a-zA-Z]+";
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.CONTAINS_REGEX, searchForRegex);
            String value = "1234567890";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(UnitTestUtils.ERR_REGEX_NOT_FOUND, searchForRegex, value));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_containsRegex_succeeds() {
        try {
            String searchForRegex = "[a-zA-Z]+";
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.CONTAINS_REGEX, searchForRegex);
            String value = "bad Value";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_doesNotContainRegex_fails() {
        try {
            String searchForRegex = "[a-zA-Z]+";
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.DOES_NOT_CONTAIN_REGEX, searchForRegex);
            String value = "bad Value";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(UnitTestUtils.ERR_REGEX_FOUND, searchForRegex, value));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_string_doesNotContainRegex_succeeds() {
        try {
            String searchForRegex = "[a-zA-Z]+";
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, StringCheckType.DOES_NOT_CONTAIN_REGEX, searchForRegex);
            String value = "1234567890";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - CheckType: Object equals
     * - No expected ValueType set in the expectation
     * Expects:
     * - Validation should fail because the expectation needs to know what ValueType to expect if we're performing an object
     * equality check
     */
    @Test
    public void test_validate_object_equals_noValueTypeSet() {
        try {
            JsonArray realArrayValue = Json.createArrayBuilder().add(1).build();
            JsonArray expectedArrayValue = Json.createArrayBuilder().add(2).build();

            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ObjectCheckType.EQUALS, expectedArrayValue);
            String content = createContentJsonString(SEARCH_KEY, realArrayValue);
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*checkType.+" + ObjectCheckType.EQUALS + ".*requires.*specific ValueType");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_jsonKey_exists_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, JsonCheckType.KEY_EXISTS, null);
            String value = "some value";
            String content = createContentJsonString("not the right key", "\"" + value + "\"");
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + FAILURE_REGEX_CONTENT_MISSING_KEY + SEARCH_KEY);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_jsonKey_exists_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, JsonCheckType.KEY_EXISTS, "should be ignored");
            double value = 3.1415;
            String content = createContentJsonString(SEARCH_KEY, value);
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_keyExpectedToBeMissing_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, JsonCheckType.KEY_DOES_NOT_EXIST, null);
            String value = "some value";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*content contains.*" + SEARCH_KEY + ".*but should not");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_keyExpectedToBeMissing_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, JsonCheckType.KEY_DOES_NOT_EXIST, null);
            String value = "some value";
            String content = createContentJsonString("some other key", "\"" + value + "\"");
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_expectNullType_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.NULL);
            String content = createContentJsonString(SEARCH_KEY, 1);
            try {
                exp.validate(content);
                fail("Should have thrown an error because the value for the specified key is not of the correct type.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*ValueType.*did not match the expected type.*expected:.*" + exp.getExpectedValueType());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_expectNullType_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.NULL);
            String content = createContentJsonString(SEARCH_KEY, null);
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expects value associated with key to be: Null
     * - An expected value is also provided in the expectation constructor
     * - Value is null
     * Expects:
     * - Validation should succeed
     * - Expected value should be ignored since the expected ValueType is null
     */
    @Test
    public void test_validate_expectNullType_withNonNullExpectedValue() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.NULL, "should be ignored", FAILURE_MESSAGE);
            String content = createContentJsonString(SEARCH_KEY, null);
            // The expected value passed to the constructor should effectively be ignored because the NULL ValueType is all that's needed to verify the value
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_expectStringType_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.STRING);
            String content = createContentJsonString(SEARCH_KEY, 1);
            try {
                exp.validate(content);
                fail("Should have thrown an error because the value for the specified key is not of the correct type.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + FAILURE_REGEX_VALUE_TYPE_DID_NOT_MATCH_THE_EXPECTED_TYPE + exp.getExpectedValueType());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_expectStringType_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.STRING);
            String content = createContentJsonString(SEARCH_KEY, "\"value\"");
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expects value associated with key to be: String
     * - Expected value also provided
     * - Value associated with key does not match the expected value
     * Expects:
     * - Validation should fail
     */
    @Test
    public void test_validate_expectStringType_withExpectedValue_fails() {
        try {
            String expectedValue = "some other value";
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.STRING, expectedValue, FAILURE_MESSAGE);
            String value = "value";
            String content = createContentJsonString(SEARCH_KEY, "\"" + value + "\"");
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(UnitTestUtils.ERR_STRING_DOES_NOT_EQUAL, expectedValue, value));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expects value associated with key to be: String
     * - Expected value also provided
     * - Value associated with key matches the expected value
     * Expects:
     * - Validation should succeed
     */
    @Test
    public void test_validate_expectStringType_withExpectedValue_succeeds() {
        try {
            String expectedValue = "value";
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.STRING, expectedValue, FAILURE_MESSAGE);
            String content = createContentJsonString(SEARCH_KEY, "\"" + expectedValue + "\"");
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_expectTrueType_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.TRUE);
            String content = createContentJsonString(SEARCH_KEY, Json.createArrayBuilder().build());
            try {
                exp.validate(content);
                fail("Should have thrown an error because the value for the specified key is not of the correct type.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + FAILURE_REGEX_VALUE_TYPE_DID_NOT_MATCH_THE_EXPECTED_TYPE + exp.getExpectedValueType());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_expectTrueType_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.TRUE);
            String content = createContentJsonString(SEARCH_KEY, true);
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expects value associated with key to be: True
     * - Expected value (of some other type) is also provided in the constructor
     * - Value associated with key is the right type
     * Expects:
     * - Expected value should be ignored since the expected ValueType is a JSON primitive
     * - Validation should succeed
     */
    @Test
    public void test_validate_expectTrueType_withNonTrueExpectedValue() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.TRUE, 123L, FAILURE_MESSAGE);
            String content = createContentJsonString(SEARCH_KEY, true);
            // The expected value passed to the constructor should effectively be ignored because the TRUE ValueType is all that's needed to verify the value
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_expectFalseType_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.FALSE);
            String content = createContentJsonString(SEARCH_KEY, true);
            try {
                exp.validate(content);
                fail("Should have thrown an error because the value for the specified key is not of the correct type.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + FAILURE_REGEX_VALUE_TYPE_DID_NOT_MATCH_THE_EXPECTED_TYPE + exp.getExpectedValueType());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_expectFalseType_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.FALSE);
            String content = createContentJsonString(SEARCH_KEY, false);
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expects value associated with key to be: False
     * - Expected value (of some other type) is also provided in the constructor
     * - Value associated with key is the right type
     * Expects:
     * - Expected value should be ignored since the expected ValueType is a JSON primitive
     * - Validation should succeed
     */
    @Test
    public void test_validate_expectFalseType_withNonFalseExpectedValue() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.FALSE, "other", FAILURE_MESSAGE);
            String content = createContentJsonString(SEARCH_KEY, false);
            // The expected value passed to the constructor should effectively be ignored because the FALSE ValueType is all that's needed to verify the value
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_expectNumberType_fails() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.NUMBER);
            String content = createContentJsonString(SEARCH_KEY, "\"number\"");
            try {
                exp.validate(content);
                fail("Should have thrown an error because the value for the specified key is not of the correct type.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + FAILURE_REGEX_VALUE_TYPE_DID_NOT_MATCH_THE_EXPECTED_TYPE + exp.getExpectedValueType());
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_expectNumberType_succeeds() {
        try {
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.NUMBER);
            String content = createContentJsonString(SEARCH_KEY, 3.14);
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expects value associated with key to be: Number
     * - Expected value (of some other type) is also provided in the constructor
     * - Value associated with key is the right type
     * Expects:
     * - We'll still compare against the expected value even though the expected value's type doesn't match the expected ValueType
     * - Validation should fail because the actual value does not match the expected value
     */
    @Test
    public void test_validate_expectNumberType_withNonNumberExpectedValue_fails() {
        try {
            String expectedValue = "other";
            double actualValue = 1.123581321;
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.NUMBER, expectedValue, FAILURE_MESSAGE);
            String content = createContentJsonString(SEARCH_KEY, actualValue);
            try {
                exp.validate(content);
                fail("Should have thrown an error because the value for the specified key does not match the expected value.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(FAILURE_REGEX_VALUE_DID_NOT_MATCH_EXPECTED_VALUE, SEARCH_KEY, expectedValue, actualValue));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Expects value associated with key to be: Number
     * - Expected value is also provided in the constructor
     * - Value associated with key is the right type, but doesn't exactly equal the expected value
     * Expects:
     * - Validation should fail because the actual value does not match the expected value
     */
    @Test
    public void test_validate_expectNumberType_valueIsAlmostEqual() {
        try {
            double expectedValue = 1.123581321;
            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.NUMBER, expectedValue, FAILURE_MESSAGE);
            double actualValue = expectedValue - 0.000000001;
            String content = createContentJsonString(SEARCH_KEY, actualValue);
            try {
                exp.validate(content);
                fail("Should have thrown an error because the value for the specified key does not match the expected value.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(FAILURE_REGEX_VALUE_DID_NOT_MATCH_EXPECTED_VALUE, SEARCH_KEY, expectedValue, actualValue));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_jsonArray_fails() {
        try {
            JsonArray realArrayValue = Json.createArrayBuilder().add(1).build();
            JsonArray expectedArrayValue = Json.createArrayBuilder().add(2).build();

            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.ARRAY, expectedArrayValue);
            String content = createContentJsonString(SEARCH_KEY, realArrayValue);
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(FAILURE_REGEX_VALUE_DID_NOT_MATCH_EXPECTED_VALUE, SEARCH_KEY, Pattern.quote(expectedArrayValue.toString()), Pattern.quote(realArrayValue.toString())));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_jsonArray_succeeds() {
        try {
            JsonArray expectedArrayValue = Json.createArrayBuilder().add(1).add(2).build();

            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.ARRAY, expectedArrayValue);
            String content = createContentJsonString(SEARCH_KEY, expectedArrayValue);
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_jsonObject_fails() {
        try {
            JsonObject realObjectValue = Json.createObjectBuilder().add("string", "value").add("number", 1).add("boolean", true).build();
            JsonObject expectedObjectValue = Json.createObjectBuilder().add("string", "value").add("number", 1).build();

            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.OBJECT, expectedObjectValue);
            String content = createContentJsonString(SEARCH_KEY, realObjectValue);
            try {
                exp.validate(content);
                fail("Should have thrown an error validating the JSON data but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + String.format(FAILURE_REGEX_VALUE_DID_NOT_MATCH_EXPECTED_VALUE, SEARCH_KEY, Pattern.quote(expectedObjectValue.toString()), Pattern.quote(realObjectValue.toString())));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_jsonObject_succeeds() {
        try {
            JsonObject expectedObjectValue = Json.createObjectBuilder().add("string", "value").add("number", 1).add("boolean", true).build();

            JsonObjectExpectation exp = new JsonObjectExpectation(SEARCH_KEY, ValueType.OBJECT, expectedObjectValue);
            String content = createContentJsonString(SEARCH_KEY, expectedObjectValue);
            exp.validate(content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    @Override
    protected Expectation createBasicExpectation() {
        return new JsonObjectExpectation(TEST_ACTION, SEARCH_KEY, ValueType.STRING, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    @Override
    protected Expectation createBasicExpectationWithNoAction() {
        return new JsonObjectExpectation(SEARCH_KEY, ValueType.STRING, SEARCH_FOR_VAL, FAILURE_MESSAGE);
    }

    protected void verifyJsonObjectExpectationValues(JsonObjectExpectation testExp, String expSearchKey, ValueType expValueType, CheckType expCheckType, String expStringValue, Object expValue, String expFailureMsg) {
        verifyExpectationValues(testExp, null, Constants.JSON_OBJECT, null, expSearchKey, expStringValue, expFailureMsg);
        assertEquals("Expected CheckType did not match expected value.", expCheckType, testExp.getExpectedCheckType());
        assertEquals("Expected (object) value did not match expected value.", expValue, testExp.getExpectedValue());
    }

    private String createContentJsonString(String key, Object value) {
        return "{\"number\":1, \"" + key + "\":" + value + ", \"obj\":{}, \"array\":[\"a\",\"b\",\"c\"]}";
    }

}
