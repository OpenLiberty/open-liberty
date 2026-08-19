/*
 * Copyright (c) 2011, 2021 Oracle and/or its affiliates. All rights reserved.
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

package jakarta.json.stream;

import jakarta.json.JsonValue;
import java.io.Closeable;
import java.io.Flushable;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Writes JSON data to an output source in a streaming way. The class
 * {@link jakarta.json.Json} contains methods to create generators for character
 * or output streams ({@link java.io.Writer} and {@link java.io.OutputStream}).
 *
 * <p>
 * The following example shows how to create a JSON generator:
 * <pre>
 * <code>
 * JsonGenerator generator = Json.createGenerator(...);
 * </code>
 * </pre>
 *
 * <p>
 * The class {@link JsonGeneratorFactory} also contains methods to create
 * {@code JsonGenerator} instances. {@link JsonGeneratorFactory} should be used
 * when creating multiple generator instances, as in the following example:
 * <pre>
 * <code>
 * JsonGeneratorFactory factory = Json.createGeneratorFactory();
 * JsonGenerator generator1 = factory.createGenerator(...);
 * JsonGenerator generator2 = factory.createGenerator(...);
 * </code>
 * </pre>
 *
 * <p>
 * JSON objects can be created using {@code JsonGenerator} by calling the
 * {@link #writeStartObject()} method and then adding name/value pairs with the
 * {@code write} method.
 * <p>
 * The following example shows how to generate an empty JSON object:
 * <pre>
 * <code>
 * JsonGenerator generator = ...;
 * generator.writeStartObject().writeEnd().close();
 * </code>
 * </pre>
 *
 * JSON arrays can be created using {@code JsonGenerator} by calling the
 * {@link #writeStartArray()} method and then adding values with the
 * {@code write} method.
 *
 * <p>
 * The following example shows how to generate an empty JSON array:
 * <pre>
 * <code>
 * JsonGenerator generator = ...;
 * generator.writeStartArray().writeEnd().close();
 * </code>
 * </pre>
 *
 * <p>
 * Other JSON values (that are not JSON objects or arrays) can be created
 * by calling the appropiate {@code write} methods.
 * <p>
 * The following example shows how to generate a JSON string:
 * <pre><code>
 * JsonGenerator generator = ...;
 * generator.write("message").close();
 * </code></pre>
 *
 * {@code JsonGenerator} methods can be chained as in the following example:
 * <pre>
 * <code>
 * generator
 *     .writeStartObject()
 *         .write("firstName", "John")
 *         .write("lastName", "Smith")
 *         .write("age", 25)
 *         .writeStartObject("address")
 *             .write("streetAddress", "21 2nd Street")
 *             .write("city", "New York")
 *             .write("state", "NY")
 *             .write("postalCode", "10021")
 *         .writeEnd()
 *         .writeStartArray("phoneNumber")
 *             .writeStartObject()
 *                 .write("type", "home")
 *                 .write("number", "212 555-1234")
 *             .writeEnd()
 *             .writeStartObject()
 *                 .write("type", "fax")
 *                 .write("number", "646 555-4567")
 *             .writeEnd()
 *         .writeEnd()
 *     .writeEnd();
 * generator.close();
 * </code>
 * </pre>
 *
 * The example code above generates the following JSON (or equivalent):
 * <pre>
 * <code>
 * {
 *   "firstName": "John", "lastName": "Smith", "age": 25,
 *   "address" : {
 *       "streetAddress": "21 2nd Street",
 *       "city": "New York",
 *       "state": "NY",
 *       "postalCode": "10021"
 *   },
 *   "phoneNumber": [
 *       {"type": "home", "number": "212 555-1234"},
 *       {"type": "fax", "number": "646 555-4567"}
 *    ]
 * }
 * </code>
 * </pre>
 *
 * The generated JSON text must strictly conform to the grammar defined in
 * <a href="http://www.ietf.org/rfc/rfc7159.txt">RFC 7159</a>.
 *
 * @see jakarta.json.Json
 * @see JsonGeneratorFactory
 */
public interface JsonGenerator extends Flushable, /*Auto*/Closeable {
    /**
     * Configuration property to generate JSON prettily. All providers
     * must support this property. The value of the property could be
     * be anything.
     */
    String PRETTY_PRINTING = "jakarta.json.stream.JsonGenerator.prettyPrinting" ;

