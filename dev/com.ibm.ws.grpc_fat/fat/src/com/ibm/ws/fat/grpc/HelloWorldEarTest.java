/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
public class HelloWorldEarTest extends HelloWorldBasicTest {

    protected static final Class<?> c = HelloWorldEarTest.class;
    private static final boolean exportApps = false;

    @Rule
    public TestName name = new TestName();

    @Server("HelloWorldServerEar")
    public static LibertyServer helloWorldServerEar;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive helloWorldService = ShrinkHelper.buildDefaultApp("HelloWorldService.war",
                                                                    "com.ibm.ws.grpc.fat.helloworld.service");

        WebArchive helloWorldClient = ShrinkHelper.buildDefaultApp("HelloWorldClient.war",
                                                                   "com.ibm.ws.grpc.fat.helloworld.client");

        JavaArchive helloWorldProtoLib = ShrinkHelper.buildJavaArchive("HelloWorldLib" + ".jar",
                                                                       "io.grpc.examples.helloworld");

        EnterpriseArchive helloWorldAppEar = ShrinkWrap.create(EnterpriseArchive.class, "HelloWorldApp.ear");
        helloWorldAppEar.addAsModule(helloWorldService);
        helloWorldAppEar.addAsModule(helloWorldClient);
        helloWorldAppEar.addAsLibrary(helloWorldProtoLib);

        ShrinkHelper.addDirectory(helloWorldAppEar, "test-applications/" + "HelloWorldApp.ear" + "/resources");
        ShrinkHelper.exportDropinAppToServer(helloWorldServerEar, helloWorldAppEar);

        if (exportApps) {
            ShrinkHelper.exportArtifact(helloWorldAppEar, "publish/savedApps/helloWorld/");
            ShrinkHelper.exportArtifact(helloWorldService, "publish/savedApps/helloWorld/");
            ShrinkHelper.exportArtifact(helloWorldClient, "publish/savedApps/helloWorld/");
        }

        helloWorldServerEar.startServer(HelloWorldEarTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (helloWorldServerEar != null && helloWorldServerEar.isStarted()) {
            helloWorldServerEar.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = helloWorldServerEar;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testHelloWorldEar() throws Exception {
        String response = runHelloWorldTest();
        assertTrue("the gRPC request did not complete correctly", response.contains("us3r1"));
    }
}
