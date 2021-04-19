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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.AcmeCA;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.docker.pebble.PebbleContainer;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Some simple tests for running the acmeCA-2.0 feature.
 * 
 * Unless you are adding something for the golden/main path, consider adding
 * the test to AcmeConfigVariationsTest instead.
 */
@RunWith(FATRunner.class)
public class AcmeSimpleTest {

	@Server("com.ibm.ws.security.acme.fat.simple")
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
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void startup_server() throws Exception {
		assumeTrue(!AcmeFatUtils.isWindowsWithOpenJDK(testName.getMethodName()));
		Certificate[] startingCertificateChain = null, endingCertificateChain = null;

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, useAcmeURIs(), DOMAINS_3);

		/***********************************************************************
		 * 
		 * TEST 1: The server will start up without the specified keystore
		 * available. It should generate a new keystore with a certificate
		 * retrieved from the ACME CA server.
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
			 * Verify that the server is now using a certificate signed by the
			 * CA.
			 */
			endingCertificateChain = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
			
			File file = new File(server.getServerRoot() + "/workarea/acmeca/" + AcmeConstants.ACME_HISTORY_FILE);
			if (!file.exists()) {
				fail("The ACME file should exist at: " + file.getAbsolutePath());
			}
		} finally {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: Shutdown.");

			/*
			 * Stop the server.
			 */
			stopServer();
		}

		/***********************************************************************
		 * 
		 * TEST 2: The server will start up with the ACME certificate generated
		 * in the first test. The same certificate will be used as it is still
		 * valid.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: Start");

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
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: Shutdown.");

			/*
			 * Stop the server.
			 */
			stopServer("CWPKI2058W");
		}

		/***********************************************************************
		 * 
		 * TEST 3: The server will start up with a self-signed certificate. It
		 * should recognize it is not a valid certificate and replace it.
		 * 
		 **********************************************************************/
		try {
			Log.info(this.getClass(), testName.getMethodName(), "TEST 3: Start");

			/*
			 * Create a self-signed certificate.
			 */
			startingCertificateChain = AcmeFatUtils.generateSelfSignedCertificate(server);

			/*
			 * Start the server and wait for acme to replace the certificate.
			 */
			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
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
			Log.info(this.getClass(), testName.getMethodName(), "TEST 3: Shutdown");

			/*
			 * Stop the server.
			 */
			stopServer();
		}
	}

	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void update_domains() throws Exception {
		assumeTrue(!AcmeFatUtils.isWindowsWithOpenJDK(testName.getMethodName()));
		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, useAcmeURIs(), DOMAINS_1);

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
			AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, useAcmeURIs(), DOMAINS_2);
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
			AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, useAcmeURIs(), DOMAINS_3);
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
			AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, useAcmeURIs(), DOMAINS_4);
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
	 * Test how changes to the subjectDN attribute result in regenerating the
	 * certificate.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void update_subjectdn() throws Exception {
		assumeTrue(!AcmeFatUtils.isWindowsWithOpenJDK(testName.getMethodName()));
		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeCA acmeCA = configuration.getAcmeCA();
		acmeCA.setDomain(Arrays.asList(DOMAINS_2));
		acmeCA.setDirectoryURI(caContainer.getAcmeDirectoryURI(false));
		acmeCA.setSubjectDN("cn=domain2.com,ou=liberty,o=ibm.com");
		ArrayList<String> accounts = new ArrayList<String>();
		accounts.add("mailto:pacman@mail.com");
		acmeCA.setAccountContact(accounts);
		AcmeFatUtils.configureAcmeCaConnection(caContainer.getAcmeDirectoryURI(useAcmeURIs()), acmeCA);
		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);

		try {
			/***********************************************************************
			 * 
			 * Start the server. The certificate should have a subject DN with
			 * cn=domain2.com.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, testName.getMethodName(), "Starting server.");
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslToCreateKeystore(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			Certificate[] certificates = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
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
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);

			certificates = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
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
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);

			certificates = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
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
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			AcmeFatUtils.waitForAcmeToNoOp(server);

			certificates = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
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
			acmeCA.setChallengePoll("1m"); // Force config update.
			AcmeFatUtils.configureAcmeCA(server, caContainer, configuration);
			AcmeFatUtils.waitForAcmeToNoOp(server);

			certificates = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
			serial2 = ((X509Certificate) certificates[0]).getSerialNumber();
			assertEquals("CN=domain1.com", ((X509Certificate) certificates[0]).getSubjectDN().getName());
			assertThat("Certificates should have not changed.", serial1, equalTo(serial2));

		} finally {
			stopServer("CWPKI2058W");
		}
	}

	/**
	 * This test will verify that when the keystore has been provided, and it
	 * does not contain the default alias, that the ACME CA feature will
	 * properly generate it.
	 * 
	 * <p/>
	 * In practice, this might be common where a customer has a single keystore
	 * that serves as both the truststore and keystore. They have inserted their
	 * trusted entries into the keystore and we need to support adding the
	 * default certificate into that keystore.
	 * 
	 * @throws Exception
	 *             if the test fails for some unforeseen reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	@MinimumJavaLevel(javaLevel = 9)
	/* Minimum Java Level to avoid a known/fixed IBM Java 8 bug with an empty keystore, IJ19292. When the builds move to 8SR6, we can run this test again */
	public void keystore_exists_without_default_alias() throws Exception {
		assumeTrue(!AcmeFatUtils.isWindowsWithOpenJDK(testName.getMethodName()));
		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();

		/*
		 * Create a keystore that is empty.
		 */
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(null, AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD.toCharArray());
		ks.store(new FileOutputStream(new File(server.getServerRoot() + "/resources/security/key.p12")),
				AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD.toCharArray());

		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, useAcmeURIs(), DOMAINS_1);

		try {
			/***********************************************************************
			 * 
			 * Start the server.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, testName.getMethodName(), "Starting server.");
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

		} finally {
			stopServer();
		}
	}

	/**
	 * Make sure if the parent directory for the account key pair does not
	 * exist, that we can create it.
	 * 
	 * @throws Exception
	 *             if there was an unforeseen error
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void account_keypair_directory_does_not_exist() throws Exception {
		assumeTrue(!AcmeFatUtils.isWindowsWithOpenJDK(testName.getMethodName()));
		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();

		/*
		 * Configure ACME to use an non-existent directory for the account key
		 * pair file.
		 */
		AcmeCA acmeCA = configuration.getAcmeCA();
		String filePath = server.getServerRoot() + "/resources/directory/does/not/exist/acmeAccountKey.p12";
		acmeCA.setAccountKeyFile(filePath);

		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, useAcmeURIs(), DOMAINS_1);

		try {
			/***********************************************************************
			 * 
			 * Start the server.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, testName.getMethodName(), "Starting server.");
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

		} finally {
			stopServer();

			/*
			 * Delete the file.
			 */
			File f = new File(filePath);
			if (f.exists()) {
				f.delete();
			}
		}
	}

	/**
	 * Make sure if the parent directory for the account key pair does not
	 * exist, that we can create it.
	 * 
	 * @throws Exception
	 *             if there was an unforeseen error
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void domain_keypair_directory_does_not_exist() throws Exception {
		assumeTrue(!AcmeFatUtils.isWindowsWithOpenJDK(testName.getMethodName()));
		ServerConfiguration configuration = ORIGINAL_CONFIG.clone();

		/*
		 * Configure ACME to use an non-existent directory for the domain key
		 * pair file.
		 */
		AcmeCA acmeCA = configuration.getAcmeCA();
		String filePath = server.getServerRoot() + "/resources/directory/does/not/exist/acmeDomainKey.p12";
		acmeCA.setDomainKeyFile(filePath);

		AcmeFatUtils.configureAcmeCA(server, caContainer, configuration, useAcmeURIs(), DOMAINS_1);

		try {
			/***********************************************************************
			 * 
			 * Start the server.
			 * 
			 **********************************************************************/
			Log.info(AcmeSimpleTest.class, testName.getMethodName(), "Starting server.");
			server.startServer();
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

		} finally {
			stopServer();

			/*
			 * Delete the file.
			 */
			File f = new File(filePath);
			if (f.exists()) {
				f.delete();
			}
		}
	}
	
	protected void stopServer(String ...msgs) throws Exception {
		AcmeFatUtils.stopServer(server, msgs);
	}
}
