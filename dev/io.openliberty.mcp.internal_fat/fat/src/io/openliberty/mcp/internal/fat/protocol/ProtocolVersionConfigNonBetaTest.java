/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.protocol;

import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.V_2025_03_26;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.V_2025_06_18;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.V_2025_11_25;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.V_2026_07_28;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.assertEchoAccepted;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.assertEchoRejected;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.initializeAndGetProtocolVersion;
import static io.openliberty.mcp.internal.fat.suite.McpMainServerSuite.serverLifecycle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Mcp;

import componenttest.custom.junit.runner.FATRunner;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;

/**
 * Tests that the beta {@code minimumProtocolVersion} and {@code maximumProtocolVersion} attributes are ignored
 * when the runtime is not beta. The server behaves as if the minimum is 2025-03-26 and the maximum is 2025-11-25.
 * <p>
 * The beta behaviour is tested by {@link ProtocolVersionConfigBetaTest}.
 */
@RunWith(FATRunner.class)
public class ProtocolVersionConfigNonBetaTest {

    // Server is managed by McpMainServerSuite — do NOT add @Server or copy the field here.

    /** Configured with a range which would only allow 2025-11-25 if it was not ignored */
    private static final String RESTRICTED_APP = "protocolVersionNonBetaRestricted";
    /** Configured with values which are not valid versions, which would be reported if they were not ignored */
    private static final String INVALID_APP = "protocolVersionNonBetaInvalid";

    private static final String ALL_VERSIONS = "2025-11-25, 2025-06-18, 2025-03-26";

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().setMarkToEndOfLog();

        WebArchive restrictedWar = ShrinkWrap.create(WebArchive.class, RESTRICTED_APP + ".war").addPackage(BasicTools.class.getPackage());
        serverLifecycle.deployWithConfiguration(restrictedWar, app -> {
            Mcp mcp = new Mcp();
            mcp.setStateless("true");
            mcp.setMinimumProtocolVersion("2025-11-25");
            mcp.setMaximumProtocolVersion("2025-11-25");
            app.getMcps().add(mcp);
        });

        WebArchive invalidWar = ShrinkWrap.create(WebArchive.class, INVALID_APP + ".war").addPackage(BasicTools.class.getPackage());
        serverLifecycle.deployWithConfiguration(invalidWar, app -> {
            Mcp mcp = new Mcp();
            mcp.setStateless("true");
            mcp.setMinimumProtocolVersion("not-a-version");
            mcp.setMaximumProtocolVersion("2022-02-02");
            app.getMcps().add(mcp);
        });
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployWithConfiguration(INVALID_APP);
        serverLifecycle.undeployWithConfiguration(RESTRICTED_APP);
    }

    @Test
    public void testConfiguredRangeIsIgnored() throws Exception {
        assertEchoAccepted(serverLifecycle.getServer(), RESTRICTED_APP, V_2025_03_26);
        assertEchoAccepted(serverLifecycle.getServer(), RESTRICTED_APP, V_2025_06_18);
        assertEchoAccepted(serverLifecycle.getServer(), RESTRICTED_APP, V_2025_11_25);
        assertEchoAccepted(serverLifecycle.getServer(), RESTRICTED_APP, null);
    }

    @Test
    public void testUnknownVersionStillRejectedWithGaVersions() throws Exception {
        assertEchoRejected(serverLifecycle.getServer(), RESTRICTED_APP, "2022-02-02", ALL_VERSIONS);
    }

    @Test
    public void testVersionNewerThanGaMaximumIsRejected() throws Exception {
        // 2026-07-28 is a valid protocol version, but it is above the maximum used when the runtime is not beta
        assertEchoRejected(serverLifecycle.getServer(), RESTRICTED_APP, V_2026_07_28, ALL_VERSIONS);
    }

    @Test
    public void testInitializeUsesGaMaximum() throws Exception {
        assertEquals(V_2025_11_25, initializeAndGetProtocolVersion(serverLifecycle.getServer(), RESTRICTED_APP, V_2025_11_25));
        assertEquals(V_2025_06_18, initializeAndGetProtocolVersion(serverLifecycle.getServer(), RESTRICTED_APP, V_2025_06_18));
        assertEquals(V_2025_11_25, initializeAndGetProtocolVersion(serverLifecycle.getServer(), RESTRICTED_APP, "2022-02-02"));
        assertEquals(V_2025_11_25, initializeAndGetProtocolVersion(serverLifecycle.getServer(), RESTRICTED_APP, V_2026_07_28));
    }

    @Test
    public void testInvalidValuesAreIgnoredWithoutError() throws Exception {
        assertEchoAccepted(serverLifecycle.getServer(), INVALID_APP, V_2025_03_26);
        assertEchoAccepted(serverLifecycle.getServer(), INVALID_APP, V_2025_11_25);
        assertTrue("Invalid protocol version values should be ignored without an error when the runtime is not beta",
                   serverLifecycle.getServer().findStringsInLogsUsingMark("CWMCM0045E|CWMCM0046E",
                                                                           serverLifecycle.getServer().getDefaultLogFile()).isEmpty());
    }
}
