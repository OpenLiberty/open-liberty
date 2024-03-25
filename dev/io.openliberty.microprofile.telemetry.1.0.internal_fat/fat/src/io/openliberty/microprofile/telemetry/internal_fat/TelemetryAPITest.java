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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.api.BaggageAPIServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.api.CommonSDKServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.api.ContextAPIServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.api.TraceAPIServlet;

/**
 * Test use of the Open Telemetry APIs:
 *
 * Tracing API (https://www.javadoc.io/doc/io.opentelemetry/opentelemetry-api/1.19.0/io/opentelemetry/api/trace/package-summary.html)
 * Baggage API (https://www.javadoc.io/doc/io.opentelemetry/opentelemetry-api/1.19.0/io/opentelemetry/api/baggage/package-summary.html)
 * Context API (https://www.javadoc.io/doc/io.opentelemetry/opentelemetry-context/1.19.0/index.html)
 * Resource SDK (https://www.javadoc.io/doc/io.opentelemetry/opentelemetry-sdk-common/1.19.0/index.html)
 *
 * The aim is these tests is only to show that the APIs are accessible and function in a basic way. This is not an exhaustive test of the APIs.
 */
@RunWith(FATRunner.class)
public class TelemetryAPITest extends FATServletClient {

    public static final String API_TEST_APP_NAME = "apiTest";
    public static final String SERVER_NAME = "Telemetry10Api";

    @TestServlets({
                    @TestServlet(contextRoot = API_TEST_APP_NAME, servlet = TraceAPIServlet.class),
                    @TestServlet(contextRoot = API_TEST_APP_NAME, servlet = BaggageAPIServlet.class),
                    @TestServlet(contextRoot = API_TEST_APP_NAME, servlet = ContextAPIServlet.class),
                    @TestServlet(contextRoot = API_TEST_APP_NAME, servlet = CommonSDKServlet.class)

    })
    @Server(SERVER_NAME)
    public static LibertyServer server;
    
    @ClassRule
    public static RepeatTests r = FATSuite.allMPRepeats(SERVER_NAME);

    @BeforeClass
    public static void setup() throws Exception {

        // API test app
        WebArchive apiTestWar = ShrinkWrap.create(WebArchive.class, API_TEST_APP_NAME + ".war")
                        .addClass(TraceAPIServlet.class)
                        .addClass(BaggageAPIServlet.class)
                        .addClass(ContextAPIServlet.class)
                        .addClass(CommonSDKServlet.class);

        ShrinkHelper.exportAppToServer(server, apiTestWar, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWMOT5100I");
    }
}
