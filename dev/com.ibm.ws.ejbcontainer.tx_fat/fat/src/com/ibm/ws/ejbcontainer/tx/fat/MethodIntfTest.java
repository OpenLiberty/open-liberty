/*******************************************************************************
 * Copyright (c) 2010, 2023 IBM Corporation and others.
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
import com.ibm.ws.ejbcontainer.tx.methodintf.web.MethodIntfServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This test ensures that the new method-intf element values specified in the
 * EJB 3.1 work properly.
 */
@RunWith(FATRunner.class)
public class MethodIntfTest {

    @Server("com.ibm.ws.ejbcontainer.tx.fat.MDBServer")
    @TestServlets({ @TestServlet(servlet = MethodIntfServlet.class, contextRoot = "MethodIntfWeb") })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.tx.fat.MDBServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.tx.fat.MDBServer")).andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers("com.ibm.ws.ejbcontainer.tx.fat.MDBServer")).andWith(FeatureReplacementAction.EE10_FEATURES().forServers("com.ibm.ws.ejbcontainer.tx.fat.MDBServer"));

    @BeforeClass
    public static void setUp() throws Exception {
        // Use ShrinkHelper to build the ears
        JavaArchive MethodIntfEJB = ShrinkHelper.buildJavaArchive("MethodIntfEJB.jar", "com.ibm.ws.ejbcontainer.tx.methodintf.ejb.");
        WebArchive MethodIntfWeb = ShrinkHelper.buildDefaultApp("MethodIntfWeb.war", "com.ibm.ws.ejbcontainer.tx.methodintf.web.");
        EnterpriseArchive MethodIntfTestApp = ShrinkWrap.create(EnterpriseArchive.class, "MethodIntfTest.ear");
        MethodIntfTestApp.addAsModule(MethodIntfEJB).addAsModule(MethodIntfWeb);

        ShrinkHelper.exportDropinAppToServer(server, MethodIntfTestApp, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
}
