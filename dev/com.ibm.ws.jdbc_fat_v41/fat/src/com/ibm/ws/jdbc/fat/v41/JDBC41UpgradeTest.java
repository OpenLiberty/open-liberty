/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.v41;

import static com.ibm.ws.jdbc.fat.v41.FATSuite.appName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class JDBC41UpgradeTest extends FATServletClient {
    private static final String servlet_basic = "BasicTestServlet";

    @Server("com.ibm.ws.jdbc.fat.v41")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.setServerConfigurationFile("server_derby40.xml");
        server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @ExpectedFFDC({ "java.sql.SQLFeatureNotSupportedException" })
    @Test
    public void testJDBC41Tolerance40Driver() throws Exception {
        runTest(server, appName + '/' + servlet_basic, testName);
    }

    @Test
    public void testJDBCVersionLimiting() throws Exception {
        runTest(server, appName + '/' + servlet_basic, testName.getMethodName() + "&expectedVersion=4.0");
    }
}
