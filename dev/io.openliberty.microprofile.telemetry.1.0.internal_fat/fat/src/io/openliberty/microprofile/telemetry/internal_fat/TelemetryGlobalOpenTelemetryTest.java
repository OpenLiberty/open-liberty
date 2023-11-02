/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.globalopentelemetry.TelemetryGlobalOpenTelemetryServlet;

@RunWith(FATRunner.class)
public class TelemetryGlobalOpenTelemetryTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10";
    public static final String APP_NAME = "TelemetryApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.allMPRepeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClasses(TelemetryGlobalOpenTelemetryServlet.class)
                        .addAsResource(new StringAsset("otel.sdk.disabled=false"),
                                       "META-INF/microprofile-config.properties");
        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.startServer();
    }

    @Test
    public void testSetGlobalOpenTelemetry() throws Exception {
        runTest(server, APP_NAME + "/GlobalOpenTelemetryServlet", "testSetGlobalOpenTelemetry");
    }

    //A warning should only be shown the first time the user attempts to get the GlobalOpenTelemetry instance
    @Test
    public void testGetGlobalOpenTelemetry() throws Exception {
        server.setMarkToEndOfLog();
        runTest(server, APP_NAME + "/GlobalOpenTelemetryServlet", "testGetGlobalOpenTelemetry");
        assertNotNull(server
                        .waitForStringInLogUsingMark("CWMOT5000W: The GlobalOpenTelemetry.get method was called. This method returns a non-functional OpenTelemetry object. Use CDI to inject an OpenTelemetry object instead."));

        server.setMarkToEndOfLog();
        runTest(server, APP_NAME + "/GlobalOpenTelemetryServlet", "testGetGlobalOpenTelemetry");
        assertNull(server
                        .verifyStringNotInLogUsingMark("CWMOT5000W: The GlobalOpenTelemetry.get method was called. This method returns a non-functional OpenTelemetry object. Use CDI to inject an OpenTelemetry object instead.",
                                                       1000));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMOT5000W", //Cannot get GlobalOpenTelemetry
                          "CWMOT5001E" //Cannot set GlobalOpenTelemetry
        );

    }
}
