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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@SkipForRepeat("jaxws-2.3")
public class CatalogFacilityTest {

    @Server("CatalogFacilityTestServer")
    public static LibertyServer server;

    private static final int CONN_TIMEOUT = 5;
    private static String wsdlLocation;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "calculator", "com.ibm.samples.jaxws.catalog.server");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer("CatalogFacilityTest.log");
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        assertNotNull("Application calculator does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*calculator"));
        wsdlLocation = new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/calculator/calculator?wsdl").toString();
    }

    @Test
    public void testCatalog() throws Exception {

        URL url;

        url = new URL(wsdlLocation);
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);

        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();
        assertNotNull("The output is null", line);
        assertTrue("The output did not contain the automatically installed message",
                   line.contains("xml version="));

        assertNotNull("Warning for using absolute WSDL should be output", server.waitForStringInLog("CWWKW0056W:.*com.ibm.samples.jaxws.catalog.server.CalculatorService"));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

}
