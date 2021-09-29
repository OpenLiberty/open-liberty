/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance.metrics.app.FallbackServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class CDIFallbackTest extends FATServletClient {

    private static final String SERVER_NAME = "CDIFaultToleranceMetrics";
    private static final String APP_NAME = "CDIFaultToleranceMetrics";

    @Server(value = SERVER_NAME)
    @TestServlet(servlet = FallbackServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    //mpMetrics-4.0 isn't ready yet so can't run against MP50
//  @ClassRule
//  public static RepeatTests r = RepeatFaultTolerance.repeatDefault(SERVER_NAME)
//                  .andWith(RepeatFaultTolerance.ft11metrics20Features(SERVER_NAME));

    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeat(SERVER_NAME, TestMode.FULL, MicroProfileActions.MP40, MicroProfileActions.MP20)
                    .andWith(RepeatFaultTolerance.ft11metrics20Features(SERVER_NAME));

    @BeforeClass
    public static void setUp() throws Exception {
        JavaArchive faulttolerance_jar = ShrinkWrap.create(JavaArchive.class, "faulttolerancemetrics.jar")
                        .addPackages(true, "com.ibm.websphere.microprofile.faulttolerance.metrics.utils");

        WebArchive CDIFaultTolerance_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, "com.ibm.websphere.microprofile.faulttolerance.metrics.app")
                        .addAsLibraries(faulttolerance_jar)
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"), "permissions.xml")
                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"));

        ShrinkHelper.exportDropinAppToServer(server, CDIFaultTolerance_war, DeployOptions.SERVER_ONLY);

        if (!server.isStarted()) {
            server.startServer();
        }

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * CWWKC1101E: The task com.ibm.ws.microprofile.faulttolerance.cdi.FutureTimeoutMonitor@3f76c259, which was submitted to executor service
             * managedScheduledExecutorService[DefaultManagedScheduledExecutorService], failed with the following error:
             * org.eclipse.microprofile.faulttolerance.exceptions.FTTimeoutException: java.util.concurrent.TimeoutException
             */
            server.stopServer("CWWKC1101E");
        }
    }
}
