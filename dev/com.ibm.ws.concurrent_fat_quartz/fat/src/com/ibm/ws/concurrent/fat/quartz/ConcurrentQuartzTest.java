/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.concurrent.fat.quartz;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import concurrent.fat.quartz.web.QuartzTestServlet;

@RunWith(FATRunner.class)
public class ConcurrentQuartzTest extends FATServletClient {

    public static final String APP_NAME = "quartzapp";

    @Server("com.ibm.ws.concurrent.fat.quartz")
    @TestServlet(servlet = QuartzTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive quartzExtensions = ShrinkWrap //
                        .create(JavaArchive.class, "quartz-extensions.jar") //
                        .addPackage("example.quartz.concurrent");

        ShrinkHelper.exportToServer(server, "quartz", quartzExtensions);

        ShrinkHelper.defaultApp(server, APP_NAME, "concurrent.fat.quartz.web");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
