/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.services;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.OASModelReader;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.Index;

import com.ibm.wsspi.adaptable.module.Container;

import io.smallrye.openapi.api.OpenApiConfig;

/**
 * Generates the OpenAPI model from an application.
 * <p>
 * Separated out into a service to account API changes in different versions of SmallRye OpenAPI.
 */
public interface ModelGenerator {

    /**
     * Generate the OpenAPI model for an application.
     * <p>
     * The returned model will take account of
     * <ul>
     * <li>any {@link OASModelReader} included in the application and configured for use
     * <li>a static openapi document included in the application
     * <li>Jakarta REST resources and MP OpenAPI annotations found within the application classes
     * <li>any configured {@link OASFilter}s that should be applied
     * </ul>
     *
     * @param config the config
     * @param appContainer the container for the application
     * @param appClassloader the application classloader
     * @param appClassloader a thread context classloader for the application
     * @param index a jandex index, including only the classes which should be scanned, or @{code null} if scanning is disabled
     * @return the OpenAPI model or {@code null} if nothing was found to document in the application
     */
    public OpenAPI generateModel(OpenApiConfig config,
                                 Container appContainer,
                                 ClassLoader appClassloader,
                                 ClassLoader threadContextClassloader,
                                 Index index);

}
