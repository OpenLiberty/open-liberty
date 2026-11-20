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
 * Conditional propagation from a client that is an EJB or a CDI bean, where the transaction a web service call is
 * made in is decided by the bean's transaction attribute rather than by the application (gap G5).
 *
 * <p>For each transaction attribute the test calls the Required, Optional and no-assertion endpoints, from a
 * caller with no transaction and from a caller whose transaction commits or rolls back, and (where the bean owns
 * the transaction) with the bean committing or rolling back. The attribute decides who owns the transaction
 * the call is made in:
 *
 * <table border="1">
 * <caption>Transaction the web service call is made in</caption>
 * <tr><td>REQUIRED</td><td>the caller's, or else the bean's</td></tr>
 * <tr><td>REQUIRES_NEW</td><td>the bean's</td></tr>
 * <tr><td>MANDATORY</td><td>the caller's; an exception if there is none</td></tr>
 * <tr><td>SUPPORTS</td><td>the caller's, or none</td></tr>
 * <tr><td>NOT_SUPPORTED</td><td>none</td></tr>
 * <tr><td>NEVER</td><td>none; an exception if the caller has one</td></tr>
 * <tr><td>BMT bean</td><td>the bean's, whatever the caller has</td></tr>
 * </table>
 *
 * <p>With {@code check_wsdl} a call is propagated only if there is a transaction and the endpoint has an assertion.
 * A Required assertion with no transaction fails fast. Rows persisted by the endpoint show whether it was enlisted.
 */
@RunWith(FATRunner.class)
@AllowedFFDC({ "com.ibm.ws.wsat.service.WSATException", "javax.transaction.RollbackException", "jakarta.transaction.RollbackException",
               "javax.xml.ws.WebServiceException", "jakarta.xml.ws.WebServiceException",
               "javax.xml.ws.soap.SOAPFaultException", "jakarta.xml.ws.soap.SOAPFaultException",
               "javax.ejb.EJBException", "jakarta.ejb.EJBException",
               "javax.ejb.EJBTransactionRequiredException", "jakarta.ejb.EJBTransactionRequiredException",
               "javax.ejb.EJBTransactionRolledbackException", "jakarta.ejb.EJBTransactionRolledbackException",
               "javax.transaction.TransactionalException", "jakarta.transaction.TransactionalException",
               "javax.transaction.TransactionRequiredException", "jakarta.transaction.TransactionRequiredException",
               "com.ibm.websphere.csi.CSIException", "com.ibm.websphere.csi.CSITransactionRequiredException",
               "com.ibm.ws.LocalTransaction.RolledbackException", "web.assertion.cmt.RollbackRequested",
               "org.apache.cxf.interceptor.Fault", "java.lang.Exception" })
public class CmtPropagationTest extends PropagationTestBase {

    @Server("WSATCmtClient")
    public static LibertyServer server;

    private static String baseUrl;

    private static final String[] SERVICES = { REQUIRED, OPTIONAL, NONE };
    private static final String[] CALLERS = { NO_TRAN, COMMIT, ROLLBACK };

    /** Who owns the transaction that the web service call is made in. */
    private enum Owner {
        CALLER, BEAN, NONE, EXCEPTION
    }

