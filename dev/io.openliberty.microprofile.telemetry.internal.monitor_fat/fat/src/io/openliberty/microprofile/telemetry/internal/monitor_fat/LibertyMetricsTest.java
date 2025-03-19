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

import componenttest.annotation.Server;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class LibertyMetricsTest extends BaseTestClass {

	private static Class<?> c = LibertyMetricsTest.class;

	
	private static final String SERVER_NAME = "LibertyMetricServer";
	
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
		 *  - ThreadPool
		 *  - RequestTiming
		 */
		
		assertTrue(server.isStarted());

		// Allow time for the collector to receive and expose metrics
		matchStringsWithRetries(() -> getContainerCollectorMetrics(container), new String[] {
				"io_openliberty_threadpool_active_threads\\{instance=\"[a-zA-Z0-9-]*\",io_openliberty_threadpool_name=\"Default Executor\",job=\"unknown_service\"\\}.*",
				"io_openliberty_threadpool_size\\{instance=\"[a-zA-Z0-9-]*\",io_openliberty_threadpool_name=\"Default Executor\",job=\"unknown_service\"\\}.*",
				"io_openliberty_request_timing_active.*",
				"io_openliberty_request_timing_slow.*",
				"io_openliberty_request_timing_hung.*",
				"io_openliberty_request_timing_processed.*"});

	}

}
