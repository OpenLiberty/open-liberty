/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.concurrency;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.jakarta.concurrency.ejb.error.AsynchInterfaceBean;
import test.jakarta.concurrency.ejb.error.AsynchInterfaceLocal;
import test.jakarta.concurrency.ejb.error.GenericAsynchBean;
import test.jakarta.concurrency.ejb.error.GenericLocal;
import test.jakarta.concurrency.web.error.ConcurrencyBeanErrorServlet;
import test.jakarta.concurrency.web.error.ConcurrentInterfaceWarningServlet;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class ConcurrencyErrorTest extends FATServletClient {

    public static final String APP_NAME_1 = "AppInterfaceWarning";
    public static final String APP_NAME_2 = "AppBeanError";

    public static final String WAR_NAME_1 = "WebInterfaceWarning";
    public static final String WAR_NAME_2 = "WebBeanError";

    @Server("com.ibm.ws.concurrent.fat.jakarta.error")
    public static LibertyServer server;

    private static EnterpriseArchive AppInterfaceWarning;
    private static EnterpriseArchive AppBeanError;

    @BeforeClass
    public static void setUp() throws Exception {
        //Web archive for interface warning
        WebArchive WebInterfaceWarning = ShrinkHelper.buildDefaultApp("WebInterfaceWarning", "test.jakarta.concurrency.web.error")
                        .deleteClass(ConcurrencyBeanErrorServlet.class);

        //Web archive for bean error
        WebArchive WebBeanError = ShrinkHelper.buildDefaultApp("WebBeanError", "test.jakarta.concurrency.web.error")
                        .deleteClass(ConcurrentInterfaceWarningServlet.class);

        //EJB where asynch methods are on the interface
        JavaArchive EJBInterfaceWarning = ShrinkHelper.buildJavaArchive("EJBInterfaceWarning", "test.jakarta.concurrency.ejb.error")
                        .deleteClasses(GenericAsynchBean.class, GenericLocal.class);

        //EJB where asynch methods are on the bean
        JavaArchive EJBBeanError = ShrinkHelper.buildJavaArchive("EJBBeanError", "test.jakarta.concurrency.ejb.error")
                        .deleteClasses(AsynchInterfaceBean.class, AsynchInterfaceLocal.class);

        AppInterfaceWarning = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME_1 + ".ear");
        AppInterfaceWarning.addAsModules(WebInterfaceWarning, EJBInterfaceWarning);

        AppBeanError = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME_2 + ".ear");
        AppBeanError.addAsModules(WebBeanError, EJBBeanError);

        server.startServer();

        server.setTraceMarkToEndOfDefaultTrace();
        ShrinkHelper.exportDropinAppToServer(server, AppInterfaceWarning);

        //Attempt to use application
        runTest(server, WAR_NAME_1, "testMethod");

        //App checker should log a warning, not an error.
        assertNull(server.waitForStringInTraceUsingMark("CNTR9426E"));
        assertNotNull(server.waitForStringInTraceUsingMark("CNTR9427W"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer("CNTR9427W", //Warning Jakarta Concurrent Asynchronous on EJB Interface method
                              "CNTR9426E", //Error Jakarta Concurrent Asynchronous on EJB method
                              "CNTR0020E" //Generic EJB threw an exception
            );
        }
    }

    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.EJBConfigurationException", //Root exception thrown by EJB Container
                    "jakarta.ejb.EJBException", //caused by EJBConfigurationException and thrown to injectionengine
                    "com.ibm.wsspi.injectionengine.InjectionException", //caused by EJBException and thrown to Servlet
                    "jakarta.servlet.UnavailableException", //caused by InjectionException
    })
    public void testBeanApplicationFailsToInstall() throws Exception {
        server.setTraceMarkToEndOfDefaultTrace();
        ShrinkHelper.exportDropinAppToServer(server, AppBeanError);

        //Attempt to use application
        try {
            runTest(server, WAR_NAME_2, "testMethod");
            fail("Should not have been able to connect to servlet.");
        } catch (AssertionError e) {
            //Expected
        }

        //MethodAttribUtils should have thrown and error, not a warning.
        assertNotNull(server.waitForStringInTraceUsingMark("CNTR9426E"));
        assertNull(server.waitForStringInTraceUsingMark("CNTR9427W"));
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

}
