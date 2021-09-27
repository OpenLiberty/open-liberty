/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile41.internal.test.suite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile41.internal.test.helloworld.HelloWorldApplication;
import io.openliberty.microprofile41.internal.test.helloworld.basic.BasicHelloWorldBean;
import io.openliberty.microprofile41.internal.test.helloworld.config.ConfiguredHelloWorldBean;
import io.openliberty.microprofile41.internal.test.helloworld.health.LivenessCheck;
import io.openliberty.microprofile41.internal.test.helloworld.health.ReadinessCheck;
import io.openliberty.microprofile41.internal.test.helloworld.health.StartupCheck;

@RunWith(FATRunner.class)
public class BasicHealthTest {

    private static final String SERVER_NAME = "MPServer41";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    private static final String APP_NAME = "helloworld";

    private static final String MESSAGE = BasicHelloWorldBean.MESSAGE;

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.LATEST, MicroProfileActions.MP50);

    @BeforeClass
    public static void setUp() throws Exception {

        PropertiesAsset config = new PropertiesAsset().addProperty("message", MESSAGE);

        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(HelloWorldApplication.class.getPackage())
                                   .addPackage(ConfiguredHelloWorldBean.class.getPackage())
                                   .addPackage(LivenessCheck.class.getPackage())
                                   .addAsResource(config, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWMOT0010W"); //CWMOT0010W: OpenTracing cannot track JAX-RS requests because an OpentracingTracerFactory class was not provided or client libraries for tracing backend are not in the class path.
        }
    }

    @Test
    public void testHealth() throws IOException {
        //check the app is working
        runGetMethod(200, "/helloworld/helloworld", MESSAGE);

        //check that the status of the started, live and ready endpoints is "UP"
        assertCheckUP("/health/started", StartupCheck.NAME);
        assertCheckUP("/health/live", LivenessCheck.NAME);
        assertCheckUP("/health/ready", ReadinessCheck.NAME);

        //check the aggregated /health endpoint for the same info
        HttpURLConnection conHealth = HttpUtils.getHttpConnectionWithAnyResponseCode(server, "/health");
        assertEquals("The Response Code was not 200 for the following endpoint: " + conHealth.getURL().toString(), 200, conHealth.getResponseCode());
        JsonObject jsonResponse = getJSONPayload(conHealth);
        assertCheckUP(jsonResponse, StartupCheck.NAME);
        assertCheckUP(jsonResponse, LivenessCheck.NAME);
        assertCheckUP(jsonResponse, ReadinessCheck.NAME);
    }

    protected static void assertCheckUP(String endpoint, String checkName) throws IOException {
        HttpURLConnection conStarted = HttpUtils.getHttpConnectionWithAnyResponseCode(server, endpoint);
        assertEquals("The Response Code was not 200 for the following endpoint: " + conStarted.getURL().toString(), 200, conStarted.getResponseCode());
        JsonObject jsonResponse = getJSONPayload(conStarted);

        assertCheckUP(jsonResponse, checkName);
    }

    protected static void assertCheckUP(JsonObject json, String name) {
        JsonArray checks = (JsonArray) json.get("checks");
        for (JsonValue value : checks) {
            JsonObject jsonObj = (JsonObject) value;
            if (name.equals(jsonObj.getString("name"))) {
                assertUP(jsonObj);
            }
        }

    }

    protected static void assertUP(JsonObject check) {
        assertEquals("The status of the Startup health check was not UP for the user-defined health checks.", check.getString("status"), "UP");
    }

    protected static JsonObject getJSONPayload(HttpURLConnection con) throws IOException {
        assertEquals("application/json; charset=UTF-8", con.getHeaderField("Content-Type"));

        BufferedReader br = HttpUtils.getResponseBody(con, "UTF-8");
        Json.createReader(br);
        JsonObject jsonResponse = Json.createReader(br).readObject();
        br.close();

        Log.info(BasicHealthTest.class, "getJSONPayload", "Response: jsonResponse= " + jsonResponse.toString());
        assertNotNull("The contents of the health endpoint must not be null.", jsonResponse.getString("status"));

        return jsonResponse;
    }

    private StringBuilder runGetMethod(int exprc, String requestUri, String testOut) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br
                                                                     .readLine())
                lines.append(line).append(sep);

            if (lines.indexOf(testOut) < 0)
                fail("Missing success message in output. " + lines);

            if (retcode != exprc)
                fail("Bad return Code from Get. Expected " + exprc + "Got"
                     + retcode);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    private int getPort() {
        return server.getHttpDefaultPort();
    }

    private String getHost() {
        return server.getHostname();
    }

}
