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

}
