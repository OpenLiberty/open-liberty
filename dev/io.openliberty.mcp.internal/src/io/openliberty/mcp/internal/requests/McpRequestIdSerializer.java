/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import io.openliberty.mcp.request.RequestId;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

/**
 * Instructions for how Jsonb should serialize {@link RequestId} types into JSON
 */
public class McpRequestIdSerializer implements JsonbSerializer<RequestId> {

    @Override
    public void serialize(RequestId id, JsonGenerator generator, SerializationContext ctx) {
        Object val = id.value();

        if (val == null || (val instanceof String str && str.isEmpty())) {
            generator.writeNull();
            return;
        }

        if (val instanceof String str) {
            generator.write(str);
        } else if (val instanceof Number num) {
            ctx.serialize(num, generator);
        } else {
            throw new JsonbException("Unsupported ID type for serialization: " + val.getClass().getName());
        }
    }
}
