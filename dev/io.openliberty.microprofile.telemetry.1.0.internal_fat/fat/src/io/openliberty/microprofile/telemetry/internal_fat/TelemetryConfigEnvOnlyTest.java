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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.ConfigServlet;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class TelemetryConfigEnvOnlyTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10ConfigEnv";
    public static final String APP_NAME = "TelemetryApp";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ConfigServlet.class, contextRoot = APP_NAME),
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.allMPRepeats(SERVER_NAME);
    
    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClasses(ConfigServlet.class);

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);

        // This tests that we do pick up config properties which are _only_ defined in the environment, using a format which MP Config only accepts for environment variables.
        server.addEnvVar("otel_service_name", "overrideDone");
        server.addEnvVar("otel_sdk_disabled", "false");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
