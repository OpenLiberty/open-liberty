/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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
package test.concurrency.schedasync;

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
import test.concurrency.schedasync.web.SchedAsyncTestServlet;

@MinimumJavaLevel(javaLevel = 21)
@RunWith(FATRunner.class)
public class ScheduledAsyncMethodsTest extends FATServletClient {

    public static final String APP_NAME = "SchedAsyncWeb";

    @Server("com.ibm.ws.concurrent.fat.schedasync")
    @TestServlet(servlet = SchedAsyncTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive SchedAsyncWeb = ShrinkHelper.buildDefaultApp("SchedAsyncWeb", "test.concurrency.schedasync.web");
        ShrinkHelper.exportAppToServer(server, SchedAsyncWeb);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        runTest(server, APP_NAME + '/' + SchedAsyncTestServlet.class.getSimpleName(), "testScheduledAsynchronousMethodsStopRunning");

        server.stopServer();
    }
}
