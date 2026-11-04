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
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.assertDeleteRejected;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.assertEchoAccepted;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.assertEchoAcceptedInSession;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.assertEchoRejected;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.assertEchoRejectedInSession;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.assertInitializeRejected;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.delete;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.initialize;
import static io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.initializeAndGetProtocolVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.Mcp;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.mcp.internal.fat.protocol.ProtocolVersionRequests.InitializeResponse;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;

/**
 * Tests the beta {@code minimumProtocolVersion} and {@code maximumProtocolVersion} attributes of the
 * {@code <mcp>} configuration element on a server which is running with the beta edition enabled.
 * <p>
 * The server is started with {@code -Dcom.ibm.ws.beta.edition=true} (see the server's {@code jvm.options}).
 * The behaviour when the runtime is not beta is tested by {@link ProtocolVersionConfigNonBetaTest}.
 * <p>
 * Most applications are stateless so requests can be sent with any {@code MCP-Protocol-Version} header.
 * {@link #STATEFUL_APP} is stateful and tests version negotiation and the range within a session.
 * <p>
 * The configuration error messages do not name the application, so each application with an invalid value uses a
 * value which no other application uses, and the log assertions match on that value.
 * <p>
 * The errors for the initial configuration are logged before the applications start, so they are searched for in the
 * whole log with {@link #assertLogged}. That does not depend on the log mark, which {@link #updateDynamicRange} moves,
 * so the tests do not depend on the order they run in.
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
    private static final String LATEST_MINIMUM_OLDER_MAXIMUM_APP = "latestMinimumOlderMaximumApp";
    private static final String INVALID_MAXIMUM_APP = "invalidMaximumApp";
    private static final String INVALID_BOTH_APP = "invalidBothApp";
    private static final String UPPER_CASE_LATEST_APP = "upperCaseLatestApp";
    private static final String WHITESPACE_APP = "whitespaceApp";
    private static final String EMPTY_VALUES_APP = "emptyValuesApp";
    private static final String STATEFUL_APP = "statefulRangeApp";
    private static final String DYNAMIC_APP = "dynamicRangeApp";

    private static final List<String> APPS = List.of(DEFAULT_APP, SINGLE_VERSION_APP, MINIMUM_ONLY_APP, MAXIMUM_ONLY_APP,
                                                     LATEST_APP, INVALID_VERSION_APP, INVALID_RANGE_APP,
                                                     LATEST_MINIMUM_OLDER_MAXIMUM_APP, INVALID_MAXIMUM_APP, INVALID_BOTH_APP,
                                                     UPPER_CASE_LATEST_APP, WHITESPACE_APP, EMPTY_VALUES_APP,
                                                     STATEFUL_APP, DYNAMIC_APP);

    private static final String ALL_VERSIONS = "2026-07-28, 2025-11-25, 2025-06-18, 2025-03-26";

    /** The values listed by CWMCM0045E, which are "latest" followed by every supported version */
    private static final String VALID_VALUES = "latest, " + ALL_VERSIONS;

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

    // ---- Default, single version, minimum only, maximum only and latest ----

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
    public void testInitializeWithHeaderInRangeIsAccepted() throws Exception {
        // A header is not needed on initialize, but it is accepted when it is within the range
        assertEquals(V_2025_06_18, initialize(server, SINGLE_VERSION_APP, V_2025_06_18, V_2025_06_18).protocolVersion());
    }

    @Test
    public void testInitializeWithHeaderOutOfRangeIsRejected() throws Exception {
        // A header which names a version outside the range is rejected even on initialize, because the header
        // is checked before the request is processed. Only a missing header is exempt.
        assertInitializeRejected(server, SINGLE_VERSION_APP, V_2025_11_25, V_2025_06_18, V_2025_06_18);
        assertInitializeRejected(server, SINGLE_VERSION_APP, V_2025_03_26, V_2025_06_18, V_2025_06_18);
        assertInitializeRejected(server, SINGLE_VERSION_APP, "2022-02-02", V_2025_06_18, V_2025_06_18);
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
    public void testMaximumOnlyDefaultsMinimumTo20250326() throws Exception {
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

    // ---- Invalid values ----

    @Test
    public void testInvalidVersionIsReportedAndDefaultsUsed() throws Exception {
        // The message names the attribute and the value, and lists the valid values
        assertLogged("Expected CWMCM0045E for the invalid minimum protocol version",
                     "CWMCM0045E.*2022-02-02.*minimumProtocolVersion.*Valid values: " + VALID_VALUES);
        // The invalid value is ignored so every version is accepted
        assertAllVersionsAccepted(INVALID_VERSION_APP);
    }

    @Test
    public void testMinimumNewerThanMaximumIsReportedAndDefaultsUsed() throws Exception {
        // The message names the minimum first and the maximum second
        assertLogged("Expected CWMCM0046E for the invalid range", "CWMCM0046E.*2025-11-25.*2025-03-26");
        assertAllVersionsAccepted(INVALID_RANGE_APP);
    }

    @Test
    public void testLatestMinimumNewerThanOlderMaximumIsReportedAndDefaultsUsed() throws Exception {
        // "latest" is resolved before the range is compared, so the message names the version it resolved to
        assertLogged("Expected CWMCM0046E for the invalid range", "CWMCM0046E.*2026-07-28.*2025-06-18");
        assertAllVersionsAccepted(LATEST_MINIMUM_OLDER_MAXIMUM_APP);
    }

    @Test
    public void testInvalidMaximumIsReportedAndValidMinimumIsKept() throws Exception {
        assertLogged("Expected CWMCM0045E for the invalid maximum protocol version",
                     "CWMCM0045E.*2099-01-01.*maximumProtocolVersion.*Valid values: " + VALID_VALUES);
        // The message starts "The <value> value is not valid for the <attribute> attribute"
        assertTrue("The valid minimum protocol version should not be reported",
                   server.findStringsInLogs("CWMCM0045E: The 2025-06-18 value is not valid").isEmpty());

        // Only the invalid maximum is replaced by its default, which is latest in a beta runtime
        String supported = "2026-07-28, 2025-11-25, 2025-06-18";
        assertEchoAccepted(server, INVALID_MAXIMUM_APP, V_2025_06_18);
        assertEchoAccepted(server, INVALID_MAXIMUM_APP, V_2025_11_25);
        assertEchoAccepted(server, INVALID_MAXIMUM_APP, V_2026_07_28);
        assertEchoRejected(server, INVALID_MAXIMUM_APP, V_2025_03_26, supported);
        assertEchoRejected(server, INVALID_MAXIMUM_APP, null, supported);
    }

    @Test
    public void testBothInvalidAreReportedAndDefaultsUsed() throws Exception {
        assertLogged("Expected CWMCM0045E for the invalid minimum protocol version",
                     "CWMCM0045E.*bogus-minimum.*minimumProtocolVersion");
        assertLogged("Expected CWMCM0045E for the invalid maximum protocol version",
                     "CWMCM0045E.*bogus-maximum.*maximumProtocolVersion");
        assertAllVersionsAccepted(INVALID_BOTH_APP);
    }

    @Test
    public void testLatestIsCaseSensitive() throws Exception {
        assertLogged("Expected CWMCM0045E for LATEST, which is not the same as latest",
                     "CWMCM0045E.*LATEST.*minimumProtocolVersion");
        assertAllVersionsAccepted(UPPER_CASE_LATEST_APP);
    }

    @Test
    public void testSurroundingWhitespaceIsIgnored() throws Exception {
        assertEchoAccepted(server, WHITESPACE_APP, V_2025_06_18);
        assertEchoRejected(server, WHITESPACE_APP, V_2025_03_26, V_2025_06_18);
        assertEchoRejected(server, WHITESPACE_APP, V_2025_11_25, V_2025_06_18);
    }

    @Test
    public void testEmptyValuesAreTreatedAsNotSet() throws Exception {
        // An empty value is passed to the component as an empty string. It means "not set", so it is not
        // reported. The message for an empty value would have no value between "The" and "value", which is why
        // there are two spaces.
        assertTrue("An empty minimum protocol version should not be reported",
                   server.findStringsInLogs("CWMCM0045E: The  value is not valid for the minimumProtocolVersion attribute").isEmpty());
        assertTrue("An empty maximum protocol version should not be reported",
                   server.findStringsInLogs("CWMCM0045E: The  value is not valid for the maximumProtocolVersion attribute").isEmpty());
        // The defaults are used so every version is accepted
        assertAllVersionsAccepted(EMPTY_VALUES_APP);
    }

    // ---- Stateful applications ----

    @Test
    public void testStatefulInitializeNegotiatesVersionAndCreatesSession() throws Exception {
        InitializeResponse response = initialize(server, STATEFUL_APP, null, V_2025_11_25);
        assertEquals(V_2025_11_25, response.protocolVersion());
        assertNotNull("Expected a session ID from a stateful application", response.sessionId());

        InitializeResponse older = initialize(server, STATEFUL_APP, null, V_2025_06_18);
        assertEquals(V_2025_06_18, older.protocolVersion());
        assertNotNull(older.sessionId());
    }

    @Test
    public void testStatefulInitializeRespondsWithMaximumWhenClientVersionNotInRange() throws Exception {
        // The session is still created. The client decides whether it can use the version it was offered.
        for (String clientVersion : new String[] { V_2025_03_26, V_2026_07_28, "2022-02-02" }) {
            InitializeResponse response = initialize(server, STATEFUL_APP, null, clientVersion);
            assertEquals("Wrong version selected for client version " + clientVersion, V_2025_11_25, response.protocolVersion());
            assertNotNull(response.sessionId());
        }
    }

    @Test
    public void testStatefulRangeAppliesToRequestsInSession() throws Exception {
        String sessionId = initialize(server, STATEFUL_APP, null, V_2025_11_25).sessionId();
        assertNotNull(sessionId);

        assertEchoAcceptedInSession(server, STATEFUL_APP, V_2025_06_18, sessionId);
        assertEchoAcceptedInSession(server, STATEFUL_APP, V_2025_11_25, sessionId);

        String supported = "2025-11-25, 2025-06-18";
        assertEchoRejectedInSession(server, STATEFUL_APP, V_2025_03_26, sessionId, supported);
        assertEchoRejectedInSession(server, STATEFUL_APP, V_2026_07_28, sessionId, supported);
        assertEchoRejectedInSession(server, STATEFUL_APP, "2022-02-02", sessionId, supported);
        // A missing header is interpreted as 2025-03-26, which is below the minimum
        assertEchoRejectedInSession(server, STATEFUL_APP, null, sessionId, supported);
    }

    @Test
    public void testStatefulInitializeWithHeaderOutOfRangeIsRejected() throws Exception {
        assertInitializeRejected(server, STATEFUL_APP, V_2025_03_26, V_2025_11_25, "2025-11-25, 2025-06-18");
        assertInitializeRejected(server, STATEFUL_APP, V_2026_07_28, V_2025_11_25, "2025-11-25, 2025-06-18");
    }

    // ---- Session DELETE requests ----

    @Test
    public void testStatefulDeleteWithHeaderOutOfRangeIsRejectedAndSessionKept() throws Exception {
        String sessionId = initialize(server, STATEFUL_APP, null, V_2025_11_25).sessionId();
        assertNotNull(sessionId);

        String supported = "2025-11-25, 2025-06-18";
        assertDeleteRejected(server, STATEFUL_APP, V_2025_03_26, sessionId, supported);
        assertDeleteRejected(server, STATEFUL_APP, V_2026_07_28, sessionId, supported);
        assertDeleteRejected(server, STATEFUL_APP, "2022-02-02", sessionId, supported);

        // A rejected DELETE must not delete the session
        assertEchoAcceptedInSession(server, STATEFUL_APP, V_2025_11_25, sessionId);

        // A header within the range is accepted and deletes the session
        delete(server, STATEFUL_APP, V_2025_06_18, sessionId, 200);
        delete(server, STATEFUL_APP, V_2025_06_18, sessionId, 404);
    }

    @Test
    public void testStatefulDeleteWithoutHeaderIsAccepted() throws Exception {
        // A missing header is allowed on DELETE, even though 2025-03-26 is below the minimum
        String sessionId = initialize(server, STATEFUL_APP, null, V_2025_11_25).sessionId();
        assertNotNull(sessionId);
        delete(server, STATEFUL_APP, null, sessionId, 200);
    }

    @Test
    public void testStatelessDeleteWithHeaderOutOfRangeIsRejected() throws Exception {
        // The header is checked before the request is answered with 404 for a stateless application
        assertDeleteRejected(server, SINGLE_VERSION_APP, V_2025_11_25, null, V_2025_06_18);
        assertDeleteRejected(server, SINGLE_VERSION_APP, "2022-02-02", null, V_2025_06_18);
        delete(server, SINGLE_VERSION_APP, V_2025_06_18, null, 404);
    }

    // ---- Configuration updates ----

    @Test
    public void testRangeCanBeUpdatedWhileServerIsRunning() throws Exception {
        boolean updated = false;
        try {
            // Initial range: only 2025-06-18
            assertEchoAccepted(server, DYNAMIC_APP, V_2025_06_18);
            assertEchoRejected(server, DYNAMIC_APP, V_2025_11_25, V_2025_06_18);
            assertEchoRejected(server, DYNAMIC_APP, V_2025_03_26, V_2025_06_18);

            // Widen the range
            updated = true;
            updateDynamicRange("2025-03-26", "2025-11-25");
            assertEchoAccepted(server, DYNAMIC_APP, V_2025_03_26);
            assertEchoAccepted(server, DYNAMIC_APP, V_2025_06_18);
            assertEchoAccepted(server, DYNAMIC_APP, V_2025_11_25);
            assertEchoRejected(server, DYNAMIC_APP, V_2026_07_28, "2025-11-25, 2025-06-18, 2025-03-26");

            // Move to latest
            updateDynamicRange("latest", "latest");
            assertEchoAccepted(server, DYNAMIC_APP, V_2026_07_28);
            assertEchoRejected(server, DYNAMIC_APP, V_2025_11_25, V_2026_07_28);
            assertEquals(V_2026_07_28, initializeAndGetProtocolVersion(server, DYNAMIC_APP, V_2025_03_26));

            // An invalid range is reported, and the defaults are used instead of keeping the previous range.
            // 2025-11-25 and 2025-06-18 appear together in no other CWMCM0046E message.
            updateDynamicRange("2025-11-25", "2025-06-18");
            assertNotNull("Expected CWMCM0046E for the updated range",
                          server.waitForStringInLog("CWMCM0046E.*2025-11-25.*2025-06-18"));
            assertAllVersionsAccepted(DYNAMIC_APP);
        } finally {
            // Restore the initial range so the server configuration is the same if the class is run again.
            // Skipped if nothing was changed, because an unchanged configuration produces no update to wait for.
            if (updated) {
                updateDynamicRange("2025-06-18", "2025-06-18");
            }
        }
        assertEchoAccepted(server, DYNAMIC_APP, V_2025_06_18);
        assertEchoRejected(server, DYNAMIC_APP, V_2025_11_25, V_2025_06_18);
    }

    private static void updateDynamicRange(String minimum, String maximum) throws Exception {
        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        Application app = config.getApplications().getBy("location", DYNAMIC_APP + ".war");
        assertNotNull("Expected to find the application in the server configuration", app);

        Mcp mcp = app.getMcps().get(0);
        mcp.setMinimumProtocolVersion(minimum);
        mcp.setMaximumProtocolVersion(maximum);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(DYNAMIC_APP));
    }

    /**
     * Asserts that a message matching the regular expression is anywhere in messages.log. Unlike
     * {@code waitForStringInLog} this searches from the start of the log and ignores the log mark.
     */
    private static void assertLogged(String failureMessage, String regexp) throws Exception {
        assertFalse(failureMessage, server.findStringsInLogs(regexp).isEmpty());
    }

    private static void assertAllVersionsAccepted(String appName) throws Exception {
        assertEchoAccepted(server, appName, V_2025_03_26);
        assertEchoAccepted(server, appName, V_2025_06_18);
        assertEchoAccepted(server, appName, V_2025_11_25);
        assertEchoAccepted(server, appName, V_2026_07_28);
        // A missing header is interpreted as 2025-03-26
        assertEchoAccepted(server, appName, null);
    }
}