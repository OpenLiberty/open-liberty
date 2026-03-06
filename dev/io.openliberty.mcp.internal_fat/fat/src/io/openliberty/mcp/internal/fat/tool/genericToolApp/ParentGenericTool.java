/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.genericToolApp;

import java.util.List;

import io.openliberty.mcp.annotations.Schema;
import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;

/**
 *
 */
public class ParentGenericTool<T> {

    @Tool(name = "addGenericToGenericArray", title = "adds generic to generic Array", description = "adds person to Generic Array, returns nothing", structuredContent = true)
    public @Schema(description = "Returns list of  object") ListWrapper<T> addGenericToGenericArray(@ToolArg(name = "generic list 1",
                                                                                                             description = "List of generics 1") T[] list1,
                                                                                                    @ToolArg(name = "generic list 2",
                                                                                                             description = "List of generics 1 ") List<T>[] list2,
                                                                                                    @ToolArg(name = "generic", description = "Generic object") T item) {
        return new ListWrapper<>(list2);
    }
}
