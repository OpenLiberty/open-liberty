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


import java.io.Closeable;
import java.math.BigDecimal;
import java.util.stream.Stream;
import java.util.Map;

import jakarta.json.JsonValue;
import jakarta.json.JsonObject;
import jakarta.json.JsonArray;

/**
 * Provides forward, read-only access to JSON data in a streaming way. This
 * is the most efficient way for reading JSON data.
 * This is the only way to parse and process JSON data that are too big to be loaded in memory.
 * <p>The class
 * {@link jakarta.json.Json} contains methods to create parsers from input
 * sources ({@link java.io.InputStream} and {@link java.io.Reader}).
 *
 * <p>
 * The following example demonstrates how to create a parser from a string
 * that contains an empty JSON array:
 * <pre>
 * <code>
 * JsonParser parser = Json.createParser(new StringReader("[]"));
 * </code>
 * </pre>
 *
 * <p>
 * The class {@link JsonParserFactory} also contains methods to create
 * {@code JsonParser} instances. {@link JsonParserFactory} is preferred
 * when creating multiple parser instances. A sample usage is shown
 * in the following example:
 * <pre>
 * <code>
 * JsonParserFactory factory = Json.createParserFactory();
 * JsonParser parser1 = factory.createParser(...);
 * JsonParser parser2 = factory.createParser(...);
 * </code>
 * </pre>
 *
 * <p>
 * {@code JsonParser} parses JSON using the pull parsing programming model.
 * In this model the client code controls the thread and calls the method
 * {@code next()} to advance the parser to the next state after
 * processing each element. The parser can generate the following events:
 * {@code START_OBJECT}, {@code END_OBJECT}, {@code START_ARRAY},
 * {@code END_ARRAY}, {@code KEY_NAME}, {@code VALUE_STRING},
 * {@code VALUE_NUMBER}, {@code VALUE_TRUE}, {@code VALUE_FALSE},
 * and {@code VALUE_NULL}.
 *
 * <p>
 * <b>For example</b>, for an empty JSON object ({ }), the parser generates the event
 * {@code START_OBJECT} with the first call to the method {@code next()} and the
 * event {@code END_OBJECT} with the second call to the method {@code next()}.
 * The following code demonstrates how to access these events:
 *
 * <pre>
 * <code>
 * Event event = parser.next(); // START_OBJECT
 * event = parser.next();       // END_OBJECT
 * </code>
 * </pre>
 *
 * <p>
 * <b>For example</b>, for the following JSON:
 * <pre>
 * {
 *   "firstName": "John", "lastName": "Smith", "age": 25,
 *   "phoneNumber": [
 *       { "type": "home", "number": "212 555-1234" },
 *       { "type": "fax", "number": "646 555-4567" }
 *    ]
 * }
 * </pre>
 *
 * <p>calls to the method {@code next()} result in parse events at the specified
 * locations below (marked in bold):
 *
 * <pre>
 * {<B>START_OBJECT</B>
 *   "firstName"<B>KEY_NAME</B>: "John"<B>VALUE_STRING</B>, "lastName"<B>KEY_NAME</B>: "Smith"<B>VALUE_STRING</B>, "age"<B>KEY_NAME</B>: 25<B>VALUE_NUMBER</B>,
 *   "phoneNumber"<B>KEY_NAME</B> : [<B>START_ARRAY</B>
 *       {<B>START_OBJECT</B> "type"<B>KEY_NAME</B>: "home"<B>VALUE_STRING</B>, "number"<B>KEY_NAME</B>: "212 555-1234"<B>VALUE_STRING</B> }<B>END_OBJECT</B>,
 *       {<B>START_OBJECT</B> "type"<B>KEY_NAME</B>: "fax"<B>VALUE_STRING</B>, "number"<B>KEY_NAME</B>: "646 555-4567"<B>VALUE_STRING</B> }<B>END_OBJECT</B>
 *    ]<B>END_ARRAY</B>
 * }<B>END_OBJECT</B>
 * </pre>
 *
 * The methods {@link #next()} and {@link #hasNext()} enable iteration over
 * parser events to process JSON data. {@code JsonParser} provides get methods
 * to obtain the value at the current state of the parser. For example, the
 * following code shows how to obtain the value "John" from the JSON above:
 *
 * <pre>
 * <code>
 * Event event = parser.next(); // START_OBJECT
 * event = parser.next();       // KEY_NAME
 * event = parser.next();       // VALUE_STRING
 * parser.getString();          // "John"
 * </code>
 * </pre>
 *
 * Starting in version 1.1, it is possible to build a partial JSON object
 * model from the stream, at the current parser position.
 * The methods {@link #getArray} and {@link #getObject} can be used to read in
 * a {@code JsonArray} or {@code JsonObject}.  For example, the following code
 * shows how to obtain the phoneNumber in a JsonArray, from the JSON above:
 *
 * <pre><code>
 * while (parser.hasNext() {
 *     Event event = parser.next();
 *     if (event == JsonParser.Event.KEY_NAME ) {
 *         String key = parser.getString();
 *         event = parser.next();
 *         if (key.equals("phoneNumber") {
 *             JsonArray phones = parser.getArray();
 *         }
 *     }
 * }
 * </code></pre>
 *
 * The methods {@link #getArrayStream} and {@link #getObjectStream} can be used
 * to get a stream of the elements of a {@code JsonArray} or {@code JsonObject}.
 * For example, the following code shows another way to obtain John's phoneNumber
 * in a {@code JsonArray} :
 *
 * <pre>{@code
 * Event event = parser.next(); // START_OBJECT
 * JsonArray phones = (JsonArray)
 *     parser.getObjectStream().filter(e->e.getKey().equals("phoneNumber"))
 *                             .map(e->e.getValue())
 *                             .findFirst()
 *                             .get();
 * }</pre>
 *
 * The methods {@link #skipArray} and {@link #skipObject} can be used to
 * skip tokens and position the parser to {@code END_ARRAY} or
 * {@code END_OBJECT}.
 * <p>
 * {@code JsonParser} can be used to parse sequence of JSON values that are not
 * enclosed in a JSON array, e.g. { } { }. The following code demonstrates how
 * to parse such sequence.
 * <pre><code>
 * JsonParser parser = Json.createParser(...);
 * while (parser.hasNext) {
 *     parser.next(); // advance parser state
 *     JsonValue value = parser.getValue();
 * }
 * </code></pre>
 *
 * @see jakarta.json.Json
 * @see JsonParserFactory
 */
