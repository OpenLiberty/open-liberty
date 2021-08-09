/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.docker.pebble.PebbleContainer;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Disables the keystore trigger. Select a few tests from
 * AcmeSimpleTest to run.
 * 
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) // No value added
public class AcmeDisableTriggerSimpleTest {
	
	@Server("com.ibm.ws.security.acme.fat.trigger")
	public static LibertyServer server;
	
	protected static ServerConfiguration ORIGINAL_CONFIG;

	/*
	 * Domains that are configured and cleared before and after the class.
	 */
	private static final String[] DOMAINS_ALL = { "domain1.com", "domain2.com", "domain3.com" };
	private static final String[] DOMAINS_1 = { "domain1.com" };
	private static final String[] DOMAINS_2 = { "domain1.com", "domain2.com", "domain3.com" };
	private static final String[] DOMAINS_3 = { "domain1.com", "domain2.com" };
	private static final String[] DOMAINS_4 = { "domain2.com" };

	public static CAContainer caContainer;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() throws Exception {
		ORIGINAL_CONFIG = server.getServerConfiguration();
		caContainer = new PebbleContainer();
		AcmeFatUtils.checkPortOpen(caContainer.getHttpPort(), 60000);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (caContainer != null) {
			caContainer.stop();
		}
	}

	@After
	public void afterTest() throws Exception {
		/*
		 * Clear the DNS records for the domain. Required since a few of the
		 * tests setup invalid A records to test failure scenarios.
		 */
		AcmeFatUtils.clearDnsForDomains(caContainer, DOMAINS_ALL);

		/*
		 * Cleanup any generated ACME files.
		 */
		AcmeFatUtils.deleteAcmeFiles(server);
	}

	
	
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void update_domains() throws Exception {

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, false, DOMAINS_1);

