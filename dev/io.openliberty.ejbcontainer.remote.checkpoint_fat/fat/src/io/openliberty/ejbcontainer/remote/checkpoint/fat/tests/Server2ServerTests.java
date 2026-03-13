/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.ejbcontainer.remote.checkpoint.fat.tests;

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

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.ejbcontainer.remote.checkpoint.fat.tests.repeataction.RepeatEE10Secure;
import io.openliberty.ejbcontainer.remote.checkpoint.fat.tests.repeataction.RepeatEE8Secure;
import io.openliberty.ejbcontainer.remote.checkpoint.fat.tests.repeataction.RepeatEE9Secure;
import io.openliberty.ejbcontainer.remote.client.web.RemoteTxAttrServlet;

/**
 * Tests remote EJB method calls between two Liberty servers.
 */
@CheckpointTest
@RunWith(FATRunner.class)
public class Server2ServerTests extends AbstractTest {

    @Server("checkpointRemoteServerClient")
    @TestServlets({ @TestServlet(servlet = RemoteTxAttrServlet.class, contextRoot = "RemoteClientWeb") })
    public static LibertyServer unsecureClientServer;

    @Server("checkpointRemoteServer")
    public static LibertyServer unsecureRemoteServer;

    @Server("checkpointSecureRemoteServerClient")
    public static LibertyServer secureClientServer;

    @Server("checkpointSecureRemoteServer")
    public static LibertyServer secureRemoteServer;

    public static boolean isSecureActive = false;

    @Override
    public LibertyServer getServer() {
        return isSecureActive ? secureClientServer : unsecureClientServer;
    }

    @ClassRule
    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE10_FEATURES().forServers("checkpointRemoteServerClient", //
                                                                                                       "checkpointRemoteServer")).andWith(new RepeatEE10Secure().forServers("checkpointSecureRemoteServerClient",
                                                                                                                                                                            "checkpointSecureRemoteServer"));

//    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE10_FEATURES().forServers("checkpointRemoteServerClient",
//                                                                                                       "checkpointRemoteServer"));

//    public static RepeatTests r = RepeatTests.with(FeatureReplacementAction.EE8_FEATURES().fullFATOnly().forServers("checkpointRemoteServerClient",
//                                                                                                                    "checkpointRemoteServer")) //
//                    .andWith(FeatureReplacementAction.EE9_FEATURES().fullFATOnly().forServers("checkpointRemoteServerClient",
//                                                                                              "checkpointRemoteServer")) //
//                    .andWith(FeatureReplacementAction.EE10_FEATURES().fullFATOnly().forServers("checkpointRemoteServerClient",
//                                                                                               "checkpointRemoteServer")) //
//                    .andWith(new RepeatEE8Secure().fullFATOnly().forServers("checkpointSecureRemoteServerClient",
//                                                                            "checkpointSecureRemoteServer")) //
//                    .andWith(new RepeatEE9Secure().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).forServers("checkpointSecureRemoteServerClient",
//                                                                                                                                             "checkpointSecureRemoteServer")) //
//                    .andWith(new RepeatEE10Secure().forServers("checkpointSecureRemoteServerClient",
//                                                               "checkpointSecureRemoteServer"));

    @BeforeClass
    public static void beforeClass() throws Exception {
        isSecureActive = RepeatEE8Secure.isActive() || RepeatEE9Secure.isActive() || RepeatEE10Secure.isActive();
        LibertyServer clientServer = isSecureActive ? secureClientServer : unsecureClientServer;
        LibertyServer remoteServer = isSecureActive ? secureRemoteServer : unsecureRemoteServer;

        assembleAndExportApplications(clientServer, remoteServer);

        // Start the server-side server
        remoteServer.startServer();

        // Checkpoint and restore the client-side server
        clientServer.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
//        clientServer.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, checkpointServer -> {
//            File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
//            try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
//                serverEnvWriter.println("RESTORE_IIOP_PORT=" + RESTORE_IIOP_PORT);
//                serverEnvWriter.println("RESTORE_IIOPS_PORT=" + RESTORE_IIOPS_PORT);
//            } catch (FileNotFoundException e) {
//                throw new UncheckedIOException(e);
//            }
//        });
        clientServer.useSecondaryHTTPPort();
        clientServer.startServer(); // Do the checkpoint
        clientServer.checkpointRestore();
    }

    static void assembleAndExportApplications(LibertyServer clientServer, LibertyServer remoteServer) throws Exception {

        //#################### InitTxRecoveryLogApp.ear (Automatically initializes transaction recovery logs)
        // FAILS CHECKPOINT BECAUSE STARTUP BEAN REQUIRES A TX
        //JavaArchive InitTxRecoveryLogEJBJar = ShrinkHelper.buildJavaArchive("InitTxRecoveryLogEJB.jar", "io.openliberty.ejbcontainer.init.recovery.ejb.");
        //
        //EnterpriseArchive InitTxRecoveryLogApp = ShrinkWrap.create(EnterpriseArchive.class, "InitTxRecoveryLogApp.ear");
        //InitTxRecoveryLogApp.addAsModule(InitTxRecoveryLogEJBJar);
        //
        //ShrinkHelper.exportDropinAppToServer(clientServer, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);
        //ShrinkHelper.exportDropinAppToServer(remoteServer, InitTxRecoveryLogApp, DeployOptions.SERVER_ONLY);

        //#################### RemoteClientApp
        JavaArchive RemoteServerSharedJar = ShrinkHelper.buildJavaArchive("RemoteServerShared.jar", "io.openliberty.ejbcontainer.remote.server.shared.", "test.");
        WebArchive RemoteClientWeb = ShrinkHelper.buildDefaultApp("RemoteClientWeb.war", "io.openliberty.ejbcontainer.remote.client.web.");
        RemoteClientWeb.addAsLibraries(RemoteServerSharedJar);

        EnterpriseArchive RemoteClientApp = ShrinkWrap.create(EnterpriseArchive.class, "RemoteClientApp.ear");
        RemoteClientApp.addAsModule(RemoteClientWeb);
        RemoteClientApp = (EnterpriseArchive) ShrinkHelper.addDirectory(RemoteClientApp, "test-applications/RemoteClientApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(clientServer, RemoteClientApp, DeployOptions.SERVER_ONLY);

        //#################### RemoteServerApp
        JavaArchive RemoteServerEJBJar = ShrinkHelper.buildJavaArchive("RemoteServerEJB.jar", "io.openliberty.ejbcontainer.remote.server.ejb.");

        EnterpriseArchive RemoteServerApp = ShrinkWrap.create(EnterpriseArchive.class, "RemoteServerApp.ear");
        RemoteServerApp.addAsLibraries(RemoteServerSharedJar).addAsModule(RemoteServerEJBJar);
        RemoteServerApp = (EnterpriseArchive) ShrinkHelper.addDirectory(RemoteServerApp, "test-applications/RemoteServerApp.ear/resources");

        ShrinkHelper.exportDropinAppToServer(remoteServer, RemoteServerApp, DeployOptions.SERVER_ONLY);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (isSecureActive) {
            secureClientServer.stopServer();
            secureRemoteServer.stopServer("CNTR0019E");
        } else {
            unsecureClientServer.stopServer();
            unsecureRemoteServer.stopServer("CNTR0019E");
        }
        isSecureActive = false;
    }
}