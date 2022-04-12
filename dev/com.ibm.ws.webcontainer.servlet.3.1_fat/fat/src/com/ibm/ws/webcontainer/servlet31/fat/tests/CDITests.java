/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

// TODO: https://github.com/OpenLiberty/open-liberty/issues/20641
@SkipForRepeat(EE10_FEATURES)
@RunWith(FATRunner.class)
public class CDITests {

    private static final Logger LOG = Logger.getLogger(CDITests.class.getName());

    @Server("servlet31_cdiServer")
    public static LibertyServer server;

    private static final String CDI12_TEST_JAR_NAME = "CDI12Test";
    private static final String CDI12_TEST_APP_NAME = "CDI12Test";

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the CDI12TestV2 jar to add to the war app as a lib
        JavaArchive cdi12TestJar = ShrinkHelper.buildJavaArchive(CDI12_TEST_JAR_NAME + ".jar",
                                                                 "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans");
        cdi12TestJar = (JavaArchive) ShrinkHelper.addDirectory(cdi12TestJar, "test-applications/CDI12Test.jar/resources");
        // Build the war app CDI12Test.war and add the dependencies
        WebArchive cdi12TestApp = ShrinkHelper.buildDefaultApp(CDI12_TEST_APP_NAME + ".war",
                                                               "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.beans",
                                                               "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.cdi.interceptors",
                                                               "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.interfaces",
                                                               "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.listeners",
                                                               "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.servlets");
        cdi12TestApp = (WebArchive) ShrinkHelper.addDirectory(cdi12TestApp, "test-applications/CDI12Test.war/resources");
        cdi12TestApp = cdi12TestApp.addAsLibrary(cdi12TestJar);

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(server, cdi12TestApp);

        // Start the server and use the class name so we can find logs easily.
        server.startServer(CDITests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    @Mode(TestMode.LITE)
    public void testServletInjection() throws Exception {
        String[] expected = { "Test Exit", "ConstructorBean::Servlet", "MethodBean::Servlet", "ServletFieldBean", "ProducerInjected::Servlet", "postConstructCalled::Servlet" };

        verifyStringsInResponse("/CDI12Test", "/CDIServletInjected", expected);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testServletIntercepor() throws Exception {
        String[] expected = { "Test Passed! InterceptedBean : ServiceMethodInterceptor was called.",
                              "Test Passed! CDIServletIntercepted : SendResponseInterceptor was called.",
                              "Test Passed! InterceptedBean : ServiceMethodInterceptor was called." };

        verifyStringsInResponse("/CDI12Test", "/CDIServletIntercepted", expected);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testAsyncListeenerCDI() throws Exception {
        String[] expected = { "onStartAsync :class com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans.ConstructorBean:",
                              "onStartAsync :class com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans.MethodBean:",
                              "onStartAsync :class com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans.AsyncListenerFieldBean:",
                              "onStartAsync :Interceptor was called:",
                              "onStartAsync ::postConstructCalled:",
                              "onComplete :class com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans.ConstructorBean:",
                              "onComplete :class com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans.MethodBean:",
                              "onComplete :class com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans.AsyncListenerFieldBean:",
                              "onComplete :Interceptor was called:",
                              "onStartAsync ::postConstructCalled:" };

        verifyStringsInResponse("/CDI12Test", "/CDIAsyncServlet", expected);
    }

    private void verifyStringsInResponse(String contextRoot, String path, String[] expectedResponseStrings) throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + contextRoot + path);
        WebResponse response = wc.getResponse(request);
        LOG.info("Response : " + response.getText());

        assertEquals("Expected " + 200 + " status code was not returned!",
                     200, response.getResponseCode());

        String responseText = response.getText();

        for (String expectedResponse : expectedResponseStrings) {
            assertTrue("The response did not contain: " + expectedResponse, responseText.contains(expectedResponse));
        }
    }
}
