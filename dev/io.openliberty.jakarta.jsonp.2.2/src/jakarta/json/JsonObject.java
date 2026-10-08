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

package jakarta.json;

import java.util.Map;

/**
 * {@code JsonObject} class represents an immutable JSON object value
 * (an unordered collection of zero or more name/value pairs).
 * It also provides unmodifiable map view to the JSON object
 * name/value mappings.
 *
 * <p>A JsonObject instance can be created from an input source using
 * {@link JsonReader#readObject()}. For example:
 * <pre><code>
 * JsonReader jsonReader = Json.createReader(...);
 * JsonObject object = jsonReader.readObject();
 * jsonReader.close();
 * </code></pre>
 *
 * It can also be built from scratch using a {@link JsonObjectBuilder}.
 *
 * <p>For example 1: An empty JSON object can be built as follows:
 * <pre><code>
 * JsonObject object = Json.createObjectBuilder().build();
 * </code></pre>
 *
 * For example 2: The following JSON
 * <pre><code>
 * {
 *     "firstName": "John", "lastName": "Smith", "age": 25,
 *     "address" : {
 *         "streetAddress": "21 2nd Street",
 *         "city": "New York",
 *         "state": "NY",
 *         "postalCode": "10021"
 *     },
 *     "phoneNumber": [
 *         { "type": "home", "number": "212 555-1234" },
 *         { "type": "fax", "number": "646 555-4567" }
 *     ]
 * }
 * </code></pre>
 * can be built using :
 * <pre><code>
 * JsonObject value = Json.createObjectBuilder()
 *     .add("firstName", "John")
 *     .add("lastName", "Smith")
 *     .add("age", 25)
 *     .add("address", Json.createObjectBuilder()
 *         .add("streetAddress", "21 2nd Street")
 *         .add("city", "New York")
 *         .add("state", "NY")
 *         .add("postalCode", "10021"))
 *     .add("phoneNumber", Json.createArrayBuilder()
 *         .add(Json.createObjectBuilder()
 *             .add("type", "home")
 *             .add("number", "212 555-1234"))
 *         .add(Json.createObjectBuilder()
 *             .add("type", "fax")
 *             .add("number", "646 555-4567")))
 *     .build();
 * </code></pre>
 *
 * {@code JsonObject} can be written to JSON as follows:
 * <pre><code>
 * JsonWriter writer = ...
 * JsonObject obj = ...;
 * writer.writeObject(obj);
 * </code></pre>
 *
 * {@code JsonObject} values can be {@link JsonObject}, {@link JsonArray},
 * {@link JsonString}, {@link JsonNumber}, {@link JsonValue#TRUE},
 * {@link JsonValue#FALSE}, {@link JsonValue#NULL}. These values can be
 * accessed using various accessor methods.
 *
 * <p>In the above example 2, "John" can be got using
 * <pre><code>
 * String firstName = object.getString("firstName");
 * </code></pre>
 *
 * This map object provides read-only access to the JSON object data,
 * and attempts to modify the map, whether direct or via its collection
 * views, result in an {@code UnsupportedOperationException}.
 *
 * <p>The map object's iteration ordering is based on the order in which
 * name/value pairs are added to the corresponding builder or the order
 * in which name/value pairs appear in the corresponding stream.
 */
public interface JsonObject extends JsonStructure, Map<String, JsonValue> {

    /**
     * Returns the array value to which the specified name is mapped.
     * This is a convenience method for {@code (JsonArray)get(name)} to
     * get the value.
     *
     * @param name the name whose associated value is to be returned
     * @return the array value to which the specified name is mapped, or
     *         {@code null} if this object contains no mapping for the name
     * @throws ClassCastException if the value to which the specified name
     * is mapped is not assignable to JsonArray type
     */
    JsonArray getJsonArray(String name);

    /**
     * Returns the object value to which the specified name is mapped.
     * This is a convenience method for {@code (JsonObject)get(name)} to
     * get the value.
     *
     * @param name the name whose associated value is to be returned
     * @return the object value to which the specified name is mapped, or
     *         {@code null} if this object contains no mapping for the name
     * @throws ClassCastException if the value to which the specified name
     * is mapped is not assignable to JsonObject type
     */
    JsonObject getJsonObject(String name);

