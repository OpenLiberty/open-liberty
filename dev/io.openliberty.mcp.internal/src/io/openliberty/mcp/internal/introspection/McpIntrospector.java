/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.introspection;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.wsspi.logging.Introspector;

import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolRegistry;

@SuppressWarnings("restriction")
@Component(service = { McpIntrospector.class, Introspector.class }, configurationPolicy = ConfigurationPolicy.IGNORE)
public class McpIntrospector implements Introspector {

//     Map from app name -> tool registry
    private static final Map<String, ToolRegistry> registryByApp = new ConcurrentHashMap<>();

    public void register(String appName, ToolRegistry registry) {
        registryByApp.put(appName, registry);
    }

    public void unregister(String appName) {
        registryByApp.remove(appName);
    }

    /**
     * Called by Liberty to dump introspection info
     */
    @Override
    public void introspect(PrintWriter writer) {
        if (registryByApp.isEmpty()) {
            writer.println("No MCP tools registered.");
            return;
        }
        writer.println("=== MCP Tool Introspection ===");

        for (Map.Entry<String, ToolRegistry> entry : registryByApp.entrySet()) {
            String appName = entry.getKey();
            ToolRegistry registry = entry.getValue();

            writer.println("Application: " + appName);

            if (!registry.hasTools()) {
                writer.println("  No tools registered.");
                continue;
            }

            for (ToolMetadata tool : registry.getAllTools()) {
                writer.println("  Tool: " + tool.name());
                writer.println("  Description: " + tool.description());
                writer.println("  Args: " + tool.arguments());
            }
        }

        writer.println("=== End MCP Tool Introspection ===");
    }

    @Override
    public String getIntrospectorName() {
        return "MCPIntrospector";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Displays MCP tools registered by each application.";
    }

}
