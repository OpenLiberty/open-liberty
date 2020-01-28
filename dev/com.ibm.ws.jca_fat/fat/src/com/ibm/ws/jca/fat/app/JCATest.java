/*******************************************************************************
 * Copyright (c) 2011,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.fat.app;

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

import componenttest.annotation.Server;
import componenttest.topology.impl.LibertyServer;

/**
 * General tests that don't involve updating configuration while the server is running.
 */
public class JCATest {

    private static final String fvtapp = "fvtapp";
    private static final String fvtweb = "fvtweb";

    @Server("com.ibm.ws.jca.fat")
    public static LibertyServer server;

    /**
     * Utility method to run a test on JCAFVTServlet.
     *
     * @param test Test name to supply as an argument to the servlet
     * @return output of the servlet
     * @throws IOException if an error occurs
     */
    private StringBuilder runInServlet(String test, String webmodule) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + webmodule + "?test=" + test);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server.addInstalledAppForValidation(fvtapp);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0701E.*com.ibm.ws.jca.resourceAdapter.properties", // occurs when Derby shutdown on FVTResourceAdapter.stop holds up deactivate for too long
                          "CWWKE0700W.*com.ibm.ws.jca.resourceAdapter.properties", // occurs when Derby shutdown on FVTResourceAdapter.stop holds up deactivate for too long
                          "WTRN0062E", //expected by testEnableSharingForDirectLookupsFalse
                          "J2CA0030E", //expected by testEnableSharingForDirectLookupsFalse
                          "J2CA0074E.*cf6"); //expected by testEnableSharingForDirectLookupsFalse
    }

    @Test
    public void testActivationSpec() throws Exception {
        runInServlet("testActivationSpec", fvtweb);
    }

    @Test
    public void testActivationSpecBindings() throws Exception {
        runInServlet("testActivationSpecBindings", fvtweb);
    }

    @Test
    public void testDestinations() throws Exception {
        runInServlet("testDestinations", fvtweb);
    }

    /**
     * Test calls runInServlet method twice because this test is related
     * to a defect where forgetting to close a connection caused an error
     * in a future transaction.
     */
    @Test
    public void testMissingCloseInServlet() throws Exception {
        runInServlet("testMissingCloseInServlet", fvtweb);
        runInServlet("testMissingCloseInServlet", fvtweb);
    }

    @Test
    public void testEnableSharingForDirectLookupsFalse() throws Exception {
        runInServlet("testEnableSharingForDirectLookupsFalse", fvtweb);
    }

}
