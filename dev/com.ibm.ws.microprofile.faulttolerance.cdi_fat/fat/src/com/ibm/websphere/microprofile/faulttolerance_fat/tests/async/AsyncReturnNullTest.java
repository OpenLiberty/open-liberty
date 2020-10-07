/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.async;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AsyncReturnNullTest extends FATServletClient {

    private static final String APP_NAME = "ftAsyncReturnNull";

    @Server("AsyncFaultTolerance")
    @TestServlet(contextRoot = APP_NAME, servlet = AsyncReturnNullTestServlet.class)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(AsyncReturnNullTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();
    }

    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeat20AndAbove("AsyncFaultTolerance");

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWMFT0003W");
        server.deleteFileFromLibertyServerRoot("dropins/" + APP_NAME + ".war");
    }

}
