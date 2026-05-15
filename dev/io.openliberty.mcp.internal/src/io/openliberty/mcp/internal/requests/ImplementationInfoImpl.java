/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mcpjava.server.Icon;
import org.mcpjava.server.ImplementationInfo;

/**
 * Describes an MCP client or server implementation
 */
public record ImplementationInfoImpl(List<Icon> icons,
                                     String name,
                                     String title,
                                     String version,
                                     String descriptionValue,
                                     String websiteUrlValue) implements ImplementationInfo {

    public ImplementationInfoImpl {
        Objects.requireNonNull(icons, "icons must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(version, "version must not be null");
    }

    @Override
    public String title() {
        return title != null ? title : name;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<String> description() {
        return Optional.ofNullable(descriptionValue);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<String> websiteUrl() {
        return Optional.ofNullable(websiteUrlValue);
    }

}
