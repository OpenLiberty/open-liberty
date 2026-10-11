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
package io.openliberty.restfulWS40.fat;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.restfulWS40.fat.rest40examples.Rest40ExamplesTestServlet;

/**
 * Tests for Jakarta REST 4.0 examples including:
 * - Basic CRUD operations with ProductResource
 * - getMatchedResourceTemplate() feature (new in REST 4.0)
 * - JSON Merge Patch support (new in REST 4.0)
 */
@RunWith(FATRunner.class)
public class Rest40ExamplesTest extends FATServletClient {

    private static final String APP_NAME = "rest40examples";

    @Server("io.openliberty.restfulWS.4.0.examples.fat")
    @TestServlet(servlet = Rest40ExamplesTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, APP_NAME,
                                                       "io.openliberty.restfulWS40.fat.rest40examples");

        // Start server without clean start to preserve logs
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop server and archive logs for debugging
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKG0075E");
        }
    }
}

// Made with Bob
