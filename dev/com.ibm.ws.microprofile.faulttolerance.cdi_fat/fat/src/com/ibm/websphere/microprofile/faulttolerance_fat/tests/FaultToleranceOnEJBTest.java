/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.TestException;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.async.FTAsyncEJBTestServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.bulkhead.BulkheadEJBTestServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.circuitbreaker.CircuitBreakerEJBTestServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.fallback.FallbackOnEJBServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.interceptedretry.InterceptedRetryOnEJBServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.retry.RetryOnEJBServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.timeout.TimeOutOnEJBServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.faulttolerance.fat.repeat.RepeatFaultTolerance;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests we can use a fault tolerance annotation placed directly on an EJB
 */
@RunWith(FATRunner.class)
public class FaultToleranceOnEJBTest extends FATServletClient {

    private final static String SERVER_NAME = "FTAnnotationsDirectlyOnEjb";
    private final static String APP_NAME = "FTAnnotationsDirectlyOnEjbApp";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(contextRoot = APP_NAME, servlet = RetryOnEJBServlet.class),
                    @TestServlet(contextRoot = APP_NAME, servlet = TimeOutOnEJBServlet.class),
                    @TestServlet(contextRoot = APP_NAME, servlet = FTAsyncEJBTestServlet.class),
                    @TestServlet(contextRoot = APP_NAME, servlet = BulkheadEJBTestServlet.class),
                    @TestServlet(contextRoot = APP_NAME, servlet = CircuitBreakerEJBTestServlet.class),
                    @TestServlet(contextRoot = APP_NAME, servlet = InterceptedRetryOnEJBServlet.class),
                    @TestServlet(contextRoot = APP_NAME, servlet = FallbackOnEJBServlet.class),
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatFaultTolerance.repeatDefault(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, TestException.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CNTR0020E");
    }

}
