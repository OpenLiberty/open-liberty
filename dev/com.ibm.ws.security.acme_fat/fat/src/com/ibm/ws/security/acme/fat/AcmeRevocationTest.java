/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
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
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileFilter;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.xml.bind.DatatypeConverter;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.AcmeCA;
import com.ibm.websphere.simplicity.config.AcmeCA.AcmeRevocationChecker;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.docker.boulder.BoulderContainer;
import com.ibm.ws.security.acme.docker.pebble.PebbleContainer;
import com.ibm.ws.security.acme.internal.web.AcmeCaRestHandler;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Verify that ACME will check the revocation status of the certificate and
 * replace it if necessary.
 * 
 * TODO There are no CRL tests.
 * 
 * >>>>QUARANTINE -- The Boulder intermediate certificate expired. Work to
 * either update the existing certificate or pull a new image of Boulder (which
 * has made significant change is ongoing). Further notes in RTC 279882.
 */
@RunWith(FATRunner.class)
// @Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) // No value added
public class AcmeRevocationTest {

	@Server("com.ibm.ws.security.acme.fat.revocation")
	public static LibertyServer server;

	public static CAContainer boulder = null;

	private static ServerConfiguration originalServerConfig;

	private static final String ADMIN_USER = "administrator";

	private static final String ADMIN_PASS = "adminpass";
	
	private static final String DOMAIN = "domain1.com"; // Set to domain1.com as it has a high rate limit on certificate requests
	
	private static final String CERTIFICATE_ENDPOINT = "/ibm/api" + AcmeCaRestHandler.PATH_CERTIFICATE;
	
	private static final String JSON_CERT_REGEN = "{\"" + AcmeCaRestHandler.OP_KEY + "\":\""
			+ AcmeCaRestHandler.OP_RENEW_CERT + "\"}";
	private static final String JSON_CERT_INVALID_OP = "{\"" + AcmeCaRestHandler.OP_KEY + "\":\"invalid\"}";

	private static final String JSON_CERT_REVOKE_DEFAULT_REASON = "{\"" + AcmeCaRestHandler.OP_KEY + "\":\""
			+ AcmeCaRestHandler.OP_REVOKE_CERT + "\"}";
	private static final String JSON_CERT_REVOKE_VALID_REASON = "{\"" + AcmeCaRestHandler.OP_KEY + "\":\""
			+ AcmeCaRestHandler.OP_REVOKE_CERT + "\",\"" + AcmeCaRestHandler.REASON_KEY + "\":\"superseded\"}";
	private static final String JSON_CERT_REVOKE_INVALID_REASON = "{\"" + AcmeCaRestHandler.OP_KEY + "\":\""
			+ AcmeCaRestHandler.OP_REVOKE_CERT + "\",\"" + AcmeCaRestHandler.REASON_KEY + "\":\"invalid\"}";

	private static final String CONTENT_TYPE_JSON = "application/json";

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() throws Exception {
		boulder = new BoulderContainer();
		originalServerConfig = server.getServerConfiguration();
		AcmeFatUtils.checkPortOpen(boulder.getHttpPort(), 60000);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (boulder != null) {
			boulder.stop();
		}
	}

	@After
	public void after() throws Exception {
		AcmeFatUtils.deleteAcmeFiles(server);
	}

	/**
	 * This test will verify that ACME will check the OCSP status of the
	 * certificate at startup, and if the status is revoked, replace it.
	 * 
	 * @throws Exception
	 *             if the test failed for some unforeseen reason.
	 */
	@Test
	public void ocsp_revocation_check_startup() throws Exception {
		final String methodName = testName.getMethodName();

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		ServerConfiguration configuration = originalServerConfig.clone();
		configureAcmeRevocation(configuration, true);
		AcmeFatUtils.configureAcmeCA(server, boulder, configuration, false, DOMAIN);

		try {

			/*
			 * Startup the server.
			 */
			Log.info(AcmeSimpleTest.class, methodName, "Starting server.");
			server.startServer();
			AcmeFatUtils.waitForSslToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);
			Certificate[] certificates1 = AcmeFatUtils.assertAndGetServerCertificate(server, boulder);

			/*
			 * Revoke the certificate using the REST API. This will not trigger
			 * a replacement, as it only revokes it.
			 */
			revokeCertificate();
			AcmeFatUtils.waitForAcmeToRevokeCert(server);

			/*
			 * The OCSP responder connected with Boulder will now return the
			 * status as revoked. Cycle the server and we should now see that
			 * the certificate is replaced since it was revoked.
			 */
			stopServer();
			server.startServer();
			server.waitForStringInLog("CWPKI2059I"); // Detected cert revoked!
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server, boulder);

