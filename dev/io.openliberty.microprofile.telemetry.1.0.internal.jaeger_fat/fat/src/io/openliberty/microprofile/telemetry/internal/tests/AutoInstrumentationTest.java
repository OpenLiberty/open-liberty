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
package io.openliberty.microprofile.telemetry.internal.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class AutoInstrumentationTest extends MicroProfileTelemetryTestBase {

    private static Class<?> c = AutoInstrumentationTest.class;
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("testServer1");
    private static final int NUM_OF_CALLS = 3;

    @BeforeClass
    public static void setUp() throws Exception {

        String os = System.getProperty("os.name").toLowerCase();
        Log.info(c, "setUp", "os.name = " + os);

        clearContainerOutput();
        String jaegerHost = jaegerContainer.getHost();
        String jaegerGrpcPort = String.valueOf(jaegerContainer.getMappedPort(JAEGER_GRPC_PORT));
        Log.info(c, "setUp", "Jaeger container: host=" + jaegerHost + "  port=" + jaegerGrpcPort);
        server.addEnvVar(ENV_OTEL_SERVICE_NAME, OTEL_SERVICE_NAME_SYSTEM);
        server.addEnvVar(ENV_OTEL_TRACES_EXPORTER, OTEL_TRACES_EXPORTER_JAEGER);
        server.addEnvVar(ENV_OTEL_EXPORTER_JAEGER_ENDPOINT, "http://" + jaegerHost + ":" + jaegerGrpcPort);

        // Construct the test application
        WebArchive system = ShrinkWrap.create(WebArchive.class, "system.war");
        system.addPackages(true, "io.openliberty.guides.system");
        ShrinkHelper.exportAppToServer(server, system);
        serverStart();
    }

    @Before
    public void setUpTest() throws Exception {

        if (!server.isStarted()) {
            serverStart();
        }
        server.addInstalledAppForValidation("system");
    }

    @After
    public void tearDown() {
    }

    @AfterClass
    public static void completeTest() throws Exception {
        try {
            if (server.isStarted()) {
                Log.info(c, "competeTest", "---> Stopping server..");
                server.stopServer("TRAS4301W");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void serverStart() throws Exception {
        Log.info(c, "serverStart", "--->  Starting Server.. ");
        server.startServer();
    }

    @Override
    protected LibertyServer getServer() {
        return server;
    }

    @Test
    public void systemSpanTest() throws JSONException, InterruptedException {
        int count = NUM_OF_CALLS;
        String result = null;
        while (count > 0) {
            result = runApp(getAppUrl("system/properties"));
            count--;
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }
        Log.info(c, "systemSpanTest", "system = " + result);
        assertNotNull("result is null", result);
        // Query Jaeger for spans
        int retries = 10;
        JSONArray dataArray = null;
        do {
            Thread.sleep(1000); // Delay for 1 second
            String json = queryJaeger();
            Log.info(c, "systemSpanTest", "Jaeger json = " + json);
            assertNotNull("Jaeger returned empty json", json);
            //
            // The normal return JSON structure will be like this:
            // {
            //   "data":[
            //     {
            //       "traceID":"...",
            //       "spans":[...],
            //       "processes": { },
            //       "warnings":"..."
            //     }
            //     {
            //       "traceID":"...",
            //       "spans":[...],
            //       "processes": { },
            //       "warnings":"..."
            //     }
            //   ],
            //  "total":0,
            //  "limit":0,
            //  "offest":0,
            //  "errors":"null"
            // }
            JSONObject jobj = new JSONObject(json);
            // Make sure erorrs is null
            assertTrue("errors is not null", jobj.getString("errors").equals("null"));
            // Make sure data contains the same number of spans as the number of JAX-RS calls
            dataArray = jobj.getJSONArray("data");
            retries--;
        } while ((retries > 0) && ((dataArray == null) || (dataArray.length() < NUM_OF_CALLS)));
        assertEquals("data does not contain the expected number of spans", NUM_OF_CALLS, dataArray.length());
    }

}
