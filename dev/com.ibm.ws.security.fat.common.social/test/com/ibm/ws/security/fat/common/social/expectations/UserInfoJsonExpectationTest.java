package com.ibm.ws.security.fat.common.social.expectations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebResponse;
import com.ibm.ws.security.fat.common.Constants.CheckType;
import com.ibm.ws.security.fat.common.Constants.JsonCheckType;
import com.ibm.ws.security.fat.common.Constants.ObjectCheckType;
import com.ibm.ws.security.fat.common.Constants.StringCheckType;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class UserInfoJsonExpectationTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.fat.common.*=all");

    private static final String FAILURE_REGEX_ERROR_READING_JSON_CONTENT = "Failed to read JSON data.+";

    private static final String KEY_TO_LOOK_FOR = "searchKey";
    private static final String TEST_ACTION = "someTestAction";

    private final WebResponse webResponse = mockery.mock(WebResponse.class);

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
            String keyToLookFor = KEY_TO_LOOK_FOR;

            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(keyToLookFor);

            verifyExpectationValues(exp, keyToLookFor, null, JsonCheckType.KEY_EXISTS, null, UserInfoJsonExpectation.DEFAULT_FAILURE_MSG);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_key_checkType_value() {
        try {
            String keyToLookFor = KEY_TO_LOOK_FOR;
            CheckType checkType = StringCheckType.DOES_NOT_CONTAIN_REGEX;
            Object value = 123;

            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(keyToLookFor, checkType, value);

            verifyExpectationValues(exp, keyToLookFor, null, checkType, value, UserInfoJsonExpectation.DEFAULT_FAILURE_MSG);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_key_valueType() {
        try {
            String keyToLookFor = KEY_TO_LOOK_FOR;
            ValueType valueType = ValueType.OBJECT;

            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(keyToLookFor, valueType);

            verifyExpectationValues(exp, keyToLookFor, valueType, JsonCheckType.VALUE_TYPE, null, UserInfoJsonExpectation.DEFAULT_FAILURE_MSG);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_key_valueType_value() {
        try {
            String keyToLookFor = KEY_TO_LOOK_FOR;
            ValueType valueType = ValueType.NULL;
            Object value = "some value";

            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(keyToLookFor, valueType, value);

            verifyExpectationValues(exp, keyToLookFor, valueType, ObjectCheckType.EQUALS, value, UserInfoJsonExpectation.DEFAULT_FAILURE_MSG);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** readJsonFromContent **************************************/

    @Test
    public void test_readJsonFromContent_nullContent() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);
            try {
                Object contentToValidate = null;
                JsonObject result = exp.readJsonFromContent(contentToValidate);
                fail("Should have thrown an exception but found JSON data: " + result);
            } catch (Exception e) {
                verifyException(e, FAILURE_REGEX_ERROR_READING_JSON_CONTENT + "provided content string is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readJsonFromContent_unknownContentType() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);
            try {
                Object contentToValidate = 123;
                JsonObject result = exp.readJsonFromContent(contentToValidate);
                fail("Should have thrown an exception but found JSON data: " + result);
            } catch (Exception e) {
                verifyException(e, FAILURE_REGEX_ERROR_READING_JSON_CONTENT + "Unknown response type");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readJsonFromContent_webResponse_nullContentString() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            final String content = null;
            mockery.checking(new Expectations() {
                {
                    allowing(webResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            try {
                JsonObject result = exp.readJsonFromContent(webResponse);
                fail("Should have thrown an exception but found JSON data: " + result);
            } catch (Exception e) {
                verifyException(e, FAILURE_REGEX_ERROR_READING_JSON_CONTENT + "provided content string is null");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readJsonFromContent_webResponse_emptyContentString() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            final String content = "";
            mockery.checking(new Expectations() {
                {
                    allowing(webResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            try {
                JsonObject result = exp.readJsonFromContent(webResponse);
                fail("Should have thrown an exception but found JSON data: " + result);
            } catch (Exception e) {
                verifyException(e, FAILURE_REGEX_ERROR_READING_JSON_CONTENT + "Did not find.*" + Pattern.quote(UserInfoJsonExpectation.USER_INFO_SERVLET_OUTPUT_REGEX));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readJsonFromContent_string_nonEmptyContentString_missingRequiredText() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            final String content = "It was the best of times, it was the worst of times.";
            try {
                JsonObject result = exp.readJsonFromContent(content);
                fail("Should have thrown an exception but found JSON data: " + result);
            } catch (Exception e) {
                verifyException(e, FAILURE_REGEX_ERROR_READING_JSON_CONTENT + "Did not find.*" + Pattern.quote(UserInfoJsonExpectation.USER_INFO_SERVLET_OUTPUT_REGEX));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readJsonFromContent_string_includesPrefixText_extractedContentNotJson() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            final String content = "It was the best of times, it was the worst of times." + UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + "{ Hello } world!";
            try {
                JsonObject result = exp.readJsonFromContent(content);
                fail("Should have thrown an exception but found JSON data: " + result);
            } catch (Exception e) {
                verifyException(e, FAILURE_REGEX_ERROR_READING_JSON_CONTENT + "JsonParsingException");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readJsonFromContent_string_includesPrefixText_extractedContentValidJson() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            final String content = "It was the best of times, it was the worst of times." + UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + "{ \"Hello\":\"Bob\" } world!";
            JsonObject result = exp.readJsonFromContent(content);
            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Did not find the expected value in the extracted JSON object. Result was: " + result, "Bob", result.getString("Hello"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readJsonFromContent_webResponse_includesPrefixText_extractedContentValidJson() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            final String content = "It \n\r was the \t\n best \r of times, " + UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + "{ \"Hello\":\"Bob\" } \n\r world!";
            mockery.checking(new Expectations() {
                {
                    one(webResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            JsonObject result = exp.readJsonFromContent(webResponse);
            assertNotNull("Result should not have been null but was.", result);
            assertEquals("Did not find the expected value in the extracted JSON object. Result was: " + result, "Bob", result.getString("Hello"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Content includes valid prefix and JSON object, but spread across multiple lines
     * Expects:
     * - Should fail because the JSON string is expected to exist on a single line
     */
    @Test
    public void test_readJsonFromContent_jsonSpansMultipleLines() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + "{\n\r\"key\":\t \n\"value\"\r }";
            mockery.checking(new Expectations() {
                {
                    allowing(webResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            try {
                JsonObject result = exp.readJsonFromContent(webResponse);
                fail("Should have thrown an exception but found JSON data: " + result);
            } catch (Exception e) {
                verifyException(e, FAILURE_REGEX_ERROR_READING_JSON_CONTENT + "Did not find.*" + Pattern.quote(UserInfoJsonExpectation.USER_INFO_SERVLET_OUTPUT_REGEX));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readJsonFromContent_emptyJson() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + "{}";
            mockery.checking(new Expectations() {
                {
                    one(webResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            JsonObject result = exp.readJsonFromContent(webResponse);
            assertNotNull("Result should not have been null but was.", result);
            assertTrue("Result should have been empty but was not. Result was: " + result, result.isEmpty());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readJsonFromContent_nonEmptyJson() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            JsonObject userInfo = createSampleJsonObjectBuilder().build();
            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + userInfo.toString();
            mockery.checking(new Expectations() {
                {
                    one(webResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            JsonObject result = exp.readJsonFromContent(webResponse);
            assertEquals("Result did not match the expected JSON object.", userInfo, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_readJsonFromContent_nonEmptyJson_withinOtherContent() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            JsonObject userInfo = createSampleJsonObjectBuilder().build();

            String beforeContent = "Say \"what\" again! \n\r \t I dare you, \r I double dare you!";
            String afterContent = "You know what they call \r \n a Quarter Pounder with cheese in Paris?";
            final String content = beforeContent + UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + userInfo.toString() + afterContent;

            mockery.checking(new Expectations() {
                {
                    one(webResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            JsonObject result = exp.readJsonFromContent(webResponse);
            assertEquals("Result did not match the expected JSON object.", userInfo, result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** validate **************************************/

    @Test
    public void test_validate_keyOnly_contentMissingKey() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            JsonObject userInfo = createSampleJsonObjectBuilder().build();

            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + userInfo.toString();
            try {
                exp.validate(TEST_ACTION, content);
                fail("Should have failed to validate the content, but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*content does not contain.*" + KEY_TO_LOOK_FOR);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_keyOnly() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR);

            JsonObject userInfo = createSampleJsonObjectBuilder().add(KEY_TO_LOOK_FOR, "some value").build();

            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + userInfo.toString();

            exp.validate(TEST_ACTION, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_key_checkType_expectedValue_contentDoesNotPassValidation() {
        try {
            String expectedValue = "read";
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR, StringCheckType.DOES_NOT_CONTAIN, expectedValue);

            JsonObject userInfo = createSampleJsonObjectBuilder().add(KEY_TO_LOOK_FOR, "lines " + expectedValue + " lines").build();

            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + userInfo.toString();
            try {
                exp.validate(TEST_ACTION, content);
                fail("Should have failed to validate the content, but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*Was not expecting to find.*" + expectedValue);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_key_checkType_expectedValue() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR, StringCheckType.CONTAINS_REGEX, "[0-9]+");

            JsonObject userInfo = createSampleJsonObjectBuilder().add(KEY_TO_LOOK_FOR, "look! numbers! 123").build();

            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + userInfo.toString();
            exp.validate(TEST_ACTION, content);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_key_valueType_contentMissingKey() {
        try {
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(KEY_TO_LOOK_FOR, ValueType.ARRAY);

            JsonObject userInfo = createSampleJsonObjectBuilder().build();

            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + userInfo.toString();
            try {
                exp.validate(TEST_ACTION, content);
                fail("Should have failed to validate the content, but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*content does not contain.*" + KEY_TO_LOOK_FOR);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_key_valueType_valueIsNotCorrectType() {
        try {
            String keyToLookFor = KEY_TO_LOOK_FOR;
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(keyToLookFor, ValueType.NULL);

            JsonObject userInfo = createSampleJsonObjectBuilder().add(keyToLookFor, 741776).build();

            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + userInfo.toString();
            try {
                exp.validate(TEST_ACTION, content);
                fail("Should have failed to validate the content, but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*ValueType of the value.*did not match the expected type");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_key_valueType_expectedValue_valueIsNotCorrectType() {
        try {
            String keyToLookFor = KEY_TO_LOOK_FOR;
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(keyToLookFor, ValueType.ARRAY, "array");

            JsonObject userInfo = createSampleJsonObjectBuilder().add(keyToLookFor, "string").build();

            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + userInfo.toString();
            try {
                exp.validate(TEST_ACTION, content);
                fail("Should have failed to validate the content, but did not.");
            } catch (Throwable e) {
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*ValueType of the value.*did not match the expected type");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_validate_key_valueType_valueDoesNotMatchExpectedValue() {
        try {
            String keyToLookFor = KEY_TO_LOOK_FOR;
            UserInfoJsonExpectation exp = new UserInfoJsonExpectation(keyToLookFor, ValueType.ARRAY, "array");

            JsonObject userInfo = createSampleJsonObjectBuilder().add(keyToLookFor, Json.createArrayBuilder().build()).build();

            final String content = UserInfoJsonExpectation.SERVLET_OUTPUT_PREFIX + userInfo.toString();
            mockery.checking(new Expectations() {
                {
                    one(webResponse).getContentAsString();
                    will(returnValue(content));
                }
            });
            try {
                exp.validate(TEST_ACTION, webResponse);
                fail("Should have failed to validate the content, but did not.");
            } catch (Throwable e) {
                // Even though the expected ValueType is an array and the expected value is NOT an array, the actual value will still be verified
                // against the expected value
                verifyException(e, Pattern.quote(exp.getFailureMsg()) + ".*Value for.*" + keyToLookFor + ".*did not match the expected value");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** Helper methods **************************************/

    protected void verifyExpectationValues(UserInfoJsonExpectation testExp, String expSearchKey, ValueType expValueType, CheckType expCheckType, Object expValue, String expFailureMsg) {
        assertEquals("Search location did not match expected value.", UserInfoJsonExpectation.SEARCH_LOCATION, testExp.getSearchLocation());
        assertEquals("Validation key did not match expected value.", expSearchKey, testExp.getValidationKey());
        assertEquals("Value type did not match expected value.", expValueType, testExp.getExpectedValueType());
        assertEquals("Check type did not match expected value.", expCheckType, testExp.getExpectedCheckType());
        assertEquals("(String) Check type did not match expected value.", null, testExp.getCheckType());
        assertEquals("Expected (string) value did not match expected value.", null, testExp.getValidationValue());
        assertEquals("Expected (object) value did not match expected value.", expValue, testExp.getExpectedValue());
        assertEquals("Failure message did not match expected value.", expFailureMsg, testExp.getFailureMsg());
    }

    private JsonObjectBuilder createSampleJsonObjectBuilder() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("sub", "sub value");
        builder.add("number", 0.1123581321);
        builder.add("boolean", false);
        builder.add("null", JsonValue.NULL);
        builder.add("object", Json.createObjectBuilder().add("string", "stringVal").add("number", 42));
        builder.add("array", Json.createArrayBuilder().add("R").add("E").add("S").add("P").add("E").add("C").add("T"));
        return builder;

    }

}
