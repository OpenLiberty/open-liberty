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

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

/**
 * Instructions for how Jsonb should serialize {@link McpRequestId} types into JSON
 */
public class McpRequestIdSerializer implements JsonbSerializer<McpRequestId> {

    @Override
    public void serialize(McpRequestId id, JsonGenerator generator, SerializationContext ctx) {
        if (id.getStrVal() != null && !id.getStrVal().isEmpty())
            generator.write(id.getStrVal());
        else if (id.getNumVal() != null)
            generator.write(id.getNumVal());
        else
            generator.writeNull();
    }

}