public interface JsonParser extends /*Auto*/Closeable {

    /**
     * An event from {@code JsonParser}.
     */
    enum Event {
        /**
         * Start of a JSON array. The position of the parser is after '['.
         */
        START_ARRAY,
        /**
         * Start of a JSON object. The position of the parser is after '{'.
         */
        START_OBJECT,
        /**
         * Name in a name/value pair of a JSON object. The position of the parser
         * is after the key name. The method {@link #getString} returns the key
         * name.
         */
        KEY_NAME,
        /**
         * String value in a JSON array or object. The position of the parser is
         * after the string value. The method {@link #getString}
         * returns the string value.
         */
        VALUE_STRING,
        /**
         * Number value in a JSON array or object. The position of the parser is
         * after the number value. {@code JsonParser} provides the following
         * methods to access the number value: {@link #getInt},
         * {@link #getLong}, and {@link #getBigDecimal}.
         */
        VALUE_NUMBER,
        /**
         * {@code true} value in a JSON array or object. The position of the
         * parser is after the {@code true} value.
         */
        VALUE_TRUE,
        /**
         * {@code false} value in a JSON array or object. The position of the
         * parser is after the {@code false} value.
         */
        VALUE_FALSE,
        /**
         * {@code null} value in a JSON array or object. The position of the
         * parser is after the {@code null} value.
         */
        VALUE_NULL,
        /**
         * End of a JSON object. The position of the parser is after '}'.
         */
        END_OBJECT,
        /**
         * End of a JSON array. The position of the parser is after ']'.
         */
        END_ARRAY
    }

    /**
     * Returns {@code true} if there are more parsing states. This method returns
     * {@code false} if the parser reaches the end of the JSON text.
     *
     * @return {@code true} if there are more parsing states.
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonParsingException if the parser encounters invalid JSON
     * when advancing to next state.
     */
    boolean hasNext();

