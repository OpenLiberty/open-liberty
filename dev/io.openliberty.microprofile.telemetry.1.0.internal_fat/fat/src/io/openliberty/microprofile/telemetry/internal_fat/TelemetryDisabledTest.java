/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
import static org.junit.Assert.assertEquals;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.tracingdisabled.TracingDisabledServlet;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class TelemetryDisabledTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10DisabledTracing";
    public static final String APP_NAME = "TelemetryDisabledTracingApp";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClasses(TracingDisabledServlet.class);

        server.addEnvVar("OTEL_SDK_DISABLED", "true");
        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.startServer();
    }

    //A warning should only be shown once
    @Test
    public void testGetGlobalOpenTelemetry() throws Exception {
        server.setMarkToEndOfLog();
        runTest(server, APP_NAME + "/TracingDisabledServlet", "testTelemetryDisabled");
        assertNotNull(server.waitForStringInLogUsingMark("CWMOT5100I: The MicroProfile Telemetry Tracing feature is enabled but not configured to generate traces for the "
                                                         + APP_NAME + " application."));
        
        //Checks 
        assertEquals(1, server.waitForMultipleStringsInLogUsingMark(2,"CWMOT5100I",1000,server.getDefaultLogFile()));

        server.setMarkToEndOfLog();
        runTest(server, APP_NAME + "/TracingDisabledServlet", "testTelemetryDisabled");
        assertNull(server.verifyStringNotInLogUsingMark("CWMOT5100I", 1000));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMOT5100I");
    }
}
