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
public class HelloWorldTest extends HelloWorldBasicTest {

    protected static final Class<?> c = HelloWorldTest.class;

    @Rule
    public TestName name = new TestName();

    @Server("HelloWorldServer")
    public static LibertyServer helloWorldServer;

    @BeforeClass
    public static void setUp() throws Exception {

        helloWorldServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        // set exportApps = true and these apps will be saved under publish/savedApps/helloWorld/
        boolean exportApps = false;
        WebArchive helloWorldService = null;
        WebArchive helloWorldClient = null;

        // add all classes from com.ibm.ws.grpc.fat.helloworld.service and io.grpc.examples.helloworld
        // to a new app HelloWorldService.war
        helloWorldService = ShrinkHelper.defaultDropinApp(helloWorldServer, "HelloWorldService.war",
                                                          "com.ibm.ws.grpc.fat.helloworld.service",
                                                          "io.grpc.examples.helloworld");

        // add all classes from com.ibm.ws.grpc.fat.helloworld.client, io.grpc.examples.helloworld,
        // and com.ibm.ws.fat.grpc.tls to a new app HelloWorldClient.war.
        helloWorldClient = ShrinkHelper.defaultDropinApp(helloWorldServer, "HelloWorldClient.war",
                                                         "com.ibm.ws.grpc.fat.helloworld.client",
                                                         "io.grpc.examples.helloworld");

        if (exportApps) {
            ShrinkHelper.exportArtifact(helloWorldService, "publish/savedApps/helloWorld/");
            ShrinkHelper.exportArtifact(helloWorldClient, "publish/savedApps/helloWorld/");
        }

        helloWorldServer.startServer(HelloWorldTest.class.getSimpleName() + ".log");
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
    public void testHelloWorld() throws Exception {
        String response = runHelloWorldTest();
        assertTrue("the gRPC request did not complete correctly", response.contains("us3r1"));
    }
}
