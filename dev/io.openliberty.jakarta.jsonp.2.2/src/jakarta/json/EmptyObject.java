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
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Set;

/**
 * Private implementation of immutable {@link JsonObject}.
 *
 * @author Lukas Jungmann
 */
final class EmptyObject extends AbstractMap<String, JsonValue> implements JsonObject, Serializable {

    /** for serialization */
    private static final long serialVersionUID = -1461653546889072583L;

    /** Default constructor. */
    EmptyObject() {}

    @Override
    public Set<Entry<String, JsonValue>> entrySet() {
        return Collections.<Entry<String, JsonValue>>emptySet();
    }

    @Override
    public JsonArray getJsonArray(String name) {
        return (JsonArray) get(name);
    }

    @Override
    public JsonObject getJsonObject(String name) {
        return (JsonObject) get(name);
    }

    @Override
    public JsonNumber getJsonNumber(String name) {
        return (JsonNumber) get(name);
    }

    @Override
    public JsonString getJsonString(String name) {
        return (JsonString) get(name);
    }

    @Override
    public String getString(String name) {
        return getJsonString(name).getString();
    }

    @Override
    public String getString(String name, String defaultValue) {
        return defaultValue;
    }

    @Override
    public int getInt(String name) {
        return getJsonNumber(name).intValue();
    }

    @Override
    public int getInt(String name, int defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean getBoolean(String name) {
        throw new NullPointerException();
    }

    @Override
    public boolean getBoolean(String name, boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean isNull(String name) {
        throw new NullPointerException();
    }

    @Override
    public ValueType getValueType() {
        return ValueType.OBJECT;
    }

    /**
     * Preserves singleton property
     * @return {@link JsonValue#EMPTY_JSON_OBJECT}
     */
    private Object readResolve() {
        return JsonValue.EMPTY_JSON_OBJECT;
    }
}
