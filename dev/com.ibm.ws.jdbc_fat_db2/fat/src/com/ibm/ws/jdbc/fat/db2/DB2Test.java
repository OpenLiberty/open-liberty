/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.db2;

import java.time.Duration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.output.OutputFrame;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

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

    @ClassRule
    public static Db2Container db2 = new Db2Container()
                    .acceptLicense()
                    // Use 5m timeout for local runs, 25m timeout for remote runs (extra time since the DB2 container can be slow to start)
                    .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 5 : 25))
                    .withLogConsumer(DB2Test::log);

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(DB2Test.class, "db2", msg);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "db2.web");

        server.addEnvVar("DB2_DBNAME", db2.getDatabaseName());
        server.addEnvVar("DB2_HOSTNAME", db2.getContainerIpAddress());
        server.addEnvVar("DB2_PORT", String.valueOf(db2.getMappedPort(50000)));
        server.addEnvVar("DB2_USER", db2.getUsername());
        server.addEnvVar("DB2_PASS", db2.getPassword());

        server.startServer();

        runTest(server, APP_NAME + '/' + SERVLET_NAME, "initDatabase");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
