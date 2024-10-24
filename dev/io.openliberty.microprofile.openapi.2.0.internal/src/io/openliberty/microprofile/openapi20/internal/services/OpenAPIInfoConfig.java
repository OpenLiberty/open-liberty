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

import java.util.Optional;

import org.eclipse.microprofile.openapi.models.info.Info;

/**
 * Provides the {@link Info} read from the server.xml configuration
 */
public interface OpenAPIInfoConfig {

    /**
     * Returns the {@code Info} object constructed from the configuration.
     *
     * @return the {@code Info}, if one is configured, otherwise an empty {@code Optional}
     */
    public Optional<Info> getInfo();
}
