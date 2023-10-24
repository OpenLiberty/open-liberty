/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.json.Json;
import jakarta.json.JsonConfig;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonWriter;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JSONP21Servlet")
public class JSONP21Servlet extends FATServlet {

    /*
     * Exercise the API of Json.createValue(Number) using int, long, Double, BigInteger, BigDecimal, and Number.
     */
    @Test
    public void testJSONCreateValueForNumber(HttpServletRequest request, HttpServletResponse response) throws Exception {
        int intNum = Integer.MAX_VALUE;
        JsonNumber jsonNumber = Json.createValue((Number) intNum);
        assertEquals("Json.createValue(Number) did not create the correct JsonNumber value.", intNum, jsonNumber.intValue());

        long longNum = Long.MIN_VALUE;
        jsonNumber = Json.createValue((Number) longNum);
        assertEquals("Json.createValue(Number) did not create the correct JsonNumber value.", longNum, jsonNumber.longValue());

        Double doubleNum = Double.MAX_VALUE;
        jsonNumber = Json.createValue(doubleNum);
        assertEquals("Json.createValue(Number) did not create the correct JsonNumber value.", doubleNum, jsonNumber.doubleValue(), 0);

        BigInteger bigInteger = new BigInteger(String.valueOf(Long.MAX_VALUE));
        jsonNumber = Json.createValue((Number) bigInteger);
        assertTrue("Json.createValue(Number) did not create the correct JsonNumber value. Instead the value was: " + jsonNumber.bigIntegerValue(),
                   jsonNumber.bigIntegerValue().compareTo(bigInteger) == 0);

        BigDecimal bigDecimal = new BigDecimal(String.valueOf(Double.MIN_VALUE));
        jsonNumber = Json.createValue((Number) bigDecimal);
        assertTrue("Json.createValue(Number) did not create the correct JsonNumber value. Instead the value was: " + jsonNumber.bigDecimalValue(),
                   jsonNumber.bigDecimalValue().compareTo(bigDecimal) == 0);

        Number number = 1234;
        jsonNumber = Json.createValue(number);
        assertEquals("Json.createValue(Number) did not create the correct JsonNumber value.", jsonNumber.numberValue().intValue(), number.intValue());

    }

