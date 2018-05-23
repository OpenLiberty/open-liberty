/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import suite.r70.base.ejb3session.sl.mix.web.AsmDescSecRolesServlet;
import suite.r70.base.ejb3session.sl.mix.web.AsmDescTranAttrServlet;

@RunWith(FATRunner.class)
public class SecureTests {

    @Server("com.ibm.ws.ejbcontainer.tx.fat.SecureServer")
    @TestServlets({ @TestServlet(servlet = AsmDescSecRolesServlet.class, contextRoot = "StatelessMixSecWeb"),
                    @TestServlet(servlet = AsmDescTranAttrServlet.class, contextRoot = "StatelessMixSecWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().forServers("com.ibm.ws.ejbcontainer.tx.fat.SecureServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.tx.fat.SecureServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive StatelessMixASMDescEJB = ShrinkHelper.buildJavaArchive("StatelessMixASMDescEJB.jar", "suite.r70.base.ejb3session.sl.mix.asmdesc.");
        WebArchive StatelessMixSecWeb = ShrinkHelper.buildDefaultApp("StatelessMixSecWeb.war", "suite.r70.base.ejb3session.sl.mix.web.");
        EnterpriseArchive StatelessMixSecApp = ShrinkWrap.create(EnterpriseArchive.class, "StatelessMixSec.ear");
        StatelessMixSecApp.addAsModule(StatelessMixASMDescEJB).addAsModule(StatelessMixSecWeb);

        ShrinkHelper.exportDropinAppToServer(server, StatelessMixSecApp);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0019E");
        }
    }
}