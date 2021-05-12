/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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

import com.ibm.ejb3x.SimpleBindingName.web.SimpleBindingNameTestServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class SimpleBindingNameTest extends FATServletClient {

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
    @TestServlet(servlet = SimpleBindingNameTestServlet.class, contextRoot = "SimpleBindingNameWeb")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server"));

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the ears
        JavaArchive SimpleBindingNameEJB = ShrinkHelper.buildJavaArchive("SimpleBindingNameEJB.jar", "com.ibm.ejb3x.SimpleBindingName.ejb.");
        ShrinkHelper.addDirectory(SimpleBindingNameEJB, "test-applications/SimpleBindingNameEJB.jar/resources");

        WebArchive SimpleBindingNameWeb = ShrinkHelper.buildDefaultApp("SimpleBindingNameWeb.war", "com.ibm.ejb3x.SimpleBindingName.web.");

        EnterpriseArchive SimpleBindingNameTestApp = ShrinkWrap.create(EnterpriseArchive.class, "SimpleBindingNameTestApp.ear");
        SimpleBindingNameTestApp.addAsModules(SimpleBindingNameEJB, SimpleBindingNameWeb);
        ShrinkHelper.addDirectory(SimpleBindingNameTestApp, "test-applications/SimpleBindingNameTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, SimpleBindingNameTestApp);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
