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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * <p>Test that a JAXRS application with LogRecordContext and JSON logging.</p>
 * 
 * <p>The test suite:</p>
 *
 * <ul>
 * <li>{@link #testHelloWorld()}</li>
 * </ul>
 */
public class FATOpentracingHelloWorldLRC extends FATTestBase {
    /**
     * Set to the generated server before any tests are run.
     */
    private static LibertyServer server;

    /**
     * Deploy the application and start the server.
     * 
     * @throws Exception Errors deploying the application.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("opentracingFATServer5");
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
     * Execute the Hello World JAXRS service and ensure JSON log contains ext_traceId and ext_spanId.
     * 
     * @throws Exception Errors executing the service.
     */
    @Test
    public void testLogRecordContext() throws Exception {
        testHelloWorld(server);
        String line = server.waitForStringInLog("helloWorld web service called");
        assertNotNull(line);
        assertTrue(line.contains("ext_traceId") && line.contains("ext_spanId"));
    }
}
