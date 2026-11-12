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

import java.time.Duration;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.config.McpServerConfigProps;

/**
 * Unit tests for the protocol version handling of {@link McpServerConfigProps}, which implements the beta
 * {@code minimumProtocolVersion} and {@code maximumProtocolVersion} configuration:
 * <ul>
 * <li>the default range, which is the whole range on a beta runtime and 2025-03-26 to 2025-11-25 otherwise</li>
 * <li>parsing a configured value, including the {@code latest} keyword</li>
 * <li>deciding which versions a request may use</li>
 * </ul>
 * Reading the attributes from server.xml, ignoring them on a non-beta runtime, and rejecting requests are
 * covered by the FAT. The beta setting is read when the defaults are calculated, so each test sets
 * {@code com.ibm.ws.beta.edition} first and the original value is restored afterwards.
 */
public class McpServerConfigPropsTest {

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

    /**
     * On a beta runtime the default maximum is the latest version, so the default range accepts every version.
     */
    @Test
    public void testDefaultsOnBeta() {
        System.setProperty(BETA_PROPERTY, "true");
        assertEquals(McpProtocolVersion.V_2025_03_26, McpServerConfigProps.defaultMinimum());
        assertEquals(McpProtocolVersion.latest(), McpServerConfigProps.defaultMaximum());
        McpServerConfigProps config = configWithOtherSettingsOnly();
        for (McpProtocolVersion version : McpProtocolVersion.values()) {
            assertTrue(version.getVersion() + " should be accepted by default on beta", config.isAccepted(version));
        }
    }

    /**
     * {@link McpProtocolVersion#isBetween} is inclusive at both ends, and excludes versions older than the
     * minimum or newer than the maximum.
     */
    @Test
    public void testIsBetween() {
        McpProtocolVersion min = McpProtocolVersion.V_2025_06_18;
        McpProtocolVersion max = McpProtocolVersion.V_2025_11_25;
        assertTrue(McpProtocolVersion.V_2025_06_18.isBetween(min, max));
        assertTrue(McpProtocolVersion.V_2025_11_25.isBetween(min, max));
        assertFalse(McpProtocolVersion.V_2025_03_26.isBetween(min, max));
        assertFalse(McpProtocolVersion.V_2026_07_28.isBetween(min, max));
    }

    /**
     * A range where the minimum is newer than the maximum contains no versions. The configuration rejects
     * such a range, so this checks that {@link McpProtocolVersion#isBetween} reports it as empty.
     */
    @Test
    public void testIsBetweenWithInvertedRange() {
        for (McpProtocolVersion version : McpProtocolVersion.values()) {
            assertFalse(version.isBetween(McpProtocolVersion.V_2025_11_25, McpProtocolVersion.V_2025_06_18));
        }
    }

    /**
     * A range where the minimum equals the maximum contains only that version. The neighbours on both
     * sides are excluded.
     */
    @Test
    public void testIsBetweenWithSingleVersionRange() {
        McpProtocolVersion only = McpProtocolVersion.V_2025_06_18;
        assertTrue(only.isBetween(only, only));
        assertFalse(McpProtocolVersion.V_2025_11_25.isBetween(only, only));
        assertFalse(McpProtocolVersion.V_2025_03_26.isBetween(only, only));
    }

    /**
     * Creates a configuration with the given bounds. A null bound is replaced by its default, which depends
     * on the beta setting at the time of the call.
     */
    private static McpServerConfigProps configWithRange(McpProtocolVersion min, McpProtocolVersion max) {
        return new McpServerConfigProps(false, null, McpServerConfigProps.FALLBACK_PATH, null, Duration.ofMinutes(10), null,
                                        McpServerConfigProps.DEFAULT_ASYNC_TIMEOUT_MS, min, max, McpConfig.EnableEndpoint.AUTO);
    }

