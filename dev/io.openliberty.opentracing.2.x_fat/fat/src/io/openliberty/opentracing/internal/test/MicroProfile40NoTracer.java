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
package io.openliberty.opentracing.internal.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * <p>Test the correct log messages if microProfile-1.3
 * is enabled but no Tracer factory is supplied.
 * No application is needed, we just start the
 * server and check the messages.</p>
 * 
 * <p>The test suite:</p>
 *
 * <ul>
 * <li>{@link #testHelloWorldMP40()}</li>
 * <li>{@link #testHelloWorldMP40SecondRequest()}</li>
 * </ul>
 */
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
public class MicroProfile40NoTracer extends FATTestBase {
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
    public void testHelloWorldMP40() throws Exception {
        testHelloWorld(server);
    }

    /**
     * Execute the Hello World JAXRS service and ensure it returns the expected response.
     * 
     * @throws Exception Errors executing the service.
     */
    @Test
    public void testHelloWorldMP40SecondRequest() throws Exception {
        testHelloWorld(server);
    }
}
