/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static io.openliberty.mcp.internal.LocalhostHeaderChecks.isLocalAddr;
import static io.openliberty.mcp.internal.LocalhostHeaderChecks.isOriginValidForLocalhost;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LocalhostHeaderChecksTest {

    @Test
    public void testIsLocalAddr() {
        assertTrue(isLocalAddr("127.0.0.1"));
        assertTrue(isLocalAddr("localhost"));
        assertTrue(isLocalAddr("::1"));
        assertTrue(isLocalAddr("[::1]"));
        assertFalse(isLocalAddr("192.168.1.1"));
        assertFalse(isLocalAddr("10.0.0.1"));
        assertFalse(isLocalAddr("example.com"));
    }

    @Test
    public void testIsOriginValidForLocalhost() {
        assertTrue(isOriginValidForLocalhost(null));
        assertTrue(isOriginValidForLocalhost("http://localhost"));
        assertTrue(isOriginValidForLocalhost("http://127.0.0.1"));
        assertTrue(isOriginValidForLocalhost("http://localhost:9080"));
        assertTrue(isOriginValidForLocalhost("http://[::1]"));
        assertTrue(isOriginValidForLocalhost("http://[::1]:9080"));

        assertFalse(isOriginValidForLocalhost("http://example.com"));
        assertFalse(isOriginValidForLocalhost("http://192.168.1.1"));
        assertFalse(isOriginValidForLocalhost("not a valid uri ://"));
        assertFalse(isOriginValidForLocalhost("file:///no-host"));
    }
}
