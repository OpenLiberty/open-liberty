/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cloudant.fat;

import static com.ibm.ws.cloudant.fat.FATSuite.cloudant;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class CloudantDemoTest extends FATServletClient {

    @Server("com.ibm.ws.cloudant.demo")
    public static LibertyServer server;

    public static final String APP_NAME = "cloudantapp";
    public static final String DB_NAME = "demodb";

    @BeforeClass
    public static void setUp() throws Exception {
        server.addEnvVar("CLOUDANT_URL", cloudant.getURL(false));
        server.addEnvVar("CLOUDANT_USER", cloudant.getUser());
        server.addEnvVar("CLOUDANT_PASS", cloudant.getPassword());
        server.addEnvVar("CLOUDANT_DBNAME", DB_NAME);

        cloudant.createDb(DB_NAME);

        ShrinkHelper.defaultApp(server, APP_NAME, "demo.web");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testClientBuilder() throws Exception {
        runTest();
    }

    @Test
    public void testDirectLookup() throws Exception {
        runTest();
    }

    @Test
    public void testInjectDatabase() throws Exception {
        runTest();
    }

    @Test
    public void testResourceRef() throws Exception {
        runTest();
    }

    private void runTest() throws Exception {
        runTest(server, APP_NAME + '/', testName.getMethodName() + "&databaseName=" + DB_NAME);
    }
}
