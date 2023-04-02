/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.repeataction.EjbOnError;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.repeataction.RepeatOnErrorEE10;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.repeataction.RepeatOnErrorEE7;
import com.ibm.ws.ejbcontainer.bindings.fat.tests.repeataction.RepeatOnErrorEE9;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class AmbiguousBindingsTest extends AbstractTest {

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
    public static RepeatTests r = RepeatTests.with(new RepeatOnErrorEE7(EjbOnError.WARN).forServers("com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnErrorEE7(EjbOnError.FAIL).forServers("com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnErrorEE7(EjbOnError.IGNORE).forServers("com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnErrorEE9(EjbOnError.WARN).fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnErrorEE9(EjbOnError.FAIL).fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnErrorEE9(EjbOnError.IGNORE).fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnErrorEE10(EjbOnError.WARN).fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnErrorEE10(EjbOnError.FAIL).fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server.err")).andWith(new RepeatOnErrorEE10(EjbOnError.IGNORE).fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server.err"));

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();
    }

    private static void addAppsAndStartServer() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive AmbiguousEJB = ShrinkHelper.buildJavaArchive("AmbiguousEJB.jar", "com.ibm.ambiguous.ejb.");
        ShrinkHelper.addDirectory(AmbiguousEJB, "test-applications/AmbiguousEJB.jar/resources");

        WebArchive AmbiguousWeb = ShrinkHelper.buildDefaultApp("AmbiguousWeb.war", "com.ibm.ambiguous.web.");

        EnterpriseArchive AmbiguousTestApp = ShrinkWrap.create(EnterpriseArchive.class, "AmbiguousTestApp.ear");
        AmbiguousTestApp.addAsModules(AmbiguousEJB, AmbiguousWeb);
        ShrinkHelper.addDirectory(AmbiguousTestApp, "test-applications/AmbiguousTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, AmbiguousTestApp, DeployOptions.SERVER_ONLY);

        if (RepeatOnErrorEE7.isActive(EjbOnError.FAIL) || RepeatOnErrorEE9.isActive(EjbOnError.FAIL) || RepeatOnErrorEE10.isActive(EjbOnError.FAIL)) {
            // don't validate apps loaded like default startServer() does
            server.startServerAndValidate(true, true, false);
        } else {
            server.startServer();
        }
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        stopServer(server, "CNTR0338W", "CNTR4002E", "CWWKZ0106E", "CWWKZ0002E");

        // Remove the customBindings.OnError configuration that was added by repeat actions
        if (RepeatOnErrorEE7.isActive()) {
            RepeatOnErrorEE7.cleanup(server);
        } else if (RepeatOnErrorEE9.isActive()) {
            RepeatOnErrorEE9.cleanup(server);
        } else if (RepeatOnErrorEE10.isActive()) {
            RepeatOnErrorEE10.cleanup(server);
        }
    }

    @Test
    @ExpectedFFDC(value = { "javax.naming.NamingException", "com.ibm.ws.container.service.state.StateChangeException" },
                  repeatAction = { "EE7_FEATURES_EjbOnErr_FAIL", "EE8_FEATURES_EjbOnErr_FAIL", "EE9_FEATURES_EjbOnErr_FAIL", "EE10_FEATURES_EjbOnErr_FAIL" })
    public void testAmbiguousBindings() throws Exception {
        addAppsAndStartServer();

        if (RepeatOnErrorEE7.isActive(EjbOnError.FAIL) || RepeatOnErrorEE9.isActive(EjbOnError.FAIL) || RepeatOnErrorEE10.isActive(EjbOnError.FAIL)) {
            // make sure application stopped with correct error
            String message = "CWWKZ0106E:";
            assertNotNull("Application AmbiguousTestApp should have been stopped", server.waitForStringInLog(message));
            message = "CNTR4002E:.*com.ibm.ambiguous.ejb.AmbiguousOtherNameRemoteHome";
            assertNotNull("Application AmbiguousTestApp did not get correct error", server.waitForStringInLog(message));
        } else if (RepeatOnErrorEE7.isActive(EjbOnError.IGNORE) || RepeatOnErrorEE9.isActive(EjbOnError.IGNORE) || RepeatOnErrorEE10.isActive(EjbOnError.IGNORE)) {
            // make sure warning is not there
            String message = "CNTR0338W:";
            assertTrue("Application AmbiguousTestApp should not have got ambiguous warning", server.findStringsInLogs(message).isEmpty());
            FATServletClient.runTest(server, servlet, "testAmbiguousOnErrorIgnore");
        } else {
            // RepeatTestFilter.CURRENT_REPEAT_ACTION == EjbOnError_WARN

            // make sure warning is there
            String message = "CNTR0338W:";
            assertNotNull("Application AmbiguousTestApp did not get ambiguous warning", server.waitForStringInLog(message));
            FATServletClient.runTest(server, servlet, "testAmbiguousOnErrorWarn");
        }
    }
}
