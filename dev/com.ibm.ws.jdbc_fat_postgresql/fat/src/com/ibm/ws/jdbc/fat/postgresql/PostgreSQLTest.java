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
package com.ibm.ws.jdbc.fat.postgresql;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.postgresql.web.PostgreSQLTestServlet;

@RunWith(FATRunner.class)
public class PostgreSQLTest extends FATServletClient {
    private static final Class<?> c = PostgreSQLTest.class;

    public static final String APP_NAME = "postgresqlApp";

    @Server("postgresql-test-server")
    @TestServlet(servlet = PostgreSQLTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RequireDockerRule<PostgreSQLContainer<?>> postgre = new RequireDockerRule<>(() -> new PostgreSQLContainer<>("postgres:11.2-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("postgresUser")
                    .withPassword("superSecret")
                    .withExposedPorts(5432)
                    .withLogConsumer(PostgreSQLTest::log));

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "@AGG", "instance 1: " + postgre.get());
        Log.info(c, "@AGG", "instance 2: " + postgre.get());
        Log.info(c, "@AGG", "instance 3: " + postgre.get());
        ShrinkHelper.defaultDropinApp(server, APP_NAME, "jdbc.fat.postgresql.web");
        server.addEnvVar("POSTGRES_PORT", String.valueOf(postgre.get().getMappedPort(5432)));
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(c, "postgresql", msg);
    }
}
