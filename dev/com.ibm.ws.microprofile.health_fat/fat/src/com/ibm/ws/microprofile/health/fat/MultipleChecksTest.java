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
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class MultipleChecksTest {

    @Server("CDIHealthMultiple")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive testingApp1 = ShrinkWrap.create(WebArchive.class,
                                                   "MultipleHealthCheckApp1.war")
                        .addPackages(true, "com.ibm.ws.microprofile.health.multiple.testapp1");
        ShrinkHelper.exportToServer(server1, "dropins", testingApp1);

        WebArchive testingApp2 = ShrinkWrap.create(WebArchive.class,
                                                   "MultipleHealthCheckApp2.war")
                        .addPackages(true, "com.ibm.ws.microprofile.health.multiple.testapp2");
        ShrinkHelper.exportToServer(server1, "dropins", testingApp2);

        WebArchive testingApp3 = ShrinkWrap.create(WebArchive.class,
                                                   "MultipleHealthCheckApp3.war")
                        .addPackages(true, "com.ibm.ws.microprofile.health.multiple.testapp3");
        ShrinkHelper.exportToServer(server1, "dropins", testingApp3);

        WebArchive hcnoCDIapp = ShrinkWrap.create(WebArchive.class,
                                                  "HealthCheckNoCDIApp.war")
                        .addPackages(true, "com.ibm.ws.microprofile.health.nocdi.testapp");
        ShrinkHelper.exportToServer(server1, "dropins", hcnoCDIapp);

        if (!server1.isStarted()) {
            server1.startServer();
        }
        server1.waitForStringInLog("CWWKT0016I: Web application available.*health*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer("CWMH0051W");
    }

    @Test
    public void testMultipleHealthChecks() throws Exception {
        testMultipleUPChecks();
        testMultipleChecksDOWN();
    }

    public void testMultipleUPChecks() throws Exception {

        //copyFiles();

        URL healthURL = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/health");
        //HttpURLConnection con = HttpUtils.getHttpConnection(healthURL);
        HttpURLConnection con = HttpUtils.getHttpConnectionWithAnyResponseCode(server1, "/health");
        assertEquals(200, con.getResponseCode());

        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));

        BufferedReader br = HttpUtils.getConnectionStream(con);
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testAllUPChecks", "Response: jsonResponse= " + jsonResponse.toString());

        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        assertEquals(3, checks.size());

        Set<String> checkObjNames = new HashSet<String>(checks.size());
        checkObjNames.add(((JsonObject) checks.get(0)).getString("name"));
        checkObjNames.add(((JsonObject) checks.get(1)).getString("name"));
        checkObjNames.add(((JsonObject) checks.get(2)).getString("name"));

        assertTrue(checkObjNames.contains("testMultipeUPChecks1"));
        assertTrue(checkObjNames.contains("testMultipeUPChecks2"));
        assertTrue(checkObjNames.contains("testMultipeUPChecks3"));

        Iterator it = checks.iterator();
        while (it.hasNext()) {
            JsonObject jsonObj = (JsonObject) it.next();
            assertEquals("UP", jsonObj.getString("state"));
            if ((jsonObj.getString("name")).equals("testMultipeUPChecks1")) {
                assertEquals("UP", jsonObj.getString("state"));
                JsonObject data = jsonObj.getJsonObject("data");
                assertEquals("online", data.getString("CPU"));
                assertEquals("functional", data.getString("Fan"));
            } else {
                assertEquals("UP", jsonObj.getString("state"));
            }
        }
        assertEquals(jsonResponse.getString("outcome"), "UP");
    }

    public void testMultipleChecksDOWN() throws Exception {

        URL healthURL = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/health");
        HttpURLConnection con = HttpUtils.getHttpConnection(healthURL, 503, 10 * 1000);
        assertEquals(503, con.getResponseCode());
        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));

        BufferedReader br = HttpUtils.getResponseBody(con, "UTF-8");
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testAllUPChecks", "Response: jsonResponse= " + jsonResponse.toString());

        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        assertEquals(3, checks.size());

        Set<String> checkObjNames = new HashSet<String>(checks.size());
        checkObjNames.add(((JsonObject) checks.get(0)).getString("name"));
        checkObjNames.add(((JsonObject) checks.get(1)).getString("name"));
        checkObjNames.add(((JsonObject) checks.get(2)).getString("name"));

        assertTrue(checkObjNames.contains("testMultipeDOWNChecks1"));
        assertTrue(checkObjNames.contains("testMultipeUPChecks2"));
        assertTrue(checkObjNames.contains("testMultipeUPChecks3"));

        Iterator it = checks.iterator();
        while (it.hasNext()) {
            JsonObject jsonObj = (JsonObject) it.next();
            if ((jsonObj.getString("name")).equals("testMultipeDOWNChecks1")) {
                assertEquals("DOWN", jsonObj.getString("state"));
                JsonObject data = jsonObj.getJsonObject("data");
                assertEquals("offline", data.getString("CPU"));
                assertEquals("failed", data.getString("Fan"));
            } else {
                assertEquals("UP", jsonObj.getString("state"));
            }
        }
        assertEquals(jsonResponse.getString("outcome"), "DOWN");

    }

    /**
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(CDIHealthCheckTest.class, method, msg);
    }

}
