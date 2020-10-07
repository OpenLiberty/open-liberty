/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.web.SimpleFS2PCCloudServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;

@Mode
@RunWith(FATRunner.class)
public class DualServerDynamicFSTest extends DualServerDynamicCoreTest {
    @Server("com.ibm.ws.transaction_FSCLOUD001")
    @TestServlet(servlet = SimpleFS2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer firstServer;

    @Server("com.ibm.ws.transaction_FSCLOUD002")
    @TestServlet(servlet = SimpleFS2PCCloudServlet.class, contextRoot = APP_NAME)
    public static LibertyServer secondServer;

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("NYTRACE: DualServerDynamicTestBase.setUp called");
        server1 = firstServer;
        server2 = secondServer;
        servletName = APP_NAME + "/SimpleFS2PCCloudServlet";
        cloud1RecoveryIdentity = "FScloud001";
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server1, APP_NAME, "com.ibm.ws.transaction.*");
        ShrinkHelper.defaultApp(server2, APP_NAME, "com.ibm.ws.transaction.*");

        server1.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
        server2.setServerStartTimeout(LOG_SEARCH_TIMEOUT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // server1.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
    }

}
