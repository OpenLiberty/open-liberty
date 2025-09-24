/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.transaction.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.TxFATServletClient;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class UOWScopeCallbackTest extends TxFATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/UOWScopeCallbackServlet";

    @Server("com.ibm.ws.transaction")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "servlets.*");

        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server);
        ShrinkHelper.cleanAllExportedArchives();
    }

    @Test
    public void testBadBehavior() throws Exception {
        runTest(server, SERVLET_NAME, "testBadBehavior");
    }

    @Test
    public void testGoodBehavior() throws Exception {
        try (AutoCloseable x = withExtraTranAttributes(server, APP_NAME, "correctUOWScopeCallbacks", "true")) {
            runTest(server, SERVLET_NAME, "testGoodBehavior");
        }
    }
}
