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
package com.ibm.ws.ejbcontainer.remote.fat.tests;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.ejbcontainer.remote.client.web.RemoteTxAttrServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests remote EJB method calls between two Liberty servers.
 */
@RunWith(FATRunner.class)
public class Server2ServerTests extends AbstractTest {

    @Server("com.ibm.ws.ejbcontainer.remote.fat.RemoteServerClient")
    @TestServlets({ @TestServlet(servlet = RemoteTxAttrServlet.class, contextRoot = "RemoteClientWeb") })
    public static LibertyServer clientServer;

    @Server("com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")
    public static LibertyServer remoteServer;

    @Override
    public LibertyServer getServer() {
        return clientServer;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteServerClient",
                                                                                                                    "com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")).andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteServerClient",
                                                                                                                                                                                                                                   "com.ibm.ws.ejbcontainer.remote.fat.RemoteServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        // Use ShrinkHelper to build the Ears

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        ShrinkHelper.exportDropinAppToServer(clientServer, InitTxRecoveryLogApp);
        ShrinkHelper.exportDropinAppToServer(remoteServer, InitTxRecoveryLogApp);

        //#################### RemoteClientApp
        JavaArchive RemoteServerSharedJar = ShrinkHelper.buildJavaArchive("RemoteServerShared.jar", "com.ibm.ws.ejbcontainer.remote.server.shared.", "test.");
        WebArchive RemoteClientWeb = ShrinkHelper.buildDefaultApp("RemoteClientWeb.war", "com.ibm.ws.ejbcontainer.remote.client.web.");
        RemoteClientWeb.addAsLibraries(RemoteServerSharedJar);

        EnterpriseArchive RemoteClientApp = ShrinkWrap.create(EnterpriseArchive.class, "RemoteClientApp.ear");
        RemoteClientApp.addAsModule(RemoteClientWeb);
        RemoteClientApp = (EnterpriseArchive) ShrinkHelper.addDirectory(RemoteClientApp, "test-applications/RemoteClientApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(clientServer, RemoteClientApp);

        //#################### RemoteServerApp
        JavaArchive RemoteServerEJBJar = ShrinkHelper.buildJavaArchive("RemoteServerEJB.jar", "com.ibm.ws.ejbcontainer.remote.server.ejb.");

        EnterpriseArchive RemoteServerApp = ShrinkWrap.create(EnterpriseArchive.class, "RemoteServerApp.ear");
        RemoteServerApp.addAsLibraries(RemoteServerSharedJar).addAsModule(RemoteServerEJBJar);
        RemoteServerApp = (EnterpriseArchive) ShrinkHelper.addDirectory(RemoteServerApp, "test-applications/RemoteServerApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(remoteServer, RemoteServerApp);

        // Finally, start servers
        remoteServer.startServer();
        clientServer.useSecondaryHTTPPort();
        clientServer.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        clientServer.stopServer();
        remoteServer.stopServer("CNTR0019E");
    }
}