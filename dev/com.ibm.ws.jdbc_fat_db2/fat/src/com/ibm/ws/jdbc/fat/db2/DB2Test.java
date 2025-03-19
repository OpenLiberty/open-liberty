/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.db2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Db2Container;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import db2.web.DB2TestServlet;

@RunWith(FATRunner.class)
public class DB2Test extends FATServletClient {

    public static final String APP_NAME = "db2fat";
    public static final String SERVLET_NAME = "DB2TestServlet";

    @Server("com.ibm.ws.jdbc.fat.db2")
    @TestServlet(servlet = DB2TestServlet.class, path = APP_NAME + '/' + SERVLET_NAME)
    public static LibertyServer server;

    public static Db2Container db2 = FATSuite.db2;

    @BeforeClass
    public static void setUp() throws Exception {

        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        ShrinkHelper.defaultApp(server, APP_NAME, "db2.web");

        server.addEnvVar("DB2_DBNAME", db2.getDatabaseName());
        server.addEnvVar("DB2_HOSTNAME", db2.getHost());
        server.addEnvVar("DB2_PORT", String.valueOf(db2.getMappedPort(50000)));
        server.addEnvVar("DB2_PORT_SECURE", String.valueOf(db2.getMappedPort(50001)));
        server.addEnvVar("DB2_USER", db2.getUsername());
        server.addEnvVar("DB2_PASS", db2.getPassword());

        // TODO extract security files from container prior to server start
        // TODO delete security files from git

        // Extract keystore from container
//        db2.copyFileFromContainer("/certs/db2-keystore.p12", server.getServerRoot() + "/security/db2-keystore.p12");

        // Extract server cert from container
//        db2.copyFileFromContainer("/certs/server.arm", server.getServerRoot() + "/security/server.crt");

        server.startServer();

        runTest(server, APP_NAME + '/' + SERVLET_NAME, "initDatabase");
    }

    @Test
    public void testCustomTrace() throws Exception {
        File logDir = new File(server.getLogsRoot());
        assertTrue(logDir.exists());
        assertTrue(logDir.isDirectory());

        File jccTraceFile = new File(logDir, "jcc.trace");
        assertTrue(jccTraceFile.exists());
        assertEquals(0, Files.lines(jccTraceFile.toPath()).count());

        runTest(server, APP_NAME + '/' + SERVLET_NAME, getTestMethodSimpleName());

        //TODO an additional file jcc.trace_cpds_2 is also generated, is this expected?
        // if so we could assert that file is always generated.

        assertTrue(Files.lines(jccTraceFile.toPath()).count() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
