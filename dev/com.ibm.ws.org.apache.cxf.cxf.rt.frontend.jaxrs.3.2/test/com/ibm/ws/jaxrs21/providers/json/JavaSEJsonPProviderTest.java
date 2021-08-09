/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.providers.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Test;

/**
 * Tests that JsonPProvider will function in a Java SE environment
 */
public class JavaSEJsonPProviderTest {

    @Test
    public void testJsonPProvider() throws Exception {
        JsonPProvider provider = new JsonPProvider(null);
        ByteArrayInputStream in = new ByteArrayInputStream("{\"str\": \"foo\", \"num\": 123}".getBytes());
        JsonObject jsonObject = (JsonObject) provider.readFrom(null, null, null, null, null, in);
        assertNotNull(jsonObject);
        assertEquals("foo", jsonObject.getString("str"));
        assertEquals(123, jsonObject.getInt("num"));
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        provider.writeTo(jsonObject, null, null, null, null, null, out);
        String jsonOutput = out.toString();
        assertNotNull(jsonOutput);
        
        JsonReader jsonReader = Json.createReader(new StringReader(jsonOutput));
        JsonObject jsonObject2 = jsonReader.readObject();
        assertNotNull(jsonObject2);
        assertEquals("foo", jsonObject2.getString("str"));
        assertEquals(123, jsonObject2.getInt("num"));
    }
}
