/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package tests;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import ddTimeout.web.DDTimeoutTestServlet;

@RunWith(FATRunner.class)
public class DDTimeoutTest extends FATServletClient {

    public static final String APP_NAME = "ddTimeout";
    public static final String SERVLET_NAME = APP_NAME + "/ddTimeoutTestServlet";

    @Server("transaction_ddtimeout")
    @TestServlet(servlet = DDTimeoutTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, APP_NAME, "ddTimeout.web.*");

        try {
            server.setServerStartTimeout(300000);
            server.startServer();
        } catch (Exception e) {
            Log.error(DDTimeoutTest.class, "setUp", e);
            // Try again
            server.startServer();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                server.stopServer("WTRN0017W");
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }
}