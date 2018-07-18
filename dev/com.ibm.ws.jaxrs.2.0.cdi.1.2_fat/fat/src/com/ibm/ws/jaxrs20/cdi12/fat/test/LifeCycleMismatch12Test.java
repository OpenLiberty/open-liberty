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

import java.util.HashMap;
import java.util.Map;

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
public class LifeCycleMismatch12Test extends AbstractTest {

    private final static String target = "lifecyclemismatch/ClientTestServlet";

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemismatch")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "lifecyclemismatch";
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemismatch",
                                                       "com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemismatch.simpleresource");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKW1001W", "CWWKW1002W");
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
    public void testApplicationScopedResource() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "ApplicationScopedResource");

        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 1");

        assertLibertyMessage("CWWKW1001W(?=.*PerRequest)(?=.*ApplicationScopedResource)(?=.*javax.enterprise.context.ApplicationScoped)(?=.*CDI)",
                             1, "equal");
        // assertLibertyMessage("CWWKW1000I: The JAXRS-2.0 Resource ApplicationScopedResource scope is Singleton. Liberty gets resource instance from CDI.", 1, "equal");
    }

    @Test
    public void testDefaultResource() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "DefaultResource");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");

    }

    @Test
    public void testDependentResource() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "DependentResource");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");

    }

    @Test
    public void testRequestScopedResource() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "RequestScopedResource");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");

    }

    @Test
    public void testSessionScopedResource() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "SessionScopedResource");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");

    }

    @Test
    public void testApplicationScopedSingleton() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "ApplicationScopedSingleton");

        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 1");

        //assertLibertyMessage("CWWKW1000W: The scope of JAXRS-2.0 Resource ApplicationScopedSingleton is Singleton. Liberty gets resource instance from CDI.", 1, "equal");

    }

    @Test
    public void testDefaultSingleton() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "DefaultSingleton");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 1");
        //assertLibertyMessage("CWWKW1000W: The scope of JAXRS-2.0 Resource DefaultSingleton is Singleton. Liberty gets resource instance from CDI.", 1, "equal");

    }

    @Test
    public void testDependentSingleton() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "DependentSingleton");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 1");
        //assertLibertyMessage("CWWKW1000W: The scope of JAXRS-2.0 Resource DependentSingleton is Singleton. Liberty gets resource instance from CDI.", 1, "equal");

    }

    @Test
    public void testRequestScopedSingleton() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "RequestScopedSingleton");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        assertLibertyMessage("CWWKW1001W(?=.*Singleton)(?=.*RequestScopedSingleton)(?=.*javax.enterprise.context.RequestScoped)(?=.*CDI)",
                             1, "equal");

    }

    @Test
    public void testSessionScopedSingleton() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "SessionScopedSingleton");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        this.runTestOnServer(target, "testResource", p,
                             "inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBean, I'm a Studnet. counter: 0");
        assertLibertyMessage("CWWKW1001W(?=.*Singleton)(?=.*SessionScopedSingleton)(?=.*javax.enterprise.context.SessionScoped)(?=.*CDI)",
                             1, "equal");

    }

    @Test
    public void testApplicationScopedProvider() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "ApplicationScopedProvider");

        this.runTestOnServer(target, "testProvider", p, "");
        assertLibertyMessage("CWWKW1002W(?=.*ApplicationScopedProvider)(?=.*javax.enterprise.context.ApplicationScoped)(?=.*CDI)",
                             1,
                             "equal");
        assertLibertyMessage("ApplicationScopedProvider inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBeancounter: 0", 1, "equal");
        this.runTestOnServer(target, "testProvider", p, "");
        assertLibertyMessage("ApplicationScopedProvider inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBeancounter: 1", 1, "equal");

    }

    @Test
    public void testDefaultProvider() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "DefaultProvider");
        this.runTestOnServer(target, "testProvider", p, "");
        assertLibertyMessage("CWWKW1002W(?=.*DefaultProvider)(?=.*javax.enterprise.context.Dependent)(?=.*CDI)",
                             1,
                             "equal");
        assertLibertyMessage("DefaultProvider inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBeancounter: 0", 1, "equal");
        this.runTestOnServer(target, "testProvider", p, "");
        assertLibertyMessage("DefaultProvider inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBeancounter: 1", 1, "equal");

    }

    @Test
    public void testDependentProvider() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "DependentProvider");
        this.runTestOnServer(target, "testProvider", p, "");
        assertLibertyMessage("CWWKW1002W(?=.*DependentProvider)(?=.*javax.enterprise.context.Dependent)(?=.*CDI)",
                             1,
                             "equal");
        assertLibertyMessage("DependentProvider inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBeancounter: 0", 1, "equal");
        this.runTestOnServer(target, "testProvider", p, "");
        assertLibertyMessage("DependentProvider inject test start...injected is NOT null...injected.getMessage returned...Hello from SimpleBeancounter: 1", 1, "equal");

    }

    @Test
    public void testRequestScopedProvider() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "RequestScopedProvider");
        this.runTestOnServer(target, "testProvider", p, "");
        assertLibertyMessage("CWWKW1002W(?=.*RequestScopedProvider)(?=.*javax.enterprise.context.RequestScoped)(?=.*JAXRS)",
                             1, "equal");
        assertLibertyMessage("RequestScopedProvider inject test start...injected is null...FAILEDcounter: 0", 1, "equal");
        this.runTestOnServer(target, "testProvider", p, "");
        assertLibertyMessage("RequestScopedProvider inject test start...injected is null...FAILEDcounter: 1", 1, "equal");

    }

    @Test
    public void testSessionScopedProvider() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("resourcePath", "SessionScopedProvider");
        this.runTestOnServer(target, "testProvider", p, "");
        assertLibertyMessage("CWWKW1002W(?=.*SessionScopedProvider)(?=.*javax.enterprise.context.SessionScoped)(?=.*JAXRS)",
                             1, "equal");
        assertLibertyMessage("SessionScopedProvider inject test start...injected is null...FAILEDcounter: 0", 1, "equal");
        this.runTestOnServer(target, "testProvider", p, "");
        assertLibertyMessage("SessionScopedProvider inject test start...injected is null...FAILEDcounter: 1", 1, "equal");

    }
}