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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.V_2025_03_26;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.V_2025_06_18;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.V_2025_11_25;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.V_2026_07_28;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.assertEchoAccepted;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.assertEchoRejected;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.initializeAndGetProtocolVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;

/**
 * Tests the beta {@code minimumProtocolVersion} and {@code maximumProtocolVersion} attributes of the
 * {@code <mcp>} configuration element on a server which is running with the beta edition enabled.
 * <p>
 * The server is started with {@code -Dcom.ibm.ws.beta.edition=true} (see the server's {@code jvm.options}).
 * The behaviour when the runtime is not beta is tested by {@link ProtocolVersionConfigNonBetaTest}.
 * <p>
 * The applications are stateless so requests can be sent with any {@code MCP-Protocol-Version} header.
 */
@RunWith(FATRunner.class)
public class ProtocolVersionConfigBetaTest {

    private static final String DEFAULT_APP = "defaultVersionsApp";
    private static final String SINGLE_VERSION_APP = "singleVersionApp";
    private static final String MINIMUM_ONLY_APP = "minimumOnlyApp";
    private static final String MAXIMUM_ONLY_APP = "maximumOnlyApp";
    private static final String LATEST_APP = "latestApp";
    private static final String INVALID_VERSION_APP = "invalidVersionApp";
    private static final String INVALID_RANGE_APP = "invalidRangeApp";

    private static final List<String> APPS = List.of(DEFAULT_APP, SINGLE_VERSION_APP, MINIMUM_ONLY_APP, MAXIMUM_ONLY_APP,
                                                     LATEST_APP, INVALID_VERSION_APP, INVALID_RANGE_APP);

    private static final String ALL_VERSIONS = "2026-07-28, 2025-11-25, 2025-06-18, 2025-03-26";

