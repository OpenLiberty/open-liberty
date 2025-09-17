/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.oracle;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.oracle.OracleContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import web.OracleTestServlet;

@CheckpointTest
@RunWith(FATRunner.class)
public class OracleCheckpointTest {
    public static final String JEE_APP = "oraclejdbcfat";
    public static final String SERVLET_NAME = "OracleTestServlet";

    @Server("com.ibm.ws.jdbc.fat.oracle")
    @TestServlet(servlet = OracleTestServlet.class, path = JEE_APP + "/" + SERVLET_NAME)
    public static LibertyServer server;

    public static final OracleContainer oracle = FATSuite.getSharedOracleContainer();

    @BeforeClass
    public static void setUp() throws Exception {
        FATSuite.initDatabaseTables(oracle);

        // Set server environment variables
        Map<String, String> envVars = new HashMap<>();
        envVars.put("ORACLE_URL", oracle.getJdbcUrl());
        envVars.put("ORACLE_USER", oracle.getUsername());
        envVars.put("ORACLE_PASSWORD", oracle.getPassword());
        envVars.put("ORACLE_DBNAME", oracle.getSid());
        envVars.put("ORACLE_PORT", Integer.toString(oracle.getFirstMappedPort()));
        envVars.put("ORACLE_HOST", oracle.getHost());

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, JEE_APP, "web");

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);

        // Start Server
        server.startServer();

        server.addEnvVarsForCheckpoint(envVars);

        server.checkpointRestore();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer();
        }
        if (oracle.isRunning()) {
            oracle.stop();
        }
    }
}
