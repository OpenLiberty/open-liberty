/*
 * Copyright (c) 2017, 2022 Oracle and/or its affiliates. All rights reserved.
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

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;

/**
 * Private implementation of immutable {@link JsonArray}.
 *
 * @author Lukas Jungmann
 */
final class EmptyArray extends AbstractList<JsonValue> implements JsonArray, Serializable, RandomAccess {

    /** for serialization */
    private static final long serialVersionUID = 7295439472061642859L;

    /** Default constructor. */
    EmptyArray() {}

    @Override
    public JsonValue get(int index) {
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public JsonObject getJsonObject(int index) {
        return (JsonObject) get(index);
    }

    @Override
    public JsonArray getJsonArray(int index) {
        return (JsonArray) get(index);
    }

    @Override
    public JsonNumber getJsonNumber(int index) {
        return (JsonNumber) get(index);
    }

    @Override
    public JsonString getJsonString(int index) {
        return (JsonString) get(index);
    }

    @Override
    public <T extends JsonValue> List<T> getValuesAs(Class<T> clazz) {
        return Collections.emptyList();
    }

    @Override
    public String getString(int index) {
        return getJsonString(index).getString();
    }

    @Override
    public String getString(int index, String defaultValue) {
        return defaultValue;
    }

    @Override
    public int getInt(int index) {
        return getJsonNumber(index).intValue();
    }

    @Override
    public int getInt(int index, int defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean getBoolean(int index) {
        return get(index) == JsonValue.TRUE;
    }

    @Override
    public boolean getBoolean(int index, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean isNull(int index) {
        return get(index) == JsonValue.NULL;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.ARRAY;
    }

    /**
     * Preserves singleton property
     * @return {@link JsonValue#EMPTY_JSON_ARRAY}
     */
    private Object readResolve() {
        return JsonValue.EMPTY_JSON_ARRAY;
    }
}
