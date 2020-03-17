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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.math.BigInteger;
import java.nio.file.Files;
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
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.docker.pebble.PebbleContainer;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;
import componenttest.annotation.CheckForLeakedPasswords;
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

	protected static ServerConfiguration ORIGINAL_CONFIG;
	private boolean usePebbleURI = false;
	private static final String[] DOMAINS_ALL = { "domain1.com", "domain2.com", "domain3.com" };
	private static final String[] DOMAINS1 = { "domain1.com" };
	private static final String[] DOMAINS2 = { "domain1.com", "domain2.com", "domain3.com" };
	private static final String[] DOMAINS3 = { "domain1.com", "domain2.com" };
	private static final String[] DOMAINS4 = { "domain2.com" };
	
	public static CAContainer caContainer;

	@BeforeClass
	public static void beforeClass() throws Exception {
		ORIGINAL_CONFIG = server.getServerConfiguration();
		caContainer = new PebbleContainer();
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
	 * Whether to use "acme://" style URIs, or "https://" style URIs.
	 * 
	 * <p/>
	 * Sub-classes can override this method to determine which style URIs should
	 * be used.
	 * 
	 * @return Whether to use "acme://" style URIs.
	 */
	protected boolean useAcmeURIs() {
		return false;
	}

	/**
	 * This test will verify that the server starts up and requests a new
	 * certificate from the ACME server when required.
	 * 
	 * @throws Exception
	 *             If the test failed for some reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.PEBBLE_TRUSTSTORE_PASSWORD)
	public void startupServer() throws Exception {
		final String methodName = "startupServer()";
		Certificate[] startingCertificateChain = null, endingCertificateChain = null;

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, caContainer.getAcmeDirectoryURI(usePebbleURI), ORIGINAL_CONFIG, DOMAINS3);

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
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
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
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

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
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			BigInteger serial1 = ((X509Certificate) startingCertificateChain[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) endingCertificateChain[0]).getSerialNumber();
			assertFalse(
					"Expected new certificate when starting with self-signed certificate: " + serial1 + ", " + serial2,
					serial1.equals(serial2));

		} finally {
			Log.info(this.getClass(), methodName, "TEST 3: Shutdown");

			/*
			 * Stop the server.
			 */
			server.stopServer();
		}
	}

	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.PEBBLE_TRUSTSTORE_PASSWORD)
	public void updateDomains() throws Exception {
		final String methodName = "updateDomains()";

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, caContainer.getAcmeDirectoryURI(usePebbleURI), ORIGINAL_CONFIG, DOMAINS1);

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
			Certificate[] certificates1 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			/***********************************************************************
			 * 
			 * TEST 1: Add more domains. This should result in a refreshed
			 * certificate.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), methodName, "TEST 1: START");
			AcmeFatUtils.configureAcmeCA(server, caContainer.getAcmeDirectoryURI(usePebbleURI), ORIGINAL_CONFIG, DOMAINS2);
			AcmeFatUtils.waitForAcmeToReplaceCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

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
			AcmeFatUtils.configureAcmeCA(server, caContainer.getAcmeDirectoryURI(usePebbleURI), ORIGINAL_CONFIG, DOMAINS3);
			AcmeFatUtils.waitForAcmeToNoOp(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates3 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

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
			AcmeFatUtils.configureAcmeCA(server, caContainer.getAcmeDirectoryURI(usePebbleURI), ORIGINAL_CONFIG, DOMAINS4);
			AcmeFatUtils.waitForAcmeToReplaceCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates4 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

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
	@CheckForLeakedPasswords(AcmeFatUtils.PEBBLE_TRUSTSTORE_PASSWORD)
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
		AcmeFatUtils.configureAcmeCA(server, caContainer.getAcmeDirectoryURI(usePebbleURI), configuration, DOMAINS1);

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
			Log.info(this.getClass(), methodName, "TEST 1: START");
			configuration = configuration.clone();
			configuration.getFeatureManager().getFeatures().add("acmeCA-2.0");
			AcmeFatUtils.configureAcmeCA(server, caContainer.getAcmeDirectoryURI(usePebbleURI), configuration, DOMAINS1);
			AcmeFatUtils.waitForAcmeToReplaceCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates1 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
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
			AcmeFatUtils.configureAcmeCA(server, caContainer.getAcmeDirectoryURI(usePebbleURI), configuration, DOMAINS1);
			AcmeFatUtils.waitAcmeFeatureUninstall(server);

			/*
			 * Verify that the server is still using a certificate signed by the
			 * CA. The default certificate generator doesn't update the
			 * certificate.
			 */
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

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
			AcmeFatUtils.configureAcmeCA(server, caContainer.getAcmeDirectoryURI(usePebbleURI), configuration, DOMAINS1);
			AcmeFatUtils.waitForAcmeToNoOp(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			Certificate[] certificates3 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			serial1 = ((X509Certificate) certificates2[0]).getSerialNumber();
			serial2 = ((X509Certificate) certificates3[0]).getSerialNumber();
			assertEquals("Expected same certificate after re-adding the acmeCA-2.0 feature.", serial1, serial2);
			Log.info(this.getClass(), methodName, "TEST 3: FINISH");

		} finally {
			/*
			 * Stop the server.
			 */
			server.stopServer("CWPKI2038W");
		}
	}


	/**
	 * This test will start with ACME configuration that has a multitude of
	 * errors. It will then address each error in sequence, verifying that the
	 * correct errors are logged. At the very end, after having addressed all
	 * issues, it will verify that the certificate is still retrieved.
	 * 
	 * @throws Exception
	 *             If there was an unforeseen error.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.PEBBLE_TRUSTSTORE_PASSWORD)
	@AllowedFFDC(value = { "java.io.IOException", "java.security.KeyStoreException",
			"com.ibm.websphere.ssl.SSLException", "org.shredzone.acme4j.exception.AcmeNetworkException",
			"java.io.FileNotFoundException" })
	public void dynamicallyUpdateBadConfig() throws Exception {
		final String methodName = "dynamicallyUpdateBadConfig()";

		/*
		 * Do some setup of necessary configuration objects for the test.
		 */
		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();

		File unreadableFile = File.createTempFile("unreadable", ".key");
		unreadableFile.setReadable(false);
		unreadableFile.deleteOnExit();

		File unwritableDir = Files.createTempDirectory("unwritable").toFile();
		unwritableDir.setWritable(false);
		unwritableDir.deleteOnExit();

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeTransportConfig acmeTransportConfig = new AcmeTransportConfig();
		acmeTransportConfig.setProtocol("SSL");
		acmeTransportConfig.setTrustStore("INVALID_TRUSTSTORE");
		acmeTransportConfig.setTrustStorePassword("INVALID_PASSWORD");
		acmeTransportConfig.setTrustStoreType("INVALID_TYPE");

		AcmeCA acmeCA = configuration.getAcmeCA();
		acmeCA.setAccountKeyFile(unreadableFile.getAbsolutePath());
		acmeCA.setAcmeTransportConfig(acmeTransportConfig);
		acmeCA.setDomainKeyFile(unreadableFile.getAbsolutePath());
		acmeCA.setSubjectDN("cn=baddomain.com");
		AcmeFatUtils.configureAcmeCA(server, configuration);

		try {
			/***********************************************************************
			 * 
			 * Start the server. The 'directoryURI' and 'domain' attributes are
			 * missing.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Starting server.");
			server.startServer();
			assertNotNull("Expected CWWKG0095E in logs.", server.waitForStringInLog(
					"CWWKG0095E: The element acmeCA is missing the required attribute directoryURI."));
			assertNotNull("Expected CWWKG0095E in logs.", server
					.waitForStringInLog("CWWKG0095E: The element acmeCA is missing the required attribute domain."));

			/***********************************************************************
			 * 
			 * Set empty string 'directoryURI' and 'domain' attributes.
			 * Directory URI is empty.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Test 1 - empty directoryURI");
			acmeCA.setDirectoryURI("");
			acmeCA.setDomain(Arrays.asList(new String[] { "" }));
			AcmeFatUtils.configureAcmeCA(server, configuration);
			assertNotNull("Expected CWPKI2008E in logs.", server.waitForStringInLog("CWPKI2008E"));

			/***********************************************************************
			 * 
			 * Add non-empty 'domain' and 'directoryURI' attributes. The
			 * subjectDN contains a bad domain in the 'cn'.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Test 2 - subjectDN has invalid domain in cn");
			acmeCA.setDomain(Arrays.asList(DOMAINS2));
			acmeCA.setDirectoryURI("https://invalid.com/directory");
			AcmeFatUtils.configureAcmeCA(server, configuration);
			assertNotNull("Expected CWPKI2039E in logs.", server.waitForStringInLog("CWPKI2039E"));

			/***********************************************************************
			 * 
			 * The subjectDN's cn RDN is not the first RDN.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Test 3 - subjectDN cn is not first RDN");
			acmeCA.setSubjectDN("ou=liberty,cn=domain1.com");
			AcmeFatUtils.configureAcmeCA(server, configuration);
			assertNotNull("Expected CWPKI2040E in logs.", server.waitForStringInLog("CWPKI2040E"));

			/***********************************************************************
			 * 
			 * The subjectDN contains a bad RDN type.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Test 4 - subjectDN has invalid RDN type");
			acmeCA.setSubjectDN("badtype=domain1.com");
			AcmeFatUtils.configureAcmeCA(server, configuration);
			assertNotNull("Expected CWPKI2041E in logs.", server.waitForStringInLog("CWPKI2041E"));

			/***********************************************************************
			 * 
			 * The subjectDN is not a valid DN.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Test 5 - subjectDN has invalid DN");
			acmeCA.setSubjectDN("invaliddn");
			AcmeFatUtils.configureAcmeCA(server, configuration);
			assertNotNull("Expected CWPKI2042E in logs.", server.waitForStringInLog("CWPKI2042E"));

			/***********************************************************************
			 * 
			 * Set valid 'subjectDN' attribute. The account key file is
			 * unreadable.
			 * 
			 **********************************************************************/
			
			if (System.getProperty("os.name", "unknown").toLowerCase().contains("windows")) { // windows not enforcing the setReadable/setWriteable
				Log.info(AcmeSimpleTest.class, methodName, "Skipping unreadable/unwriteable file tests on Windows: " + System.getProperty("os.name", "unknown"));
				acmeCA.setSubjectDN("cn=domain1.com");
				acmeCA.setAccountKeyFile(null);
			} else {
				Log.info(AcmeSimpleTest.class, methodName, "Test 6 - unreadable account key file");
				acmeCA.setSubjectDN("cn=domain1.com");
				AcmeFatUtils.configureAcmeCA(server, configuration);
				assertNotNull("Expected CWPKI2021E in logs.", server.waitForStringInLog("CWPKI2021E"));

				/***********************************************************************
				 * 
				 * Set the account key file to be unwritable. The account key file
				 * is unwritable.
				 * 
				 **********************************************************************/
				Log.info(AcmeSimpleTest.class, methodName, "Test 7 - unwritable account key file");
				acmeCA.setAccountKeyFile(unwritableDir + "/unwritable.key");
				AcmeFatUtils.configureAcmeCA(server, configuration);
				assertNotNull("Expected CWPKI2023E in logs.", server.waitForStringInLog("CWPKI2023E"));

				/***********************************************************************
				 * 
				 * Set the account key file to default. The domain key file is
				 * unreadable.
				 * 
				 **********************************************************************/
				Log.info(AcmeSimpleTest.class, methodName, "Test 8 - unreadable domain key file");
				acmeCA.setAccountKeyFile(null);
				AcmeFatUtils.configureAcmeCA(server, configuration);
				assertNotNull("Expected CWPKI2020E in logs.", server.waitForStringInLog("CWPKI2020E"));

				/***********************************************************************
				 * 
				 * Set the domain key file to be unwritable. The domain key file is
				 * unwritable.
				 * 
				 **********************************************************************/
				Log.info(AcmeSimpleTest.class, methodName, "Test 9 - unwritable domain key file");
				acmeCA.setDomainKeyFile(unwritableDir + "/unwritable.key");
				AcmeFatUtils.configureAcmeCA(server, configuration);
				assertNotNull("Expected CWPKI2022E in logs.", server.waitForStringInLog("CWPKI2022E"));
			}

			/***********************************************************************
			 * 
			 * Set the account key file to the default location. We will get an
			 * error due to the invalid truststore type.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Test 10 - unwritable domain key file");
			acmeCA.setDomainKeyFile(null);
			AcmeFatUtils.configureAcmeCA(server, configuration);
			assertNotNull("Expected CWPKI2016E in logs.", server.waitForStringInLog("CWPKI2016E.*INVALID_TYPE"));

			/***********************************************************************
			 * 
			 * Set keystore type to a valid type. We will get an error due to
			 * the invalid truststore.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Test 11 - invalid truststore");
			acmeTransportConfig.setTrustStoreType("PKCS12");
			AcmeFatUtils.configureAcmeCA(server, configuration);
			assertNotNull("Expected CWPKI2016E in logs.", server.waitForStringInLog("CWPKI2016E.*INVALID_TRUSTSTORE"));

			/***********************************************************************
			 * 
			 * Set truststore to a valid truststore. We will get an error due to
			 * the invalid truststore password.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Test 12 - invalid truststore password");
			acmeTransportConfig.setTrustStore("resources/security/pebble-truststore.p12");
			AcmeFatUtils.configureAcmeCA(server, configuration);
			assertNotNull("Expected CWPKI2016E in logs.", server.waitForStringInLog("CWPKI2016E"));

			/***********************************************************************
			 * 
			 * Set truststore password. We will get some sort of connection
			 * error.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Test 13 - invalid directoryURI");
			acmeTransportConfig.setTrustStorePassword(AcmeFatUtils.PEBBLE_TRUSTSTORE_PASSWORD);
			AcmeFatUtils.configureAcmeCA(server, configuration);
			assertNotNull("Expected CWPKI2016E in logs.",
					server.waitForStringInLog("CWPKI2016E.*https://invalid.com/directory"));

			/***********************************************************************
			 * 
			 * Set the domain key file to default. The certificate should now be
			 * configured.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Test 14 - successful certificate generation");
			acmeCA.setDirectoryURI(FATSuite.pebble.getAcmeDirectoryURI(false));
			AcmeFatUtils.configureAcmeCA(server, configuration);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);

		} finally {
			server.stopServer("CWWKG0095E", "CWWKE0701E", "CWPKI2016E", "CWPKI2020E", "CWPKI2021E", "CWPKI2022E",
					"CWPKI2023E", "CWPKI2008E", "CWPKI2037E", "CWPKI2038W", "CWPKI2039E", "CWPKI2040E", "CWPKI2041E",
					"CWPKI2042E");
		}
	}

	/**
	 * Test how changes to the subjectDN attribute result in regenerating the
	 * certificate.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.PEBBLE_TRUSTSTORE_PASSWORD)
	public void subjectDN() throws Exception {
		final String methodName = "subjectDN()";

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeCA acmeCA = configuration.getAcmeCA();
		acmeCA.setDomain(Arrays.asList(DOMAINS2));
		acmeCA.setDirectoryURI(FATSuite.pebble.getAcmeDirectoryURI(false));
		acmeCA.setSubjectDN("cn=domain2.com,ou=liberty,o=ibm.com");
		AcmeFatUtils.configureAcmeCaConnection(useAcmeURIs(), acmeCA);
		AcmeFatUtils.configureAcmeCA(server, configuration);

		try {
			/***********************************************************************
			 * 
			 * Start the server. The certificate should have a subject DN with
			 * cn=domain2.com.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, methodName, "Starting server.");
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			Certificate[] certificates = AcmeFatUtils.assertAndGetServerCertificate(server);
			assertEquals("Certificate subject DN was not the expected value.", "CN=domain2.com",
					((X509Certificate) certificates[0]).getSubjectDN().getName());

			/***********************************************************************
			 * 
			 * Reconfigure the subjectDN. The certificate should have a subject
			 * DN with cn=domain3.com.
			 * 
			 **********************************************************************/
			BigInteger serial1 = ((X509Certificate) certificates[0]).getSerialNumber();
			acmeCA.setSubjectDN("cn=domain3.com,ou=liberty,o=ibm.com");
			AcmeFatUtils.configureAcmeCA(server, configuration);
			AcmeFatUtils.waitForAcmeToReplaceCertificate(server);

			certificates = AcmeFatUtils.assertAndGetServerCertificate(server);
			BigInteger serial2 = ((X509Certificate) certificates[0]).getSerialNumber();
			assertEquals("Certificate subject DN was not the expected value.", "CN=domain3.com",
					((X509Certificate) certificates[0]).getSubjectDN().getName());
			assertThat("Certificates should have been different.", serial1, not(equalTo(serial2)));

			/***********************************************************************
			 * 
			 * Reconfigure the subjectDN. The certificate should have a subject
			 * DN with cn=domain1.com.
			 * 
			 **********************************************************************/
			acmeCA.setSubjectDN("cn=domain1.com,ou=liberty,o=ibm.com");
			AcmeFatUtils.configureAcmeCA(server, configuration);
			AcmeFatUtils.waitForAcmeToReplaceCertificate(server);

			certificates = AcmeFatUtils.assertAndGetServerCertificate(server);
			serial2 = ((X509Certificate) certificates[0]).getSerialNumber();
			assertEquals("Certificate subject DN was not the expected value.", "CN=domain1.com",
					((X509Certificate) certificates[0]).getSubjectDN().getName());
			assertThat("Certificates should have been different.", serial1, not(equalTo(serial2)));

			/***********************************************************************
			 * 
			 * Modify the subjectDN again. The certificate should not be
			 * replaced because Pebble only honors the cn in the subject DN
			 * (which is the same).
			 * 
			 **********************************************************************/
			serial1 = ((X509Certificate) certificates[0]).getSerialNumber();
			acmeCA.setSubjectDN("cn=domain1.com");
			AcmeFatUtils.configureAcmeCA(server, configuration);
			AcmeFatUtils.waitForAcmeToNoOp(server);

			certificates = AcmeFatUtils.assertAndGetServerCertificate(server);
			serial2 = ((X509Certificate) certificates[0]).getSerialNumber();
			assertEquals("Certificate subject DN was not the expected value.", "CN=domain1.com",
					((X509Certificate) certificates[0]).getSubjectDN().getName());
			assertThat("Certificates should have not changed.", serial1, equalTo(serial2));

			/***********************************************************************
			 * 
			 * Make the subjectDN the same as before.
			 * 
			 **********************************************************************/
			serial1 = serial2;
			acmeCA.setSubjectDN("cn=domain1.com");
			acmeCA.setChallengeRetries(5); // Force config update.
			AcmeFatUtils.configureAcmeCA(server, configuration);
			AcmeFatUtils.waitForAcmeToNoOp(server);

			certificates = AcmeFatUtils.assertAndGetServerCertificate(server);
			serial2 = ((X509Certificate) certificates[0]).getSerialNumber();
			assertEquals("CN=domain1.com", ((X509Certificate) certificates[0]).getSubjectDN().getName());
			assertThat("Certificates should have not changed.", serial1, equalTo(serial2));

		} finally {
			server.stopServer();
		}
	}
}