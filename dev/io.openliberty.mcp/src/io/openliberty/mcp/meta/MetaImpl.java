/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.meta;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;

/**
 *
 */
public class MetaImpl implements Meta {

    public static MetaImpl from(JsonObject json) {
        if (json != null) {
            return new MetaImpl(json);
        }
        return new MetaImpl(null);
    }

    private final JsonObject meta;

    private MetaImpl(JsonObject meta) {
        this.meta = meta;
    }

    @Override
    public Object getValue(MetaKey key, Jsonb jsonb) {
        if (meta == null) {
            return null;
        }
        return jsonb.fromJson(meta.get(key.toString()).toString(), Object.class);
    }

    @Override
    public JsonObject asJsonObject() {
        return meta == null ? Json.createObjectBuilder().build() : meta;
    }

}
