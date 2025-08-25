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
package test.concurrent.no.vt;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.concurrent.no.vt.web.ConcurrentVTDisabledServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ConcurrentVirtualThreadsDisabledTest extends FATServletClient {

    @Server("com.ibm.ws.concurrent.fat.no.vt")
    @TestServlet(servlet = ConcurrentVTDisabledServlet.class,
                 contextRoot = "ConcurrentNoVTWeb")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive ConcurrentNoVTWeb = ShrinkHelper
                        .buildDefaultApp("ConcurrentNoVTWeb",
                                         "test.concurrent.no.vt.web");
        ShrinkHelper.addDirectory(ConcurrentNoVTWeb,
                                  "test-applications/ConcurrentNoVTWeb/resources");
        ShrinkHelper.exportAppToServer(server, ConcurrentNoVTWeb);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
