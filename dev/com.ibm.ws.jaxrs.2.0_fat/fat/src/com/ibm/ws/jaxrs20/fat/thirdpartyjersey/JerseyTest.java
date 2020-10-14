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
package com.ibm.ws.jaxrs20.fat.thirdpartyjersey;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
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
public class JerseyTest {

    @Server("com.ibm.ws.jaxrs.fat.thirdpartyjersey")
    public static LibertyServer server;

    private static final String jersey = "publish/shared/resources/jersey/";
    private static final String warpf = "thirdpartyjerseypf";
    private static final String warpl = "thirdpartyjerseypl";

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive jerseyPF = ShrinkHelper.buildDefaultApp(warpf, "com.ibm.ws.jaxrs.fat.jersey");
        jerseyPF.addAsLibraries(new File(jersey).listFiles());
        ShrinkHelper.exportAppToServer(server, jerseyPF);
        server.addInstalledAppForValidation(warpf);

        WebArchive jerseyPL = ShrinkHelper.buildDefaultApp(warpl, "com.ibm.ws.jaxrs.fat.jersey");
        jerseyPL.addAsLibraries(new File(jersey).listFiles());
        ShrinkHelper.exportAppToServer(server, jerseyPL);

        server.addInstalledAppForValidation(warpf);
        server.addInstalledAppForValidation(warpl);

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
    public void testRS20ServerwithJerseyLibParentFirst() throws IOException {
        runParentFirstGetMethod(200, "Hello World");
    }

    @Test
    public void testRS20ServerwithJerseyLibParentLast() throws IOException {
        runParentLastGetMethod(200, "Hello World");
    }

    private StringBuilder runParentFirstGetMethod(int exprc, String testOut)
                    throws IOException {
        URL url = new URL("http://localhost:" + getPort()
                          + "/thirdpartyjerseypf/rest/helloworld");
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

    private StringBuilder runParentLastGetMethod(int exprc, String testOut) throws IOException {
        URL url = new URL("http://localhost:" + getPort()
                          + "/thirdpartyjerseypl/rest/helloworld");
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