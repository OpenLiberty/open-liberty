/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.remote.fat.tests;

import static org.junit.Assert.assertNotNull;

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
import com.ibm.ws.ejbcontainer.remote.client.web.RemoteTxAttrServlet;
import com.ibm.ws.ejbcontainer.remote.fat.tests.repeataction.RepeatEE10Secure;
import com.ibm.ws.ejbcontainer.remote.fat.tests.repeataction.RepeatEE7Secure;
import com.ibm.ws.ejbcontainer.remote.fat.tests.repeataction.RepeatEE8Secure;
import com.ibm.ws.ejbcontainer.remote.fat.tests.repeataction.RepeatEE9Secure;

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
    public static LibertyServer unsecureClientServer;

    @Server("com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")
    public static LibertyServer unsecureRemoteServer;

    @Server("com.ibm.ws.ejbcontainer.remote.fat.SecureRemoteServerClient")
    public static LibertyServer secureClientServer;

    @Server("com.ibm.ws.ejbcontainer.remote.fat.SecureRemoteServer")
    public static LibertyServer secureRemoteServer;

    public static boolean isSecureActive = false;

    @Override
    public LibertyServer getServer() {
        return isSecureActive ? secureClientServer : unsecureClientServer;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE7_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteServerClient",
                                                                                                                    "com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")) //
                    .andWith(FeatureReplacementAction.EE8_FEATURES().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteServerClient",
                                                                                "com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")) //
                    .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteServerClient",
                                                                                              "com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")) //
                    .andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.fat.RemoteServerClient",
                                                                                               "com.ibm.ws.ejbcontainer.remote.fat.RemoteServer")) //
                    .andWith(new RepeatEE7Secure().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.fat.SecureRemoteServerClient",
                                                                            "com.ibm.ws.ejbcontainer.remote.fat.SecureRemoteServer")) //
                    .andWith(new RepeatEE8Secure().fullFATOnly().forServers("com.ibm.ws.ejbcontainer.remote.fat.SecureRemoteServerClient",
                                                                            "com.ibm.ws.ejbcontainer.remote.fat.SecureRemoteServer")) //
                    .andWith(new RepeatEE9Secure().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers("com.ibm.ws.ejbcontainer.remote.fat.SecureRemoteServerClient",
                                                                                                                                             "com.ibm.ws.ejbcontainer.remote.fat.SecureRemoteServer")) //
                    .andWith(new RepeatEE10Secure().forServers("com.ibm.ws.ejbcontainer.remote.fat.SecureRemoteServerClient",
                                                               "com.ibm.ws.ejbcontainer.remote.fat.SecureRemoteServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        isSecureActive = RepeatEE7Secure.isActive() || RepeatEE8Secure.isActive() || RepeatEE9Secure.isActive() || RepeatEE10Secure.isActive();
        LibertyServer clientServer = isSecureActive ? secureClientServer : unsecureClientServer;
        LibertyServer remoteServer = isSecureActive ? secureRemoteServer : unsecureRemoteServer;

        // Use ShrinkHelper to build the Ears

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "com.ibm.ws.ejbcontainer.init.recovery.ejb.");

        EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);

        ShrinkHelper.exportDropinAppToServer(clientServer, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);
        ShrinkHelper.exportDropinAppToServer(remoteServer, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);

        //#################### RemoteClientApp
        JavaArchive RemoteServerSharedJar = ShrinkHelper.buildJavaArchive("RemoteServerShared.jar", "com.ibm.ws.ejbcontainer.remote.server.shared.", "test.");
        WebArchive RemoteClientWeb = ShrinkHelper.buildDefaultApp("RemoteClientWeb.war", "com.ibm.ws.ejbcontainer.remote.client.web.");
        RemoteClientWeb.addAsLibraries(RemoteServerSharedJar);

        EnterpriseArchive RemoteClientApp = ShrinkWrap.create(EnterpriseArchive.class, "RemoteClientApp.ear");
        RemoteClientApp.addAsModule(RemoteClientWeb);
        RemoteClientApp = (EnterpriseArchive) ShrinkHelper.addDirectory(RemoteClientApp, "test-applications/RemoteClientApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(clientServer, RemoteClientApp, DeployOptions.SERVER_ONLY);

        //#################### RemoteServerApp
        JavaArchive RemoteServerEJBJar = ShrinkHelper.buildJavaArchive("RemoteServerEJB.jar", "com.ibm.ws.ejbcontainer.remote.server.ejb.");

        EnterpriseArchive RemoteServerApp = ShrinkWrap.create(EnterpriseArchive.class, "RemoteServerApp.ear");
        RemoteServerApp.addAsLibraries(RemoteServerSharedJar).addAsModule(RemoteServerEJBJar);
        RemoteServerApp = (EnterpriseArchive) ShrinkHelper.addDirectory(RemoteServerApp, "test-applications/RemoteServerApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(remoteServer, RemoteServerApp, DeployOptions.SERVER_ONLY);

        // Finally, start servers
        remoteServer.startServer();
        if (isSecureActive) {
            // verify the appSecurity-2.0 feature is ready
            assertNotNull("Security service did not report it was ready", remoteServer.waitForStringInLogUsingMark("CWWKS0008I"));
            assertNotNull("LTPA configuration did not report it was ready", remoteServer.waitForStringInLogUsingMark("CWWKS4105I"));
            assertNotNull("ORB did not report it was ready", remoteServer.waitForStringInLogUsingMark("CWWKI0001I"));
        }

        clientServer.useSecondaryHTTPPort();
        clientServer.startServer();
        if (isSecureActive) {
            // verify the appSecurity-2.0 feature is ready
            assertNotNull("Security service did not report it was ready", clientServer.waitForStringInLogUsingMark("CWWKS0008I"));
            assertNotNull("LTPA configuration did not report it was ready", clientServer.waitForStringInLogUsingMark("CWWKS4105I"));
            assertNotNull("ORB did not report it was ready", clientServer.waitForStringInLogUsingMark("CWWKI0001I"));
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (isSecureActive) {
            try {
                secureClientServer.stopServer("CWWKS9582E");
            } finally {
                secureRemoteServer.stopServer("CNTR0019E", "CWWKS9582E");
            }
        } else {
            try {
                unsecureClientServer.stopServer();
            } finally {
                unsecureRemoteServer.stopServer("CNTR0019E");
            }
        }
        isSecureActive = false;
    }
}