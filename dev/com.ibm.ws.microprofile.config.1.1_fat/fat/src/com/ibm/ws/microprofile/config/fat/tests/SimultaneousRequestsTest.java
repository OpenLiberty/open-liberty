/*******************************************************************************
* Copyright (c) 2016, 2020 IBM Corporation and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/

package com.ibm.ws.microprofile.config.fat.tests;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.microprofile.appConfig.simultaneousRequests.test.SimultaneousRequestsTestServlet;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions;
import com.ibm.ws.microprofile.config.fat.repeat.RepeatConfigActions.Version;
import com.ibm.ws.microprofile.config.fat.suite.SharedShrinkWrapApps;

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
public class SimultaneousRequestsTest extends FATServletClient {

    public static final String APP_NAME = "simultaneousRequests";

    @Server("SimultaneousRequestsServer")
    @TestServlet(servlet = SimultaneousRequestsTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatConfigActions.repeat("SimultaneousRequestsServer", Version.CONFIG14_EE7, Version.LATEST);

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive simultaneousRequests_war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                                        .addPackages(true, "com.ibm.ws.microprofile.appConfig.simultaneousRequests.test")
                                                        .addAsLibrary(SharedShrinkWrapApps.getTestAppUtilsJar())
                                                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/permissions.xml"),
                                                                               "permissions.xml")
                                                        .addAsManifestResource(new File("test-applications/" + APP_NAME + ".war/resources/META-INF/microprofile-config.properties"),
                                                                               "microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, simultaneousRequests_war);

        server.startServer();
    }

    @Test
    public void testSimultaneousRequests() throws Exception {

        Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                runTest(server, APP_NAME, "testSimultaneousRequests");
                return null;
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Void> requestOne = executor.submit(callable);
        Thread.sleep(1000); //Just to be safe, space the requests out to ensure the first thread sets a flag.
        Future<Void> requestTwo = executor.submit(callable);

        requestOne.get();
        requestTwo.get();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
