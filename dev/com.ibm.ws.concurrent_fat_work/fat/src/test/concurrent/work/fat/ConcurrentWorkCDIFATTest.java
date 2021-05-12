/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.concurrent.work.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.topology.impl.LibertyServer;
import test.concurrent.work.cdi.WorkTestCDIServlet;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class ConcurrentWorkCDIFATTest {
    private static final String APP_NAME = "WorkTestCDIApp";

    @Server("com.ibm.ws.concurrent.fat.work.cdi")
    @TestServlet(servlet = WorkTestCDIServlet.class, path = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "test.concurrent.work.cdi");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted())
            server.stopServer("CWWKE1205E"); // StartTimeoutException intentionally caused by test
    }
}