/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.featureUtility.fat;

import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class InstallServerTest extends FeatureUtilityToolTest {
    private static final Class<?> c = InstallServerTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
	final String methodName = "beforeClassSetup";
        Log.entering(c, methodName);
        /* Enable tests only if running on a zOS machine, otherwise skip class */
        Assume.assumeTrue(!isZos);
	deleteFeaturesAndLafilesFolders("beforeClassSetup");
	replaceWlpProperties(libertyVersion);
        Log.exiting(c, methodName);
    }

    @Before
    public void beforeSetUp() throws Exception {
	copyFileToMinifiedRoot("etc", "publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
	writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", mavenLocalRepo1);
    }

    @After
    public void afterCleanUp() throws Exception {
	deleteFeaturesAndLafilesFolders("afterCleanUp");
	deleteUsrExtFolder("afterCleanUp");
	deleteEtcFolder("afterCleanUp");
    }

	@AfterClass
	public static void cleanUp() throws Exception {
		if (!isZos) {
			resetOriginalWlpProps();
		}
	}

    /**
     * Test install when no features are specified in server.xml
     */
    @Test
    public void testInstallBlankFeatures() throws Exception {
        String METHOD_NAME = "testInstallBlankFeatures";
        Log.entering(c, METHOD_NAME);

	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/noFeaturesServerXml/server.xml");
	String[] param2s = { "installServerFeatures", "serverX", "--verbose" };

		ProgramOutput po = runFeatureUtility(METHOD_NAME, param2s);
        String output = po.getStdout();
        String noFeaturesMessage = "The server does not require any additional features.";

        assertTrue("No features should be installed", output.indexOf(noFeaturesMessage) >= 0);
	checkCommandOutput(po, 0, null, null);

        Log.exiting(c, METHOD_NAME);
    }


    /**
     * Install a server twice. If new features are added, it should install new
     * features. If all features all already installed, then exit with rc = 0.
     */
    @Test
    public void testAlreadyInstalledFeatures() throws Exception {
	final String METHOD_NAME = "testAlreadyInstalledFeatures";
	Log.entering(c, METHOD_NAME);

	// replace the server.xml
	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/plainServerXml/server.xml");

	// install the server
	String[] param1s = { "installServerFeatures", "serverX" };
	ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
	checkCommandOutput(po, 0, null, null);

	// install server again
	po = runFeatureUtility(METHOD_NAME, param1s);
	checkCommandOutput(po, 0, null, null);

	Log.exiting(c, METHOD_NAME);
    }

    /**
     * Install osgiConsole-1.0 on its own first, then install a server.xml with
     * osgiConsole-1.0 and eventLogging-1.0. The autofeature
     * osgiConsole-1.0-eventLogging-1.0 should be installed along with the other
     * features. TODO: check if this is auto feature case.
     * 
     * @throws Exception
     */
    @Test
    public void testInstallAutoFeatureServerXml() throws Exception {
	final String METHOD_NAME = "testInstallAutoFeatureServerXml";
	Log.entering(c, METHOD_NAME);

	String[] param1s = { "installFeature", "osgiConsole-1.0", "--verbose" };
//	String[] filesList = { "/lib/features/com.ibm.websphere.appserver.eventLogging-1.0-osgiConsole-1.0.mf" };

	ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
	checkCommandOutput(po, 0, null, null);

	// replace the server.xml and install from server.xml now
	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/autoFeatureServerXml/server.xml");
	String[] param2s = { "installServerFeatures", "serverX", "--verbose" };

	po = runFeatureUtility(METHOD_NAME, param2s);

	checkCommandOutput(po, 0, null, null);
	Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test the install of jsp-2.2, jsp-2.3 from maven central. Multi-version is not
     * supported with installServerFeature as it cannot be installed to same
     * resource.
     *
     * @throws Exception
     */
    @Test
    public void testInvalidMultiVersionFeatures() throws Exception {
	final String METHOD_NAME = "testInvalidMultiVersionFeatures";
	Log.entering(c, METHOD_NAME);

	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/multiVersionServerXml/server.xml");

	String[] param1s = { "installServerFeatures", "serverX", "--verbose" };
	ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

	checkCommandOutput(po, 21, "CWWKF1405E", null);
	Log.exiting(c, METHOD_NAME);
    }
    
   
    /**
     * Install an user feature with the "--featuresBom" parameters
     */
    @Test
    public void testInstallServerFeatureUserFeature() throws Exception {
	final String METHOD_NAME = "testInstallServerFeatureUserFeature";
	Log.entering(c, METHOD_NAME);
	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/usrFeaturesServerXml/server.xml");

	String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

	String[] param1s = { "installServerFeatures", "serverX",
		"--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
	ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
	checkCommandOutput(po, 0, null, filesList);

	Log.exiting(c, METHOD_NAME);
    }

    /**
     * Install an User feature with the "ext.test:testesa1" parameters
     */
    @Test
    public void testInstallUserFeatureToExtension() throws Exception {
	final String METHOD_NAME = "testInstallUserFeatureToExtension";
	Log.entering(c, METHOD_NAME);
	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/usrFeaturesToServerXml/server.xml");

	String[] param1s = { "installServerFeatures", "serverX",
		"--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };

	createExtensionDirs("ext.test");

	String[] filesList = { "usr/cik/extensions/ext.test/lib/features/testesa1.mf",
		"usr/cik/extensions/ext.test/bin/testesa1.bat" };

	ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
	checkCommandOutput(po, 0, null, filesList);
	Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test installServerFeature when a feature is installed but its dependencies
     * aren't. This can happen when a feature tolerates two versions of a
     * dependency, only one of which is installed, and the solution from the kernel
     * resolver uses the other one
     */
    @Test
    public void testIsfFeatureTolerates() throws Exception {
	final String METHOD_NAME = "testIsfFeatureTolerates";
	Log.entering(c, METHOD_NAME);
	String[] filesList = { "lib/features/io.openliberty.wimcore.internal.ee-9.0.mf",
		"lib/features/com.ibm.websphere.appserver.federatedRegistry-1.0.mf" };

	// jakartaee-8.0 and federatedRegistry-1.0
	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/plainServerXml2/server.xml");

	// install the server
	String[] param1s = { "installServerFeatures", "serverX" };
	ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
	checkCommandOutput(po, 0, null, null);

	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/plainServerXml3/server.xml");

	// install server again with jakartaee-9.1 and federatedRegistry-1.0
	po = runFeatureUtility(METHOD_NAME, param1s);
	
	try{
        checkCommandOutput(po, 0, null, filesList);
    }catch(AssertionError e) {
        retryFeatureUtility(METHOD_NAME);
    }

	Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test already installed user feature with WLP_USER_DIR set
     */
    @Test
    public void testAlreadyInstalledUsrFeatureWlpUserDir() throws Exception {
	final String METHOD_NAME = "testAlreadyInstalledUsrFeatureWlpUserDir";
	Log.entering(c, METHOD_NAME);

	copyFileToMinifiedRoot("etc", "publish/propertyFiles/server.env");

	copyFileToMinifiedRoot("myUserDir/servers/serverX", "publish/tmp/usrFeaturesServerXml/server.xml");

	writeToProps(minifiedRoot + "/etc/featureUtility.properties", "test.featuresBOM",
		"com.ibm.ws.userFeature:features-bom:19.0.0.8");

	Properties envProps = new Properties();
	envProps.put("WLP_USER_DIR", minifiedRoot + "/myUserDir");

	String[] filesList = { "myUserDir/extension/lib/features/testesa1.mf", "myUserDir/extension/bin/testesa1.bat" };

	String[] param1s = { "installServerFeatures", "serverX", "--verbose" };
	ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s, envProps);
	checkCommandOutput(po, 0, null, filesList);

	// run isf command again
	po = runFeatureUtility(METHOD_NAME, param1s, envProps);
	checkCommandOutput(po, 0, "No features were installed", filesList);

	Log.exiting(c, METHOD_NAME);
    }

	/**
	 * Install an user feature with the "--featuresBom" and "--verify=all"
	 * parameters
	 */
	@Test
	public void testInstallServerFeaturesUsrVerifyAll() throws Exception {
	    final String METHOD_NAME = "testInstallServerFeaturesUsrVerifyAll";
	    Log.entering(c, METHOD_NAME);

	    copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/usrFeaturesServerXml/server.xml");

	    writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
		    mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/valid/validKey.asc");
	    writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "71f8e6239b6834aa");

	    String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

	    String[] param1s = { "installServerFeatures", "serverX",
		    "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verify=all", "--verbose" };

	    ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
	    checkCommandOutput(po, 0, null, filesList);

	    Log.exiting(c, METHOD_NAME);
	}

	/*
	 * Test resolveAsSet(EE8, adminCenter-1.0), then resolve(EE9). There should be
	 * extra private features for adminCenter-1.0 which we need to install if we
	 * want adminCenter to work with EE9.
	 */
	@Test
	public void testResolveAsSetThenResolve() throws Exception {
	    final String METHOD_NAME = "testResolveAsSetThenResolve";
	    Log.entering(c, METHOD_NAME);

	    copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/adminCenterServerXml/server.xml");

	    String[] param1s = { "installServerFeatures", "serverX" };
	    String[] filesList = { "/lib/features/io.openliberty.adminCenter1.0.internal.ee-9.0.mf" };

	    // Run isf first
	    ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
	    checkCommandOutput(po, 0, null, null);

	    // Install jakartaee-9.1 and check if private feature is installed.
	    String[] param2s = { "installFeature", "jakartaee-9.1" };
	    po = runFeatureUtility(METHOD_NAME, param2s);  
	    try{
            checkCommandOutput(po, 0, null, filesList);
        }catch(AssertionError e) {
            retryFeatureUtility(METHOD_NAME);
        }

	    Log.exiting(c, METHOD_NAME);

	}

}
