/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.schemas.blueprints;

import java.util.Optional;

import io.openliberty.mcp.internal.schemas.SchemaGenerator.SchemaGenerationContext;
import jakarta.json.JsonObjectBuilder;

/**
 * Stores all of the <em>static</em> data related to a <em>single type</em> that's required to generate a schema for it.
 * <p>
 * We extract and store the static data separately because the actual schema generated from a blueprint can be different depending on whether it's being used for input or output
 * and what other types are included in the same schema document.
 * <p>
 * For example, if a type is used in more than one place, its schema may be placed under {@code $defs} and referenced, but if not it may be used directly.
 */
public interface SchemaCreationBlueprint {
    /**
     * Converts blueprint to jsonSchema (JosnObjectBuilder) recursively based on contextual information.
     *
     * @param ctx the schema generation context
     * @param description optional description to use for the schema, {@code null} to not override the description
     *
     * @return the generated schema
     */
    public JsonObjectBuilder toJsonSchemaObject(SchemaGenerationContext ctx, String description);

    /**
     * Indicates whether instances of this blueprint should be required when included as object properties
     *
     * @return whether schemas created from this blueprint are required
     */
    public default boolean isRequired() {
        return true;
    }

    /**
     * Returns the base name that should be used when instances of this blueprint are added to {@code $defs}
     *
     * @return the defs name, or an empty optional if this blueprint should not be added to {@code $defs}
     */
    public default Optional<String> getDefsName() {
        return Optional.empty();
    }
}