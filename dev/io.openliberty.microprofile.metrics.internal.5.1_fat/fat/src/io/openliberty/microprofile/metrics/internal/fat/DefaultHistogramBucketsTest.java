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
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class DefaultHistogramBucketsTest {

	private static Class<?> c = LibraryRefTest.class;

	@Server("DefaultBucketServer")
	public static LibertyServer server;

	@BeforeClass
	public static void setUp() throws Exception {
		trustAll();

		ShrinkHelper.defaultDropinApp(server, "testDefaultBuckets",
				"io.openliberty.microprofile.metrics.internal.fat.defaultBuckets");
		server.startServer();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		// catch if a server is still running.
		if (server != null && server.isStarted()) {
			server.stopServer("CWMCG0007E", "CWMCG0014E", "CWMCG0015E", "CWMCG5003E", "CWPMI2006W", "CWMMC0013E",
					"CWWKG0033W");
		}
	}

	@Test
	public void testDefaultBucketsEnabledHistogram() throws Exception {
		final String method = "testDefaultBucketsEnabledHistogram";
		// hit rest endpoint

		String res = getHttpsServlet("/testDefaultBuckets/test/histogram");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=testHistogram");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("testHistogram_bucket{mp_scope=\"application\",le=\""))
					count++;

			}
		}
		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void testDefaultBucketsEnabledTimer() throws Exception {

		final String method = "testDefaultBucketsEnabledTimer";
		// hit rest endpoint

		String res = getHttpsServlet("/testDefaultBuckets/test/timer");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=testTimer");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("testTimer_seconds_bucket{mp_scope=\"application\",le=\""))
					count++;

			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

	}

	@Test
	public void testHistogramDefaultBucketsMinMax() throws Exception {

		final String method = "testHistogramDefaultBucketsMinMax";
		// hit rest endpoint

		String res = getHttpsServlet("/testDefaultBuckets/test/histogramMinMax");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=testHistogramMinMax");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("testHistogramMinMax_bucket{mp_scope=\"application\",le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*").matcher(line);
					if (matcher.find()) {
						double bucketVal = Double.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, allOf(greaterThanOrEqualTo(10.0), lessThanOrEqualTo(88.0)));
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

		String res = getHttpsServlet("/testDefaultBuckets/test/timerMinMax");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=testTimerMinMax");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("testTimerMinMax_seconds_bucket{mp_scope=\"application\",le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*").matcher(line);
					if (matcher.find()) {
						double bucketVal = Double.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, allOf(greaterThanOrEqualTo(0.5), lessThanOrEqualTo(19.0)));
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

		String res = getHttpsServlet("/testDefaultBuckets/test/histogramBadEnableConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=histogram.bad.enable.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("histogram_bad_enable_config_bucket{mp_scope=\"application\",le=\"")) {
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

		String res = getHttpsServlet("/testDefaultBuckets/test/timerBadMaxGoodMinConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=timer.bad.max.good.min.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("timer_bad_max_good_min_config_seconds_bucket{mp_scope=\"application\",le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*").matcher(line);
					if (matcher.find()) {
						double bucketVal = Double.parseDouble(matcher.group(1).trim());
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

		String res = getHttpsServlet("/testDefaultBuckets/test/timerBadMinGoodMaxConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=timer.bad.min.good.max.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("timer_bad_min_good_max_config_seconds_bucket{mp_scope=\"application\",le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*").matcher(line);
					if (matcher.find()) {
						double bucketVal = Double.parseDouble(matcher.group(1).trim());
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

		String res = getHttpsServlet("/testDefaultBuckets/test/histogramBadMinGoodMaxConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=histogram.bad.min.good.max.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("histogram_bad_min_good_max_config_bucket{mp_scope=\"application\",le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*").matcher(line);
					if (matcher.find()) {
						double bucketVal = Double.parseDouble(matcher.group(1).trim());
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

		String res = getHttpsServlet("/testDefaultBuckets/test/histogramBadMaxGoodMinConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=histogram.bad.max.good.min.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("histogram_bad_max_good_min_config_bucket{mp_scope=\"application\",le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*").matcher(line);
					if (matcher.find()) {
						double bucketVal = Double.parseDouble(matcher.group(1).trim());
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

		String res = getHttpsServlet("/testDefaultBuckets/test/timerGoodMinGoodMaxConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=timer.good.min.good.max.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("timer_good_min_good_max_config_seconds_bucket{mp_scope=\"application\",le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*").matcher(line);
					if (matcher.find()) {
						double bucketVal = Double.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, allOf(greaterThanOrEqualTo(0.3), lessThanOrEqualTo(19.0)));
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

		String res = getHttpsServlet("/testDefaultBuckets/test/histogramGoodMaxGoodMinConfig");

		Log.info(c, method, "REST return " + res);

		String metrics = getHttpsServlet("/metrics?scope=application&name=histogram.good.max.good.min.config");

		Log.info(c, method, "[SCOPED METRICS]: " + metrics);

		metrics = getHttpsServlet("/metrics");

		Log.info(c, method, "[ALL METRICS]: " + metrics);

		int count = 0;
		try (Scanner sc = new Scanner(metrics)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.contains("histogram_good_max_good_min_config_bucket{mp_scope=\"application\",le=\"")) {
					count++;
					Matcher matcher = Pattern.compile(".*le=\"(\\d+.\\d+)\".*").matcher(line);
					if (matcher.find()) {
						double bucketVal = Double.parseDouble(matcher.group(1).trim());
						assertThat(bucketVal, allOf(greaterThanOrEqualTo(2.0), lessThanOrEqualTo(78.0)));
					}
				}
			}
		}

		assertThat("Count is: " + count, count, greaterThan(5));

	}

	private static void trustAll() throws Exception {
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			} }, new SecureRandom());
			SSLContext.setDefault(sslContext);
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (Exception e) {
			Log.error(c, "trustAll", e);
		}
	}

	private String getHttpsServlet(String servletPath) throws Exception {
		HttpsURLConnection con = null;
		try {
			String sURL = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + servletPath;
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
					+ Base64.getEncoder().encodeToString(("theUser:thePassword").getBytes(StandardCharsets.UTF_8)); // Java
																													// 8
			con.setRequestProperty("Authorization", authorization);
			con.setRequestProperty("Accept", "text/plain");
			con.setRequestMethod("GET");

			String sep = System.getProperty("line.separator");
			String line = null;
			StringBuilder lines = new StringBuilder();
			BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

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

}
