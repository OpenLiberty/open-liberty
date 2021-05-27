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
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
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
        resetOriginalWlpProps();
        cleanUpTempFiles();
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
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        String noFeaturesMessage = "The server does not require any additional features.";
        assertTrue("No features should be installed", output.indexOf(noFeaturesMessage) >= 0);

        Log.exiting(c, METHOD_NAME);
    }

//    /**
//     * // TODO. this test case will be added once the disableUsrFeatures pull request is merged!!
//     * @throws Exception
//     */
//    @Test
//    public void testUsrFeatureServerXml() throws Exception {
//        String METHOD_NAME = "testUsrFeatureServerXml";
//        copyFileToMinifiedRoot("usr/servers/serverZ", "../../publish/tmp/usrFeaturesServerXml/server.xml");
//        String[] param2s = { "installServerFeatures", "serverZ"};
//        deleteFeaturesAndLafilesFolders(METHOD_NAME);
//
//
//        ProgramOutput po = runFeatureUtility(METHOD_NAME, param2s);
//        assertEquals("Exit code should be 0",0, po.getReturnCode());
//        String output = po.getStdout();
//        // server.xml contains jsp-2.3, so jsp-2.3 should be installed.
//        assertTrue("Output should contain jsp-2.3", output.indexOf("jsp-2.3") >= 0);
//
//        String noFeaturesMessage = InstallLogUtils.Messages.INSTALL_KERNEL_MESSAGES.getMessage("MSG_SERVER_NEW_FEATURES_NOT_REQUIRED");
//        assertTrue("No features should be installed", output.indexOf(noFeaturesMessage) >= 0);
//    }

//    /**
//     * Install a server twice.
//     */
//    public void testAlreadyInstalledFeatures() throws Exception {
//        final String METHOD_NAME = "testAlreadyInstalledFeatures";
//        Log.entering(c, METHOD_NAME);
//
//        // replace the server.xml
//        copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/plainServerXml/server.xml");
//
//        // install the server
//        String[] param1s = { "installServerFeatures", "serverX"};
//        deleteFeaturesAndLafilesFolders(METHOD_NAME);
//        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
//        assertEquals("Exit code should be 0",0, po.getReturnCode());
//        String output = po.getStdout();
//        assertTrue("Output should contain jsp-2.3", output.indexOf("jsf-2.2") >= 0);
//
//        // install server again
//        deleteFeaturesAndLafilesFolders(METHOD_NAME);
//        po = runFeatureUtility(METHOD_NAME, param1s);
//        assertEquals("Exit code should be 22",22, po.getReturnCode());
////        output = po.getStdout();
////        assertTrue("Output should contain jsp-2.3", output.indexOf("jsf-2.2") >= 0);
//
//
//
//        Log.exiting(c, METHOD_NAME);
//    }


    /**
     * Install jsf-2.2 on its own first, then install a server.xml with jsf-2.2 and cdi-1.2. The autofeature cdi1.2-jsf2.2 should be installed along with the other features.
     * @throws Exception
     */
    @Test
    public void testInstallAutoFeatureServerXml() throws Exception {
        final String METHOD_NAME = "testInstallAutoFeatureServerXml";
        Log.entering(c, METHOD_NAME);

        String [] param1s = {"installFeature", "jsf-2.2", "--verbose"};

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Output should contain jsf-2.2", output.indexOf("jsf-2.2") >= 0);

        // replace the server.xml and install from server.xml now
        copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/autoFeatureServerXml/server.xml");
        String[] param2s = { "installServerFeatures", "serverX"};
        deleteFeaturesAndLafilesFolders(METHOD_NAME);


        po = runFeatureUtility(METHOD_NAME, param2s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        output = po.getStdout();
        assertTrue("Output should contain jsf-2.2", output.indexOf("jsf-2.2") >= 0);
        assertTrue("Output should contain cdi-1.2", output.indexOf("cdi-1.2") >= 0);
        assertTrue("The autofeature cdi1.2-jsf-2.2 should be installed" , new File(minifiedRoot + "/lib/features/com.ibm.websphere.appserver.cdi1.2-jsf2.2.mf").exists());

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

        copyFileToMinifiedRoot("usr/servers/serverX", "../../publish/tmp/multiVersionServerXml/server.xml");
        String[] param1s = { "installServerFeatures", "serverX", "--verbose"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 21",21, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain CWWKF1405E", output.contains("CWWKF1405E"));

//        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.jsp-2.3", fileLists);
        Log.exiting(c, METHOD_NAME);
    }


}
