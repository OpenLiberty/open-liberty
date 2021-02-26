/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedbeans.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.managedbeans.fat.mb.bindings.web.ManagedBeanServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * These tests are for the managed bean bindings.
 * Bindings are set with the ibm-managed-bean-bnd.xml file.
 */
@RunWith(FATRunner.class)
public class ManagedBeanBindingsEJBTest extends FATServletClient {

    public static final String APP_NAME = "ManagedBeanBindingsTestWeb";

    @Server("ManagedBeansBindingsEjbServer")
    @TestServlet(servlet = ManagedBeanServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("ManagedBeansBindingsEjbServer"))
                    .andWith(FeatureReplacementAction.EE8_FEATURES().forServers("ManagedBeansBindingsEjbServer"))
                    .andWith(new JakartaEE9Action().fullFATOnly().forServers("ManagedBeansBindingsEjbServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### ManagedBeanBindingsApp.ear
        JavaArchive ManagedBeanBindingsEJB = ShrinkHelper.buildJavaArchive("ManagedBeanBindingsEJB.jar", "com.ibm.ws.managedbeans.fat.mb.bindings.ejb.");
        ManagedBeanBindingsEJB = (JavaArchive) ShrinkHelper.addDirectory(ManagedBeanBindingsEJB, "test-applications/ManagedBeanBindingsEJB.jar/resources");
        WebArchive ManagedBeanBindingsWeb = ShrinkHelper.buildDefaultApp("ManagedBeanBindingsWeb.war", "com.ibm.ws.managedbeans.fat.mb.bindings.web.");
        EnterpriseArchive ManagedBeanBindingsApp = ShrinkWrap.create(EnterpriseArchive.class, "ManagedBeanBindingsApp.ear");
        ManagedBeanBindingsApp.addAsModule(ManagedBeanBindingsEJB).addAsModule(ManagedBeanBindingsWeb);
        ManagedBeanBindingsApp = (EnterpriseArchive) ShrinkHelper.addDirectory(ManagedBeanBindingsApp, "test-applications/ManagedBeanBindingsApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, ManagedBeanBindingsApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("J2CA0086W");
        }
    }
}
