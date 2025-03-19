/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.monitor_fat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class SessionMetricsTest extends BaseTestClass {

	private static Class<?> c = SessionMetricsTest.class;

	private static final String SERVER_NAME = "SessionsMetricsServer";
	
	@Server(SERVER_NAME)
	public static LibertyServer server;

    @ClassRule
    public static RepeatTests rt = FATSuite.testRepeatMPTel20(SERVER_NAME);
	
    //TODO switch to use ghcr.io/open-telemetry/opentelemetry-collector-releases/opentelemetry-collector-contrib:0.117.0
    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
	@ClassRule
	public static GenericContainer<?> container = new GenericContainer<>(new ImageFromDockerfile()
			.withDockerfileFromBuilder(builder -> builder.from(IMAGE_NAME).copy("/etc/otelcol-contrib/config.yaml",
					"/etc/otelcol-contrib/config.yaml"))
			.withFileFromFile("/etc/otelcol-contrib/config.yaml", new File(PATH_TO_AUTOFVT_TESTFILES + "config.yaml")))
			.withLogConsumer(new SimpleLogConsumer(SessionMetricsTest.class, "opentelemetry-collector-contrib"))
			.withExposedPorts(8888, 8889, 4317);

	@BeforeClass
	public static void beforeClass() throws Exception {
		
        WebArchive testWAR = ShrinkWrap
                .create(WebArchive.class, "testSessionApp.war")
                .addPackage(
                            "io.openliberty.microprofile.telemetry.internal.monitor_fat.session.servlet");

        ShrinkHelper.exportDropinAppToServer(server, testWAR,
                                     DeployOptions.SERVER_ONLY);
		
		server.addEnvVar("OTEL_EXPORTER_OTLP_METRICS_ENDPOINT",
				"http://" + container.getHost() + ":" + container.getMappedPort(4317));        
        
		server.startServer();

		// Read to run a smarter planet
		server.waitForStringInLogUsingMark("CWWKF0011I");
		server.setMarkToEndOfLog();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		// catch if a server is still running.
		if (server != null && server.isStarted()) {
			server.stopServer();
		}
	}
	
	
	
	@Test
	public void sessionsMetricTest() throws Exception { // -Dglobal.debug.java2.sec=false

		String testName = "sessionsMetricTest";
		
		assertTrue(server.isStarted());

        checkStrings(requestHttpServlet("/testSessionApp/testSessionServlet", server), new String[] { "Session id:" });
        Log.info(c, testName, "------- session metrics should be available ------");

		// Allow time for the collector to receive and expose metrics
		matchStringsWithRetries(() -> getContainerCollectorMetrics(container),
                new String[] { "io_openliberty_session_created_total\\{instance=\"[a-zA-Z0-9-]*\",io_openliberty_app_name=\"default_host/testSessionApp\",job=\"unknown_service\"\\}.*",
                        "io_openliberty_session_live\\{instance=\"[a-zA-Z0-9-]*\",io_openliberty_app_name=\"default_host/testSessionApp\",job=\"unknown_service\"\\}.*",
                        "io_openliberty_session_active\\{instance=\"[a-zA-Z0-9-]*\",io_openliberty_app_name=\"default_host/testSessionApp\",job=\"unknown_service\"\\}.*",
                        "io_openliberty_session_invalidated_total\\{instance=\"[a-zA-Z0-9-]*\",io_openliberty_app_name=\"default_host/testSessionApp\",job=\"unknown_service\"\\}.*",
                        "io_openliberty_session_invalidated_by_timeout_total\\{instance=\"[a-zA-Z0-9-]*\",io_openliberty_app_name=\"default_host/testSessionApp\",job=\"unknown_service\"\\}.*" });

	}
	

}
