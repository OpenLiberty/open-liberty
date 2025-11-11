/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openliberty.mcp.internal.sessions.McpSessionId;

public class SessionIdTest {

    @Test
    public void testSessionIdToString() {
        McpSessionId id = new McpSessionId("123456789");
        assertEquals("123******", id.toString());
    }

}
