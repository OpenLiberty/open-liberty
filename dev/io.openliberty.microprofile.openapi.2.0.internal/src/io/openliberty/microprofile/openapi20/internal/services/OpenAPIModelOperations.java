/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.services;

import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;

import io.smallrye.openapi.runtime.OpenApiRuntimeException;

/**
 * Model operations which require a different implementation on different versions of the OpenAPI model.
 */
public interface OpenAPIModelOperations {

    /**
     * Create a shallow copy of an OpenAPI model
     * <p>
     * This allows us to replace the servers and info sections without modifying the original model.
     *
     * @param model the original OpenAPI model
     * @return shallow copy of {@code model}
     */
    public OpenAPI shallowCopy(OpenAPI model);

    /**
     * Parse JSON into an Info object
     *
     * @param infoJson a JSON serialized OpenAPI info section
     * @return an Info model object
     * @throws OpenApiRuntimeException if there's an error reading the JSON
     */
    public Info parseInfo(String infoJson) throws OpenApiRuntimeException;

    /**
     * Checks whether an OpenAPI model is the default model generated when an app has no content.
     *
     * @param model the OpenAPI model to check
     * @return {@code true} if it's the default model, otherwise {@code false}
     */
    public boolean isDefaultOpenApiModel(OpenAPI model);

    /**
     * Creates a default OpenAPI model object
     *
     * @return the default OpenAPI model
     */
    public OpenAPI createDefaultOpenApiModel();

    /**
     * Get the list of accepted types from a schema
     * <p>
     * Works around the change in type signature of the Schema.getType method in MP OpenAPI 4.0.
     * <p>
     * OpenAPI v3.0 only allows a single type here, whereas OpenAPI v3.1 allows multiple.
     *
     * @param schema the schema
     * @return the list of schema types, or {@code null} if the {@code type} property is not set
     */
    public List<SchemaType> getTypes(Schema schema);

}
