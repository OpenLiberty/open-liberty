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
				"io_openliberty_threadpool_active_threads{io_openliberty_threadpool_name=\"Default Executor\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
				"io_openliberty_threadpool_size{io_openliberty_threadpool_name=\"Default Executor\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
				"io_openliberty_request_timing_active",
				"io_openliberty_request_timing_slow",
				"io_openliberty_request_timing_hung",
				"io_openliberty_request_timing_count"});

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
                new String[] { "io_openliberty_session_created_total{io_openliberty_application_name=\"default_host/testSessionApp\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_session_live{io_openliberty_application_name=\"default_host/testSessionApp\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_session_active{io_openliberty_application_name=\"default_host/testSessionApp\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_session_invalidated_total{io_openliberty_application_name=\"default_host/testSessionApp\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_session_invalidated_by_timeout_total{io_openliberty_application_name=\"default_host/testSessionApp\",job=\"io.openliberty.microprofile.telemetry.runtime\"}" });

	}
	
	@Test
	public void connectionPoolTest() throws Exception {
		String testName = "threadPoolMetricsTest";
		
		assertTrue(server.isStarted());
		
		Log.info(c, testName, "------- Add JDBC application and run JDBC servlet ------");
        ShrinkHelper.defaultDropinApp(server, "testJDBCApp",
                "io.openliberty.microprofile.telemetry.internal.monitor_fat.jdbc.servlet");
        Log.info(c, testName, "------- added testJDBCApp to dropins -----");
        checkStrings(requestHttpServlet("/testJDBCApp/testJDBCServlet?operation=create", server),
                new String[] { "sql: create table cities" });
        Log.info(c, testName, "------- connectionpool metrics should be available ------");
        
		// Allow time for the collector to receive and expose metrics
		TimeUnit.SECONDS.sleep(4);
        
        checkStrings(getContainerCollectorMetrics(container), 
                new String[] { "io_openliberty_connection_pool_handle_count{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS1\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_free{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS1\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_destroyed_total{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS1\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_created_total{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS1\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_count{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS1\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        
                        "io_openliberty_connection_pool_connection_use_time_seconds_bucket{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS1\",job=\"io.openliberty.microprofile.telemetry.runtime\",le=\"+Inf\"}",
                        "io_openliberty_connection_pool_connection_use_time_seconds_sum{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS1\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_use_time_seconds_count{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS1\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_queued_requests_total{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS1\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_used_total{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS1\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        
                        "io_openliberty_connection_pool_handle_count{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS2\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_free{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS2\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_destroyed_total{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS2\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_created_total{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS2\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_count{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS2\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        
                        "io_openliberty_connection_pool_connection_use_time_seconds_bucket{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS2\",job=\"io.openliberty.microprofile.telemetry.runtime\",le=\"+Inf\"}",
                        "io_openliberty_connection_pool_connection_use_time_seconds_sum{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS2\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_use_time_seconds_count{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS2\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_queued_requests_total{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS2\",job=\"io.openliberty.microprofile.telemetry.runtime\"}",
                        "io_openliberty_connection_pool_connection_used_total{io_openliberty_datasource_jndi_name=\"jdbc/exampleDS2\",job=\"io.openliberty.microprofile.telemetry.runtime\"}" });
	}

}
