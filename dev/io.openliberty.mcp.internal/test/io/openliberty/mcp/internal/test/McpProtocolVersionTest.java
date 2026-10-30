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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.mcp.internal.McpProtocolVersion;

public class McpProtocolVersionTest {

    private static final String BETA_PROPERTY = "com.ibm.ws.beta.edition";

    private String originalBetaValue;

    @Before
    public void saveBetaProperty() {
        originalBetaValue = System.getProperty(BETA_PROPERTY);
    }

    @After
    public void restoreBetaProperty() {
        if (originalBetaValue == null) {
            System.clearProperty(BETA_PROPERTY);
        } else {
            System.setProperty(BETA_PROPERTY, originalBetaValue);
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void testParseBetaVersionRejectedOnNonBeta() {
        System.setProperty(BETA_PROPERTY, "false");
        McpProtocolVersion.parse("2026-07-28");
    }

    @Test
    public void testParseBetaVersionAcceptedOnBeta() {
        System.setProperty(BETA_PROPERTY, "true");
        assertEquals(McpProtocolVersion.V_2026_07_28, McpProtocolVersion.parse("2026-07-28"));
    }

    @Test
    public void testParseGaVersionsOnNonBeta() {
        System.setProperty(BETA_PROPERTY, "false");
        assertEquals(McpProtocolVersion.V_2025_11_25, McpProtocolVersion.parse("2025-11-25"));
        assertEquals(McpProtocolVersion.V_2025_06_18, McpProtocolVersion.parse("2025-06-18"));
        assertEquals(McpProtocolVersion.V_2025_03_26, McpProtocolVersion.parse("2025-03-26"));
    }

    @Test
    public void testAvailableValuesExcludesBetaOnNonBeta() {
        System.setProperty(BETA_PROPERTY, "false");
        assertFalse(Arrays.asList(McpProtocolVersion.availableValues()).contains(McpProtocolVersion.V_2026_07_28));
        assertEquals(3, McpProtocolVersion.availableValues().length);
    }

    @Test
    public void testAvailableValuesIncludesBetaOnBeta() {
        System.setProperty(BETA_PROPERTY, "true");
        assertTrue(Arrays.asList(McpProtocolVersion.availableValues()).contains(McpProtocolVersion.V_2026_07_28));
        assertEquals(McpProtocolVersion.values().length, McpProtocolVersion.availableValues().length);
    }

    @Test
    public void testIsAvailable() {
        System.setProperty(BETA_PROPERTY, "false");
        assertFalse(McpProtocolVersion.V_2026_07_28.isAvailable());
        assertTrue(McpProtocolVersion.V_2025_11_25.isAvailable());

        System.setProperty(BETA_PROPERTY, "true");
        assertTrue(McpProtocolVersion.V_2026_07_28.isAvailable());
        assertTrue(McpProtocolVersion.V_2025_11_25.isAvailable());
    }

    @Test(expected = NoSuchElementException.class)
    public void testAdapterRejectsBetaVersionOnNonBeta() throws Exception {
        System.setProperty(BETA_PROPERTY, "false");
        new McpProtocolVersion.McpProtocolVersionAdapter().adaptFromJson("2026-07-28");
    }

    @Test
    public void testAdapterRoundTripsBetaVersionOnBeta() throws Exception {
        System.setProperty(BETA_PROPERTY, "true");
        McpProtocolVersion.McpProtocolVersionAdapter adapter = new McpProtocolVersion.McpProtocolVersionAdapter();
        assertEquals(McpProtocolVersion.V_2026_07_28, adapter.adaptFromJson("2026-07-28"));
        assertEquals("2026-07-28", adapter.adaptToJson(McpProtocolVersion.V_2026_07_28));
    }
}
