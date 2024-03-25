/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jca.jms.example.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jca.jms.example.fat.FATSuite;

@RunWith(FATRunner.class)
public class JCAStoreSampleAppTest extends FATServletClient {
    private static final String WAR_NAME = "jcastore";

    @Server(FATSuite.SERVER)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the web module and application
        WebArchive fvtweb_war = ShrinkWrap.create(WebArchive.class, WAR_NAME + ".war");
        fvtweb_war.addPackage("test.jca.jms.example.mdb");
        fvtweb_war.addPackage("test.jca.jms.example.web");
        fvtweb_war.addAsWebInfResource(new File("test-applications/" + WAR_NAME + "/resources/WEB-INF/web.xml"));
        ShrinkHelper.exportAppToServer(server, fvtweb_war);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testJCAStoreSampleApp() throws Exception {
        runTest(server, WAR_NAME + "/JCAStoreServlet", "purchase10Lightbulbs");
        runTest(server, WAR_NAME + "/JCAStoreServlet", "purchase12Lightbulbs");
        // MDB should run now and restock the amount to 35 - need to wait for that to happen
        String line = server.waitForStringInLog("InventoryTrackerMDB completed processing for item", 30000);
        if (line == null)
            throw new Exception("Didn't find output from MDB in messages.log");
        runTest(server, WAR_NAME + "/JCAStoreServlet", "purchase32Lightbulbs");
    }
}
