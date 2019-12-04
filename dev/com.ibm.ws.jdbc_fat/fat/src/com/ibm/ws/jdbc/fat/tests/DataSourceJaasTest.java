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
package com.ibm.ws.jdbc.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class DataSourceJaasTest extends FATServletClient {

    @Server("com.ibm.ws.jdbc.jaas.fat")
    public static LibertyServer server;

    private static final String basicfat = "basicfat";

    @BeforeClass
    public static void setUp() throws Exception {
        server.configureForAnyDatabase();
        server.addInstalledAppForValidation("jdbcapp");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("DSRA9543W"); //Expected since we're using a GSS Credential for authentication with Derby.
    }

    @Test
    public void testDataSourceMappingConfigAlias() throws Exception {
        runTest(server, basicfat + '/', testName);
    }

    @Test
    public void testDataSourceCustomLoginConfiguration() throws Exception {
        runTest(server, basicfat + '/', testName);
    }

    @Test
    public void testJAASLoginWithGSSCredential() throws Exception {
        runTest(server, basicfat + '/', testName);
    }
}