    /**
     * Creates a configuration which sets other attributes but leaves {@code minimumProtocolVersion} and
     * {@code maximumProtocolVersion} unset, as when a user configures an {@code <mcp>} element in server.xml
     * without a protocol version range. The unset bounds are replaced by their defaults, which depend on the
     * beta setting at the time of the call.
     */
    private static McpServerConfigProps configWithOtherSettingsOnly() {
        return new McpServerConfigProps(true, "app", "/custom", null, Duration.ofMinutes(5), null, 1_000, null, null, McpConfig.EnableEndpoint.AUTO);
    }

    /**
     * A version is accepted if it is inside the configured range, inclusive at both ends, and
     * {@link McpServerConfigProps#acceptedProtocolVersions()} lists the accepted versions newest first.
     */
    @Test
    public void testIsAcceptedUsesConfiguredRange() {
        System.setProperty(BETA_PROPERTY, "true");
        McpServerConfigProps config = configWithRange(McpProtocolVersion.V_2025_06_18, McpProtocolVersion.V_2025_11_25);
        assertFalse(config.isAccepted(McpProtocolVersion.V_2025_03_26));
        assertTrue(config.isAccepted(McpProtocolVersion.V_2025_06_18));
        assertTrue(config.isAccepted(McpProtocolVersion.V_2025_11_25));
        assertFalse(config.isAccepted(McpProtocolVersion.V_2026_07_28));
        assertEquals(List.of(McpProtocolVersion.V_2025_11_25, McpProtocolVersion.V_2025_06_18), config.acceptedProtocolVersions());
    }

    /**
     * A beta version is never accepted on a non-beta runtime, even if a range which includes it is supplied.
     * This is the fence that makes requests with the beta version fail on a non-beta runtime.
     */
    @Test
    public void testIsAcceptedRejectsBetaVersionOnNonBeta() {
        System.setProperty(BETA_PROPERTY, "false");
        McpServerConfigProps config = configWithRange(McpProtocolVersion.V_2025_03_26, McpProtocolVersion.V_2026_07_28);
        assertFalse(config.isAccepted(McpProtocolVersion.V_2026_07_28));
        assertEquals(List.of(McpProtocolVersion.V_2025_11_25, McpProtocolVersion.V_2025_06_18, McpProtocolVersion.V_2025_03_26),
                     config.acceptedProtocolVersions());
    }

    /**
     * When no protocol version range is configured on a non-beta runtime, the minimum is 2025-03-26 and the
     * maximum is 2025-11-25, so the beta version is not accepted.
     */
    @Test
    public void testDefaultsOnNonBeta() {
        System.setProperty(BETA_PROPERTY, "false");
        assertEquals(McpProtocolVersion.V_2025_03_26, McpServerConfigProps.defaultMinimum());
        assertEquals(McpProtocolVersion.V_2025_11_25, McpServerConfigProps.defaultMaximum());
        McpServerConfigProps config = configWithOtherSettingsOnly();
        assertEquals(McpProtocolVersion.V_2025_03_26, config.minimumProtocolVersion());
        assertEquals(McpProtocolVersion.V_2025_11_25, config.maximumProtocolVersion());
        assertEquals(List.of(McpProtocolVersion.V_2025_11_25, McpProtocolVersion.V_2025_06_18, McpProtocolVersion.V_2025_03_26),
                     config.acceptedProtocolVersions());
        assertFalse(config.isAccepted(McpProtocolVersion.V_2026_07_28));
    }

    /**
     * The {@code latest} keyword selects the newest available version, and any other value is parsed as a
     * protocol version string.
     */
    @Test
    public void testParseProtocolVersion() {
        System.setProperty(BETA_PROPERTY, "true");
        assertEquals(McpProtocolVersion.latest(), McpServerConfigProps.parseProtocolVersion("latest"));
        assertEquals(McpProtocolVersion.V_2025_06_18, McpServerConfigProps.parseProtocolVersion("2025-06-18"));
    }
}
