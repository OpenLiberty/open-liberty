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
import test.corba.remote.war.CSIv2NoIIOPPortServlet;

/**
 * Similar to {@link CSIv2DisabledIIOPPortTest}, but with a single SSL port: the EJB server's iiopEndpoint
 * has no iiopPort attribute at all (it defaults to -1) and only an iiopsOptions
 * child with iiopsPort. Plain IIOP must be disabled and SSL-only IIOP must work.
 *
 * <p>This runs sequentially with CSIv2DisabledIIOPPortTest: both EJB servers use the
 * same ports, so they must never be started at the same time.
 */
@RunWith(FATRunner.class)
public class CSIv2NoIIOPPortTest extends FATServletClient {

    /** EJB server: no iiopPort, SSL-only on iiopsPort=IIOP.secondary.secure */
    @Server("csiv2NoIIOPPortServer")
    public static LibertyServer ejbServer;

    /** Client server: SSL client ORB, nameService pointing at server's iiopsPort */
    @Server("csiv2NoIIOPPortClient")
    @TestServlet(servlet = CSIv2NoIIOPPortServlet.class, contextRoot = "test.corba.remote")
    public static LibertyServer clientServer;

    /** Yoko trace line written each time an IIOP acceptor (listening socket) is opened. */
    private static final String ACCEPTOR_CREATED = "Acceptor created using socket";

    @BeforeClass
    public static void beforeClass() throws Exception {
        ShrinkHelper.exportDropinAppToServer(ejbServer, FATSuite.TEST_CORBA_EAR);
        ejbServer.startServer();

        // CWWKI0001I must announce the name service on the iiopsPort
        final String sslPort = System.getProperty("IIOP.secondary.secure");
        assertNotNull("Expected CWWKI0001I for the name service on iiopsPort " + sslPort,
                      ejbServer.waitForStringInLog("CWWKI0001I: .*corbaloc:iiops:localhost:" + sslPort + "/NameService"));

        // With no iiopPort the EJB server must open exactly one acceptor, and it must be
        // the SSL one on iiopsPort. Check this before the client server is started.
        assertNotNull("Expected Yoko to log '" + ACCEPTOR_CREATED + "' for the SSL acceptor",
                      ejbServer.waitForStringInTrace(ACCEPTOR_CREATED));
        List<String> acceptors = ejbServer.findStringsInTrace(ACCEPTOR_CREATED);
        assertEquals("Exactly one IIOP acceptor must be created when iiopPort is absent: " + acceptors,
                     1, acceptors.size());
        String acceptor = acceptors.get(0);
        assertTrue("The only acceptor must be an SSL acceptor: " + acceptor, acceptor.contains("[SSL:"));
        assertTrue("The only acceptor must listen on iiopsPort " + sslPort + ": " + acceptor,
                   acceptor.contains("localport=" + sslPort + "]"));

        ShrinkHelper.exportDropinAppToServer(clientServer, FATSuite.TEST_CORBA_REMOTE_WAR);
        clientServer.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        clientServer.stopServer();
        ejbServer.stopServer();
    }
}
