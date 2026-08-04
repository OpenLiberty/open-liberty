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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
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
import test.jakarta.concurrency32cdi.web.Concurrency32CDITestServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 21)
public class Concurrency32WithCDITest extends FATServletClient {

    public static final String APP_NAME = "Concurrency32CDITestApp";

    @Server("com.ibm.ws.concurrent.fat.jakarta.ee12.cdi")
    @TestServlet(servlet = Concurrency32CDITestServlet.class,
                 contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive concurrency32CDITestWeb = ShrinkHelper
                        .buildDefaultApp("Concurrency32CDITestWeb",
                                         "test.jakarta.concurrency32cdi.web");
        ShrinkHelper.addDirectory(concurrency32CDITestWeb,
                                  "test-applications/Concurrency32CDITestWeb/resources");

        EnterpriseArchive concurrency32CDITestApp = ShrinkWrap
                        .create(EnterpriseArchive.class,
                                "Concurrency32CDITestApp.ear");
        concurrency32CDITestApp.addAsModule(concurrency32CDITestWeb);
        ShrinkHelper.addDirectory(concurrency32CDITestApp,
                                  "test-applications/Concurrency32CDITestApp/resources");
        ShrinkHelper.exportAppToServer(server,
                                       concurrency32CDITestApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer();
    }
}
