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

package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.spec.WebArchive;
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
public class HelloWorldThirdPartyApiTest extends HelloWorldBasicTest {

    protected static final Class<?> c = HelloWorldThirdPartyApiTest.class;

    @Rule
    public TestName name = new TestName();

    @Server("HelloWorldThirdPartyAPIServer")
    public static LibertyServer helloWorldServer;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(helloWorldServer, "HelloWorldService.war",
                                      "com.ibm.ws.grpc.fat.helloworld.service",
                                      "io.grpc.examples.helloworld");

        WebArchive helloWorldApp = ShrinkHelper.buildDefaultApp("HelloWorldClient",
                                                                "com.ibm.ws.grpc.fat.helloworld.client",
                                                                "io.grpc.examples.helloworld");
        ShrinkHelper.exportAppToServer(helloWorldServer, helloWorldApp);
        helloWorldServer.addInstalledAppForValidation("HelloWorldClient");

        helloWorldServer.startServer(HelloWorldThirdPartyApiTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
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

    @Test
    public void testHelloWorldThirdPartyApi() throws Exception {
        String response = runHelloWorldTest();
        assertTrue("the gRPC request did not complete correctly", response.contains("us3r1"));
    }

    @Override
    protected String getURLPath() {
        return "grpcClientThirdPartyApi";
    }
}
