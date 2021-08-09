/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.providers;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.Test;

public class JsonPProviderTest {

    JsonPProvider provider = new JsonPProvider();

    @Test
    public void testMBRforJsonObject() throws Exception {
        InputStream stream = new ByteArrayInputStream("{\"foo\": \"bar\"}".getBytes());
        assertTrue(provider.isReadable(JsonObject.class, JsonObject.class, new Annotation[] {}, APPLICATION_JSON_TYPE));
        JsonObject obj = (JsonObject) provider.readFrom(Object.class, JsonObject.class, new Annotation[] {}, 
                                                        APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(), stream);
        assertTrue(obj.containsKey("foo"));
        assertEquals("bar", obj.getString("foo"));
    }

    @Test
    public void testMBWforJsonObject() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonObject obj = Json.createObjectBuilder().add("foo", "bar").build();
        assertTrue(provider.isWriteable(obj.getClass(), obj.getClass(), new Annotation[] {}, APPLICATION_JSON_TYPE));
        provider.writeTo(obj, obj.getClass(), obj.getClass(), new Annotation[] {}, APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(), stream);
        assertEquals("{\"foo\":\"bar\"}", new String(stream.toByteArray()));
    }

    @Test
    public void testMBRforJsonArray() throws Exception {
        InputStream stream = new ByteArrayInputStream("[\"foo\", \"bar\", \"baz\"]".getBytes());
        assertTrue(provider.isReadable(JsonArray.class, JsonArray.class, new Annotation[] {}, APPLICATION_JSON_TYPE));
        JsonArray arr = (JsonArray) provider.readFrom(Object.class, JsonArray.class, new Annotation[] {}, 
                                                      APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(), stream);
        assertNotNull(arr);
        assertEquals(3, arr.size());
        assertEquals("foo", ((JsonString)arr.get(0)).getString());
        assertEquals("bar", ((JsonString)arr.get(1)).getString());
        assertEquals("baz", ((JsonString)arr.get(2)).getString());
    }

    @Test
    public void testMBWforJsonArray() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonArray arr = Json.createArrayBuilder().add("foo").add("bar").add("baz").build();
        assertTrue(provider.isWriteable(arr.getClass(), arr.getClass(), new Annotation[] {}, APPLICATION_JSON_TYPE));
        provider.writeTo(arr, arr.getClass(), arr.getClass(), new Annotation[] {}, APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(), stream);
        assertEquals("[\"foo\",\"bar\",\"baz\"]", new String(stream.toByteArray()));
    }

    @Test
    public void testMBWforJsonString() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonString str = new JsonString() {

            @Override
            public ValueType getValueType() {
                return ValueType.STRING;
            }

            @Override
            public CharSequence getChars() {
                return "Hello World!";
            }

            @Override
            public String getString() {
                return "Hello World!";
            }
            
        };
        assertTrue(provider.isWriteable(str.getClass(), str.getClass(), new Annotation[] {}, APPLICATION_JSON_TYPE));
        provider.writeTo(str, str.getClass(), str.getClass(), new Annotation[] {}, APPLICATION_JSON_TYPE, new MultivaluedHashMap<>(), stream);
        assertEquals("\"Hello World!\"", new String(stream.toByteArray()));
    }
}
