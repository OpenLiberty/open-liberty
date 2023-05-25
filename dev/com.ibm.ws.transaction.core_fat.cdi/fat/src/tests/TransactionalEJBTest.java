/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class TransactionalEJBTest extends FATServletClient {

    public static final String APP_NAME = "transactionalEJB";
    public static final String SERVLET_NAME = APP_NAME + "/transactionalEJB";

    private final long TIMEOUT = 10000; // should have failed very fast

    @Server("com.ibm.ws.transactional")
    public static LibertyServer server;

    @Test
    public void testNoTransactionalEJB() throws Exception {
        // Check transactionalEJB app didn't start
        assertNotNull("TestEJB did not fail to load", server.waitForStringInLog("CWOWB2000E", TIMEOUT));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWWKZ0002E");
        ShrinkHelper.cleanAllExportedArchives();
    }

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "transactionalEJB.*");
        server.setServerStartTimeout(600000);
        LibertyServer.setValidateApps(false);
        server.startServer(true);
    }
}
