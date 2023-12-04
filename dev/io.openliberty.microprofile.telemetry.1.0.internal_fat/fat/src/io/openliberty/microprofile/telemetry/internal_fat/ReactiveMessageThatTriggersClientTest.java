/*******************************************************************************
 * Copyright (c) 2019, 2023  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureSet;
import componenttest.rules.repeater.RepeatTests;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.microprofile.telemetry.internal_fat.apps.rest.client.server.JaxRsApplication;
import io.openliberty.microprofile.telemetry.internal_fat.apps.rest.rest.client.user.MessageAndRestClientTestServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporterProvider;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import io.openliberty.microprofile.telemetry.internal_fat.shared.spans.AbstractSpanMatcher;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ReactiveMessageThatTriggersClientTest extends FATServletClient {

    public static final String APP_NAME = "ReactiveMessageThatTriggersRestClientApp";
    public static final String REST_APP_NAME = "RestClientTriggeredByReactiveMessagApp";
    public static final String SERVER_NAME = "Telemetry10ReactiveAndJaxClient";

    @ClassRule
    public static RepeatTests r = TelemetryActions.repeat(SERVER_NAME, MicroProfileActions.MP61, MicroProfileActions.MP60, TelemetryActions.MP50_MPTEL11);

    @Server(SERVER_NAME)
    @TestServlet(servlet = MessageAndRestClientTestServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(ReactiveMessageThatTriggersClientTest.class, "setUp", "begin setUp");

        // InMemorySpanExporter shared library
        PropertiesAsset exporterConfig = new PropertiesAsset()
                        .addProperty("otel.traces.exporter", "in-memory");
        JavaArchive exporterJar = ShrinkWrap.create(JavaArchive.class, "exporter.jar")
                        .addClasses(InMemorySpanExporter.class, InMemorySpanExporterProvider.class)
                        .addPackage(TestSpans.class.getPackage())
                        .addPackage(AbstractSpanMatcher.class.getPackage())
                        .addAsServiceProvider(ConfigurableSpanExporterProvider.class, InMemorySpanExporterProvider.class)
                        .addAsResource(exporterConfig, "META-INF/microprofile-config.properties");

        ShrinkHelper.exportToServer(server, "shared", exporterJar, SERVER_ONLY);

        PropertiesAsset userAppConfig = new PropertiesAsset()
                        .addProperty("otel.service.name", APP_NAME);
        WebArchive userWar = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                        .addPackages(true, MessageAndRestClientTestServlet.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addAsResource(userAppConfig, "META-INF/microprofile-config.properties");

        PropertiesAsset jaxServerConfig = new PropertiesAsset()
                        .addProperty("otel.service.name", REST_APP_NAME);
        WebArchive jaxServerWar = ShrinkWrap.create(WebArchive.class, REST_APP_NAME + ".war")
                        .addPackages(true, JaxRsApplication.class.getPackage())
                        .addPackage(TestSpans.class.getPackage())
                        .addAsResource(jaxServerConfig, "META-INF/microprofile-config.properties");

        server.addEnvVar("OTEL_SDK_DISABLED", "false");
        server.addEnvVar("OTEL_BSP_SCHEDULE_DELAY", "100");
        server.startServer(true, false);

        server.setMarkToEndOfLog();

        //waitForStringInLogUsingMark must be used instead of waitForStringInLog to avoid an obscure issue
        //waitForStringInLog will detect a string starting CWWKZ0001I and if so dig up the cached errors from
        //the apps not being present at server startup
        ShrinkHelper.exportAppToServer(server, jaxServerWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);
        server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + REST_APP_NAME); //Ensure the rest endpoint is ready before the next app pings it on startup

        ShrinkHelper.exportAppToServer(server, userWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);
        server.waitForStringInLogUsingMark("CWWKZ0001I:.*" + APP_NAME);

        Log.info(ReactiveMessageThatTriggersClientTest.class, "setUp", "end setUp");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(ReactiveMessageThatTriggersClientTest.class, "tearDown", "begin tearDown");

        server.stopServer("CWWKZ0014W"); //App not found error, this will happen because we start the server then deploy the apps
        server.deleteDirectoryFromLibertyServerRoot("apps");

        Log.info(ReactiveMessageThatTriggersClientTest.class, "tearDown", "end tearDown");
    }

}
