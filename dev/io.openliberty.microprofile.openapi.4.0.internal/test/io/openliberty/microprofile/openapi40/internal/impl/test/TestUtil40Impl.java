/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.impl.test;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.openliberty.microprofile.openapi20.internal.merge.MergeProcessorImpl;
import io.openliberty.microprofile.openapi20.internal.services.MergeProcessor;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.test.merge.parts.OpenAPIProviderCustom;
import io.openliberty.microprofile.openapi20.test.merge.parts.TestUtil;
import io.openliberty.microprofile.openapi40.internal.impl.OpenAPI31MergeProcessor;
import io.openliberty.microprofile.openapi40.internal.impl.OpenAPI31ModelWalkerImpl;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.runtime.io.OpenApiParser;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

public class TestUtil40Impl implements TestUtil {

    /**
     * Create an OpenAPIProvdier for the given model with an application path of {@code /}
     *
     * @param openapi the openapi model
     * @return the provider
     */
    @Override
    public OpenAPIProvider createProvider(OpenAPI openapi) {
        return createProvider(openapi, "/");
    }

    /**
     * Create an OpenAPIProvider for the given model
     *
     * @param openapi the openapi model
     * @param applicationPath the application path for the returned provider
     * @return the provider
     */
    @Override
    public OpenAPIProvider createProvider(OpenAPI openapi, String applicationPath) {
        return new OpenAPIProviderCustom(openapi, applicationPath);
    }

    /**
     * Merge several OpenAPI models using {@link MergeProcessorImpl}
     *
     * @param docs the openapi models
     * @return the merged model
     */
    @Override
    public OpenAPI merge(OpenAPI... docs) {
        List<OpenAPIProvider> providers = new ArrayList<>();
        for (OpenAPI doc : docs) {
            providers.add(createProvider(doc));
        }
        return merge(providers);
    }

    /**
     * Merge several OpenAPI providers using {@link MergeProcessorImpl}
     *
     * @param providers the providers
     * @return the merged model
     */
    @Override
    public OpenAPI merge(List<OpenAPIProvider> providers) {
        OpenAPIProvider merged = getMergeProcessor().mergeDocuments(providers);
        assertThat("Merge problems", merged.getMergeProblems(), empty());
        return merged.getModel();
    }

    /**
     * Create a {@link OpenAPI31MergeProcessor} and initialize references
     *
     * @return the MergeProcessor under test
     */
    @Override
    public MergeProcessor getMergeProcessor() {
        OpenAPI31MergeProcessor mergeProcessor = new OpenAPI31MergeProcessor();
        try {
            // Inject model walker
            Field f = MergeProcessorImpl.class.getDeclaredField("modelWalker");
            f.setAccessible(true);
            f.set(mergeProcessor, new OpenAPI31ModelWalkerImpl());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mergeProcessor;
    }

    @Override
    public String serialize(OpenAPI model, Format format) throws IOException {
        return OpenApiSerializer.serialize(model, new ObjectMapper(), getSmallryeFormat(format));
    }

    @Override
    public OpenAPI parse(InputStream is, Format format) throws IOException {
        return OpenApiParser.parse(is, getSmallryeFormat(format), OpenApiConfig.fromConfig(ConfigProvider.getConfig()));
    }

    private io.smallrye.openapi.runtime.io.Format getSmallryeFormat(Format format) {
        switch (format) {
            case JSON:
                return io.smallrye.openapi.runtime.io.Format.JSON;
            case YAML:
                return io.smallrye.openapi.runtime.io.Format.YAML;
            default:
                throw new IllegalArgumentException("Invalid format");
        }
    }

}
