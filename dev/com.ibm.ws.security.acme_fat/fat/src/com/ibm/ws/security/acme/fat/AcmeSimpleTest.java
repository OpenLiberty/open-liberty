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
import static junit.framework.Assert.fail;

import java.math.BigInteger;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.PebbleContainer;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Some simple test for running the acmeCA-2.0 feature.
 */
@RunWith(FATRunner.class)
public class AcmeSimpleTest {

	@Server("com.ibm.ws.security.acme.fat.simple")
	public static LibertyServer server;

	private static ServerConfiguration ORIGINAL_CONFIG;
	private static final String[] DOMAINS_ALL = { "domain1.com", "domain2.com", "domain3.com" };
	private static final String[] DOMAINS1 = { "domain1.com" };
	private static final String[] DOMAINS2 = { "domain1.com", "domain2.com", "domain3.com" };
	private static final String[] DOMAINS3 = { "domain1.com", "domain2.com" };
	private static final String[] DOMAINS4 = { "domain2.com" };

	@BeforeClass
	public static void beforeClass() throws Exception {
		ORIGINAL_CONFIG = server.getServerConfiguration();

		/*
		 * Configure mock DNS server.
		 */
		AcmeFatUtils.configureDnsForDomains(DOMAINS_ALL);
		AcmeFatUtils.checkPortOpen(PebbleContainer.HTTP_PORT, 60000);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		/*
		 * Clear the DNS records for the domain.
		 */
		AcmeFatUtils.clearDnsForDomains(DOMAINS_ALL);
	}

	@After
	public void afterTest() {
		/*
		 * Cleanup any generated ACME files.
		 */
		AcmeFatUtils.deleteAcmeFiles(server);

	}

	/**
	 * This test will verify that the server starts up and requests a new
	 * certificate from the ACME server when required.
	 * 
	 * @throws Exception
	 *             If the test failed for some reason.
	 */
	@Test
	public void startupServer() throws Exception {
		final String methodName = "startupServer()";
		Certificate[] startingCertificateChain = null, endingCertificateChain = null;

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, ORIGINAL_CONFIG, DOMAINS3);

		/***********************************************************************
		 * 
		 * TEST 1: The server will start up without the specified keystore
		 * available. It should generate a new keystore with a certificate
		 * retrieved from the ACME CA server.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), methodName, "TEST 1: Start");

			/*
			 * Start the server and wait for the certificate to be installed.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server);

		} finally {
			Log.info(this.getClass(), methodName, "TEST 1: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer();
		}

		/***********************************************************************
		 * 
		 * TEST 2: The server will start up with the ACME certificate generated
		 * in the first test. The same certificate will be used as it is still
		 * valid.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), methodName, "TEST 2: Start");

			/*
			 * Save the previous certificate chain.
			 */
			startingCertificateChain = endingCertificateChain;