    /**
     * Writes the JSON start object character. It starts a new child object
     * context within which JSON name/value pairs can be written to the object.
     * This method is valid only in an array context, field context or in no context (when a
     * context is not yet started). This method can only be called once in
     * no context.
     *
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is called within an
     *      object context or if it is called more than once in no context.
     */
    JsonGenerator writeStartObject();

    /**
     * Writes the JSON name/start object character pair in the current
     * object context. It starts a new child object context within which JSON
     * name/value pairs can be written to the object.
     *
     * @param name a name within the JSON name/object pair to be written
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *     object context
     */
    JsonGenerator writeStartObject(String name);

    /**
     * Writes the JSON name with a colon. It starts a field context, in which valid
     * options are writing a value, starting an object or an array.
     *
     * Writing value closes field context, if object or array is started after field name,
     * field context will be closed after object/array close.
     *
     * @param name name of json field
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *     object context
     *
     * @since 1.1
     */
    JsonGenerator writeKey(String name);

    /**
     * Writes the JSON start array character. It starts a new child array
     * context within which JSON values can be written to the array. This
     * method is valid only in an array context, field context or in no context (when a
     * context is not yet started). This method can only be called once in
     * no context.
     *
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is called within an
     *      object context or if called more than once in no context
     */
    JsonGenerator writeStartArray();

    /**
     * Writes the JSON name/start array character pair with in the current
     * object context. It starts a new child array context within which JSON
     * values can be written to the array.
     *
     * @param name a name within the JSON name/array pair to be written
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within
     *      an object context
     */
    JsonGenerator writeStartArray(String name);

    /**
     * Writes a JSON name/value pair in the current object context.
     *
     * @param name a name in the JSON name/value pair to be written in
     *             current JSON object
     * @param value a value in the JSON name/value pair to be written in
     *             current JSON object
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      object context
     */
    JsonGenerator write(String name, JsonValue value);

    /**
     * Writes a JSON name/string value pair in the current object context.
     * The specified value is written as JSON string value.
     *
     * @param name a name in the JSON name/string pair to be written in
     *             current JSON object
     * @param value a value in the JSON name/string pair to be written in
     *             current JSON object
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      object context
     */
    JsonGenerator write(String name, String value);

    /**
     * Writes a JSON name/number value pair in the current object context.
     * The specified value is written as a JSON number value. The string
     * {@code new BigDecimal(value).toString()}
     * is used as the text value for writing.
     *
     * @param name a name in the JSON name/number pair to be written in
     *             current JSON object
     * @param value a value in the JSON name/number pair to be written in
     *             current JSON object
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      object context.
     */
    JsonGenerator write(String name, BigInteger value);

    /**
     * Writes a JSON name/number value pair in the current object context.
     * The specified value is written as a JSON number value. The specified
     * value's {@code toString()} is used as the text value for writing.
     *
     * @param name a name in the JSON name/number pair to be written in
     *             current JSON object
     * @param value a value in the JSON name/number pair to be written in
     *             current JSON object
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      object context.
     */
    JsonGenerator write(String name, BigDecimal value);

    /**
     * Writes a JSON name/number value pair in the current object context.
     * The specified value is written as a JSON number value. The string
     * {@code new BigDecimal(value).toString()} is used as the text value
     * for writing.
     *
     * @param name a name in the JSON name/number pair to be written in
     *             current JSON object
     * @param value a value in the JSON name/number pair to be written in
     *             current JSON object
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      object context.
     */
    JsonGenerator write(String name, int value);

    /**
     * Writes a JSON name/number value pair in the current object context.
     * The specified value is written as a JSON number value. The string
     * {@code new BigDecimal(value).toString()} is used as the text
     * value for writing.
     *
     * @param name a name in the JSON name/number pair to be written in
     *             current JSON object
     * @param value a value in the JSON name/number pair to be written in
     *             current JSON object
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      object context.
     */
    JsonGenerator write(String name, long value);

    /**
     * Writes a JSON name/number value pair in the current object context.
     * The specified value is written as a JSON number value. The string
     * {@code BigDecimal.valueOf(double).toString()}
     * is used as the text value for writing.
     *
     * @param name a name in the JSON name/number pair to be written in
     *             current JSON object
     * @param value a value in the JSON name/number pair to be written in
     *             current JSON object
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws NumberFormatException if the value is Not-a-Number (NaN) or infinity.
     * @throws JsonGenerationException if this method is not called within an
     *      object context
     */
    JsonGenerator write(String name, double value);

