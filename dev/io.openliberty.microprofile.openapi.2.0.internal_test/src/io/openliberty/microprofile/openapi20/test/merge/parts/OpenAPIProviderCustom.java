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

import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;

/**
 *
 */
public class OpenAPIProviderCustom implements OpenAPIProvider {

    private final OpenAPI openAPI;
    private final String applicationPath;

    public OpenAPIProviderCustom(OpenAPI openapi, String applicationPath) {
        this.openAPI = openapi;
        this.applicationPath = applicationPath;
    }

    @Override
    public String getApplicationPath() {
        return applicationPath;
    }

    @Override
    public OpenAPI getModel() {
        return this.openAPI;
    }

    @Override
    public List<String> getMergeProblems() {
        return Collections.emptyList();
    }

}