			/*
			 * Start the server and wait for acme to determine the certificate
			 * was good.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForAcmeToNoOp(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is using a certificate signed by the CA.
			 */
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server);

			assertEquals("The certificate should not have been updated.",
					((X509Certificate) startingCertificateChain[0]).getSerialNumber(),
					((X509Certificate) endingCertificateChain[0]).getSerialNumber());
		} finally {
			Log.info(this.getClass(), methodName, "TEST 2: Shutdown.");

			/*
			 * Stop the server.
			 */
			server.stopServer();
		}

		/***********************************************************************
		 * 
		 * TEST 3: The server will start up with a self-signed certificate. It
		 * should recognize it is not a valid certificate and replace it.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), methodName, "TEST 3: Start");

			/*
			 * Create a self-signed certificate.
			 */
			startingCertificateChain = AcmeFatUtils.generateSelfSignedCertificate(server);

			/*
			 * Start the server and wait for acme to replace the certificate.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForAcmeToReplaceCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is using a certificate signed by the CA.
			 */
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server);

			BigInteger serial1 = ((X509Certificate) startingCertificateChain[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) endingCertificateChain[0]).getSerialNumber();
			assertFalse("Expected new certificate when starting with self-signed certificate: " + serial1 +", "+ serial2, serial1.equals(serial2));

		} finally {
			Log.info(this.getClass(), methodName, "TEST 3: Shutdown");

			/*
			 * Stop the server.
			 */
			server.stopServer();
		}
	}

	@Test
	public void updateDomains() throws Exception {
		final String methodName = "updateDomains()";

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, ORIGINAL_CONFIG, DOMAINS1);

		try {

			/*
			 * Start the server and wait for the certificate to be installed.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates1 = AcmeFatUtils.assertAndGetServerCertificate(server);

			/***********************************************************************
			 * 
			 * TEST 1: Add more domains. This should result in a refreshed
			 * certificate.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), methodName, "TEST 1: START");
			AcmeFatUtils.configureAcmeCA(server, ORIGINAL_CONFIG, DOMAINS2);
			AcmeFatUtils.waitForAcmeToReplaceCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server);

			BigInteger serial1 = ((X509Certificate) certificates1[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) certificates2[0]).getSerialNumber();
			assertFalse("Expected new certificate after adding new domain.", serial1.equals(serial2));
			Log.info(this.getClass(), methodName, "TEST 1: FINISH");

			/***********************************************************************
			 * 
			 * TEST 2: Remove a domain that IS NOT the subject CN. We SHOULD NOT
			 * refresh the certificate.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), methodName, "TEST 2: START");
			AcmeFatUtils.configureAcmeCA(server, ORIGINAL_CONFIG, DOMAINS3);
			AcmeFatUtils.waitForAcmeToNoOp(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates3 = AcmeFatUtils.assertAndGetServerCertificate(server);

			serial1 = ((X509Certificate) certificates2[0]).getSerialNumber();
			serial2 = ((X509Certificate) certificates3[0]).getSerialNumber();
			assertEquals("Expected same certificate after removing non-CN domain.", serial1, serial2);
			Log.info(this.getClass(), methodName, "TEST 2: FINISH");

			/***********************************************************************
			 * 
			 * TEST 3: Remove a domain that IS the subject CN. We SHOULD refresh
			 * the certificate.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), methodName, "TEST 3: START");
			AcmeFatUtils.configureAcmeCA(server, ORIGINAL_CONFIG, DOMAINS4);
			AcmeFatUtils.waitForAcmeToReplaceCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates4 = AcmeFatUtils.assertAndGetServerCertificate(server);

			serial1 = ((X509Certificate) certificates3[0]).getSerialNumber();
			serial2 = ((X509Certificate) certificates4[0]).getSerialNumber();
			assertFalse("Expected new certificate after adding new domain.", serial1.equals(serial2));
			Log.info(this.getClass(), methodName, "TEST 3: FINISH");

		} finally {
			/*
			 * Stop the server.
			 */
			server.stopServer();
		}
	}

	@Test
	public void dynamicallyEnableDisableAcmeFeature() throws Exception {
		final String methodName = "dynamicallyEnableDisableAcmeFeature()";

		/*
		 * Modify the configuration so that transportSecurity-1.0 feature is
		 * enabled, but acmeCA-2.0 is not enabled. Also enable servlet-4.0 so
		 * the HTTPS end-point comes up.
		 */
		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		configuration.getFeatureManager().getFeatures().remove("acmeCA-2.0");
		configuration.getFeatureManager().getFeatures().add("transportSecurity-1.0");
		configuration.getFeatureManager().getFeatures().add("servlet-4.0");
		AcmeFatUtils.configureAcmeCA(server, configuration, DOMAINS1);

		try {

			/*
			 * Start the server and wait for the (self-signed) certificate to be
			 * installed.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is NOT using a certificate signed by the
			 * ACME CA.
			 */
			try {
				AcmeFatUtils.assertAndGetServerCertificate(server);
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
			Log.info(this.getClass(), methodName, "TEST 1: START");
			configuration = configuration.clone();
			configuration.getFeatureManager().getFeatures().add("acmeCA-2.0");
			AcmeFatUtils.configureAcmeCA(server, configuration, DOMAINS1);
			AcmeFatUtils.waitForAcmeToReplaceCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates1 = AcmeFatUtils.assertAndGetServerCertificate(server);
			Log.info(this.getClass(), methodName, "TEST 1: FINISH");

			/***********************************************************************
			 * 
			 * TEST 2: Remove the acmeCA-2.0 feature. Certificate should not
			 * change.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), methodName, "TEST 2: START");
			configuration = configuration.clone();
			configuration.getFeatureManager().getFeatures().remove("acmeCA-2.0");
			AcmeFatUtils.configureAcmeCA(server, configuration, DOMAINS1);
			AcmeFatUtils.waitAcmeFeatureUninstall(server);

			/*
			 * Verify that the server is still using a certificate signed by the
			 * CA. The default certificate generator doesn't update the
			 * certificate.
			 */
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server);

			BigInteger serial1 = ((X509Certificate) certificates1[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) certificates2[0]).getSerialNumber();
			assertEquals("Expected same certificate after removing acmeCA-2.0 feature.", serial1, serial2);
			Log.info(this.getClass(), methodName, "TEST 2: FINISH");

			/***********************************************************************
			 * 
			 * TEST 3: Add the acmeCA-2.0 feature back, again. Certificate
			 * should not change.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), methodName, "TEST 3: START");
			configuration = configuration.clone();
			configuration.getFeatureManager().getFeatures().add("acmeCA-2.0");
			AcmeFatUtils.configureAcmeCA(server, configuration, DOMAINS1);
			AcmeFatUtils.waitForAcmeToNoOp(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates3 = AcmeFatUtils.assertAndGetServerCertificate(server);

			serial1 = ((X509Certificate) certificates2[0]).getSerialNumber();
			serial2 = ((X509Certificate) certificates3[0]).getSerialNumber();
			assertEquals("Expected same certificate after re-adding the acmeCA-2.0 feature.", serial1, serial2);
			Log.info(this.getClass(), methodName, "TEST 3: FINISH");

		} finally {
			/*
			 * Stop the server.
			 */
			server.stopServer();
		}
	}
}
