/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.sim.zos.context;

import static org.junit.Assert.assertNotNull;

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
import test.concurrent.sim.zos.context.web.SimZOSContextTestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class SimZOSContextProviderTest extends FATServletClient {

    @Server("com.ibm.ws.concurrent.fat.zcontext")
    @TestServlet(servlet = SimZOSContextTestServlet.class, contextRoot = "SimZOSContextWeb")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive SimZOSContextWeb = ShrinkHelper.buildDefaultApp("SimZOSContextWeb", "test.concurrent.sim.zos.context.web");
        ShrinkHelper.addDirectory(SimZOSContextWeb, "test-applications/SimZOSContextWeb/resources");
        ShrinkHelper.exportAppToServer(server, SimZOSContextWeb);

        server.startServer();
        // wait for LTPA key to be available to avoid CWWKS4000E
        assertNotNull("CWWKS4105I.* not received on server",
                      server.waitForStringInLog("CWWKS4105I.*"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
