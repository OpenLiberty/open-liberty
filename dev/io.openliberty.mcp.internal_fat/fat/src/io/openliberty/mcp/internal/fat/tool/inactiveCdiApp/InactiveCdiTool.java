/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.inactiveCdiApp;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.content.TextContent;

/**
 *
 */
public class InactiveCdiTool {
    @Tool(name = "toolWithoutCDI", title = "Tool Without CDI", description = "Should not be called as the class is not a CDI bean")
    public TextContent toolWithoutCDI() {
        return new TextContent("Hello world!");
    }
}
