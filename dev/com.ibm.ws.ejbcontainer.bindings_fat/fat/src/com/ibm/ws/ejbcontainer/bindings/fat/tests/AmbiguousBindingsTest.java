/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.repeataction.RepeatOnError;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.repeataction.RepeatOnError.EJBONERROR;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class AmbiguousBindingsTest extends FATServletClient {

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            try {
                System.runFinalization();
                System.gc();
                server.serverDump("heap");
            } catch (Exception e1) {
                System.out.println("Failed to dump server");
                e1.printStackTrace();
            }
        }
    };

    @Server("com.ibm.ws.ejbcontainer.bindings.fat.server.err")
    public static LibertyServer server;

    private static String servlet = "AmbiguousWeb/AmbiguousTestServlet";

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new RepeatOnError(EJBONERROR.WARN, "com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnError(EJBONERROR.FAIL, "com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnError(EJBONERROR.IGNORE, "com.ibm.ws.ejbcontainer.bindings.fat.server.err"));

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the ears
        JavaArchive AmbiguousEJB = ShrinkHelper.buildJavaArchive("AmbiguousEJB.jar", "com.ibm.ambiguous.ejb.");
        ShrinkHelper.addDirectory(AmbiguousEJB, "test-applications/AmbiguousEJB.jar/resources");

        WebArchive AmbiguousWeb = ShrinkHelper.buildDefaultApp("AmbiguousWeb.war", "com.ibm.ambiguous.web.");

        EnterpriseArchive AmbiguousTestApp = ShrinkWrap.create(EnterpriseArchive.class, "AmbiguousTestApp.ear");
        AmbiguousTestApp.addAsModules(AmbiguousEJB, AmbiguousWeb);
        ShrinkHelper.addDirectory(AmbiguousTestApp, "test-applications/AmbiguousTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, AmbiguousTestApp);

        if (RepeatTestFilter.CURRENT_REPEAT_ACTION == "EJBCBOnErr_FAIL") {
            // don't validate apps loaded like default startServer() does
            server.startServerAndValidate(true, true, false);
        } else {
            server.startServer();
        }

    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0338W", "CNTR4002E", "CWWKZ0106E", "CWWKZ0002E");
        }
    }

    @Test
    @AllowedFFDC("javax.naming.NamingException")
    public void testAmbiguousBindings() throws Exception {

        if (RepeatTestFilter.CURRENT_REPEAT_ACTION == "EJBCBOnErr_FAIL") {
            // make sure application stopped with correct error
            String message = "CWWKZ0106E:";
            assertNotNull("Application AmbiguousTestApp should have been stopped", server.waitForStringInLog(message));
            message = "CNTR4002E:.*com.ibm.ambiguous.ejb.AmbiguousOtherNameRemoteHome";
            assertNotNull("Application AmbiguousTestApp did not get correct error", server.waitForStringInLog(message));
        } else if (RepeatTestFilter.CURRENT_REPEAT_ACTION == "EJBCBOnErr_IGNORE") {
            // make sure warning is not there
            String message = "CNTR0338W:";
            assertTrue("Application AmbiguousTestApp should not have got ambiguous warning", server.findStringsInLogs(message).isEmpty());
            FATServletClient.runTest(server, servlet, "testAmbiguousOnErrorIgnore");
        } else {
            // RepeatTestFilter.CURRENT_REPEAT_ACTION == EJBCBOnErr_WARN

            // make sure warning is there
            String message = "CNTR0338W:";
            assertNotNull("Application AmbiguousTestApp did not get ambiguous warning", server.waitForStringInLog(message));
            FATServletClient.runTest(server, servlet, "testAmbiguousOnErrorWarn");
        }
    }
}
