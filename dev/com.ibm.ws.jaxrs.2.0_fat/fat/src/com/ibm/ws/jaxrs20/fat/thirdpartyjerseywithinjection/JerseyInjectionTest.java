/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.thirdpartyjerseywithinjection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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
public class JerseyInjectionTest {

    @Server("com.ibm.ws.jaxrs.fat.thirdpartyjerseywithinjection")
    public static LibertyServer server;
    private static HttpClient client;

    private static final String jersey = "publish/shared/resources/jerseywithinjection/";
    private static final String warpf = "thirdpartyjerseypfwithinjection";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive jerseyPF = ShrinkHelper.buildDefaultApp(warpf, "com.ibm.ws.jaxrs.fat.jerseywithinjection");
        jerseyPF.addAsLibraries(new File(jersey).listFiles());
        ShrinkHelper.exportAppToServer(server, jerseyPF);
        server.addInstalledAppForValidation(warpf);

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
        server.stopServer("CWWKW1002W");
    }

    private int getPort() {
        return server.getHttpDefaultPort();
    }
    @Before
    public void getHttpClient() {
        client = new DefaultHttpClient();
    }

    @After
    public void resetHttpClient() {
        client.getConnectionManager().shutdown();
    }

    @Test
    public void testRS20ServerwithJerseyInjectGet() throws IOException {
        runtGetMethod(200, "Hello World", "WSJdbcDataSource", "/thirdpartyjerseypfwithinjection/rest/helloworld");
    }

    @Test
    public void testRS20ServerwithJerseyResourceGet() throws IOException {
        runtGetMethod(200, "Hello World2", "WSJdbcDataSource","/thirdpartyjerseypfwithinjection/rest2/helloworld2");
    }

    @Test
    public void testRS20ServerwithJerseyInjectMBRMBW() throws IOException {
        runPostMethod("/thirdpartyjerseypfwithinjection/rest/helloworld/post1");
    }
    @Test
    public void testRS20ServerwithJerseyResourceMBRMBW() throws IOException {
        runPostMethod("/thirdpartyjerseypfwithinjection/rest2/helloworld2/post1");
    }

    public void runPostMethod(String path) throws IOException {
        HttpPost postMethod = new HttpPost("http://localhost:" + getPort() + path);
        StringEntity entity = new StringEntity("{\"data1\":\"data1\",\"data2\":1,\"data3\":true}");
        entity.setContentType("application/json");
        postMethod.setEntity(entity);

        HttpResponse resp = client.execute(postMethod);

        assertStatesExsited(5000, new String[] {
                                                "isReadable",
                                                "readFrom",
                                                "isWriteable",
                                                "writeTo",
                                                "Hello World",
                                                "WSJdbcDataSource"
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

    private StringBuilder runtGetMethod(int exprc, String testOut, String testOut2, String path)
                    throws IOException {
        URL url = new URL("http://localhost:" + getPort() + path);
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

            assertStatesExsited(5000, new String[] {testOut2});

            if (retcode != exprc)
                fail("Bad return Code from Get. Expected " + exprc + "Got"
                        + retcode);

            return lines;
        } finally {
            con.disconnect();
        }
    }
}