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
public class TelemetryConfigNullTest extends FATServletClient {

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

        // These properties are set to the empty string in server.xml which should lead to there being no value set for these properties in MP Config
        // HOWEVER, OTel does its own reading of environment variables if there's no value in MP Config to override it, so it will see the values set here
        // This is not ideal and is a current limitation, but we still want to test the scenario to ensure we don't throw an exception when processing config
        server.addEnvVar("otel_service_name", "overrideDone");
        server.addEnvVar("otel_sdk_disabled", "false");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
