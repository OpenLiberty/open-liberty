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

package com.ibm.ws.jaxrs2x.fat;


import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class HelloWorldTest {
    @Server("com.ibm.ws.jaxrs2x.fat")
    public static LibertyServer server;

    private static final String hellowar = "helloworld";

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultApp(server, hellowar, "com.ibm.ws.jaxrs2x.fat.helloworld");
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
        server.stopServer();
    }

    private int getPort() {
        return server.getHttpDefaultPort();
    }

    @Test
    public void testSimple() throws IOException {
        URL url = new URL("http://localhost:" + getPort()
                          + "/helloworld/rest/helloworld");
        runGetMethod(200, "Hello World", url);
    }

    @Test
    public void testSecondServletMappingForSameClass() throws IOException {
        URL url = new URL("http://localhost:" + getPort()
                          + "/helloworld/restSecondMapping/helloworld");
        runGetMethod(200, "Hello World", url);
    }

    private StringBuilder runGetMethod(int exprc, String testOut, URL url)
                    throws IOException {

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
}