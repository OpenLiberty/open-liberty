/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.featureUtility.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class InstallFeatureTest extends FeatureUtilityToolTest {

	private static final Class<?> c = InstallFeatureTest.class;

	@BeforeClass
	public static void beforeClassSetup() throws Exception {
		final String methodName = "beforeClassSetup";
        /* Enable tests only if running on a zOS machine, otherwise skip class */
        Assume.assumeTrue(!isZos);
		Log.entering(c, methodName);
		setupEnv();
		Log.exiting(c, methodName);
	}

	@Before
	public void beforeCleanUp() throws Exception {
		// rollback wlp version 2 times (e.g 20.0.0.5 -> 20.0.0.3)
		resetOriginalWlpProps();
		replaceWlpProperties(getPreviousWlpVersion());
		replaceWlpProperties(getPreviousWlpVersion());
	}

	@After
	public void afterCleanUp() throws Exception {
		resetOriginalWlpProps();
	}

	@AfterClass
	public static void cleanUp() throws Exception {
		// TODO
		resetOriginalWlpProps();
		cleanUpTempFiles();
		deleteRepo("AfterClassCleanUp");
	}

	protected static void deleteFiles(String methodName, String featureName, String[] filePathsToClear)
			throws Exception {

		Log.info(c, methodName, "If Exists, Deleting files for " + featureName);

		for (String filePath : filePathsToClear) {
			if (server.fileExistsInLibertyInstallRoot(filePath)) {
				server.deleteFileFromLibertyInstallRoot(filePath);
			}
		}

		server.deleteDirectoryFromLibertyInstallRoot("lafiles");

		Log.info(c, methodName, "Finished deleting files associated with " + featureName);
	}

	/**
	 * Test the install of jsp-2.2, jsp-2.3 from maven central. Multi-version is not
	 * supported with installServerFeature as it cannot be installed to same
	 * resource.
	 *
	 * @throws Exception
	 */
	@Test
	public void testMultiVersionFeatures() throws Exception {
		final String METHOD_NAME = "testMultiVersionFeatures";
		Log.entering(c, METHOD_NAME);
		// Setup
		replaceWlpProperties("20.0.0.4");

		String[] jsp22FilesList = { relativeMinifiedRoot + "/wlp/lib/features/com.ibm.websphere.appserver.jsp-2.2.mf" };
		String[] jsp23FilesList = { relativeMinifiedRoot + "/wlp/lib/features/com.ibm.websphere.appserver.jsp-2.3.mf" };

		deleteFiles(METHOD_NAME, "jsp-2.2", jsp22FilesList);
		deleteFiles(METHOD_NAME, "jsp-2.3", jsp23FilesList);
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/20.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/20.0.0.4/features-20.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/features/20.0.0.4",
				"../../publish/repo/io/openliberty/features/features/20.0.0.4/features-20.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/jsp-2.3/20.0.0.4",
				"../../publish/repo/io/openliberty/features/jsp-2.3/20.0.0.4/jsp-2.3-20.0.0.4.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/jsp-2.2/20.0.0.4",
				"../../publish/repo/io/openliberty/features/jsp-2.2/20.0.0.4/jsp-2.2-20.0.0.4.esa");

		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");

		// Begin Test
		String[] param1s = { "installFeature", "jsp-2.2", "jsp-2.3", "--verbose" };
		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Should contain jsp-2.2", output.contains("jsp-2.2"));
		assertTrue("Should contain jsp-2.3", output.contains("jsp-2.3"));
	}

	/**
	 * Test installation of feature json-1.0 from local repository
	 *
	 * @throws Exception
	 */
	@Test
	public void testInstallFeature() throws Exception {
		final String METHOD_NAME = "testInstallFeature";
		Log.entering(c, METHOD_NAME);
		// Setup
		replaceWlpProperties("21.0.0.4");
		String[] json10FilesList = {
				relativeMinifiedRoot + "/wlp/lib/features/com.ibm.websphere.appserver.json-1.0.mf" };
		deleteFiles(METHOD_NAME, "json-1.0", json10FilesList);

		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/21.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/21.0.0.4/features-21.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/features/21.0.0.4",
				"../../publish/repo/io/openliberty/features/features/21.0.0.4/features-21.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/json-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/json-1.0/21.0.0.4/json-1.0-21.0.0.4.esa");

		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");

		// Begin Test
		String[] param1s = { "installFeature", "json-1.0", "--verbose" };
