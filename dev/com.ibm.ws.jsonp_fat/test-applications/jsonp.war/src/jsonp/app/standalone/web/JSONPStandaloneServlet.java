/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jsonp.app.standalone.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.servlet.annotation.WebServlet;

import org.junit.Before;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JSONPStandaloneServlet")
public class JSONPStandaloneServlet extends FATServlet {

    /**
     * Configuration properties for maximums while processing json objects
     */
    public static final String MAX_DEPTH = "org.eclipse.parsson.maxDepth";
    private static final String MAX_BIGINTEGER_SCALE = "org.eclipse.parsson.maxBigIntegerScale";
    private static final String MAX_BIGDECIMAL_LEN = "org.eclipse.parsson.maxBigDecimalLength";

    @Before
    public void clearProperties() {
        System.clearProperty(MAX_DEPTH);
        System.clearProperty(MAX_BIGINTEGER_SCALE);
        System.clearProperty(MAX_BIGDECIMAL_LEN);
    }

    ///// Big decimals used to verify scales and lengths

    // π as JsonNumber with 51 source characters
    private static final String Π_100 = "3.14159265358979323846264338327950288419716939937510582097494459230781640628620899862803482534211706";

    // π as JsonNumber with 500 source characters
    private static final String Π_500 = Π_100
                                        + "7982148086513282306647093844609550582231725359408128481117450284102701938521105559644622948954930381"
                                        + "9644288109756659334461284756482337867831652712019091456485669234603486104543266482133936072602491412"
                                        + "7372458700660631558817488152092096282925409171536436789259036001133053054882046652138414695194151160"
                                        + "9433057270365759591953092186117381932611793105118548074462379962749567351885752724891227938183011949";

    // π as JsonNumber with 501 source characters
    private static final String Π_501 = Π_500 + "1";

    // π as JsonNumber with 1100 source characters
    private static final String Π_1100 = Π_500
                                         + "1298336733624406566430860213949463952247371907021798609437027705392171762931767523846748184676694051"
                                         + "3200056812714526356082778577134275778960917363717872146844090122495343014654958537105079227968925892"
                                         + "3542019956112129021960864034418159813629774771309960518707211349999998372978049951059731732816096318"
                                         + "5950244594553469083026425223082533446850352619311881710100031378387528865875332083814206171776691473"
                                         + "0359825349042875546873115956286388235378759375195778185778053217122680661300192787661119590921642019"
                                         + "8938095257201065485863278865936153381827968230301952035301852968995773622599413891249721775283479131";

    // π as JsonNumber with 1101 source characters
    private static final String Π_1101 = Π_1100 + "5";

    //// MAX DEPTH TESTS /////

    /*
     * This nests an object inside an array.
     * Therefore, the stack depth will be double the input depth.
     */
    private static String createDeepNestedDoc(final int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < depth; i++) {
            sb.append("{ \"a\": [");
        }
        sb.append(" \"val\" ");
        for (int i = 0; i < depth; i++) {
            sb.append("]}");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Verify default configuration results in runtime exception
     */
    @Test
    public void testArrayNestingException() {
        String json = createDeepNestedDoc(500);
        JsonParser parser = Json.createParser(new StringReader(json));
        try {
            while (parser.hasNext()) {
                JsonParser.Event ev = parser.next();
                if (JsonParser.Event.START_ARRAY == ev) {
                    parser.getLocation();
                }
            }
            fail("Expected to catch exception, but instead passed.");
        } catch (RuntimeException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected exception to contain the maximum depth value of 1000 but instead got: " + e.getMessage(),
                       doesMessageContain(e.getMessage(), 1000));
        } catch (Exception e) {
            fail("Expected to catch RuntimeException, but instead caught: " + e);
        } finally {
            parser.close();
        }
    }

