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
package com.ibm.ws.ejbcontainer.async.fat.tests;

import static junit.framework.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.async.fat.secure.web.AsyncSecureServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class AsyncSecureTests extends AbstractTest {

    @Server("com.ibm.ws.ejbcontainer.async.fat.AsyncSecureServer")
    @TestServlets({ @TestServlet(servlet = AsyncSecureServlet.class, contextRoot = "AsyncSecureTestWeb") })
    public static LibertyServer server;

    @Override
    public LibertyServer getServer() {
        return server;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncSecureServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.async.fat.AsyncSecureServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears & Wars

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        ShrinkHelper.exportDropinAppToServer(server, InitTxRecoveryLogApp);

        //#################### AsyncSecureTestApp.ear
        JavaArchive AsyncSecureTestEJB = ShrinkHelper.buildJavaArchive("AsyncSecureTestEJB.jar", "com.ibm.ws.ejbcontainer.async.fat.secure.ejb.");
        WebArchive AsyncSecureTestWeb = ShrinkHelper.buildDefaultApp("AsyncSecureTestWeb.war", "com.ibm.ws.ejbcontainer.async.fat.secure.web.");
        EnterpriseArchive AsyncSecureTestApp = ShrinkWrap.create(EnterpriseArchive.class, "AsyncSecureTestApp.ear");
        AsyncSecureTestApp.addAsModule(AsyncSecureTestEJB).addAsModule(AsyncSecureTestWeb);
        AsyncSecureTestApp = (EnterpriseArchive) ShrinkHelper.addDirectory(AsyncSecureTestApp, "test-applications/AsyncSecureTestApp.ear/resources");

        ShrinkHelper.exportAppToServer(server, AsyncSecureTestApp);
        server.addInstalledAppForValidation("AsyncSecureTestApp");

        // Finally, start server
        server.startServer();

        assertNotNull("Security service did not report it was ready", server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("LTPA configuration should report it is ready", server.waitForStringInLog("CWWKS4105I"));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.stopServer();
    }
}