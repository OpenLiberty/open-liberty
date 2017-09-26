/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class HealthTest {

    @Server("CDIHealth")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {

        if (!server1.isStarted()) {
            server1.startServer();
        }
        server1.waitForStringInLog("CWWKT0016I: Web application available.*health*");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }

    @Test
    public void testFeatureInstall() throws Exception {

        assertNotNull("Kernel did not start", server1.waitForStringInLog("CWWKE0002I"));
        assertNotNull("Server did not start", server1.waitForStringInLog("CWWKF0011I"));

        assertNotNull("FeatureManager should report update is complete",
                      server1.waitForStringInLog("CWWKF0008I"));
    }

    @Test
    public void testNoHealthCheckNoAppInstalled() throws Exception {

        HttpURLConnection con = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, "/health");
        assertEquals(200, con.getResponseCode());

        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));

        BufferedReader br = HttpUtils.getConnectionStream(con);
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testNoHealthCheckNoAppInstalled", "Response: jsonResponse= " + jsonResponse.toString());

        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        JsonArray testJsonArray = Json.createArrayBuilder().build(); //empty array

        assertEquals(0, checks.size());
        assertEquals(checks, testJsonArray);
        assertTrue(jsonResponse.getString("outcome").equals("UP"));
    }

    /**
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(CDIHealthCheckTest.class, method, msg);
    }
}
