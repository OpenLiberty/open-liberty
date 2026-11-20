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

import org.jboss.shrinkwrap.api.spec.WebArchive;
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
 * The scenario that motivated conditional propagation: a transactional client calling a service on a
 * server that has no WS-AT support at all (a third-party or legacy JAX-WS service).
 *
 * <p>{@code WSATPlainJaxws} runs {@code jaxws-2.2} only. It hosts the no-assertion endpoint, and nothing
 * else of the {@code assertion} application. A WS-AT coordination context is sent with
 * {@code mustUnderstand="1"}, so with {@code propagation="always"} this server refuses the request. With
 * {@code check_wsdl} the client sees that the target's WSDL carries no assertion and sends plain SOAP.
 *
 * <p>{@code WSATConditional} (public feature) is the client.
 */
@RunWith(FATRunner.class)
public class PlainJaxwsTargetTest extends PropagationTestBase {

    @Server("WSATConditional")
    public static LibertyServer client;

    @Server("WSATPlainJaxws")
    public static LibertyServer plain;

    private static String clientUrl;
    private static String plainUrl;

    @BeforeClass
    public static void beforeTests() throws Exception {
        plain.setHttpDefaultPort(DBTestBase.server2Port);

        clientUrl = "http://" + client.getHostname() + ":" + client.getHttpDefaultPort();
        plainUrl = "http://" + plain.getHostname() + ":" + plain.getHttpDefaultPort();

        DBTestBase.initWSATTest(client);
        DBTestBase.initWSATTest(plain);

        ShrinkHelper.defaultDropinApp(client, "assertion", "web.assertion.*");

        // The plain server has no WS-AT support, so it can only host the endpoint without an assertion.
        final WebArchive plainApp = ShrinkHelper.buildDefaultApp("assertion", "web.assertion.*");
        plainApp.delete("/WEB-INF/classes/web/assertion/server/HelloImplAssertionRequired.class");
        plainApp.delete("/WEB-INF/classes/web/assertion/server/HelloImplAssertionOptional.class");
        plainApp.delete("/WEB-INF/classes/web/assertion/server/HelloImplAssertionIngorable.class");
        plainApp.delete("/WEB-INF/classes/web/assertion/server/HelloImplPolicyAtOperation.class");
        plainApp.delete("/WEB-INF/classes/web/assertion/server/HelloImplPolicyAtPort.class");
        plainApp.delete("/WEB-INF/classes/web/assertion/server/HelloImplCoexistingPolicies.class");
        ShrinkHelper.exportDropinAppToServer(plain, plainApp);

        FATUtils.startServers(client, plain);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            FATUtils.stopServers(new String[] { "CWWKG0032W", "CWWKW.*", "SRVE0777E" }, client, plain);
        } finally {
            DBTestBase.cleanupWSATTest(client);
            DBTestBase.cleanupWSATTest(plain);
        }
    }

    @Before
    public void setUp() throws Exception {
        client.saveServerConfiguration();
    }

    @After
    public void restore() throws Exception {
        client.restoreServerConfigurationAndWaitForApps();
    }

    /**
     * 1.1 {@code always}: the coordination context is sent with {@code mustUnderstand="1"} and the plain
     * server cannot process it, so it returns a SOAP fault. The endpoint is not invoked: no row exists.
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.wsat.service.WSATException", "javax.xml.ws.WebServiceException", "jakarta.xml.ws.WebServiceException",
                   "javax.xml.ws.soap.SOAPFaultException", "jakarta.xml.ws.soap.SOAPFaultException",
                   "org.apache.cxf.interceptor.Fault", "java.lang.Exception" })
    public void test1_1_Always_PlainServerRejectsMustUnderstand_Commit() throws Exception {
        setPropagation("always");
        initTable(plainUrl);
        final String key = newKey();
        assertException("A plain JAX-WS server must reject the mustUnderstand WS-AT header",
                        call(clientUrl, plainUrl, NONE, COMMIT, key));
        assertEquals("The rejected request must not have been executed", 0, count(plainUrl, key));
    }

    @Test
    @AllowedFFDC({ "com.ibm.ws.wsat.service.WSATException", "javax.xml.ws.WebServiceException", "jakarta.xml.ws.WebServiceException",
                   "javax.xml.ws.soap.SOAPFaultException", "jakarta.xml.ws.soap.SOAPFaultException",
                   "org.apache.cxf.interceptor.Fault", "java.lang.Exception" })
    public void test1_1_Always_PlainServerRejectsMustUnderstand_Rollback() throws Exception {
        setPropagation("always");
        initTable(plainUrl);
        final String key = newKey();
        assertException("A plain JAX-WS server must reject the mustUnderstand WS-AT header",
                        call(clientUrl, plainUrl, NONE, ROLLBACK, key));
        assertEquals("The rejected request must not have been executed", 0, count(plainUrl, key));
    }

    /**
     * 1.2 {@code check_wsdl}: the target's WSDL has no assertion, so plain SOAP is sent and the plain server
     * accepts it. The endpoint saw no transaction, so its row is committed locally and survives a client
     * rollback.
     */
    @Test
    public void test1_2_CheckWsdl_PlainServerAcceptsPlainSoap_Commit() throws Exception {
        setPropagation("check_wsdl");
        initTable(plainUrl);
        assertOutcome(clientUrl, plainUrl, NONE, false, true);
    }

    @Test
    public void test1_2_CheckWsdl_PlainServerAcceptsPlainSoap_Rollback() throws Exception {
        setPropagation("check_wsdl");
        initTable(plainUrl);
        assertOutcome(clientUrl, plainUrl, NONE, false, false);
    }

    /** Set the {@code propagation} attribute on the client's {@code <wsAtomicTransaction/>} and wait for the update. */
    private void setPropagation(String propagation) throws Exception {
        Log.info(getClass(), "setPropagation", "wsAtomicTransaction propagation=" + propagation);
        client.setMarkToEndOfLog();
        final ServerConfiguration config = client.getServerConfiguration();
        config.getWsAtomicTransaction().setPropagation(propagation);
        client.updateServerConfiguration(config);
        client.waitForConfigUpdateInLogUsingMark(null);
    }
}
