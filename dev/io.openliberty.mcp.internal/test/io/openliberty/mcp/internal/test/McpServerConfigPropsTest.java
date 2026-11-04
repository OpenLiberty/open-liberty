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
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.mcp.internal.McpProtocolVersion;
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
        McpServerConfigProps config = configWithRange(null, null);
        for (McpProtocolVersion version : McpProtocolVersion.values()) {
            assertTrue(version.getVersion() + " should be accepted by default on beta", config.isAccepted(version));
        }
    }

    /**
     * {@link McpServerConfigProps#isBetween} is inclusive at both ends, and excludes versions older than the
     * minimum or newer than the maximum.
     */
    @Test
    public void testIsBetween() {
        McpProtocolVersion min = McpProtocolVersion.V_2025_06_18;
        McpProtocolVersion max = McpProtocolVersion.V_2025_11_25;
        assertTrue(McpServerConfigProps.isBetween(McpProtocolVersion.V_2025_06_18, min, max));
        assertTrue(McpServerConfigProps.isBetween(McpProtocolVersion.V_2025_11_25, min, max));
        assertFalse(McpServerConfigProps.isBetween(McpProtocolVersion.V_2025_03_26, min, max));
        assertFalse(McpServerConfigProps.isBetween(McpProtocolVersion.V_2026_07_28, min, max));
    }

    /**
     * A range where the minimum is newer than the maximum contains no versions. The configuration rejects
     * such a range, so this checks that {@link McpServerConfigProps#isBetween} reports it as empty.
     */
    @Test
    public void testIsBetweenWithInvertedRange() {
        for (McpProtocolVersion version : McpProtocolVersion.values()) {
            assertFalse(McpServerConfigProps.isBetween(version, McpProtocolVersion.V_2025_11_25, McpProtocolVersion.V_2025_06_18));
        }
    }

    /**
     * Creates a configuration with the given bounds. A null bound is replaced by its default, which depends
     * on the beta setting at the time of the call.
     */
    private static McpServerConfigProps configWithRange(McpProtocolVersion min, McpProtocolVersion max) {
        return new McpServerConfigProps(false, null, McpServerConfigProps.FALLBACK_PATH, null, Duration.ofMinutes(10), null,
                                        McpServerConfigProps.DEFAULT_ASYNC_TIMEOUT_MS, min, max);
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
     * On a non-beta runtime the server behaves as if the minimum is 2025-03-26 and the maximum is 2025-11-25,
     * so the beta version is not accepted by default.
     */
    @Test
    public void testDefaultsOnNonBeta() {
        System.setProperty(BETA_PROPERTY, "false");
        assertEquals(McpProtocolVersion.V_2025_03_26, McpServerConfigProps.defaultMinimum());
        assertEquals(McpProtocolVersion.V_2025_11_25, McpServerConfigProps.defaultMaximum());
        McpServerConfigProps config = configWithRange(null, null);
        assertEquals(List.of(McpProtocolVersion.V_2025_11_25, McpProtocolVersion.V_2025_06_18, McpProtocolVersion.V_2025_03_26),
                     config.acceptedProtocolVersions());
        assertFalse(config.isAccepted(McpProtocolVersion.V_2026_07_28));
    }

    /**
     * Null protocol versions in the configuration are replaced by the defaults.
     * <p>
     * This creates a new instance rather than using {@code DEFAULT_CONFIG}, which is a constant created when the
     * class is loaded and so would reflect whichever beta setting was in effect at that point.
     */
    @Test
    public void testNullProtocolVersionsUseDefaults() {
        System.setProperty(BETA_PROPERTY, "false");
        McpServerConfigProps config = configWithRange(null, null);
        assertEquals(McpProtocolVersion.V_2025_03_26, config.minimumProtocolVersion());
        assertEquals(McpProtocolVersion.V_2025_11_25, config.maximumProtocolVersion());
    }

    /**
     * Every supported version can be configured by its version string, and "latest" selects the newest one.
     * This covers versions which are added later without a change to the FAT.
     */
    @Test
    public void testParseProtocolVersionOnBeta() {
        System.setProperty(BETA_PROPERTY, "true");
        for (McpProtocolVersion version : McpProtocolVersion.values()) {
            assertEquals(version, McpServerConfigProps.parseProtocolVersion(version.getVersion()));
        }
        assertEquals(McpProtocolVersion.latest(), McpServerConfigProps.parseProtocolVersion("latest"));
    }

    /**
     * On a non-beta runtime "latest" selects the newest version which is available, not the beta version.
     */
    @Test
    public void testParseLatestOnNonBeta() {
        System.setProperty(BETA_PROPERTY, "false");
        assertEquals(McpProtocolVersion.V_2025_11_25, McpServerConfigProps.parseProtocolVersion("latest"));
    }

    /**
     * A configured beta version is not a valid value on a non-beta runtime.
     */
    @Test(expected = NoSuchElementException.class)
    public void testParseBetaVersionRejectedOnNonBeta() {
        System.setProperty(BETA_PROPERTY, "false");
        McpServerConfigProps.parseProtocolVersion("2026-07-28");
    }

    /**
     * A value which is neither {@code latest} nor a known protocol version is not valid.
     */
    @Test(expected = NoSuchElementException.class)
    public void testParseUnknownVersionRejected() {
        System.setProperty(BETA_PROPERTY, "true");
        McpServerConfigProps.parseProtocolVersion("1999-01-01");
    }
}