//		String[] fileLists = { "lib/features/com.ibm.websphere.appserver.json-1.0.mf" };
		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Should contain json-1.0", output.contains("json-1.0"));

		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Test the installation of features eventLogging-1.0 and osgiConsole-1.0, which
	 * should also install the autofeature eventLogging-1.0-osgiCOnsole-1.0
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInstallAutoFeature() throws Exception {
		final String METHOD_NAME = "testInstallAutoFeature";
		Log.entering(c, METHOD_NAME);
		// Setup
		replaceWlpProperties("21.0.0.4");
		String[] autoFeaturesFilesList = {
				relativeMinifiedRoot + "/wlp/lib/features/com.ibm.websphere.appserver.eventLogging-1.0.mf",
				relativeMinifiedRoot + "/wlp/lib/features/com.ibm.websphere.appserver.osgiConsole-1.0.mf"
		};
		deleteFiles(METHOD_NAME, "autoFeatures eventLogging-1.0,osgiConsole-1.0", autoFeaturesFilesList);

		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/21.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/21.0.0.4/features-21.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/features/21.0.0.4",
				"../../publish/repo/io/openliberty/features/features/21.0.0.4/features-21.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/osgiConsole-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/osgiConsole-1.0/21.0.0.4/com.ibm.websphere.appserver.osgiConsole-1.0.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/osgiConsole-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/osgiConsole-1.0/21.0.0.4/osgiConsole-1.0-21.0.0.4.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/eventLogging-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/eventLogging-1.0/21.0.0.4/com.ibm.websphere.appserver.eventLogging-1.0.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/eventLogging-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/eventLogging-1.0/21.0.0.4/com.ibm.websphere.appserver.requestProbeJDBC-1.0.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/eventLogging-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/eventLogging-1.0/21.0.0.4/com.ibm.websphere.appserver.requestProbes-1.0.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/eventLogging-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/eventLogging-1.0/21.0.0.4/com.ibm.websphere.appserver.requestProbeServlet-1.0.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/eventLogging-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/eventLogging-1.0/21.0.0.4/eventLogging-1.0-21.0.0.4.esa");

		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");

		// Begin Test
		String[] param1s = { "installFeature", "eventLogging-1.0", "osgiConsole-1.0", "--verbose" };
