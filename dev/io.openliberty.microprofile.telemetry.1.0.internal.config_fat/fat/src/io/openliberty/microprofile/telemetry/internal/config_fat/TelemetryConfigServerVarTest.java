/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.config_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal.config_fat.apps.telemetry.ConfigServlet;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class TelemetryConfigServerVarTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10ConfigServerVar";
    public static final String APP_NAME = "TelemetryApp";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ConfigServlet.class, contextRoot = APP_NAME),
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = TelemetryActions.latestTelemetryRepeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsResource(ConfigServlet.class.getResource("microprofile-config.properties"), "META-INF/microprofile-config.properties")
                        .addClasses(ConfigServlet.class);

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        //Set for testing purposes. The variables in the server.xml should override these variables.
        server.addEnvVar("OTEL_SERVICE_NAME", "overrideThisEnvVar");
        server.addEnvVar("OTEL_SDK_DISABLED", "true");
        server.startServer();
    }

    @Test
    //We don't throw warning related to runtime mode in versions older than the runtime mode.
    @SkipForRepeat({ TelemetryActions.MP14_MPTEL11_ID, TelemetryActions.MP41_MPTEL11_ID, TelemetryActions.MP50_MPTEL11_ID, MicroProfileActions.MP61_ID,
                     MicroProfileActions.MP60_ID })
    public void testWarningForConflictingValues() throws Exception {
        //Don't set the mark, as the warning comes from app startup.

        //Checks for a warning message because server.xml conflicts with the system properties and env variables.
        assertNotNull(server.waitForStringInLogUsingMark("CWMOT5007W"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMOT5007W");
    }
}
