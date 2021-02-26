/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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

import com.ibm.ejb2x.defbnd.web.EJB2XDefBndTestServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.bindings.defbnd.web.DefaultBindingsServlet;
import com.ibm.ws.ejbcontainer.bindings.defbnd.web.DefaultComponentBindingsServlet;
import com.ibm.ws.ejbcontainer.bindings.defbnd.web.DefaultJavaColonBindingsServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class DefaultBindingsTest extends FATServletClient {

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
    @TestServlets({ @TestServlet(servlet = DefaultJavaColonBindingsServlet.class, contextRoot = "EJB3DefBndWeb"),
                    @TestServlet(servlet = DefaultBindingsServlet.class, contextRoot = "EJB3DefBndWeb"),
                    @TestServlet(servlet = DefaultComponentBindingsServlet.class, contextRoot = "EJB3DefBndWeb"),
                    @TestServlet(servlet = EJB2XDefBndTestServlet.class, contextRoot = "EJB2XDefBndWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server"));

    @BeforeClass
    public static void setUp() throws Exception {
        server.deleteAllDropinApplications();
        server.removeAllInstalledAppsForValidation();

        // Use ShrinkHelper to build the ears
        JavaArchive EJB3DefBndBean = ShrinkHelper.buildJavaArchive("EJB3DefBndBean.jar", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.");
        ShrinkHelper.addDirectory(EJB3DefBndBean, "test-applications/EJB3DefBndBean.jar/resources");
        WebArchive EJB3DefBndWeb = ShrinkHelper.buildDefaultApp("EJB3DefBndWeb.war", "com.ibm.ws.ejbcontainer.bindings.defbnd.web.");
        EnterpriseArchive EJB3DefBndTestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB3DefBndTestApp.ear");
        EJB3DefBndTestApp.addAsModules(EJB3DefBndBean, EJB3DefBndWeb);
        ShrinkHelper.addDirectory(EJB3DefBndTestApp, "test-applications/EJB3DefBndTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB3DefBndTestApp);

        //EJB2X
        JavaArchive EJB2XDefBndEJB = ShrinkHelper.buildJavaArchive("EJB2XDefBndEJB.jar", "com.ibm.ejb2x.defbnd.ejb.");
        ShrinkHelper.addDirectory(EJB2XDefBndEJB, "test-applications/EJB2XDefBndEJB.jar/resources");
        WebArchive EJB2XDefBndWeb = ShrinkHelper.buildDefaultApp("EJB2XDefBndWeb.war", "com.ibm.ejb2x.defbnd.web.");
        EnterpriseArchive EJB2XDefBndTestApp = ShrinkWrap.create(EnterpriseArchive.class, "EJB2XDefBndTestApp.ear");
        EJB2XDefBndTestApp.addAsModules(EJB2XDefBndEJB, EJB2XDefBndWeb);
        ShrinkHelper.addDirectory(EJB2XDefBndTestApp, "test-applications/EJB2XDefBndTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, EJB2XDefBndTestApp);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}