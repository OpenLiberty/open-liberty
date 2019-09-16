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
package com.ibm.ws.testing.opentracing.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * <p>Test the correct log messages if microProfile-2.1
 * is enabled but no Tracer factory is supplied.
 * No application is needed, we just start the
 * server and check the messages.</p>
 * 
 * <p>The test suite:</p>
 *
 * <ul>
 * <li>{@link #testHelloWorldMP21()}</li>
 * <li>{@link #testHelloWorldMP13SecondRequest()}</li>
 * </ul>
 */
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
public class MicroProfileNoTracer extends FATTestBase {
    /**
     * Set to the generated server before any tests are run.
     */
    private static LibertyServer server;

    /**
     * Start the server.
     * 
     * @throws Exception Errors deploying the application.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("opentracingFATServer4");
        deployHelloWorldApp(server);
        server.startServer();
    }

    /**
     * Stop the server.
     * 
     * @throws Exception Errors stopping the server.
     */
    @AfterClass
    public static void tearDown() throws Exception {
        stopHelloWorldServer(server);
    }

    /**
     * Execute the Hello World JAXRS service and ensure it returns the expected response.
     * 
     * @throws Exception Errors executing the service.
     */
    @Test
    public void testHelloWorldMP21() throws Exception {
        testHelloWorld(server);
    }

    /**
     * Execute the Hello World JAXRS service and ensure it returns the expected response.
     * 
     * @throws Exception Errors executing the service.
     */
    @Test
    public void testHelloWorldMP21SecondRequest() throws Exception {
        testHelloWorld(server);
    }
}
