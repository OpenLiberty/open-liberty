/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.internal.InstallLogUtils;

public class InstallServerTest extends FeatureUtilityToolTest {
    private static final Class<?> c = FeatureUtilityToolTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        final String methodName = "setup";
        Log.entering(c, methodName);
        /* Enable tests only if running on a zOS machine, otherwise skip class */
        Assume.assumeTrue(!isZos);
        setupEnv();
        copyFileToMinifiedRoot("usr/temp", "../../publish/tmp/serverX.zip");
        replaceWlpProperties(getPreviousWlpVersion());
        replaceWlpProperties(getPreviousWlpVersion());
        Log.exiting(c, methodName);
    }

    @Before
    public void beforeCleanUp() throws Exception {
        resetOriginalWlpProps();
        replaceWlpProperties(getPreviousWlpVersion());
        replaceWlpProperties(getPreviousWlpVersion());
        deleteFeaturesAndLafilesFolders("beforeCleanUp");
    }

    @After
    public void afterCleanUp() throws Exception {
        resetOriginalWlpProps();
        replaceWlpProperties(getPreviousWlpVersion());
        replaceWlpProperties(getPreviousWlpVersion());
        deleteFeaturesAndLafilesFolders("afterCleanUp");
    }

	@AfterClass
	public static void cleanUp() throws Exception {
		// TODO
		if (!isZos) {
			resetOriginalWlpProps();
			cleanUpTempFiles();
		}
	}
    /**
     * Test install when no features are specified in server.xml
     */
    @Test
    public void testInstallBlankFeatures() throws Exception {
        String METHOD_NAME = "testInstallBlankFeatures";
        Log.entering(c, METHOD_NAME);

        copyFileToMinifiedRoot("usr/servers/serverY", "../../publish/tmp/noFeaturesServerXml/server.xml");
        String[] param2s = { "installServerFeatures", "serverY", "--verbose"};

		ProgramOutput po = runFeatureUtility(METHOD_NAME, param2s);
        String output = po.getStdout();
        String noFeaturesMessage = "The server does not require any additional features.";

        assertTrue("No features should be installed", output.indexOf(noFeaturesMessage) >= 0);
        assertEquals("Exit code should be 0",0, po.getReturnCode());

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

		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");

		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/21.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/21.0.0.4/features-21.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/features/21.0.0.4",
				"../../publish/repo/io/openliberty/features/features/21.0.0.4/features-21.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/json-1.0/21.0.0.4",
				"../../publish/repo/io/openliberty/features/json-1.0/21.0.0.4/json-1.0-21.0.0.4.esa");

		// replace the server.xml
		copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/plainServerXml/server.xml");

		// install the server
		String[] param1s = { "installServerFeatures", "serverX" };
		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());

		// install server again
		po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());

		Log.exiting(c, METHOD_NAME);
	}


    /**
     * Install osgiConsole-1.0 on its own first, then install a server.xml with osgiConsole-1.0 and eventLogging-1.0. The autofeature osgiConsole-1.0-eventLogging-1.0 should be installed along with the other features.
     * @throws Exception
     */
    @Test
    public void testInstallAutoFeatureServerXml() throws Exception {
        final String METHOD_NAME = "testInstallAutoFeatureServerXml";
        Log.entering(c, METHOD_NAME);
        replaceWlpProperties("21.0.0.4");
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
        
        writeToProps(minifiedRoot+ "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");

        String [] param1s = {"installFeature", "osgiConsole-1.0", "--verbose"};

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String output = po.getStdout();
        assertTrue("Output should contain osgiConsole-1.0", output.indexOf("osgiConsole-1.0") >= 0);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());

        // replace the server.xml and install from server.xml now
        copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/autoFeatureServerXml/server.xml");
        String[] param2s = { "installServerFeatures", "serverX", "--verbose"};
        deleteFeaturesAndLafilesFolders(METHOD_NAME);


        po = runFeatureUtility(METHOD_NAME, param2s);
        output = po.getStdout();
        assertTrue("Output should contain osgiConsole-1.0", output.indexOf("osgiConsole-1.0") >= 0);
        assertTrue("Output should contain eventLogging-1.0", output.indexOf("eventLogging-1.0") >= 0);
        // assertTrue("The autofeature eventLogging-1.0-osgiConsole-1.0 should be installed" , new File(minifiedRoot + "/lib/features/com.ibm.websphere.appserver.eventLogging-1.0-osgiConsole-1.0.mf").exists());
		assertEquals("Exit code should be 0", 0, po.getReturnCode());
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test the install of jsp-2.2, jsp-2.3 from maven central.
     * Multi-version is not supported with installServerFeature as it cannot be installed to same resource. 
     *
     * @throws Exception
     */
    @Test
    public void testInvalidMultiVersionFeatures() throws Exception {
        final String METHOD_NAME = "testInvalidMultiVersionFeatures";
        Log.entering(c, METHOD_NAME);
        
		replaceWlpProperties("20.0.0.4");
		copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/20.0.0.4",
				"../../publish/repo/com/ibm/websphere/appserver/features/features/20.0.0.4/features-20.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/features/20.0.0.4",
				"../../publish/repo/io/openliberty/features/features/20.0.0.4/features-20.0.0.4.json");

		copyFileToMinifiedRoot("repo/io/openliberty/features/jsp-2.3/20.0.0.4",
				"../../publish/repo/io/openliberty/features/jsp-2.3/20.0.0.4/jsp-2.3-20.0.0.4.esa");

		copyFileToMinifiedRoot("repo/io/openliberty/features/jsp-2.2/20.0.0.4",
				"../../publish/repo/io/openliberty/features/jsp-2.2/20.0.0.4/jsp-2.2-20.0.0.4.esa");

		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
        copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/multiVersionServerXml/server.xml");
		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");

        String[] param1s = { "installServerFeatures", "serverX", "--verbose"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String output = po.getStdout();

        assertTrue("Should contain CWWKF1405E", output.contains("CWWKF1405E"));

//        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.jsp-2.3", fileLists);
		assertEquals("Exit code should be 21", 21, po.getReturnCode());
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Install an user feature with the "--featuresBom" parameters
     */
    @Test
    public void testInstallServerFeatureUserFeature() throws Exception {
        final String METHOD_NAME = "testInstallServerFeatureUserFeature";
        Log.entering(c, METHOD_NAME);

        replaceWlpProperties("21.0.0.4");
        copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
        copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/usrFeaturesServerXml/server.xml");
        
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
        
        writeToProps(minifiedRoot+ "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");
        
        String[] filesList = { "usr/extension/lib/features/testesa1.mf",
								"usr/extension/bin/testesa1.bat" };
        
        String[] param1s = { "installServerFeatures", "serverX", "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String output = po.getStdout();
        
        assertFilesExist(filesList);
        assertTrue("Should contain testesa1", output.contains("testesa1"));

		deleteUsrExtFolder(METHOD_NAME);
		deleteEtcFolder(METHOD_NAME);

        assertEquals("Exit code should be 0",0, po.getReturnCode());

        Log.exiting(c, METHOD_NAME);
    }
    

    /**
     * Install an User feature with the "ext.test:testesa1" parameters
     */
    @Test
    public void testInstallUserFeatureToExtension() throws Exception {
    	final String METHOD_NAME = "testInstallUserFeatureToExtension";
        Log.entering(c, METHOD_NAME);

        replaceWlpProperties("21.0.0.4");
        copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
        copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/usrFeaturesToServerXml/server.xml");
        
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
        
        writeToProps(minifiedRoot+ "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");
        
        String[] param1s = { "installServerFeatures", "serverX", "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose"};
        
        createExtensionDirs("ext.test");
        
        String[] filesList = { "usr/cik/extensions/ext.test/lib/features/testesa1.mf",
        						"usr/cik/extensions/ext.test/bin/testesa1.bat" };
        
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String output = po.getStdout();
        
        assertTrue("Should contain testesa1", output.contains("testesa1"));
        assertFilesExist(filesList);

        deleteUsrToExtFolder(METHOD_NAME);
        deleteEtcFolder(METHOD_NAME);

		assertEquals("Exit code should be 0", 0, po.getReturnCode());
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

		replaceWlpProperties("22.0.0.1");

		String[] filesList = { "lib/features/io.openliberty.wimcore.internal.ee-9.0.mf",
				"lib/features/com.ibm.websphere.appserver.federatedRegistry-1.0.mf" };

		// jakartaee-8.0 and federatedRegistry-1.0
		copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/plainServerXml2/server.xml");

		// install the server
		String[] param1s = { "installServerFeatures", "serverX" };
		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());

		copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/plainServerXml3/server.xml");

		// install server again with jakartaee-9.1 and federatedRegistry-1.0
		po = runFeatureUtility(METHOD_NAME, param1s);

		assertFilesExist(filesList);
		assertEquals("Exit code should be 0", 0, po.getReturnCode());

		Log.exiting(c, METHOD_NAME);
	}

	/**
	 * Test already installed user feature with WLP_USER_DIR set
	 */
	@Test
	public void testAlreadyInstalledUsrFeatureWlpUserDir() throws Exception {
		final String METHOD_NAME = "testAlreadyInstalledUsrFeatureWlpUserDir";
		Log.entering(c, METHOD_NAME);

		replaceWlpProperties("21.0.0.4");
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
		copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/server.env");

		copyFileToMinifiedRoot("myUserDir/servers/serverX", "../../publish/tmp/usrFeaturesServerXml/server.xml");

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
		writeToProps(minifiedRoot + "/etc/featureUtility.properties", "test.featuresBOM",
				"com.ibm.ws.userFeature:features-bom:19.0.0.8");

		Properties envProps = new Properties();
		envProps.put("WLP_USER_DIR", minifiedRoot + "/myUserDir");

		String[] filesList = { "myUserDir/extension/lib/features/testesa1.mf", "myUserDir/extension/bin/testesa1.bat" };

		String[] param1s = { "installServerFeatures", "serverX", "--verbose" };
		ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s, envProps);
		String output = po.getStdout();

		assertFilesExist(filesList);
		assertTrue("Should contain testesa1", output.contains("testesa1"));

		// run isf command again
		po = runFeatureUtility(METHOD_NAME, param1s, envProps);
		output = po.getStdout();
		assertTrue("Should contain \"No features were installed\"", output.contains("No features were installed"));

		deleteUsrExtFolder(METHOD_NAME);
		deleteEtcFolder(METHOD_NAME);

		assertEquals("Exit code should be 0", 0, po.getReturnCode());

		Log.exiting(c, METHOD_NAME);
	}

}
