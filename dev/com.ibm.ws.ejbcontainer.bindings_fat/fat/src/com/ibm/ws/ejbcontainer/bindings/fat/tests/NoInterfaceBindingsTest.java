/*******************************************************************************
 * Copyright (c) 2009, 2023 IBM Corporation and others.
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
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.web.NoInterfaceBindingSingletonServlet;
import com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.web.NoInterfaceBindingStatefulServlet;
import com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.web.NoInterfaceBindingStatelessServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class NoInterfaceBindingsTest extends AbstractTest {

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

    @Server("com.ibm.ws.ejbcontainer.bindings.noInterface.fat.server")
    @TestServlets({ @TestServlet(servlet = NoInterfaceBindingSingletonServlet.class, contextRoot = "NoInterfaceBndWeb"),
                    @TestServlet(servlet = NoInterfaceBindingStatefulServlet.class, contextRoot = "NoInterfaceBndWeb"),
                    @TestServlet(servlet = NoInterfaceBindingStatelessServlet.class, contextRoot = "NoInterfaceBndWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.noInterface.fat.server")).andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.noInterface.fat.server")).andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers("com.ibm.ws.ejbcontainer.bindings.noInterface.fat.server")).andWith(FeatureReplacementAction.EE10_FEATURES().forServers("com.ibm.ws.ejbcontainer.bindings.noInterface.fat.server"));

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the ears
        JavaArchive NoInterfaceBndBean = ShrinkHelper.buildJavaArchive("NoInterfaceBndBean.jar", "com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.ejb.");
        ShrinkHelper.addDirectory(NoInterfaceBndBean, "test-applications/NoInterfaceBndBean.jar/resources");

        JavaArchive NoInterfaceBndCompBean = ShrinkHelper.buildJavaArchive("NoInterfaceBndCompBean.jar");
        ShrinkHelper.addDirectory(NoInterfaceBndCompBean, "test-applications/NoInterfaceBndCompBean.jar/resources");

        JavaArchive NoInterfaceBndCustomBean = ShrinkHelper.buildJavaArchive("NoInterfaceBndCustomBean.jar");
        ShrinkHelper.addDirectory(NoInterfaceBndCustomBean, "test-applications/NoInterfaceBndCustomBean.jar/resources");

        JavaArchive NoInterfaceBndSimpleBean = ShrinkHelper.buildJavaArchive("NoInterfaceBndSimpleBean.jar");
        ShrinkHelper.addDirectory(NoInterfaceBndSimpleBean, "test-applications/NoInterfaceBndSimpleBean.jar/resources");

        WebArchive NoInterfaceBndWeb = ShrinkHelper.buildDefaultApp("NoInterfaceBndWeb.war", "com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.web.");
        EnterpriseArchive NoInterfaceBndTestApp = ShrinkWrap.create(EnterpriseArchive.class, "NoInterfaceBndTestApp.ear");
        NoInterfaceBndTestApp.addAsModules(NoInterfaceBndBean, NoInterfaceBndCompBean, NoInterfaceBndCustomBean, NoInterfaceBndSimpleBean, NoInterfaceBndWeb);
        ShrinkHelper.addDirectory(NoInterfaceBndTestApp, "test-applications/NoInterfaceBndTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, NoInterfaceBndTestApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        stopServer(server, "CNTR0168W", "CNTR0338W");
    }
}
