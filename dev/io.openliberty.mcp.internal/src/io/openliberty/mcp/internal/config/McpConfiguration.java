/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.config;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Configuration for the MCP Server feature.
 * Handles server.xml element <mcpServer stateless="true|false"/>.
 */

@Component(service = McpConfiguration.class, configurationPolicy = org.osgi.service.component.annotations.ConfigurationPolicy.OPTIONAL)
public class McpConfiguration {

    private static final String STATELESS_KEY = "stateless";

    private boolean stateless = false;

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> config) {

        Object value = config.get(STATELESS_KEY);

        stateless = Boolean.parseBoolean(String.valueOf(value));
    }

    public boolean isStateless() {
        return stateless;
    }
}
