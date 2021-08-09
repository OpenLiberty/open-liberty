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

import static com.ibm.websphere.simplicity.ShrinkHelper.addDirectory;
import static com.ibm.websphere.simplicity.ShrinkHelper.buildDefaultApp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.AcmeCA;
import com.ibm.websphere.simplicity.config.AcmeCA.AcmeTransportConfig;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.docker.pebble.PebbleContainer;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.AllowedFFDC;
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
 * Configuration failure tests and toggling the feature on and off.
 * 
 * These were moved from AcmeSimpleTest to keep the Lite bucket short.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) // No value added
public class AcmeConfigVariationsTest {

	@Server("com.ibm.ws.security.acme.fat.config_var")
	public static LibertyServer server;

	protected static ServerConfiguration ORIGINAL_CONFIG;

	/*
	 * Domains that are configured and cleared before and after the class.
	 */
	private static final String[] DOMAINS_ALL = { "domain1.com", "domain2.com", "domain3.com" };
	private static final String[] DOMAINS_1 = { "domain1.com" };
	private static final String[] DOMAINS_2 = { "domain1.com", "domain2.com", "domain3.com" };

	protected static final String SLOW_APP = "slowapp.war";
	protected final static String PUBLISH_FILES = "publish/files";
	protected static final String APPS_DIR = "apps";

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
		configuration.getAcmeCA().setCertCheckerSchedule(configuration.getAcmeCA().getRenewCertMin() + "ms");
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, useAcmeURIs(), DOMAINS_1);

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
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, useAcmeURIs(), DOMAINS_1);
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
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, useAcmeURIs(), DOMAINS_1);
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
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, useAcmeURIs(), DOMAINS_1);
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
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	@AllowedFFDC(value = { "java.io.IOException", "java.security.KeyStoreException",
			"com.ibm.websphere.ssl.SSLException", "org.shredzone.acme4j.exception.AcmeNetworkException",
			"java.io.FileNotFoundException", "sun.security.validator.ValidatorException" })
	public void updateconfig_bad_to_good() throws Exception {

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

		ArrayList<String> accounts = new ArrayList<String>();
		accounts.add("mailto:pacman@mail.com");
		acmeCA.setAccountContact(accounts);
		/*
		 * Check that we reset the minimum levels
		 */
		acmeCA.setCertCheckerSchedule("2ms");
		acmeCA.setCertCheckerErrorSchedule("2ms");
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);

		try {
			/***********************************************************************
			 * 
			 * Start the server. The 'directoryURI' and 'domain' attributes are
			 * missing.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Starting server.");
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
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 1 - empty directoryURI");
			acmeCA.setDirectoryURI("");
			acmeCA.setDomain(Arrays.asList(new String[] { "" }));
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			assertNotNull("Expected CWPKI2008E in logs.", server.waitForStringInLog("CWPKI2008E"));

			/***********************************************************************
			 * 
			 * Add non-empty 'domain' and 'directoryURI' attributes. The
			 * subjectDN contains a bad domain in the 'cn'.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 2 - subjectDN has invalid domain in cn");
			acmeCA.setDomain(Arrays.asList(DOMAINS_2));
			acmeCA.setDirectoryURI("https://invalid.com/directory");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			assertNotNull("Expected CWPKI2039E in logs.", server.waitForStringInLog("CWPKI2039E"));

			/***********************************************************************
			 * 
			 * The subjectDN's cn RDN is not the first RDN.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 3 - subjectDN cn is not first RDN");
			acmeCA.setSubjectDN("ou=liberty,cn=domain1.com");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			assertNotNull("Expected CWPKI2040E in logs.", server.waitForStringInLog("CWPKI2040E"));

			/***********************************************************************
			 * 
			 * The subjectDN contains a bad RDN type.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 4 - subjectDN has invalid RDN type");
			acmeCA.setSubjectDN("badtype=domain1.com");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			assertNotNull("Expected CWPKI2041E in logs.", server.waitForStringInLog("CWPKI2041E"));

			/***********************************************************************
			 * 
			 * The subjectDN is not a valid DN.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 5 - subjectDN has invalid DN");
			acmeCA.setSubjectDN("invaliddn");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			assertNotNull("Expected CWPKI2042E in logs.", server.waitForStringInLog("CWPKI2042E"));

			if (AcmeFatUtils.isWindows(testName.getMethodName())) {
				acmeCA.setSubjectDN("cn=domain1.com");
				acmeCA.setAccountKeyFile(null);
			} else {
				/***********************************************************************
				 * 
				 * Set valid 'subjectDN' attribute. The account key file is
				 * unreadable.
				 * 
				 **********************************************************************/
				Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 6 - unreadable account key file");
				acmeCA.setSubjectDN("cn=domain1.com");
				AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
				assertNotNull("Expected CWPKI2021E in logs.", server.waitForStringInLog("CWPKI2021E"));

				/***********************************************************************
				 * 
				 * Set the account key file to be unwritable. The account key
				 * file is unwritable.
				 * 
				 **********************************************************************/
				Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 7 - unwritable account key file");
				acmeCA.setAccountKeyFile(unwritableDir + "/unwritable.key");
				AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
				assertNotNull("Expected CWPKI2023E in logs.", server.waitForStringInLog("CWPKI2023E"));

				/***********************************************************************
				 * 
				 * Set the account key file to default. The domain key file is
				 * unreadable.
				 * 
				 **********************************************************************/
				Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 8 - unreadable domain key file");
				acmeCA.setAccountKeyFile(null);
				AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
				assertNotNull("Expected CWPKI2020E in logs.", server.waitForStringInLog("CWPKI2020E"));

				/***********************************************************************
				 * 
				 * Set the domain key file to be unwritable. The domain key file
				 * is unwritable.
				 * 
				 **********************************************************************/
				Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 9 - unwritable domain key file");
				acmeCA.setDomainKeyFile(unwritableDir + "/unwritable.key");
				AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
				assertNotNull("Expected CWPKI2022E in logs.", server.waitForStringInLog("CWPKI2022E"));
			}

			/***********************************************************************
			 * 
			 * Set the account key file to the default location. We will get an
			 * error due to the invalid truststore type.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 10 - unwritable domain key file");
			acmeCA.setDomainKeyFile(null);
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			assertNotNull("Expected CWPKI2016E in logs.", server.waitForStringInLog("CWPKI2016E.*INVALID_TYPE"));

			/***********************************************************************
			 * 
			 * Set keystore type to a valid type. We will get an error due to
			 * the invalid truststore.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 11 - invalid truststore");
			acmeTransportConfig.setTrustStoreType("PKCS12");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			assertNotNull("Expected CWPKI2016E in logs.", server.waitForStringInLog("CWPKI2016E.*INVALID_TRUSTSTORE"));

			/***********************************************************************
			 * 
			 * Set truststore to a valid truststore. We will get an error due to
			 * the invalid truststore password.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 12 - invalid truststore password");
			acmeTransportConfig.setTrustStore("resources/security/cacerts.p12");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			assertNotNull("Expected CWPKI2016E in logs.", server.waitForStringInLog("CWPKI2016E"));

			/***********************************************************************
			 * 
			 * Set truststore password. We will get some sort of connection
			 * error.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 13 - invalid directoryURI");
			acmeTransportConfig.setTrustStorePassword(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD);
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			assertNotNull("Expected CWPKI2016E in logs.",
					server.waitForStringInLog("CWPKI2016E.*https://invalid.com/directory"));

			
			/***********************************************************************
			 * 
			 * Set the domain key file to default. The certificate should now be configured.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(),
					"Test 14 - successful certificate generation");
			acmeCA.setDirectoryURI(caContainer.getAcmeDirectoryURI(false));
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslToCreateKeystore(server);

			assertNotNull("Should have found the cert checker waking up after updates: " + AcmeFatUtils.ACME_CHECKER_TRACE,
					server.waitForStringInTrace(AcmeFatUtils.ACME_CHECKER_TRACE));
			
			assertNotNull("Should have found warning that the certCheckerScheduler time was reset", server.findStringsInLogs("CWPKI2070W"));
			assertNotNull("Should have found warning that the certCheckerErrorScheduler time was reset", server.findStringsInLogs("CWPKI2071W"));

		} finally {
			stopServer("CWWKG0095E", "CWWKE0701E", "CWPKI2016E", "CWPKI2020E", "CWPKI2021E", "CWPKI2022E",
					"CWPKI2023E", "CWPKI2008E", "CWPKI2037E", "CWPKI2039E", "CWPKI2040E", "CWPKI2041E", "CWPKI2042E",
					"CWPKI0823E", "CWPKI0828E", "CWPKI2070W", "CWPKI2071W", "CWPKI0804E");
			/*
			 * Running on Sun produces some additional errors on the invalid directory URI,
			 * added them to the stop list
			 */
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
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, useAcmeURIs(), DOMAINS_2);
		caContainer.addDnsARecord("domain2.com", "127.0.0.1");

		try {
			/***********************************************************************
			 * 
			 * Start the server. The domain2.com domain will fail to validate.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Starting server.");
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
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, useAcmeURIs(), DOMAINS_1);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
		} finally {
			stopServer("CWPKI2001E", "CWPKI0804E", "CWWKO0801E");
		}
	}
	
	protected void stopServer(String ...msgs) throws Exception {
		AcmeFatUtils.stopServer(server, msgs);
	}

	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void slowAppStartup() throws Exception {

		/*
		 * Build our slow starting app
		 */
		WebArchive slow = buildDefaultApp("slowapp.war", "test.app.*");
		addDirectory(slow, "test-applications/slowapp.war/resources");
		ShrinkHelper.exportArtifact(slow, PUBLISH_FILES, true, true);

		/*
		 * Configure the acmeCA-2.0 feature and add intentionally "slow" app to make
		 * sure acmeCA still starts and fetches a certificate. The server should stop
		 * waiting after 30 seconds and open the HTTP port
		 */
		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		configuration.getFeatureManager().getFeatures().add("servlet-4.0");
		Application slowApp = new Application();
		slowApp.setId("slow");
		slowApp.setLocation("slowapp.war");
		configuration.getApplications().add(slowApp);
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, false, DOMAINS_1);

		server.copyFileToLibertyServerRoot(PUBLISH_FILES, APPS_DIR, SLOW_APP);

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
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			File file = new File(server.getServerRoot() + "/workarea/acmeca/" + AcmeConstants.ACME_HISTORY_FILE);
			if (!file.exists()) {
				fail("The ACME file should exist at: " + file.getAbsolutePath());
			}

			/*
			 * Double check that the slow app installed
			 */
			assertFalse("Slow app not installed", server.findStringsInLogs("SlowApp is sleeping").isEmpty());
		} finally {
			stopServer();
		}
	}

	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	@AllowedFFDC(value = { "java.io.IOException", "org.shredzone.acme4j.exception.AcmeNetworkException" })
	public void httpReadConnectTimeouts() throws Exception {

		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, false, true, DOMAINS_1);

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
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			/***********************************************************************
			 * 
			 * Set a super low http connect and read timeout
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 2 - http connect timeout");
			configuration = server.getServerConfiguration().clone();
			AcmeTransportConfig acmeTransportConfig = new AcmeTransportConfig();
			acmeTransportConfig.setHttpConnectTimeout("1ms");
			configuration.getAcmeCA().setAcmeTransportConfig(acmeTransportConfig);
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			AcmeFatUtils.renewCertificate(server);
			assertNotNull("Expected CWPKI2016E in logs.", server.waitForStringInLog("CWPKI2016E"));

			/***********************************************************************
			 * 
			 * Set the http connect time to better length. The request will now timeout on
			 * http read.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(), "Test 3 - http read timeout");
			server.setMarkToEndOfLog(server.getDefaultLogFile());
			acmeTransportConfig.setHttpConnectTimeout("45s");
			acmeTransportConfig.setHttpReadTimeout("1ms");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			AcmeFatUtils.renewCertificate(server);
			assertNotNull("Expected CWPKI2016E in logs.", server.waitForStringInLog("CWPKI2016E"));

			/***********************************************************************
			 * 
			 * Set the http read time to better length. The certificate should now be
			 * configured.
			 * 
			 **********************************************************************/
			Log.info(AcmeConfigVariationsTest.class, testName.getMethodName(),
					"Test 4 - successful certificate generation");
			acmeTransportConfig.setHttpReadTimeout("45s");
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);

		} finally {
			stopServer("CWPKI2016E");
		}
	}
}
