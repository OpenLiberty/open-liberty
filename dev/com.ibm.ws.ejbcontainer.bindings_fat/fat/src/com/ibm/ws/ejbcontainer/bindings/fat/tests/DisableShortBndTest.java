/*******************************************************************************
 * Copyright (c)  2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.fat.tests;

import java.util.HashSet;

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
import com.ibm.websphere.simplicity.config.EJBContainerElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ejbcontainer.bindings.configtests.otherweb.DisableShortBndOtherServlet;
import com.ibm.ws.ejbcontainer.bindings.configtests.web.DisableShortBndServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class DisableShortBndTest extends FATServletClient {

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

    private static final Class<?> c = DisableShortBndTest.class;
    private static HashSet<String> apps = new HashSet<String>();
    private static String servlet = "ConfigTestsWeb/DisableShortBndServlet";
    private static String servletOther = "ConfigTestsOtherWeb/DisableShortBndOtherServlet";

    @Server("com.ibm.ws.ejbcontainer.bindings.fat.server")
    @TestServlets({ @TestServlet(servlet = DisableShortBndServlet.class, contextRoot = "ConfigTestsWeb"),
                    @TestServlet(servlet = DisableShortBndOtherServlet.class, contextRoot = "ConfigTestsOtherWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server"));

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        apps.add("ConfigTestsTestApp");
        apps.add("ConfigTestsOtherTestApp");

        // Use ShrinkHelper to build the ears

        // -------------- ConfigTestsTestApp ------------
        JavaArchive ConfigTestsEJB = ShrinkHelper.buildJavaArchive("ConfigTestsEJB.jar", "com.ibm.ws.ejbcontainer.bindings.configtests.ejb.");

        WebArchive ConfigTestsWeb = ShrinkHelper.buildDefaultApp("ConfigTestsWeb.war", "com.ibm.ws.ejbcontainer.bindings.configtests.web.");

        EnterpriseArchive ConfigTestsTestApp = ShrinkWrap.create(EnterpriseArchive.class, "ConfigTestsTestApp.ear");
        ConfigTestsTestApp.addAsModules(ConfigTestsEJB, ConfigTestsWeb);
        ShrinkHelper.addDirectory(ConfigTestsTestApp, "test-applications/ConfigTestsTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, ConfigTestsTestApp);

        // -------------- ConfigTestsOtherTestApp ------------
        JavaArchive ConfigTestsOtherEJB = ShrinkHelper.buildJavaArchive("ConfigTestsOtherEJB.jar", "com.ibm.ws.ejbcontainer.bindings.configtests.otherejb.");

        WebArchive ConfigTestsOtherWeb = ShrinkHelper.buildDefaultApp("ConfigTestsOtherWeb.war", "com.ibm.ws.ejbcontainer.bindings.configtests.otherweb.");

        EnterpriseArchive ConfigTestsOtherTestApp = ShrinkWrap.create(EnterpriseArchive.class, "ConfigTestsOtherTestApp.ear");
        ConfigTestsOtherTestApp.addAsModules(ConfigTestsOtherEJB, ConfigTestsOtherWeb);
        ShrinkHelper.addDirectory(ConfigTestsOtherTestApp, "test-applications/ConfigTestsOtherTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, ConfigTestsOtherTestApp);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    private void updateConfigElement(String disableShortDefaultBindings) throws Exception {
        String m = "updateConfigElement";

        ServerConfiguration config = server.getServerConfiguration();
        EJBContainerElement ejbElement = config.getEJBContainer();
        if (ejbElement.getDisableShortDefaultBindings() != disableShortDefaultBindings) {
            Log.info(c, m, "adding disableShortDefaultBindings =" + disableShortDefaultBindings + "to server config");

            ejbElement.setDisableShortDefaultBindings(disableShortDefaultBindings);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(apps);
            for (String app : apps) {
                server.restartDropinsApplication(app + ".ear");
            }
        } else {
            Log.info(c, m, "config element already set to desired value");
        }

    }

    @Test
    public void testAllShortBindingsDisabled() throws Exception {
        updateConfigElement("*");

        FATServletClient.runTest(server, servletOther, "testAllShortBindingsDisabled");
        FATServletClient.runTest(server, servlet, "testAllShortBindingsDisabled");
    }

    @Test
    public void testOtherAppShortBindingsDisabled() throws Exception {
        updateConfigElement("ConfigTestsOtherTestApp");

        FATServletClient.runTest(server, servletOther, "testOtherAppShortBindingsDisabled");
        FATServletClient.runTest(server, servlet, "testOtherAppShortBindingsDisabled");
    }

    @Test
    public void testAppShortBindingsDisabled() throws Exception {
        updateConfigElement("ConfigTestsTestApp");

        FATServletClient.runTest(server, servletOther, "testAppShortBindingsDisabled");
        FATServletClient.runTest(server, servlet, "testAppShortBindingsDisabled");
    }

    @Test
    public void testBothAppShortBindingsDisabled() throws Exception {
        updateConfigElement("ConfigTestsTestApp:ConfigTestsOtherTestApp");

        FATServletClient.runTest(server, servletOther, "testBothAppShortBindingsDisabled");
        FATServletClient.runTest(server, servlet, "testBothAppShortBindingsDisabled");
    }

    @Test
    public void testNoShortBindingsDisabled() throws Exception {
        updateConfigElement(null);

        FATServletClient.runTest(server, servletOther, "testNoShortBindingsDisabled");
        FATServletClient.runTest(server, servlet, "testNoShortBindingsDisabled");
    }
}
