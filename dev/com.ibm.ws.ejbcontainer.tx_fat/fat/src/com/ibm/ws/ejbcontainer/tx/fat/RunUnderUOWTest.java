/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.tx.fat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.ejbcontainer.tx.rununderuow.web.RunUnderUOWServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test that UOWManager.runUnderUOW works properly with EJBs.
 */
@RunWith(FATRunner.class)
public class RunUnderUOWTest extends FATServletClient {
    @Server("com.ibm.ws.ejbcontainer.tx.fat.RunUnderUOWServer")
    @TestServlets({ @TestServlet(servlet = RunUnderUOWServlet.class, contextRoot = "RunUnderUOWWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.tx.fat.RunUnderUOWServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.tx.fat.RunUnderUOWServer")).andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.tx.fat.RunUnderUOWServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive RunUnderUOWBean = ShrinkHelper.buildJavaArchive("RunUnderUOWBean.jar", "com.ibm.ws.ejbcontainer.tx.rununderuow.ejb.");
        WebArchive RunUnderUOWWeb = ShrinkHelper.buildDefaultApp("RunUnderUOWWeb.war", "com.ibm.ws.ejbcontainer.tx.rununderuow.web.");
        EnterpriseArchive RunUnderUOWTestApp = ShrinkWrap.create(EnterpriseArchive.class, "RunUnderUOWTestApp.ear");
        RunUnderUOWTestApp.addAsModule(RunUnderUOWBean).addAsModule(RunUnderUOWWeb);

        ShrinkHelper.exportDropinAppToServer(server, RunUnderUOWTestApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

}
