/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.configload.AppRetryCountingBean;
import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.configload.CountingException;
import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.configload.RequestRetryCountingBean;
import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.configload.RetryTesterServlet;
import com.ibm.websphere.microprofile.faulttolerance_fat.multimodule.tests.configload.WarRetryCountingBean;
import com.ibm.websphere.microprofile.faulttolerance_fat.suite.FATSuite;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;

/**
 * Test configuration works across multiple modules in an EAR
 * <p>
 * This can be problematic, as the config visible to the class depends on where in the EAR it's located.
 * <p>
 * Note that normally config is retrieved based on the thread context classloader. However, when we load the config for FaultTolerance annotations, we use the annotated class's
 * classloader instead. This avoids a situation where one bean in an application library jar would need to have a different config depending on which module called it.
 */
public class TestMultiModuleConfigLoad extends LoggingTest {

    // Field is required to declare the shared server as a class rule
    @ClassRule
    public static SharedServer SHARED_SERVER = FATSuite.MULTI_MODULE_SERVER;

    @BeforeClass
    public static void appSetup() throws Exception {
        // Use Shrinkwrap to build the .ear to avoid tons of duplication of packaging and to keep all the test resources together
        // However this does mean you need to keep an eye on which classes go into each archive

        WebArchive war1 = ShrinkWrap.create(WebArchive.class, "war1.war")
                        .addClass(RetryTesterServlet.class)
                        .addClass(WarRetryCountingBean.class);

        WebArchive war2 = ShrinkWrap.create(WebArchive.class, "war2.war")
                        .addClass(RetryTesterServlet.class)
                        .addClass(WarRetryCountingBean.class)
                        // Sets Retry/maxRetries = 1
                        .addAsManifestResource(RetryTesterServlet.class.getResource("war2-config.properties"), "microprofile-config.properties");

        WebArchive war3 = ShrinkWrap.create(WebArchive.class, "war3.war")
                        .addClass(RetryTesterServlet.class)
                        .addClass(WarRetryCountingBean.class)
                        // Sets MP_Fault_Tolerance_NonFallback_Enabled=false
                        .addAsManifestResource(RetryTesterServlet.class.getResource("war3-config.properties"), "microprofile-config.properties");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jar1.jar")
                        .addClass(AppRetryCountingBean.class)
                        .addClass(RequestRetryCountingBean.class)
                        .addClass(CountingException.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "FaultToleranceMultiModule.ear")
                        .addAsModule(war1)
                        .addAsModule(war2)
                        .addAsModule(war3)
                        .addAsLibrary(jar)
                        .setApplicationXML(RetryTesterServlet.class.getResource("multi-module-application.xml"));

        ShrinkHelper.exportToServer(SHARED_SERVER.getLibertyServer(), "dropins", ear);
        SHARED_SERVER.getLibertyServer().addInstalledAppForValidation("FaultToleranceMultiModule");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        SHARED_SERVER.getLibertyServer().deleteFileFromLibertyServerRoot("dropins/FaultToleranceMultiModule.ear");
        SHARED_SERVER.getLibertyServer().removeInstalledAppForValidation("FaultToleranceMultiModule");
    }

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    /**
     * Test config of an App scoped bean in an application library
     * <p>
     * The config should be based on the app classloader and should not vary by which .war called it
     */
    @Test
    public void testRetryCountAppScope() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/war1/retry-tester?scope=app", "4");
        getSharedServer().verifyResponse(browser, "/war2/retry-tester?scope=app", "4");
        getSharedServer().verifyResponse(browser, "/war3/retry-tester?scope=app", "4");
    }

    /**
     * Test config of a Request scoped bean in an application library
     * <p>
     * The config should be based on the app classloader and should not vary by which .war called it
     * <p>
     * In contrast to the previous test, a new instance should be created and configured on each request.
     */
    @Test
    public void testRetryCountRequestScope() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/war1/retry-tester?scope=request", "4");
        getSharedServer().verifyResponse(browser, "/war2/retry-tester?scope=request", "4");
        getSharedServer().verifyResponse(browser, "/war3/retry-tester?scope=request", "4");
    }

    /**
     * Test config of an Application scoped bean in a .war
     * <p>
     * The config should be based on the war config, so it should be different for each .war
     * <p>
     * In addition, war3.war disables the annotation entirely.
     */
    @Test
    public void testRetryCountWarConfigWarScope() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        getSharedServer().verifyResponse(browser, "/war1/retry-tester?scope=war", "4");
        getSharedServer().verifyResponse(browser, "/war2/retry-tester?scope=war", "2");
        getSharedServer().verifyResponse(browser, "/war3/retry-tester?scope=war", "1");
    }

}
