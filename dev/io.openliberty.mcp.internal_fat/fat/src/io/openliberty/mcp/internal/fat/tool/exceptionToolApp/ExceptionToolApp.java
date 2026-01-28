/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.exceptionToolApp;

import io.openliberty.mcp.annotations.Tool;
import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class ExceptionToolApp {

    @Tool(name = "exceptionTool", title = "Multi chain exception", description = "creates a chain of exceptions", structuredContent = false)
    public void exceptionTool() throws Exception {
        exceptionChain(3);
    }

    private void exceptionChain(int count) throws Exception {
        try {
            if (count > 0) {
                exceptionChain(count - 1);
            } else {
                throw new RuntimeException("Root Exception");
            }
        } catch (Exception e) {
            throw new Exception("Exception at level " + count, e);
        }
    }
}
