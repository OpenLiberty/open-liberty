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

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.reenableut.web.CompliantReEnableUTTestServlet;
import com.ibm.ws.reenableut.web.LegacyReEnableUTTestServlet;
import com.ibm.ws.transaction.fat.util.FATUtils;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ReEnableUTTest extends FATServletClient {

    public static final String APP_NAME = "reenableUT";

    @Server("com.ibm.ws.transactional")
    @TestServlet(servlet = LegacyReEnableUTTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @Server("com.ibm.ws.reenableut")
    @TestServlet(servlet = CompliantReEnableUTTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server2;

    @BeforeClass
    public static void setUp() throws Exception {

        final WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.reenableut.web.*");
        ShrinkHelper.exportDropinAppToServer(server, app);
        ShrinkHelper.exportDropinAppToServer(server2, app);

        server2.setHttpDefaultPort(Integer.parseInt(System.getProperty("HTTP_secondary")));
        FATUtils.startServers(server, server2);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        FATUtils.stopServers(server, server2);
    }
}
