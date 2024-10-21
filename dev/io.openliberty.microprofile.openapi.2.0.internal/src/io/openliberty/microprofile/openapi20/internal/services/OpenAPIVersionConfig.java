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

import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * Applies any version configuration provided by the user.
 */
public interface OpenAPIVersionConfig {

    /**
     * Updates the {@code openapi} property of the model object according to the configuration
     *
     * @param model the {@code OpenAPI} model object to update
     */
    public void applyConfig(OpenAPI model);
}
