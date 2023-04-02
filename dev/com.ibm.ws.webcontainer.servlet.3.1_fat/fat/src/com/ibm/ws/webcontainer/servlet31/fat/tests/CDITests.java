/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.servlet31.fat.tests;

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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class CDITests {

    private static final Logger LOG = Logger.getLogger(CDITests.class.getName());

    @Server("servlet31_cdiServer")
    public static LibertyServer server;

    private static final String CDI12_TEST_JAR_NAME = "CDI12Test";
    private static final String CDI12_TEST_APP_NAME = "CDI12Test";
    private static final String CDI12_TEST_EE10_APP_NAME = "CDI12TestEE10";

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
        /*
         * In CDI 4.0 (EE10) an empty beans.xml is treated as bean-discovery-mode="annotated"
         * in previous CDI features it was treated as bean-discovery-mode="all".
         *
         * Adding the cdi emptyBeansXmlCDI3Compatibility configuration to the server.xml used for this test will give us
         * the pre CDI 4.0 behavior. The configuration will be ignored in CDI features before CDI 4.0.
         *
         * We need to use the CDI12TestEE10.war during the EE10 repeat action because it contains an actual empty beans.xml.
         *
         * The existing test application contains a 1.0 beans.xml with no version set.
         *
         * The cdi emptyBeansXmlCDI3Compatibility configuration will only work for a completely empty beans.xml.
         *
         * Previous CDI specifications stated that a beans.xml with no version would have bean-discovery-mode="all".
         */
        if (JakartaEE10Action.isActive()) {
            cdi12TestApp = (WebArchive) ShrinkHelper.addDirectory(cdi12TestApp, "test-applications/" + CDI12_TEST_EE10_APP_NAME + ".war/resources");
        } else {
            cdi12TestApp = (WebArchive) ShrinkHelper.addDirectory(cdi12TestApp, "test-applications/" + CDI12_TEST_APP_NAME + ".war/resources");
        }
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
    public void testServletInjection() throws Exception {
        String[] expected = { "Test Exit", "ConstructorBean::Servlet", "MethodBean::Servlet", "ServletFieldBean", "ProducerInjected::Servlet", "postConstructCalled::Servlet" };

        verifyStringsInResponse("/CDI12Test", "/CDIServletInjected", expected);
    }

    @Test
    public void testServletIntercepor() throws Exception {
        String[] expected = { "Test Passed! InterceptedBean : ServiceMethodInterceptor was called.",
                              "Test Passed! CDIServletIntercepted : SendResponseInterceptor was called.",
                              "Test Passed! InterceptedBean : ServiceMethodInterceptor was called." };

        verifyStringsInResponse("/CDI12Test", "/CDIServletIntercepted", expected);
    }

    @Test
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
