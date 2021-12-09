/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.classloading.DummyServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.classloading.TestServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class TestMultiModuleClassLoading extends FATServletClient {

    private static final String SERVER_NAME = "FaultToleranceMultiModule";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    //run against both EE9, EE8 and EE7 features
    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeat(SERVER_NAME, TestMode.LITE, MicroProfileActions.MP50, MicroProfileActions.MP13,
                                                              RepeatFaultTolerance.MP21_METRICS20);

    @BeforeClass
    public static void setupApp() throws Exception {
        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war")
                        .addPackage(TestServlet.class.getPackage())
                        .addAsManifestResource(TestServlet.class.getResource("war-config.properties"), "microprofile-config.properties");

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war")
                        .addClass(DummyServlet.class);

        // Need to include two wars to stop us taking shortcuts with the classloader
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "MultiModuleClassLoading.ear")
                        .addAsModule(war1)
                        .addAsModule(war2)
                        .setApplicationXML(TestServlet.class.getResource("application.xml"));

        ShrinkHelper.exportDropinAppToServer(server, ear, SERVER_ONLY, OVERWRITE);
        server.startServer();
    }

    @AfterClass
    public static void removeApp() throws Exception {
        server.stopServer();
        server.deleteFileFromLibertyServerRoot("dropins/MultiModuleClassLoading.ear");
    }

    @Test
    public void testClassLoading() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/war1/test", "OK - FallbackB");
    }

}
