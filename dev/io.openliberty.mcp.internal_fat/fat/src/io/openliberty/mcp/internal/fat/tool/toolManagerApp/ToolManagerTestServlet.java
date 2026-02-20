/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.toolManagerApp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolManager.ToolInfo;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

/**
 * Conducts in-server tests on the ToolManager
 */
@WebServlet("/toolManagerTestServlet")
public class ToolManagerTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;
    @Inject
    private ToolManager toolManager;

    @Test
    public void testMethodToolInfo() {
        // Defined by ToolManagerStartupTestBean.methodTool
        ToolInfo methodTool = toolManager.getTool("methodTool");

        assertEquals("name", "methodTool", methodTool.name());
        assertEquals("arguments.size", 0, methodTool.arguments().size());
        assertNull("description", methodTool.description());
        assertNull("title", methodTool.title());
        assertEquals("annotations", Optional.empty(), methodTool.annotations());
        assertTrue("isMethod", methodTool.isMethod());
        assertNotNull("createdAt", methodTool.createdAt());
    }

    @Test
    public void testCreatedToolInfo() {
        // Created in ToolManagerStartupTestBean.createCreatedTool
        ToolInfo startupTool = toolManager.getTool("startup-tool");

        assertEquals("name", "startup-tool", startupTool.name());
        assertEquals("arguments.size", 0, startupTool.arguments().size());
        assertNull("description", startupTool.description());
        assertNull("title", startupTool.title());
        assertEquals("annotations", Optional.empty(), startupTool.annotations());
        assertFalse("isMethod", startupTool.isMethod());
        assertNotNull("createdAt", startupTool.createdAt());
    }

    @Test
    public void testRemovedToolInfo() {
        // Removed in ToolManagerStartupTestBean.removeMethodTool
        ToolInfo removedMethodTool = toolManager.getTool("methodToolToBeRemoved");
        assertNull("removedMethodTool", removedMethodTool);
    }
}
