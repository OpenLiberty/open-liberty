/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat;

import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Test to mpOpenAPI-1.0 feature tolerates other EE8 features and mpConfig-1.2
 */
@RunWith(FATRunner.class)
public class EE8CompatabilityTest extends FATServletClient {

    @Server("compatabilityServer")
    public static LibertyServer server;
    
    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer("EE8CompatabilityTest.log", true);
        assertNotNull("Web application is not available at /openapi/",
                      server.waitForStringInLog("CWWKT0016I.*/openapi/")); // wait for /openapi/ endpoint to become available
        assertNotNull("Web application is not available at /openapi/ui/",
                      server.waitForStringInLog("CWWKT0016I.*/openapi/ui/")); // wait for /openapi/ui/ endpoint to become available
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }   
    
    @Test
    public void testDocsEndpoint() throws Exception {
        // Check that the /openapi endpoint is available.
        final OpenAPIConnection docsConnection = OpenAPIConnection.openAPIDocsConnection(server, false);
        checkConnectionIsOK(docsConnection);
    }

    @Test
    public void testOpenAPUIEndpoint() throws Exception {
        // Check that the /openapi/ui endpoint is available.
        final OpenAPIConnection uiConnection = OpenAPIConnection.openAPIUIConnection(server, false);
        checkConnectionIsOK(uiConnection);
    }

    private void checkConnectionIsOK(OpenAPIConnection c) throws Exception {
        c.expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();
    }

}