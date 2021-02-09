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
package com.ibm.ws.jaxrs20.fat.jacksonJsonIgnore;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class JacksonJsonIgnoreTest {

    @Server("com.ibm.ws.jaxrs.fat.jacksonJsonIgnore")
    public static LibertyServer server;

    private static HttpClient client;
    private static final String jacksonwar = "jacksonJsonIgnore";
    private static final String jackson = "publish/shared/resources/jackson2x/";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(jacksonwar, "com.ibm.ws.jaxrs.fat.jacksonJsonIgnore");
        app.addAsLibraries(new File(jackson).listFiles());
        ShrinkHelper.exportAppToServer(server, app);
        server.addInstalledAppForValidation(jacksonwar);

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

    private final String mappedUri = getBaseTestUri(jacksonwar, "pojo", "test");

    @Test
    public void testJsonIgnore() throws IOException {
        HttpGet getMethod = new HttpGet(mappedUri);
        HttpResponse resp = client.execute(getMethod);
        assertEquals(200, resp.getStatusLine().getStatusCode());
        // The original wink test expected there to be a space after the comma
        assertEquals("{\"fish\":\"fish::{\\\"fish\\\":\\\"fish\\\"}::\\n\"}", asString(resp));
    }

}