    @Server("mcp-server-protocol-version-beta")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        for (String app : APPS) {
            WebArchive war = ShrinkWrap.create(WebArchive.class, app + ".war").addPackage(BasicTools.class.getPackage());
            ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);
        }

        server.startServer();

        for (String app : APPS) {
            assertNotNull("Application " + app + " did not start",
                          server.waitForStringInLog("MCP server endpoint: .*/" + app + "/mcp$"));
        }
    }

    @AfterClass
    public static void teardown() throws Exception {
        // Errors are expected for the apps with invalid configuration
        server.stopServer("CWMCM0045E", "CWMCM0046E");
    }

    @Test
    public void testDefaultAcceptsAllVersions() throws Exception {
        assertEchoAccepted(server, DEFAULT_APP, V_2025_03_26);
        assertEchoAccepted(server, DEFAULT_APP, V_2025_06_18);
        assertEchoAccepted(server, DEFAULT_APP, V_2025_11_25);
        assertEchoAccepted(server, DEFAULT_APP, V_2026_07_28);
        // A missing header is interpreted as 2025-03-26
        assertEchoAccepted(server, DEFAULT_APP, null);
    }

    @Test
    public void testDefaultRejectsUnknownVersion() throws Exception {
        assertEchoRejected(server, DEFAULT_APP, "2022-02-02", ALL_VERSIONS);
    }

    @Test
    public void testSingleVersionRange() throws Exception {
        assertEchoAccepted(server, SINGLE_VERSION_APP, V_2025_06_18);
        assertEchoRejected(server, SINGLE_VERSION_APP, V_2025_03_26, V_2025_06_18);
        assertEchoRejected(server, SINGLE_VERSION_APP, V_2025_11_25, V_2025_06_18);
        assertEchoRejected(server, SINGLE_VERSION_APP, V_2026_07_28, V_2025_06_18);
        assertEchoRejected(server, SINGLE_VERSION_APP, "2022-02-02", V_2025_06_18);
    }

    @Test
    public void testMissingHeaderRejectedWhenDefaultVersionNotInRange() throws Exception {
        // A missing header is interpreted as 2025-03-26, which is below the minimum
        assertEchoRejected(server, SINGLE_VERSION_APP, null, V_2025_06_18);
    }

    @Test
    public void testInitializeDoesNotNeedHeaderWhenDefaultVersionNotInRange() throws Exception {
        // initialize negotiates the version so it does not need a header
        assertEquals(V_2025_06_18, initializeAndGetProtocolVersion(server, SINGLE_VERSION_APP, V_2025_06_18));
    }

    @Test
    public void testInitializeRespondsWithMaximumWhenClientVersionNotInRange() throws Exception {
        assertEquals(V_2025_06_18, initializeAndGetProtocolVersion(server, SINGLE_VERSION_APP, V_2025_11_25));
        assertEquals(V_2025_06_18, initializeAndGetProtocolVersion(server, SINGLE_VERSION_APP, V_2026_07_28));
        assertEquals(V_2025_06_18, initializeAndGetProtocolVersion(server, SINGLE_VERSION_APP, V_2025_03_26));
        assertEquals(V_2025_06_18, initializeAndGetProtocolVersion(server, SINGLE_VERSION_APP, "2022-02-02"));
    }

    @Test
    public void testMinimumOnlyDefaultsMaximumToLatest() throws Exception {
        String supported = "2026-07-28, 2025-11-25, 2025-06-18";
        assertEchoAccepted(server, MINIMUM_ONLY_APP, V_2025_06_18);
        assertEchoAccepted(server, MINIMUM_ONLY_APP, V_2025_11_25);
        assertEchoAccepted(server, MINIMUM_ONLY_APP, V_2026_07_28);
        assertEchoRejected(server, MINIMUM_ONLY_APP, V_2025_03_26, supported);
        assertEchoRejected(server, MINIMUM_ONLY_APP, null, supported);
        assertEquals(V_2025_11_25, initializeAndGetProtocolVersion(server, MINIMUM_ONLY_APP, V_2025_11_25));
    }

    @Test
    public void testMaximumOnlyDefaultsMinimumToOldest() throws Exception {
        String supported = "2025-06-18, 2025-03-26";
        assertEchoAccepted(server, MAXIMUM_ONLY_APP, V_2025_03_26);
        assertEchoAccepted(server, MAXIMUM_ONLY_APP, V_2025_06_18);
        assertEchoAccepted(server, MAXIMUM_ONLY_APP, null);
        assertEchoRejected(server, MAXIMUM_ONLY_APP, V_2025_11_25, supported);
        assertEchoRejected(server, MAXIMUM_ONLY_APP, V_2026_07_28, supported);
        assertEquals(V_2025_06_18, initializeAndGetProtocolVersion(server, MAXIMUM_ONLY_APP, V_2025_11_25));
    }

    @Test
    public void testLatestSelectsNewestVersion() throws Exception {
        // latest is the newest version the runtime supports
        assertEchoAccepted(server, LATEST_APP, V_2026_07_28);
        assertEchoRejected(server, LATEST_APP, V_2025_11_25, V_2026_07_28);
        assertEchoRejected(server, LATEST_APP, V_2025_06_18, V_2026_07_28);
        assertEchoRejected(server, LATEST_APP, V_2025_03_26, V_2026_07_28);
        assertEchoRejected(server, LATEST_APP, null, V_2026_07_28);
        assertEquals(V_2026_07_28, initializeAndGetProtocolVersion(server, LATEST_APP, V_2025_03_26));
    }

    @Test
    public void testInvalidVersionIsReportedAndDefaultsUsed() throws Exception {
        assertNotNull("Expected CWMCM0045E for the invalid protocol version",
                      server.waitForStringInLog("CWMCM0045E.*2022-02-02.*minimumProtocolVersion|CWMCM0045E.*minimumProtocolVersion.*2022-02-02"));
        // The invalid value is ignored so every version is accepted
        assertEchoAccepted(server, INVALID_VERSION_APP, V_2025_03_26);
        assertEchoAccepted(server, INVALID_VERSION_APP, V_2025_06_18);
        assertEchoAccepted(server, INVALID_VERSION_APP, V_2025_11_25);
        assertEchoAccepted(server, INVALID_VERSION_APP, V_2026_07_28);
    }

    @Test
    public void testMinimumNewerThanMaximumIsReportedAndDefaultsUsed() throws Exception {
        assertNotNull("Expected CWMCM0046E for the invalid range",
                      server.waitForStringInLog("CWMCM0046E"));
        assertEchoAccepted(server, INVALID_RANGE_APP, V_2025_03_26);
        assertEchoAccepted(server, INVALID_RANGE_APP, V_2025_06_18);
        assertEchoAccepted(server, INVALID_RANGE_APP, V_2025_11_25);
        assertEchoAccepted(server, INVALID_RANGE_APP, V_2026_07_28);
    }
}