    /**
     * Returns the number value to which the specified name is mapped.
     * This is a convenience method for {@code (JsonNumber)get(name)} to
     * get the value.
     *
     * @param name the name whose associated value is to be returned
     * @return the number value to which the specified name is mapped, or
     *         {@code null} if this object contains no mapping for the name
     * @throws ClassCastException if the value to which the specified name
     * is mapped is not assignable to JsonNumber type
     */
    JsonNumber getJsonNumber(String name);

    /**
     * Returns the string value to which the specified name is mapped.
     * This is a convenience method for {@code (JsonString)get(name)} to
     * get the value.
     *
     * @param name the name whose associated value is to be returned
     * @return the string value to which the specified name is mapped, or
     *         {@code null} if this object contains no mapping for the name
     * @throws ClassCastException if the value to which the specified name
     * is mapped is not assignable to JsonString type
     */
    JsonString getJsonString(String name);

    /**
     * A convenience method for
     * {@code getJsonString(name).getString()}
     *
     * @param name whose associated value is to be returned as String
     * @return the String value to which the specified name is mapped
     * @throws NullPointerException if the specified name doesn't have any
     * mapping
     * @throws ClassCastException if the value for specified name mapping
     * is not assignable to JsonString
     */
    String getString(String name);

    /**
     * Returns the string value of the associated {@code JsonString} mapping
     * for the specified name. If {@code JsonString} is found, then its
     * {@link jakarta.json.JsonString#getString()} is returned. Otherwise,
     * the specified default value is returned.
     *
     * @param name whose associated value is to be returned as String
     * @param defaultValue a default value to be returned
     * @return the string value of the associated mapping for the name,
     * or the default value
     */
    String getString(String name, String defaultValue);

    /**
     * A convenience method for
     * {@code getJsonNumber(name).intValue()}
     *
     * @param name whose associated value is to be returned as int
     * @return the int value to which the specified name is mapped
     * @throws NullPointerException if the specified name doesn't have any
     * mapping
     * @throws ClassCastException if the value for specified name mapping
     * is not assignable to JsonNumber
     */
    int getInt(String name);

    /**
     * Returns the int value of the associated {@code JsonNumber} mapping
     * for the specified name. If {@code JsonNumber} is found, then its
     * {@link jakarta.json.JsonNumber#intValue()} is returned. Otherwise,
     * the specified default value is returned.
     *
     * @param name whose associated value is to be returned as int
     * @param defaultValue a default value to be returned
     * @return the int value of the associated mapping for the name,
     * or the default value
     */
    int getInt(String name, int defaultValue);

    /**
     * Returns the boolean value of the associated mapping for the specified
     * name. If the associated mapping is JsonValue.TRUE, then returns true.
     * If the associated mapping is JsonValue.FALSE, then returns false.
     *
     * @param name whose associated value is to be returned as boolean
     * @return the boolean value to which the specified name is mapped
     * @throws NullPointerException if the specified name doesn't have any
     * mapping
     * @throws ClassCastException if the value for specified name mapping
     * is not assignable to JsonValue.TRUE or JsonValue.FALSE
     */
    boolean getBoolean(String name);

    /**
     * Returns the boolean value of the associated mapping for the specified
     * name. If the associated mapping is JsonValue.TRUE, then returns true.
     * If the associated mapping is JsonValue.FALSE, then returns false.
     * Otherwise, the specified default value is returned.
     *
     * @param name whose associated value is to be returned as int
     * @param defaultValue a default value to be returned
     * @return the boolean value of the associated mapping for the name,
     * or the default value
     */
    boolean getBoolean(String name, boolean defaultValue);

    /**
     * Returns {@code true} if the associated value for the specified name is
     * {@code JsonValue.NULL}.
     *
     * @param name name whose associated value is checked
     * @return return true if the associated value is {@code JsonValue.NULL},
     * otherwise false
     * @throws NullPointerException if the specified name doesn't have any
     * mapping
     */
    boolean isNull(String name);

}