//		String[] fileListA = { "lib/features/com.ibm.websphere.appserver.eventLogging-1.0.mf" };
//		String[] fileListB = { "lib/features/com.ibm.websphere.appserver.osgiConsole-1.0.mf" };

		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Output should contain eventLogging-1.0", output.indexOf("eventLogging-1.0") >= 0);
		assertTrue("Output should contain osgiConsole-1.0", output.indexOf("osgiConsole-1.0") >= 0);

		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Test the licenseAcceptance for a base feature. Note that this test case will
	 * only be run on an Open Liberty wlp. It will be skipped for Closed Liberty
	 * wlps.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testBaseLicenseAccept() throws Exception {
		final String METHOD_NAME = "testBaseLicenseAccept";
		Log.entering(c, METHOD_NAME);
		// Setup
		String[] adminCenter10FilesList = {
				relativeMinifiedRoot + "/wlp/lib/features/com.ibm.websphere.appserver.adminCenter-1.0.mf"

		};

		deleteFiles(METHOD_NAME, "adminCenter-1.0", adminCenter10FilesList);

		if (FeatureUtilityToolTest.isClosedLiberty) {
			Log.info(c, METHOD_NAME, "Wlp is already Closed liberty. This test case will not be run.");
			Log.exiting(c, METHOD_NAME);
			return;
		}
		replaceWlpProperties("20.0.0.4");
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/20.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/20.0.0.4/features-20.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/features/20.0.0.4",
				"../../publish/repo/io/openliberty/features/features/20.0.0.4/features-20.0.0.4.json");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/wlp-base-license/20.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/wlp-base-license/20.0.0.4/wlp-base-license-20.0.0.4.zip");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/wlp-nd-license/20.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/wlp-nd-license/20.0.0.4/wlp-nd-license-20.0.0.4.zip");

		// Begin Test
		Map<String, String> propsMap = new HashMap<String, String>();
		propsMap.put("featureLocalRepo", minifiedRoot + "/repo/");
		propsMap.put("wlptestjson.featuresbom", "com.ibm.websphere.appserver.features:features:20.0.0.4");
		writeToProps(minifiedRoot + "/etc/featureUtility.properties", propsMap);
		String[] param1s = { "installFeature", "adminCenter-1.0", "--acceptLicense", "--verbose" };

		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		String edition = getClosedLibertyWlpEdition();
		assertTrue("Edition should be found in the WebSphereApplicationServer.properties file", edition != null);
		assertTrue("Should be edition Base", (edition.contains("BASE")));

		deleteProps(METHOD_NAME);

		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Test the licenseAcceptance by providing both a base and ND feature, the
	 * resulting wlp should be of version ND. Note that this test case will only be
	 * run on an Open Liberty wlp. * It will be skipped for Closed Liberty wlps.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMultiFeatureLicenseAccept() throws Exception {
		final String METHOD_NAME = "testMultiFeatureLicenseAccept";
		Log.entering(c, METHOD_NAME);

		if (FeatureUtilityToolTest.isClosedLiberty) {
			Log.info(c, METHOD_NAME, "Wlp is already Closed liberty. This test case will not be run.");
			Log.exiting(c, METHOD_NAME);
			return;
		}
		replaceWlpProperties("20.0.0.4");
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/20.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/20.0.0.4/features-20.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/features/20.0.0.4",
				"../../publish/repo/io/openliberty/features/features/20.0.0.4/features-20.0.0.4.json");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/wlp-base-license/20.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/wlp-base-license/20.0.0.4/wlp-base-license-20.0.0.4.zip");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/wlp-nd-license/20.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/wlp-nd-license/20.0.0.4/wlp-nd-license-20.0.0.4.zip");

		Map<String, String> propsMap = new HashMap<String, String>();
		propsMap.put("featureLocalRepo", minifiedRoot + "/repo/");
		propsMap.put("wlptestjson.featuresbom", "com.ibm.websphere.appserver.features:features:20.0.0.4");
		writeToProps(minifiedRoot + "/etc/featureUtility.properties", propsMap);
		String[] param1s = { "installFeature", "adminCenter-1.0", "deploy-1.0", "--acceptLicense", "--verbose" };

		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		String edition = getClosedLibertyWlpEdition();
		assertTrue("Edition should be found in the WebSphereApplicationServer.properties file", edition != null);
		assertTrue("Should be edition ND", (edition.contains("ND")));

		deleteProps(METHOD_NAME);

		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Test the licenseAcceptance by providing both a base and ND feature, the
	 * resulting wlp should be of version ND.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFeatureLocalRepoOverride() throws Exception {
		final String METHOD_NAME = "testFeatureLocalRepoOverride";
		Log.entering(c, METHOD_NAME);
		// Setup
		replaceWlpProperties("20.0.0.4");
		String[] el30FilesList = { relativeMinifiedRoot + "/wlp/lib/features/com.ibm.websphere.appserver.el-3.0.mf" };
		deleteFiles(METHOD_NAME, "el-3.0", el30FilesList);
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/20.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/20.0.0.4/features-20.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/features/20.0.0.4",
				"../../publish/repo/io/openliberty/features/features/20.0.0.4/features-20.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/el-3.0/20.0.0.4",
				"../../publish/repo/io/openliberty/features/el-3.0/20.0.0.4/el-3.0-20.0.0.4.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/com.ibm.websphere.appserver.javax.el-3.0/20.0.0.4",
				"../../publish/repo/io/openliberty/features/com.ibm.websphere.appserver.javax.el-3.0/20.0.0.4/com.ibm.websphere.appserver.javax.el-3.0-20.0.0.4.esa");

		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");

		// Begin Test
		String[] param1s = { "installFeature", "el-3.0", "--verbose" };

		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Should contain el-3.0", output.contains("el-3.0"));

		deleteEtcFolder(METHOD_NAME);

		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Test the installation of a made up feature.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInvalidFeature() throws Exception {
		final String METHOD_NAME = "testInvalidFeature";
		Log.entering(c, METHOD_NAME);
		replaceWlpProperties("21.0.0.4");
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/21.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/21.0.0.4/features-21.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/features/21.0.0.4",
				"../../publish/repo/io/openliberty/features/features/21.0.0.4/features-21.0.0.4.json");

		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");
		String[] param1s = { "installFeature",
				"veryClearlyMadeUpFeatureThatNoOneWillEverThinkToCreateThemselvesAbCxYz-1.0", "--verbose" };
		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 21", 21, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Should contain CWWKF1299E or CWWKF1203E",
				output.indexOf("CWWKF1402E") >= 0 || output.indexOf("CWWKF1203E") >= 0);

		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Try to install a feature (ssl-1.0) twice. Expected to fail.
	 */
	@Test
	public void testAlreadyInstalledFeature() throws Exception {
		final String METHOD_NAME = "testAlreadyInstalledFeature";
		Log.entering(c, METHOD_NAME);
		// Setup
		replaceWlpProperties("21.0.0.4");
		String[] ssl10FilesList = { relativeMinifiedRoot + "/wlp/lib/features/com.ibm.websphere.appserver.ssl-1.0.mf" };
		deleteFiles(METHOD_NAME, "ssl-1.0", ssl10FilesList);

		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/21.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/21.0.0.4/features-21.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/features/21.0.0.4",
				"../../publish/repo/io/openliberty/features/features/21.0.0.4/features-21.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/ssl-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/ssl-1.0/21.0.0.4/com.ibm.websphere.appserver.certificateCreator-1.0.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/ssl-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/ssl-1.0/21.0.0.4/com.ibm.websphere.appserver.channelfw-1.0.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/ssl-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/ssl-1.0/21.0.0.4/com.ibm.websphere.appserver.ssl-1.0.esa");

		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");


		// Begin Test
		String[] param1s = { "installFeature", "ssl-1.0", "--verbose" };

		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Should contain ssl-1.0", output.contains("ssl-1.0"));

		po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 22 indicating already installed feature", 22, po.getReturnCode());
		output = po.getStdout();
		assertTrue("Should contain CWWKF1250I", output.contains("CWWKF1250I"));

		Log.exiting(c, METHOD_NAME);
	}

	// test case disabled for now
