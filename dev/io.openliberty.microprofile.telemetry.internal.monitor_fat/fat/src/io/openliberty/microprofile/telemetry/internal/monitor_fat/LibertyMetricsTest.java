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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class LibertyMetricsTest extends BaseTestClass {

	private static Class<?> c = LibertyMetricsTest.class;

	@Server("LibertyMetricServer")
	public static LibertyServer server;

	@ClassRule
	public static GenericContainer<?> container = new GenericContainer<>(new ImageFromDockerfile()
			.withDockerfileFromBuilder(builder -> builder.from(IMAGE_NAME).copy("/etc/otelcol-contrib/config.yaml",
					"/etc/otelcol-contrib/config.yaml"))
			.withFileFromFile("/etc/otelcol-contrib/config.yaml", new File(PATH_TO_AUTOFVT_TESTFILES + "config.yaml")))
			.withLogConsumer(new SimpleLogConsumer(LibertyMetricsTest.class, "opentelemetry-collector-contrib"))
			.withExposedPorts(8888, 8889, 4317);

	@BeforeClass
	public static void beforeClass() throws Exception {
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
	public void threadPoolAndRequestTimingMetricsTest() throws Exception {

		/*
		 * These metrics should be available from startup.
		 * 
		 */
		
		assertTrue(server.isStarted());

		// Allow time for the collector to receive and expose metrics
		TimeUnit.SECONDS.sleep(4);

		checkStrings(getContainerCollectorMetrics(container), new String[] {
				"io_openliberty_threadpool_active_thread_count{io_openliberty_pool=\"Default_Executor\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
				"io_openliberty_threadpool_size{io_openliberty_pool=\"Default_Executor\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
				"io_openliberty_request_timing_active_request_count",
				"io_openliberty_request_timing_slow_request_count",
				"io_openliberty_request_timing_hung_request_count",
				"io_openliberty_request_timing_requests"});

	}
	
	
	
	@Test
	public void sessionsMetricTest() throws Exception { // -Dglobal.debug.java2.sec=false

		String testName = "sessionsMetricTest";
		
		assertTrue(server.isStarted());

		
        Log.info(c, testName, "------- Add session application and run session servlet ------");
        
        ShrinkHelper.defaultDropinApp(server, "testSessionApp",
                "io.openliberty.microprofile.telemetry.internal.monitor_fat.session.servlet");
		
        Log.info(c, testName, "------- added testSessionApp to dropins -----");
        checkStrings(requestHttpServlet("/testSessionApp/testSessionServlet", server), new String[] { "Session id:" });
        Log.info(c, testName, "------- session metrics should be available ------");

		// Allow time for the collector to receive and expose metrics
		TimeUnit.SECONDS.sleep(4);

        checkStrings(getContainerCollectorMetrics(container),
                new String[] { "io_openliberty_session_creates_total{io_openliberty_appname=\"default_host_testSessionApp\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_session_live_session_count{io_openliberty_appname=\"default_host_testSessionApp\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_session_active_session_count{io_openliberty_appname=\"default_host_testSessionApp\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_session_invalidated_total{io_openliberty_appname=\"default_host_testSessionApp\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_session_invalidated_by_timeout_total{io_openliberty_appname=\"default_host_testSessionApp\",job=\"io.openliberty.microprofile.telemetry.runtime\"}" });

	}
	

}