		try {

			/*
			 * Start the server and wait for the certificate to be installed.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates1 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			/***********************************************************************
			 * 
			 * TEST 1: Add more domains. This should result in a refreshed
			 * certificate.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: START");
			AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, false, DOMAINS_2);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			BigInteger serial1 = ((X509Certificate) certificates1[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) certificates2[0]).getSerialNumber();
			assertFalse("Expected new certificate after adding new domain.", serial1.equals(serial2));
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: FINISH");

			/***********************************************************************
			 * 
			 * TEST 2: Remove a domain that IS NOT the subject CN. We SHOULD NOT
			 * refresh the certificate.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: START");
			AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, false, DOMAINS_3);
			AcmeFatUtils.waitForAcmeToNoOp(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates3 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			serial1 = ((X509Certificate) certificates2[0]).getSerialNumber();
			serial2 = ((X509Certificate) certificates3[0]).getSerialNumber();
			assertEquals("Expected same certificate after removing non-CN domain.", serial1, serial2);
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: FINISH");

			/***********************************************************************
			 * 
			 * TEST 3: Remove a domain that IS the subject CN. We SHOULD refresh
			 * the certificate.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), testName.getMethodName(), "TEST 3: START");
			AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, false, DOMAINS_4);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates4 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			serial1 = ((X509Certificate) certificates3[0]).getSerialNumber();
			serial2 = ((X509Certificate) certificates4[0]).getSerialNumber();
			assertFalse("Expected new certificate after adding new domain.", serial1.equals(serial2));
			Log.info(this.getClass(), testName.getMethodName(), "TEST 3: FINISH");

		} finally {
			/*
			 * Stop the server.
			 */
			stopServer();
		}
	}
	
	/**
	 * This test will verify that even when the ACME CA server fails to validate
	 * our domain on startup due to configuration issues that can't be validated
	 * before requesting the certificate, we can recover by updating
	 * configuration.
	 * 
	 * <p/>
	 * NOTE: This does not cover errors that are not due to configuration issues
	 * (for example, when the DNS record for the domain does not point at your
	 * server). Those will require a restart of the server.
	 * 
	 * @throws Exception
	 *             if the test fails for some unforeseen reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	@ExpectedFFDC({ "com.ibm.ws.security.acme.AcmeCaException" })
	@MinimumJavaLevel(javaLevel = 9)
	/* Minimum Java Level to avoid a known/fixed IBM Java 8 bug with an empty keystore, IJ19292. When the builds move to 8SR6, we can run this test again */
	public void startup_failure_recover() throws Exception {

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();

		/*
		 * Configure the acmeCA-2.0 feature. Point domain2.com to an invalid
		 * address. Any attempt to validate ownership of this domain is going to
		 * fail.
		 */
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, false, DOMAINS_2);
		caContainer.addDnsARecord("domain2.com", "127.0.0.1");

		try {
			/***********************************************************************
			 * 
			 * Start the server. The domain2.com domain will fail to validate.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, testName.getMethodName(), "Starting server.");
			server.startServer();
			server.waitForStringInLog("CWPKI2001E.*authorization challenge failed for the domain2.com domain");
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Try and connect to Liberty's HTTPS port. We should fail as there
			 * is no cert.
			 */
			try (CloseableHttpClient httpclient = AcmeFatUtils.getInsecureHttpsClient()) {

				/*
				 * Create a GET request to the Liberty server.
				 */
				HttpGet httpGet = new HttpGet("https://localhost:" + server.getHttpDefaultSecurePort());

				/*
				 * Send the GET request and process the response.
				 */
				try (final CloseableHttpResponse response = httpclient.execute(httpGet)) {
					fail("Expected HttpHostConnectException.");
				} catch (HttpHostConnectException e) {
					// Expected if the HTTPS endpoint is NOT up.
				} catch (SSLHandshakeException e) {
					// Expected if we wait for the HTTPS endpoint to come up.
				}
			}

			/***********************************************************************
			 * 
			 * Update the domains so that domain2.com is no longer in the
			 * configuration.
			 * 
			 **********************************************************************/
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, false, DOMAINS_1);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
		} finally {
			stopServer("CWPKI2001E", "CWPKI0804E", "CWWKO0801E");
		}
	}
	
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void toggle_acme_feature() throws Exception {

		/*
		 * Modify the configuration so that transportSecurity-1.0 feature is
		 * enabled, but acmeCA-2.0 is not enabled. Also enable servlet-4.0 so
		 * the HTTPS end-point comes up.
		 */
		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		configuration.getFeatureManager().getFeatures().remove("acmeCA-2.0");
		configuration.getFeatureManager().getFeatures().add("transportSecurity-1.0");
		configuration.getFeatureManager().getFeatures().add("servlet-4.0");
		configuration.getAcmeCA().setCertCheckerSchedule(AcmeConstants.RENEW_CERT_MIN_DEFAULT + "ms");
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, false, DOMAINS_1);

		try {

			/*
			 * Start the server and wait for the (self-signed) certificate to be
			 * installed.
			 */
			server.startServer();
			AcmeFatUtils.waitForSslToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is NOT using a certificate signed by the
			 * ACME CA.
			 */
			try {
				AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
				fail("Expected SignatureException.");
			} catch (SignatureException e) {
				assertEquals("Expected error message was not found.", "Signature does not match.", e.getMessage());
			}

			/***********************************************************************
			 * 
			 * TEST 1: Add the acmeCA-2.0 feature. This will generate a new ACME
			 * CA certificate.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: START");
			configuration = configuration.clone();
			configuration.getFeatureManager().getFeatures().add("acmeCA-2.0");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, false, DOMAINS_1);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the CA. We may
			 * need to a wait a short bit at the SSL config completes the update and clears
			 * the cache.
			 */
			Certificate[] certificates1 = AcmeFatUtils.waitForAcmeCert(server, caContainer, 10000);
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: FINISH");

			/***********************************************************************
			 * 
			 * TEST 2: Remove the acmeCA-2.0 feature. Certificate should not
			 * change.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: START");
			configuration = configuration.clone();
			configuration.getFeatureManager().getFeatures().remove("acmeCA-2.0");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, false, DOMAINS_1);
			AcmeFatUtils.waitAcmeFeatureUninstall(server);

			long timeElapsed = System.currentTimeMillis();

			/*
			 * Verify that the server is still using a certificate signed by the
			 * CA. The default certificate generator doesn't update the
			 * certificate.
			 */
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			BigInteger serial1 = ((X509Certificate) certificates1[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) certificates2[0]).getSerialNumber();
			assertEquals("Expected same certificate after removing acmeCA-2.0 feature.", serial1, serial2);
			
			/*
			 * Check log for the amount of time it would take to wake up/run the scheduler
			 */
			assertNull("Should not have found the cert checker waking up: " + AcmeFatUtils.ACME_CHECKER_TRACE,
					server.waitForStringInTrace(AcmeFatUtils.ACME_CHECKER_TRACE, configuration.getAcmeCA().getRenewCertMin() - timeElapsed + 1000));
			
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: FINISH");

			/***********************************************************************
			 * 
			 * TEST 3: Add the acmeCA-2.0 feature back, again. Certificate
			 * should not change.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), testName.getMethodName(), "TEST 3: START");
			configuration = configuration.clone();
			configuration.getFeatureManager().getFeatures().add("acmeCA-2.0");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, false, DOMAINS_1);
			AcmeFatUtils.waitForAcmeToNoOp(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates3 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			serial1 = ((X509Certificate) certificates2[0]).getSerialNumber();
			serial2 = ((X509Certificate) certificates3[0]).getSerialNumber();
			assertEquals("Expected same certificate after re-adding the acmeCA-2.0 feature.", serial1, serial2);

			/**
			 * Make sure the scheduler started again
			 */
			assertNotNull("Should have found the cert checker waking up: " + AcmeFatUtils.ACME_CHECKER_TRACE,
					server.waitForStringInTrace(AcmeFatUtils.ACME_CHECKER_TRACE));

			Log.info(this.getClass(), testName.getMethodName(), "TEST 3: FINISH");

		} finally {
			/*
			 * Stop the server.
			 */
			stopServer("CWPKI2058W");
		}
	}

	private void stopServer(String...msgs) throws Exception {
		AcmeFatUtils.stopServer(server, msgs);
	}
}
