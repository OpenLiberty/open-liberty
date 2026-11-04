/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.request.probe.ejb.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Verifies that the four EJB request-probe transform descriptors register (and unregister)
 * in trace when each EJB/MDB feature is added to a server that already has eventLogging-1.0
 * (or requestTiming-1.0), and that they are absent when neither probe feature is present.
 * It also verifies that the descriptors unregister when the probe feature is removed while
 * an EJB feature is still present, and re-register when the probe feature is added back.
 *
 * <p>Both the EE7/EE8 feature names (ejb-3.2, mdb-3.2, …) and the EE9+ names
 * (enterpriseBeans-4.0, mdb-4.0, …) are tested in a single run, since the underlying
 * probe registration logic is identical across EE levels.
 *
 * <p>The expected descriptors (className|methodName|methodDesc|eventType):
 * <ul>
 * <li>com/ibm/ws/ejbcontainer/requesttiming/async|invoke|()V|websphere.ejb.async</li>
 * <li>com/ibm/ws/ejbcontainer/requesttiming/mdb|invoke|()V|websphere.ejb.mdb</li>
 * <li>com/ibm/ws/ejbcontainer/requesttiming/remote|invoke|()V|websphere.ejb.remote</li>
 * <li>com/ibm/ws/ejbcontainer/requesttiming/timer|invoke|()V|websphere.ejb.timer</li>
 * </ul>
 */
@RunWith(FATRunner.class)
public class RequestProbeEJBRegTDTest {

    private static final Class<?> c = RequestProbeEJBRegTDTest.class;

    @Server("EJBProbeServer")
    public static LibertyServer server;

    @Rule
    public TestName testName = new TestName();

    private static final String TRACE_LOG = "logs/trace.log";

    private static final long DESCRIPTOR_TIMEOUT_MS = 30000;

    // The four expected descriptor detail strings logged by RequestProbeBCIManagerImpl at finer.
    // Each is the argument passed to Tr.entry(tc, "setRequestProbeMetaDataProvider", tdDetails)
    // i.e. the line *after* "> setRequestProbeMetaDataProvider" in trace.log.
    // @formatter:off
    static final List<String> EXPECTED_TDS = Collections.unmodifiableList(Arrays.asList(
        "com/ibm/ws/ejbcontainer/requesttiming/async|invoke|()V|websphere.ejb.async",
        "com/ibm/ws/ejbcontainer/requesttiming/mdb|invoke|()V|websphere.ejb.mdb",
        "com/ibm/ws/ejbcontainer/requesttiming/remote|invoke|()V|websphere.ejb.remote",
        "com/ibm/ws/ejbcontainer/requesttiming/timer|invoke|()V|websphere.ejb.timer"));
    // @formatter:on

