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
package com.ibm.ws.concurrent.mp.fat;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class MPConcurrentJAXRSTest extends FATServletClient {
    private static final String APP_NAME = "MPConcurrentJAXRSApp";

    /**
     * Maximum number of nanoseconds to wait for a JAX-RS request to complete.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    private static ExecutorService testThreads;

    @Server("MPConcurrentJAXRSTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME, "concurrent.mp.fat.jaxrs.web");
        server.startServer();
        testThreads = Executors.newFixedThreadPool(5);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            testThreads.shutdownNow();
        } finally {
            server.stopServer();
        }
    }

    /**
     * Invoke JAX-RS methods that each run asynchronous operations that access URIInfo context of a class-level field.
     * Verify that URIInfo context is propagated from the main request to the asynchronous tasks.
     */
    @Test
    public void testURIInfoFromJAXRSMethodsInParallel() throws Exception {
        List<Future<JsonStructure>> futures = new ArrayList<Future<JsonStructure>>(3);
        for (String request : new String[] {
                                             "/testapp/testUriInfo/path1?q=10",
                                             "/testapp/testUriInfo/path2?q=20",
                                             "/testapp/testUriInfo/path2?q=22"
        }) {
            futures.add(testThreads.submit(() -> {
                HttpURLConnection con = HttpUtils.getHttpConnection(server, APP_NAME + request);
                con.setDoOutput(true);
                con.connect();
                try (BufferedReader reader = HttpUtils.getConnectionStream(con)) {
                    int rc = con.getResponseCode();
                    if (rc == HttpURLConnection.HTTP_OK || rc == HttpURLConnection.HTTP_PARTIAL) {
                        JsonStructure json = Json.createReader(reader).read();
                        assertEquals(json.toString(), HttpURLConnection.HTTP_OK, rc);
                        return json;
                    }
                    assertEquals(con.getResponseMessage(), HttpURLConnection.HTTP_OK, rc);
                    return null;
                } finally {
                    con.disconnect();
                }
            }));
        }

        JsonObject json;
        json = (JsonObject) futures.get(0).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        String err = "Response is " + json.toString();

        assertEquals(err, "testUriInfo/path1", json.getString("uriInfoPath"));
        assertEquals(err, "10", json.getString("uriInfoQueryParam"));

        json = (JsonObject) futures.get(1).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(err, "testUriInfo/path2", json.getString("uriInfoPath"));
        assertEquals(err, "20", json.getString("uriInfoQueryParam"));

        json = (JsonObject) futures.get(2).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        assertEquals(err, "testUriInfo/path2", json.getString("uriInfoPath"));
        assertEquals(err, "22", json.getString("uriInfoQueryParam"));
    }
}
