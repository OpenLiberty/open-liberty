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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.EJBContainerElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ejbcontainer.bindings.disableshrt.otherweb.DisableShortBndOtherServlet;
import com.ibm.ws.ejbcontainer.bindings.disableshrt.web.DisableShortBndServlet;

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
    private static final Class<?> c = DisableShortBndTest.class;
    private static HashSet<String> apps = new HashSet<String>();
    private static String servlet = "DisableShrtBndWeb/DisableShortBndServlet";
    private static String servletOther = "DisableShrtBndOtherWeb/DisableShortBndOtherServlet";

    @Server("com.ibm.ws.ejbcontainer.bindings.fat.server")
    @TestServlets({ @TestServlet(servlet = DisableShortBndServlet.class, contextRoot = "DisableShrtBndWeb"),
                    @TestServlet(servlet = DisableShortBndOtherServlet.class, contextRoot = "DisableShrtBndOtherWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server"));

    @BeforeClass
    public static void setUp() throws Exception {
        apps.add("DisableShrtBndTestApp");
        apps.add("DisableShrtBndOtherTestApp");

        // Use ShrinkHelper to build the ears

        // -------------- DisableShrtBndTestApp ------------
        JavaArchive DisableShrtBndEJB = ShrinkHelper.buildJavaArchive("DisableShrtBndEJB.jar", "com.ibm.ws.ejbcontainer.bindings.disableshrt.ejb.");

        WebArchive DisableShrtBndWeb = ShrinkHelper.buildDefaultApp("DisableShrtBndWeb.war", "com.ibm.ws.ejbcontainer.bindings.disableshrt.web.");

        EnterpriseArchive DisableShrtBndTestApp = ShrinkWrap.create(EnterpriseArchive.class, "DisableShrtBndTestApp.ear");
        DisableShrtBndTestApp.addAsModules(DisableShrtBndEJB, DisableShrtBndWeb);
        ShrinkHelper.addDirectory(DisableShrtBndTestApp, "test-applications/DisableShrtBndTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, DisableShrtBndTestApp);

        // -------------- DisableShrtBndOtherTestApp ------------
        JavaArchive DisableShrtBndOtherEJB = ShrinkHelper.buildJavaArchive("DisableShrtBndOtherEJB.jar", "com.ibm.ws.ejbcontainer.bindings.disableshrt.otherejb.");

        WebArchive DisableShrtBndWeb2X = ShrinkHelper.buildDefaultApp("DisableShrtBndOtherWeb.war", "com.ibm.ws.ejbcontainer.bindings.disableshrt.otherweb.");

        EnterpriseArchive DisableShrtBndOtherTestApp = ShrinkWrap.create(EnterpriseArchive.class, "DisableShrtBndOtherTestApp.ear");
        DisableShrtBndOtherTestApp.addAsModules(DisableShrtBndOtherEJB, DisableShrtBndWeb2X);
        ShrinkHelper.addDirectory(DisableShrtBndOtherTestApp, "test-applications/DisableShrtBndOtherTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, DisableShrtBndOtherTestApp);

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
    public void testNoShortBindingsDisabled() throws Exception {
        updateConfigElement(null);

        FATServletClient.runTest(server, servletOther, "testNoShortBindingsDisabled");
        FATServletClient.runTest(server, servlet, "testNoShortBindingsDisabled");
    }

    @Test
    public void testAllShortBindingsDisabled() throws Exception {
        updateConfigElement("*");

        FATServletClient.runTest(server, servletOther, "testAllShortBindingsDisabled");
        FATServletClient.runTest(server, servlet, "testAllShortBindingsDisabled");
    }

    @Test
    public void testOtherAppShortBindingsDisabled() throws Exception {
        updateConfigElement("DisableShrtBndOtherTestApp");

        FATServletClient.runTest(server, servletOther, "testOtherAppShortBindingsDisabled");
        FATServletClient.runTest(server, servlet, "testOtherAppShortBindingsDisabled");
    }

    @Test
    public void testAppShortBindingsDisabled() throws Exception {
        updateConfigElement("DisableShrtBndTestApp");

        FATServletClient.runTest(server, servletOther, "testAppShortBindingsDisabled");
        FATServletClient.runTest(server, servlet, "testAppShortBindingsDisabled");
    }

    @Test
    public void testBothAppShortBindingsDisabled() throws Exception {
        updateConfigElement("DisableShrtBndTestApp:DisableShrtBndOtherTestApp");

        FATServletClient.runTest(server, servletOther, "testBothAppShortBindingsDisabled");
        FATServletClient.runTest(server, servlet, "testBothAppShortBindingsDisabled");
    }
}