			/*
			 * Certificate should have been updated.
			 */
			BigInteger serial1 = ((X509Certificate) certificates1[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) certificates2[0]).getSerialNumber();
			assertThat("Expected new certificate.", serial1, not(equalTo(serial2)));

		} finally {
			stopServer();
		}
	}

	/**
	 * This test will verify that ACME will check the OCSP status of the
	 * certificate at startup, and if the status is revoked, replace it.
	 * 
	 * @throws Exception
	 *             if the test failed for some unforeseen reason.
	 */
	@Test
	public void ocsp_revocation_check_startup_disabled() throws Exception {
		final String methodName = testName.getMethodName();

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		ServerConfiguration configuration = originalServerConfig.clone();
		configureAcmeRevocation(configuration, false);
		AcmeFatUtils.configureAcmeCA(server, boulder, configuration, false, DOMAIN);

		try {

			/*
			 * Startup the server.
			 */
			Log.info(AcmeSimpleTest.class, methodName, "Starting server.");
			server.startServer();
			AcmeFatUtils.waitForSslToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);
			Certificate[] certificates1 = AcmeFatUtils.assertAndGetServerCertificate(server, boulder);

			/*
			 * Revoke the certificate using the REST API. This will not trigger
			 * a replacement, as it only revokes it.
			 */
			revokeCertificate();
			AcmeFatUtils.waitForAcmeToRevokeCert(server);

			/*
			 * The OCSP responder connected with Boulder will now return the
			 * status as revoked. Cycle the server. This time we will not
			 * perform the revocation check since it is disabled.
			 */
			stopServer();
			server.startServer();
			AcmeFatUtils.waitForSslEndpoint(server);
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server, boulder);

			/*
			 * Certificate should NOT have been updated.
			 */
			BigInteger serial1 = ((X509Certificate) certificates1[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) certificates2[0]).getSerialNumber();
			assertThat("Expected same certificate.", serial1, equalTo(serial2));

		} finally {
			stopServer();
		}
	}

	/**
	 * This test will verify that ACME will try to call the OCSP responder, but
	 * given an unreachable OCSP responder URI (from the certificate), will fail
	 * to detect that the certificate is revoked. It will leave the certificate
	 * alone since this is considered a soft failure, and issue a warning
	 * message to the logs.
	 * 
	 * @throws Exception
	 *             if the test failed for some unforeseen reason.
	 */
	@Test
	public void ocsp_revocation_check_startup_unreachable_oscp_responder() throws Exception {
		final String methodName = testName.getMethodName();

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		ServerConfiguration configuration = originalServerConfig.clone();
		AcmeFatUtils.configureAcmeCA(server, boulder, configuration, false, DOMAIN);

		try {

			/*
			 * Startup the server.
			 */
			Log.info(AcmeSimpleTest.class, methodName, "Starting server.");
			server.startServer();
			AcmeFatUtils.waitForSslToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);
			Certificate[] certificates1 = AcmeFatUtils.assertAndGetServerCertificate(server, boulder);

			/*
			 * Revoke the certificate using the REST API. This will not trigger
			 * a replacement, as it only revokes it.
			 */
			revokeCertificate();
			AcmeFatUtils.waitForAcmeToRevokeCert(server);

			/*
			 * The OCSP responder URI in the certificate cannot be reached from
			 * our server, so it will fail to determine that the certificate is
			 * revoked. We treat this as a soft failure since it is possible it
			 * may be a network glitch. A warning will be written to logs.
			 */
			stopServer();
			server.startServer();
			server.waitForStringInLog("CWPKI2058W"); // Soft failures...
			AcmeFatUtils.waitForSslEndpoint(server);
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server, boulder);

			/*
			 * Certificate should NOT have been updated.
			 */
			BigInteger serial1 = ((X509Certificate) certificates1[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) certificates2[0]).getSerialNumber();
			assertThat("Expected same certificate.", serial1, equalTo(serial2));

		} finally {
			stopServer("CWPKI2058W");
		}
	}

	/**
	 * Configure ACME revocation so that we can talk to the OCSP responder
	 * running on Boulder.
	 * 
	 * @param configuration
	 *            The server configuration to update.
	 */
	private static void configureAcmeRevocation(ServerConfiguration configuration, boolean enabled) {
		AcmeCA acmeCa = configuration.getAcmeCA();
		AcmeRevocationChecker acmeRevocationChecker = new AcmeRevocationChecker();
		acmeRevocationChecker.setOcspResponderUrl(boulder.getOcspResponderUrl());
		acmeRevocationChecker.setEnabled(enabled);
		acmeCa.setAcmeRevocationChecker(acmeRevocationChecker);
		acmeCa.setRenewCertMin(17000L);
	}

	/**
	 * Issue a POST request to the ACME REST API to revoke the certificate, but
	 * not replace it.
	 * 
	 * @return The JSON response.
	 * @throws Exception
	 *             if the request failed.
	 */
	private static String revokeCertificate() throws Exception {
		final String methodName = "revokeCertificate()";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

			/*
			 * Create a POST request to the Liberty server.
			 */
			HttpPost httpPost = new HttpPost("https://localhost:" + server.getHttpDefaultSecurePort() + "/ibm/api"
					+ AcmeCaRestHandler.PATH_CERTIFICATE);
			httpPost.setHeader("Authorization",
					"Basic " + DatatypeConverter.printBase64Binary((ADMIN_USER + ":" + ADMIN_PASS).getBytes()));
			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setEntity(new StringEntity("{\"operation\":\"revokeCertificate\"}"));

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(AcmeRevocationTest.class, methodName, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				assertEquals("Unexpected status code response.", 200, statusLine.getStatusCode());

				/*
				 * Check content type header.
				 */
				Header[] headers = response.getHeaders("content-type");
				assertNotNull("Expected content type header.", headers);
				assertEquals("Expected 1 content type header.", 1, headers.length);
				assertEquals("Unexpected content type.", "application/json", headers[0].getValue());

				String contentString = EntityUtils.toString(response.getEntity());
				Log.info(AcmeRevocationTest.class, methodName, "HTTP post contents: \n" + contentString);

				return contentString;
			}
		}
	}
	
	/**
	 * Revoke the certificate and ensure that the scheduler thread renews it.
	 * 
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void autoRenewOnRevoke() throws Exception {
		Certificate[] startingCertificateChain = null;

		/*
		 * Configure the acmeCA-2.0 feature.
		 * 
		 */

		ServerConfiguration configuration = originalServerConfig.clone();
		configureAcmeRevocation(configuration, true);
		configuration.getAcmeCA().setCertCheckerSchedule(configuration.getAcmeCA().getRenewCertMin() + "ms");
		
		AcmeFatUtils.configureAcmeCA(server, boulder, configuration, DOMAIN);

		/***********************************************************************
		 * TEST: The server generates a certificate normally. We revoke it and
		 * verify that the certificate checker automatically renews it.
		 * 
		 **********************************************************************/

		try {
			Log.info(this.getClass(), testName.getMethodName(), "TEST: Revoke certificate and check that we auto renew it.");

			/*
			 * Start the server and wait for acme to determine the certificate was good.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForSslEndpoint(server);



			/*
			 * Verify that the server is using a certificate signed by the CA.
			 */
			startingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, boulder);

			/*
			 * Revoke the certificate
			 */
			revokeCertificate();

			/*
			 * Wait for the cert checker to run and update
			 */
			assertNotNull("Should log message that the certificate was revoked",
					server.waitForStringInLogUsingMark("CWPKI2067I", (configuration.getAcmeCA().getRenewCertMin() * 3)) );

			assertNotNull("Should log message that the certificate was renewed",
					server.waitForStringInLogUsingMark("CWPKI2007I", (configuration.getAcmeCA().getRenewCertMin() * 3)) );

			AcmeFatUtils.waitForNewCert(server, boulder, startingCertificateChain);


		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST: Shutdown.");

			/*
			 * Stop the server.
			 */
			stopServer();
		}
	}
	
	/**
	 * Test the REST revoke endpoint, provide valid and invalid reasons for the revoke
	 * @throws Exception
	 */
	@Test
	public void certificate_endpoint_post_revoke_certificate() throws Exception {
		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		ServerConfiguration configuration = originalServerConfig.clone();
		configureAcmeRevocation(configuration, true);
		AcmeFatUtils.configureAcmeCA(server, boulder, configuration, false, true, DOMAIN);

		try {

			/*
			 * Startup the server.
			 */
			Log.info(this.getClass(), testName.getMethodName(), "Rest revokation test.");
			server.startServer();
			AcmeFatUtils.waitForSslToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);


			/*
			 * Revoke with invalid reason.
			 */
			String jsonResponse = performPost(CERTIFICATE_ENDPOINT, 400, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
					CONTENT_TYPE_JSON, JSON_CERT_REVOKE_INVALID_REASON);
			assertJsonResponse(jsonResponse, 400);

			/*
			 * Revoke with unspecified / default reason.
			 */
			jsonResponse = performPost(CERTIFICATE_ENDPOINT, 200, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
					CONTENT_TYPE_JSON, JSON_CERT_REVOKE_DEFAULT_REASON);
			assertJsonResponse(jsonResponse, 200);

			/*
			 * Request a new certificate.
			 */
			jsonResponse = performPost(CERTIFICATE_ENDPOINT, 200, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
					CONTENT_TYPE_JSON, JSON_CERT_REGEN);
			assertJsonResponse(jsonResponse, 200);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Revoke with a valid reason.
			 */
			jsonResponse = performPost(CERTIFICATE_ENDPOINT, 200, CONTENT_TYPE_JSON, ADMIN_USER, ADMIN_PASS,
					CONTENT_TYPE_JSON, JSON_CERT_REVOKE_VALID_REASON);
			assertJsonResponse(jsonResponse, 200);

		} finally {
			stopServer();
		}
	}

	/**
	 * Make a POST call to the REST endpoint.
	 * 
	 * @param endpoint            The endpoint to call.
	 * @param expectedStatus      The expected HTTP return code.
	 * @param expectedContentType The expected response content type.
	 * @param user                The user to make the call with.
	 * @param password            The password to make the call with.
	 * @return the response in the form of a string.
	 * @throws Exception If the call failed.
	 */
	private static String performPost(String endpoint, int expectedStatus, String expectedContentType, String user,
			String password, String contentType, String content) throws Exception {
		String methodName = "performPost()";

		try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

			/*
			 * Create a POST request to the Liberty server.
			 */
			HttpPost httpPost = new HttpPost("https://localhost:" + server.getHttpDefaultSecurePort() + endpoint);
			httpPost.setHeader("Authorization",
					"Basic " + DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));
			if (contentType != null && !contentType.isEmpty()) {
				httpPost.setHeader("Content-Type", contentType);
			}
			if (content != null && !content.isEmpty()) {
				httpPost.setEntity(new StringEntity(content));
			}

			/*
			 * Send the POST request and process the response.
			 */
			try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
				AcmeFatUtils.logHttpResponse(PebbleContainer.class, methodName, httpPost, response);

				StatusLine statusLine = response.getStatusLine();
				assertEquals("Unexpected status code response.", expectedStatus, statusLine.getStatusCode());

				/*
				 * Check content type header.
				 */
				if (expectedContentType != null) {
					Header[] headers = response.getHeaders("content-type");
					assertNotNull("Expected content type header.", headers);
					assertEquals("Expected 1 content type header.", 1, headers.length);
					assertEquals("Unexpected content type.", expectedContentType, headers[0].getValue());
				}

				String contentString = EntityUtils.toString(response.getEntity());
				Log.info(AcmeRevocationTest.class, methodName, "HTTP post contents: \n" + contentString);

				return contentString;
			}
		}
	}

	/**
	 * Find all files in the directory whose file name matches the given pattern.
	 * 
	 * @param dir     The directory to search.
	 * @param pattern The pattern to check file names against.
	 * @return The array of files that match.
	 */
	public static File[] findFilesThatMatch(String dir, final String pattern) {
		File searchDir = new File(dir);

		return searchDir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().matches(pattern);
			}
		});
	}

	/**
	 * Assert that the expected JSON response is returned.
	 * 
	 * @param jsonResponse       The actual JSON response.
	 * @param expectedStatusCode The expected status code.
	 */
	private static void assertJsonResponse(String jsonResponse, int expectedStatusCode) {
		assertThat("Unexpected HTTP status code returned in JSON response.", jsonResponse,
				containsString("\"httpCode\":" + expectedStatusCode));
		if (expectedStatusCode == 200) {
			assertThat("Expected no error message in JSON response.", jsonResponse,
					not(containsString("\"message\":")));
		} else {
			assertThat("Expected error message in JSON response.", jsonResponse, containsString("\"message\":"));
		}
	}

	private void stopServer(String...msgs) throws Exception {
		AcmeFatUtils.stopServer(server, msgs);
	}
	
	/**
	 * Start a pebble server and swap between pebble and boulder and make sure
	 * we still get a certificate when switching.
	 * 
	 * @throws Exception
	 *             if the test failed for some unforeseen reason.
	 */
	@Test
	public void swapBetweenCAProviders() throws Exception {
		final String methodName = testName.getMethodName();

		CAContainer pebble = new PebbleContainer();
		try {

			/*
			 * Configure and start with Pebble
			 */
			ServerConfiguration configuration = originalServerConfig.clone();
			configureAcmeRevocation(configuration, true);
			AcmeFatUtils.configureAcmeCA(server, pebble, configuration, false, DOMAIN);

			Log.info(AcmeSimpleTest.class, methodName, "Starting server with Pebble");
			server.startServer();
			AcmeFatUtils.waitForSslToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			server.setMarkToEndOfLog(server.getDefaultLogFile());

			Certificate[] certificates1 = AcmeFatUtils.assertAndGetServerCertificate(server, pebble);

			Log.info(AcmeSimpleTest.class, methodName, "Swap to Boulder");
			AcmeFatUtils.configureAcmeCA(server, boulder, configuration, false, DOMAIN);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			certificates1 = AcmeFatUtils.waitForNewCert(server, boulder, certificates1);
			server.setMarkToEndOfLog(server.getDefaultLogFile());

			Log.info(AcmeSimpleTest.class, methodName, "Swap back to Pebble");
			AcmeFatUtils.configureAcmeCA(server, pebble, configuration, false, DOMAIN);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			certificates1 = AcmeFatUtils.waitForNewCert(server, pebble, certificates1);

			Log.info(AcmeSimpleTest.class, methodName, "Swap back to Boulder");
			AcmeFatUtils.configureAcmeCA(server, boulder, configuration, false, DOMAIN);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			certificates1 = AcmeFatUtils.waitForNewCert(server, boulder, certificates1);
			server.setMarkToEndOfLog(server.getDefaultLogFile());

		} finally {
			try {
				stopServer();
			} finally {
				if (pebble != null) {
					pebble.stop();
				}
			}
		}
	}
}