    /*
     * Validate the correct JsonParser.Event from the JSONParser.currentEvent() is returned at the start, during, and end of parsing JSON data.
     */
    @Test
    public void testJSONGetCurrentEventFromParser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonObject json = Json.createObjectBuilder()
                        .add("firstName", "Steve")
                        .add("lastName", "Watson")
                        .add("age", 45)
                        .add("phoneNumber", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                        .add("type", "office")
                                                        .add("number", "507-253-1234")))
                        .add("~/", "specialCharacters")
                        .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonWriter writer = Json.createWriter(baos);
        writer.writeObject(json);
        writer.close();

        JsonParser parser = Json.createParser(new ByteArrayInputStream(baos.toByteArray()));
        parser.next();
        Event event = parser.currentEvent();
        assertTrue("JsonParser.currentEvent() was: " + event.name() + ". Expected value is: " + Event.START_OBJECT,
                   event == Event.START_OBJECT);

        parser.skipObject();
        event = parser.currentEvent();
        assertTrue("JsonParser.currentEvent() was: " + event.name() + ". Expected value is: " + Event.END_OBJECT,
                   event == Event.END_OBJECT);

    }

    /*
     * Verify when a KeyStrategy is not provider the default behavior doesn't change over time.
     * Parsson's default is the same as `KeyStartegy.LAST`.
     */
    @Test
    public void testJsonConfigDuplicateKeyStrategyDefault(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonReaderFactory readerFactoryLast = Json.createReaderFactory(Map.of(JsonConfig.KEY_STRATEGY, JsonConfig.KeyStrategy.LAST));
        String jsonString = "{\"firstName\":\"John\","
                            + "\"firstName\":\"Steve\","
                            + "\"lastName\":\"Watson\","
                            + "\"age\":25,"
                            + "\"age\":45,"
                            + "\"phoneNumber\":"
                            + "["
                            + "{\"type\":\"office\","
                            + "\"number\":\"507-253-1234\"},"
                            + "{\"type\":\"home\","
                            + "\"number\":\"507-253-2468\"}"
                            + "],"
                            + "\"phoneNumber\":"
                            + "["
                            + "{\"type\":\"office\","
                            + "\"number\":\"507-253-4321\"}"
                            + "],"
                            + "\"~/\":\"specialCharacters\"}";
        JsonReader reader = readerFactoryLast.createReader(new StringReader(jsonString));
        JsonObject jsonResult = reader.readObject();
        assertEquals("Steve", jsonResult.getString("firstName"));
        assertEquals(45, jsonResult.getInt("age"));
        assertEquals("507-253-4321", jsonResult.getJsonArray("phoneNumber").getJsonObject(0).getString("number"));

    }

    /*
     * Verify that `KeyStrategy.LAST` returns the last occurrence of duplicate keys while reading JSON data.
     */
    @Test
    public void testJsonConfigDuplicateKeyStrategyLast(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonReaderFactory readerFactoryLast = Json.createReaderFactory(Map.of(JsonConfig.KEY_STRATEGY, JsonConfig.KeyStrategy.LAST));
        String jsonString = "{\"firstName\":\"John\","
                            + "\"firstName\":\"Steve\","
                            + "\"lastName\":\"Watson\","
                            + "\"age\":25,"
                            + "\"age\":45,"
                            + "\"phoneNumber\":"
                            + "["
                            + "{\"type\":\"office\","
                            + "\"number\":\"507-253-1234\"},"
                            + "{\"type\":\"home\","
                            + "\"number\":\"507-253-2468\"}"
                            + "],"
                            + "\"phoneNumber\":"
                            + "["
                            + "{\"type\":\"office\","
                            + "\"number\":\"507-253-4321\"}"
                            + "],"
                            + "\"~/\":\"specialCharacters\"}";
        JsonReader reader = readerFactoryLast.createReader(new StringReader(jsonString));
        JsonObject jsonResult = reader.readObject();
        assertEquals("Steve", jsonResult.getString("firstName"));
        assertEquals(45, jsonResult.getInt("age"));
        assertEquals("507-253-4321", jsonResult.getJsonArray("phoneNumber").getJsonObject(0).getString("number"));

    }

    /*
     * Verify that `KeyStrategy.FIRST` returns the first occurrence of duplicate keys while reading JSON data.
     */
    @Test
    public void testJsonConfigDuplicateKeyStrategyFirst(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonReaderFactory readerFactoryFirst = Json.createReaderFactory(Map.of(JsonConfig.KEY_STRATEGY, JsonConfig.KeyStrategy.FIRST));
        String jsonString = "{\"firstName\":\"John\","
                            + "\"firstName\":\"Steve\","
                            + "\"lastName\":\"Watson\","
                            + "\"age\":25,"
                            + "\"age\":45,"
                            + "\"phoneNumber\":"
                            + "["
                            + "{\"type\":\"office\","
                            + "\"number\":\"507-253-1234\"},"
                            + "{\"type\":\"home\","
                            + "\"number\":\"507-253-2468\"}"
                            + "],"
                            + "\"phoneNumber\":"
                            + "["
                            + "{\"type\":\"office\","
                            + "\"number\":\"507-253-4321\"}"
                            + "],"
                            + "\"~/\":\"specialCharacters\"}";
        JsonReader reader = readerFactoryFirst.createReader(new StringReader(jsonString));
        JsonObject jsonResult = reader.readObject();
        assertEquals("John", jsonResult.getString("firstName"));
        assertEquals(25, jsonResult.getInt("age"));
        assertEquals("507-253-1234", jsonResult.getJsonArray("phoneNumber").getJsonObject(0).getString("number"));

    }

    /*
     * Verify that `KeyStartegy.NONE` prevents duplicate entries while reading JSON data by throwing a JsonException.
     */
    @Test
    public void testJsonConfigDuplicateKeyStrategyNone(HttpServletRequest request, HttpServletResponse response) throws Exception {
        JsonReaderFactory readerFactoryNone = Json.createReaderFactory(Map.of(JsonConfig.KEY_STRATEGY, JsonConfig.KeyStrategy.NONE));
        String jsonString = "{\"firstName\":\"John\","
                            + "\"firstName\":\"Steve\","
                            + "\"lastName\":\"Watson\","
                            + "\"age\":25,"
                            + "\"age\":45,"
                            + "\"phoneNumber\":"
                            + "["
                            + "{\"type\":\"office\","
                            + "\"number\":\"507-253-1234\"},"
                            + "{\"type\":\"home\","
                            + "\"number\":\"507-253-2468\"}"
                            + "],"
                            + "\"phoneNumber\":"
                            + "["
                            + "{\"type\":\"office\","
                            + "\"number\":\"507-253-4321\"}"
                            + "],"
                            + "\"~/\":\"specialCharacters\"}";
        try {
            JsonReader reader = readerFactoryNone.createReader(new StringReader(jsonString));
            reader.read();
            //duplicates should be rejected.
            fail();
        } catch (JsonException ex) {
            //expected
        }
    }

    //Verify that calling close on a JsonGenerator multiple times does not cause data corruption
    //Fixed in 1.1.1: https://github.com/eclipse-ee4j/parsson/issues/20
    @Test
    public void testJsonGeneratorDuplicateClose() {
        StringWriter sw = new StringWriter();
        JsonGeneratorFactory factory = Json.createGeneratorFactory(Collections.emptyMap());

        //Create a generator and double close it
        try (JsonGenerator generator = factory.createGenerator(sw)) {
            generator.writeStartObject();
            generator.writeEnd();
            generator.close(); //Generator has been closed and buffer is flushed and returned to the pool
            assertEquals("{}", sw.toString()); //Expected result
        } //Closed again and a second reference to the same buffer is returned to the pool

        StringWriter sw1 = new StringWriter();
        StringWriter sw2 = new StringWriter();
        try (
                        JsonGenerator generator1 = factory.createGenerator(sw1); //Gets first reference to buffer
                        JsonGenerator generator2 = factory.createGenerator(sw2); //Gets second reference to buffer
        ) {
            //Both generators are writing to the same buffer
            generator1.writeStartObject();
            generator1.write("key", "value");

            generator2.writeStartArray();
            generator2.write("item");
            generator2.write("item2");

            generator1.write("key2", "value2");

            generator2.writeEnd();

            generator1.writeEnd();
        }

        assertEquals("{\"key\":\"value\",\"key2\":\"value2\"}", sw1.toString()); //Failing case: ["item","item2,]key2":"value2"}
        assertEquals("[\"item\",\"item2\"]", sw2.toString()); //Failing case: ["item","item2,]
    }

    //Verify that calling close on a JsonTokenizer (PASSON INTERNAL CLASS)
    //multiple times does not cause data corruption.
    //Only exposed via JsonParser
    //Fixed in JsonParser 1.1.0: https://github.com/eclipse-ee4j/parsson/issues/25
    //Fixed in JsonTokenizer 1.1.3: https://github.com/eclipse-ee4j/parsson/issues/77
    @Test
    public void testJsonTokenizerDuplicateClose() throws IOException {
        byte[] content = "[\"test\"]".getBytes();
        JsonProvider json = JsonProvider.provider();

        //First round will put 2 buffers back into pool
        //Second and Third rounds will end up using those 2 buffers and getting corrupted data.
        for (int i = 0; i < 3; i++) {
            try (InputStream in = new ByteArrayInputStream(content)) {

                //JsonParser uses JsonTokenizer to parse
                try (JsonParser parser = json.createParser(in)) {

                    JsonParser.Event firstEvent = parser.next();
                    assertEquals(JsonParser.Event.START_ARRAY, firstEvent);

                    while (parser.hasNext()) {
                        JsonParser.Event event = parser.next();
                        if (event == JsonParser.Event.START_OBJECT) {
                            JsonObject object = parser.getObject();
                            object.toString();
                        }
                    }

                    parser.close(); //Returns JsonTokenizer buffer to pool

                } // Returns JsonTokenizer buffer to pool again
            }
        }
    }
}
