/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import org.junit.runner.RunWith;

import com.ibm.ejb3x.BindingName.web.BindingNameTestServlet;
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
public class BindingNameTest extends FATServletClient {

    @Server("com.ibm.ws.ejbcontainer.bindings.fat.server")
    @TestServlet(servlet = BindingNameTestServlet.class, contextRoot = "BindingNameWeb")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.bindings.fat.server"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive BindingNameEJB = ShrinkHelper.buildJavaArchive("BindingNameEJB.jar", "com.ibm.ejb3x.BindingName.ejb.");
        ShrinkHelper.addDirectory(BindingNameEJB, "test-applications/BindingNameEJB.jar/resources");

        WebArchive BindingNameWeb = ShrinkHelper.buildDefaultApp("BindingNameWeb.war", "com.ibm.ejb3x.BindingName.web.");

        EnterpriseArchive BindingNameTestApp = ShrinkWrap.create(EnterpriseArchive.class, "BindingNameTestApp.ear");
        BindingNameTestApp.addAsModules(BindingNameEJB, BindingNameWeb);
        ShrinkHelper.addDirectory(BindingNameTestApp, "test-applications/BindingNameTestApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(server, BindingNameTestApp);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
