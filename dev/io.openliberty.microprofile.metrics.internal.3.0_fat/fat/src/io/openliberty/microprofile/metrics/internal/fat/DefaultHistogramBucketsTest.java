/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.metrics.internal.fat;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class DefaultHistogramBucketsTest {

	private static Class<?> c = DefaultHistogramBucketsTest.class;

	static String[] addedFeatures = {"jaxrs-2.1", "mpMetrics-3.0", "cdi-2.0"};

	static String[] removedFeatures = {"restfulWS-3.0", "mpMetrics-4.0"};

	@ClassRule
	public static RepeatTests r = RepeatTests.withoutModification()
			.andWith(new FeatureReplacementAction().withID("mpMetrics-4.0")
					.removeFeatures(
							new HashSet<>(Arrays.asList(removedFeatures)))
					.addFeatures(new HashSet<>(Arrays.asList(addedFeatures)))
					.forServers("DefaultBucketServer"));

	@Server("DefaultBucketServer")
	public static LibertyServer server;

	@BeforeClass
	public static void setUp() throws Exception {
		trustAll();

		WebArchive testWAR = ShrinkWrap
				.create(WebArchive.class, "testDefaultBuckets.war")
				.addPackage(
						"io.openliberty.microprofile.metrics.internal.fat.defaultBuckets")
				.addAsManifestResource(new File(
						"test-applications/testDefaultBucketsApp/resources/META-INF/microprofile-config.properties"),
						"microprofile-config.properties");

		ShrinkHelper.exportDropinAppToServer(server, testWAR,
				DeployOptions.SERVER_ONLY);

		server.startServer();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		// catch if a server is still running.
		if (server != null && server.isStarted()) {
			server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E",
					"CWMCG5003E", "CWPMI2006W", "CWMMC0013E", "CWWKG0033W");
		}
	}

	@Before
	public void beforeTest() {

		// Check that both CWWKO0219I messages for non-secure and secured http
		// endpoints are initialized
		server.waitForMultipleStringsInLog(2, "CWWKO0219I");

	}

	@Test
	@Mode(TestMode.FULL)
	public void testDefaultBucketsEnabledHistogram() throws Exception {
		final String method = "testDefaultBucketsEnabledHistogram";
		// hit rest endpoint

		String res = getHttpServlet("/testDefaultBuckets/test/histogram");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=testHistogram");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("application_testHistogram_bucket{le=\""))
					count++;

			}
		}
		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void testDefaultBucketsEnabledTimer() throws Exception {

		final String method = "testDefaultBucketsEnabledTimer";
		// hit rest endpoint

		String res = getHttpServlet("/testDefaultBuckets/test/timer");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=testTimer");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("application_testTimer_seconds_bucket{le=\""))
					count++;

			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void testHistogramDefaultBucketsMinMax() throws Exception {

		final String method = "testHistogramDefaultBucketsMinMax";
		// hit rest endpoint

		String res = getHttpServlet("/testDefaultBuckets/test/histogramMinMax");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=testHistogramMinMax");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_testHistogramMinMax_bucket{le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*")
							.matcher(line);
					if (matcher.find()) {
						double bucketVal = Double
								.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, allOf(greaterThanOrEqualTo(10.0),
								lessThanOrEqualTo(88.0)));
					}
				}
			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void testTimerDefaultBucketsMinMax() throws Exception {

		final String method = "testTimerDefaultBucketsMinMax";

		// hit rest endpoint

		String res = getHttpServlet("/testDefaultBuckets/test/timerMinMax");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=testTimerMinMax");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_testTimerMinMax_seconds_bucket{le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*")
							.matcher(line);
					if (matcher.find()) {
						double bucketVal = Double
								.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, allOf(greaterThanOrEqualTo(0.5),
								lessThanOrEqualTo(19.0)));
					}
				}
			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void testHistogramBadEnableConfig() throws Exception {

		final String method = "testHistogramBadEnableConfig";

		// hit rest endpoint

		String res = getHttpServlet(
				"/testDefaultBuckets/test/histogramBadEnableConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=histogram.bad.enable.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_histogram_bad_enable_config_bucket{le=\"")) {
					count++;
				}
			}
		}

		assertThat("Count is: " + count, count, equalTo(0));

	}

	@Test
	public void timerBadMaxGoodMinConfig() throws Exception {

		final String method = "timerBadMaxGoodMinConfig";

		// hit rest endpoint

		String res = getHttpServlet(
				"/testDefaultBuckets/test/timerBadMaxGoodMinConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=timer.bad.max.good.min.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_timer_bad_max_good_min_config_seconds_bucket{le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*")
							.matcher(line);
					if (matcher.find()) {
						double bucketVal = Double
								.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, greaterThanOrEqualTo(0.2));
					}
				}
			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void timerBadMinGoodMaxConfig() throws Exception {

		final String method = "timerBadMinGoodMaxConfig";

		// hit rest endpoint

		String res = getHttpServlet(
				"/testDefaultBuckets/test/timerBadMinGoodMaxConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=timer.bad.min.good.max.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_timer_bad_min_good_max_config_seconds_bucket{le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*")
							.matcher(line);
					if (matcher.find()) {
						double bucketVal = Double
								.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, lessThanOrEqualTo(15.0));
					}
				}
			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void histogramBadMinGoodMaxConfig() throws Exception {

		final String method = "histogramBadMinGoodMaxConfig";

		// hit rest endpoint

		String res = getHttpServlet(
				"/testDefaultBuckets/test/histogramBadMinGoodMaxConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=histogram.bad.min.good.max.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_histogram_bad_min_good_max_config_bucket{le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*")
							.matcher(line);
					if (matcher.find()) {
						double bucketVal = Double
								.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, lessThanOrEqualTo(145.0));
					}
				}
			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void histogramBadMaxGoodMixConfig() throws Exception {

		final String method = "histogramBadMinGoodMaxConfig";

		// hit rest endpoint

		String res = getHttpServlet(
				"/testDefaultBuckets/test/histogramBadMaxGoodMinConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=histogram.bad.max.good.min.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_histogram_bad_max_good_min_config_bucket{le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*")
							.matcher(line);
					if (matcher.find()) {
						double bucketVal = Double
								.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, greaterThanOrEqualTo(67.0));
					}
				}
			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void timerGoodMinGoodMaxConfig() throws Exception {

		final String method = "timerGoodMinGoodMaxConfig";

		// hit rest endpoint

		String res = getHttpServlet(
				"/testDefaultBuckets/test/timerGoodMinGoodMaxConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=timer.good.min.good.max.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_timer_good_min_good_max_config_seconds_bucket{le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*")
							.matcher(line);
					if (matcher.find()) {
						double bucketVal = Double
								.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, allOf(greaterThanOrEqualTo(0.3),
								lessThanOrEqualTo(19.0)));
					}
				}
			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void histogramGoodMaxGoodMinConfig() throws Exception {

		final String method = "histogramGoodMaxGoodMinConfig";

		// hit rest endpoint

		String res = getHttpServlet(
				"/testDefaultBuckets/test/histogramGoodMaxGoodMinConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet(
				"/metrics?scope=application&name=histogram.good.max.good.min.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains(
						"application_histogram_good_max_good_min_config_bucket{le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*")
							.matcher(line);
					if (matcher.find()) {
						double bucketVal = Double
								.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, allOf(greaterThanOrEqualTo(2.0),
								lessThanOrEqualTo(78.0)));
					}
				}
			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

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
