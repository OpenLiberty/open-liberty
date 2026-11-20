/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
 * {@code propagation="check_wsdl"} decides from the client's effective policy, so these tests check that the
 * decision does not depend on <i>where</i> the WS-AT assertion sits in the WSDL, on other assertions beside
 * it, or on whether the client's WSDL is the endpoint's current one.
 *
 * <ul>
 * <li>{@code PolicyAtOperation}: the policy is referenced from a {@code wsdl:operation} of the binding, not
 * from the binding itself.</li>
 * <li>{@code PolicyAtPort}: the policy is referenced from the {@code wsdl:port}.</li>
 * <li>{@code Coexisting}: the WS-AT assertion shares its policy alternative with a WS-Addressing assertion.</li>
 * <li>{@code StaleRequired} / {@code StaleOptional}: the endpoint carries an assertion, but the client uses a
 * packaged copy of the WSDL without one.</li>
 * </ul>
 */
@RunWith(FATRunner.class)
public class PolicyPlacementTest extends PropagationTestBase {

    private static final String AT_OPERATION = "PolicyAtOperation";
    private static final String AT_PORT = "PolicyAtPort";
    private static final String COEXISTING = "Coexisting";
    private static final String STALE_REQUIRED = "StaleRequired";
    private static final String STALE_OPTIONAL = "StaleOptional";

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
    // 1.x Assertion placement and coexistence: always recognised, so propagated and enlisted
    // ---------------------------------------------------------------------------------------------

    @Test
    public void test1_1_CheckWsdl_AssertionAtOperation_Commit() throws Exception {
        assertPropagated(AT_OPERATION, true);
    }

    @Test
    public void test1_1_CheckWsdl_AssertionAtOperation_Rollback() throws Exception {
        assertPropagated(AT_OPERATION, false);
    }

    @Test
    public void test1_2_CheckWsdl_AssertionAtPort_Commit() throws Exception {
        assertPropagated(AT_PORT, true);
    }

    @Test
    public void test1_2_CheckWsdl_AssertionAtPort_Rollback() throws Exception {
        assertPropagated(AT_PORT, false);
    }

    @Test
    public void test1_3_CheckWsdl_CoexistingPolicies_Commit() throws Exception {
        assertPropagated(COEXISTING, true);
    }

    @Test
    public void test1_3_CheckWsdl_CoexistingPolicies_Rollback() throws Exception {
        assertPropagated(COEXISTING, false);
    }

    /** 1.4 The assertions are Required wherever they sit, so no transaction means failing fast. */
    @Test
    @AllowedFFDC({ "com.ibm.ws.wsat.service.WSATException", "javax.transaction.RollbackException",
                   "javax.xml.ws.WebServiceException", "jakarta.xml.ws.WebServiceException",
                   "org.apache.cxf.interceptor.Fault", "java.lang.Exception" })
    public void test1_4_CheckWsdl_NoTran_FailsFastWhereverTheAssertionSits() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        for (String service : new String[] { AT_OPERATION, AT_PORT, COEXISTING }) {
            final String key = newKey();
            assertException(service + ": Required assertion without a transaction", call(baseUrl, baseUrl, service, NO_TRAN, key));
            assertEquals(service + ": the failed call must not have reached the endpoint", 0, count(baseUrl, key));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 2.x Stale packaged WSDL: the client's own copy of the WSDL is what decides
    // ---------------------------------------------------------------------------------------------

    /**
     * 2.1 The endpoint has an Optional assertion but the client's packaged WSDL has none, so {@code check_wsdl}
     * sends plain SOAP. The endpoint accepts it and is not enlisted: its row survives a rollback.
     */
    @Test
    public void test2_1_CheckWsdl_StaleWsdl_Optional_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, STALE_OPTIONAL, false, true);
    }

    @Test
    public void test2_1_CheckWsdl_StaleWsdl_Optional_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, STALE_OPTIONAL, false, false);
    }

    /**
     * 2.2 The endpoint has a Required assertion, the client's packaged WSDL has none, so plain SOAP is sent and
     * the endpoint rejects it. The endpoint is not invoked, so no row exists.
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.wsat.service.WSATException", "javax.xml.ws.WebServiceException", "jakarta.xml.ws.WebServiceException",
                   "org.apache.cxf.interceptor.Fault", "java.lang.Exception" })
    public void test2_2_CheckWsdl_StaleWsdl_Required_Rejected() throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        final String key = newKey();
        assertException("Required endpoint reached with a stale client WSDL", call(baseUrl, baseUrl, STALE_REQUIRED, COMMIT, key));
        assertEquals("The rejected request must not have been executed", 0, count(baseUrl, key));
    }

    /** 2.3 Control: {@code always} ignores the client's WSDL, so the same stale-WSDL calls are propagated and enlisted. */
    @Test
    public void test2_3_Always_StaleWsdl_Optional_Rollback() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, STALE_OPTIONAL, true, false);
    }

    @Test
    public void test2_3_Always_StaleWsdl_Required_Rollback() throws Exception {
        setPropagation("always");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, STALE_REQUIRED, true, false);
    }

    // ---------------------------------------------------------------------------------------------

    private void assertPropagated(String service, boolean commit) throws Exception {
        setPropagation("check_wsdl");
        initTable(baseUrl);
        assertOutcome(baseUrl, baseUrl, service, true, commit);
    }

    /** Set the {@code propagation} attribute on {@code <wsAtomicTransaction/>} and wait for the dynamic config update. */
    private void setPropagation(String propagation) throws Exception {
        Log.info(getClass(), "setPropagation", "wsAtomicTransaction propagation=" + propagation);
        server.setMarkToEndOfLog();
        final ServerConfiguration config = server.getServerConfiguration();
        config.getWsAtomicTransaction().setPropagation(propagation);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }
}
