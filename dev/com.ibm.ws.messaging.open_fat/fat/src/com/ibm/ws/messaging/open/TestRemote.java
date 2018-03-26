/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.open;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import testjms.web.TestRemoteJmsServlet;

@RunWith(FATRunner.class)
public class TestRemote extends FATServletClient {
    private static final String APP_NAME = "testjms";

    @Server("FATJmsServer")
    @TestServlet(servlet = TestRemoteJmsServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file jndiName 'APP_NAME.war' once it's written to a file
        // Include the 'APP_NAME.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server, APP_NAME, APP_NAME + ".web.*");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
