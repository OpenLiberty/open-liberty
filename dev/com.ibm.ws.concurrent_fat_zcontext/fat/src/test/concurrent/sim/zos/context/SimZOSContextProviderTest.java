/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.concurrent.sim.zos.context;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ManagedThreadFactory;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

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

    /**
     * Verify that ForkJoinWorkerThreads which are created by a ManagedThreadFactory are
     * interrupted upon deactivate of the ManagedThreadFactory just as other managed threads are.
     */
    @Test
    public void testInterruptOnDeactivate() throws Exception {
        // Add <managedThreadFactory id="tf-testInterruptOnDeactivate" jndiName="concurrent/testInterruptOnDeactivate-threadFactory"/>
        ServerConfiguration config = server.getServerConfiguration();
        ManagedThreadFactory threadFactory = new ManagedThreadFactory();
        threadFactory.setId("tf-testInterruptOnDeactivate");
        threadFactory.setJndiName("concurrent/testInterruptOnDeactivate-threadFactory");
        config.getManagedThreadFactories().add(threadFactory);

        // save
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.emptySet());

        runTest(server, "SimZOSContextWeb", "testInterruptOnDeactivate_threadStart");

        // Remove the managedThreadFactory
        config.getManagedThreadFactories().remove(threadFactory);

        // save
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("SimZOSContextWeb"));

        runTest(server, "SimZOSContextWeb", "testInterruptOnDeactivate_waitForInterrupt");
    }
}
