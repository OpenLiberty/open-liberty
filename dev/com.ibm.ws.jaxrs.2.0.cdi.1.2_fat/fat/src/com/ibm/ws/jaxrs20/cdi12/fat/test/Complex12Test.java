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

import static org.junit.Assert.assertTrue;

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
public class Complex12Test extends AbstractTest {

    private final Class<?> c = Complex12Test.class;

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.complex")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "complex";
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.cdi12.fat.complex");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKW1002W");
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
    public void testResourceHelloWorld_ResourceProvider() throws IOException {
        runGetMethod("/rest/jordanresource", 200, "JordanResource: Hello World", true);
    }

    @Test
    public void testResourceContext_ResourceProvider() throws IOException {
        runGetMethod("/rest/jordanresource/uriinfo", 200, "JordanResource Context: jordanresource/uriinfo", true);
    }

    @Test
    public void testResourceInject_ResourceProvider() throws IOException {
        runGetMethod("/rest/jordanresource/simplebean", 200, "JordanResource Inject: Hello from SimpleBean", true);
    }

    @Test
    public void testResourceProvider_ResourceProvider() throws IOException {
        runGetMethod("/rest/helloworld2", 200, "", true);
        // Disable the message test first, add after confirm
        //assertLibertyMessage("The JAXRS-2.0 Resource HelloWorldResource2 scope is Singleton. Liberty gets resource instance from CDI.", 1, "equal");
        runGetMethod("/rest/helloworld2", 200, "", true);
        // Disable the message test first, add after confirm
        //assertLibertyMessage("The CDI scope of the JAXRS-2.0 Provider ContextRequestFilter is Dependent. Liberty gets the provider instance from CDI.", 1, "equal");
    }

    @Test
    public void testGetClassesGetSingletons_ResourceProvider() throws IOException {
        runGetMethod("/rest/jordanresource/provider/jordan", 200, "", true);
        assertLibertyMessage("ResourceProvider Context uriinfo: jordanresource/provider/jordan", 1, "equal");
        assertLibertyMessage("ResourceProvider Inject simplebean: Hello from SimpleBean", 1, "equal");
    }

    @Test
    public void testResourceProviderHelloWorld_ResourceProvider() throws IOException {
        runGetMethod("/rest/resourceprovider", 200, "ResourceProvider: Hello World", true);
    }

    @Test
    public void testResourceProviderContext_ResourceProvider() throws IOException {
        runGetMethod("/rest/resourceprovider/uriinfo", 200, "ResourceProvider Context: resourceprovider/uriinfo", true);
    }

    @Test
    public void testResourceProviderInject_ResourceProvider() throws IOException {
        runGetMethod("/rest/resourceprovider/simplebean", 200, "ResourceProvider Inject: Hello from SimpleBean", true);
    }

    @Test
    public void testResourceProviderAreSameInstance_ResourceProvider() throws IOException {
        String result = runGetMethod("/rest/jordanresource/cdibeans", 200, "", false).toString();

        String str = "bean.getBeanClass(): class com.ibm.ws.jaxrs20.cdi.fat.complex10.JordanResourceProvider";
        assertTrue(result.indexOf(str) == result.lastIndexOf(str));
    }
}