/*
 * Copyright (c) 2011, 2024 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.json;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;

/**
 * Factory class for creating JSON processing objects.
 * This class provides the most commonly used methods for creating these
 * objects and their corresponding factories. The factory classes provide
 * all the various ways to create these objects.
 *
 * <p>
 * The methods in this class locate a provider instance using the method
 * {@link JsonProvider#provider()}. This class uses the provider instance
 * to create JSON processing objects.
 *
 * <p>
 * The following example shows how to create a JSON parser to parse
 * an empty array:
 * <pre>
 * <code>
 * StringReader reader = new StringReader("[]");
 * JsonParser parser = Json.createParser(reader);
 * </code>
 * </pre>
 *
 * <p>
 * All the methods in this class are safe for use by multiple concurrent
 * threads.
 */
public final class Json {

    /**
     * No instantiation.
     */
    private Json() {
    }

    /**
     * Creates a JSON parser from a character stream.
     *
     * @param reader i/o reader from which JSON is to be read
     * @return a JSON parser
     */
    public static JsonParser createParser(Reader reader) {
        return JsonProvider.provider().createParser(reader);
    }

    /**
     * Creates a JSON parser from a byte stream.
     * The character encoding of the stream is determined as specified in
     * <a href="http://tools.ietf.org/rfc/rfc7159.txt">RFC 7159</a>.
     *
     * @param in i/o stream from which JSON is to be read
     * @throws JsonException if encoding cannot be determined
     *         or i/o error (IOException would be cause of JsonException)
     * @return a JSON parser
     */
    public static JsonParser createParser(InputStream in) {
        return JsonProvider.provider().createParser(in);
    }

    /**
     * Creates a JSON generator for writing JSON to a character stream.
     *
     * @param writer a i/o writer to which JSON is written
     * @return a JSON generator
     */
    public static JsonGenerator createGenerator(Writer writer) {
        return JsonProvider.provider().createGenerator(writer);
    }

    /**
     * Creates a JSON generator for writing JSON to a byte stream.
     *
     * @param out i/o stream to which JSON is written
     * @return a JSON generator
     */
    public static JsonGenerator createGenerator(OutputStream out) {
        return JsonProvider.provider().createGenerator(out);
    }

    /**
     * Creates a parser factory for creating {@link JsonParser} objects.
     *
     * @return JSON parser factory.
     *
    public static JsonParserFactory createParserFactory() {
        return JsonProvider.provider().createParserFactory();
    }
     */

    /**
     * Creates a parser factory for creating {@link JsonParser} objects.
     * The factory is configured with the specified map of provider specific
     * configuration properties. Provider implementations should ignore any
     * unsupported configuration properties specified in the map.
     *
     * @param config a map of provider specific properties to configure the
     *               JSON parsers. The map may be empty or null
     * @return JSON parser factory
     */
    public static JsonParserFactory createParserFactory(Map<String, ?> config) {
        return JsonProvider.provider().createParserFactory(config);
    }

    /**
     * Creates a generator factory for creating {@link JsonGenerator} objects.
     *
     * @return JSON generator factory
     *
    public static JsonGeneratorFactory createGeneratorFactory() {
        return JsonProvider.provider().createGeneratorFactory();
    }
    */

    /**
     * Creates a generator factory for creating {@link JsonGenerator} objects.
     * The factory is configured with the specified map of provider specific
     * configuration properties. Provider implementations should ignore any
     * unsupported configuration properties specified in the map.
     *
     * @param config a map of provider specific properties to configure the
     *               JSON generators. The map may be empty or null
     * @return JSON generator factory
     */
    public static JsonGeneratorFactory createGeneratorFactory(
            Map<String, ?> config) {
        return JsonProvider.provider().createGeneratorFactory(config);
    }

    /**
     * Creates a JSON writer to write a
     * JSON {@link JsonObject object} or {@link JsonArray array}
     * structure to the specified character stream.
     *
     * @param writer to which JSON object or array is written
     * @return a JSON writer
     */
    public static JsonWriter createWriter(Writer writer) {
        return JsonProvider.provider().createWriter(writer);
    }

    /**
     * Creates a JSON writer to write a
     * JSON {@link JsonObject object} or {@link JsonArray array}
     * structure to the specified byte stream. Characters written to
     * the stream are encoded into bytes using UTF-8 encoding.
     *
     * @param out to which JSON object or array is written
     * @return a JSON writer
     */
    public static JsonWriter createWriter(OutputStream out) {
        return JsonProvider.provider().createWriter(out);
    }

    /**
     * Creates a JSON reader from a character stream.
     *
     * @param reader a reader from which JSON is to be read
     * @return a JSON reader
     */
    public static JsonReader createReader(Reader reader) {
        return JsonProvider.provider().createReader(reader);
    }

    /**
     * Creates a JSON reader from a byte stream. The character encoding of
     * the stream is determined as described in
     * <a href="http://tools.ietf.org/rfc/rfc7159.txt">RFC 7159</a>.
     *
     * @param in a byte stream from which JSON is to be read
     * @return a JSON reader
     */
    public static JsonReader createReader(InputStream in) {
        return JsonProvider.provider().createReader(in);
    }

