/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.web.BndTestServlet;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.web.InterfaceAndNamespaceTestServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class EJBinWARBindingsTest extends FATServletClient {

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

    @Server("com.ibm.ws.ejbcontainer.bindings.fat.server")
    @TestServlets({ @TestServlet(servlet = BndTestServlet.class, contextRoot = "EJBinWARTest"),
                    @TestServlet(servlet = InterfaceAndNamespaceTestServlet.class, contextRoot = "EJBinWARTest") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server"));

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the ears
        JavaArchive EJBinWARIntf = ShrinkHelper.buildJavaArchive("EJBinWARIntf.jar", "com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.");
        JavaArchive EJBBean = ShrinkHelper.buildJavaArchive("EJBBean.jar", "com.ibm.ws.ejbcontainer.bindings.ejbinwar.ejb.");
        JavaArchive EJBinWARBean = ShrinkHelper.buildJavaArchive("EJBinWARBean.jar", "com.ibm.ws.ejbcontainer.bindings.ejbinwar.ejbinwar.");
        ShrinkHelper.addDirectory(EJBinWARBean, "test-applications/EJBinWARBean.jar/resources");
        WebArchive EJBinWARTest = ShrinkHelper.buildDefaultApp("EJBinWARTest.war", "com.ibm.ws.ejbcontainer.bindings.ejbinwar.web.");
        ShrinkHelper.addDirectory(EJBinWARTest, "test-applications/EJBinWARTest.war/resources");
        EJBinWARTest.addAsLibrary(EJBinWARBean);
        EnterpriseArchive EJBinWARTestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJBinWARTestApp.ear");
        EJBinWARTestApp.addAsModule(EJBinWARTest);
        EJBinWARTestApp.addAsModule(EJBBean);
        EJBinWARTestApp.addAsLibrary(EJBinWARIntf);
        ShrinkHelper.addDirectory(EJBinWARTestApp, "test-applications/EJBinWARTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJBinWARTestApp);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}