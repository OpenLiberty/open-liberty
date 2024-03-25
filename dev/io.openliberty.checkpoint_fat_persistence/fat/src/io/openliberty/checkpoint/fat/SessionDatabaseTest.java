/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package io.openliberty.checkpoint.fat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatActions.SEVersion;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.database.DerbyNetworkUtilities;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class SessionDatabaseTest extends FATServletClient {

    static final String SERVER_NAME = "sessionDatabaseServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    public static SessionDatabaseApp app = null;
    private static int DERBY_PORT = 1528;

    @ClassRule
    public static RepeatTests rt = RepeatTests.with(new FeatureReplacementAction().forServers(SERVER_NAME).removeFeatures(Collections.singleton("jdbc-*")).addFeature("jdbc-4.2").withID("JDBC4.2"))
                    .andWith(new FeatureReplacementAction().forServers(SERVER_NAME).removeFeatures(Collections.singleton("jdbc-*")).addFeature("jdbc-4.1").withID("JDBC4.1").fullFATOnly())
                    .andWith(new FeatureReplacementAction().forServers(SERVER_NAME).removeFeatures(Collections.singleton("jdbc-*"))
                                    .addFeature("jdbc-4.3")
                                    .withID("JDBC4.3")
                                    .withMinJavaLevel(SEVersion.JAVA11)
                                    .fullFATOnly());

    @BeforeClass
    public static void setUp() throws Exception {
        DerbyNetworkUtilities.startDerbyNetwork(DERBY_PORT);
        app = new SessionDatabaseApp(server, true, "io.checkpoint.session.database.web");

        Consumer<LibertyServer> preRestoreLogic = checkpointServer -> {
            File serverEnvFile = new File(checkpointServer.getServerRoot() + "/server.env");
            try (PrintWriter serverEnvWriter = new PrintWriter(new FileOutputStream(serverEnvFile))) {
                serverEnvWriter.println("DERBY_PORT=" + DERBY_PORT);
            } catch (FileNotFoundException e) {
                throw new UncheckedIOException(e);
            }
        };

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, preRestoreLogic);
        server.startServer();
        server.checkpointRestore();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            DerbyNetworkUtilities.stopDerbyNetwork(DERBY_PORT);
        }
    }

    /**
     * Ensure that various types of objects can be stored in a session,
     * serialized when the session is evicted from memory, and deserialized
     * when the session is accessed again.
     */
    @Test
    public void testSerialization() throws Exception {
        List<String> session = new ArrayList<>();
        app.invokeServlet("testSerialization", session);
        try {
            app.invokeServlet("evictSession", null);
            app.invokeServlet("testSerialization_complete", session);
            server.stopServer(false, "");
            server.checkpointRestore();
            app.invokeServlet("testSerialization_complete", session);
        } finally {
            app.invalidateSession(session);
        }
    }

    /**
     * Ensure that various types of objects can be stored in a session,
     * serialized when the session is evicted from memory, and deserialized
     * when the session is accessed again.
     */
    @Test
    public void testSerializeDataSource() throws Exception {
        List<String> session = new ArrayList<>();
        app.invokeServlet("testSerializeDataSource", session);
        try {
            app.invokeServlet("evictSession", null);
            app.invokeServlet("testSerializeDataSource_complete", session);
            server.stopServer(false, "");
            server.checkpointRestore();
            app.invokeServlet("testSerializeDataSource_complete", session);
        } finally {
            app.invalidateSession(session);
        }
    }
}
