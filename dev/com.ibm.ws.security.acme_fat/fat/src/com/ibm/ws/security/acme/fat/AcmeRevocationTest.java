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
import org.junit.ClassRule;
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
import com.ibm.ws.security.acme.internal.web.AcmeCaRestHandler;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Verify that ACME will check the revocation status of the certificate and
 * replace it if necessary.
 * 
 * TODO There are no CRL tests.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AcmeRevocationTest {

	@Server("com.ibm.ws.security.acme.fat.revocation")
	public static LibertyServer server;

	@ClassRule
	public static CAContainer boulder = new BoulderContainer();

	private static ServerConfiguration originalServerConfig;

	private static final String ADMIN_USER = "administrator";

	private static final String ADMIN_PASS = "adminpass";

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() throws Exception {
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
	public void after() {
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
		AcmeFatUtils.configureAcmeCA(server, boulder, configuration, false, "domain.com");

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
			server.stopServer();
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
			server.stopServer();
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
		AcmeFatUtils.configureAcmeCA(server, boulder, configuration, false, "domain.com");

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
			server.stopServer();
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
			server.stopServer();
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
		AcmeFatUtils.configureAcmeCA(server, boulder, configuration, false, "domain.com");

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
			server.stopServer();
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
			server.stopServer("CWPKI2058W");
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
}
