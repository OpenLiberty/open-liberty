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
package com.ibm.websphere.microprofile.faulttolerance.metrics.fat.tests.circuitbreaker;

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
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class CircuitBreakerMetricTest extends FATServletClient {

    private static final String APP_NAME = "ftCircuitBreakerMetrics";
    private static final String SERVER_NAME = "CDIFaultToleranceMetricsRemoval";

    @Server(SERVER_NAME)
    @TestServlet(contextRoot = APP_NAME, servlet = CircuitBreakerMetricServlet.class)
    public static LibertyServer server;

    @ClassRule
    // Note: this test only runs against FT 1.1 and 2.0 as later versions of the TCK include this test.
    public static RepeatTests r = RepeatTests.with(RepeatFaultTolerance.mp32Features(SERVER_NAME))
                    .andWith(RepeatFaultTolerance.ft11metrics20Features(SERVER_NAME).fullFATOnly())
                    .andWith(RepeatFaultTolerance.mp20Features(SERVER_NAME).fullFATOnly());

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackage(CircuitBreakerMetricServlet.class.getPackage())
                        .addAsManifestResource(CircuitBreakerMetricTest.class.getResource("permissions.xml"), "permissions.xml");

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
        server.deleteFileFromLibertyServerRoot("dropins/" + APP_NAME + ".war");
    }

}
