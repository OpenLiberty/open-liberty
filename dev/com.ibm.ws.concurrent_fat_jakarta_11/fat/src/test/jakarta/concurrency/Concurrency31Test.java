/*******************************************************************************
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
 *******************************************************************************/
package test.jakarta.concurrency;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.concurrent.spi.ThreadContextProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.concurrency31.web.Concurrency31TestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
public class Concurrency31Test extends FATServletClient {

    public static final String APP_NAME = "Concurrency31TestApp";

    @Server("com.ibm.ws.concurrent.fat.jakarta.ee11")
    @TestServlet(servlet = Concurrency31TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Test application ConcurrencyTestApp.ear [ConcurrencyTestEJB.jar, ConcurrencyTestWeb.war, application.xml]

        WebArchive Concurrency31TestWeb = ShrinkHelper.buildDefaultApp("Concurrency31TestWeb", "test.jakarta.concurrency31.web");
        ShrinkHelper.addDirectory(Concurrency31TestWeb, "test-applications/Concurrency31TestWeb/resources");

        EnterpriseArchive Concurrency31TestApp = ShrinkWrap.create(EnterpriseArchive.class, "Concurrency31TestApp.ear");
        Concurrency31TestApp.addAsModule(Concurrency31TestWeb);
        ShrinkHelper.addDirectory(Concurrency31TestApp, "test-applications/Concurrency31TestApp/resources");
        ShrinkHelper.exportAppToServer(server, Concurrency31TestApp);

        // fake third-party library that also includes a thread context provider
        JavaArchive timeZoneContextProviderJar = ShrinkWrap //
                        .create(JavaArchive.class, "time-zone-context.jar")
                        .addPackage("test.context.timezone")
                        .addAsServiceProvider(ThreadContextProvider.class.getName(),
                                              "test.context.timezone.TimeZoneContextProvider");
        ShrinkHelper.exportToServer(server, "lib", timeZoneContextProviderJar);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            // Test that virtual threads are interrupted (two of them will print to
            // System.out after interrupt) when the application stops.
            ServerConfiguration config = server.getServerConfiguration();
            config.getApplications()
                            .removeBy("location", "Concurrency31TestApp.ear");
            server.setMarkToEndOfLog(server.getDefaultLogFile());
            server.updateServerConfiguration(config);

            String[] expected = {
                                  "WWKZ0009I.*Concurrency31TestApp",
                                  "O TestVirtualThreadsInterruptedWhenAppStopped1",
                                  "O TestVirtualThreadsInterruptedWhenAppStopped2"
            };
            List<String> found = server //
                            .waitForConfigUpdateInLogUsingMark(Collections.emptySet(),
                                                               expected);

            // waitForConfigUpdateInLogUsingMark returns additional lines
            // that we didn't ask for, so we cannot compare the count.
            // Instead, check for each separately.

            assertEquals(found.toString(), 1, found.stream()
                            .filter(line -> line.matches(".*" + expected[0] + ".*"))
                            .count());

            assertEquals(found.toString(), 1, found.stream()
                            .filter(line -> line.contains(expected[1]))
                            .count());

            assertEquals(found.toString(), 1, found.stream()
                            .filter(line -> line.contains(expected[2]))
                            .count());
        } finally {
            server.stopServer();
        }
    }
}
