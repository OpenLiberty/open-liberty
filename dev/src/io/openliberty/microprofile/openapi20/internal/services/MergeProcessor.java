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

public interface MergeProcessor {

    /**
     * Create a merged OpenAPI model from a list of OpenAPIProviders.
     * <p>
     * The input objects will not be modified.
     *
     * @param documents the OpenAPI models to merge
     * @return the merged model
     */
    public OpenAPIProvider mergeDocuments(List<OpenAPIProvider> documents);
}
