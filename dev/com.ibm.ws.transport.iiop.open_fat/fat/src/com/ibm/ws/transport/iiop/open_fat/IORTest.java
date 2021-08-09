/*
 * =============================================================================
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.transport.iiop.open_fat;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import servlet.IORTestServlet;

@RunWith(FATRunner.class)
public class IORTest extends FATServletClient {

    @Server("com.ibm.ws.transport.iiop.open_fat.server")
    @TestServlet(servlet = IORTestServlet.class, contextRoot = "TestCorbaWeb")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        for (Archive<?> app: FATSuite.SERVER_APPS) ShrinkHelper.exportDropinAppToServer(server, app);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }

}