    /**
     * Writes a JSON name/boolean value pair in the current object context.
     * If value is true, it writes the JSON {@code true} value, otherwise
     * it writes the JSON {@code false} value.
     *
     * @param name a name in the JSON name/boolean pair to be written in
     *             current JSON object
     * @param value a value in the JSON name/boolean pair to be written in
     *             current JSON object
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      object context.
     */
    JsonGenerator write(String name, boolean value);


    /**
     * Writes a JSON name/null value pair in an current object context.
     *
     * @param name a name in the JSON name/null pair to be written in
     *             current JSON object
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      object context
     */
    JsonGenerator writeNull(String name);

    /**
     * Writes the end of the current context. If the current context is
     * an array context, this method writes the end-of-array character (']').
     * If the current context is an object context, this method writes the
     * end-of-object character ('}'). After writing the end of the current
     * context, the parent context becomes the new current context.
     * If parent context is field context, it is closed.
     *
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is called in no context.
     */
    JsonGenerator writeEnd();

    /**
     * Writes the specified value as a JSON value within
     * the current array, field or root context.
     *
     * @param value a value to be written in current JSON array
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      array, root or field context.
     */
    JsonGenerator write(JsonValue value);

    /**
     * Writes the specified value as a JSON string value within
     * the current array, field or root context.
     *
     * @param value a value to be written in current JSON array
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      array, root or field context.
     */
    JsonGenerator write(String value);

    /**
     * Writes the specified value as a JSON number value within
     * the current array, field or root context. The specified value's {@code toString()}
     * is used as the the text value for writing.
     *
     * @param value a value to be written in current JSON array
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      array, root or field context.
     *
     * @see jakarta.json.JsonNumber
     */
    JsonGenerator write(BigDecimal value);

    /**
     * Writes the specified value as a JSON number value within
     * the current array, field or root context. The string {@code new BigDecimal(value).toString()}
     * is used as the text value for writing.
     *
     * @param value a value to be written in current JSON array
     * @return this generator.
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      array, root or field context.
     *
     * @see jakarta.json.JsonNumber
     */
    JsonGenerator write(BigInteger value);

    /**
     * Writes the specified value as a JSON number value within
     * the current array, field or root context. The string {@code new BigDecimal(value).toString()}
     * is used as the text value for writing.
     *
     * @param value a value to be written in current JSON array
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      array, root or field context.
     */
    JsonGenerator write(int value);

    /**
     * Writes the specified value as a JSON number value within
     * the current array, field or root context. The string {@code new BigDecimal(value).toString()}
     * is used as the text value for writing.
     *
     * @param value a value to be written in current JSON array
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      array, root or field context.
     */
    JsonGenerator write(long value);

    /**
     * Writes the specified value as a JSON number value within the current
     * array, field or root context. The string {@code BigDecimal.valueOf(value).toString()}
     * is used as the text value for writing.
     *
     * @param value a value to be written in current JSON array
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      array, root or field context.
     * @throws NumberFormatException if the value is Not-a-Number (NaN) or infinity.
     */
    JsonGenerator write(double value);

    /**
     * Writes a JSON true or false value within the current array, field or root context.
     * If value is true, this method writes the JSON {@code true} value,
     * otherwise it writes the JSON {@code false} value.
     *
     * @param value a {@code boolean} value
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      array, root or field context.
     */
    JsonGenerator write(boolean value);

    /**
     * Writes a JSON null value within the current array, field or root context.
     *
     * @return this generator
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if this method is not called within an
     *      array, root or field context.
     */
    JsonGenerator writeNull();

    /**
     * Closes this generator and frees any resources associated with it.
     * This method closes the underlying output source.
     *
     * The underlying stream is closed only if complete JSON object is written. In case
     * of incomplete object JsonGenerationException is thrown and underlying stream is
     * not closed.
     *
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonGenerationException if an incomplete JSON is generated
     */
    @Override
    void close();

    /**
     * Flushes the underlying output source. If the generator has saved
     * any characters in a buffer, writes them immediately to the underlying
     * output source before flushing it.
     *
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     */
    @Override
    void flush();

}