//    @Test
//    public void testInvalidMavenCoordinateGroupId() throws Exception {
//        String methodName = "testInvalidMavenCoordinateGroupId";
//        deleteFeaturesAndLafilesFolders(methodName);
//
//        String [] param1s = {"if", "madeUpGroupId:mpHealth-2.0"};
//        ProgramOutput po = runFeatureUtility(methodName, param1s);
//        assertEquals("Group ID does not exist", 21, po.getReturnCode());
//        String output = po.getStdout();
//        assertTrue("Msg contains CWWKF1402E", output.indexOf("CWWKF1402E") >=0);
//        deleteFeaturesAndLafilesFolders(methodName);
//
//        // TODO change this message in FeatureUtility
//
//    }

	@Test
	public void testInvalidMavenCoordinateArtifactId() throws Exception {
		String methodName = "testInvalidMavenCoordinateArtifactId";

		String[] param1s = { "if", "io.openliberty.features:mpHealth", "--verbose" };
		ProgramOutput po = runFeatureUtility(methodName, param1s);
		assertEquals("Invalid feature shortname", 21, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Expected CWWKF1402E", output.indexOf("CWWKF1402E") >= 0);

	}

	/**
	 * Test the output when passing in poorly formatted feature names or maven
	 * coordinates
	 *
	 * @throws Exception
	 */
	@Test
	public void testInvalidMavenCoordinateVersion() throws Exception {
		String methodName = "testInvalidMavenCoordinateVersion";
		// version mismatch. get an old Liberty version.

		String oldVersion = "19.0.0.1";
		String[] param1s = { "if", "io.openliberty.features:mpHealth-2.0:" + oldVersion, "--verbose" };
		ProgramOutput po = runFeatureUtility(methodName, param1s);
		assertEquals("Incompatible feature version", 21, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Expected CWWKF1395E msg", output.indexOf("CWWKF1395E") >= 0);

	}

	/**
	 * The packaging in a maven coordinate can only be "esa", so we must verify that
	 * it only works with esa.
	 *
	 * @throws Exception
	 */
	@Test
	public void testInvalidMavenCoordinatePackaging() throws Exception {
		String methodName = "testInvalidMavenCoordinatePackaging";
		Log.entering(c, methodName);

		String currentVersion = getCurrentWlpVersion();

		String[] jsp23FilesList = { relativeMinifiedRoot + "/wlp/lib/features/com.ibm.websphere.appserver.jsp-2.3.mf" };
		deleteFiles(methodName, "jsp-2.3", jsp23FilesList);

		// test with invalid packaging
		String[] param1s = { "if", "io.openliberty.features:jsp-2.3:" + currentVersion + ":jar", "--verbose" };
		ProgramOutput po = runFeatureUtility(methodName, param1s);
		assertEquals(21, po.getReturnCode());
		// "CWWKF1395E"
		String output = po.getStdout();
		assertTrue("expected CWWKF1396E", output.indexOf("CWWKF1396E") >= 0);

		// now try with valid packaging
		String[] param2s = { "if", "io.openliberty.features:jsp-2.3:" + currentVersion + ":esa", "--verbose" };
		po = runFeatureUtility(methodName, param2s);
		assertEquals("Should install successfully.", 0, po.getReturnCode());
		Log.exiting(c, methodName);
	}

	@Test
	public void testInvalidMavenCoordinateFormatting() throws Exception {
		String methodName = "testInvalidMavenCoordinateFormatting";
		ProgramOutput po;
		String output;
		String version = getCurrentWlpVersion();

		String[] param1s = { "if", "groupId:artifactId:" + version + ":esa:unsupportedOption", "--verbose" };
		po = runFeatureUtility(methodName, param1s);
		assertEquals(21, po.getReturnCode());
		output = po.getStdout();
		assertTrue("should output CWWKF1397E ", output.indexOf("CWWKF1397E") >= 0);

		String[] param2s = { "if", ":::", "--verbose" };
		po = runFeatureUtility(methodName, param2s);
		assertEquals(21, po.getReturnCode());
		output = po.getStdout();
		assertTrue("should output CWWKF1397E ", output.indexOf("CWWKF1397E") >= 0);

		String[] param3s = { "if", "groupId::" + version, "--verbose" };
		po = runFeatureUtility(methodName, param3s);
		assertEquals(21, po.getReturnCode());
		output = po.getStdout();
		assertTrue("should output CWWKF1397E ", output.indexOf("CWWKF1397E") >= 0);

		String[] param4s = { "if", "groupId:::esa", "--verbose" };
		po = runFeatureUtility(methodName, param4s);
		assertEquals(21, po.getReturnCode());
		output = po.getStdout();
		assertTrue("should output CWWKF1397E ", output.indexOf("CWWKF1397E") >= 0);

	}

	@Test
	public void testBlankFeature() throws Exception {
		String methodName = "testBlankFeature";

		String[] param1s = { "if", " ", "--verbose" };
		ProgramOutput po = runFeatureUtility(methodName, param1s);
		assertEquals(20, po.getReturnCode()); // 20 refers to ReturnCode.BAD_ARGUMENT
		String output = po.getStdout();
		assertTrue("Should refer to ./featureUtility help", output.indexOf("Usage") >= 0);

	}

	/**
	 * Tests the functionality of viewSettings --viewvalidationmessages with a
	 * well-formatted wlp/etc/featureUtility.properties file
	 */
	@Test
	public void testSettingsValidationClean() throws Exception {
		final String METHOD_NAME = "testSettingsValidationClean";
		Log.entering(c, METHOD_NAME);

		// copy over the featureUtility.properties file from the publish folder
		copyFileToMinifiedRoot("etc", "../../publish/tmp/cleanPropertyFile/featureUtility.properties");
		String[] param1s = { "viewSettings", "--viewvalidationmessages" };
		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals(0, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Should pass validation",
				output.contains("Validation Results: The properties file successfully passed the validation."));

		deleteEtcFolder(METHOD_NAME);
		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Tests the functionality of viewSettings --viewvalidationmessages with an
	 * invalid wlp/etc/featureUtility.properties file
	 */
	@Test
	public void testSettingsValidationInvalid() throws Exception {
		final String METHOD_NAME = "testSettingsValidationInvalid";
		Log.entering(c, METHOD_NAME);

		// copy over the featureUtility.properties file from the publish folder
		copyFileToMinifiedRoot("etc", "../../publish/tmp/invalidPropertyFile/featureUtility.properties");
		String[] param1s = { "viewSettings", "--viewvalidationmessages" };
		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals(20, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Shouldnt pass validation", output.contains("Number of errors"));

		deleteEtcFolder(METHOD_NAME);
		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Install an user feature with the "--featuresBom" parameters
	 */
	@Test
	public void testFeatureInstallUserFeature() throws Exception {
		final String METHOD_NAME = "testFeatureInstallUserFeature";
		Log.entering(c, METHOD_NAME);

		replaceWlpProperties("21.0.0.4");
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/21.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/21.0.0.4/features-21.0.0.4.json");
		copyFileToMinifiedRoot("repo/io/openliberty/features/features/21.0.0.4",
				"../../publish/repo/io/openliberty/features/features/21.0.0.4/features-21.0.0.4.json");
		copyFileToMinifiedRoot("repo/com/ibm/ws/userFeature/features-bom/19.0.0.8",
				"../../publish/repo/com/ibm/ws/userFeature/features-bom/19.0.0.8/features-bom-19.0.0.8.pom");
		copyFileToMinifiedRoot("repo/com/ibm/ws/userFeature/features/19.0.0.8",
				"../../publish/repo/com/ibm/ws/userFeature/features/19.0.0.8/features-19.0.0.8.json");
		copyFileToMinifiedRoot("repo/com/ibm/ws/userFeature/testesa1/19.0.0.8",
				"../../publish/repo/com/ibm/ws/userFeature/testesa1/19.0.0.8/testesa1-19.0.0.8.esa");

		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");
		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "enable.options", "true");

		String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

		String[] param1s = { "installFeature", "testesa1", "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8",
				"--verbose" };
		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		String output = po.getStdout();

		assertTrue("Should contain testesa1", output.contains("testesa1"));
		assertFilesExist(filesList);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());

		deleteUsrExtFolder(METHOD_NAME);
		deleteEtcFolder(METHOD_NAME);
		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Install an User feature with the "--to=Extension" parameters
	 */
	@Test
	public void testFeatureInstallUserFeatureToExtension() throws Exception {
		final String METHOD_NAME = "testFeatureInstallUserFeatureToExtension";
		Log.entering(c, METHOD_NAME);

		replaceWlpProperties("21.0.0.4");
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/21.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/21.0.0.4/features-21.0.0.4.json");
		copyFileToMinifiedRoot("repo/io/openliberty/features/features/21.0.0.4",
				"../../publish/repo/io/openliberty/features/features/21.0.0.4/features-21.0.0.4.json");
		copyFileToMinifiedRoot("repo/com/ibm/ws/userFeature/features-bom/19.0.0.8",
				"../../publish/repo/com/ibm/ws/userFeature/features-bom/19.0.0.8/features-bom-19.0.0.8.pom");
		copyFileToMinifiedRoot("repo/com/ibm/ws/userFeature/features/19.0.0.8",
				"../../publish/repo/com/ibm/ws/userFeature/features/19.0.0.8/features-19.0.0.8.json");
		copyFileToMinifiedRoot("repo/com/ibm/ws/userFeature/testesa1/19.0.0.8",
				"../../publish/repo/com/ibm/ws/userFeature/testesa1/19.0.0.8/testesa1-19.0.0.8.esa");

		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");
		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "enable.options", "true");

		String[] param1s = { "installFeature", "testesa1", "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8",
				"--to=ext.test", "--verbose" };

		createExtensionDirs("ext.test");

		String[] filesList = { "usr/cik/extensions/ext.test/lib/features/testesa1.mf",
				"usr/cik/extensions/ext.test/bin/testesa1.bat" };

		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		String output = po.getStdout();

		assertTrue("Should contain testesa1", output.contains("testesa1"));
		assertFilesExist(filesList);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());

		deleteUsrToExtFolder(METHOD_NAME);
		deleteEtcFolder(METHOD_NAME);
		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Test if user Features.json file exists on provided featuresBOM coordinate
	 */
	@Test
	public void testNonExistentUserFeaturesJson() throws Exception {
		final String METHOD_NAME = "testNonExistentUserFeaturesJson";
		Log.entering(c, METHOD_NAME);

		replaceWlpProperties("21.0.0.4");
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");
		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "enable.options", "true");

		String[] param1s = { "installFeature", "testesa1", "--featuresBOM=invalid:invalid:19.0.0.8", "--verbose" };

		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 21", 21, po.getReturnCode());
		String output = po.getStdout();
		assertTrue("Should contain CWWKF1409E", output.contains("CWWKF1409E"));

		deleteEtcFolder(METHOD_NAME);
		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Test invalid featuresBOM Maven coordinate
	 */
	@Test
	public void testInvalidUserFeatureBomCoordinate() throws Exception {
		final String METHOD_NAME = "testFearureInstallUserFeatureToExtension";
		Log.entering(c, METHOD_NAME);

		replaceWlpProperties("21.0.0.4");
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");
		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "enable.options", "true");

		String[] param1s = { "installFeature", "testesa1", "--featuresBOM=com.ibm.ws.userFeature:invalid",
				"--verbose" };

		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		String output = po.getStdout();

		assertTrue("Should contain CWWKF1503E", output.contains("CWWKF1503E"));
		assertEquals("Exit code should be 21", 21, po.getReturnCode());

		deleteEtcFolder(METHOD_NAME);
		Log.exiting(c, METHOD_NAME);
	}
}
