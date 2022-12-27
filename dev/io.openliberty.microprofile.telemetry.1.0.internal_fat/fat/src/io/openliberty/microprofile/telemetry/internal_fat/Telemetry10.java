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
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.OpenTelemetryBeanServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.PatchTestApp;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.SpanCurrentServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.Telemetry10Servlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.WithSpanServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.MetricsDisabledServlet;

@RunWith(FATRunner.class)
public class Telemetry10 extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10";
    public static final String APP_NAME = "TelemetryApp";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = Telemetry10Servlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = OpenTelemetryBeanServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = BaggageServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = SpanCurrentServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = MetricsDisabledServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = WithSpanServlet.class, contextRoot = APP_NAME)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsResource(Telemetry10Servlet.class.getResource("microprofile-config.properties"), "META-INF/microprofile-config.properties")
                        .addClasses(Telemetry10Servlet.class,
                                    OpenTelemetryBeanServlet.class,
                                    PatchTestApp.class,
                                    BaggageServlet.class,
                                    MetricsDisabledServlet.class,
                                    SpanCurrentServlet.class,
                                    WithSpanServlet.class);

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWNEN0047W.*Telemetry10Servlet");
    }
}
