/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.test.merge.parts;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.openliberty.microprofile.openapi20.internal.merge.MergeProcessorImpl;
import io.openliberty.microprofile.openapi20.internal.services.MergeProcessor;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;

public interface TestUtil {

    /**
     * Create an OpenAPIProvdier for the given model with an application path of {@code /}
     *
     * @param openapi the openapi model
     * @return the provider
     */
    OpenAPIProvider createProvider(OpenAPI openapi);

    /**
     * Create an OpenAPIProvider for the given model
     *
     * @param openapi the openapi model
     * @param applicationPath the application path for the returned provider
     * @return the provider
     */
    OpenAPIProvider createProvider(OpenAPI openapi, String applicationPath);

    /**
     * Merge several OpenAPI models using {@link MergeProcessorImpl}
     *
     * @param docs the openapi models
     * @return the merged model
     */
    OpenAPI merge(OpenAPI... docs);

    /**
     * Merge several OpenAPI providers using {@link MergeProcessorImpl}
     *
     * @param providers the providers
     * @return the merged model
     */
    OpenAPI merge(List<OpenAPIProvider> providers);

    /**
     * Create a {@link MergeProcessorImpl} and initialize references
     *
     * @return the MergeProcessor under test
     */
    MergeProcessor getMergeProcessor();

    /**
     * Serialize a model as a string
     *
     * @param model the model
     * @param format the format (json/yaml)
     * @return the serialized model
     * @throws IOException
     */
    String serialize(OpenAPI model, Format format) throws IOException;

    /**
     * Parse a model from a stream
     *
     * @param is the input stream
     * @param format the format (json/yaml)
     * @return the parsed model
     * @throws IOException
     */
    OpenAPI parse(InputStream is, Format format) throws IOException;

    public enum Format {
        JSON, YAML
    }

}