    /**
     * Creates a reader factory for creating {@link JsonReader} objects.
     * The factory is configured with the specified map of provider specific
     * configuration properties. Provider implementations should ignore any
     * unsupported configuration properties specified in the map.
     *
     * @param config a map of provider specific properties to configure the
     *               JSON readers. The map may be empty or null
     * @return a JSON reader factory
     */
    public static JsonReaderFactory createReaderFactory(Map<String, ?> config) {
        return JsonProvider.provider().createReaderFactory(config);
    }

    /**
     * Creates a writer factory for creating {@link JsonWriter} objects.
     * The factory is configured with the specified map of provider specific
     * configuration properties. Provider implementations should ignore any
     * unsupported configuration properties specified in the map.
     *
     * @param config a map of provider specific properties to configure the
     *               JSON writers. The map may be empty or null
     * @return a JSON writer factory
     */
    public static JsonWriterFactory createWriterFactory(Map<String, ?> config) {
        return JsonProvider.provider().createWriterFactory(config);
    }

    /**
     * Creates a JSON array builder
     *
     * @return a JSON array builder
     */
    public static JsonArrayBuilder createArrayBuilder() {
        return JsonProvider.provider().createArrayBuilder();
    }

    /**
     * Creates a JSON array builder, initialized with the specified array
     *
     * @param array the initial array in the builder
     * @return a JSON array builder
     *
     * @since 1.1
     */
    public static JsonArrayBuilder createArrayBuilder(JsonArray array) {
        return JsonProvider.provider().createArrayBuilder(array);
    }

    /**
     * Creates a JSON array builder, initialized with the content of specified {@code collection}.
     * If the {@code collection} contains {@link Optional}s then resulting JSON array builder
     * contains the value from the {@code collection} only if the {@link Optional} is not empty.
     *
     * @param collection the initial data for the builder
     * @return a JSON array builder
     * @exception IllegalArgumentException if the value from the {@code collection} cannot be converted
     *            to the corresponding {@link JsonValue}
     *
     * @since 1.1
     */
    public static JsonArrayBuilder createArrayBuilder(Collection<?> collection) {
        return JsonProvider.provider().createArrayBuilder(collection);
    }

    /**
     * Creates a JSON object builder
     *
     * @return a JSON object builder
     */
    public static JsonObjectBuilder createObjectBuilder() {
        return JsonProvider.provider().createObjectBuilder();
    }

    /**
     * Creates a JSON object builder, initialized with the specified object.
     *
     * @param object the initial object in the builder
     * @return a JSON object builder
     *
     * @since 1.1
     */
    public static JsonObjectBuilder createObjectBuilder(JsonObject object) {
        return JsonProvider.provider().createObjectBuilder(object);
    }

    /**
     * Creates a JSON object builder, initialized with the data from specified {@code map}.
     * If the @{code map} contains {@link Optional}s then resulting JSON object builder
     * contains the key from the {@code map} only if the {@link Optional} is not empty.
     *
     * @param map the initial object in the builder
     * @return a JSON object builder
     * @exception IllegalArgumentException if the value from the {@code map} cannot be converted
     *            to the corresponding {@link JsonValue}
     *
     * @since 1.1
     */
    public static JsonObjectBuilder createObjectBuilder(Map<String, ?> map) {
        return JsonProvider.provider().createObjectBuilder(map);
    }

    /**
     * Creates JSON Pointer (<a href="http://tools.ietf.org/html/rfc6901">RFC 6901</a>)
     * from given {@code jsonPointer} string.
     * <ul>
     *     <li>An empty {@code jsonPointer} string defines a reference to the target itself.</li>
     *     <li>If the {@code jsonPointer} string is non-empty, it must be a sequence of '{@code /}' prefixed tokens.</li>
     * </ul>
     *
     * @param jsonPointer the valid escaped JSON Pointer string
     * @throws NullPointerException if {@code jsonPointer} is {@code null}
     * @throws JsonException if {@code jsonPointer} is not a valid JSON Pointer
     * @return a JSON Pointer
     *
     * @since 1.1
     */
    public static JsonPointer createPointer(String jsonPointer) {
        return JsonProvider.provider().createPointer(jsonPointer);
    }

    /**
     * Creates a JSON Patch builder (<a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>).
     *
     * @return a JSON Patch builder
     *
     * @since 1.1
     */
    public static JsonPatchBuilder createPatchBuilder() {
        return JsonProvider.provider().createPatchBuilder();
    }

    /**
     * Creates a JSON Patch builder
     * (<a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>),
     * initialized with the specified operations.
     *
     * @param array the initial patch operations
     * @return a JSON Patch builder
     *
     * @since 1.1
     */
    public static JsonPatchBuilder createPatchBuilder(JsonArray array) {
        return JsonProvider.provider().createPatchBuilder(array);
    }

