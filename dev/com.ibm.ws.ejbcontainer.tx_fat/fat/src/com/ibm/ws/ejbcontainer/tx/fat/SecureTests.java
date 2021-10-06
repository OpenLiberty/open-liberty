/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
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
import com.ibm.ws.ejbcontainer.tx.statlesmixsec.web.AsmDescSecRolesServlet;
import com.ibm.ws.ejbcontainer.tx.statlesmixsec.web.AsmDescTranAttrServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class SecureTests {

    @Server("com.ibm.ws.ejbcontainer.tx.fat.SecureServer")
    @TestServlets({ @TestServlet(servlet = AsmDescSecRolesServlet.class, contextRoot = "StatelessMixSecWeb"),
                    @TestServlet(servlet = AsmDescTranAttrServlet.class, contextRoot = "StatelessMixSecWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.tx.fat.SecureServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.tx.fat.SecureServer")).andWith(FeatureReplacementAction.EE9_FEATURES().forServers("com.ibm.ws.ejbcontainer.tx.fat.SecureServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive StatelessMixASMDescEJB = ShrinkHelper.buildJavaArchive("StatelessMixASMDescEJB.jar", "com.ibm.ws.ejbcontainer.tx.statlesmixsec.ejb.");
        WebArchive StatelessMixSecWeb = ShrinkHelper.buildDefaultApp("StatelessMixSecWeb.war", "com.ibm.ws.ejbcontainer.tx.statlesmixsec.web.");
        EnterpriseArchive StatelessMixSecApp = ShrinkWrap.create(EnterpriseArchive.class, "StatelessMixSec.ear");
        StatelessMixSecApp.addAsModule(StatelessMixASMDescEJB).addAsModule(StatelessMixSecWeb);

        ShrinkHelper.exportDropinAppToServer(server, StatelessMixSecApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CNTR0019E");
        }
    }
}