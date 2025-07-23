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
package io.openliberty.microprofile.openapi20.internal;

import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;

/**
 * An OpenAPIProvider for a model created by merging models from multiple modules
 */
public class MergedOpenAPIProvider implements OpenAPIProvider {

    private final OpenAPI model;
    private final List<String> mergeProblems;
    private final String applicationPath;

    /**
     * @param model the merged OpenAPI model
     * @param mergeProblems the list of merge problems
     */
    public MergedOpenAPIProvider(OpenAPI model, List<String> mergeProblems) {
        this.model = model;
        this.mergeProblems = Collections.unmodifiableList(mergeProblems);
        this.applicationPath = null;
    }

    /**
     * @param model the merged OpenAPI model
     * @param mergeProblems the list of merge problems
     * @param applicationPath the application path for the merged model
     */
    public MergedOpenAPIProvider(OpenAPI model, List<String> mergeProblems, String applicationPath) {
        this.model = model;
        this.mergeProblems = Collections.unmodifiableList(mergeProblems);
        this.applicationPath = applicationPath;
    }

    @Override
    public String getApplicationPath() {
        return applicationPath;
    }

    @Override
    public OpenAPI getModel() {
        return model;
    }

    @Override
    public String toString() {
        return "Merged model";
    }

    @Override
    public List<String> getMergeProblems() {
        return mergeProblems;
    }

}
