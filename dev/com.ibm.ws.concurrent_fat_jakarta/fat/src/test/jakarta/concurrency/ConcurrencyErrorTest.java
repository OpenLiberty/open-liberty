/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.jakarta.concurrency;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.concurrency.ejb.error.AsynchClassBean;
import test.jakarta.concurrency.ejb.error.AsynchInterfaceBean;
import test.jakarta.concurrency.ejb.error.AsynchInterfaceLocal;
import test.jakarta.concurrency.ejb.error.GenericAsynchBean;
import test.jakarta.concurrency.ejb.error.GenericLocal;
import test.jakarta.concurrency.web.error.ConcurrencyBeanErrorServlet;
import test.jakarta.concurrency.web.error.ConcurrencyClassErrorServlet;
import test.jakarta.concurrency.web.error.ConcurrencyInterfaceWarningServlet;
import test.jakarta.concurrency.web.error.config.ResourceDefinitionErrorServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ConcurrencyErrorTest extends FATServletClient {

    public static final String APP_NAME_1 = "AppInterfaceWarning";
    public static final String APP_NAME_2 = "AppBeanError";
    public static final String APP_NAME_3 = "AppClassError";

    public static final String WAR_NAME_1 = "WebInterfaceWarning";
    public static final String WAR_NAME_2 = "WebBeanError";
    public static final String WAR_NAME_3 = "WebClassError";

    @Server("com.ibm.ws.concurrent.fat.jakarta.error")
    @TestServlet(servlet = ResourceDefinitionErrorServlet.class, contextRoot = "WebResourceDefErrors")
    public static LibertyServer server;

    private static EnterpriseArchive AppInterfaceWarning;
    private static EnterpriseArchive AppBeanError;
    private static EnterpriseArchive AppClassError;
    private static WebArchive WebManagedExecutorDefError;
    private static WebArchive WebManagedScheduledExecutorDefError;

    @BeforeClass
    public static void setUp() throws Exception {
        //Web archive with invalid resource definition annotation configurations that fail at run time
        WebArchive WebResourceDefErrors = ShrinkHelper.buildDefaultApp("WebResourceDefErrors",
                                                                       "test.jakarta.concurrency.web.error.config");
        ShrinkHelper.exportAppToServer(server, WebResourceDefErrors);

        //Web archive with invalid ManagedExecutorDefinition
        WebManagedExecutorDefError = ShrinkHelper.buildDefaultApp("WebManagedExecutorDefError",
                                                                  "test.jakarta.concurrency.web.error.executordef");

        //Web archive with invalid ManagedScheduledExecutorDefinition
        WebManagedScheduledExecutorDefError = ShrinkHelper.buildDefaultApp("WebManagedScheduledExecutorDefError",
                                                                           "test.jakarta.concurrency.web.error.scheduledexecutordef");

        //Web archive for interface warning
        WebArchive WebInterfaceWarning = ShrinkHelper.buildDefaultApp("WebInterfaceWarning", "test.jakarta.concurrency.web.error")
                        .deleteClasses(ConcurrencyBeanErrorServlet.class, ConcurrencyClassErrorServlet.class);

        //Web archive for bean error
        WebArchive WebBeanError = ShrinkHelper.buildDefaultApp("WebBeanError", "test.jakarta.concurrency.web.error")
                        .deleteClasses(ConcurrencyInterfaceWarningServlet.class, ConcurrencyClassErrorServlet.class);

        //Web archive for class bean error
        WebArchive WebClassError = ShrinkHelper.buildDefaultApp("WebClassError", "test.jakarta.concurrency.web.error")
                        .deleteClasses(ConcurrencyInterfaceWarningServlet.class, ConcurrencyBeanErrorServlet.class);

        //EJB where asynch methods are on the interface
        JavaArchive EJBInterfaceWarning = ShrinkHelper.buildJavaArchive("EJBInterfaceWarning", "test.jakarta.concurrency.ejb.error")
                        .deleteClasses(GenericAsynchBean.class, GenericLocal.class, AsynchClassBean.class);

        //EJB where asynch methods are on the bean
        JavaArchive EJBBeanError = ShrinkHelper.buildJavaArchive("EJBBeanError", "test.jakarta.concurrency.ejb.error")
                        .deleteClasses(AsynchInterfaceBean.class, AsynchInterfaceLocal.class, AsynchClassBean.class);

        //EJB where asynch methods are on the class
        JavaArchive EJBClassError = ShrinkHelper.buildJavaArchive("EJBClassError", "test.jakarta.concurrency.ejb.error")
                        .deleteClasses(AsynchInterfaceBean.class, AsynchInterfaceLocal.class, GenericAsynchBean.class);

        AppInterfaceWarning = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME_1 + ".ear");
        AppInterfaceWarning.addAsModules(WebInterfaceWarning, EJBInterfaceWarning);

        AppBeanError = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME_2 + ".ear");
        AppBeanError.addAsModules(WebBeanError, EJBBeanError);

        AppClassError = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME_3 + ".ear");
        AppClassError.addAsModules(WebClassError, EJBClassError);

        server.startServer();

        server.setTraceMarkToEndOfDefaultTrace();
        ShrinkHelper.exportDropinAppToServer(server, AppInterfaceWarning);

        //Attempt to use application
        runTest(server, WAR_NAME_1, "testMethod");

        //App checker should log a warning, not an error.
        assertNull(server.waitForStringInTraceUsingMark("CNTR0342E"));
        assertNotNull(server.waitForStringInTraceUsingMark("CNTR0343W"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CNTR0343W", //Warning Jakarta Concurrent Asynchronous on EJB Interface method
                              "CNTR0342E", //Error Jakarta Concurrent Asynchronous on EJB method
                              "CNTR0020E", //Generic EJB threw an exception
                              "CWNEN0011E" //Resource definition config errors during application startup
            );
        }
    }

    @Test
    @Mode(FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", //Root exception thrown by EJB Container
                    "jakarta.ejb.EJBException", //caused by EJBConfigurationException and thrown to injectionengine
                    "com.ibm.wsspi.injectionengine.InjectionException", //caused by EJBException and thrown to Servlet
                    "jakarta.servlet.UnavailableException", //caused by InjectionException
    })
    public void testBeanApplicationFailsToInstall() throws Exception {
        server.setTraceMarkToEndOfDefaultTrace();
        ShrinkHelper.exportDropinAppToServer(server, AppBeanError);

        boolean testRan = false;

        //Attempt to use application
        try {
            runTest(server, WAR_NAME_2, "testMethod");
            testRan = true;
        } catch (AssertionError e) {
            //Expected
        }

        assertFalse("Test application should not have been installed and allowed to run.", testRan);

        //MethodAttribUtils should have thrown and error, not a warning.
        assertNotNull(server.waitForStringInTraceUsingMark("CNTR0342E"));
        assertNull(server.waitForStringInTraceUsingMark("CNTR0343W"));
    }

    @Test
    @Mode(FULL)
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", //Root exception thrown by EJB Container
                    "jakarta.ejb.EJBException", //caused by EJBConfigurationException and thrown to injectionengine
                    "com.ibm.wsspi.injectionengine.InjectionException", //caused by EJBException and thrown to Servlet
                    "jakarta.servlet.UnavailableException", //caused by InjectionException
    })
    public void testClassApplicationFailsToInstall() throws Exception {
        server.setTraceMarkToEndOfDefaultTrace();
        ShrinkHelper.exportDropinAppToServer(server, AppClassError);

        boolean testRan = false;

        //Attempt to use application
        try {
            runTest(server, WAR_NAME_3, "testMethod");
            testRan = true;
        } catch (AssertionError e) {
            //Expected
        }

        assertFalse("Test application should not have been installed and allowed to run.", testRan);

        //MethodAttribUtils should have thrown and error, not a warning.
        assertNotNull(server.waitForStringInTraceUsingMark("CNTR0342E"));
        assertNull(server.waitForStringInTraceUsingMark("CNTR0343W"));
    }

    @Test
    public void getThreadName() throws Exception {
        runTest(server, WAR_NAME_1, testName);
    }

    @Test
    public void getThreadNameNonAsyc() throws Exception {
        runTest(server, WAR_NAME_1, testName);
    }

    @Test
    @ExpectedFFDC("java.lang.IllegalStateException")
    public void getState() throws Exception {
        runTest(server, WAR_NAME_1, testName);
    }

    @Test
    @ExpectedFFDC("java.lang.IllegalStateException")
    public void getStateFromService() throws Exception {
        runTest(server, WAR_NAME_1, testName);
    }

    @AllowedFFDC("com.ibm.wsspi.injectionengine.InjectionException")
    @Mode(FULL)
    @Test
    public void testMaxAsyncNegativeAppFailsToInstall() throws Exception {
        server.setMarkToEndOfLog();
        ShrinkHelper.exportDropinAppToServer(server, WebManagedExecutorDefError, DeployOptions.DISABLE_VALIDATION);

        assertNotNull(server.waitForStringInLogUsingMark("CWNEN0011E.*maxAsync=-10"));
        assertNotNull(server.waitForStringInLogUsingMark("CWWKT0017I.*WebManagedExecutorDefError")); // app removed
    }

    @AllowedFFDC("com.ibm.wsspi.injectionengine.InjectionException")
    @Mode(FULL)
    @Test
    public void testMaxAsyncZeroAppFailsToInstall() throws Exception {
        server.setMarkToEndOfLog();
        ShrinkHelper.exportDropinAppToServer(server, WebManagedScheduledExecutorDefError, DeployOptions.DISABLE_VALIDATION);

        assertNotNull(server.waitForStringInLogUsingMark("CWNEN0011E.*maxAsync=0"));
        assertNotNull(server.waitForStringInLogUsingMark("CWWKT0017I.*WebManagedScheduledExecutorDefError")); // app removed
    }
}
