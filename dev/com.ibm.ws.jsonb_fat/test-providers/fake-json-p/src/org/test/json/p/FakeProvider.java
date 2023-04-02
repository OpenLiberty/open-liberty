/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package org.test.json.p;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonPointer;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;

/**
 * A fake JSON provider that delegates to Parsson.
 * This is in lieu of a third party json provider which at the time or creation did not exist.
 * TODO: If Johnzon implements JSON 2.1 this can be replaced to confirm compatibility.
 */
public class FakeProvider extends JsonProvider {
    private static final JsonProvider json;

    static {
        try {
            json = (JsonProvider) Class.forName("org.eclipse.parsson.JsonProviderImpl").getConstructor().newInstance(null);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public JsonArrayBuilder createArrayBuilder() {
        return json.createArrayBuilder();
    }

    @Override
    public JsonBuilderFactory createBuilderFactory(Map<String, ?> config) {
        return json.createBuilderFactory(config);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder() {
        return json.createObjectBuilder();
    }

    @Override
    public JsonReader createReader(Reader reader) {
        return json.createReader(reader);
    }

    @Override
    public JsonReader createReader(InputStream in) {
        return json.createReader(in);
    }

    @Override
    public JsonReaderFactory createReaderFactory(Map<String, ?> config) {
        return json.createReaderFactory(config);
    }

    @Override
    public JsonWriter createWriter(Writer writer) {
        return json.createWriter(writer);
    }

    @Override
    public JsonWriter createWriter(OutputStream out) {
        return json.createWriter(out);
    }

    @Override
    public JsonWriterFactory createWriterFactory(Map<String, ?> config) {
        return json.createWriterFactory(config);
    }

    @Override
    public JsonGenerator createGenerator(Writer writer) {
        return json.createGenerator(writer);
    }

    @Override
    public JsonGenerator createGenerator(OutputStream out) {
        return json.createGenerator(out);
    }

    @Override
    public JsonParser createParser(Reader reader) {
        return json.createParser(reader);
    }

    @Override
    public JsonParser createParser(InputStream in) {
        return json.createParser(in);
    }

    @Override
    public JsonParserFactory createParserFactory(Map<String, ?> config) {
        return json.createParserFactory(config);
    }

    @Override
    public JsonGeneratorFactory createGeneratorFactory(Map<String, ?> config) {
        return json.createGeneratorFactory(config);
    }

    @Override
    public JsonPatchBuilder createPatchBuilder() {
        return json.createPatchBuilder();
    }

    @Override
    public JsonPatchBuilder createPatchBuilder(JsonArray ja) {
        return json.createPatchBuilder(ja);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(JsonObject object) {
        return json.createObjectBuilder(object);
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(Map<String, ?> map) {
        return json.createObjectBuilder(map);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder(JsonArray array) {
        return json.createArrayBuilder(array);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder(Collection<?> collection) {
        return json.createArrayBuilder(collection);
    }

    @Override
    public JsonPointer createPointer(String jsonPointer) {
        return json.createPointer(jsonPointer);
    }

    @Override
    public JsonPatch createPatch(JsonArray array) {
        return json.createPatch(array);
    }

    @Override
    public JsonPatch createDiff(JsonStructure source, JsonStructure target) {
        return json.createDiff(source, target);
    }

    @Override
    public JsonMergePatch createMergePatch(JsonValue patch) {
        return json.createMergePatch(patch);
    }

    @Override
    public JsonMergePatch createMergeDiff(JsonValue source, JsonValue target) {
        return json.createMergeDiff(source, target);
    }

    @Override
    public JsonString createValue(String value) {
        return json.createValue(value);
    }

    @Override
    public JsonNumber createValue(int value) {
        return json.createValue(value);
    }

    @Override
    public JsonNumber createValue(long value) {
        return json.createValue(value);
    }

    @Override
    public JsonNumber createValue(double value) {
        return json.createValue(value);
    }

    @Override
    public JsonNumber createValue(BigInteger value) {
        return json.createValue(value);
    }

    @Override
    public JsonNumber createValue(BigDecimal value) {
        return json.createValue(value);
    }

    @Override
    public JsonNumber createValue(Number value) {
        return json.createValue(value);
    }

}