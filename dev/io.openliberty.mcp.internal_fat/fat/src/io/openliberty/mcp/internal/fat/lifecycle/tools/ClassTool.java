/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.lifecycle.tools;

import java.util.logging.Logger;

import com.ibm.websphere.simplicity.log.Log;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;

/**
 *
 */
@Dependent
public class ClassTool {
    private static final Logger LOG = Logger.getLogger(ClassTool.class.getName());

    public static void info(Class<?> clazz, String method, String message) {
        Log.info(clazz, method, message);
    }

    private String id;

    @PostConstruct
    void init() {
        id = "ClassTool#" + System.identityHashCode(this);
        LOG.info("[LIFECYCLE] @PostConstruct ClassTool fired (" + id + ")");
    }

    @PreDestroy
    void destroy() {
        LOG.info("[LIFECYCLE] @PreDestroy ClassTool (" + id + ")");
    }

    @Tool(name = "sayHello", title = "Greets by name", description = "Returns a friendly greeting")
    public String sayHello(@ToolArg(name = "name", description = "person to greet") String name) {
        LOG.info("[LOGGED] Class Tool logged");
        return "Hello, " + name;
    }
}
