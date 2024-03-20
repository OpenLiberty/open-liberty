/**
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
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
import static org.junit.Assert.assertTrue;

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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class BadHistogramTimerConfigTest {

	private static Class<?> c = BadHistogramTimerConfigTest.class;

	static String[] removedFeatures = {"jaxrs-2.1", "mpMetrics-3.0", "cdi-2.0",
			"localConnector-1.0"};

	@Server("BadHistogramTimer")
	public static LibertyServer server;

	@BeforeClass
	public static void setUp() throws Exception {
		trustAll();

		WebArchive testWAR = ShrinkWrap
				.create(WebArchive.class, "badHistogramTimer.war")
				.addPackage(
						"io.openliberty.microprofile.metrics.internal.fat.badHistogramTimer")
				.addAsManifestResource(new File(
						"test-applications/testBadHistogramTimerApp/resources/META-INF/microprofile-config.properties"),
						"microprofile-config.properties");

		ShrinkHelper.exportDropinAppToServer(server, testWAR,
				DeployOptions.SERVER_ONLY);

		server.startServer();

		String line = server.waitForStringInLog(
				"CWWKT0016I: Web application available.*badHistogramTimer*");
		Log.info(c, "setUp",
				"Web Application available message found: " + line);
		assertNotNull(
				"The CWWKT0016I Web Application available message did not appear in messages.log",
				line);

	}

	@AfterClass
	public static void afterClass() throws Exception {
		// catch if a server is still running.
		if (server != null && server.isStarted()) {
			server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E",
					"CWMCG5003E", "CWPMI2006W", "CWMMC0013E", "CWWKG0033W");
		}
		server.removeAllInstalledAppsForValidation();

	}

	@Test
	public void checkBadHistogramPercentiles() throws Exception {
		final String method = "testDefaultBucketsEnabledTimer";
		String res = getHttpServlet(
				"/badHistogramTimer/test/badHistogramPercentiles");

		String metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		Set<Double> expectedValues = new HashSet<>(
				Arrays.asList(0.1, 0.3, 0.4));
		Set<Double> actualValues = new HashSet<>();

		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_badHistogramPercentiles{quantile=\"")) {
					count++;
					System.out.println("Quantile line found: " + line);
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualValues.add(Double.parseDouble(matcher.group(1)));
					}
				}

			}
		}

		assertTrue("Count is: " + count, count == 3);

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", actualValues,
				containsInAnyOrder(expectedValues.toArray(new Double[0])));

	}

	@Test
	public void checkBadTimerPercentiles() throws Exception {

		final String method = "badTimerPercentiles";
		String res = getHttpServlet(
				"/badHistogramTimer/test/badHistogramPercentiles");

		String metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		Set<Double> expectedValues = new HashSet<>(Arrays.asList(0.1));
		Set<Double> actualValues = new HashSet<>();

		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_badTimerPercentiles_seconds{quantile=\"")) {
					count++;
					System.out.println("Quantile line found: " + line);
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualValues.add(Double.parseDouble(matcher.group(1)));

					}
				}

			}
		}

		assertTrue("Configured percentiles length do not match: " + count,
				count == 1);

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", actualValues,
				containsInAnyOrder(expectedValues.toArray(new Double[0])));

	}

	@Test
	public void checkBadHistogramBuckets() throws Exception {
		final String method = "badHistogramBuckets";

		String res = getHttpServlet(
				"/badHistogramTimer/test/badHistogramPercentiles");

		String metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		Set<Double> expectedValues = new HashSet<>(
				Arrays.asList(10.0, 12.0, 90.0));
		Set<Double> actualValues = new HashSet<>();

		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_badHistogramBuckets_bucket{le=\"")) {
					System.out.println("Quantile line found: " + line);
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualValues.add(Double.parseDouble(matcher.group(1)));
						count++;
					}
				}

			}
		}

		assertThat("Configured buckets length do not match", count, equalTo(3));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", actualValues,
				containsInAnyOrder(expectedValues.toArray(new Double[0])));

	}

	@Test
	public void checkBadTimerBuckets() throws Exception {
		final String method = "badTimerBuckets";

		String res = getHttpServlet(
				"/badHistogramTimer/test/badHistogramPercentiles");

		String metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		Set<Double> expectedValues = new HashSet<>(
				Arrays.asList(0.01, 0.03, 0.5));
		Set<Double> actualValues = new HashSet<>();

		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_badTimerBuckets_seconds_bucket{le=\"")) {
					System.out.println("Quantile line found: " + line);
					Matcher matcher = Pattern.compile("\"(\\d+(\\.\\d+)?)\"")
							.matcher(line);
					while (matcher.find()) {
						actualValues.add(Double.parseDouble(matcher.group(1)));
						count++;
					}
				}

			}
		}

		assertThat("Configured buckets length do not match", count, equalTo(3));

		// Check if actualValues contain all expectedValues
		assertThat("Configured buckets do not match", actualValues,
				containsInAnyOrder(expectedValues.toArray(new Double[0])));
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
