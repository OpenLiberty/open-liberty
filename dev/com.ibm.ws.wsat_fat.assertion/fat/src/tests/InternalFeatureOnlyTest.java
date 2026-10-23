/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.WsAtomicTransaction;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests the private, internal WS-AT feature in isolation (NEW_TESTS.md sections 1 and 2).
 *
 * <p>{@code WSATInternalOnly} enables {@code wsatInternalTest-1.0}, a test-only wrapper around
 * {@code io.openliberty.wsAtomicTransaction1.2.internal.ee-6.0}, and deliberately does <b>not</b> enable
 * the public {@code wsAtomicTransaction-1.2} feature. {@code propagation} belongs to the public feature
 * only, so it must have no effect here and outbound propagation must be permanently off, while inbound
 * participant coordination must still work.
 *
 * <p>{@code WSATConditional} (public feature, default {@code always}) acts as the remote coordinator
 * for the inbound test.
 */
@RunWith(FATRunner.class)
public class InternalFeatureOnlyTest extends PropagationTestBase {

    private static final String TEST_FEATURE = "wsatInternalTest-1.0";

    @Server("WSATInternalOnly")
    public static LibertyServer internal;

    @Server("WSATConditional")
    public static LibertyServer coordinator;

    private static String internalUrl;
    private static String coordinatorUrl;

