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
public class Basic12Test extends AbstractTest {

    private static final String classesType = "PerRequest";
    private static final String singletonsType = "Singleton";

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.basic")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "basic";
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.cdi12.fat.basic");
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
    // Restart server for log cleaning
    public void testPerRequestHelloworld() throws Exception {
        server.restartServer();
        runGetMethod("/rest/helloworldc", 200, classesType + " Resource: Hello World", true);
        assertLibertyMessage(classesType + " Injection successful...", 1, "equal");
    }

    @Test
    public void testPerRequestContextInResource4C() throws Exception {
        server.restartServer();
        runGetMethod("/rest/helloworldc/uriinfo", 200, classesType + " Resource Context: helloworldc", true);
        assertLibertyMessage(classesType + " Injection successful...", 1, "equal");
        runGetMethod("/rest/helloworldc/uriinfo", 200, classesType + " Resource Context: helloworldc", true);
        assertLibertyMessage(classesType + " Injection successful...", 2, "equal");
    }

    @Test
    public void testPerRequestContextInResource4C_lifecycle() throws IOException {
        runGetMethod("/rest/helloworldc/uriinfo", 200, classesType + " Resource Context: helloworldc", true);
        assertLibertyMessage("The counts is: 1.", 0, "equal");
    }

    @Test
    public void testPerRequestContextInResource4C_queryParam() throws IOException {
        runGetMethod("/rest/helloworldc/uriinfo?id=123", 200, classesType + " Resource Context: helloworldc", true);
        assertLibertyMessage("The id is: 123.", 1, "equal");
        runGetMethod("/rest/helloworldc/uriinfo?id=321", 200, classesType + " Resource Context: helloworldc", true);
        assertLibertyMessage("The id is: 321.", 1, "equal");
    }

    @Test
    public void testPerRequestInjectInResource() throws IOException {
        runGetMethod("/rest/helloworldc/simplebean", 200, classesType + " Resource Inject: Hello from SimpleBean", true);
//        assertLibertyMessage(classesType + " Injection successful...", 8, "less");
    }

    @Test
    public void testPerRequestInjectSetInResource() throws IOException {
        runGetMethod("/rest/helloworldc/person", 200, classesType + " Resource Inject: I am a Student.", true);
//        assertLibertyMessage(classesType + " Injection successful...", 8, "less");
    }

    @Test
    public void testPerRequestInjectSetInResourceInChild() throws IOException {
        runGetMethod("/rest/injectionInChild/person", 200, classesType + " Resource Inject: I am a Student.", true);
//        assertLibertyMessage(classesType + " Injection successful...", 8, "less");
    }

    @Test
    public void testPerRequestContextInApplication() throws IOException {
        runGetMethod("/rest/helloworlds", 200, singletonsType + " Resource: Hello World", true);
        assertLibertyMessage("@Context in getClasses Application: true", 1, "equal");
    }

    @Test
    public void testPerRequestInjectInApplication() throws IOException {
        runGetMethod("/rest/helloworlds", 200, singletonsType + " Resource: Hello World", true);
        assertLibertyMessage("@Inject in getClasses Application: true", 1, "equal");
    }

    @Test
    public void testSingletonHelloworld() throws IOException {
        runGetMethod("/rest/helloworlds", 200, singletonsType + " Resource: Hello World", true);
        assertLibertyMessage(singletonsType + " Injection successful...", 1, "equal");
    }

    @Test
    public void testSingletonContextInResource() throws IOException {
        runGetMethod("/rest/helloworlds/uriinfo", 200, singletonsType + " Resource Context: helloworlds", true);
        assertLibertyMessage(singletonsType + " Injection successful...", 1, "equal");
    }

    @Test
    public void testSingletonInjectInResource() throws IOException {
        runGetMethod("/rest/helloworlds/simplebean", 200, singletonsType + " Resource Inject: Hello from SimpleBean", true);
        assertLibertyMessage(singletonsType + " Injection successful...", 1, "equal");
    }

