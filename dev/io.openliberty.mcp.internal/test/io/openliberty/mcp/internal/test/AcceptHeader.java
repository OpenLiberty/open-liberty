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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openliberty.mcp.internal.HeaderValidation;

public class AcceptHeader {
    @Test
    public void testAcceptHeaderExactlyRequired() {
        assertTrue(HeaderValidation.acceptContains("application/json, text/event-stream", "application/json"));
        assertTrue(HeaderValidation.acceptContains("application/json, text/event-stream", "text/event-stream"));
    }

    @Test
    public void testAcceptHeaderWildcard() {
        assertTrue(HeaderValidation.acceptContains("*/*", "application/json"));
        assertTrue(HeaderValidation.acceptContains("*/*", "text/event-stream"));
    }

    @Test
    public void testAcceptHeaderWithQuality() {
        String mime = "application/json;q=1.0, text/*;q=0.5";
        assertFalse(HeaderValidation.acceptContains("application/json", mime));
        assertFalse(HeaderValidation.acceptContains("text/event-stream", mime));
    }

    @Test
    public void testAcceptHeaderMissingOne() {
        String header = "application/json";
        assertTrue(HeaderValidation.acceptContains(header, "application/json"));
        assertFalse(HeaderValidation.acceptContains(header, "text/event-stream"));
    }

    @Test
    public void testAcceptHeaderInvalid() {
        assertFalse(HeaderValidation.acceptContains("image/png", "application/json"));
    }

    @Test
    public void testAcceptHeaderNull() {
        assertFalse(HeaderValidation.acceptContains(null, "application/json"));
    }
}
