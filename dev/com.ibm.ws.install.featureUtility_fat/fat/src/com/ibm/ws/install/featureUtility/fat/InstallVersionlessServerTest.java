/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

public class InstallVersionlessServerTest extends FeatureUtilityToolTest {
    private static final Class<?> c = InstallVersionlessServerTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
	final String methodName = "beforeClassSetup";
        Log.entering(c, methodName);
        /* Enable tests only if running on a zOS machine, otherwise skip class */
        Assume.assumeTrue(!isZos);
	deleteFeaturesAndLafilesFolders("beforeClassSetup");
	replaceWlpProperties("24.0.0.8");
        Log.exiting(c, methodName);
    }

    @Before
    public void beforeSetUp() throws Exception {
	copyFileToMinifiedRoot("etc", "publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
	writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", mavenLocalRepo2);
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
     * Test the install of versionless with bogus platform name xxx from maven central. Should throw expected platform name not found
     *
     * @throws Exception
     */
    @Test
    public void testVersionlessWithBadPlatformFeatures() throws Exception {
	final String METHOD_NAME = "testVersionlessWithBadPlatformFeatures";
	Log.entering(c, METHOD_NAME);

	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/versionlessBadPlatform/server.xml");
	
	String[] param1s = { "installServerFeatures", "serverX", "--verify=skip", "--verbose" };
	
	ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

	checkCommandOutput(po, 21, "CWWKF1515E", null); //UnKnown platform error
	Log.exiting(c, METHOD_NAME);
    }
    
    /**
     * Test the install of versionless with no platform defined. Should throw expected platform can't be determined error
     *
     * @throws Exception
     */
    @Test
    public void testVersionlessWithNoPlatformFeatures() throws Exception {
	final String METHOD_NAME = "testVersionlessWithNoPlatformFeatures";
	Log.entering(c, METHOD_NAME);

	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/versionlessNoPlatform/server.xml");

	String[] param1s = { "installServerFeatures", "serverX", "--verify=skip", "--verbose" };
	ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

	checkCommandOutput(po, 21, "CWWKF1516E", null); //Platform not determined
	Log.exiting(c, METHOD_NAME);
    }

     /**
     * Test the install of versionless with  platform name. install servlet-4.0 feature
     *
     * @throws Exception
     */
    @Test
    public void testVersionlessWithPlatformFeatures() throws Exception {
	final String METHOD_NAME = "testVersionlessWithPlatformFeatures";
	Log.entering(c, METHOD_NAME);

	copyFileToMinifiedRoot("usr/servers/serverX", "publish/tmp/versionlessWithPlatform/server.xml");
	
	String[] param1s = { "installServerFeatures", "serverX", "--verify=skip", "--verbose" };
	String[] filesList = { "/lib/features/com.ibm.websphere.appserver.servlet-4.0.mf" };
	
	ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

	checkCommandOutput(po, 0, null, filesList); //Should have servlet-6.0
	Log.exiting(c, METHOD_NAME);
    }

    

}
