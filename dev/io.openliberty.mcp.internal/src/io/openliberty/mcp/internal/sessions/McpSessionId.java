/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.sessions;

public record McpSessionId(String value) {

    @Override
    public String toString() {
        if (value.length() <= 3)
            return value;
        return value.substring(0, 3) + "*".repeat(value.length() - 3);
    }

}
