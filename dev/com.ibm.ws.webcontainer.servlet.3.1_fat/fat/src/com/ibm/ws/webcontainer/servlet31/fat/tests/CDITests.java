/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;

import java.util.logging.Logger;
import java.util.Set;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

//Temporarily skipped for EE9 jakarta until cdi-3.0 feature is developed
@RunWith(FATRunner.class)
@SkipForRepeat(EE9_FEATURES)
public class CDITests extends LoggingTest {
    
    private static final Logger LOG = Logger.getLogger(CDITests.class.getName());

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_cdiServer");

    private static final String CDI12_TEST_JAR_NAME = "CDI12Test";
    private static final String CDI12_TEST_APP_NAME = "CDI12Test";

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the CDI12TestV2 jar to add to the war app as a lib
        JavaArchive CDI12TestJar = ShrinkHelper.buildJavaArchive(CDI12_TEST_JAR_NAME + ".jar",
                                                                 "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.jar.cdi.beans");
        CDI12TestJar = (JavaArchive) ShrinkHelper.addDirectory(CDI12TestJar, "test-applications/CDI12Test.jar/resources");
        // Build the war app CDI12Test.war and add the dependencies
        WebArchive CDI12TestApp = ShrinkHelper.buildDefaultApp(CDI12_TEST_APP_NAME + ".war",
                                                               "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.beans",
                                                               "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.cdi.interceptors",
                                                               "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.interfaces",
                                                               "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.listeners",
                                                               "com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.servlets");
        CDI12TestApp = (WebArchive) ShrinkHelper.addDirectory(CDI12TestApp, "test-applications/CDI12Test.war/resources");
        CDI12TestApp = CDI12TestApp.addAsLibrary(CDI12TestJar);
        // Verify if the apps are in the server before trying to deploy them
        if (SHARED_SERVER.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(CDI12_TEST_APP_NAME);
            LOG.info("addAppToServer : " + CDI12_TEST_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
              ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), CDI12TestApp);
          }
        SHARED_SERVER.startIfNotStarted();
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + CDI12_TEST_APP_NAME);
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (SHARED_SERVER.getLibertyServer() != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer(null);
        }
    }

    @Test
    @Mode(TestMode.LITE)
    public void testServletInjection() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
        String[] expected = { "Test Exit", "ConstructorBean::Servlet", "MethodBean::Servlet", "ServletFieldBean", "ProducerInjected::Servlet", "postConstructCalled::Servlet" };
        String[] unexpected = { "Test Exception" };
        verifyResponse(wb, "/CDI12Test/CDIServletInjected", expected, unexpected);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testServletIntercepor() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
        String[] expected = { "Test Passed! InterceptedBean : ServiceMethodInterceptor was called.",
                              "Test Passed! CDIServletIntercepted : SendResponseInterceptor was called.",
                              "Test Passed! InterceptedBean : ServiceMethodInterceptor was called." };
        String[] unexpected = { "Test Failed!" };
        verifyResponse(wb, "/CDI12Test/CDIServletIntercepted", expected, unexpected);
    }

    @Test
    @Mode(TestMode.LITE)
    public void testAsyncListeenerCDI() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();
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
        String[] unexpected = { "Test Exception" };
        verifyResponse(wb, "/CDI12Test/CDIAsyncServlet", expected, unexpected);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }
}
