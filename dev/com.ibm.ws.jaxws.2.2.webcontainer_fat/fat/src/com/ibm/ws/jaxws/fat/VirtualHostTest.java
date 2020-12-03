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
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class VirtualHostTest {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(new FeatureReplacementAction().addFeature("jaxws-2.3").removeFeature("jaxws-2.2").removeFeature("jsp-2.2").removeFeature("servlet-3.1").withID("jaxws-2.3"));

    @Server("com.ibm.ws.jaxws.virtualhost")
    public static LibertyServer server;

    private static final String virtualhostWar = "virtualhost";

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(server, virtualhostWar, "com.ibm.ws.jaxws.virtualhost.services");

        server.startServer(true);
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
        runGetMethod(200, "demo1");
    }

    private StringBuilder runGetMethod(int exprc, String testOut) throws IOException {
        URL url = new URL("http://localhost:" + getPort()
                          + "/virtualhost/Endpoint");
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

            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line);

            if (lines.indexOf("hello demo1") < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

}