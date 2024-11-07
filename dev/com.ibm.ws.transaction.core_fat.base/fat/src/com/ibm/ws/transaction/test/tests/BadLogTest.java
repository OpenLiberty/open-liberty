/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.transaction.test.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Transaction;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import servlets.BadLogServlet;

@RunWith(FATRunner.class)
public class BadLogTest extends FATServletClient {

    public static final String APP_NAME = "transaction";

    @Server("com.ibm.ws.transaction_badlog")
    @TestServlet(servlet = BadLogServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultApp(server, APP_NAME, "servlets.*");

        // Tran log dir config is an unwritable location on windows.
        // If we are not on windows we need to set it to an similarly unwritable location.
        final ServerConfiguration serverConfig = server.getServerConfiguration();
        final Transaction tranConfig = serverConfig.getTransaction();

        if (OperatingSystem.WINDOWS != server.getMachine().getOperatingSystem()) {
            tranConfig.setTransactionLogDirectory("/bin");
            server.updateServerConfiguration(serverConfig);
        }

        FATUtils.startServers(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server);
    }
}

