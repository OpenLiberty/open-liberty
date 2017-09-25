/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.microprofile.health.fat;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.*;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class CDIHealthCheckTest {

    @Server("CDIHealth")
    public static LibertyServer server1;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive testingApp = ShrinkWrap.create(WebArchive.class,
                                                  "HealthCheckTestApp.war")
                        .addPackage("com.ibm.ws.microprofile.health.testapp");
        ShrinkHelper.exportToServer(server1, "dropins", testingApp);

        if (!server1.isStarted()) {
            server1.startServer();
        }
        server1.waitForStringInLog("CWWKT0016I: Web application available.*health*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1.stopServer();
    }

    public void testJsonReceived() throws Exception {

        URL healthURL = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/health");

        HttpURLConnection con = HttpUtils.getHttpConnection(healthURL, 200, 10 * 1000);

        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));
        BufferedReader br = HttpUtils.getConnectionStream(con);
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testJsonReceived", "Response: jsonResponse= " + jsonResponse.toString());
        assertNotNull("need to retieve contents of url", jsonResponse.getString("outcome"));
        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        assertEquals(1, checks.size());
        assertEquals("testJsonReceived", ((JsonObject) checks.get(0)).getString("name"));
        assertEquals(jsonResponse.getString("outcome"), "UP");
    }

    @Test
    public void testSingleHealthChecks() throws Exception {
        testJsonReceived();
        testSingleOutcomeUP();
        testSingleOutcomeDOWN();
        testCheckUPWithData();
        testCheckDOWNWithData();
    }

    public void testSingleOutcomeUP() throws Exception {

        URL healthURL = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/health");

        HttpURLConnection con = HttpUtils.getHttpConnection(healthURL, 200, 10 * 1000);

        BufferedReader br = HttpUtils.getConnectionStream(con);
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testSingleOutcomeUP", "Response: jsonResponse= " + jsonResponse.toString());
        //assertNotNull("need to retieve contents of url", jsonResponse.getString("outcome"));
        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        assertEquals(1, checks.size());
        assertEquals("testSingleOutcomeUP", ((JsonObject) checks.get(0)).getString("name"));
        assertEquals("UP", ((JsonObject) checks.get(0)).getString("state"));
        assertEquals(jsonResponse.getString("outcome"), "UP");
    }

    public void testSingleOutcomeDOWN() throws Exception {

        URL healthURL = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/health");

        HttpURLConnection con = null;
        con = HttpUtils.getHttpConnection(healthURL, 503, 10 * 1000);
        assertEquals(503, con.getResponseCode());

        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));
        BufferedReader br = HttpUtils.getResponseBody(con, "UTF-8");
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testSingleOutcomeDOWN", "Response: jsonResponse= " + jsonResponse.toString());
        assertNotNull("need to retieve contents of url", jsonResponse.getString("outcome"));
        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        assertEquals(1, checks.size());
        assertEquals("testSingleOutcomeDOWN", ((JsonObject) checks.get(0)).getString("name"));
        assertEquals("DOWN", ((JsonObject) checks.get(0)).getString("state"));
        assertEquals(jsonResponse.getString("outcome"), "DOWN");
    }

    public void testCheckUPWithData() throws Exception {

        URL healthURL = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/health");
        HttpURLConnection con = HttpUtils.getHttpConnection(healthURL, 200, 10 * 1000);

        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));
        BufferedReader br = HttpUtils.getConnectionStream(con);
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testCheckUPWithData", "Response: jsonResponse= " + jsonResponse.toString());

        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        assertEquals(1, checks.size());
        assertEquals("testCheckUPWithData", ((JsonObject) checks.get(0)).getString("name"));
        assertEquals("UP", ((JsonObject) checks.get(0)).getString("state"));
        JsonObject data = ((JsonObject) checks.get(0)).getJsonObject("data");
        assertEquals("online", data.getString("CPU"));
        assertEquals("functional", data.getString("Fan"));

        assertEquals(jsonResponse.getString("outcome"), "UP");
    }

    public void testCheckDOWNWithData() throws Exception {

        URL healthURL = new URL("http://" + server1.getHostname() + ":" + server1.getHttpDefaultPort() + "/health");
        HttpURLConnection con = HttpUtils.getHttpConnection(healthURL, 503, 10 * 1000);
        assertEquals(503, con.getResponseCode());
        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));
        BufferedReader br = HttpUtils.getResponseBody(con, "UTF-8");
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();
        log("testCheckDOWNWithData", "Response: jsonResponse= " + jsonResponse.toString());

        JsonArray checks = (JsonArray) jsonResponse.get("checks");

        assertEquals(1, checks.size());
        assertEquals("testCheckDOWNWithData", ((JsonObject) checks.get(0)).getString("name"));
        assertEquals("DOWN", ((JsonObject) checks.get(0)).getString("state"));
        JsonObject data = ((JsonObject) checks.get(0)).getJsonObject("data");
        assertEquals("offline", data.getString("CPU"));
        assertEquals("failed", data.getString("Fan"));

        assertEquals(jsonResponse.getString("outcome"), "DOWN");
    }

    /**
     * helper for simple logging.
     */
    private static void log(String method, String msg) {
        Log.info(CDIHealthCheckTest.class, method, msg);
    }
}
