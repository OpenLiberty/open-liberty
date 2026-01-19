/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.deploymentErrorApps;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InvalidToolNameTest {

    /////////////////////////////
    @Tool(name = "validTool")
    public String validTool(@ToolArg(name = "arg") String arg) {
        return arg;
    }

    @Tool(name = "validTool1")
    public String validTool1(@ToolArg(name = "arg") String arg) {
        return arg;
    }

    @Tool(name = "valid_Tool2")
    public String valid_Tool2(@ToolArg(name = "arg") String arg) {
        return arg;
    }

    @Tool(name = "valid_Tool.3")
    public String valid_Tool3(@ToolArg(name = "arg") String arg) {
        return arg;
    }

    @Tool(name = "valid_Tool-Name.4")
    public String valid_ToolName4(@ToolArg(name = "arg") String arg) {
        return arg;
    }

    @Tool(name = "invalid tool")
    public String invalidTool1(@ToolArg(name = "arg") String arg) {
        return arg;
    }

    @Tool(name = "invalidtool2!")
    public String invalidTool2(@ToolArg(name = "arg") String arg) {
        return arg;
    }

    @Tool(name = "invalid,tool3")
    public String invalidTool3(@ToolArg(name = "arg") String arg) {
        return arg;
    }

    @Tool(name = "")
    public String invalidTool4(@ToolArg(name = "arg") String arg) {
        return arg;
    }

    @Tool(name = "openlibertyopenliberty openlibertyopenliberty_openlibertyopenlibertyopenlibertyopenlibertyopenliberty openlibertyopenliberty_openlibertyopenlibertyopenliberty")
    public String invalidTool5(@ToolArg(name = "arg") String arg) {
        return arg;
    }

}
