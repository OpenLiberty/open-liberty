/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import javax.enterprise.inject.spi.Extension;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.CDIArchiveHelper;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.beansxml.BeansAsset.CDIVersion;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.BaggageServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.ConfigServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.MetricsDisabledServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.OpenTelemetryBeanServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.SpanCurrentServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.WithSpanExtension;
import io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry.WithSpanServlet;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;

@RunWith(FATRunner.class)
public class Telemetry10 extends FATServletClient {

    public static final String SERVER_NAME = "Telemetry10";
    public static final String APP_NAME = "TelemetryApp";

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = OpenTelemetryBeanServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = BaggageServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = SpanCurrentServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = MetricsDisabledServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = WithSpanServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = ConfigServlet.class, contextRoot = APP_NAME),
    })
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = TelemetryActions.latestTelemetryRepeats(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addAsResource(OpenTelemetryBeanServlet.class.getResource("microprofile-config.properties"), "META-INF/microprofile-config.properties")
                        .addClasses(OpenTelemetryBeanServlet.class,
                                    BaggageServlet.class,
                                    MetricsDisabledServlet.class,
                                    SpanCurrentServlet.class,
                                    WithSpanServlet.class,
                                    WithSpanExtension.class,
                                    ConfigServlet.class)
                        .addAsServiceProvider(Extension.class, WithSpanExtension.class);

        if (TelemetryActions.mpTelemetryEE7IsActive()) {
            // On EE7 / CDI 1.2 only, use of AnnotationLiteral requires app permissions
            app.addAsManifestResource(WithSpanExtension.class.getResource("permissions-ee7.xml"), "permissions.xml");
        }

        CDIArchiveHelper.addBeansXML(app, CDIVersion.CDI11);

        ShrinkHelper.exportAppToServer(server, app, SERVER_ONLY);
        //Set for testing purposes. The properties in the server.xml should override these variables.
        server.addEnvVar("OTEL_SERVICE_NAME", "overrideThisEnvVar");
        server.addEnvVar("OTEL_SDK_DISABLED", "true");
        server.startServer();
    }

    @AfterClass
    //CWMOT5006W: Warning about conflicting otel.sdk.enabled properties. A conflicting value is set in bootstrap.properties and server.xml
    public static void tearDown() throws Exception {
        server.stopServer("CWNEN0047W.*Telemetry10Servlet", "CWMOT5007W");
    }
}