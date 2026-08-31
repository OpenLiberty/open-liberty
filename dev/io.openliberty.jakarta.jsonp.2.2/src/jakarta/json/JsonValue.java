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

/**
 * <code>JsonValue</code> represents an immutable JSON value.
 *
 *
 * <p>A JSON value is one of the following:
 * an object ({@link JsonObject}), an array ({@link JsonArray}),
 * a number ({@link JsonNumber}), a string ({@link JsonString}),
 * {@code true} ({@link JsonValue#TRUE JsonValue.TRUE}), {@code false}
 * ({@link JsonValue#FALSE JsonValue.FALSE}),
 * or {@code null} ({@link JsonValue#NULL JsonValue.NULL}).
 */
public interface JsonValue {

    /**
     * The empty JSON object.
     *
     * @since 1.1
     */
    static final JsonObject EMPTY_JSON_OBJECT = new EmptyObject();

    /**
     * The empty JSON array.
     *
     * @since 1.1
     */
    static final JsonArray EMPTY_JSON_ARRAY = new EmptyArray();

    /**
     * Indicates the type of a {@link JsonValue} object.
     */
    enum ValueType {
        /**
         * JSON array.
         */
        ARRAY,

        /**
         * JSON object.
         */
        OBJECT,

        /**
         * JSON string.
         */
        STRING,

        /**
         * JSON number.
         */
        NUMBER,

        /**
         * JSON true.
         */
        TRUE,

        /**
         * JSON false.
         */
        FALSE,

        /**
         * JSON null.
         */
        NULL
    }

    /**
     * JSON null value.
     */
    static final JsonValue NULL = new JsonValueImpl(ValueType.NULL);

    /**
     * JSON true value.
     */
    static final JsonValue TRUE = new JsonValueImpl(ValueType.TRUE);

    /**
     * JSON false value.
     */
    static final JsonValue FALSE = new JsonValueImpl(ValueType.FALSE);

    /**
     * Returns the value type of this JSON value.
     *
     * @return JSON value type
     */
    ValueType getValueType();

    /**
     * Return the JsonValue as a JsonObject
     *
     * @return the JsonValue as a JsonObject
     * @throws ClassCastException if the JsonValue is not a JsonObject
     *
     * @since 1.1
     */
    default JsonObject asJsonObject() {
        return JsonObject.class.cast(this);
    }

    /**
     * Return the JsonValue as a JsonArray
     *
     * @return the JsonValue as a JsonArray
     * @throws ClassCastException if the JsonValue is not a JsonArray
     *
     * @since 1.1
     */
    default JsonArray asJsonArray() {
        return JsonArray.class.cast(this);
    }

    /**
     * Returns JSON text for this JSON value.
     *
     * @return JSON text
     */
    @Override
    String toString();

}