    @Test
    public void testSingletonInjectSetInResource() throws IOException {
        runGetMethod("/rest/helloworlds/person", 200, singletonsType + " Resource Inject: I am a Student.", true);
        assertLibertyMessage(singletonsType + " Injection successful...", 1, "equal");
    }

    // @Test
    //todo: need add back when provider context injection is available
    public void testSingletonContextInProvider() throws IOException {
        runGetMethod("/rest/helloworlds/provider/jordan", 200, "", true);
        assertLibertyMessage(singletonsType + " Injection successful...", 1, "equal");
        assertLibertyMessage("Provider Context uriinfo: helloworlds/provider/jordan", 0, "more");
    }

    @Test
    public void testSingletonInjectInProvider() throws IOException {
        runGetMethod("/rest/helloworlds/provider/jordan", 200, "", true);
        assertLibertyMessage(singletonsType + " Injection successful...", 1, "equal");
        assertLibertyMessage("Filter Injection successful...", 1, "equal");
        assertLibertyMessage("Provider Inject simplebean: Hello from SimpleBean", 0, "more");
    }

    // @Test
    //todo: need add back when provider context injection is available
    public void testSingletonContextInjectInFilter() throws IOException {
        runGetMethod("/rest/helloworlds", 200, "", true);
        assertLibertyMessage("RequestFilter Context uriinfo: helloworlds", 0, "more");
        assertLibertyMessage("RequestFilter Inject person: I am a Student.", 0, "more");
    }

    @Test
    public void testSingletonContextInApplication() throws IOException {
        runGetMethod("/rest/helloworld2/contextfromapp", 200, singletonsType + "2 Resource Application Context: helloworld2/contextfromapp", true);
    }

    @Test
    public void testSingletonInjectInApplication() throws IOException {
        runGetMethod("/rest/helloworld2/personfromapp", 200, singletonsType + "2 Resource Application Inject: I am a Student.", true);
        assertLibertyMessage("Application Injection successful...", 1, "equal");
    }

    @Test
    public void testAlternativeIsNotInCDIBeans() throws IOException {
        String result = runGetMethod("/rest/helloworldc/cdibeans", 200, "", false).toString();
        System.out.println("testAlternativeIsNotInCDIBeans Result: " + result);
        assertTrue(!result.contains("Teacher"));
    }

    @Test
    public void testSingletonConstrutorWithParamter() throws IOException {
        String result = runGetMethod("/rest/helloworld2/cdibeans", 200, "", false).toString();
//        System.out.println("testSingletonConstrutorWithParamter Result: " + result);
        assertTrue(!result.contains("HelloWorldResource2"));
    }

    @Test
    public void testSingletonConstrutorInjection() throws IOException {
        runGetMethod("/rest/helloworld2/simplebean", 200, singletonsType + "2 Resource Inject: simpleBean is null", true);
    }

    @Test
    public void testInjectionInConstructor_1() throws IOException {
        String result = runGetMethod("/rest/helloworldt1?type=test", 200, "", false).toString();
        System.out.println("testInjectionInConstructor_1 Result: " + result);
        assertTrue(result.contains("test"));
    }

    @Test
    public void testInjectionInConstructor_2() throws IOException {
        String result = runGetMethod("/rest/helloworldt2", 200, "", false).toString();
        System.out.println("testInjectionInConstructor_2 Result: " + result);
        assertTrue(result.contains("helloworldt2"));
    }

    @Test
    public void testInjectionInConstructor_3() throws IOException {
        String result = runGetMethod("/rest/helloworldt3", 200, "", false).toString();
        System.out.println("testInjectionInConstructor_3 Result: " + result);
        assertTrue(result.contains("helloworldt3"));
    }

    // @Test
    public void testServletResourceAreSameInstance() throws IOException {
        runGetMethod("/rest/helloworld", 200, "Hello from SimpleBean=Hello from SimpleBean", true);
        String result = runGetMethod("/rest/helloworldc/cdibeans", 200, "", false).toString();

        String str = "bean.getBeanClass(): class com.ibm.ws.jaxrs20.cdi.fat.basic.SimpleBean";
        assertTrue(result.indexOf(str) == result.lastIndexOf(str));
    }


}