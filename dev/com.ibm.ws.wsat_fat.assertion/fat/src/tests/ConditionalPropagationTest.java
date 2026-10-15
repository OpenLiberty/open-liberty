/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.util.DBTestBase;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for the {@code propagation} attribute of {@code <wsAtomicTransaction/>} on the public
 * {@code wsAtomicTransaction-1.2} feature (NEW_TESTS.md sections 3.x, 4.x and 5.x).
 *
 * <p>Every successful scenario is run to <b>both</b> commit and rollback completion, because that
 * is what proves whether the endpoint was actually enlisted in the client's transaction. See
 * {@link PropagationTestBase#assertOutcome}.
 */
@RunWith(FATRunner.class)
public class ConditionalPropagationTest extends PropagationTestBase {

    @Server("WSATConditional")
    public static LibertyServer server;

    private static String baseUrl;

    @BeforeClass
    public static void beforeTests() throws Exception {
        baseUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();

        DBTestBase.initWSATTest(server);
        ShrinkHelper.defaultDropinApp(server, "assertion", "web.assertion.*");
        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(new String[] { "CWWKG0032W" }, server);
        DBTestBase.cleanupWSATTest(server);
    }

    @Before
    public void setUp() throws Exception {
        server.saveServerConfiguration();
    }

    @After
    public void restore() throws Exception {
        server.restoreServerConfigurationAndWaitForApps();
    }

    // ---------------------------------------------------------------------------------------------
    // 3.x Default / zero-migration mode: propagation="always" or unconfigured
    // ---------------------------------------------------------------------------------------------

    /** 3.1 No propagation attribute: context attached unconditionally, even to a service with no assertion. */
    @Test
    public void test3_1_DefaultUnconfigured_Commit() throws Exception {
        setPropagation(null);
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, NONE, true, true);
    }

    @Test
    public void test3_1_DefaultUnconfigured_Rollback() throws Exception {
        setPropagation(null);
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, NONE, true, false);
    }

    /** 3.2 Explicit propagation="always". */
    @Test
    public void test3_2_Always_Commit() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, NONE, true, true);
    }

    @Test
    public void test3_2_Always_Rollback() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, NONE, true, false);
    }

    /**
     * 3.3 {@code propagation} is a known attribute on the public feature, so an unsupported value must be
     * reported (CWWKG0032W) rather than silently ignored. Contrast with
     * {@code InternalFeatureOnlyTest.test1_1_UnknownPropagationSilentlyIgnored}, where the attribute is unknown.
     * The server falls back to the default, {@code always}.
     */
    @Test
    public void test3_3_InvalidValueReported() throws Exception {
        server.setMarkToEndOfLog();
        setPropagation("not_a_valid_mode");

        assertNotNull("Expected CWWKG0032W for an unsupported propagation value on the public feature",
                      server.waitForStringInLogUsingMark("CWWKG0032W", 10000));

        // Falls back to the default: context still attached unconditionally.
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, NONE, true, false);
    }

    // ---------------------------------------------------------------------------------------------
    // 4.x Conditional mode: propagation="check_wsdl"
    // ---------------------------------------------------------------------------------------------

    /** 4.1 Required assertion: propagated, endpoint enlisted. */
    @Test
    public void test4_1_CheckWsdl_Required_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, REQUIRED, true, true);
    }

    @Test
    public void test4_1_CheckWsdl_Required_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, REQUIRED, true, false);
    }

    /** 4.2 Optional assertion: propagated, endpoint enlisted. */
    @Test
    public void test4_2_CheckWsdl_Optional_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, OPTIONAL, true, true);
    }

    @Test
    public void test4_2_CheckWsdl_Optional_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, OPTIONAL, true, false);
    }

    /** 4.3 No assertion: plain SOAP, no WS-AT context, endpoint not enlisted (its row survives rollback). */
    @Test
    public void test4_3_CheckWsdl_NoAssertion_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, NONE, false, true);
    }

    @Test
    public void test4_3_CheckWsdl_NoAssertion_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, NONE, false, false);
    }

    /** 4.4 No active transaction + required assertion: fails. */
    @Test
    @AllowedFFDC({ "com.ibm.ws.wsat.service.WSATException", "javax.transaction.RollbackException",
                   "javax.xml.ws.WebServiceException", "jakarta.xml.ws.WebServiceException",
                   "org.apache.cxf.interceptor.Fault", "java.lang.Exception" })
    public void test4_4_CheckWsdl_NoTran_Required() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertException("Required assertion without a transaction",
                        call(baseUrl, baseUrl, REQUIRED, NO_TRAN, newKey()));
    }

    /** 4.5 No active transaction + optional / no assertion: plain SOAP succeeds. */
    @Test
    public void test4_5_CheckWsdl_NoTran_Optional() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertNoTranOk(OPTIONAL);
    }

    @Test
    public void test4_5_CheckWsdl_NoTran_NoAssertion() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertNoTranOk(NONE);
    }

    // ---------------------------------------------------------------------------------------------
    // 5.x Dynamic configuration updates and lifecycle
    // ---------------------------------------------------------------------------------------------

    /** 5.1 always -> check_wsdl takes effect without restart. */
    @Test
    public void test5_1_DynamicSwitch_AlwaysToCheckWsdl() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, NONE, true, false); // propagated while "always"

        setPropagation("check_wsdl");
        assertOutcome(baseUrl, baseUrl, NONE, false, false); // suppressed immediately afterwards
    }

    /** 5.2 check_wsdl -> always takes effect without restart. */
    @Test
    public void test5_2_DynamicSwitch_CheckWsdlToAlways() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, NONE, false, false); // suppressed while "check_wsdl"

        setPropagation("always");
        assertOutcome(baseUrl, baseUrl, NONE, true, false); // propagated immediately afterwards
    }

    /** 5.3 The propagation mode survives a server pause / resume cycle. */
    @Test
    public void test5_3_PauseResume_PreservesMode() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, NONE, false, false);

        server.setMarkToEndOfLog();
        server.executeServerScript("pause", null);
        assertNotNull("Server did not pause (CWWKE0938I)", server.waitForStringInLogUsingMark("CWWKE0938I"));

        server.setMarkToEndOfLog();
        server.executeServerScript("resume", null);
        assertNotNull("Server did not resume (CWWKE0939I)", server.waitForStringInLogUsingMark("CWWKE0939I"));

        assertOutcome(baseUrl, baseUrl, NONE, false, false); // still check_wsdl
        assertOutcome(baseUrl, baseUrl, OPTIONAL, true, false); // and still propagating when asserted
    }

    // ---------------------------------------------------------------------------------------------

    private void assertNoTranOk(String service) {
        final String key = newKey();
        final String r = call(baseUrl, baseUrl, service, NO_TRAN, key);
        org.junit.Assert.assertTrue("Expected OK from plain SOAP call but got: " + r, r.startsWith("OK:"));
        org.junit.Assert.assertTrue("No transaction exists, endpoint must not see one: " + r, r.endsWith("tx=NONE"));
        org.junit.Assert.assertEquals("Row from non-transactional call should persist", 1, count(baseUrl, key));
    }

    /**
     * Set (or, when {@code null}, remove) the {@code propagation} attribute on
     * {@code <wsAtomicTransaction/>} and wait for the dynamic config update to complete.
     */
    private void setPropagation(String propagation) throws Exception {
        Log.info(getClass(), "setPropagation", "wsAtomicTransaction propagation=" + propagation);
        server.setMarkToEndOfLog();
        final ServerConfiguration config = server.getServerConfiguration();
        config.getWsAtomicTransaction().setPropagation(propagation);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }
}
