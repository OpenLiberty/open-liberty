/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
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
package com.ibm.ws.testcontainers.example;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.generic.ContainersTestServlet;

import org.junit.ClassRule;
import componenttest.rules.SkipJavaSemeruWithFipsEnabled;


/**
 * Example test class showing how to use a predefined TestCotnainer
 * Note: The annotation @SkipJavaSemeruWithFipsEnabled.SkipJavaSemeruWithFipsEnabledRule is used only when the build environment is eanbled with FIPS,
 * which indicates the test is excluded as out of scope for our internal FIPS compliant testing due to sample usage.
 */
@SkipJavaSemeruWithFipsEnabled.SkipJavaSemeruWithFipsEnabledRule
@RunWith(FATRunner.class)
public class BasicTest extends FATServletClient {

    public static final String APP_NAME = "app";

    @Server("build.example.testcontainers")
    @TestServlet(servlet = ContainersTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    static PostgreSQLContainer<?> container = FATSuite.container;

    @ClassRule
    public static final SkipJavaSemeruWithFipsEnabled skipJavaSemeruWithFipsEnabled = new SkipJavaSemeruWithFipsEnabled("build.example.testcontainers");

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "web.generic");

        /*
         * Use server.addEnvVar() to pass any variables from the container that is needed
         * within server.xml, or by the application.
         */
        server.addEnvVar("PS_URL", container.getJdbcUrl());
        server.addEnvVar("PS_USER", container.getUsername());
        server.addEnvVar("PS_PASSWORD", container.getPassword());

        server.startServer();

        runTest(server, APP_NAME, "setupDatabase");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
