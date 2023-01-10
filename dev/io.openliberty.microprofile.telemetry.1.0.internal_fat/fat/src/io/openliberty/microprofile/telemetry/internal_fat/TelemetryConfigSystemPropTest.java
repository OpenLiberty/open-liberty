/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.BaggageServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.ConfigServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.MetricsDisabledServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.OpenTelemetryBeanServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.PatchTestApp;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.SpanCurrentServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.Telemetry10Servlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.WithSpanServlet;

@RunWith(FATRunner.class)
public class TelemetryConfigSystemPropTest extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10ConfigSystemProp";
    public static final String APP_NAME = "TelemetryApp";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ConfigServlet.class, contextRoot = APP_NAME),
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsResource(ConfigServlet.class.getResource("microprofile-config.properties"), "META-INF/microprofile-config.properties")
                        .addClasses(ConfigServlet.class);

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.addEnvVar("OTEL_SERVICE_NAME", "overrideThisEnvVar");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWNEN0047W.*ConfigServlet");
    }
}
