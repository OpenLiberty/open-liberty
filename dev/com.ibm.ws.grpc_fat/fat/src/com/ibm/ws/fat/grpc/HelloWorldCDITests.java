/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class HelloWorldCDITests extends HelloWorldBasicTest {

    protected static final Class<?> c = HelloWorldCDITests.class;

    private static final Logger LOG = Logger.getLogger(c.getName());

    private static final String DEFAULT_CONFIG_FILE = "grpc.server.xml";

    private static final String CDI_ENABLED = "grpc.server.cdi.xml";
    private static final String CDI_ENABLED_INTERCEPTOR = "grpc.server.cdi.interceptor.xml";

    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;

    private static final Set<String> appName = Collections.singleton("HelloWorldService");

    @Rule
    public TestName name = new TestName();

    @Server("HelloWorldServer")
    public static LibertyServer helloWorldServer;

    @BeforeClass
    public static void setUp() throws Exception {

        helloWorldServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        ShrinkHelper.defaultDropinApp(helloWorldServer, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        ShrinkHelper.defaultDropinApp(helloWorldServer, "HelloWorldClient.war",
                                      "com.ibm.ws.grpc.fat.helloworld.client",
                                      "io.grpc.examples.helloworld",
                                      "com.ibm.ws.fat.grpc.tls");

        helloWorldServer.startServer(HelloWorldCDITests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Setting serverConfigurationFile to null forces a server.xml update (when GrpcTestUtils.setServerConfiguration() is first called) on the repeat run
        // If not set to null, test failures may occur (since the incorrect server.xml could be used)
        serverConfigurationFile = null;

        if (helloWorldServer != null && helloWorldServer.isStarted()) {
            helloWorldServer.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = helloWorldServer;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    /**
     * Tests that the CDI-enabled GreetingCDIBean is injected correctly when cdi-2.0 is enabled
     *
     * @throws Exception
     */
    @Test
    public void testCDIService() throws Exception {
        // enable cdi-2.0
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(helloWorldServer, serverConfigurationFile, CDI_ENABLED, appName, LOG);

        String response = runHelloWorldTest();
        assertTrue("the gRPC CDI request did not complete correctly", response.contains("howdy from GreetingCDIBean us3r1"));

    }

    /**
     * Tests that the CDI-enabled GreetingCDIBean is injected into HelloWorldServerCDIInterceptor when cdi-2.0 is enabled
     *
     * @throws Exception
     */
    @Test
    public void testCDIInterceptor() throws Exception {
        // configure an interceptor
        serverConfigurationFile = GrpcTestUtils.setServerConfiguration(helloWorldServer, serverConfigurationFile, CDI_ENABLED_INTERCEPTOR, appName, LOG);

        String response = runHelloWorldTest();

        assertTrue("the gRPC CDI interceptor was not invoked correctly", response.contains("server CDI interceptor invoked"));
        assertTrue("the gRPC request did not complete correctly", response.contains("us3r1"));

    }
}
