/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.cdi12.fat.test;

import java.io.IOException;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class DisableTest extends AbstractTest {

    private static final String classesType = "PerRequest";
    private static final String singletonsType = "Singleton";

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.disable")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "disable";
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.cdi12.fat.disable");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testPerRequestHelloworld_DisableCDI() throws IOException {
        runGetMethod("/rest/helloworldc", 200, classesType + " Resource: Hello World", true);
    }

    @Test
    public void testPerRequestContextInResource4C_DisableCDI() throws IOException {
        runGetMethod("/rest/helloworldc/uriinfo", 200, classesType + " Resource Context: helloworldc", true);
    }

    @Test
    public void testPerRequestInjectInResource_DisableCDI() throws IOException {
        runGetMethod("/rest/helloworldc/simplebean", 200, classesType + " Resource Inject: simpleBean is null", true);
    }

    @Test
    public void testPerRequestInjectFromCInResource_DisableCDI() throws IOException {
        runGetMethod("/rest/helloworldc/simplebeanFromNew", 200, classesType + " Resource Inject from New: Hello from SimpleBean", true);
    }

    @Test
    public void testSingletonHelloworld_DisableCDI() throws IOException {
        runGetMethod("/rest/helloworlds", 200, singletonsType + " Resource: Hello World", true);
    }

    @Test
    public void testSingletonContextInResource_DisableCDI() throws IOException {
        runGetMethod("/rest/helloworlds/uriinfo", 200, singletonsType + " Resource Context: helloworlds", true);
    }

    @Test
    public void testSingletonInjectInResource_DisableCDI() throws IOException {
        runGetMethod("/rest/helloworlds/simplebean", 200, singletonsType + " Resource Inject: simpleBean is null", true);
    }

    @Test
    public void testSingletonInjectFromCInResource_DisableCDI() throws IOException {
        runGetMethod("/rest/helloworlds/simplebeanFromNew", 200, singletonsType + " Resource Inject from New: Hello from SimpleBean", true);
    }
}