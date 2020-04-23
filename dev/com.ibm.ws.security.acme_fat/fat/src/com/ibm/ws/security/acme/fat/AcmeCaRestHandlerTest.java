/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.fat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.docker.pebble.PebbleContainer;
import com.ibm.ws.security.acme.internal.web.AcmeCaRestHandler;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;

/**
 * Test the {@link AcmeCaRestHandler} REST endpoint.
 */
@RunWith(FATRunner.class)
public class AcmeCaRestHandlerTest {

	@Server("com.ibm.ws.security.acme.fat.rest")
	public static LibertyServer server;

	private static ServerConfiguration ORIGINAL_CONFIG;
	private static final String[] DOMAINS = { "domain1.com", "domain2.com", "domain3.com" };
	private static final String REST_ENDPOINT = "/ibm/api/acmeca";
	private static final String ADMIN_USER = "administrator";
	private static final String ADMIN_PASS = "adminpass";
	private static final String READER_USER = "reader";
	private static final String READER_PASS = "readerpass";
	private static final String UNAUTHORIZED_USER = "unauthorized";
	private static final String UNAUTHORIZED_PASS = "unauthorizedpass";

	@ClassRule
	public static CAContainer pebble = new PebbleContainer();

	static {
		ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		ORIGINAL_CONFIG = server.getServerConfiguration();
	}

	@After
	public void afterTest() {
		/*
		 * Cleanup any generated ACME files.
		 */
		AcmeFatUtils.deleteAcmeFiles(server);
	}

	/**
	 * Verify that making a call to refresh the certificate actually forcibly
	 * refreshes the certificate.
	 * 
	 * @throws Exception
	 *             if the test failed for some unforeseen reason.
	 */
	@Test
	public void refreshCertificate() throws Exception {
		final String methodName = "refreshCertificate()";

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, pebble, ORIGINAL_CONFIG, DOMAINS);

