/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.fat;

import static com.ibm.ws.jaxrs20.fat.TestUtils.getPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.topology.impl.LibertyServer;

/*
 * MP OpenTracing adds an ExceptionMapper<Throwable> in order to track all unhandled exceptions from
 * JAX-RS resources/providers.  This can conflict with ExceptionMappers that are registered by the
 * application.  This test ensures that application-provided ExceptionMappers are invoked, and not
 * the MP OT mapper.
 */
@RunWith(FATRunner.class)
@SkipForRepeat(JakartaEE10Action.ID) //MP Open Tracing replace with MP Telemetry so this test is not valid beyond EE9
public class ExceptionMappingWithOTTest {

    @Server("com.ibm.ws.jaxrs.fat.exceptionMappingWithOT")
    public static LibertyServer server;

    private static CloseableHttpClient client;
    private static final String testWar = "exceptionMappingWithOT";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, testWar, "com.ibm.ws.jaxrs.fat.exceptionMappingWithOT");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on server",
                      server.waitForStringInLog("CWWKF0011I"));

        // wait for LTPA key to be available to avoid CWWKS4000E
        assertNotNull("CWWKS4105I.* not received on server",
                      server.waitForStringInLog("CWWKS4105I.*"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKW1002W", "CWMOT0008E", "CWWKS9582E", "CWMOT0010W");
        }
    }

    @Before
    public void getHttpClient() {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void resetHttpClient() throws IOException {
        client.close();
    }

    private String getBaseTestUri() {
        return "http://localhost:" + getPort() + "/" + testWar;
    }

    /**
     * Tests that exception mapping works with MP OpenTracing enabled.
     */
    @Test
    public void testExceptionIsMappedWithOpenTracingEnabled() throws Exception {
        HttpGet getMethod = new HttpGet(getBaseTestUri() + "/exceptionMappingWithOT");
        HttpResponse resp = client.execute(getMethod);
        assertEquals(410, resp.getStatusLine().getStatusCode());
    }
}