    /**
     * All EJB/MDB feature names to exercise — EE7/EE8 names followed by EE9+ names.
     * Both sets ship in the same Liberty image, so both can be tested in a single run
     * without EE-level repeat actions.
     */
    // @formatter:off
    static final List<String> ALL_EJB_FEATURES = Collections.unmodifiableList(Arrays.asList(
        // EE7 / EE8 names
        "ejb-3.2",
        "ejbHome-3.2",
        "ejbLite-3.2",
        "ejbPersistentTimer-3.2",
        "ejbRemote-3.2",
        "mdb-3.2",
        // EE9 / EE10 / EE11 names
        "enterpriseBeans-4.0",
        "enterpriseBeansHome-4.0",
        "enterpriseBeansLite-4.0",
        "enterpriseBeansPersistentTimer-4.0",
        "enterpriseBeansRemote-4.0",
        "mdb-4.0"));
    // @formatter:on

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeClass
    public static void beforeClass() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Before each test, ensure the server is in its baseline state:
     * only eventLogging-1.0 in the feature list, no EJB feature.
     */
    @Before
    public void resetToBaseline() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        // Remove any EJB features that a previous test left behind, case-insensitively.
        List<String> toRemove = new ArrayList<>();
        for (String f : features) {
            String lower = f.toLowerCase();
            if (lower.startsWith("ejb") || lower.startsWith("enterprisebeans") || lower.startsWith("mdb")
                || lower.equals("requesttiming-1.0")) {
                toRemove.add(f);
            }
        }
        // Restore eventLogging-1.0 if a test removed it (or replaced it with requestTiming).
        boolean hasEventLogging = features.stream().anyMatch(f -> f.equalsIgnoreCase("eventLogging-1.0"));
        boolean hasRequestTiming = features.stream().anyMatch(f -> f.equalsIgnoreCase("requestTiming-1.0"));
        boolean needsReset = !toRemove.isEmpty() || !hasEventLogging || hasRequestTiming;
        if (needsReset) {
            features.removeAll(toRemove);
            if (!hasEventLogging) {
                features.add("eventLogging-1.0");
            }
            server.setMarkToEndOfLog();
            server.setTraceMarkToEndOfDefaultTrace();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null, true);
        } else {
            server.setMarkToEndOfLog();
            server.setTraceMarkToEndOfDefaultTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Polls the trace (from the current mark, without advancing it) until the "> {method} Entry"
     * line has been seen at least once per expected descriptor and every expected descriptor
     * detail string has been seen. The bundle activation, and the descriptors' registration,
     * are asynchronous and may be concurrent, so neither ordering nor adjacency of the lines is
     * assumed: all matching lines since the mark are collected on each poll.
     *
     * @return the expected descriptors that were not seen before the timeout (empty on success)
     */
    private Set<String> awaitDescriptors(String method, String context) throws Exception {
        String entryRegex = "> " + method + "\\s+Entry";
        String descriptorRegex = EXPECTED_TDS.stream().map(Pattern::quote).collect(Collectors.joining("|"));
        Set<String> missing = new HashSet<>(EXPECTED_TDS);
        int entries = 0;
        long deadline = System.currentTimeMillis() + DESCRIPTOR_TIMEOUT_MS;
        do {
            entries = server.findStringsInLogsAndTraceUsingMark(entryRegex).size();
            List<String> detailLines = server.findStringsInLogsAndTraceUsingMark(descriptorRegex);
            missing.clear();
            for (String expected : EXPECTED_TDS) {
                if (detailLines.stream().noneMatch(line -> line.contains(expected))) {
                    missing.add(expected);
                }
            }
            if (missing.isEmpty() && entries >= EXPECTED_TDS.size()) {
                return missing;
            }
            Thread.sleep(500);
        } while (System.currentTimeMillis() < deadline);

        Log.info(c, testName.getMethodName(), context + ": " + method + " entries=" + entries + ", missing=" + missing);
        assertTrue(context + ": expected at least " + EXPECTED_TDS.size() + " '" + method + "' entries but saw " + entries,
                   entries >= EXPECTED_TDS.size());
        return missing;
    }

    /**
     * Waits for all four EJB descriptors to be registered.
     */
    private void assertAllFourDescriptorsRegistered(String context) throws Exception {
        Set<String> missing = awaitDescriptors("setRequestProbeMetaDataProvider", context);
        assertTrue(context + ": timed out waiting for TDs: " + missing, missing.isEmpty());
        Log.info(c, testName.getMethodName(), context + ": all four EJB TDs registered");
    }

    /**
     * Waits for all four EJB descriptors to be unregistered.
     *
     * unsetRequestProbeMetaDataProvider logs the same detail string as the set variant, so the
     * descriptor strings alone cannot be told apart from a registration. This is unambiguous here
     * because removeFeature() moves the trace mark to the end of the trace immediately before the
     * configuration update, so only the unset path can log these strings after the mark. Requiring
     * the unset Entry lines as well confirms the unset path actually ran.
     */
    private void assertAllFourDescriptorsUnregistered(String context) throws Exception {
        Set<String> missing = awaitDescriptors("unsetRequestProbeMetaDataProvider", context);
        assertTrue(context + ": timed out waiting for unregistration of TDs: " + missing, missing.isEmpty());
        Log.info(c, testName.getMethodName(), context + ": all four EJB TDs unregistered");
    }

    /**
     * Adds a single feature and waits for the config update.
     *
     * Adding an EJB feature for the first time can trigger a JCA/transaction subsystem
     * cold-start that takes longer than the 12-second local CWWKF0008I timeout. To handle
     * this we first wait for CWWKF0012I ("features installed") with a generous 60-second
     * timeout; by the time that message appears CWWKF0008I is imminent and well within the
     * subsequent 12-second window.
     */
    private void addFeature(String feature) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getFeatureManager().getFeatures().add(feature);
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(config);
        // Wait up to 60s for feature installation to complete before checking config update.
        server.waitForStringInLogUsingMark("CWWKF0012I", 60000);
        server.waitForConfigUpdateInLogUsingMark(null, true);
    }

    /**
     * Removes a single feature (case-insensitively) and waits for the config update.
     * The feature set may contain features in a different case (e.g. after a repeat run),
     * so we locate and remove by case-insensitive comparison.
     */
    private void removeFeature(String feature) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        features.removeIf(f -> f.equalsIgnoreCase(feature));
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null, true);
    }

    /**
     * Replaces one feature (case-insensitively) with another and waits for the config update.
     */
    private void replaceFeature(String remove, String add) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        features.removeIf(f -> f.equalsIgnoreCase(remove));
        features.add(add);
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null, true);
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * For each EJB/MDB feature: adding it alongside eventLogging-1.0 must cause all four
     * EJB descriptors to register; removing it must cause them to unregister.
     * Both EE7/EE8 names (ejb-3.2, …) and EE9+ names (enterpriseBeans-4.0, …) are covered
     * in the same run without needing EE-level repeat actions.
     */
    @Test
    public void testAllFourTDsRegisterWithEachEJBFeature() throws Exception {
        for (String ejbFeature : ALL_EJB_FEATURES) {
            Log.info(c, testName.getMethodName(), "--- Testing feature: " + ejbFeature);

            addFeature(ejbFeature);
            assertAllFourDescriptorsRegistered("Added " + ejbFeature);

            // Clean up: remove the EJB feature and confirm unregistration
            server.setTraceMarkToEndOfDefaultTrace();
            removeFeature(ejbFeature);
            assertAllFourDescriptorsUnregistered("Removed " + ejbFeature);
        }
    }

    /**
     * When an EJB feature is present but no probe feature (no eventLogging, no requestTiming),
     * no EJB descriptors should register. Exercised with both ejbLite-3.2 (EE7/EE8) and
     * enterpriseBeansLite-4.0 (EE9+) to cover both generations in a single run.
     */
    @Test
    public void testNoProbeFeatureNoRegistration() throws Exception {
        for (String ejbFeature : Arrays.asList("ejbLite-3.2", "enterpriseBeansLite-4.0")) {
            Log.info(c, testName.getMethodName(), "--- Testing no-probe with feature: " + ejbFeature);

            server.setTraceMarkToEndOfDefaultTrace();
            removeFeature("eventLogging-1.0");

            addFeature(ejbFeature);
            // addFeature() waits for CWWKF0012I (features installed) and CWWKF0008I
            // (feature update completed), so all bundles are fully active — no sleep needed.

            // findStringsInLogsUsingMark only sees lines after the mark set above (after
            // eventLogging-1.0 was removed), so this confirms no TDs registered without a probe.
            List<String> registrations = server.findStringsInLogsUsingMark("> setRequestProbeMetaDataProvider", TRACE_LOG);
            assertTrue("EJB TDs should NOT register when no probe feature is present [" + ejbFeature + "], but found: "
                       + registrations, registrations.isEmpty());

            Log.info(c, testName.getMethodName(), "Confirmed: no TD registration without probe feature [" + ejbFeature + "]");

            // Restore baseline for the next iteration: remove the EJB feature, re-add eventLogging.
            removeFeature(ejbFeature);
            addFeature("eventLogging-1.0");
        }
    }

    /**
     * With an EJB feature present, the four EJB descriptors must register. Dynamically
     * removing eventLogging-1.0 must then cause them to unregister, and re-adding
     * eventLogging-1.0 must cause them to re-register. The EJB feature is removed at the end
     * of each iteration and the unregistration is verified.
     * Exercised with both ejbLite-3.2 and enterpriseBeansLite-4.0.
     */
    @Test
    public void testDynamicRemoveAndReAddEventLogging() throws Exception {
        for (String ejbFeature : Arrays.asList("ejbLite-3.2", "enterpriseBeansLite-4.0")) {
            Log.info(c, testName.getMethodName(), "--- Testing dynamic remove/re-add with feature: " + ejbFeature);

            // Add the EJB feature → four TDs register
            addFeature(ejbFeature);
            assertAllFourDescriptorsRegistered("Initial add of " + ejbFeature);

            // Remove eventLogging-1.0 → TDs must unregister
            server.setTraceMarkToEndOfDefaultTrace();
            removeFeature("eventLogging-1.0");
            assertAllFourDescriptorsUnregistered("After removing eventLogging-1.0 [" + ejbFeature + "]");

            // Re-add eventLogging-1.0 → TDs must re-register
            server.setTraceMarkToEndOfDefaultTrace();
            addFeature("eventLogging-1.0");
            assertAllFourDescriptorsRegistered("After re-adding eventLogging-1.0 [" + ejbFeature + "]");

            // Clean up EJB feature before next iteration; @Before will handle the rest.
            server.setTraceMarkToEndOfDefaultTrace();
            removeFeature(ejbFeature);
            assertAllFourDescriptorsUnregistered("Cleanup removal of " + ejbFeature);
        }
    }

    /**
     * Verifies that the four EJB descriptors register under requestTiming-1.0 (not just
     * eventLogging-1.0): eventLogging-1.0 is swapped for requestTiming-1.0, the EJB feature
     * is added (descriptors must register) and then removed (descriptors must unregister),
     * and eventLogging-1.0 is restored.
     * Exercised with both ejbLite-3.2 and enterpriseBeansLite-4.0.
     */
    @Test
    public void testDescriptorsRegisterWithRequestTiming() throws Exception {
        for (String ejbFeature : Arrays.asList("ejbLite-3.2", "enterpriseBeansLite-4.0")) {
            Log.info(c, testName.getMethodName(), "--- Testing requestTiming with feature: " + ejbFeature);

            // Step 1: swap eventLogging-1.0 → requestTiming-1.0 (no EJB feature yet).
            replaceFeature("eventLogging-1.0", "requestTiming-1.0");

            // Step 2: add the EJB feature — TDs must register under requestTiming-1.0.
            addFeature(ejbFeature);
            assertAllFourDescriptorsRegistered("Added " + ejbFeature + " with requestTiming-1.0");

            // Step 3: remove the EJB feature — TDs must unregister.
            server.setTraceMarkToEndOfDefaultTrace();
            removeFeature(ejbFeature);
            assertAllFourDescriptorsUnregistered("Removed " + ejbFeature + " with requestTiming-1.0");

            // Restore eventLogging-1.0 for the next iteration / @Before cleanup.
            replaceFeature("requestTiming-1.0", "eventLogging-1.0");
        }
    }
}