    /**
     * Verify default configuration can be overwritten
     */
    @Test
    public void testArrayNestingConfig() {
        String json = createDeepNestedDoc(500);
        System.setProperty(MAX_DEPTH, "1002");
        JsonParser parser = Json.createParser(new StringReader(json));
        try {
            while (parser.hasNext()) {
                JsonParser.Event ev = parser.next();
                if (JsonParser.Event.START_ARRAY == ev) {
                    parser.getLocation();
                }
            }
        } finally {
            parser.close();
        }
    }

    /**
     * Verify default configuration edge case
     */
    @Test
    public void testArrayNesting() {
        String json = createDeepNestedDoc(499);
        JsonParser parser = Json.createParser(new StringReader(json));
        try {
            while (parser.hasNext()) {
                JsonParser.Event ev = parser.next();
                if (JsonParser.Event.START_ARRAY == ev) {
                    parser.getLocation();
                }
            }
        } finally {
            parser.close();
        }
    }

    /**
     * Verify default configuration results in runtime exception
     */
    @Test
    public void testObjectNestingException() {
        String json = createDeepNestedDoc(500);
        JsonParser parser = Json.createParser(new StringReader(json));
        try {
            while (parser.hasNext()) {
                JsonParser.Event ev = parser.next();
                if (JsonParser.Event.START_OBJECT == ev) {
                    parser.getLocation();
                }
            }
            fail("Expected to catch exception, but instead passed.");
        } catch (RuntimeException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected exception to contain the exceeded depth value of 1000 but instead got: " + e.getMessage(),
                       doesMessageContain(e.getMessage(), 1000));
        } catch (Exception e) {
            fail("Expected to catch RuntimeException, but instead caught: " + e);
        } finally {
            parser.close();
        }

    }

    /**
     * Verify default configuration can be overwritten
     */
    @Test
    public void testObjectNestingConfiguration() {
        String json = createDeepNestedDoc(500);
        System.setProperty(MAX_DEPTH, "1002");
        JsonParser parser = Json.createParser(new StringReader(json));
        try {
            while (parser.hasNext()) {
                JsonParser.Event ev = parser.next();
                if (JsonParser.Event.START_OBJECT == ev) {
                    parser.getLocation();
                }
            }
        } finally {
            parser.close();
        }

    }

    /**
     * Verify default configuration edge case
     */
    @Test
    public void testObjectNesting() {
        String json = createDeepNestedDoc(499);
        JsonParser parser = Json.createParser(new StringReader(json));
        try {
            while (parser.hasNext()) {
                JsonParser.Event ev = parser.next();
                if (JsonParser.Event.START_OBJECT == ev) {
                    parser.getLocation();
                }
            }
        } finally {
            parser.close();
        }
    }

    ////MAX SCALE TESTS /////

    /**
     * Since jsonp-1.0 does not have the Json.createValue() method,
     * we have to surround the value in an object
     * read it, then deserialize the value.
     */
    private static class BigDecimalTestCase {
        @Override
        public String toString() {
            String serializedTruncated = serialized.length() > 100 ? serialized.substring(0, 50) + "..." + serialized.substring(serialized.length() - 50) : serialized;

            return "BigDecimalTestCase [value=" + value.toEngineeringString()
                   + ", key=" + key
                   + ", length=" + length
                   + ", serialized=" + serializedTruncated + "]";
        }

        public BigDecimal value;
        public String serialized;
        public String key;
        public String length;

        //Private constructor
        private BigDecimalTestCase() {
        }

        /**
         * Returns a json serialized BigDecimal in the form:
         * - 0.
         * - followed by 0s equal to the scale minus 1
         * - followed by 1
         *
         * The length of the big decimal will be the scale + 2 to
         * account for the leading "0."
         *
         * @param int the scale
         * @return a BigDecimalTestCase
         */
        static BigDecimalTestCase oneWithPositiveScale(int scale) {
            assertTrue(scale > 0);

            BigDecimalTestCase testCase = new BigDecimalTestCase();
            testCase.key = "one";
            testCase.value = BigDecimal.valueOf(1, scale);
            testCase.length = Integer.toString(testCase.value.toPlainString().length());
            testCase.serialized = "{ \"" + testCase.key + "\": " + testCase.value.toPlainString() + " }";

            System.out.println("Testcase: " + testCase.toString());

            // Verify BigDecimal
            assertEquals(scale, testCase.value.scale());
            assertEquals(BigInteger.ONE, testCase.value.unscaledValue());

            // Verify scale and value are maintained
            BigDecimal unserialized = new BigDecimal(testCase.value.toPlainString());
            assertEquals(testCase.value.scale(), unserialized.scale());
            assertEquals(testCase.value.unscaledValue(), unserialized.unscaledValue());

            return testCase;
        }

        /**
         * Returns a json serialized BigDecimal in the form:
         * - 1
         * - followed by 0s equal to the abs value of the scale
         *
         * The length of the big decimal will be the scale + 1 to
         * account for the leading "1"
         *
         * @param int the scale
         * @return a BigDecimalTestCase
         */
        static BigDecimalTestCase oneWithNegativeScale(int scale) {
            assertTrue(scale < 0);
            // BigDecimal shifts the unscaledValue when deserializing so 1E25 becomes 10E24.
            // This shifting happens between unscaledValues 1 to 999.
            // Avoid shifting by requiring a power divisible by 3 so assertions can be made.
            assertTrue(scale % 3 == 0);

            BigDecimalTestCase testCase = new BigDecimalTestCase();
            testCase.key = "one";
            testCase.value = BigDecimal.valueOf(1, scale);
            testCase.length = Integer.toString(testCase.value.toPlainString().length());
            testCase.serialized = "{ \"" + testCase.key + "\": " + testCase.value.toEngineeringString() + " }";

            System.out.println("Testcase: " + testCase.toString());

            // Verify BigDecimal
            assertEquals(BigInteger.ONE, testCase.value.unscaledValue());
            assertEquals(scale, testCase.value.scale());

            // Verify scale and value are maintained
            BigDecimal unserialized = new BigDecimal(testCase.value.toEngineeringString());
            assertEquals(testCase.value.unscaledValue(), unserialized.unscaledValue());
            assertEquals(testCase.value.scale(), unserialized.scale());

            return testCase;
        }
    }

    @Test
    public void testBigIntegerScaleBelowLimit() {
        BigDecimalTestCase testCase = BigDecimalTestCase.oneWithPositiveScale(25);

        StringReader sr = new StringReader(testCase.serialized);
        JsonReader reader = Json.createReader(sr);

        JsonValue value;
        try {
            value = reader.readObject().get(testCase.key);
        } finally {
            reader.close();
        }

        assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
        assertTrue(value instanceof JsonNumber);
        assertEquals(testCase.value.toBigInteger(), ((JsonNumber) value).bigIntegerValue());
        // Invalid to call bigIntegerValueExact() with a positive scale
    }

    @Test
    public void testBigIntegerScaleAboveLimit() {
        BigDecimalTestCase testCase = BigDecimalTestCase.oneWithPositiveScale(100001);

        // Allow BigDecimal length to exceed default maximum to allow us
        // to test BigDecimal scale maximum. This is easier to exploit in jsonp-1.1
        // and above where you can directly call Json.createValue() and bypass this check.
        System.setProperty(MAX_BIGDECIMAL_LEN, testCase.length);

        StringReader sr = new StringReader(testCase.serialized);
        JsonReader reader = Json.createReader(sr);

        JsonValue value;
        try {
            value = reader.readObject().get(testCase.key);
        } finally {
            reader.close();
        }

        assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
        assertTrue(value instanceof JsonNumber);

        BigInteger result;
        try {
            result = ((JsonNumber) value).bigIntegerValue();
            fail("Expected a BigDecimal scale of 100001 to have thrown an UnsupportedOperationException, instead got: " + result);
        } catch (UnsupportedOperationException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected the exception to have the exceeded decimal scale of 100001 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 100001));
            assertTrue("Expected the exception to have the maximum decimal scale of 100000 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 100000));
        } catch (Exception e) {
            fail("Expected to catch UnsupportedOperationException, instead caught: " + e);
        }

        try {
            // UnsupportedOperationException is thrown before any call to BigDecimal
            // which would have resulted in an ArithmeticException
            result = ((JsonNumber) value).bigIntegerValueExact();
            fail("Expected a BigDecimal scale of 100001 to have thrown an UnsupportedOperationException, instead got: " + result);
        } catch (UnsupportedOperationException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected the exception to have the exceeded decimal scale of 100001 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 100001));
            assertTrue("Expected the exception to have the maximum decimal scale of 100000 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 100000));
        } catch (Exception e) {
            fail("Expected to catch UnsupportedOperationException, instead caught: " + e);
        }
    }

    @Test
    public void testBigIntegerScaleBelowNegLimit() {
        BigDecimalTestCase testCase = BigDecimalTestCase.oneWithNegativeScale(-24);

        StringReader sr = new StringReader(testCase.serialized);
        JsonReader reader = Json.createReader(sr);

        JsonValue value;
        try {
            value = reader.readObject().get(testCase.key);
        } finally {
            reader.close();
        }

        assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
        assertTrue(value instanceof JsonNumber);
        assertEquals(testCase.value.toBigInteger(), ((JsonNumber) value).bigIntegerValue());
        assertEquals(testCase.value.toBigIntegerExact(), ((JsonNumber) value).bigIntegerValueExact());
    }

    @Test
    public void testBigIntegerScaleAboveNegLimit() {
        BigDecimalTestCase testCase = BigDecimalTestCase.oneWithNegativeScale(-100002);

        // Allow BigDecimal length to exceed default maximum to allow us
        // to test BigDecimal scale maximum. This is easier to exploit in jsonp-1.1
        // and above where you can directly call Json.createValue() and bypass this check.
        System.setProperty(MAX_BIGDECIMAL_LEN, testCase.length);

        StringReader sr = new StringReader(testCase.serialized);
        JsonReader reader = Json.createReader(sr);

        JsonValue value;
        try {
            value = reader.readObject().get(testCase.key);
        } finally {
            reader.close();
        }

        assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
        assertTrue(value instanceof JsonNumber);

        BigInteger result;
        try {
            result = ((JsonNumber) value).bigIntegerValue();
            fail("Expected a BigDecimal scale of -100002 to have thrown an UnsupportedOperationException, instead got: " + result);
        } catch (UnsupportedOperationException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected the exception to have the exceeded decimal scale of 100002 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 100002));
            assertTrue("Expected the exception to have the maximum decimal scale of 100000 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 100000));
        } catch (Exception e) {
            fail("Expected to catch UnsupportedOperationException, instead caught: " + e);
        }

        try {
            result = ((JsonNumber) value).bigIntegerValueExact();
            fail("Expected a BigDecimal scale of -100002 to have thrown an UnsupportedOperationException, instead got: " + result);
        } catch (UnsupportedOperationException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected the exception to have the exceeded decimal scale of 100002 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 100002));
            assertTrue("Expected the exception to have the maximum decimal scale of 100000 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 100000));
        } catch (Exception e) {
            fail("Expected to catch UnsupportedOperationException, instead caught: " + e);
        }
    }

    @Test
    public void testBigIntegerScaleBelowConfiguredLimit() {
        System.setProperty(MAX_BIGINTEGER_SCALE, "50");

        BigDecimalTestCase testCase = BigDecimalTestCase.oneWithPositiveScale(25);

        StringReader sr = new StringReader(testCase.serialized);
        JsonReader reader = Json.createReader(sr);

        JsonValue value;
        try {
            value = reader.readObject().get(testCase.key);
        } finally {
            reader.close();
        }

        assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
        assertTrue(value instanceof JsonNumber);
        assertEquals(testCase.value.toBigInteger(), ((JsonNumber) value).bigIntegerValue());
        // Invalid to call bigIntegerValueExact() with a positive scale
    }

    @Test
    public void testBigIntegerScaleAboveConfiguredLimit() {
        System.setProperty(MAX_BIGINTEGER_SCALE, "50");

        BigDecimalTestCase testCase = BigDecimalTestCase.oneWithPositiveScale(51);

        StringReader sr = new StringReader(testCase.serialized);
        JsonReader reader = Json.createReader(sr);

        JsonValue value;
        try {
            value = reader.readObject().get(testCase.key);
        } finally {
            reader.close();
        }

        assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
        assertTrue(value instanceof JsonNumber);

        BigInteger result;
        try {
            result = ((JsonNumber) value).bigIntegerValue();
            fail("Expected a BigDecimal scale of 51 to have thrown an UnsupportedOperationException, instead got: " + result);
        } catch (UnsupportedOperationException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected the exception to have the exceeded decimal scale of 51 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 51));
            assertTrue("Expected the exception to have the maximum decimal scale of 50 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 50));
        } catch (Exception e) {
            fail("Expected to catch UnsupportedOperationException, instead caught: " + e);
        }

        try {
            // UnsupportedOperationException is thrown before any call to BigDecimal
            // which would have resulted in an ArithmeticException
            result = ((JsonNumber) value).bigIntegerValueExact();
            fail("Expected a BigDecimal scale of 51 to have thrown an UnsupportedOperationException, instead got: " + result);
        } catch (UnsupportedOperationException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected the exception to have the exceeded decimal scale of 51 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 51));
            assertTrue("Expected the exception to have the maximum decimal scale of 50 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 50));
        } catch (Exception e) {
            fail("Expected to catch UnsupportedOperationException, instead caught: " + e);
        }
    }

    @Test
    public void testBigIntegerScaleBelowNegConfiguredLimit() {
        System.setProperty(MAX_BIGINTEGER_SCALE, "50");

        BigDecimalTestCase testCase = BigDecimalTestCase.oneWithNegativeScale(-27);

        StringReader sr = new StringReader(testCase.serialized);
        JsonReader reader = Json.createReader(sr);

        JsonValue value;
        try {
            value = reader.readObject().get(testCase.key);
        } finally {
            reader.close();
        }

        assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
        assertTrue(value instanceof JsonNumber);
        assertEquals(testCase.value.toBigInteger(), ((JsonNumber) value).bigIntegerValue());
        assertEquals(testCase.value.toBigIntegerExact(), ((JsonNumber) value).bigIntegerValueExact());
    }

    @Test
    public void testBigIntegerScaleAboveNegConfiguredLimit() {
        System.setProperty(MAX_BIGINTEGER_SCALE, "50");

        BigDecimalTestCase testCase = BigDecimalTestCase.oneWithNegativeScale(-51);

        StringReader sr = new StringReader(testCase.serialized);
        JsonReader reader = Json.createReader(sr);

        JsonValue value;
        try {
            value = reader.readObject().get(testCase.key);
        } finally {
            reader.close();
        }

        assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
        assertTrue(value instanceof JsonNumber);

        BigInteger result;
        try {
            result = ((JsonNumber) value).bigIntegerValue();
            fail("Expected a BigDecimal scale of -51 to have thrown an UnsupportedOperationException, instead got: " + result);
        } catch (UnsupportedOperationException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected the exception to have the exceeded decimal scale of 51 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 51));
            assertTrue("Expected the exception to have the maximum decimal scale of 50 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 50));
        } catch (Exception e) {
            fail("Expected to catch UnsupportedOperationException, instead caught: " + e);
        }

        try {
            result = ((JsonNumber) value).bigIntegerValueExact();
            fail("Expected a BigDecimal scale of -51 to have thrown an UnsupportedOperationException, instead got: " + result);
        } catch (UnsupportedOperationException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected the exception to have the exceeded decimal scale of 51 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 51));
            assertTrue("Expected the exception to have the maximum decimal scale of 50 but instead got:" + e.getMessage(),
                       doesMessageContain(e.getMessage(), 50));
        } catch (Exception e) {
            fail("Expected to catch UnsupportedOperationException, instead caught: " + e);
        }
    }

    ////MAX LENGTH TESTS /////

    /*
     * Since jsonp-1.0 doesn't have the JsonReader.readValue() method,
     * we have to surround the value in an object
     * read it, then deserialize the value.
     */
    private String piAsJsonObject(String value) {
        return "{ \"pi\": " + value + "}";
    }

    // Verify BigDecimal length limit at default max
    @Test
    public void testLargeBigDecimalAtLimit() {
        StringReader sr = new StringReader(piAsJsonObject(Π_1100));
        JsonReader reader = Json.createReader(sr);

        JsonValue value;
        try {
            value = reader.readObject().get("pi");
        } finally {
            reader.close();
        }

        assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
        assertTrue(value instanceof JsonNumber);
        assertEquals(new BigDecimal(Π_1100), ((JsonNumber) value).bigDecimalValue());
    }

    // Verify BigDecimal length limit above default max
    @Test
    public void testLargeBigDecimalAboveDefaultLimit() {
        StringReader sr = new StringReader(piAsJsonObject(Π_1101));
        JsonReader reader = Json.createReader(sr);

        try {
            reader.readObject().get("pi");
            fail("No exception was thrown from BigDecimal parsing with source characters array length over limit");
        } catch (UnsupportedOperationException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected the exception to have the exceeded decimal length 1101 but instead got: " + e.getMessage(),
                       e.getMessage().contains("1101"));
            assertTrue("Expected the exception to have the maximum decimal length 1100 but instead got: " + e.getMessage(),
                       e.getMessage().contains("1100"));
        } catch (Exception e) {
            fail("Expected to catch UnsupportedOperationException, instead caught: " + e);
        } finally {
            reader.close();
        }
    }

    // Verify BigDecimal length limit at configured max
    @Test
    public void testLargeBigDecimalAtConfiguredLimit() {
        System.setProperty(MAX_BIGDECIMAL_LEN, "500");

        StringReader sr = new StringReader(piAsJsonObject(Π_500));
        JsonReader reader = Json.createReader(sr);

        JsonValue value;
        try {
            value = reader.readObject().get("pi");
        } finally {
            reader.close();
        }

        assertEquals(JsonValue.ValueType.NUMBER, value.getValueType());
        assertTrue(value instanceof JsonNumber);
        assertEquals(new BigDecimal(Π_500), ((JsonNumber) value).bigDecimalValue());
    }

    // Verify BigDecimal length limit above configured max
    @Test
    public void testLargeBigDecimalAboveConfiguredLimit() {
        System.setProperty(MAX_BIGDECIMAL_LEN, "500");

        StringReader sr = new StringReader(piAsJsonObject(Π_501));
        JsonReader reader = Json.createReader(sr);
        try {
            reader.readObject().get("pi");
            fail("No exception was thrown from BigDecimal parsing with source characters array length over limit");
        } catch (UnsupportedOperationException e) {
            //Make sure we didn't catch the wrong exception, but don't verify the actual message since it might change in future versions.
            assertTrue("Expected the exception to have the exceeded decimal length 501 but instead got: " + e.getMessage(),
                       e.getMessage().contains("501"));
            assertTrue("Expected the exception to have the maximum decimal length 500 but instead got: " + e.getMessage(),
                       e.getMessage().contains("500"));
        } catch (Exception e) {
            fail("Expected to catch UnsupportedOperationException, instead caught: " + e);
        } finally {
            reader.close();
        }
    }

    /*
     * Glassfish did not format numbers in messages, Parsson does and therefore we must account for that when we assert
     * values in exception messages.
     * TODO consider update Glassfish messages to also format numbers
     */
    private static boolean doesMessageContain(String message, Integer value) {
        return message.contains(value.toString()) || message.contains(NumberFormat.getInstance().format(value));
    }
}