		try {
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSSLToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Get the current certificate.
			 */
			Log.info(this.getClass(), methodName, "Performing GET #1");
			String html1 = performGet(200, ADMIN_USER, ADMIN_PASS);
			assertNotNull("Should have received an HTML document from REST endpoint.", html1);

			Log.info(this.getClass(), methodName, "Performing PUT #1");
			performPut(204, ADMIN_USER, ADMIN_PASS);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			Log.info(this.getClass(), methodName, "Performing GET #2");
			String html2 = performGet(200, ADMIN_USER, ADMIN_PASS);
			assertNotNull("Should have received an HTML document from REST endpoint.", html2);
			String serial1 = getLeafSerialFromHtml(html1);
			String serial2 = getLeafSerialFromHtml(html2);
			assertThat("Certificates should have been different.", serial2, not(equalTo(serial1)));
		} finally {
			/*
			 * Stop the server.
			 */
			server.stopServer();
		}
	}

	/**
	 * Verify that authorization on the endpoint works as expected.
	 * 
	 * @throws Exception
	 *             if the test failed for some unforeseen reason.
	 */
	@Test
	public void testAuthorization() throws Exception {
		final String methodName = "refreshCertificate()";

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, pebble, ORIGINAL_CONFIG, DOMAINS);

		try {
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSSLToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Test authorization for GET requests.
			 */
			Log.info(this.getClass(), methodName, "Performing GET for user with administrator-role.");
			String html1 = performGet(200, ADMIN_USER, ADMIN_PASS);
			assertNotNull("Should have received an HTML document from REST endpoint.", html1);

			Log.info(this.getClass(), methodName, "Performing GET for user with reader-role.");
			String html2 = performGet(200, READER_USER, READER_PASS);
			assertNotNull("Should have received an HTML document from REST endpoint.", html2);

			Log.info(this.getClass(), methodName, "Performing GET for user with NO role.");
			String html3 = performGet(403, UNAUTHORIZED_USER, UNAUTHORIZED_PASS);
			assertNotNull("Should have received an HTML document from REST endpoint.", html3);

			/*
			 * Test authorization for PUT requests.
			 */
			Log.info(this.getClass(), methodName, "Performing PUT for user with administrator-role.");
			performPut(204, ADMIN_USER, ADMIN_PASS);

			Log.info(this.getClass(), methodName, "Performing PUT for user with reader-role.");
			performPut(403, READER_USER, READER_PASS);

			Log.info(this.getClass(), methodName, "Performing PUT for user with NO role.");
			performPut(403, UNAUTHORIZED_USER, UNAUTHORIZED_PASS);

		} finally {
			/*
			 * Stop the server.
			 */
			server.stopServer();
		}
	}

	/**
	 * Verify that various HTTP methods are not allowed.
	 * 
	 * @throws Exception
	 *             If the test failed for some unforeseen reason.
	 */
	@Test
	public void methodNotAllowed() throws Exception {

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, pebble, ORIGINAL_CONFIG, DOMAINS);

		try {
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSSLToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * These methods should be not allowed.
			 */
			performDelete(ADMIN_USER, ADMIN_PASS);
			performOptions(ADMIN_USER, ADMIN_PASS);
			performPost(ADMIN_USER, ADMIN_PASS);

		} finally {
			/*
			 * Stop the server.
			 */
			server.stopServer();
		}
	}

	/**
	 * Make a GET call to the REST endpoint. This will return the currently
	 * configured certificate.
	 * 
	 * @param expectedStatus
	 *            The expected HTTP return code.
	 * @param user
	 *            The user to make the call with.
	 * @param password
	 *            The password to make the call with.
	 * @throws Exception
	 *             If the call failed.
	 */
	private static String performGet(int expectedStatus, String user, String password) throws Exception {
		String methodName = "performRestGet()";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

			/*
			 * Create a GET request to the Liberty server.
			 */
			HttpGet httpGet = new HttpGet("https://localhost:" + server.getHttpDefaultSecurePort() + REST_ENDPOINT);
			httpGet.setHeader("Authorization",
					"Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));

			/*
			 * Send the GET request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
				AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpGet, response);

				StatusLine statusLine = response.getStatusLine();
				assertEquals("Unexpected status code response.", expectedStatus, statusLine.getStatusCode());

				String html = EntityUtils.toString(response.getEntity());
				Log.info(AcmeCaRestHandlerTest.class, methodName, "HTTP GET contents: \n" + html);

				return html;
			}
		}
	}

	/**
	 * Make a PUT call to the REST endpoint. This will drive a certificate
	 * refresh.
	 * 
	 * @param expectedStatus
	 *            The expected HTTP return code.
	 * @param user
	 *            The user to make the call with.
	 * @param password
	 *            The password to make the call with.
	 * @throws Exception
	 *             If the call failed.
	 */
	private static void performPut(int expectedStatus, String user, String password) throws Exception {
		String methodName = "performPut()";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

			/*
			 * Create a PUT request to the Liberty server.
			 */
			HttpPut httpPut = new HttpPut("https://localhost:" + server.getHttpDefaultSecurePort() + REST_ENDPOINT);
			httpPut.setHeader("Authorization",
					"Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));

			/*
			 * Send the PUT request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPut)) {
				AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpPut, response);

				StatusLine statusLine = response.getStatusLine();
				assertEquals("Unexpected status code response.", expectedStatus, statusLine.getStatusCode());
			}
		}
	}

	/**
	 * Make a POST call to the REST endpoint. This method is not allowed.
	 * 
	 * @param user
	 *            The user to make the call with.
	 * @param password
	 *            The password to make the call with.
	 * @throws Exception
	 *             If the call failed.
	 */
	private static void performPost(String user, String password) throws Exception {
		String methodName = "performPut()";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

			/*
			 * Create a POST request to the Liberty server.
			 */
			HttpPost httpPost = new HttpPost("https://localhost:" + server.getHttpDefaultSecurePort() + REST_ENDPOINT);
			httpPost.setHeader("Authorization",
					"Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				assertEquals("Unexpected status code response.", 405, statusLine.getStatusCode());
			}
		}
	}

	/**
	 * Make a DELETE call to the REST endpoint. This method is not allowed.
	 * 
	 * @param user
	 *            The user to make the call with.
	 * @param password
	 *            The password to make the call with.
	 * @throws Exception
	 *             If the call failed.
	 */
	private static void performDelete(String user, String password) throws Exception {
		String methodName = "performPut()";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

			/*
			 * Create a DELETE request to the Liberty server.
			 */
			HttpDelete httpDelete = new HttpDelete(
					"https://localhost:" + server.getHttpDefaultSecurePort() + REST_ENDPOINT);
			httpDelete.setHeader("Authorization",
					"Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));

			/*
			 * Send the DELETE request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpDelete)) {
				AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpDelete, response);

				StatusLine statusLine = response.getStatusLine();
				assertEquals("Unexpected status code response.", 405, statusLine.getStatusCode());
			}
		}
	}

	/**
	 * Make a DELETE call to the REST endpoint. This method is not allowed.
	 * 
	 * @param user
	 *            The user to make the call with.
	 * @param password
	 *            The password to make the call with.
	 * @throws Exception
	 *             If the call failed.
	 */
	private static void performOptions(String user, String password) throws Exception {
		String methodName = "performPut()";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

			/*
			 * Create a OPTIONS request to the Liberty server.
			 */
			HttpOptions httpOptions = new HttpOptions(
					"https://localhost:" + server.getHttpDefaultSecurePort() + REST_ENDPOINT);
			httpOptions.setHeader("Authorization",
					"Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));

			/*
			 * Send the OPTIONS request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpOptions)) {
				AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpOptions, response);

				StatusLine statusLine = response.getStatusLine();
				assertEquals("Unexpected status code response.", 405, statusLine.getStatusCode());
			}
		}
	}

	/**
	 * Get the leaf certificates serial number from the HTML page returned from
	 * the REST client.
	 * 
	 * @param htmlCertChain
	 *            the HTML content from a GET request.
	 * @return The serial number, or null if one was not found.
	 */
	private static String getLeafSerialFromHtml(String htmlCertChain) {
		String serial = null;

		Matcher m = Pattern.compile(".*SerialNumber:\\s+\\[\\s*(.*)\\].*").matcher(htmlCertChain);
		if (m.find()) {
			serial = m.group(1);
		}

		Log.info(AcmeCaRestHandlerTest.class, "getLeafSerialFromHtml(String)", serial);
		return serial;
	}
}
