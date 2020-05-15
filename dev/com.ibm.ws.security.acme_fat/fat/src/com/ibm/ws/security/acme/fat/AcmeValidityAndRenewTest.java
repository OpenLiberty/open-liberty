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

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

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
	 * The server will start with a renewBeforeExpiration set lower than the lowest minimum
	 * renew time allowed. The renewBeforeExpiration is reset to the minimum renew time.
	 * 
	 * @throws Exception If the test failed for some reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void serverBelowMinRenew() throws Exception {
		final String methodName = "serverBelowMinRenew";

		/*
		 * Configure the acmeCA-2.0 feature.
		 * 
		 */

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		configuration.getAcmeCA().setRenewBeforeExpiration(AcmeConstants.RENEW_CERT_MIN - 1000 + "ms");
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

		/***********************************************************************
		 * 
		 * TEST 1: The server will start with a renewBeforeExpiration set lower than the lowest minimum
		 * renew time allowed.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), methodName, "TEST renew is set to below the minimum renew time.");

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

		} finally {
			Log.info(this.getClass(), methodName, "TEST 1: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2051W");
		}
	}

	
	/**
	 * The server will start with a renewBeforeExpiration set lower than the warning level
	 * for having a short renew time. A warning message should be logged.
	 * 
	 * @throws Exception If the test failed for some reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void serverBelowWarnRenew() throws Exception {
		final String methodName = "serverBelowWarnRenew";

		/*
		 * Configure the acmeCA-2.0 feature.
		 * 
		 */

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		configuration.getAcmeCA().setRenewBeforeExpiration(AcmeConstants.RENEW_CERT_MIN_WARN_LEVEL - 1000 + "ms");
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

		/***********************************************************************
		 * 
		 * TEST The server will start with a renewBeforeExpiration set lower than the warning level
		 * for having a short renew time.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), methodName, "TEST renew is set to below the minimum renew warning time.");

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
			Log.info(this.getClass(), methodName, "TEST: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2055W");
		}

	}

	/**
	 * Starting the server with a renewBeforeExpiration longer than the certificate
	 * validity period. The renewBeforeExpiration will be reset to the default time.
	 * @throws Exception
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void serverRenewLongerThanValidity() throws Exception {
		final String methodName = "serverRenewLongerThanValidity";

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
			Log.info(this.getClass(), methodName, "TEST renew is set longer than the certificate validity period.");

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
			Log.info(this.getClass(), methodName, "TEST Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2054W");
		}

	}

	/**
	 * Test renewing the certification on server startup due to the expiration date.
	 * 
	 * This test calculates the renewBeforeExpiration based on the validity period of
	 * the certificate received. It is slightly shorter than the validity period so we
	 * can test it in an automated test.
	 * 
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void serverRenewOnRestart() throws Exception {
		final String methodName = "serverRenewOnRestart";
		Certificate[] startingCertificateChain = null, endingCertificateChain = null;

		/*
		 * Configure the acmeCA-2.0 feature.
		 * 
		 */

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, DOMAINS1);

		long serverTime = System.currentTimeMillis();
		/***********************************************************************
		 * 
		 * TEST 1: The server generate a certificate normally.
		 * 
		 **********************************************************************/

		try {
			Log.info(this.getClass(), methodName, "TEST 1: renew is fine, get a certificate at startup.");

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
			Log.info(this.getClass(), methodName, "TEST 1: Shutdown.");

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
			Log.info(this.getClass(), methodName, "TEST 2: Restart with renew time close to validity period.");

			long notAfter = ((X509Certificate) startingCertificateChain[0]).getNotAfter().getTime();
			long notBefore = ((X509Certificate) startingCertificateChain[0]).getNotBefore().getTime();

			long extraBuffer = serverTime - notBefore;
			// check if the certificate is slightly in the future to make sure we wait long
			// enough.
			long totalBuffer = (extraBuffer > 0 ? extraBuffer : 0) + timeBufferToExpire;

			Log.info(this.getClass(), methodName, "Not before: " + notBefore + ", extra buffer " + extraBuffer);

			/*
			 * Set a renew time just shy of default Pebble validity period) so we will
			 * request a new certificate on restart)
			 */
			long justShyOfValidityPeriod = (notAfter - notBefore) - timeBufferToExpire;
			Log.info(this.getClass(), methodName, "Time configured: " + justShyOfValidityPeriod);

			/*
			 * Wait a bit before we start the server to make sure we'll be in the renew
			 * period.
			 */
			while ((System.currentTimeMillis() - serverTime) < totalBuffer) {
				Log.info(this.getClass(), methodName,
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
		} finally {
			Log.info(this.getClass(), methodName, "TEST 2: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2055W"); // we are running with and intentionally short renewBeforeExpiration.
		}

	}
}