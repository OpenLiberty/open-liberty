package com.ibm.ws.security.common.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class CommonConfigUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    final static String CWWKS6104W_CONFIG_REQUIRED_ATTRIBUTE_NULL = "CWWKS6104W";
    final static String CWWKS6105W_CONFIG_REQUIRED_ATTRIBUTE_NULL_WITH_CONFIG_ID = "CWWKS6105W";

    CommonConfigUtils utils = new CommonConfigUtils();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new CommonConfigUtils();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    /******************************************* getConfigAttribute *******************************************/

    @Test
    public void getConfigAttribute_emptyProps_nullKey() {
        try {
            String result = utils.getConfigAttribute(new HashMap<String, Object>(), null);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAttribute_emptyProps() {
        try {
            String result = utils.getConfigAttribute(new HashMap<String, Object>(), "requiredAttribute");
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAttribute_missingKey() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");

            String result = utils.getConfigAttribute(props, "requiredAttribute");
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAttribute_withKey_valueEmpty() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = "";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getConfigAttribute(props, chosenAttr);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAttribute_withKey_valueWhitespace() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = " \t\n\r";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getConfigAttribute(props, chosenAttr);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAttribute_withKey_valueNonEmpty() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = "Some value";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getConfigAttribute(props, chosenAttr);
            assertEquals("Value for " + chosenAttr + " property did not match expected value. Properties were: " + props, value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* getConfigAttributeWithDefaultValue *******************************************/

    @Test
    public void getConfigAttributeWithDefaultValue_emptyProps_nullKey() {
        try {
            String result = utils.getConfigAttributeWithDefaultValue(new HashMap<String, Object>(), null, "defaultValue");
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAttributeWithDefaultValue_emptyProps() {
        try {
            String defaultValue = "defaultValue";
            String result = utils.getConfigAttributeWithDefaultValue(new HashMap<String, Object>(), "requiredAttribute", defaultValue);
            assertEquals("Result should have been set to the default value provided.", defaultValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAttributeWithDefaultValue_missingKey() {
        try {
            String defaultValue = "defaultValue";
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");

            String result = utils.getConfigAttributeWithDefaultValue(props, "requiredAttribute", defaultValue);
            assertEquals("Result should have been set to the default value provided.", defaultValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAttributeWithDefaultValue_withKey_valueEmpty() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = "";
            String defaultValue = "defaultValue";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getConfigAttributeWithDefaultValue(props, chosenAttr, defaultValue);
            assertEquals("Result should have been set to the default value provided.", defaultValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAttributeWithDefaultValue_withKey_valueWhitespace() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = " \t\n\r";
            String defaultValue = "defaultValue";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getConfigAttributeWithDefaultValue(props, chosenAttr, defaultValue);
            assertEquals("Result should have been set to the default value provided.", defaultValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getConfigAttributeWithDefaultValue_withKey_valueNonEmpty() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = "Some value";
            String defaultValue = "defaultValue";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getConfigAttributeWithDefaultValue(props, chosenAttr, defaultValue);
            assertEquals("Result should equal the value set in properties (" + value + ") and not the default value (" + defaultValue + ").", value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getRequiredConfigAttribute **************************************/

    @Test
    public void getRequiredConfigAttribute_emptyProps_nullKey() {
        try {
            String result = utils.getRequiredConfigAttribute(new HashMap<String, Object>(), null);
            assertNull("Result should have been null but was [" + result + "].", result);

            // Shouldn't emit log message for null key
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttribute_emptyProps() {
        try {
            String attr = "requiredAttribute";
            String result = utils.getRequiredConfigAttribute(new HashMap<String, Object>(), attr);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyMissingRequiredAttributes(outputMgr, attr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttribute_missingKey() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            String attr = "requiredAttribute";

            String result = utils.getRequiredConfigAttribute(props, attr);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyMissingRequiredAttributes(outputMgr, attr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttribute_withKey_valueEmpty() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = "";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getRequiredConfigAttribute(props, chosenAttr);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyMissingRequiredAttributes(outputMgr, chosenAttr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttribute_withKey_valueWhitespace() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = " \t\n\r";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getRequiredConfigAttribute(props, chosenAttr);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyMissingRequiredAttributes(outputMgr, chosenAttr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttribute_withKey_valueGood() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = "Some value";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getRequiredConfigAttribute(props, chosenAttr);
            assertEquals("Value for " + chosenAttr + " property did not match expected value. Properties were: " + props, value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getRequiredConfigAttributeWithDefaultValue **************************************/

    @Test
    public void getRequiredConfigAttributeWithDefaultValue_emptyProps_nullKey() {
        try {
            String result = utils.getRequiredConfigAttributeWithDefaultValue(new HashMap<String, Object>(), null, "defaultValue");
            assertNull("Result should have been null but was [" + result + "].", result);

            // Shouldn't emit log message for null key
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttributeWithDefaultValue_emptyProps() {
        try {
            String defaultValue = "defaultValue";
            String attr = "requiredAttribute";
            String result = utils.getRequiredConfigAttributeWithDefaultValue(new HashMap<String, Object>(), attr, defaultValue);
            assertEquals("Result should have been set to the default value provided.", defaultValue, result);

            // Shouldn't emit log message because default value is provided
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttributeWithDefaultValue_missingKey() {
        try {
            String defaultValue = "defaultValue";
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            String attr = "requiredAttribute";

            String result = utils.getRequiredConfigAttributeWithDefaultValue(props, attr, defaultValue);
            assertEquals("Result should have been set to the default value provided.", defaultValue, result);

            // Shouldn't emit log message because default value is provided
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttributeWithDefaultValue_withKey_valueEmpty() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = "";
            String defaultValue = "defaultValue";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getRequiredConfigAttributeWithDefaultValue(props, chosenAttr, defaultValue);
            assertEquals("Result should have been set to the default value provided.", defaultValue, result);

            // Shouldn't emit log message because default value is provided
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttributeWithDefaultValue_withKey_valueWhitespace() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = " \t\n\r";
            String defaultValue = "defaultValue";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getRequiredConfigAttributeWithDefaultValue(props, chosenAttr, defaultValue);
            assertEquals("Result should have been set to the default value provided.", defaultValue, result);

            // Shouldn't emit log message because default value is provided
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttributeWithDefaultValue_withKey() {
        try {
            String chosenAttr = "requiredAttribute";
            String value = "Some value";
            String defaultValue = "defaultValue";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getRequiredConfigAttributeWithDefaultValue(props, chosenAttr, defaultValue);
            assertEquals("Result should equal the value set in properties (" + value + ") and not the default value (" + defaultValue + ").", value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getRequiredConfigAttributeWithConfigId **************************************/

    @Test
    public void getRequiredConfigAttributeWithConfigId_emptyProps_nullKey() {
        try {
            String configId = RandomUtils.getRandomSelection(null, "", "someConfigId");
            String result = utils.getRequiredConfigAttributeWithConfigId(new HashMap<String, Object>(), null, configId);
            assertNull("Result should have been null but was [" + result + "].", result);

            // Shouldn't emit log message for null key
            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttributeWithConfigId_emptyProps() {
        try {
            String configId = RandomUtils.getRandomSelection(null, "", "someConfigId");
            String attr = "requiredAttribute";
            String result = utils.getRequiredConfigAttributeWithConfigId(new HashMap<String, Object>(), attr, configId);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyMissingRequiredAttributesWithConfigId(outputMgr, attr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttributeWithConfigId_missingKey() {
        try {
            String configId = RandomUtils.getRandomSelection(null, "", "someConfigId");
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            String attr = "requiredAttribute";

            String result = utils.getRequiredConfigAttributeWithConfigId(props, attr, configId);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyMissingRequiredAttributesWithConfigId(outputMgr, attr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttributeWithConfigId_withKey_valueEmpty() {
        try {
            String configId = RandomUtils.getRandomSelection(null, "", "someConfigId");
            String chosenAttr = "requiredAttribute";
            String value = "";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getRequiredConfigAttributeWithConfigId(props, chosenAttr, configId);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyMissingRequiredAttributesWithConfigId(outputMgr, chosenAttr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttributeWithConfigId_withKey_valueWhitespace() {
        try {
            String configId = RandomUtils.getRandomSelection(null, "", "someConfigId");
            String chosenAttr = "requiredAttribute";
            String value = " \t\n\r";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getRequiredConfigAttributeWithConfigId(props, chosenAttr, configId);
            assertNull("Result should have been null but was [" + result + "].", result);

            verifyMissingRequiredAttributesWithConfigId(outputMgr, chosenAttr);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getRequiredConfigAttributeWithConfigId_withKey_valueGood() {
        try {
            String configId = RandomUtils.getRandomSelection(null, "", "someConfigId");
            String chosenAttr = "requiredAttribute";
            String value = "Some value";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, value);

            String result = utils.getRequiredConfigAttributeWithConfigId(props, chosenAttr, configId);
            assertEquals("Value for " + chosenAttr + " property did not match expected value. Properties were: " + props, value, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getBooleanConfigAttribute **************************************/

    @Test
    public void getBooleanConfigAttribute_emptyProps_falseDefault() {
        try {
            String chosenAttr = "requiredAttribute";

            boolean result = utils.getBooleanConfigAttribute(new HashMap<String, Object>(), chosenAttr, false);
            assertFalse("Result should have been false for an empty props map and false default value.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBooleanConfigAttribute_emptyProps_trueDefault() {
        try {
            String chosenAttr = "requiredAttribute";

            boolean result = utils.getBooleanConfigAttribute(new HashMap<String, Object>(), chosenAttr, true);
            assertTrue("Result should have been true for an empty props map and true default value.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBooleanConfigAttribute_nullKey_falseDefault() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");

            boolean result = utils.getBooleanConfigAttribute(props, null, false);
            assertFalse("Result should have been false for null key provided and false default value.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBooleanConfigAttribute_nullKey_trueDefault() {
        try {
            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");

            boolean result = utils.getBooleanConfigAttribute(props, null, true);
            assertTrue("Result should have been true for null key provided and true default value.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBooleanConfigAttribute_missingProp_falseDefault() {
        try {
            String chosenAttr = "requiredAttribute";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");

            boolean result = utils.getBooleanConfigAttribute(props, chosenAttr, false);
            assertFalse("Result should have been false for props map missing the specified key and false default value.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBooleanConfigAttribute_missingProp_trueDefault() {
        try {
            String chosenAttr = "requiredAttribute";

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");

            boolean result = utils.getBooleanConfigAttribute(props, chosenAttr, true);
            assertTrue("Result should have been true for props map missing the specified key and true default value.", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getBooleanConfigAttribute_withProp_oppositeDefault() {
        try {
            String chosenAttr = "requiredAttribute";
            boolean expectedValue = RandomUtils.getRandomSelection(true, false);

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, expectedValue);

            boolean result = utils.getBooleanConfigAttribute(props, chosenAttr, !expectedValue);
            assertEquals("Result did not match the expected value.", expectedValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** getIntegerConfigAttribute **************************************/

    @Test
    public void getIntegerConfigAttribute_emptyProps() {
        try {
            String chosenAttr = "requiredAttribute";
            int defaultValue = RandomUtils.getRandomSelection(-1000, 0, 1, 1000);

            int result = utils.getIntegerConfigAttribute(new HashMap<String, Object>(), chosenAttr, defaultValue);
            assertEquals("Result should equalled default value for an empty props map.", defaultValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIntegerConfigAttribute_nullKey() {
        try {
            int defaultValue = RandomUtils.getRandomSelection(-1000, 0, 1, 1000);

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");

            int result = utils.getIntegerConfigAttribute(props, null, defaultValue);
            assertEquals("Result should equalled default value for null key provided.", defaultValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIntegerConfigAttribute_missingProp() {
        try {
            String chosenAttr = "requiredAttribute";
            int defaultValue = RandomUtils.getRandomSelection(-1000, 0, 1, 1000);

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");

            int result = utils.getIntegerConfigAttribute(props, chosenAttr, defaultValue);
            assertEquals("Result should equalled default value for props map missing the specified key.", defaultValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getIntegerConfigAttribute_withProp_differentDefaultValue() {
        try {
            String chosenAttr = "requiredAttribute";
            int expectedValue = RandomUtils.getRandomSelection(-1000, 0, 1, 1000);
            int defaultValue = (expectedValue + 5) * 2; // Make sure the default value is different than the expected value

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("key1", "value1");
            props.put("key2", "value2");
            props.put(chosenAttr, expectedValue);

            int result = utils.getIntegerConfigAttribute(props, chosenAttr, defaultValue);
            assertEquals("Result did not match the expected value.", expectedValue, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /************************************** trim **************************************/

    @Test
    public void trim_string_null() {
        try {
            String result = CommonConfigUtils.trim((String) null);
            assertNull("Trimmed result from null input should be null. Result was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void trim_string_empty() {
        try {
            String result = CommonConfigUtils.trim("");
            assertNull("Trimmed result from empty input should be null. Result was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void trim_string_allWhitespace() {
        try {
            String result = CommonConfigUtils.trim(" \n\t \r ");
            assertNull("Trimmed result from all whitespace input should be null. Result was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void trim_string_leadingAndTrailingWhitespace() {
        try {
            String expectedResult = "some   value\t.";
            String result = CommonConfigUtils.trim(" \n\t" + expectedResult + " \r ");
            assertEquals("Result from input with leading and trailing whitespace did not match expected value.", expectedResult, result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void trim_stringArray_null() {
        try {
            String[] result = CommonConfigUtils.trim((String[]) null);
            assertNull("Trimmed result from null input should be null. Result was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void trim_stringArray_empty() {
        try {
            String[] result = CommonConfigUtils.trim(new String[0]);
            assertNull("Trimmed result from empty array input should be null. Result was [" + result + "].", result);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void trim_stringArray_mixed() {
        try {
            String entry1 = "entry1";
            String entry2 = "entry,  \ntwo";
            String entry3 = "entry 3";
            String[] result = CommonConfigUtils.trim(new String[] { "  \n" + entry1, entry2, entry3 + "\t " });
            assertEquals("Result should have same number of entries as input. Result was [" + result + "].", 3, result.length);
            assertEquals("Result from input with leading whitespace did not match expected value.", entry1, result[0]);
            assertEquals("Result from input with interior whitespace did not match expected value.", entry2, result[1]);
            assertEquals("Result from input with trailing whitespace did not match expected value.", entry3, result[2]);

            verifyNoLogMessage(outputMgr, MSG_BASE);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /******************************************* Helper methods *******************************************/

    protected void verifyMissingRequiredAttributes(SharedOutputManager outputMgr, Map<String, Object> attributeMap) {
        verifyMissingRequiredAttributes(outputMgr, attributeMap.keySet().toArray(new String[attributeMap.keySet().size()]));
    }

    protected void verifyMissingRequiredAttributes(SharedOutputManager outputMgr, String... attributes) {
        for (String attr : attributes) {
            verifyLogMessageWithInserts(outputMgr, CWWKS6104W_CONFIG_REQUIRED_ATTRIBUTE_NULL, attr);
        }
    }

    protected void verifyMissingRequiredAttributesWithConfigId(SharedOutputManager outputMgr, String configId, String... attributes) {
        for (String attr : attributes) {
            verifyLogMessageWithInserts(outputMgr, CWWKS6105W_CONFIG_REQUIRED_ATTRIBUTE_NULL_WITH_CONFIG_ID, attr, configId);
        }
    }
}