    @BeforeClass
    public static void beforeTests() throws Exception {
        baseUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();

        DBTestBase.initWSATTest(server);
        ShrinkHelper.defaultDropinApp(server, "assertion", "web.assertion.*");
        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            FATUtils.stopServers(new String[] { "CWWKG0032W", "WTRN0.*", "CNTR0.*", "CWWKW.*" }, server);
        } finally {
            DBTestBase.cleanupWSATTest(server);
        }
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
    // EJB, container-managed (check_wsdl)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void test10_1_Ejb_Required() throws Exception {
        check("check_wsdl", "EJB", "REQUIRED", Owner.CALLER, Owner.BEAN);
    }

    @Test
    public void test10_2_Ejb_RequiresNew() throws Exception {
        check("check_wsdl", "EJB", "REQUIRES_NEW", Owner.BEAN, Owner.BEAN);
    }

    @Test
    public void test10_3_Ejb_Mandatory() throws Exception {
        check("check_wsdl", "EJB", "MANDATORY", Owner.CALLER, Owner.EXCEPTION);
    }

    @Test
    public void test10_4_Ejb_Supports() throws Exception {
        check("check_wsdl", "EJB", "SUPPORTS", Owner.CALLER, Owner.NONE);
    }

    @Test
    public void test10_5_Ejb_NotSupported() throws Exception {
        check("check_wsdl", "EJB", "NOT_SUPPORTED", Owner.NONE, Owner.NONE);
    }

    @Test
    public void test10_6_Ejb_Never() throws Exception {
        check("check_wsdl", "EJB", "NEVER", Owner.EXCEPTION, Owner.NONE);
    }

    /** A bean-managed bean begins its own transaction; the caller's is suspended. */
    @Test
    public void test10_7_Bmt() throws Exception {
        check("check_wsdl", "BMT", "", Owner.BEAN, Owner.BEAN);
    }

    // ---------------------------------------------------------------------------------------------
    // CDI @Transactional (check_wsdl)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void test11_1_Cdi_Required() throws Exception {
        check("check_wsdl", "CDI", "REQUIRED", Owner.CALLER, Owner.BEAN);
    }

    @Test
    public void test11_2_Cdi_RequiresNew() throws Exception {
        check("check_wsdl", "CDI", "REQUIRES_NEW", Owner.BEAN, Owner.BEAN);
    }

    @Test
    public void test11_3_Cdi_Mandatory() throws Exception {
        check("check_wsdl", "CDI", "MANDATORY", Owner.CALLER, Owner.EXCEPTION);
    }

    @Test
    public void test11_4_Cdi_Supports() throws Exception {
        check("check_wsdl", "CDI", "SUPPORTS", Owner.CALLER, Owner.NONE);
    }

    @Test
    public void test11_5_Cdi_NotSupported() throws Exception {
        check("check_wsdl", "CDI", "NOT_SUPPORTED", Owner.NONE, Owner.NONE);
    }

    @Test
    public void test11_6_Cdi_Never() throws Exception {
        check("check_wsdl", "CDI", "NEVER", Owner.EXCEPTION, Owner.NONE);
    }

    // ---------------------------------------------------------------------------------------------
    // always: the attribute still decides the transaction, but every call made in one is propagated
    // ---------------------------------------------------------------------------------------------

    @Test
    public void test12_1_Always_Ejb_Required() throws Exception {
        check("always", "EJB", "REQUIRED", Owner.CALLER, Owner.BEAN);
    }

    @Test
    public void test12_2_Always_Ejb_NotSupported() throws Exception {
        check("always", "EJB", "NOT_SUPPORTED", Owner.NONE, Owner.NONE);
    }

    @Test
    public void test12_3_Always_Cdi_RequiresNew() throws Exception {
        check("always", "CDI", "REQUIRES_NEW", Owner.BEAN, Owner.BEAN);
    }

    @Test
    public void test12_4_Always_Bmt() throws Exception {
        check("always", "BMT", "", Owner.BEAN, Owner.BEAN);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * Run every combination of endpoint, caller transaction and (when the bean owns the transaction) bean outcome
     * for one bean method, and assert the outcome expected for it.
     *
     * @param withTran    who owns the transaction when the caller has one
     * @param withoutTran who owns the transaction when the caller has none
     */
    private void check(String propagation, String kind, String attr, Owner withTran, Owner withoutTran) throws Exception {
        setPropagation(propagation);
        initTable(baseUrl);
        final boolean always = "always".equals(propagation);

        for (String service : SERVICES) {
            for (String caller : CALLERS) {
                final boolean callerHasTran = !NO_TRAN.equals(caller);
                final Owner owner = callerHasTran ? withTran : withoutTran;
                // Only ask the bean to roll back when its own transaction is the one in play.
                final boolean[] beanRollbacks = owner == Owner.BEAN ? new boolean[] { false, true } : new boolean[] { false };
                for (boolean beanRollback : beanRollbacks) {
                    final String label = kind + " " + attr + " " + service + " caller=" + caller + " beanRollback=" + beanRollback + ": ";
                    final String key = newKey();
                    final String r = callCmt(baseUrl, baseUrl, kind, attr, service, caller, beanRollback, key);
                    Log.info(getClass(), "check", label + r);

                    final boolean serviceAsserted = !NONE.equals(service);
                    if (owner == Owner.EXCEPTION || (owner == Owner.NONE && REQUIRED.equals(service))) {
                        // No transaction is allowed, or none exists but the Required assertion needs one: fail fast.
                        assertException(label + "expected the call to fail", r);
                        assertEquals(label + "the failed call must not have reached the endpoint", 0, count(baseUrl, key));
                        continue;
                    }

                    assertTrue(label + "expected OK but got: " + r, r.startsWith("OK:"));

                    final boolean inTran = owner == Owner.BEAN || owner == Owner.CALLER;
                    final boolean propagated = inTran && (always || serviceAsserted);
                    assertTrue(label + "expected the endpoint to " + (propagated ? "" : "NOT ") + "see a global transaction but got: " + r,
                               r.endsWith(propagated ? "tx=ACTIVE" : "tx=NONE"));

                    final boolean committed = owner == Owner.BEAN ? !beanRollback : COMMIT.equals(caller);
                    final int expectedRows = (propagated && !committed) ? 0 : 1;
                    assertEquals(label + "unexpected number of persisted rows", expectedRows, count(baseUrl, key));
                }
            }
        }
    }

    /** Set the {@code propagation} attribute on {@code <wsAtomicTransaction/>} and wait for the update. */
    private void setPropagation(String propagation) throws Exception {
        Log.info(getClass(), "setPropagation", "wsAtomicTransaction propagation=" + propagation);
        server.setMarkToEndOfLog();
        final ServerConfiguration config = server.getServerConfiguration();
        config.getWsAtomicTransaction().setPropagation(propagation);
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }
}