    /**
     * Returns the event for the next parsing state.
     *
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws JsonParsingException if the parser encounters invalid JSON
     * when advancing to next state.
     * @throws java.util.NoSuchElementException if there are no more parsing
     * states.
     * @return the event for the next parsing state
     */
    Event next();

    /**
     * Returns the event for the current parsing state.
     *
     * @return the event for the current parsing state
     *
     * @since 2.1
     */
    default public Event currentEvent() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a {@code String} for the name in a name/value pair,
     * for a string value or a number value. This method should only be called
     * when the parser state is {@link Event#KEY_NAME}, {@link Event#VALUE_STRING},
     * or {@link Event#VALUE_NUMBER}.
     *
     * @return a name when the parser state is {@link Event#KEY_NAME}
     *         a string value when the parser state is {@link Event#VALUE_STRING}
     *         a number value when the parser state is {@link Event#VALUE_NUMBER}
     * @throws IllegalStateException when the parser state is not
     *      {@code KEY_NAME}, {@code VALUE_STRING}, or {@code VALUE_NUMBER}
     */
    String getString();

    /**
     * Returns true if the JSON number at the current parser state is a
     * integral number. A {@link BigDecimal} may be used to store the value
     * internally and this method semantics are defined using its
     * {@code scale()}. If the scale is zero, then it is considered integral
     * type. This integral type information can be used to invoke an
     * appropriate accessor method to obtain a numeric value as in the
     * following example:
     *
     * <pre>
     * <code>
     * JsonParser parser = ...
     * if (parser.isIntegralNumber()) {
     *     parser.getInt();     // or other methods to get integral value
     * } else {
     *     parser.getBigDecimal();
     * }
     * </code>
     * </pre>
     *
     * @return true if this number is a integral number, otherwise false
     * @throws IllegalStateException when the parser state is not
     *      {@code VALUE_NUMBER}
     */
    boolean isIntegralNumber();

    /**
     * Returns a JSON number as an integer. The returned value is equal
     * to {@code new BigDecimal(getString()).intValue()}. Note that
     * this conversion can lose information about the overall magnitude
     * and precision of the number value as well as return a result with
     * the opposite sign. This method should only be called when the parser
     * state is {@link Event#VALUE_NUMBER}.
     *
     * @return an integer for a JSON number
     * @throws IllegalStateException when the parser state is not
     *      {@code VALUE_NUMBER}
     * @see java.math.BigDecimal#intValue()
     */
    int getInt();

    /**
     * Returns a JSON number as a long. The returned value is equal
     * to {@code new BigDecimal(getString()).longValue()}. Note that this
     * conversion can lose information about the overall magnitude and
     * precision of the number value as well as return a result with
     * the opposite sign. This method is only called when the parser state is
     * {@link Event#VALUE_NUMBER}.
     *
     * @return a long for a JSON number
     * @throws IllegalStateException when the parser state is not
     *      {@code VALUE_NUMBER}
     * @see java.math.BigDecimal#longValue()
     */
    long getLong();

    /**
     * Returns a JSON number as a {@code BigDecimal}. The {@code BigDecimal}
     * is created using {@code new BigDecimal(getString())}. This
     * method should only called when the parser state is
     * {@link Event#VALUE_NUMBER}.
     *
     * @return a {@code BigDecimal} for a JSON number
     * @throws IllegalStateException when the parser state is not
     *      {@code VALUE_NUMBER}
     */
    BigDecimal getBigDecimal();

    /**
     * Return the location that corresponds to the parser's current state in
     * the JSON input source. The location information is only valid in the
     * current parser state (or until the parser is advanced to a next state).
     *
     * @return a non-null location corresponding to the current parser state
     * in JSON input source
     */
    JsonLocation getLocation();

    /**
     * Returns a {@code JsonObject} and advances the parser to the
     * corresponding {@code END_OBJECT}.
     *
     * @return the {@code JsonObject} at the current parser position
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws IllegalStateException when the parser state is not
     *     {@code START_OBJECT}
     * @throws JsonParsingException if the parser encounters invalid JSON
     * when advancing to next state.
     * @throws java.util.NoSuchElementException if there are no more parsing
     * states.
     *
     * @since 1.1
     */
    default public JsonObject getObject() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a {@code JsonValue} at the current parser position.
     * If the parser state is {@code START_ARRAY}, the behavior is
     * the same as {@link #getArray}. If the parser state is
     * {@code START_OBJECT}, the behavior is the same as
     * {@link #getObject}. For all other cases, if applicable, the JSON value is
     * read and returned.
     *
     * @return the {@code JsonValue} at the current parser position.
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws IllegalStateException when the parser state is
     *     {@code END_OBJECT} or {@code END_ARRAY}
     * @throws JsonParsingException if the parser encounters invalid JSON
     * when advancing to next state.
     * @throws java.util.NoSuchElementException if there are no more parsing
     * states.
     *
     * @since 1.1
     */
    default public JsonValue getValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a {@code JsonArray} and advance the parser to the
     * the corresponding {@code END_ARRAY}.
     *
     * @return the {@code JsonArray} at the current parser position
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     * @throws IllegalStateException when the parser state is not
     *     {@code START_ARRAY}
     * @throws JsonParsingException if the parser encounters invalid JSON
     * when advancing to next state.
     * @throws java.util.NoSuchElementException if there are no more parsing
     * states.
     *
     * @since 1.1
     */
    default public JsonArray getArray() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a stream of the {@code JsonArray} elements.
     * The parser state must be {@code START_ARRAY}.
     * The elements are read lazily, on an as-needed basis, as
     * required by the stream operations.
     * If the stream operations do not consume
     * all of the array elements, {@link skipArray} can be used to
     * skip the unprocessed array elements.
     *
     * @return a stream of elements of the {@code JsonArray}
     *
     * @throws IllegalStateException when the parser state is not
     *     {@code START_ARRAY}
     *
     * @since 1.1
     */
    default public Stream<JsonValue> getArrayStream() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a stream of the {@code JsonObject}'s
     * name/value pairs. The parser state must be {@code START_OBJECT}.
     * The name/value pairs are read lazily, on an as-needed basis, as
     * required by the stream operations.
     * If the stream operations do not consume
     * all of the object's name/value pairs, {@link skipObject} can be
     * used to skip the unprocessed elements.
     *
     * @return a stream of name/value pairs of the {@code JsonObject}
     *
     * @throws IllegalStateException when the parser state is not
     *     {@code START_OBJECT}
     *
     * @since 1.1
     */
    default public Stream<Map.Entry<String,JsonValue>> getObjectStream() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a stream of {@code JsonValue} from a sequence of
     * JSON values. The values are read lazily, on an as-needed basis,
     * as needed by the stream operations.
     *
     * @return a Stream of {@code JsonValue}
     *
     * @throws IllegalStateException if the parser is in an array or object.
     *
     * @since 1.1
     */
    default public Stream<JsonValue> getValueStream() {
        throw new UnsupportedOperationException();
    }

    /**
     * Advance the parser to {@code END_ARRAY}.
     * If the parser is in array context, i.e. it has previously
     * encountered a {@code START_ARRAY} without encountering the
     * corresponding {@code END_ARRAY}, the parser is advanced to
     * the corresponding {@code END_ARRAY}.
     * If the parser is not in any array context, nothing happens.
     *
     * @since 1.1
     */
    default public void skipArray() {
        throw new UnsupportedOperationException();
    }

    /**
     * Advance the parser to {@code END_OBJECT}.
     * If the parser is in object context, i.e. it has previously
     * encountered a {@code START_OBJECT} without encountering the
     * corresponding {@code END_OBJECT}, the parser is advanced to
     * the corresponding {@code END_OBJECT}.
     * If the parser is not in any object context, nothing happens.
     *
     * @since 1.1
     */
    default public void skipObject() {
        throw new UnsupportedOperationException();
    }

    /**
     * Closes this parser and frees any resources associated with the
     * parser. This method closes the underlying input source.
     *
     * @throws jakarta.json.JsonException if an i/o error occurs (IOException
     * would be cause of JsonException)
     */
    @Override
    void close();
}
