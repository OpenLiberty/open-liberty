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
package test.jdbc.h2.client;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jdbc.h2.client.web.H2ClientTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class H2ClientTest extends FATServletClient {

    @Server("com.ibm.ws.jdbc.fat.h2client")
    @TestServlet(servlet = H2ClientTestServlet.class, contextRoot = "H2ClientTestApp")
    public static LibertyServer server;

    private static org.h2.tools.Server h2TcpServer;

    @BeforeClass
    public static void setUp() throws Exception {
        // -tcpPort 0: let the OS assign a free ephemeral port to avoid collisions
        //             with other processes or parallel test runs; actual port is
        //             retrieved via getPort() and passed to Liberty as h2TcpPort.
        // -ifNotExists: allow the server to create the database on first connect
        //               without requiring it to exist beforehand.
        h2TcpServer = org.h2.tools.Server.createTcpServer("-tcpPort", "0", "-ifNotExists").start();

        server.addEnvVar("h2TcpPort", String.valueOf(h2TcpServer.getPort()));

        WebArchive war = ShrinkHelper.buildDefaultApp("H2ClientTestApp",
                                                      "test.jdbc.h2.client.web");
        ShrinkHelper.exportAppToServer(server, war);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            if (h2TcpServer != null)
                h2TcpServer.stop();
        }
    }
}
