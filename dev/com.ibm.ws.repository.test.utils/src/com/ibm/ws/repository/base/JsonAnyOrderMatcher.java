/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.repository.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matches JsonValues, ignoring any ordering of items in arrays or fields in objects.
 */
public class JsonAnyOrderMatcher extends TypeSafeMatcher<JsonValue> {

    private final Object matchingObject;

    public JsonAnyOrderMatcher(JsonValue value) {
        matchingObject = makeOrderInsensitive(value);
    }

    /** {@inheritDoc} */
    @Override
    public void describeTo(Description description) {
        description.appendText("Json equivelent (ignoring order of array elements and fields) to ").appendValue(matchingObject);

    }

    /** {@inheritDoc} */
    @Override
    protected boolean matchesSafely(JsonValue value) {
        return matchingObject.equals(makeOrderInsensitive(value));
    }

    private static Object makeOrderInsensitive(JsonValue value) {
        switch (value.getValueType()) {
            case ARRAY:
                return makeArrayOrderInsensitive((JsonArray) value);
            case OBJECT:
                return makeObjectOrderInsensitive((JsonObject) value);
            default:
                return value;
        }
    }

    private static Set<Object> makeArrayOrderInsensitive(JsonArray jsonArray) {
        Set<Object> result = new HashSet<Object>();
        for (JsonValue value : jsonArray) {
            result.add(makeOrderInsensitive(value));
        }
        return result;
    }

    private static Map<String, Object> makeObjectOrderInsensitive(JsonObject jsonObject) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            result.put(entry.getKey(), makeOrderInsensitive(entry.getValue()));
        }
        return result;
    }

    /**
     * Creates a matcher for JsonValues that matches if the examined JsonValue is equal to the specified JsonValue, ignoring the order of fields and array elements.
     */
    @Factory
    public static JsonAnyOrderMatcher matchesJsonInAnyOrder(JsonValue value) {
        return new JsonAnyOrderMatcher(value);
    }

}
