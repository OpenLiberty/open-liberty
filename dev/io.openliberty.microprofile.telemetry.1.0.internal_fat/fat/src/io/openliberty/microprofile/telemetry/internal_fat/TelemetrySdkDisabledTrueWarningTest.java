/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.HttpMethod;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import junit.framework.Assert;

/**
 * Test to verify that only a singular CWMOT5100I is emitted for each unique internal WAB/application
 * i.e. health, metrics, rest handler ,etc.
 *
 * This issue only affects mpTelemetry-2.0 when used with HTTP Metrics (supported only in mpTelemetry-2.0)
 */
@RunWith(FATRunner.class)
public class TelemetrySdkDisabledTrueWarningTest extends FATServletClient {

    private static final String CLASS_NAME = TelemetrySdkDisabledTrueWarningTest.class.getName();

    private static final String CWMOT5100I = "CWMOT5100I";

    public static final String SERVER_NAME = "Telemetry10sdkDisbledTrueWarning";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = TelemetryActions.telemetry20Repeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void uniqeCWMOT5100ITest() throws Exception {
        Assert.assertTrue(server.isStarted());

        //Startup check.
        validateCWMOT5100Iwarning();

        //hit metrics
        requestHttpServlet("/metrics");
        //hit health
        requestHttpServlet("/health");

        //runtime check after hitting endpoints
        validateCWMOT5100Iwarning();

    }

    private void validateCWMOT5100Iwarning() throws Exception {
        Set<String> warningsIssued = new HashSet<String>();
        for (String warning : server.findStringsInLogs(CWMOT5100I)) {

            //returns false if string already exists in set
            if (!warningsIssued.add(warning.split(CWMOT5100I)[1])) {
                Assert.fail(String.format("Detected duplciate %s warning: %s", CWMOT5100I, warning));
            }
        }
    }

    protected String requestHttpServlet(String servletPath) {
        HttpURLConnection con = null;
        try {
            String path = "http://" + server.getHostname() + ":"
                          + server.getHttpDefaultPort() + servletPath;

            URI theURI = new URI(path);

            URL theURL = theURI.toURL();
            con = (HttpURLConnection) theURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod(HttpMethod.GET);
            String sep = System.getProperty("line.separator");
            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }

            Assert.assertEquals("Did not recieve 200 response code while connecting to " + path, 200, con.getResponseCode());

            return lines.toString();
        } catch (IOException e) {
            Log.info(this.getClass(), "requestHttpServlet", "Encountered IO exception " + e);
            return null;
        } catch (Exception e) {
            Log.info(this.getClass(), "requestHttpServlet", "Encountered an exception " + e);
            return null;
        } finally {
            if (con != null)
                con.disconnect();
        }

    }
}