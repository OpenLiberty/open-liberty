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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.docker.pebble.PebbleContainer;
import com.ibm.ws.security.acme.internal.AcmeHistory;
import com.ibm.ws.security.acme.internal.AcmeHistoryEntry;
import com.ibm.ws.security.acme.internal.util.AcmeConstants;
import com.ibm.ws.security.acme.utils.AcmeFatUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.CheckForLeakedPasswords;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
/**
 * Test for AcmeHistoricalFile. Ensure the file is created, updated, and
 * causes refreshing of certificates.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES) // No value added
public class AcmeSwapDirectoriesTest {

	@Server("com.ibm.ws.security.acme.fat.simple")
	public static LibertyServer server;

	protected static ServerConfiguration ORIGINAL_CONFIG;
	
	private final String acmeFile = AcmeConstants.ACME_HISTORY_FILE;

	/*
	 * Domains that are configured and cleared before and after the class.
	 */
	private static final String[] DOMAINS_1 = { "domain1.com" };
	private static final String spaceDelim = "                  ";
	
	private AcmeHistory acmeHelper = new AcmeHistory();

	public static CAContainer caContainer;
	public static CAContainer caContainer2;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() throws Exception {
		ORIGINAL_CONFIG = server.getServerConfiguration();
		caContainer = new PebbleContainer();
		caContainer2 = new PebbleContainer();
		AcmeFatUtils.checkPortOpen(caContainer.getHttpPort(), 60000);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (caContainer != null) {
			caContainer.stop();
		}
		if (caContainer2 != null) {
			caContainer2.stop();
		}
	}

	@After
	public void afterTest() throws Exception {

		/*
		 * Cleanup any generated ACME files.
		 */
		AcmeFatUtils.deleteAcmeFiles(server);
	}

	/**
	 * This test will verify that the ACME file is created and updated
	 * properly. When the directoryURI changes, the certificate should
	 * be refreshed.
	 * 
	 * @throws Exception
	 *             If the test failed for some reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void update_directoryURI() throws Exception {

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
			 * TEST 1: Check that the ACME historical file was created and populated
			 * with the first certificate and original directoryURI. 
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: START");
			File file = new File(server.getServerRoot() + "/workarea/acmeca/" + acmeFile);
			if (!file.exists()) {
				fail("The ACME file should exist at: " + file.getAbsolutePath());
			}
			String firstDirURI = null;
			ArrayList<String> dirURIs = acmeHelper.getDirectoryURIHistory(file);
			if (dirURIs != null && !dirURIs.isEmpty()) {
				firstDirURI = dirURIs.get(dirURIs.size()-1);
			}
			assertEquals(caContainer.getAcmeDirectoryURI(false), firstDirURI);
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: FINISH");
			
			/***********************************************************************
			 * 
			 * TEST 2: Update the directoryURI. This should result in a refreshed
			 * and revoked certificate.
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: START");
			AcmeFatUtils.configureAcmeCA(server, caContainer2, ORIGINAL_CONFIG, false, false, false, DOMAINS_1);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForAcmeToRevokeCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * new CA.
			 */
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer2);

			BigInteger serial1 = ((X509Certificate) certificates1[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) certificates2[0]).getSerialNumber();
			assertFalse("Expected new certificate after adding new domain.", serial1.equals(serial2));
			
			//Check that the ACME historical file was updated.
			String secondDirURI = null;
			dirURIs = acmeHelper.getDirectoryURIHistory(file);
			if (dirURIs != null && !dirURIs.isEmpty()) {
				secondDirURI = dirURIs.get(dirURIs.size()-1);
			}
			assertEquals(2, dirURIs.size());
			assertNotSame(firstDirURI, secondDirURI);
			assertEquals(caContainer2.getAcmeDirectoryURI(false), secondDirURI);
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: FINISH");

			/***********************************************************************
			 * 
			 * TEST 3: Stop the server, change the directoryURI, and start the server.
			 * We should renew the certificate. (This will also create a new ACME
			 * file because starting the server updates the workarea directory)
			 * 
			 **********************************************************************/			
			Log.info(this.getClass(), testName.getMethodName(), "TEST 3: START");
			stopServer();
			AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, false, false, false, DOMAINS_1);
			server.startServer();
			AcmeFatUtils.waitForAcmeAppToStart(server);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			AcmeFatUtils.waitForSslEndpoint(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * new CA.
			 */
			Certificate[] certificates3 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);

			BigInteger serial3 = ((X509Certificate) certificates3[0]).getSerialNumber();
			assertFalse("Expected new certificate after adding new domain.", serial2.equals(serial3));
			
			//Check that the ACME historical file was updated. 	
			String thirdDirURI = null;
			dirURIs = acmeHelper.getDirectoryURIHistory(file);
			if (dirURIs != null && !dirURIs.isEmpty()) {
				thirdDirURI = dirURIs.get(dirURIs.size()-1);
			}
			assertNotSame(secondDirURI, thirdDirURI);
			assertEquals(caContainer.getAcmeDirectoryURI(false), thirdDirURI);
			Log.info(this.getClass(), testName.getMethodName(), "TEST 3: FINISH");
		} finally {
			/*
			 * Stop the server.
			 */
			stopServer("CWPKI2038W", "CWWKG0027W");
		}
	}
	/**
	 * This test will verify that the certificate is refreshed
	 * when the ACME file is unreadable or unwritable.
	 * 
	 * 1. Start the server and wait for initial certificate.
     * 2. Set the ACME file to be unreadable. The next time we try to read it, we should force refresh the certificate.
     * 3. Update the URI and wait for the certificate to refresh.
     * 4. Delete the acme file and set the acme directory to be unreadable.
     * 5. Change the directoryURI. We won’t be able to write the acme file, but we should refresh the certificate.
	 * 
	 * @throws Exception
	 *             If the test failed for some reason.
	 */
	@Test
    @AllowedFFDC(value = { "java.io.FileNotFoundException", "java.io.IOException" })
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void update_directoryURI_filePermissions() throws Exception {
        Assume.assumeTrue(!AcmeFatUtils.isWindows(testName.getMethodName()));
		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, false, false, false, DOMAINS_1);

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
			 * TEST 1: Remove the read permission on the ACME file.
			 * Update the directoryURI. This should result in a refreshed
			 * certificate. 
			 * 
			 **********************************************************************/
			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: START");
			File acmefile = new File(server.getServerRoot() + "/workarea/acmeca/" + acmeFile);
			acmefile.setReadable(false,false);

			AcmeFatUtils.configureAcmeCA(server, caContainer2, ORIGINAL_CONFIG, false, false, false, DOMAINS_1);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);

			/*
			 * Verify that the server is now using a certificate signed by the
			 * new CA.
			 */
			Certificate[] certificates2 = AcmeFatUtils.assertAndGetServerCertificate(server, caContainer2);

			BigInteger serial1 = ((X509Certificate) certificates1[0]).getSerialNumber();
			BigInteger serial2 = ((X509Certificate) certificates2[0]).getSerialNumber();
			assertFalse("Expected new certificate after being unable to read ACME file.", serial1.equals(serial2));

			Log.info(this.getClass(), testName.getMethodName(), "TEST 1: FINISH.");
			
			/***********************************************************************
			 * 
			 * TEST 2: Remove the write permissions on the ACME file.
			 * Update the directoryURI. This should result in a refreshed
			 * certificate.
			 * 
			 **********************************************************************/

			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: START");
			if (acmefile.exists()) {
				Log.info(this.getClass(), testName.getMethodName(), "deleting acme file " + server.getServerRoot() + "/workarea/acmeca/" + acmeFile);
				acmefile.delete();
			}

			File acmeDir = Files.createDirectories(Paths.get(server.getServerRoot() + "/workarea/acmeca")).toFile();
			if (acmeDir.exists()) {
				Log.info(this.getClass(), testName.getMethodName(), "acme dir " + acmeDir.getAbsolutePath() + " " + Files.isWritable(Paths.get(acmeDir.getAbsolutePath())));
				acmeDir.setWritable(false);
			}

			AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, false, false, false, DOMAINS_1);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);

			if (acmefile.exists()) {
				fail("The ACME file should not exist.");
			}
			Log.info(this.getClass(), testName.getMethodName(), "TEST 2: FINISH.");
				
			
		} finally {
			/*
			 * Stop the server.
			 */
			stopServer("CWPKI2038W", "CWPKI2072W", "CWWKG0027W");
		}
	}
	/**
	 * This test will verify that the ACME file does not exceed
	 * 10 entries. When the 11th entry is added, the oldest one
	 * should be removed.
	 * 
	 * @throws Exception
	 *             If the test failed for some reason.
	 */
	@Test
	@CheckForLeakedPasswords(AcmeFatUtils.CACERTS_TRUSTSTORE_PASSWORD)
	public void update_directoryURI_maxFileSize() throws Exception {

		/*
		 * Configure the acmeCA-2.0 feature.
		 */
		AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, false, false, false, DOMAINS_1);
		
		//Make sure the ACME directory is writable - in case the previous test fails out.
		File acmeDir = Files.createDirectories(Paths.get(server.getServerRoot() + "/workarea/acme")).toFile();
		if (acmeDir.exists()) {
			acmeDir.setWritable(true);
		}
		
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
			AcmeFatUtils.assertAndGetServerCertificate(server, caContainer);
			/***********************************************************************
			 * 
			 * TEST: Populate the ACME file to max size and update the directoryURI.
			 * We should refresh the certificate and the number of lines in the ACME
			 * file should stay the same.
			 * 
			 **********************************************************************/

			File file = new File(server.getServerRoot() + "/workarea/acmeca/" + acmeFile);
			//Write 10 entries to the acme file
			for (int i=0; i<5; i++) {
				AcmeFatUtils.configureAcmeCA(server, caContainer2, ORIGINAL_CONFIG, false, false, false, DOMAINS_1);
				AcmeFatUtils.waitForAcmeToCreateCertificate(server);
				AcmeFatUtils.configureAcmeCA(server, caContainer, ORIGINAL_CONFIG, false, false, false, DOMAINS_1);
				AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			}
			
			//Make sure we start with 10 entries.
			ArrayList<String> dirURIs = acmeHelper.getDirectoryURIHistory(file);
			assertEquals(10, dirURIs.size());

			//Update the config. This normally wouldn't cause a certificate refresh, but
			//there has been a change in directoryURI.
			AcmeFatUtils.configureAcmeCA(server, caContainer2, ORIGINAL_CONFIG, false, false, false, DOMAINS_1);
			AcmeFatUtils.waitForAcmeToCreateCertificate(server);
			
			//Check the ACME file for the new entry.
			//After refreshing the certificate and updating the ACME file,
			//we should still have 10 entries.
			dirURIs = acmeHelper.getDirectoryURIHistory(file);
			assertEquals(10, dirURIs.size());
			assertEquals(caContainer.getAcmeDirectoryURI(false), dirURIs.get(8));
			assertEquals(caContainer2.getAcmeDirectoryURI(false), dirURIs.get(9));
		} finally {
			/*
			 * Stop the server.
			 */
			stopServer("CWPKI2038W", "CWWKG0027W");
		}
	}
	
	private void stopServer(String...msgs) throws Exception {
		AcmeFatUtils.stopServer(server, msgs);
	}
}
