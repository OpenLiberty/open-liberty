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
package com.ibm.ws.jaxrs20.fat.providercache;

import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class ProviderCacheTest {

    @Server("com.ibm.ws.jaxrs.fat.providercache")
    public static LibertyServer server;

    private static HttpClient client;
    private static final String providercachewar = "providerCache";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, providercachewar, "com.ibm.ws.jaxrs.fat.providercache");

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

    private final String mappedUri = getBaseTestUri(providercachewar, "rest", "test1");

    @Test
    public void testPOSTPerson() throws IOException {
        HttpPost postMethod = new HttpPost(mappedUri + "/post1");
        StringEntity entity = new StringEntity("{\"data1\":\"data1\",\"data2\":1,\"data3\":true}");
        entity.setContentType("application/json");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);

        assertStatesExsited(5000, new String[] {
                                                "TestEntityMessageBodyReaderWriter#isReadable",
                                                "isReadable",
                                                "readFrom",
                                                "post1",
                                                "TestEntityMessageBodyReaderWriter#isWriteable",
                                                "writeTo"
        });

        assertEquals(200, resp.getStatusLine().getStatusCode());
    }

    private void assertStatesExsited(long timeout, String... states) {
        String findStr = null;
        if (states != null && states.length != 0) {
            for (String state : states) {
                findStr = server.waitForStringInLog(state, timeout);
                assertTrue("Unable to find the output [" + state + "]  in the server log", findStr != null);
            }
        }
    }
}