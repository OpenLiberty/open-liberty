/*
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.corba.remote.war.RemoteEphemeralIIOPPortTestServlet;

/**
 * Tests that a client on a separate Liberty server can look up and invoke an EJB
 * hosted on a server that was started with {@code iiopPort="-1"} (no SSL, no CSIv2).
 *
 * <p>Sequence:
 * <ol>
 *   <li>Start {@code ephemeralIiopServer} with {@code iiopPort="-1"} and the EJB app.
 *       Yoko opens a {@code ServerSocket(0)} and the OS assigns an ephemeral port.</li>
 *   <li>Wait for the Yoko trace line {@code "Acceptor created using socket"} which
 *       includes the socket's {@code toString()} — that embeds the actual local port.</li>
 *   <li>Parse the ephemeral port from that trace line and inject it as the
 *       {@code iiop.ephemeral.port} bootstrap variable on {@code ephemeralIiopClient}.</li>
 *   <li>Start {@code ephemeralIiopClient} whose {@code server.xml} contains
 *       {@code <orb nameService="corbaname::localhost:${iiop.ephemeral.port}"/>}.</li>
 *   <li>The test servlet ({@link RemoteEphemeralIIOPPortTestServlet}) performs a real
 *       cross-process CosNaming lookup and EJB method invocation over that port.</li>
 * </ol>
 */
@RunWith(FATRunner.class)
public class RemoteEphemeralIIOPPortTest extends FATServletClient {

    /**
     * Yoko logs "Acceptor created using socket ServerSocket[addr=...,localport=NNNNN]"
     * at FINE level when {@code iiopPort=-1} causes {@code ServerSocket(0)} to be used.
     * We match the {@code localport=} portion to extract the ephemeral port number.
     */
    private static final Pattern ACCEPTOR_PORT_PATTERN =
            Pattern.compile("localport=(\\d+)");

    @Server("ephemeralIIOPServer")
    public static LibertyServer ejbServer;

    @Server("ephemeralIIOPClient")
    @TestServlet(servlet = RemoteEphemeralIIOPPortTestServlet.class, contextRoot = "test.corba.remote")
    public static LibertyServer clientServer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Deploy EJB app to the server that exposes it on an ephemeral port
        ShrinkHelper.exportDropinAppToServer(ejbServer, FATSuite.TEST_CORBA_EAR);

        // Start EJB server first so the ephemeral port is assigned.
        // Pass the shared log file name so the timedexit-1.0 feature on ejbServer
        // keeps the server alive while clientServer is running its test methods.
        // Without this, timedexit fires ~15 s after ejbServer launches, which is
        // before the client servlet tests finish (startup + SSL handshake > 15 s).
        ejbServer.startServer("RemoteEphemeralIiopPortTest.log");

        // Wait until Yoko has opened the acceptor and logged the socket details.
        // The trace line looks like:
        //   Acceptor created using socket ServerSocket[addr=0.0.0.0/0.0.0.0,localport=43217]
        String acceptorLine = ejbServer.waitForStringInTrace("Acceptor created using socket");
        assertNotNull("Expected Yoko to log 'Acceptor created using socket' after opening ServerSocket(0)", acceptorLine);

        // Extract the ephemeral port from "localport=NNNNN" in the matched line
        Matcher m = ACCEPTOR_PORT_PATTERN.matcher(acceptorLine);
        if (!m.find()) {
            fail("Could not parse ephemeral port from acceptor trace line: " + acceptorLine);
        }
        int ephemeralPort = Integer.parseInt(m.group(1));
        System.out.println("### RemoteEphemeralIiopPortTest: ephemeral IIOP port = " + ephemeralPort + " ###");

        // Inject the ephemeral port into the client server's bootstrap properties
        // so that server.xml's ${iiop.ephemeral.port} resolves to the real port
        // when the client ORB is initialised.
        clientServer.addBootstrapProperties(
                Collections.singletonMap("iiop.ephemeral.port", String.valueOf(ephemeralPort)));

        // Deploy the client war and start the client server
        ShrinkHelper.exportDropinAppToServer(clientServer, FATSuite.TEST_CORBA_REMOTE_WAR);
        clientServer.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        clientServer.stopServer();
        ejbServer.stopServer();
    }
}
