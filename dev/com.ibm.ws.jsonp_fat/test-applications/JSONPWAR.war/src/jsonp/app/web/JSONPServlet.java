/*******************************************************************************
 * Copyright (c) 2014,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jsonp.app.web;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParserFactory;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import junit.framework.Assert;

@SuppressWarnings("serial")
@WebServlet("/JSONPServlet")
public class JSONPServlet extends FATServlet {
    private String jsonData = "";

    @Override
    protected void before() throws Exception {
        jsonData = "";
    }

    /**
     * Ensure that JsonObjectBuilder is functioning.
     */
    @Test
    public void testJsonBuild() {
        JsonObject value = Json.createObjectBuilder()
                        .add("firstName", "Steve")
                        .add("lastName", "Watson")
                        .add("age", 45)
                        .add("phoneNumber", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder().add("type", "office").add("number", "507-253-1234"))
                                        .add(Json.createObjectBuilder().add("type", "cell").add("number", "507-253-4321")))
                        .build();
        JsonParser parser = getJsonParser(value);
        parseJson(parser);
        checkJsonData();
    }

    /**
     * Ensure that JsonReader is functioning.
     */
    @Test
    public void testJsonRead() {
        ServletContext context = getServletContext();
        InputStream is = context.getResourceAsStream("/WEB-INF/json_read_test_data.js");
        JsonReader reader = Json.createReader(is);
        JsonObject jsonData = reader.readObject();
        JsonParser parser = getJsonParser(jsonData);
        parseJson(parser);
        checkJsonData();
    }

    /**
     * Ensure that JsonGenerator is functioning.
     */
    @Test
    public void testJsonStream() {
        String outputDir = System.getenv("X_LOG_DIR") + "/json_stream_test_data.js";
        FileOutputStream os = createFileOutputStream(outputDir);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(JsonGenerator.PRETTY_PRINTING, new Object());
        JsonGeneratorFactory factory = Json.createGeneratorFactory(props);
        JsonGenerator generator = factory.createGenerator(os);
        generator.writeStartObject()
                        .write("firstName", "Steve")
                        .write("lastName", "Watson")
                        .write("age", 45)
                        .writeStartArray("phoneNumber")
                        .writeStartObject()
                        .write("type", "office")
                        .write("number", "507-253-1234")
                        .writeEnd()
                        .writeStartObject()
                        .write("type", "cell")
                        .write("number", "507-253-4321")
                        .writeEnd()
                        .writeEnd()
                        .writeEnd();
        generator.close();

        JsonParser parser = getJsonParser(outputDir);
        parseJson(parser);
        checkJsonData();
    }

    /**
     * Ensure that JsonWriter is functioning.
     */
    @Test
    public void testJsonWrite() {
        InputStream originalInputStream = getServletContext().getResourceAsStream("/WEB-INF/json_read_test_data.js");
        JsonObject originalJsonData = readJsonFile(originalInputStream);

        // Write json file
        String outputDir = System.getenv("X_LOG_DIR") + "/json_write_test_data.js";
        FileOutputStream os = createFileOutputStream(outputDir);
        JsonWriter writer = Json.createWriter(os);
        writer.writeObject(originalJsonData);
        writer.close();

        FileInputStream newInputStream = createFileInputStream(outputDir);
        JsonObject newJsonData = readJsonFile(newInputStream);
        JsonParser parser = getJsonParser(newJsonData);
        parseJson(parser);
        checkJsonData();
    }

    private JsonObject readJsonFile(InputStream is) {
        JsonReader reader = Json.createReader(is);
        JsonObject value = reader.readObject();
        return value;
    }

    protected JsonParser getJsonParser(String fileLocation) {
        FileInputStream fis = createFileInputStream(fileLocation);
        JsonParserFactory jsonParserFactory = Json.createParserFactory(new HashMap<String, Object>());
        JsonParser parser = jsonParserFactory.createParser(fis);
        return parser;
    }

    protected JsonParser getJsonParser(JsonObject value) {
        JsonParserFactory jsonParserFactory = Json.createParserFactory(new HashMap<String, Object>());
        JsonParser parser = jsonParserFactory.createParser(value);
        return parser;
    }

    protected void parseJson(JsonParser parser) {

        Boolean startObjectOrArray = false;
        Boolean endObjectOrArray = false;
        while (parser.hasNext()) {
            Event event = parser.next();

            if (endObjectOrArray && (event.equals(Event.START_ARRAY) || event.equals(Event.START_OBJECT))) {
                logJsonElement(",");
            }
            endObjectOrArray = false;

            switch (event) {
                case START_ARRAY:
                    startObjectOrArray = true;
                    logJsonElement("[");
                    break;
                case END_ARRAY:
                    endObjectOrArray = true;
                    logJsonElement("]");
                    break;
                case START_OBJECT:
                    startObjectOrArray = true;
                    logJsonElement("{");
                    break;
                case END_OBJECT:
                    endObjectOrArray = true;
                    logJsonElement("}");
                    break;
                case VALUE_NUMBER:
                    if (parser.isIntegralNumber()) {
                        logJsonElement(Integer.toString(parser.getInt()));
                    } else {
                        logJsonElement(Long.toString(parser.getLong()));
                    }
                    break;
                case VALUE_FALSE:
                    logJsonElement("FALSE");
                    break;
                case VALUE_NULL:
                    logJsonElement("NULL");
                    break;
                case VALUE_TRUE:
                    logJsonElement("TRUE");
                    break;
                case KEY_NAME:
                    if (!startObjectOrArray) {
                        logJsonElement(",");
                    }
                    startObjectOrArray = false;

                    logJsonElement("\"" + parser.getString() + "\":");
                    break;
                case VALUE_STRING:
                    logJsonElement("\"" + parser.getString() + "\"");
                    break;
            }
        }
        //System.out.println("DEBUG: " + jsonData);
    }

    private void logJsonElement(String element) {
        System.out.println(element);
        jsonData = jsonData + element;
    }

    protected void checkJsonData() {
        String expectedString = "{\"firstName\":\"Steve\",\"lastName\":\"Watson\",\"age\":45,\"phoneNumber\":[{\"type\":\"office\",\"number\":\"507-253-1234\"},{\"type\":\"cell\",\"number\":\"507-253-4321\"}]}";
        Assert.assertEquals(expectedString, jsonData);
    }

    protected FileOutputStream createFileOutputStream(String fileLocation) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileLocation);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("AbstractJSONPServlet threw an unexpected FileNotFoundException.");
        }
        return fos;
    }

    protected FileInputStream createFileInputStream(String fileLocation) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileLocation);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Assert.fail("AbstractJSONPServlet threw an unexpected FileNotFoundException.");
        }
        return fis;
    }
}