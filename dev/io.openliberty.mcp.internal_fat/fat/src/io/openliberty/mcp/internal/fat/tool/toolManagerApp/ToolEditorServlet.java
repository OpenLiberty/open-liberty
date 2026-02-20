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

import java.io.IOException;

import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet("/editTools")
public class ToolEditorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    public static final String DYNAMIC_REPEATER = "dynamicRepeater";

    @Inject
    private ToolManager toolManager;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JsonObject request = readRequest(req);
        String action = request.getString("action");
        switch (action) {
            case "add" -> addDynamicRepeaterTool(request);
            case "remove" -> removeDynamicRepeaterTool();
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    private void addDynamicRepeaterTool(JsonObject request) {
        int repeatCount = request.getInt("repeatCount");
        toolManager.newTool(DYNAMIC_REPEATER)
                   .setDescription("repeat string " + repeatCount + " times")
                   .addArgument("inputString", null, true, String.class)
                   .setHandler(req -> {
                       String input = (String) req.args().get("inputString");
                       StringBuilder sb = new StringBuilder();
                       for (int i = 0; i < repeatCount; i++) {
                           sb.append(input);
                       }
                       return ToolResponse.success(sb.toString());
                   })
                   .register();
    }

    private void removeDynamicRepeaterTool() {
        toolManager.removeTool(DYNAMIC_REPEATER);
    }

    private JsonObject readRequest(HttpServletRequest req) throws IOException {
        try (JsonReader p = Json.createReader(req.getInputStream())) {
            return p.readObject();
        }
    }

}
