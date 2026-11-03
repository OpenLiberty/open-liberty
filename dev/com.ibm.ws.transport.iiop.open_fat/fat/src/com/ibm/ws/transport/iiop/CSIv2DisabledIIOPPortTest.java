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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
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
import test.corba.remote.war.CSIv2DisabledIIOPServlet;

/**
 * Two-server test proving that a CSIv2-capable SSL client on a separate Liberty
 * server can look up and invoke an EJB hosted on a server configured with
 * iiopPort="-1" (plain IIOP disabled) and iiopsPort only (SSL-only IIOP).
 *
 * <p>The EJB server (csiv2SslOnlyServer) opens only a single TLS socket on
 * iiopsPort and publishes zero plain TAG_INTERNET_IOP profiles in the IOR.
 * The client server (csiv2SslOnlyClient) is configured with appSecurity and
 * a shared keystore so it can negotiate SSL with the server's acceptor.
 *
 * <p>This is the real production use case for iiopPort="-1": force all IIOP
 * traffic through SSL, completely disabling unencrypted plain IIOP.
 */
@RunWith(FATRunner.class)
public class CSIv2DisabledIIOPPortTest extends FATServletClient {

    /** EJB server: iiopPort=-1, SSL-only on iiopsPorts IIOP.secondary.secure (2820) and IIOP.secure (2810) */
    @Server("csiv2DisabledIIOPServer")
    public static LibertyServer ejbServer;

    /** Client server: SSL client ORB, nameService pointing at server's iiopsPort */
    @Server("csiv2DisabledIIOPClient")
    @TestServlet(servlet = CSIv2DisabledIIOPServlet.class, contextRoot = "test.corba.remote")
    public static LibertyServer clientServer;

    /** Yoko trace line written each time an IIOP acceptor (listening socket) is opened. */
    private static final String ACCEPTOR_CREATED = "Acceptor created using socket";

    /** The two iiopsPorts configured in the server's server.xml. */
    private static final List<String> SSL_PORTS = Arrays.asList(System.getProperty("IIOP.secure"),
                                                                System.getProperty("IIOP.secondary.secure"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Deploy the EJB app to the SSL-only server
        ShrinkHelper.exportDropinAppToServer(ejbServer, FATSuite.TEST_CORBA_EAR);
        ejbServer.startServer();

        // CWWKI0001I must announce the name service on each iiopsPort
        for (String sslPort : SSL_PORTS) {
            assertNotNull("Expected CWWKI0001I for the name service on iiopsPort " + sslPort,
                          ejbServer.waitForStringInLog(nameServerAvailable("iiops", sslPort)));
        }

        // With iiopPort="-1" the EJB server must open exactly two acceptors, both SSL, one per
        // iiopsOptions entry. Check this now, before the client server is started,
        // because later traffic can legitimately cause further (non-endpoint) ORB acceptors
        // to be logged in the same trace file.
        assertNotNull("Expected Yoko to log '" + ACCEPTOR_CREATED + "' for the SSL acceptors",
                      ejbServer.waitForStringInTrace(ACCEPTOR_CREATED));
        List<String> acceptors = ejbServer.findStringsInTrace(ACCEPTOR_CREATED);
        assertEquals("Exactly two IIOP acceptors must be created when iiopPort=-1: " + acceptors,
                     SSL_PORTS.size(), acceptors.size());
        for (String sslPort : SSL_PORTS) {
            assertTrue("An SSL acceptor must listen on iiopsPort " + sslPort + ": " + acceptors,
                       acceptors.stream().anyMatch(a -> a.contains("[SSL:") && a.contains("localport=" + sslPort + "]")));
        }

        // Deploy the client WAR and start the client server
        ShrinkHelper.exportDropinAppToServer(clientServer, FATSuite.TEST_CORBA_REMOTE_WAR);
        clientServer.startServer();
    }

    /** Regex matching the CWWKI0001I name server message for the given corbaloc protocol and port. */
    private static String nameServerAvailable(String protocol, String port) {
        return "CWWKI0001I: .*corbaloc:" + protocol + ":localhost:" + port + "/NameService";
    }

    @AfterClass
    public static void afterClass() throws Exception {
        clientServer.stopServer();
        ejbServer.stopServer();
    }
}
