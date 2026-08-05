/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.unsupportedAnnotationApp;

import org.mcpjava.server.completion.CompletePrompt;
import org.mcpjava.server.completion.CompleteResourceTemplate;
import org.mcpjava.server.prompts.Prompt;
import org.mcpjava.server.resources.Resource;
import org.mcpjava.server.resources.ResourceTemplate;
import org.mcpjava.server.tools.Tool;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A CDI bean that deliberately uses unsupported MCP annotations alongside a valid {@code @Tool}
 * method. The application must still start successfully, but a warning must be logged for each
 * method carrying an unsupported annotation.
 */
@ApplicationScoped
public class UnsupportedAnnotationTools {

    @Tool(description = "A valid tool that should still be registered")
    public String validTool() {
        return "ok";
    }

    @Prompt
    public void promptMethod() {}

    @Resource(uri = "")
    public void resourceMethod() {}

    @ResourceTemplate(uriTemplate = "")
    public void resourceTemplateMethod() {}

    @CompletePrompt(value = "")
    public void completePromptMethod() {}

    @CompleteResourceTemplate(value = "")
    public void completeResourceTemplateMethod() {}
}
