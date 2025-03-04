/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.DisabledProvidersServlet;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class TelemetryDisabledProvidersTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10DisabledProviders";
    public static final String APP_NAME = "TelemetryDisabledProvidersApp";
    private static final String RESOURCES_PACKAGE = "io.opentelemetry.instrumentation.resources.";
    private static final String OS_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "OsResourceProvider";
    private static final String HOST_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "HostResourceProvider";
    private static final String PROCESS_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "ProcessResourceProvider";
    private static final String PROCESS_RUNTIME_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "ProcessRuntimeResourceProvider";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = DisabledProvidersServlet.class, contextRoot = APP_NAME),
    })
    public static LibertyServer server;

    //This test tests that resource providers can be disabled with `otel.java.disabled.resource.providers` in MPTel 2.0
    @ClassRule
    public static RepeatTests r = TelemetryActions.telemetry20Repeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addClasses(DisabledProvidersServlet.class)
                        .addAsManifestResource(TelemetryAttributesTest.class.getResource("permissions-TelemetryAttributesTest.xml"), "permissions.xml");

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.addEnvVar("OTEL_SDK_DISABLED", "false");
        server.addEnvVar("OTEL_JAVA_DISABLED_RESOURCE_PROVIDERS", OS_RESOURCE_PROVIDER + "," + PROCESS_RESOURCE_PROVIDER + "," + PROCESS_RUNTIME_RESOURCE_PROVIDER + "," + HOST_RESOURCE_PROVIDER);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }
}
