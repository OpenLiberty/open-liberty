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
package test.jakarta.concurrency32;

import jakarta.enterprise.concurrent.spi.ThreadContextProvider;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
import test.jakarta.concurrency32.web.Concurrency32TestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
public class Concurrency32Test extends FATServletClient {

    public static final String APP_NAME = "Concurrency32TestApp";

    @Server("com.ibm.ws.concurrent.fat.jakarta.ee12")
    @TestServlet(servlet = Concurrency32TestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Test application Concurrency32TestApp.ear

        WebArchive concurrency32TestWeb = ShrinkHelper
                        .buildDefaultApp("Concurrency32TestWeb",
                                         "test.jakarta.concurrency32.web");
        ShrinkHelper.addDirectory(concurrency32TestWeb,
                                  "test-applications/Concurrency32TestWeb/resources");

        EnterpriseArchive concurrency32TestApp = ShrinkWrap
                        .create(EnterpriseArchive.class,
                                "Concurrency32TestApp.ear");
        concurrency32TestApp.addAsModule(concurrency32TestWeb);
        ShrinkHelper.addDirectory(concurrency32TestApp,
                                  "test-applications/Concurrency32TestApp/resources");
        ShrinkHelper.exportAppToServer(server,
                                       concurrency32TestApp);

        // fake third-party library that also includes a thread context provider
        JavaArchive localeContextProviderJar = ShrinkWrap //
                        .create(JavaArchive.class,
                                "locale-context.jar")
                        .addPackage("test.context.locale")
                        .addAsServiceProvider(ThreadContextProvider.class.getName(),
                                              "test.context.locale.LocaleContextProvider");
        ShrinkHelper.exportToServer(server,
                                    "lib",
                                    localeContextProviderJar);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }
}
