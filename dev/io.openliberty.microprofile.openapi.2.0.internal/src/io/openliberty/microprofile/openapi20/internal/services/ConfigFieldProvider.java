/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.services;

import java.util.Collection;

import io.smallrye.openapi.api.OpenApiConfig;

/**
 * Provides the {@link ConfigField ConfigFields} for a specific version of the {@link OpenApiConfig} interface
 * <p>
 * Each version of the mpOpenAPI feature must provide an implementation of this service which returns a collection of {@link ConfigField ConfigFields} for the version of
 * {@link OpenApiConfig} used by that feature version.
 * <p>
 * This service must be used whenever we need to iterate over all fields of {@link OpenApiConfig}
 */
public interface ConfigFieldProvider {

    /**
     * Get the {@link ConfigField ConfigFields} which can be read from {@link OpenApiConfig}
     *
     * @return the fields which can be read from {@code OpenApiConfig}
     */
    public Collection<ConfigField> getConfigFields();

}
