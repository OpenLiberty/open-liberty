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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mcpjava.server.Icon;
import org.mcpjava.server.ImplementationInfo;

import jakarta.json.bind.adapter.JsonbAdapter;

/**
 * Describes an MCP client or server implementation.
 */
public record ImplementationInfoImpl(List<Icon> icons,
                                     String name,
                                     String title,
                                     String version,
                                     String descriptionValue,
                                     String websiteUrlValue) implements ImplementationInfo {

    /**
     * Used where we have to provide an ImplementationInfo, but we don't actually have the information.
     * E.g. where the server is in stateless mode
     */
    public static final ImplementationInfoImpl UNKNOWN = new ImplementationInfoImpl(Collections.emptyList(),
                                                                                    "Unknown",
                                                                                    null,
                                                                                    "0.0",
                                                                                    null,
                                                                                    null);

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

    /**
     * Transfer Object that exactly matches the structure of the object in the protocol
     */
    public static class ImplementationInfoTO {
        public List<Icon> icons;
        public String name;
        public String title;
        public String version;
        public String description;
        public String websiteUrl;
    }

    /**
     * Adapts between the API object and the transfer object
     */
    public static class Adapter implements JsonbAdapter<ImplementationInfo, ImplementationInfoTO> {

        @Override
        public ImplementationInfo adaptFromJson(ImplementationInfoTO to) throws Exception {
            return new ImplementationInfoImpl(to.icons == null ? List.of() : to.icons,
                                              to.name,
                                              to.title,
                                              to.version,
                                              to.description,
                                              to.websiteUrl);
        }

        @Override
        public ImplementationInfoTO adaptToJson(ImplementationInfo info) throws Exception {
            ImplementationInfoImpl data = (ImplementationInfoImpl) info;
            // Note differences:
            // using data.title, not data.title() to bypass logic which returns the name for the title
            // Using descriptionValue and websiteUrlValue which can be null
            ImplementationInfoTO to = new ImplementationInfoTO();
            to.icons = data.icons.isEmpty() ? null : data.icons;
            to.name = data.name;
            to.title = data.title;
            to.version = data.version;
            to.description = data.descriptionValue;
            to.websiteUrl = data.websiteUrlValue;
            return to;
        }

    }

}
