/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.ejb;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@Mode(FULL)
@RunWith(FATRunner.class)
public class AsyncEJBTest extends FATServletClient {

    private static final String SERVER_NAME = "FaultToleranceEJB";
    private static final String APP_NAME = "AsyncEJBTest";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = AsyncEJBServlet.class)
    public static LibertyServer server;

    //run against both EE8 and EE7 features
    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeatAll(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(AsyncEJBServlet.class.getPackage())
                        .addAsManifestResource(AsyncEJBServlet.class.getPackage(), "permissions.xml", "permissions.xml");
        ShrinkHelper.exportAppToServer(server, app, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
        server.deleteFileFromLibertyServerRoot("apps/" + APP_NAME + ".war");
    }

}
