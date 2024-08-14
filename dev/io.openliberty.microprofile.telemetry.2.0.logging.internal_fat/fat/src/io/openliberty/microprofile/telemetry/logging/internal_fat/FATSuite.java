/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.logging.internal_fat;

import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(Suite.class)
@SuiteClasses({
                TelemetryMessagesTest.class,
                TelemetryMessagesCheckpointTest.class,
                TelemetryFFDCTest.class,
                TelemetryFFDCCheckpointTest.class,
                TelemetryTraceTest.class,
                TelemetryTraceCheckpointTest.class,
                TelemetrySourcesTest.class,
                TelemetryApplicationConfigTest.class,
                TelemetryDropinsTest.class
})

public class FATSuite {

    public static RepeatTests testRepeatMPTel20() {
        return TelemetryActions
                        .repeat(FeatureReplacementAction.ALL_SERVERS, MicroProfileActions.MP70_EE11, MicroProfileActions.MP70_EE10,
                                TelemetryActions.MP50_MPTEL20_JAVA8, TelemetryActions.MP41_MPTEL20, TelemetryActions.MP14_MPTEL20);
    }

    private static final int CONN_TIMEOUT = 10;

    static void hitWebPage(LibertyServer server, String contextRoot, String servletName, boolean failureAllowed,
                           String params) throws MalformedURLException, IOException, ProtocolException, InterruptedException {
        try {
            String urlStr = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + contextRoot + "/" + servletName;
            urlStr = params != null ? urlStr + params : urlStr;
            URL url = new URL(urlStr);
            int expectedResponseCode = failureAllowed ? HttpURLConnection.HTTP_INTERNAL_ERROR : HttpURLConnection.HTTP_OK;
            HttpURLConnection con = HttpUtils.getHttpConnection(url, expectedResponseCode, CONN_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String line = br.readLine();
            // Make sure the server gave us something back
            assertNotNull(line);
            con.disconnect();
        } catch (IOException e) {
            // A message about a 500 code may be fine
            if (!failureAllowed) {
                throw e;
            }
        }
    }
}