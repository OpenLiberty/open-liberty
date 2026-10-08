/*******************************************************************************
 * Copyright (c) 2017, 2026 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.sse.fat;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.sse.broadcaster.BroadcasterTestServlet;

@RunWith(FATRunner.class)
public class BroadcasterTest extends FATServletClient {

    static final String appName = "BroadcasterApp";

    @Server("io.openliberty.sse.broadcaster")
    @TestServlet(servlet = BroadcasterTestServlet.class, contextRoot = appName)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(appName, "io.openliberty.sse.broadcaster");
        ShrinkHelper.exportDropinAppToServer(server, app);
        server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // SRVE8055E/SRVE8056E: Expected when the Liberty server shuts down while SSE
        // client connections are still open. The webcontainer logs these when it tries
        // to flush/close response streams that the client has already disconnected from
        // (Connection reset by peer). This is normal SSE teardown behaviour.
        server.stopServer("SRVE8055E", "SRVE8056E");
    }

}
