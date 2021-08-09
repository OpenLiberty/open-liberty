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

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;

public class StreamJSONP extends AbstractJSONP {

    public void testJsonStream() {
    	String outputDir = System.getenv("X_LOG_DIR") + "/json_stream_test_data.js";
        generateJSON(outputDir);
        JsonParser parser = getJsonParser(outputDir);
        parseJson(parser);
        checkJsonData();
    }

    private void generateJSON(String fileLocation) {
        FileOutputStream os = createFileOutputStream(fileLocation);
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
    }
}