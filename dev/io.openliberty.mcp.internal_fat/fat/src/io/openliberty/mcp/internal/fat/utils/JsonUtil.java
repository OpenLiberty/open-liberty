/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Null-safe helpers for reading values out of JSON, used by the response classes so that their
 * getters can say which part of the JSON they return.
 */
final class JsonUtil {

    private JsonUtil() {}

    /**
     * Returns the value at the given path, or {@code null} if the object is {@code null}, any part
     * of the path is missing, an intermediate value is not an object, or the value is not of the
     * given type. A JSON {@code null} is not an instance of any type but {@code Object}.
     *
     * <p>The path is given as one argument per level, never as a single delimited string, so that
     * any field name can be addressed, including names that contain {@code /} or {@code .} such as
     * {@code _meta} keys like {@code "api.ibmtest.org/location"}.
     *
     * <pre>{@code
     * get(json, Boolean.class, "result", "isError");
     * get(json, String.class, "result", "_meta", "api.ibmtest.org/location");
     * }</pre>
     *
     * @param json the object to start from
     * @param type the expected type of the value, e.g. {@code JSONObject.class}
     * @param path the names of the nested fields to follow, outermost first. With no names the
     *     value is {@code json} itself.
     */
    static <T> T get(JSONObject json, Class<T> type, String... path) {
        Object current = json;
        for (String name : path) {
            if (!(current instanceof JSONObject object)) {
                return null;
            }
            current = object.opt(name);
        }
        return type.isInstance(current) ? type.cast(current) : null;
    }

    /**
     * Returns the items of the array as a list, or an empty list if the array is {@code null}. An item
     * that is not of the given type is represented by {@code null}.
     */
    static <T> List<T> toListOf(JSONArray array, Class<T> type) {
        List<T> items = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                Object item = array.opt(i);
                items.add(type.isInstance(item) ? type.cast(item) : null);
            }
        }
        return items;
    }
}
