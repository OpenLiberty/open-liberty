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
package com.ibm.ws.transport.iiop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.corba.remote.war.CSIv2BothPortsServlet;

/**
 * Two-server test for an EJB server that has BOTH a plain iiopPort and an SSL iiopsPort.
 *
 * <p>The client server looks the EJB up with full corbaname URLs. Lookups whose protocol
 * matches the port (corbaname:iiop on iiopPort, corbaname:iiops on iiopsPort) must work and
 * the bean must be invocable. Cross lookups (corbaname:iiop on iiopsPort and corbaname:iiops
 * on iiopPort) must fail with BAD_PARAM "error parsing URL" (OMG minor code 0x7).
 *
 * <p>This runs sequentially with the other CSIv2 tests: the EJB server uses {@code bvt.prop.IIOP}
 * and {@code bvt.prop.IIOP.secondary.secure}, so it must never run at the same time as
 * CSIv2DisabledIIOPPortTest / CSIv2NoIIOPPortTest (which use the same secure port).
 */
@RunWith(FATRunner.class)
public class CSIv2BothPortsTest extends FATServletClient {

    /** EJB server: plain iiopPort=IIOP and SSL iiopsPort=IIOP.secondary.secure */
    @Server("csiv2BothPortsServer")
    public static LibertyServer ejbServer;

    /** Client server: no nameService; the servlet uses full corbaname URLs */
    @Server("csiv2BothPortsClient")
    @TestServlet(servlet = CSIv2BothPortsServlet.class, contextRoot = "test.corba.remote")
    public static LibertyServer clientServer;

    /** Yoko trace line written each time an IIOP acceptor (listening socket) is opened. */
    private static final String ACCEPTOR_CREATED = "Acceptor created using socket";

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.exportDropinAppToServer(ejbServer, FATSuite.TEST_CORBA_EAR);
        ejbServer.startServer();

        // CWWKI0001I must announce the name service on both the plain iiopPort and the iiopsPort
        final String plainPort = System.getProperty("IIOP.secondary");
        final String sslPort = System.getProperty("IIOP.secondary.secure");
        assertNotNull("Expected CWWKI0001I for the name service on iiopPort " + plainPort,
                      ejbServer.waitForStringInLog("CWWKI0001I: .*corbaloc:iiop:localhost:" + plainPort + "/NameService"));
        assertNotNull("Expected CWWKI0001I for the name service on iiopsPort " + sslPort,
                      ejbServer.waitForStringInLog("CWWKI0001I: .*corbaloc:iiops:localhost:" + sslPort + "/NameService"));

        // With both ports configured the EJB server must open exactly two acceptors: one plain
        // on iiopPort and one SSL on iiopsPort. Check before the client server is started,
        // because later traffic can legitimately cause further (non-endpoint) ORB acceptors.
        assertNotNull("Expected Yoko to log '" + ACCEPTOR_CREATED + "'",
                      ejbServer.waitForStringInTrace(ACCEPTOR_CREATED));
        List<String> acceptors = ejbServer.findStringsInTrace(ACCEPTOR_CREATED);
        assertEquals("Exactly two IIOP acceptors must be created when both ports are configured: " + acceptors,
                     2, acceptors.size());
        String plain = null;
        String ssl = null;
        for (String acceptor : acceptors) {
            if (acceptor.contains("[SSL:")) {
                ssl = acceptor;
            } else {
                plain = acceptor;
            }
        }
        assertNotNull("One acceptor must be an SSL acceptor: " + acceptors, ssl);
        assertNotNull("One acceptor must be a plain acceptor: " + acceptors, plain);
        assertTrue("The SSL acceptor must listen on iiopsPort " + sslPort + ": " + ssl,
                   ssl.contains("localport=" + sslPort + "]"));
        assertTrue("The plain acceptor must listen on iiopPort " + plainPort + ": " + plain,
                   plain.contains("localport=" + plainPort + "]"));
        assertFalse("The plain acceptor must not be an SSL acceptor: " + plain, plain.contains("[SSL:"));

        ShrinkHelper.exportDropinAppToServer(clientServer, FATSuite.TEST_CORBA_REMOTE_WAR);
        clientServer.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        clientServer.stopServer();
        ejbServer.stopServer();
    }
}
