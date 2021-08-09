/*******************************************************************************
 * Copyright (c) 2015,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.jsonp.fat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParserFactory;

public abstract class AbstractJSONP {
    private String jsonData = "";

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
        if (!expectedString.equals(jsonData))
            throw new RuntimeException("DEBUG: EXPECTED <" + expectedString + "> FOUND <" + jsonData + ">");
    }

    protected FileOutputStream createFileOutputStream(String fileLocation) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileLocation);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("AbstractJSONPServlet threw an unexpected FileNotFoundException.", e);
        }
        return fos;
    }

    protected FileInputStream createFileInputStream(String fileLocation) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileLocation);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("AbstractJSONPServlet threw an unexpected FileNotFoundException.", e);
        }
        return fis;
    }
}