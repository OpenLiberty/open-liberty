/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;

/**
 * Test which other features start if you start mpTelemetry on its own
 */
@Mode(FULL)
@RunWith(FATRunner.class)
public class TelemetryAloneTest {

    @Server("Telemetry10Alone")
    public static LibertyServer server;

    @After
    public void stopServer() throws Exception {
        server.stopServer();
    }

    @Test
    public void testMpTelemetry10Alone() throws Exception {
        setFeature("mpTelemetry-1.0");
        server.startServer();
        String featureString = server.waitForStringInLog("CWWKF0012I"); // The server installed the following features:
        assertThat(featureString, allOf(containsString("cdi-4.0"), containsString("restfulWS-3.1")));
    }

    @Test
    public void testMpTelemetry11Alone() throws Exception {
        setFeature("mpTelemetry-1.1");
        server.startServer();
        String featureString = server.waitForStringInLog("CWWKF0012I"); // The server installed the following features:
        assertThat(featureString, allOf(containsString("cdi-4.0"), containsString("restfulWS-3.1")));
    }

    private static void setFeature(String feature) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        Set<String> features = config.getFeatureManager().getFeatures();
        features.clear();
        features.add(feature);
        server.updateServerConfiguration(config);
    }
}
