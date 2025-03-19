/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.postgresql;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import jdbc.fat.postgresql.web.PostgreSQLAWSTestServlet;

@RunWith(FATRunner.class)
public class PostgreSQLAWSTest extends FATServletClient {

    private static final String JEE_APP = "postgresqlaws";
    private static final String SERVLET_NAME = "PostgreSQLAWSTestServlet";

    private static final String PROTOCOL_PREFIX = "jdbc:aws-wrapper:";

    //TODO can we run AWS Secret Manager or Identity and Access Management in a container here to have a full working example?
    // Consider localstack as a mock AWS environment

    private static String transformJdbcUrl(String baseUrl) {
        final String newUrl = baseUrl.replace("jdbc:", PROTOCOL_PREFIX);
        Log.info(FATSuite.class, "transformJdbcUrl", "Replacing URL: " + baseUrl + " -> " + newUrl);
        return newUrl;
    }

    @Server("server-PostgreSQLAWSTest")
    @TestServlet(servlet = PostgreSQLAWSTestServlet.class, path = JEE_APP + "/" + SERVLET_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        // Set server environment variables
        server.addEnvVar("PG_URL", transformJdbcUrl(FATSuite.postgre.getJdbcUrl()));
        server.addEnvVar("PG_USER", FATSuite.postgre.getUsername());
        server.addEnvVar("PG_PASSWORD", FATSuite.postgre.getPassword());
        server.addEnvVar("PG_DBNAME", FATSuite.postgre.getDatabaseName());
        server.addEnvVar("PG_PORT", Integer.toString(FATSuite.postgre.getFirstMappedPort()));
        server.addEnvVar("PG_HOST", FATSuite.postgre.getHost());

        // Create a normal Java EE application and export to server
        ShrinkHelper.defaultApp(server, JEE_APP, "jdbc.fat.postgresql.web");

        // Start Server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server.isStarted()) {
            server.stopServer(
                              "J2CA0046E" //ResourceAllocationException
            );
        }
    }
}
