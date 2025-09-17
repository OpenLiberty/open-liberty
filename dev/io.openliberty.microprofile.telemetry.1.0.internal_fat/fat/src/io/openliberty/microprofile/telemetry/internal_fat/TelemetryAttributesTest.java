/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.ResourceServlet;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class TelemetryAttributesTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10ResourceAttributes";
    public static final String APP_NAME = "TelemetryResourcesApp";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ResourceServlet.class, contextRoot = APP_NAME),
    })
    public static LibertyServer server;

    //This test tests resources that were added to Liberty in MPTel 2.0
    @ClassRule
    public static RepeatTests r = TelemetryActions.telemetry21andLatest20Repeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClasses(ResourceServlet.class)
                        .addAsManifestResource(TelemetryAttributesTest.class.getResource("permissions-TelemetryAttributesTest.xml"), "permissions.xml");

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.addEnvVar("OTEL_SDK_DISABLED", "false");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
