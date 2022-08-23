/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.test;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests appSecurity-5.0
 */
@RunWith(FATRunner.class)
public class SimplestAnnotatedTest {

    private static final Logger LOG = Logger.getLogger(SimplestAnnotatedTest.class.getName());
    private static final String APP_NAME = "SimplestAnnotated";

    @Server("io.openliberty.security.jakartasec-3.0_fat.rp")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "oidc.servlets");

        // TODO: Use CommonTests framework to create and start the RP server
        server.startServer(SimplestAnnotatedTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testSimplestAnnotatedServlet() throws Exception {
        // TODO: Perform a full OIDC flow. Uncomment to drive Jakarta Security 3.0's CDI extension.
//        try {
//            HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/OidcAnnotatedServlet", "Hello world!");
//        } catch (Exception e) {
//        }
    }
}
