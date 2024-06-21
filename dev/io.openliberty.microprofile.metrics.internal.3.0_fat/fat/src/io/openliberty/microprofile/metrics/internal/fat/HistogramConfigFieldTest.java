/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.microprofile.metrics.internal.fat;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class HistogramConfigFieldTest {

	private static Class<?> c = HistogramConfigFieldTest.class;

	@Server("HistogramConfig")
	public static LibertyServer server;

	@BeforeClass
	public static void setUp() throws Exception {
		trustAll();

		WebArchive testWAR = ShrinkWrap
				.create(WebArchive.class, "histogram.war")
				.addPackage(
						"io.openliberty.microprofile.metrics.internal.fat.histogram")
				.addAsManifestResource(new File(
						"test-applications/testHistogramApp/resources/META-INF/microprofile-config.properties"),
						"microprofile-config.properties");

		ShrinkHelper.exportDropinAppToServer(server, testWAR,
				DeployOptions.SERVER_ONLY);

		server.startServer();

		String line = server.waitForStringInLog(
				"CWWKT0016I: Web application available.*histogram*");
		Log.info(c, "setUp",
				"Web Application available message found: " + line);
		assertNotNull(
				"The CWWKT0016I Web Application available message did not appear in messages.log",
				line);
		Thread.sleep(5000);
	}

	@Before
	public void ensureServerStarted() throws Exception {
		trustAll();

		if (!server.isStarted()) {
			WebArchive testWAR = ShrinkWrap
					.create(WebArchive.class, "histogram.war")
					.addPackage(
							"io.openliberty.microprofile.metrics.internal.fat.histogram")
					.addAsManifestResource(new File(
							"test-applications/testHistogramApp/resources/META-INF/microprofile-config.properties"),
							"microprofile-config.properties");

			ShrinkHelper.exportDropinAppToServer(server, testWAR,
					DeployOptions.SERVER_ONLY);

			server.startServer();

			String line = server.waitForStringInLog(
					"CWWKT0016I: Web application available.*histogram*");
			Log.info(c, "setUp",
					"Web Application available message found: " + line);
			assertNotNull(
					"The CWWKT0016I Web Application available message did not appear in messages.log",
					line);
			Thread.sleep(5000);
		}
	}

	@AfterClass
	public static void afterClass() throws Exception {
		// catch if a server is still running.
		if (server != null && server.isStarted()) {
			server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E",
					"CWMCG5003E", "CWPMI2006W", "CWMMC0013E", "CWWKG0033W",
					"CWMMC0015W", "CWMMC0016W", "CWMMC0017W");
		}
		server.removeAllInstalledAppsForValidation();

	}

	@Test
	public void checkHistogramCustomPercentiles() throws Exception {

		final String method = "injectedHistogramCustomPercentiles";
		String res = getHttpServlet("/histogram/test/getHistograms");

		String metrics = "";
		try {
			metrics = getHttpsServlet("/metrics");

		} catch (Exception e) {
			Log.info(c, method, "Metrics endpoint failed! " + e.getMessage());
			metrics = getHttpsServlet("/metrics");
		}

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		Set<Double> expectedValues = new HashSet<>(Arrays.asList(0.2, 0.4));
		Set<Double> actualValues = new HashSet<>();

		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_io_openliberty_microprofile_metrics_internal_fat_histogram_MetricsResource_injectedHistogramCustomPercentiles{quantile=\"")) {
					count++;
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualValues.add(Double.parseDouble(matcher.group(1)));
					}
				}

			}
		}
		assertThat("Configured buckets length do not match", count, equalTo(2));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", actualValues,
				containsInAnyOrder(expectedValues.toArray(new Double[0])));
	}

	@Test
	public void checkHistogramNoPercentiles() throws Exception {

		final String method = "injectedHistogramNoPercentiles";
		String res = getHttpServlet("/histogram/test/getHistograms");

		String metrics = "";
		try {
			metrics = getHttpsServlet("/metrics");

		} catch (Exception e) {
			Log.info(c, method, "Metrics endpoint failed! " + e.getMessage());
			metrics = getHttpsServlet("/metrics");
		}

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_io_openliberty_microprofile_metrics_internal_fat_histogram_MetricsResource_injectedHistogramNoPercentiles{quantile=\"")) {
					count++;
				}

			}
		}
		assertThat("Configured buckets length do not match", count, equalTo(0));

	}

	@Test
	public void checkHistogramCustomBucketsDefaultPercentiles()
			throws Exception {

		final String method = "injectedHistogramCustomBucketsDefaultPercentiles";
		String res = getHttpServlet("/histogram/test/getHistograms");

		String metrics = "";
		try {
			metrics = getHttpsServlet("/metrics");

		} catch (Exception e) {
			Log.info(c, method, "Metrics endpoint failed! " + e.getMessage());
			metrics = getHttpsServlet("/metrics");
		}

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int percentileCount = 0;
		int bucketCount = 0;

		Set<Double> expectedPercentileValues = new HashSet<>(
				Arrays.asList(0.5, 0.75, 0.95, 0.98, 0.99, 0.999));
		Set<Double> actualPercentileValues = new HashSet<>();

		Set<Double> expectedBucketValues = new HashSet<>(
				Arrays.asList(100.0, 200.0));
		Set<Double> actualBucketValues = new HashSet<>();

		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_io_openliberty_microprofile_metrics_internal_fat_histogram_MetricsResource_injectedHistogramCustomBucketsDefaultPercentiles{quantile=\"")) {
					percentileCount++;
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualPercentileValues
								.add(Double.parseDouble(matcher.group(1)));
					}
				} else if (line.contains(
						"application_io_openliberty_microprofile_metrics_internal_fat_histogram_MetricsResource_injectedHistogramCustomBucketsDefaultPercentiles_bucket{le=\"")) {
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualBucketValues
								.add(Double.parseDouble(matcher.group(1)));
						bucketCount++;
					}
				}

			}
		}
		assertThat("Configured buckets length do not match", percentileCount,
				equalTo(6));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", actualPercentileValues,
				containsInAnyOrder(
						expectedPercentileValues.toArray(new Double[0])));

		assertThat("Configured buckets length do not match", bucketCount,
				equalTo(2));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", actualBucketValues,
				containsInAnyOrder(
						expectedBucketValues.toArray(new Double[0])));
	}

	@Test
	public void checkHistogramCustomBucketsCustomPercentiles()
			throws Exception {
		final String method = "injectedHistogramCustomBucketsCustomPercentiles";
		String res = getHttpServlet("/histogram/test/getHistograms");

		String metrics = "";
		try {
			metrics = getHttpsServlet("/metrics");

		} catch (Exception e) {
			Log.info(c, method, "Metrics endpoint failed! " + e.getMessage());
			metrics = getHttpsServlet("/metrics");
		}

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int percentileCount = 0;
		int bucketCount = 0;

		Set<Double> expectedPercentileValues = new HashSet<>(
				Arrays.asList(0.7, 0.8));
		Set<Double> actualPercentileValues = new HashSet<>();

		Set<Double> expectedBucketValues = new HashSet<>(
				Arrays.asList(150.0, 120.0));
		Set<Double> actualBucketValues = new HashSet<>();

		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_io_openliberty_microprofile_metrics_internal_fat_histogram_MetricsResource_injectedHistogramCustomBucketsCustomPercentiles{quantile=\"")) {
					percentileCount++;
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualPercentileValues
								.add(Double.parseDouble(matcher.group(1)));
					}
				} else if (line.contains(
						"application_io_openliberty_microprofile_metrics_internal_fat_histogram_MetricsResource_injectedHistogramCustomBucketsCustomPercentiles_bucket{le=\"")) {
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualBucketValues
								.add(Double.parseDouble(matcher.group(1)));
						bucketCount++;
					}
				}

			}
		}
		assertThat("Configured buckets length do not match", percentileCount,
				equalTo(2));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", actualPercentileValues,
				containsInAnyOrder(
						expectedPercentileValues.toArray(new Double[0])));

		assertThat("Configured buckets length do not match", bucketCount,
				equalTo(2));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", actualBucketValues,
				containsInAnyOrder(
						expectedBucketValues.toArray(new Double[0])));

	}

	@Test
	public void checkHistogramCustomBucketsNoPercentiles() throws Exception {

		final String method = "injectedHistogramCustomBucketsNoPercentiles";
		String res = getHttpServlet("/histogram/test/getHistograms");

		String metrics = "";
		try {
			metrics = getHttpsServlet("/metrics");

		} catch (Exception e) {
			Log.info(c, method, "Metrics endpoint failed! " + e.getMessage());
			metrics = getHttpsServlet("/metrics");
		}

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int percentileCount = 0;
		int bucketCount = 0;

		Set<Double> expectedBucketValues = new HashSet<>(
				Arrays.asList(444.0, 555.0));
		Set<Double> actualBucketValues = new HashSet<>();

		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_io_openliberty_microprofile_metrics_internal_fat_histogram_MetricsResource_injectedHistogramCustomBucketsNoPercentiles{quantile=\"")) {
					percentileCount++;
				} else if (line.contains(
						"application_io_openliberty_microprofile_metrics_internal_fat_histogram_MetricsResource_injectedHistogramCustomBucketsNoPercentiles_bucket{le=\"")) {
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualBucketValues
								.add(Double.parseDouble(matcher.group(1)));
						bucketCount++;
					}
				}

			}
		}
		assertThat("Configured buckets length do not match", percentileCount,
				equalTo(0));

		assertThat("Configured buckets length do not match", bucketCount,
				equalTo(2));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", actualBucketValues,
				containsInAnyOrder(
						expectedBucketValues.toArray(new Double[0])));

	}

	@Test
	@Mode(TestMode.FULL)
	public void checkPrecedenceHistogram() throws Exception {
		final String method = "injected_precedence_histogram";

		String res = getHttpServlet("/histogram/test/getHistograms");

		String metrics = "";
		try {
			metrics = getHttpsServlet("/metrics");

		} catch (Exception e) {
			Log.info(c, method, "Metrics endpoint failed! " + e.getMessage());
			metrics = getHttpsServlet("/metrics");
		}

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int percentileCount = 0;
		int bucketCount = 0;
		Set<Double> expectedPercentileValues = new HashSet<>(
				Arrays.asList(0.8, 0.9));
		Set<Double> actualPercentileValues = new HashSet<>();

		Set<Double> expectedBucketValues = new HashSet<>(
				Arrays.asList(23.0, 45.0));
		Set<Double> actualBucketValues = new HashSet<>();

		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"injected_precedence_histogram{quantile=\"")) {
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualPercentileValues
								.add(Double.parseDouble(matcher.group(1)));
						percentileCount++;
					}
				} else if (line.contains(
						"injected_precedence_histogram_bucket{le=\"")) {
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualBucketValues
								.add(Double.parseDouble(matcher.group(1)));
						bucketCount++;
					}
				}

			}
		}

		assertThat("Configured buckets length do not match", percentileCount,
				equalTo(2));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", expectedPercentileValues,
				containsInAnyOrder(
						actualPercentileValues.toArray(new Double[0])));

		assertThat("Configured buckets length do not match", bucketCount,
				equalTo(2));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", expectedBucketValues,
				containsInAnyOrder(actualBucketValues.toArray(new Double[0])));
	}

	@Test
	@Mode(TestMode.FULL)
	public void checkPrecedenceOverrideHistogram() throws Exception {

		final String method = "injected_precedence_override_histogram";

		String res = getHttpServlet("/histogram/test/getHistograms");

		String metrics = "";
		try {
			metrics = getHttpsServlet("/metrics");

		} catch (Exception e) {
			Log.info(c, method, "Metrics endpoint failed! " + e.getMessage());
			metrics = getHttpsServlet("/metrics");
		}

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int percentileCount = 0;
		int bucketCount = 0;
		Set<Double> expectedPercentileValues = new HashSet<>(
				Arrays.asList(0.2));
		Set<Double> actualPercentileValues = new HashSet<>();

		Set<Double> expectedBucketValues = new HashSet<>(Arrays.asList(32.0));
		Set<Double> actualBucketValues = new HashSet<>();

		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"injected_precedence_override_histogram{quantile=\"")) {
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualPercentileValues
								.add(Double.parseDouble(matcher.group(1)));
						percentileCount++;
					}
				} else if (line.contains(
						"injected_precedence_override_histogram_bucket{le=\"")) {
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualBucketValues
								.add(Double.parseDouble(matcher.group(1)));
						bucketCount++;
					}
				}

			}
		}

		assertThat("Configured buckets length do not match", percentileCount,
				equalTo(1));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", expectedPercentileValues,
				containsInAnyOrder(
						actualPercentileValues.toArray(new Double[0])));

		assertThat("Configured buckets length do not match", bucketCount,
				equalTo(1));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", expectedBucketValues,
				containsInAnyOrder(actualBucketValues.toArray(new Double[0])));

	}

	private static void trustAll() throws Exception {
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, new TrustManager[]{new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] arg0,
						String arg1) throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] arg0,
						String arg1) throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			}}, new SecureRandom());
			SSLContext.setDefault(sslContext);
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (Exception e) {
			Log.error(c, "trustAll", e);
		}
	}

	private String getHttpsServlet(String servletPath) throws Exception {
		Log.info(c, "HistogramConfig",
				"Server status(2): " + server.isStarted());

		HttpsURLConnection con = null;
		try {
			String sURL = "https://" + server.getHostname() + ":"
					+ server.getHttpDefaultSecurePort() + servletPath;
			Log.info(c, "getHttpsServlet", sURL);
			URL checkerServletURL = new URL(sURL);
			con = (HttpsURLConnection) checkerServletURL.openConnection();
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
			});
			String authorization = "Basic "
					+ Base64.getEncoder().encodeToString(("theUser:thePassword")
							.getBytes(StandardCharsets.UTF_8)); // Java
																// 8
			con.setRequestProperty("Authorization", authorization);
			con.setRequestProperty("Accept", "text/plain");
			con.setRequestMethod("GET");

			String sep = System.getProperty("line.separator");
			String line = null;
			StringBuilder lines = new StringBuilder();
			BufferedReader br = new BufferedReader(
					new InputStreamReader(con.getInputStream()));

			while ((line = br.readLine()) != null && line.length() > 0) {
				if (!line.startsWith("#"))
					lines.append(line).append(sep);
			}
			Log.info(c, "getHttpsServlet", sURL);
			return lines.toString();
		} finally {
			if (con != null)
				con.disconnect();
		}
	}

	private String getHttpServlet(String servletPath) throws Exception {
		Log.info(c, "HistogramConfig", "Server status: " + server.isStarted());
		HttpURLConnection con = null;
		try {
			String sURL = "http://" + server.getHostname() + ":"
					+ server.getHttpDefaultPort() + servletPath;
			URL checkerServletURL = new URL(sURL);
			con = (HttpURLConnection) checkerServletURL.openConnection();
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setRequestMethod("GET");
			String sep = System.getProperty("line.separator");
			String line = null;
			StringBuilder lines = new StringBuilder();
			BufferedReader br = new BufferedReader(
					new InputStreamReader(con.getInputStream()));

			while ((line = br.readLine()) != null && line.length() > 0) {
				lines.append(line).append(sep);
			}
			Log.info(c, "getHttpServlet", sURL);
			return lines.toString();
		} finally {
			if (con != null)
				con.disconnect();
		}
	}
}