/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.test.merge.parts;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.openliberty.microprofile.openapi20.internal.OpenAPIProvider;
import io.openliberty.microprofile.openapi20.internal.merge.MergeProcessor;

public class TestUtil {

    /**
     * Create an OpenAPIProvdier for the given model with an application path of {@code /}
     *
     * @param openapi the openapi model
     * @return the provider
     */
    public static OpenAPIProvider createProvider(OpenAPI openapi) {
        return createProvider(openapi, "/");
    }

    /**
     * Create an OpenAPIProvider for the given model
     *
     * @param openapi the openapi model
     * @param applicationPath the application path for the returned provider
     * @return the provider
     */
    public static OpenAPIProvider createProvider(OpenAPI openapi, String applicationPath) {
        return new OpenAPIProviderCustom(openapi, applicationPath);
    }

    /**
     * Merge several OpenAPI models using {@link MergeProcessor}
     *
     * @param docs the openapi models
     * @return the merged model
     */
    public static OpenAPI merge(OpenAPI... docs) {
        List<OpenAPIProvider> providers = new ArrayList<>();
        for (OpenAPI doc : docs) {
            providers.add(createProvider(doc));
        }
        return merge(providers);
    }

    /**
     * Merge several OpenAPI providers using {@link MergeProcessor}
     *
     * @param providers the providers
     * @return the merged model
     */
    public static OpenAPI merge(List<OpenAPIProvider> providers) {
        OpenAPIProvider merged = MergeProcessor.mergeDocuments(providers);
        assertThat("Merge problems", merged.getMergeProblems(), empty());
        return merged.getModel();
    }
}
