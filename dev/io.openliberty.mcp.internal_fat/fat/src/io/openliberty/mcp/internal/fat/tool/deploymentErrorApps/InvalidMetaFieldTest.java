/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.deploymentErrorApps;

import org.mcpjava.server.MetaField;
import org.mcpjava.server.MetaField.Type;
import org.mcpjava.server.tools.Tool;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InvalidMetaFieldTest {

    @Tool
    @MetaField(prefix = "a&b", name = "a", value = "foo")
    public String invalidPrefix() {
        return null;
    }

    @Tool
    @MetaField(prefix = "a.b", name = "c/d", value = "foo")
    public String invalidName() {
        return null;
    }

    @Tool
    @MetaField(prefix = "a.b", name = "c", value = "foo", type = Type.INT)
    public String invalidIntValue() {
        return null;
    }

    @Tool
    @MetaField(prefix = "a.b", name = "c", value = "{\"foo\"", type = Type.JSON)
    public String invalidJsonValue() {
        return null;
    }
}
