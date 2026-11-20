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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    /**
     * 3.1 No propagation attribute: context attached unconditionally, even to a service with no assertion.
     * The {@code always} default comes from the default instance supplied by the public feature, not from
     * the metatype.
     */
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

    /** 3.4 Explicit {@code always} with an asserted endpoint: propagated and enlisted, as with {@code check_wsdl}. */
    @Test
    public void test3_4_Always_Required_Commit() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, REQUIRED, true, true);
    }

    @Test
    public void test3_4_Always_Required_Rollback() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, REQUIRED, true, false);
    }

    @Test
    public void test3_4_Always_Optional_Commit() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, OPTIONAL, true, true);
    }

    @Test
    public void test3_4_Always_Optional_Rollback() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, OPTIONAL, true, false);
    }

    /**
     * 3.5 {@code always} with no active transaction: a Required assertion still fails fast ("Detected WS-AT
     * policy, however there is no active transaction"), while Optional and no assertion send plain SOAP.
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.wsat.service.WSATException", "javax.transaction.RollbackException",
                   "javax.xml.ws.WebServiceException", "jakarta.xml.ws.WebServiceException",
                   "org.apache.cxf.interceptor.Fault", "java.lang.Exception" })
    public void test3_5_Always_NoTran_Required_FailsFast() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        final String key = newKey();
        assertException("Required assertion without a transaction", call(baseUrl, baseUrl, REQUIRED, NO_TRAN, key));
        assertEquals("The failed call must not have reached the endpoint", 0, count(baseUrl, key));
    }

    @Test
    public void test3_5_Always_NoTran_Optional() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertNoTranOk(OPTIONAL);
    }

    @Test
    public void test3_5_Always_NoTran_NoAssertion() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertNoTranOk(NONE);
    }

    /**
     * 3.3 {@code propagation} is a known attribute on the public feature, so an unsupported value must be
     * reported (CWWKG0032W) rather than silently ignored. Contrast with
     * {@code InternalFeatureOnlyTest.test1_1_PropagationAcceptedButInert}, where the attribute is accepted
     * but inert. The server falls back to the default, {@code always}.
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
    // 6.x Indeterminate policy: a Dispatch client created without a WSDL
    //
    // The other indeterminate cases (no CXF Endpoint on the exchange, no EffectivePolicy) are not
    // covered here because they are very difficult to reproduce in a FAT. A JAX-WS proxy or Dispatch
    // always has an Endpoint on its exchange, and a client whose policy contains the WS-AT assertion
    // never reaches WSCoorUtil.isOptional (CoorContextOutInterceptor tests WSCoorUtil.assertAT first).
    // Forcing them would need a test feature exposing CXF internals and custom interceptors that
    // strip the Endpoint and the AssertionInfoMap, which would test those hacks as much as the product.
    // They are covered by com.ibm.ws.wsat.utils.WSCoorUtilTest in com.ibm.ws.wsat.webservice.
    // ---------------------------------------------------------------------------------------------

    /**
     * 6.1 In {@code check_wsdl} mode an indeterminate policy is treated as "no assertion": plain SOAP, no
     * WS-AT context, endpoint not enlisted (its row survives rollback). The target endpoint does declare an
     * optional assertion, so it would have been enlisted had the client been able to see it (see 6.2 and 4.2).
     */
    @Test
    public void test6_1_CheckWsdl_DispatchWithoutWsdl_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, DISPATCH, false, true);
    }

    @Test
    public void test6_1_CheckWsdl_DispatchWithoutWsdl_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, DISPATCH, false, false);
    }

    /** 6.2 Control: with {@code always} the same {@code Dispatch} call is propagated and enlisted. */
    @Test
    public void test6_2_Always_DispatchWithoutWsdl_Commit() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, DISPATCH, true, true);
    }

    @Test
    public void test6_2_Always_DispatchWithoutWsdl_Rollback() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, DISPATCH, true, false);
    }

    /**
     * 6.3 {@code check_wsdl} with a {@code Dispatch} client (no WSDL) to a <b>Required</b> endpoint. The client cannot
     * see the assertion, so by design it sends plain SOAP and does not fail fast. The endpoint's own policy then
     * rejects the request, which carries no coordination context. The endpoint is not invoked, so no row exists.
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.wsat.service.WSATException", "javax.xml.ws.WebServiceException", "jakarta.xml.ws.WebServiceException",
                   "org.apache.cxf.interceptor.Fault", "java.lang.Exception" })
    public void test6_3_CheckWsdl_DispatchWithoutWsdl_Required_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        final String key = newKey();
        assertException("Required endpoint reached by a client that cannot see its policy",
                        call(baseUrl, baseUrl, DISPATCH_REQUIRED, COMMIT, key));
        assertEquals("The rejected request must not have been executed", 0, count(baseUrl, key));
    }

    @Test
    @AllowedFFDC({ "com.ibm.ws.wsat.service.WSATException", "javax.xml.ws.WebServiceException", "jakarta.xml.ws.WebServiceException",
                   "org.apache.cxf.interceptor.Fault", "java.lang.Exception" })
    public void test6_3_CheckWsdl_DispatchWithoutWsdl_Required_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        final String key = newKey();
        assertException("Required endpoint reached by a client that cannot see its policy",
                        call(baseUrl, baseUrl, DISPATCH_REQUIRED, ROLLBACK, key));
        assertEquals("The rejected request must not have been executed", 0, count(baseUrl, key));
    }

    /** 6.4 Control: with {@code always} the same {@code Dispatch} call is propagated, accepted and enlisted. */
    @Test
    public void test6_4_Always_DispatchWithoutWsdl_Required_Commit() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, DISPATCH_REQUIRED, true, true);
    }

    @Test
    public void test6_4_Always_DispatchWithoutWsdl_Required_Rollback() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, DISPATCH_REQUIRED, true, false);
    }

    // ---------------------------------------------------------------------------------------------
    // 7.x Several endpoints in one global transaction
    //
    // Calls with an assertion (REQ, OPT) are enlisted. Calls without one (NONE) are plain SOAP and commit
    // locally, so their rows survive a client rollback. See PropagationTestBase.assertMixedOutcome.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void test7_1_CheckWsdl_Mixed_RequiredThenNone_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertMixedOutcome(baseUrl, baseUrl, "REQ_NONE", false, true);
    }

    @Test
    public void test7_1_CheckWsdl_Mixed_RequiredThenNone_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertMixedOutcome(baseUrl, baseUrl, "REQ_NONE", false, false);
    }

    @Test
    public void test7_2_CheckWsdl_Mixed_NoneThenRequired_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertMixedOutcome(baseUrl, baseUrl, "NONE_REQ", false, true);
    }

    @Test
    public void test7_2_CheckWsdl_Mixed_NoneThenRequired_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertMixedOutcome(baseUrl, baseUrl, "NONE_REQ", false, false);
    }

    @Test
    public void test7_3_CheckWsdl_Mixed_RequiredNoneRequired_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertMixedOutcome(baseUrl, baseUrl, "REQ_NONE_REQ", false, true);
    }

    @Test
    public void test7_3_CheckWsdl_Mixed_RequiredNoneRequired_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertMixedOutcome(baseUrl, baseUrl, "REQ_NONE_REQ", false, false);
    }

    @Test
    public void test7_4_CheckWsdl_Mixed_OptionalNoneOptional_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertMixedOutcome(baseUrl, baseUrl, "OPT_NONE_OPT", false, true);
    }

    @Test
    public void test7_4_CheckWsdl_Mixed_OptionalNoneOptional_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertMixedOutcome(baseUrl, baseUrl, "OPT_NONE_OPT", false, false);
    }

    /** 7.5 Control: with {@code always} every endpoint is enlisted, so a rollback removes every row. */
    @Test
    public void test7_5_Always_Mixed_Commit() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertMixedOutcome(baseUrl, baseUrl, "REQ_NONE_REQ", true, true);
    }

    @Test
    public void test7_5_Always_Mixed_Rollback() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertMixedOutcome(baseUrl, baseUrl, "REQ_NONE_REQ", true, false);
    }

    // ---------------------------------------------------------------------------------------------
    // 8.x Fault isolation
    // ---------------------------------------------------------------------------------------------

    /**
     * 8.1 With {@code check_wsdl}, an application fault from an endpoint with no assertion is plain SOAP and
     * must not mark the client's global transaction rollback-only. The transaction commits and the
     * Required endpoint's row, enlisted in it, persists.
     */
    @Test
    public void test8_1_CheckWsdl_FaultFromNonAssertedEndpoint_TransactionStillCommits() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        final String key = newKey();
        final String r = callFaultIsolation(baseUrl, baseUrl, key);

        assertTrue("Expected OK but got: " + r, r.startsWith("OK:"));
        assertTrue("The fault from the non-asserted endpoint should have reached the client: " + r, !r.contains("caught=none"));
        assertTrue("Transaction must still be ACTIVE (not marked rollback-only) after the fault: " + r,
                   r.endsWith("status=0")); // javax.transaction.Status.STATUS_ACTIVE; STATUS_MARKED_ROLLBACK is 1
        assertEquals("Required endpoint's row should have been committed", 1, count(baseUrl, key + "-req"));
    }

    /**
     * 8.2 Control: {@code always} behaves the same way. An application fault, as opposed to a WS-AT protocol
     * failure, does not mark the client's transaction rollback-only even when the faulting endpoint was sent
     * a coordination context. (The FTS gap review expected {@code always} to poison the transaction; running it
     * shows it does not.) So 8.1 shows that a fault from a non-asserted endpoint is harmless under
     * {@code check_wsdl}, not that it is harmful under {@code always}.
     */
    @Test
    public void test8_2_Always_FaultFromNonAssertedEndpoint_TransactionStillCommits() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        final String key = newKey();
        final String r = callFaultIsolation(baseUrl, baseUrl, key);

        assertTrue("Expected OK but got: " + r, r.startsWith("OK:"));
        assertTrue("The application fault should have reached the client: " + r, !r.contains("caught=none"));
        assertTrue("Transaction must still be ACTIVE after an application fault: " + r, r.endsWith("status=0"));
        assertEquals("Required endpoint's row should have been committed", 1, count(baseUrl, key + "-req"));
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
