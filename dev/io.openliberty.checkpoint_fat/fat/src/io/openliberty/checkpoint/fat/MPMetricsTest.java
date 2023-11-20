/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.fat;

import static io.openliberty.checkpoint.fat.FATSuite.getTestMethodNameOnly;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@CheckpointTest
public class MPMetricsTest extends FATServletClient {

    @Server("checkpointMPMetrics")
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests repeatTest = FATSuite.defaultMPRepeat("checkpointMPMetrics");

    @Before
    public void setUp() throws Exception {
        server.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        server.startServer(getTestMethodNameOnly(testName) + ".log");
    }

    public static URL createURL(LibertyServer server, String path, boolean secure) throws MalformedURLException {
        if (!path.startsWith("/"))
            path = "/" + path;
        if (secure) {
            return new URL("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + path);
        }
        return new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path);
    }

    @Test
    public void testMetricsEndpoint() throws Exception {
        server.checkpointRestore();
        assertNotNull("TCP Channel defaultHttpEndpoint-ssl has not started", server.waitForStringInLog("CWWKO0219I.*defaultHttpEndpoint-ssl"));

        //server.xml has mpMetrics authentication set to true. So /metrics shouldn't be available on http port.
        assertEquals("Expected response code not found", HttpURLConnection.HTTP_NOT_FOUND,
                     HttpUtils.getHttpConnection(server, createURL(server, "/metrics/", false).toExternalForm()).getResponseCode());

        String usernameAndPassword = "admin" + ":" + "adminpwd";
        String authorizationHeaderValue = "Basic "
                                          + java.util.Base64.getEncoder()
                                                          .encodeToString(usernameAndPassword.getBytes());

        HttpUtils.trustAllCertificates();
        // Verify /metrics is available on https port.
        HttpUtils.getHttpConnection(createURL(server, "/metrics/", true), HttpURLConnection.HTTP_OK, new int[] { HttpURLConnection.HTTP_MOVED_TEMP }, HttpUtils.DEFAULT_TIMEOUT,
                                    HTTPRequestMethod.GET, singletonMap("Authorization", authorizationHeaderValue), null);
    }

    @Test
    public void testUpdateAuthentication() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        //Setting mpMetrics authentication to false
        config.getMPMetricsElement().setAuthentication(false);
        server.updateServerConfiguration(config);
        server.checkpointRestore();
        assertNotNull("Config wasn't updated successfully",
                      server.waitForStringInLogUsingMark("CWWKG0017I.* | CWWKG0018I.*", 6000));
        // After setting authentication to false, verify /metrics is available on http port.
        HttpUtils.getHttpConnection(createURL(server, "/metrics/", false), HttpURLConnection.HTTP_OK, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
    }

    @After
    public void tearDown() throws Exception {
        server.stopServer();
    }
}
