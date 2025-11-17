/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.businessExceptionApp;

import java.io.IOException;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import io.openliberty.mcp.annotations.WrapBusinessError;
import io.openliberty.mcp.tools.ToolCallException;
import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class ToolErrorHandlingTools {

    //////////
    // Exception Handling Tools
    //////////

    @Tool(name = "businessErrorTool", title = "Business Error Handler", description = "This tool throws error on business tool")
    public String businessErrorTool(@ToolArg(name = "input", description = "Handles business tool error") String input) {
        throw new ToolCallException("Invalid business input: " + input);
    }

    @Tool(name = "unwrappedExceptionTool", title = "Unwrapped Exception Tool", description = "Throws unwrapped RuntimeException")
    public String rawExceptionTOol(@ToolArg(name = "input", description = "Throws raw exception") String input) {
        throw new IllegalStateException("rawExceptionTool test error for \" + input");
    }

    @Tool(name = "wrappedNoArgsTool", title = "Tool with no Args", description = "Throws raw exception with @WrapBusinessException but no types listed")
    @WrapBusinessError()
    public String wrappedNoArgsTool(@ToolArg(name = "input", description = "Error Triggers") String input) {
        throw new IllegalArgumentException("Wrapped error for input: " + input);
    }

    @Tool(name = "listedWrappedExceptionTool", title = "Listed Wrapped Exception", description = "Tool throws exception listed in WrapBusinessException")
    @WrapBusinessError({ IllegalArgumentException.class })
    public String listedWrappedExceptionTool(@ToolArg(name = "input", description = "Invalid input") String input) {
        throw new IllegalArgumentException("Invalid business input: " + input);
    }

    @Tool(name = "superclassWrappedExceptionTool", title = "Superclass Listed", description = "Tool throws subclass exception")
    @WrapBusinessError({ RuntimeException.class })
    public String superclassWrappedExceptionTool(@ToolArg(name = "input", description = "This is input") String input) {
        throw new CustomBusinessException("Invalid input for superclass: " + input);
    }

    @Tool(name = "excludeExceptionTool", title = "Excludes exception Tool", description = "Throws a business exception not listed in WrapBusinessException")
    @WrapBusinessError({ IllegalArgumentException.class })
    public String excludeExceptionTool(@ToolArg(name = "input") String input) {
        throw new UnwrappedBusinessException("Internal server error: " + input);
    }

    @Tool(name = "checkedExceptionTool", title = "Checked Exception", description = "Throws a checked exception")
    @WrapBusinessError({ IOException.class })
    public String checkedExceptionTool(@ToolArg(name = "input", description = "Checked exception Triggers") String input) throws IOException {
        throw new IOException("Checked error for: " + input);
    }

    @Tool(name = "unwrappedCheckedExceptionTool", title = "Unwrapped Checked", description = "Throws IOException but not listed")
    @WrapBusinessError({})
    public String unwrappedCheckedExceptionTool(@ToolArg(name = "input", description = "Triggers unwrapped checked") String input) throws IOException {
        throw new IOException("Unwrapped checked error for: " + input);
    }
}
