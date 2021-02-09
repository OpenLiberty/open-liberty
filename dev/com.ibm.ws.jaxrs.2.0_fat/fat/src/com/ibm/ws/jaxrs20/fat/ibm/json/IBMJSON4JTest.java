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
package com.ibm.ws.jaxrs20.fat.ibm.json;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * The primary purpose of this test is to make sure that
 * applications can access the APIs in the com.ibm.json4j bundle.
 * Not intended to test the com.ibm.json.* code.
 */
@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class IBMJSON4JTest {

    @Server("com.ibm.ws.jaxrs.fat.ibmjson4j")
    public static LibertyServer server;

    private static HttpClient client;
    private static final String jsonwar = "ibmjson4j";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, jsonwar, "com.ibm.ws.jaxrs.fat.ibm.json");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void getHttpClient() {
        client = new DefaultHttpClient();
    }

    @After
    public void resetHttpClient() {
        client.getConnectionManager().shutdown();
    }

    private static String BASE_JSON4J_URL = getBaseTestUri(jsonwar, "music", "listsongs");

    @Test
    public void testAccessJSONObject() throws Exception {
        HttpGet get = new HttpGet(BASE_JSON4J_URL);
        HttpResponse resp = client.execute(get);

        String strResp = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        // The order of items put in the JSONObject isn't necessarily preserved--
        // so that's why we're only checking for the presence of one item, versus
        // all of them. Anyway, this test is more concerned that it is not getting
        // a 404 due to a ClassNotFoundException for any com.ibm.json.* classes.
        assertTrue(strResp.contains("\"Hootchie Cootchie Man\":\"Junior Wells\""));
    }

    @Test
    public void testAccessJSONArray() throws Exception {
        HttpGet get = new HttpGet(BASE_JSON4J_URL + "/array");
        HttpResponse resp = client.execute(get);

        String strResp = asString(resp);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        String expectedAppend = "\"Me and Mr. Jones\":\"Amy Winehouse\"";
        assertTrue(strResp.contains(expectedAppend));
    }
}