    /**
     * Creates a JSON Patch (<a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>)
     * from the specified operations.
     *
     * @param array patch operations
     * @return a JSON Patch
     *
     * @since 1.1
     */
    public static JsonPatch createPatch(JsonArray array) {
        return JsonProvider.provider().createPatch(array);
    }

    /**
     * Generates a JSON Patch (<a href="http://tools.ietf.org/html/rfc6902">RFC 6902</a>)
     * from the source and target {@code JsonStructure}.
     * The generated JSON Patch need not be unique.
     *
     * @param source the source
     * @param target the target, must be the same type as the source
     * @return a JSON Patch which when applied to the source, yields the target
     *
     * @since 1.1
     */
    public static JsonPatch createDiff(JsonStructure source, JsonStructure target) {
        return JsonProvider.provider().createDiff(source, target);
    }

    /**
     * Creates JSON Merge Patch (<a href="http://tools.ietf.org/html/rfc7396">RFC 7396</a>)
     * from specified {@code JsonValue}.
     *
     * @param patch the patch
     * @return a JSON Merge Patch
     *
     * @since 1.1
     */
    public static JsonMergePatch createMergePatch(JsonValue patch) {
        return JsonProvider.provider().createMergePatch(patch);
    }

    /**
     * Generates a JSON Merge Patch (<a href="http://tools.ietf.org/html/rfc7396">RFC 7396</a>)
     * from the source and target {@code JsonValue}s
     * which when applied to the {@code source}, yields the {@code target}.
     *
     * @param source the source
     * @param target the target
     * @return a JSON Merge Patch
     *
     * @since 1.1
     */
    public static JsonMergePatch createMergeDiff(JsonValue source, JsonValue target) {
        return JsonProvider.provider().createMergeDiff(source, target);
    }

    /**
     * Creates a builder factory for creating {@link JsonArrayBuilder}
     * and {@link JsonObjectBuilder} objects.
     * The factory is configured with the specified map of provider specific
     * configuration properties. Provider implementations should ignore any
     * unsupported configuration properties specified in the map.
     *
     * @param config a map of provider specific properties to configure the
     *               JSON builders. The map may be empty or null
     * @return a JSON builder factory
     */
    public static JsonBuilderFactory createBuilderFactory(
            Map<String, ?> config) {
        return JsonProvider.provider().createBuilderFactory(config);
    }

    /**
     * Creates a JsonString.
     *
     * @param value a JSON string
     * @return the JsonString for the string
     *
     * @since 1.1
     */
    public static JsonString createValue(String value) {
        return JsonProvider.provider().createValue(value);
    }

    /**
     * Creates a JsonNumber.
     *
     * @param value a JSON number
     * @return the JsonNumber for the number
     *
     * @since 1.1
     */
    public static JsonNumber createValue(int value) {
        return JsonProvider.provider().createValue(value);
    }

    /**
     * Creates a JsonNumber.
     *
     * @param value a JSON number
     * @return the JsonNumber for the number
     *
     * @since 1.1
     */
    public static JsonNumber createValue(long value) {
        return JsonProvider.provider().createValue(value);
    }

    /**
     * Creates a JsonNumber.
     *
     * @param value a JSON number
     * @return the JsonNumber for the number
     *
     * @since 1.1
     */
    public static JsonNumber createValue(double value) {
        return JsonProvider.provider().createValue(value);
    }

    /**
     * Creates a JsonNumber.
     *
     * @param value a JSON number
     * @return the JsonNumber for the number
     *
     * @since 1.1
     */
    public static JsonNumber createValue(BigDecimal value) {
        return JsonProvider.provider().createValue(value);
    }

    /**
     * Creates a JsonNumber.
     *
     * @param value a JSON number
     * @return the JsonNumber for the number
     *
     * @since 1.1
     */
    public static JsonNumber createValue(BigInteger value) {
        return JsonProvider.provider().createValue(value);
    }

    /**
     * Encodes (escapes) a passed string as defined by <a href="http://tools.ietf.org/html/rfc6901">RFC 6901</a>.
     * This method doesn't validate the passed JSON-pointer string.
     *
     * @param pointer the JSON-pointer string to encode
     * @return encoded JSON-pointer string
     * 
     * @since 1.1
     */
    public static String encodePointer(String pointer) {
        return pointer.replace("~", "~0").replace("/", "~1");
    }

    /**
     * Decodes a passed JSON-pointer string as defined by <a href="http://tools.ietf.org/html/rfc6901">RFC 6901</a>.
     * This method doesn't validate the passed JSON-pointer string.
     *
     * @param escaped the JSON-pointer string to decode
     * @return decoded JSON-pointer string
     *     
     * @since 1.1
     */
    public static String decodePointer(String escaped) {
        return escaped.replace("~1", "/").replace("~0", "~");
    }

    /**
     * Creates a JsonNumber.
     *
     * @param value a JSON number
     * @return the JsonNumber for the number
     *
     * @since 2.1
     */
    public static JsonNumber createValue(Number value) {
        return JsonProvider.provider().createValue(value);
    }
}
