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
package io.openliberty.checkpoint.fat;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.PostgreSQLContainer;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
@MinimumJavaLevel(javaLevel = 17)
public class JakartaDataTest extends FATServletClient {
    private static final String POSTGRES_DB = "test";
    private static final String POSTGRES_USER = "productionuser";
    private static final String POSTGRES_PASS = "productionpw";

    private static final String DATA_APP = "jakartaDataApp";

    @ClassRule
    public static PostgreSQLContainer postgre = new PostgreSQLContainer("postgres:17.0-alpine")
                    .withDatabaseName(POSTGRES_DB)
                    .withUsername(POSTGRES_USER)
                    .withPassword(POSTGRES_PASS)
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "postgre"));

    @Server("jakartaDataServer")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        // Create tables for the DB and insert some test rows
        // NOTE that by default liberty prepends `wlp` to the table names
        try (Connection conn = postgre.createConnection("")) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE users( id BIGSERIAL NOT NULL, email VARCHAR (255), name VARCHAR (255), PRIMARY KEY (id) );");
            stmt.execute("CREATE SCHEMA premadeschema");
            stmt.close();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO users (email,name) values (?,?)");
            ps.setString(1, "jo@email.com");
            ps.setString(2, "jo");
            ps.executeUpdate();
            ps = conn.prepareStatement("INSERT INTO users (email,name) values (?,?)");
            ps.setString(1, "james@email.com");
            ps.setString(2, "james");
            ps.executeUpdate();
        }

        // Set server environment variables
        Map<String, String> envVars = new HashMap<>();
        envVars.put("DB_USER", postgre.getUsername());
        envVars.put("DB_PASSWORD", postgre.getPassword());
        envVars.put("DB_PORT", Integer.toString(postgre.getFirstMappedPort()));
        envVars.put("DB_HOST", postgre.getHost());

        ShrinkHelper.defaultApp(server, DATA_APP, "io.openliberty.checkpoint.data.app", "io.openliberty.checkpoint.data.entity", "io.openliberty.checkpoint.data.repo");

        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer();
        server.addEnvVarsForCheckpoint(envVars);
        server.checkpointRestore();
    }

    @Test
    public void testJakartaDataGetUsers() throws InterruptedException, IOException {
        String response = HttpUtils.getHttpResponseAsString(server, "jakartaDataApp/app/users");
        assertTrue("Response does not have jo:" + response, response.contains("jo@email.com"));
        assertTrue("Response does not have james:" + response, response.contains("james@email.com"));
    }

    @AfterClass
    public static void stopServer() throws Exception {
        server.stopServer();
    }
}
