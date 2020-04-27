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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

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

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Verify that we log the correct warnings for different
 * validity/renewBeforeExpiration settings
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class AcmeValidityAndRenewTest {

	@Server("com.ibm.ws.security.acme.fat.simple")
	public static LibertyServer server;

	private static ServerConfiguration ORIGINAL_CONFIG;
	private static final String[] DOMAINS1 = { "domain1.com" };

	public static CAContainer caContainer;

	public static final long timeBufferToExpire = 30000L; // milliseconds

	public static final int CHECKER_SECONDS = 1000;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() throws Exception {
		ORIGINAL_CONFIG = server.getServerConfiguration();
		caContainer = new com.ibm.ws.security.acme.docker.pebble.PebbleContainer();
		AcmeFatUtils.checkPortOpen(caContainer.getHttpPort(), 60000);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (caContainer != null) {
			caContainer.stop();
		}
	}

	@After
	public void afterTest() {
		/*
		 * Cleanup any generated ACME files.
		 */
		AcmeFatUtils.deleteAcmeFiles(server);

	}

	/**
	 * The server will start with a renewBeforeExpiration set lower than the lowest
	 * minimum renew time allowed. The renewBeforeExpiration is reset to the minimum
	 * renew time.
	 * 
	 * @throws Exception If the test failed for some reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void serverBelowMinRenew() throws Exception {
		/*
		 * Configure the acmeCA-2.0 feature.
		 * 
		 */

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		configuration.getAcmeCA().setRenewBeforeExpiration(AcmeConstants.RENEW_CERT_MIN - 1000 + "ms");
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

		/***********************************************************************
		 * 
		 * TEST 1: The server will start with a renewBeforeExpiration set lower than the
		 * lowest minimum renew time allowed.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), testName.getMethodName(), "TEST renew is set to below the minimum renew time.");

			/*
			 * Start the server and wait for the certificate to be installed.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is now using a certificate signed by the CA.
			 */
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			assertNotNull("Should log warning on minimum renewBeforeExpiration",
					server.waitForStringInLog("CWPKI2051W"));
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2051W");
		}
	}

	
	/**
	 * The server will start with a renewBeforeExpiration set lower than the warning
	 * level for having a short renew time. A warning message should be logged.
	 * 
	 * @throws Exception If the test failed for some reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void serverBelowWarnRenew() throws Exception {
		/*
		 * Configure the acmeCA-2.0 feature.
		 * 
		 */

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		configuration.getAcmeCA().setRenewBeforeExpiration(AcmeConstants.RENEW_CERT_MIN_WARN_LEVEL - 1000 + "ms");
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

		/***********************************************************************
		 * 
		 * TEST The server will start with a renewBeforeExpiration set lower than the
		 * warning level for having a short renew time.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), testName.getMethodName(), "TEST renew is set to below the minimum renew warning time.");

			/*
			 * Start the server and wait for acme to determine the certificate was good.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is using a certificate signed by the CA.
			 */
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			assertNotNull("Should log warning on renewBeforeExpiration being too short",
					server.waitForStringInLog("CWPKI2055W"));

		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: Shutdown.");
			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2055W");
		}

	}

	/**
	 * Starting the server with a renewBeforeExpiration longer than the certificate
	 * validity period. The renewBeforeExpiration will be reset to the default time.
	 * 
	 * @throws Exception
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void serverRenewLongerThanValidity() throws Exception {
		/*
		 * Configure the acmeCA-2.0 feature.
		 * 
		 */

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		configuration.getAcmeCA().setRenewBeforeExpiration(365 * 5 + 1 + "d");
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

		/***********************************************************************
		 * 
		 * TEST The server will start with a renewBeforeExpiration set longer than the
		 * certificate validity period..
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), testName.getMethodName(), "TEST renew is set longer than the certificate validity period.");

			/*
			 * Start the server and wait for acme to determine the certificate was good.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is using a certificate signed by the CA.
			 */
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			assertNotNull(
					"Should log warning that renewBeforeExpiration is too long compared to the cert validity period",
					server.waitForStringInLog("CWPKI2054W"));

		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2054W");
		}

	}

	/**
	 * Test renewing the certification on server startup due to the expiration date.
	 * 
	 * This test calculates the renewBeforeExpiration based on the validity period
	 * of the certificate received. It is slightly shorter than the validity period
	 * so we can test it in an automated test.
	 * 
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void autoRenewOnRestartAndCertChecker() throws Exception {
		Certificate[] startingCertificateChain = null, endingCertificateChain = null;

		/*
		 * Configure the acmeCA-2.0 feature.
		 * 
		 */

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

		long serverTime = System.currentTimeMillis();
		/***********************************************************************
		 * TEST 1: The server generate a certificate normally.
		 * 
		 **********************************************************************/

		try {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: renew is fine, get a certificate at startup.");

			/*
			 * Start the server and wait for acme to determine the certificate was good.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			serverTime = System.currentTimeMillis();

			/*
			 * Verify that the server is using a certificate signed by the CA.
			 */
			startingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer();
		}

		/***********************************************************************
		 * 
		 * TEST 2: Start with a very short renew period, causing a renewal on startup.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), testName.getMethodName(),
					"TEST 2: Restart with renew time close to validity period.");

			long notAfter = ((X509Certificate) startingCertificateChain[0]).getNotAfter().getTime();
			long notBefore = ((X509Certificate) startingCertificateChain[0]).getNotBefore().getTime();

			long extraBuffer = serverTime - notBefore;
			// check if the certificate is slightly in the future to make sure we wait long
			// enough.
			long totalBuffer = (extraBuffer > 0 ? extraBuffer : 0) + timeBufferToExpire;

			Log.info(this.getClass(), testName.getMethodName(),
					"Not before: " + notBefore + ", extra buffer " + extraBuffer);

			/*
			 * Set a renew time just shy of default Pebble validity period) so we will
			 * request a new certificate on restart)
			 */
			long justShyOfValidityPeriod = (notAfter - notBefore) - timeBufferToExpire;
			Log.info(this.getClass(), testName.getMethodName(), "Time configured: " + justShyOfValidityPeriod);

			/*
			 * Wait a bit before we start the server to make sure we'll be in the renew
			 * period.
			 */
			while ((System.currentTimeMillis() - serverTime) < totalBuffer) {
				Log.info(this.getClass(), testName.getMethodName(),
						"Waiting for " + totalBuffer + "ms to pass before restarting the server.");
				Thread.sleep(2000);
			}

			configuration = ORIGINAL_CONFIG.clone();
			configuration.getAcmeCA().setRenewBeforeExpiration(justShyOfValidityPeriod + "ms");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server got a new certificate
			 */
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			assertNotSame("The certificate should have been marked as expired at startup and renewed.",
					((X509Certificate) startingCertificateChain[0]).getSerialNumber(),
					((X509Certificate) endingCertificateChain[0]).getSerialNumber());

			assertNotNull("Should log  message that the certificate was renewed",
					server.waitForStringInLogUsingMark("CWPKI2052I"));
		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2055W"); // we are running with and intentionally short renewBeforeExpiration.
		}

		/***********************************************************************
		 * 
		 * TEST 3: Start with a very short renew period and short cert checker time out,
		 * causing a renew request at runtime.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), testName.getMethodName(),
					"TEST 3: Restart with renew time close to validity period and short cert checker.");

			configuration.getAcmeCA().setCertCheckerSchedule((timeBufferToExpire + 1000) + "ms");
			configuration.getAcmeCA().setCertCheckerSchedule(AcmeConstants.RENEW_CERT_MIN + "ms");

			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server got a new certificate at startup
			 */
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			String serial1 = ((X509Certificate) startingCertificateChain[0]).getSerialNumber().toString(16);
			String serial2 = ((X509Certificate) endingCertificateChain[0]).getSerialNumber().toString(16);

			assertThat("The certificate should have been marked as expired at startup and renewed.", serial1, not(equalTo(serial2)));	

			startingCertificateChain = endingCertificateChain;

			/*
			 * The certificate checker should automatically renew the certificate.
			 */
			AcmeFatUtils.waitForNewCert(server, caContainer, startingCertificateChain, timeBufferToExpire * 2);

			assertNotNull("Should log message that the certificate was renewed",
					server.waitForStringInLogUsingMark("CWPKI2052I"));

		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 3: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2049W");
		}
	}

	/**
	 * This test will verify that the server starts up and requests a new
	 * certificate from the ACME server when required.
	 * 
	 * @throws Exception If the test failed for some reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void certCheckerRunningNoRenew() throws Exception {

		Certificate[] startingCertificateChain = null, endingCertificateChain = null;

		/*
		 * Configure the acmeCA-2.0 feature.
		 */

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		configuration.getAcmeCA().setCertCheckerSchedule(AcmeConstants.RENEW_CERT_MIN + "ms");
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

		/***********************************************************************
		 * 
		 * TEST 1: The server will start up without the specified keystore available. It
		 * should generate a new keystore with a certificate retrieved from the ACME CA
		 * server.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: Start");

			/*
			 * Start the server and wait for the certificate to be installed.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is now using a certificate signed by the CA.
			 */
			startingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			assertNotNull("Should have found the cert checker waking up: " + AcmeFatUtils.ACME_CHECKER_TRACE,
					server.waitForStringInTrace(AcmeFatUtils.ACME_CHECKER_TRACE));

			/*
			 * Should have the same certificate
			 */
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			assertEquals("The certificate should be the same after the cert checker runs.",
					((X509Certificate) startingCertificateChain[0]).getSerialNumber(),
					((X509Certificate) endingCertificateChain[0]).getSerialNumber());
		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer();
		}
	}

	/**
	 * This test will verify that the server starts up and requests a new
	 * certificate from the ACME server when required.
	 * 
	 * @throws Exception If the test failed for some reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void certCheckerDisabled() throws Exception {

		Certificate[] startingCertificateChain = null, endingCertificateChain = null;

		/*
		 * Configure the acmeCA-2.0 feature.
		 */

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		configuration.getAcmeCA().setCertCheckerSchedule(AcmeConstants.RENEW_CERT_MIN + "ms");
		configuration.getAcmeCA().setRenewBeforeExpiration("0ms");
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

		/***********************************************************************
		 * 
		 * TEST 1: The server will start up without the specified keystore available. It
		 * should generate a new keystore with a certificate retrieved from the ACME CA
		 * server.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: Start");

			/*
			 * Start the server and wait for the certificate to be installed.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is now using a certificate signed by the CA.
			 */
			startingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
			assertNull("Should not have found the cert checker waking up: " + AcmeFatUtils.ACME_CHECKER_TRACE,
					server.waitForStringInTrace(AcmeFatUtils.ACME_CHECKER_TRACE, CHECKER_SECONDS + 5000));

			/*
			 * Should have the same certificate
			 */
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			assertEquals("The certificate should be the same after the cert checker runs.",
					((X509Certificate) startingCertificateChain[0]).getSerialNumber(),
					((X509Certificate) endingCertificateChain[0]).getSerialNumber());
			
			assertNotNull("Should log disabled message", server.findStringsInLogs("CWPKI2069I"));
		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer();
		}
	}

	/**
	 * Verifies that the certificate checker can hit an exception, recover and fetch
	 * a new certificate.
	 * 
	 * The setRenewBeforeExpiration is configured to be slightly less than the
	 * Pebble certificate validity period so the certificate checker will renew it
	 * for us.
	 * 
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	@AllowedFFDC(value = { "java.io.IOException", "org.shredzone.acme4j.exception.AcmeNetworkException",
			"com.ibm.ws.security.acme.AcmeCaException" })
	public void autoRenewWithErrorAndRecovery() throws Exception {
		Certificate[] startingCertificateChain = null;

		/*
		 * Configure the acmeCA-2.0 feature.
		 * 
		 */

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

		try {
			Log.info(this.getClass(), testName.getMethodName(),
					"TEST: Restart the Challenge server, check that the cert checker hits an error and recovers.");

			/*
			 * Start the server and wait for acme to determine the certificate was good.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is using a certificate signed by the CA.
			 */
			startingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			/*
			 * Set a setRenewBeforeExpiration time just shy of default Pebble validity
			 * period so we will request a new certificate on restart)
			 */
			long notAfter = ((X509Certificate) startingCertificateChain[0]).getNotAfter().getTime();
			long notBefore = ((X509Certificate) startingCertificateChain[0]).getNotBefore().getTime();
			long justShyOfValidityPeriod = (notAfter - notBefore) - timeBufferToExpire;
			Log.info(this.getClass(), testName.getMethodName(), "Time configured: " + justShyOfValidityPeriod);

			configuration.getAcmeCA().setRenewBeforeExpiration(justShyOfValidityPeriod + "ms");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);
			configuration.getAcmeCA().setCertCheckerSchedule((timeBufferToExpire + 1000) + "ms");
			configuration.getAcmeCA().setCertCheckerErrorSchedule(AcmeConstants.RENEW_CERT_MIN + "ms");

			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

			/*
			 * The certificate checker should automatically renew the certificate.
			 */
			AcmeFatUtils.waitForNewCert(server, caContainer, startingCertificateChain, timeBufferToExpire * 2);

			assertNotNull("Should log message that the certificate was renewed",
					server.waitForStringInLogUsingMark("CWPKI2052I"));

			assertNotNull("Should log message that the certificate was renewed after restarting Pebble",
					server.waitForStringInLogUsingMark("CWPKI2007I"));
			server.setMarkToEndOfLog();

			startingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			/*
			 * Stop the Challenge server so we should hit an error on the cert checker.
			 *
			 */
			caContainer.stopDNSServer();

			assertNotNull("Should log message that the certificate renew failed",
					server.waitForStringInLogUsingMark("CWPKI2065W", timeBufferToExpire * 2));

			/*
			 * Start the Challenge server and the cert checker should recover and fetch a
			 * new cert
			 *
			 */
			try {
				caContainer.startDNSServer();
			} catch (Throwable t) {

				/*
				 * Running locally on windows, the start fails intermittently
				 */
				Log.info(this.getClass(), testName.getMethodName(),
						"First try to restart DNS server failed, try again");
				caContainer.startDNSServer();
			}

			assertNotNull("Should log message that the certificate was renewed after restarting the challenge server",
					server.waitForStringInLogUsingMark("CWPKI2007I", (AcmeConstants.RENEW_CERT_MIN * 2)));
			AcmeFatUtils.waitForNewCert(server, caContainer, startingCertificateChain, timeBufferToExpire);

		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2049W", "CWPKI2065W");
		}
	}

}