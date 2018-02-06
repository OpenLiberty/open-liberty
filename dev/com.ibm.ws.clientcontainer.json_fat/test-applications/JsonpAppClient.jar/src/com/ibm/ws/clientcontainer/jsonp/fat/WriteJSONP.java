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
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonParser;

public class WriteJSONP extends AbstractJSONP {

    public void testJsonWrite() {
        InputStream originalInputStream = ReadJSONP.class.getResourceAsStream("/META-INF/json_read_test_data.js");
        JsonObject originalJsonData = readJsonFile(originalInputStream);
        
        String outputDir = System.getenv("X_LOG_DIR") + "/json_write_test_data.js";
        writeJsonFile(outputDir, originalJsonData);
        FileInputStream newInputStream = createFileInputStream(outputDir);
        JsonObject newJsonData = readJsonFile(newInputStream);
        JsonParser parser = getJsonParser(newJsonData);
        parseJson(parser);
        checkJsonData();
    }

    private void writeJsonFile(String fileLocation, JsonObject value) {
        FileOutputStream os = createFileOutputStream(fileLocation);
        JsonWriter writer = Json.createWriter(os);
        writer.writeObject(value);
        writer.close();
    }

    private JsonObject readJsonFile(InputStream is) {
        JsonReader reader = Json.createReader(is);
        JsonObject value = reader.readObject();
        return value;
    }
}