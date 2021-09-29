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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.completionstage;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class CDICompletionStageTest extends FATServletClient {

    private static final String SERVER_NAME = "FaultToleranceMultiModule";
    private static final String APP_NAME = "ftCompletionStage";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, TestMode.FULL, MicroProfileActions.MP50, MicroProfileActions.MP41, MicroProfileActions.MP22);

    @Server(SERVER_NAME)
    @TestServlet(servlet = CDICompletionStageServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(CDICompletionStageTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, app, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
        server.deleteDirectoryFromLibertyServerRoot("dropins/" + APP_NAME + ".war");
    }

}
