/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.injection.misc.web.InjectionMiscServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Tests miscellaneous EJB injection scenarios.
 */
@RunWith(FATRunner.class)
public class InjectionMiscTest extends FATServletClient {
    @Server("com.ibm.ws.ejbcontainer.injection.fat.InjectionMiscServer")
    @TestServlets({ @TestServlet(servlet = InjectionMiscServlet.class, contextRoot = "InjectionMiscWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.injection.fat.InjectionMiscServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.injection.fat.InjectionMiscServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive InjectionMiscBean = ShrinkHelper.buildJavaArchive("InjectionMiscBean.jar", "com.ibm.ws.ejbcontainer.injection.misc.ejb.");
        WebArchive InjectionMiscWeb = ShrinkHelper.buildDefaultApp("InjectionMiscWeb.war", "com.ibm.ws.ejbcontainer.injection.misc.web.");
        EnterpriseArchive InjectionMiscTestApp = ShrinkWrap.create(EnterpriseArchive.class, "InjectionMiscTestApp.ear");
        InjectionMiscTestApp.addAsModule(InjectionMiscBean).addAsModule(InjectionMiscWeb);
        ShrinkHelper.addDirectory(InjectionMiscTestApp, "test-applications/InjectionMiscTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, InjectionMiscTestApp);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