    @BeforeClass
    public static void beforeTests() throws Exception {
        internal.setHttpDefaultPort(DBTestBase.server1Port);

        internalUrl = "http://" + internal.getHostname() + ":" + internal.getHttpDefaultPort();
        coordinatorUrl = "http://" + coordinator.getHostname() + ":" + coordinator.getHttpDefaultPort();

        DBTestBase.initWSATTest(internal);
        DBTestBase.initWSATTest(coordinator);

        // System feature: as a usr extension feature the private internal feature was not
        // resolvable (CWWKF0001E "feature definition could not be found").
        internal.installSystemFeature(TEST_FEATURE);

        ShrinkHelper.defaultDropinApp(internal, "assertion", "web.assertion.*");
        ShrinkHelper.defaultDropinApp(coordinator, "assertion", "web.assertion.*");

        FATUtils.startServers(internal, coordinator);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            FATUtils.stopServers(new String[] { "CWWKW.*", "SRVE0777E" }, internal, coordinator);
        } finally {
            internal.uninstallSystemFeature(TEST_FEATURE);
            DBTestBase.cleanupWSATTest(internal);
            DBTestBase.cleanupWSATTest(coordinator);
        }
    }

    @Before
    public void setUp() throws Exception {
        internal.saveServerConfiguration();
        coordinator.saveServerConfiguration();
    }

    @After
    public void restore() throws Exception {
        internal.restoreServerConfigurationAndWaitForApps();
        coordinator.restoreServerConfigurationAndWaitForApps();
    }

    // ---------------------------------------------------------------------------------------------
    // 1.x Configuration and schema rejection
    // ---------------------------------------------------------------------------------------------

    /**
     * 1.1 {@code propagation} is declared in the {@code com.ibm.ws.wsat.common} metatype, which is
     * loaded by both the internal and public features, so the config service accepts it without a
     * {@code CWWKG0032W} warning even when only the internal feature is present. The attribute is
     * beta, so it is hidden from the schema, and the public feature alone supplies its default.
     *
     * <p>The attribute is <em>inert</em> at runtime because {@code WSATPolicyAwareInterceptor} only
     * adds the outbound coordination context interceptor when
     * {@code WSATFeatureService.isPublicFeaturePresent()} returns {@code true}. With the internal
     * feature only, that guard returns {@code false}, so outbound propagation is permanently off
     * regardless of whatever value is written to {@code <wsAtomicTransaction propagation="..."/>}.
     *
     * <p>Asserts: no {@code CWWKG0032W} (attribute is known); endpoint reports {@code tx=NONE}
     * (not enlisted) because the public feature is not present; its row persists on both commit
     * and rollback because the endpoint committed locally.
     */
    @Test
    public void test1_1_PropagationAcceptedButInert() throws Exception {
        internal.setMarkToEndOfLog();
        updateInternal("propagation", "check_wsdl");

        assertNull("propagation is a known attribute; CWWKG0032W must not be logged",
                   internal.waitForStringInLogUsingMark("CWWKG0032W", 2000));

        // Despite the config, outbound propagation must still be off — the internal feature's
        // runtime guard (isPublicFeaturePresent) blocks the outbound interceptor entirely.
        initTable(internalUrl);
        assertOutcome(internalUrl, internalUrl, NONE, false, false);
    }

    /** 1.2 Base plumbing attributes are still accepted, cleanly. */
    @Test
    public void test1_2_BaseConfigAccepted() throws Exception {
        internal.setMarkToEndOfLog();
        final ServerConfiguration config = internal.getServerConfiguration();
        final WsAtomicTransaction wsat = config.getWsAtomicTransaction();
        wsat.setSslEnabled(false);
        wsat.setExtraAttribute("asyncResponseTimeout", "45s");
        wsat.setExtraAttribute("virtualHostRef", "default_host");
        internal.updateServerConfiguration(config);
        internal.waitForConfigUpdateInLogUsingMark(null);

        assertNull("Valid base configuration must not produce CWWKG0032W",
                   internal.waitForStringInLogUsingMark("CWWKG0032W", 2000));
    }

    // ---------------------------------------------------------------------------------------------
    // 2.x Outbound propagation is permanently off with only the internal feature
    // ---------------------------------------------------------------------------------------------

    /** 2.1 No assertion: plain SOAP. Endpoint is not enlisted, so its row persists on commit AND rollback. */
    @Test
    public void test2_1_NoAssertion_Commit() throws Exception {
        initTable(internalUrl);
        assertOutcome(internalUrl, internalUrl, NONE, false, true);
    }

    @Test
    public void test2_1_NoAssertion_Rollback() throws Exception {
        initTable(internalUrl);
        assertOutcome(internalUrl, internalUrl, NONE, false, false);
    }

    /** 2.2 Optional assertion: still plain SOAP, still not enlisted. */
    @Test
    public void test2_2_OptionalAssertion_Commit() throws Exception {
        initTable(internalUrl);
        assertOutcome(internalUrl, internalUrl, OPTIONAL, false, true);
    }

    @Test
    public void test2_2_OptionalAssertion_Rollback() throws Exception {
        initTable(internalUrl);
        assertOutcome(internalUrl, internalUrl, OPTIONAL, false, false);
    }

    /** 2.3 Required assertion: cannot be satisfied, so the call fails immediately. */
    @Test
    @AllowedFFDC({ "com.ibm.ws.wsat.service.WSATException", "javax.transaction.RollbackException",
                   "javax.xml.ws.WebServiceException", "jakarta.xml.ws.WebServiceException",
                   "org.apache.cxf.interceptor.Fault", "java.lang.Exception" })
    public void test2_3_RequiredAssertion_Fails() throws Exception {
        initTable(internalUrl);
        final String key = newKey();
        final String r = call(internalUrl, internalUrl, REQUIRED, COMMIT, key);
        assertException("Required WS-AT assertion cannot be satisfied without public outbound propagation", r);
        assertEquals("The call must fail before reaching the endpoint, so nothing may be persisted", 0, count(internalUrl, key));
    }

    // ---------------------------------------------------------------------------------------------
    // 2.4 Inbound participant coordination still works
    // ---------------------------------------------------------------------------------------------

    /** Remote coordinator commits: the participant's row persists. */
    @Test
    public void test2_4_InboundParticipant_Commit() throws Exception {
        initTable(internalUrl);
        initTable(coordinatorUrl);
        assertInbound(true);
    }

    /** Remote coordinator rolls back: the participant's row must NOT persist. */
    @Test
    public void test2_4_InboundParticipant_Rollback() throws Exception {
        initTable(internalUrl);
        initTable(coordinatorUrl);
        assertInbound(false);
    }

    // ---------------------------------------------------------------------------------------------

    private void assertInbound(boolean commit) {
        final String key = newKey();
        final String r = call(coordinatorUrl, internalUrl, REQUIRED, commit ? COMMIT : ROLLBACK, key);
        assertTrue("Inbound coordination should succeed under the internal feature: " + r, r.startsWith("OK:"));
        assertTrue("Participant should have joined the remote transaction: " + r, r.endsWith("tx=ACTIVE"));
        assertEquals("Participant row should follow the remote coordinator's " + (commit ? "commit" : "rollback"),
                     commit ? 1 : 0, count(internalUrl, key));
    }

    private void updateInternal(String attribute, String value) throws Exception {
        final ServerConfiguration config = internal.getServerConfiguration();
        config.getWsAtomicTransaction().setExtraAttribute(attribute, value);
        internal.updateServerConfiguration(config);
        internal.waitForConfigUpdateInLogUsingMark(null);
    }
}
