/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class InstallFeatureTest extends FeatureUtilityToolTest {

    private static final Class<?> c = InstallFeatureTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        final String methodName = "setup";
        Log.entering(c, methodName);
        setupEnv();

        // rollback wlp version 2 times (e.g 20.0.0.5 -> 20.0.0.3)
        replaceWlpProperties(getPreviousWlpVersion());
        replaceWlpProperties(getPreviousWlpVersion());
        Log.exiting(c, methodName);
    }
    
    @AfterClass
    public static void cleanUp() throws Exception {
        // TODO
        resetOriginalWlpProps();
        cleanUpTempFiles();
    }

    /**
     * Test the install of jsp-2.3 from maven central.
     * 
     * @throws Exception
     */
    @Test
    public void testInstallFeature() throws Exception {
        final String METHOD_NAME = "testInstallFeature";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "jsp-2.3"};
        String [] fileLists = {"lib/features/com.ibm.websphere.appserver.jsp-2.3.mf"};
//        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.jsp-2.3", fileLists);
        deleteFeaturesAndLafilesFolders(METHOD_NAME);

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain jsp-2.3", output.contains("jsp-2.3"));

//        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.jsp-2.3", fileLists);
        deleteFeaturesAndLafilesFolders(METHOD_NAME);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test the installation of features cdi-1.2 and jsf-2.2 together, which should also install the autofeature cdi1.2-jsf2.2.
     * @throws Exception
     */
    @Test
    public void testInstallAutoFeature() throws Exception {
        final String METHOD_NAME = "testInstallAutoFeature";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "jsf-2.2", "cdi-1.2"};
        String [] fileListA = {"lib/features/com.ibm.websphere.appserver.jsf-2.2.mf", "lib/features/com.ibm.websphere.appserver.cdi1.2-jsf2.2.mf"};
        String [] fileListB = {"lib/features/com.ibm.websphere.appserver.cdi-1.2.mf"};
//        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.jsf-2.2", fileListA);
//        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.cdi-1.2", fileListB);
        deleteFeaturesAndLafilesFolders(METHOD_NAME);


        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Output should contain jsf-2.2", output.indexOf("jsf-2.2") >= 0);
        assertTrue("Output should contain cdi-1.2", output.indexOf("cdi-1.2") >= 0);
        assertTrue("The autofeature cdi1.2-jsf-2.2 should be installed" , new File(minifiedRoot + "/lib/features/com.ibm.websphere.appserver.cdi1.2-jsf2.2.mf").exists());

//        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.jsf-2.2", fileListA);
////        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.cdi-1.2", fileListB);
        deleteFeaturesAndLafilesFolders(METHOD_NAME);


        Log.exiting(c, METHOD_NAME);
    }


    /**
     * Test the licenseAcceptance for a base feature
     * @throws Exception
     */
    @Test
    public void testBaseLicenseAccept() throws Exception {
        final String METHOD_NAME = "testBaseLicenseAccept";
        Log.entering(c, METHOD_NAME);
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
        
        writeToProps(minifiedRoot+ "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");
        String[] param1s = { "installFeature", "adminCenter-1.0", "--acceptLicense" };

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String edition = getWlpEdition();
        
        assertTrue("Should be edition Base", (edition.contains("BASE")));

        deleteProps(METHOD_NAME);
        deleteRepo(METHOD_NAME);
        deleteFeaturesAndLafilesFolders(METHOD_NAME);


        Log.exiting(c, METHOD_NAME);
    }
    
    /**
     * Test the licenseAcceptance by providing both a base and ND feature, the resulting wlp should be
     * of version ND.
     * @throws Exception
     */
    @Test
    public void testMultiFeatureLicenseAccept() throws Exception {
        final String METHOD_NAME = "testBaseLicenseAccept";
        Log.entering(c, METHOD_NAME);
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
        
        writeToProps(minifiedRoot+ "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");
        String[] param1s = { "installFeature", "adminCenter-1.0", "deploy-1.0", "--acceptLicense" };

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String edition = getWlpEdition();
        
        assertTrue("Should be edition ND", (edition.contains("ND")));

        deleteProps(METHOD_NAME);
        deleteRepo(METHOD_NAME);
        deleteFeaturesAndLafilesFolders(METHOD_NAME);


        Log.exiting(c, METHOD_NAME);
    }
    
    /**
     * Test the licenseAcceptance by providing both a base and ND feature, the resulting wlp should be
     * of version ND.
     * @throws Exception
     */
    @Test
    public void testFeatureLocalRepoOverride() throws Exception {
        final String METHOD_NAME = "testBaseLicenseAccept";
        Log.entering(c, METHOD_NAME);
        replaceWlpProperties("20.0.0.4");
        copyFileToMinifiedRoot("etc", "../../publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
        
        copyFileToMinifiedRoot("repo/com/ibm/websphere/appserver/features/features/20.0.0.4",
        		"../../publish/repo/com/ibm/websphere/appserver/features/features/20.0.0.4/features-20.0.0.4.json");
        
        copyFileToMinifiedRoot("repo/io/openliberty/features/features/20.0.0.4",
        		"../../publish/repo/io/openliberty/features/features/20.0.0.4/features-20.0.0.4.json");
        
        copyFileToMinifiedRoot("repo/io/openliberty/features/el-3.0/20.0.0.4",
        		"../../publish/repo/io/openliberty/features/el-3.0/20.0.0.4/el-3.0-20.0.0.4.esa");
        
        
        writeToProps(minifiedRoot+ "/etc/featureUtility.properties", "featureLocalRepo", minifiedRoot + "/repo/");
        String[] param1s = { "installFeature", "el-3.0", "--verbose"};

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain el-3.0", output.contains("el-3.0"));
        
        
        deleteProps(METHOD_NAME);
        deleteRepo(METHOD_NAME);
        deleteFeaturesAndLafilesFolders(METHOD_NAME);


        Log.exiting(c, METHOD_NAME);
    }


    /**
     * Test the installation of a made up feature.
     * @throws Exception
     */
    @Test
    public void testInvalidFeature() throws Exception {
        final String METHOD_NAME = "testInvalidFeature";
        Log.entering(c, METHOD_NAME);

        deleteFeaturesAndLafilesFolders(METHOD_NAME);
        String[] param1s = { "installFeature", "veryClearlyMadeUpFeatureThatNoOneWillEverThinkToCreateThemselvesAbCxYz-1.0"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 21",21,  po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain CWWKF1299E or CWWKF1203E", output.indexOf("CWWKF1402E")>=0 ||output.indexOf("CWWKF1203E") >= 0);
        deleteFeaturesAndLafilesFolders(METHOD_NAME);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Try to install a feature (mpHealth-2.0) twice. Expected to fail.
     */
    @Test
    public void testAlreadyInstalledFeature() throws Exception {
        final String METHOD_NAME = "testAlreadyInstalledFeature";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "mpHealth-2.0"};
        String [] fileLists = {"lib/features/com.ibm.websphere.appserver.mpHealth-2.0.mf"};
//        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.mpHealth-2.0", fileLists);
        deleteFeaturesAndLafilesFolders(METHOD_NAME);

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Should contain mpHealth-2.0", output.contains("mpHealth-2.0"));

        po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 22 indicating already installd feature",22, po.getReturnCode());
        output = po.getStdout();
        assertTrue("Should contain CWWKF1250I", output.contains("CWWKF1250I"));


//        deleteFiles(METHOD_NAME, "com.ibm.websphere.appserver.mpHealth-2.0", fileLists);
        deleteFeaturesAndLafilesFolders(METHOD_NAME);
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
        deleteFeaturesAndLafilesFolders(methodName);

        String [] param1s = {"if", "io.openliberty.features:mpHealth"};
        ProgramOutput po = runFeatureUtility(methodName, param1s);
        assertEquals("Invalid feature shortname", 21, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Expected CWWKF1402E", output.indexOf("CWWKF1402E") >= 0);
        deleteFeaturesAndLafilesFolders(methodName);

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
        deleteFeaturesAndLafilesFolders(methodName);

        String oldVersion = "19.0.0.1";
        String [] param1s = {"if", "io.openliberty.features:mpHealth-2.0:"+oldVersion};
        ProgramOutput po = runFeatureUtility(methodName, param1s);
        assertEquals("Incompatible feature version" , 21, po.getReturnCode());
        String output = po.getStdout();
        assertTrue("Expected CWWKF1395E msg", output.indexOf("CWWKF1395E") >= 0);
        deleteFeaturesAndLafilesFolders(methodName);

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

        deleteFeaturesAndLafilesFolders(methodName);

        // test with invalid packaging
        String [] param1s = {"if", "io.openliberty.features:jsp-2.3:"+currentVersion+":jar"};
        ProgramOutput po = runFeatureUtility(methodName, param1s);
        assertEquals(21, po.getReturnCode());
        //"CWWKF1395E"
        String output = po.getStdout();
        assertTrue("expected CWWKF1396E", output.indexOf("CWWKF1396E")>=0);

        // now try with valid packaging
        String [] param2s = {"if", "io.openliberty.features:jsp-2.3:"+currentVersion+":esa"};
        po = runFeatureUtility(methodName, param2s);
        assertEquals("Should install successfully.", 0, po.getReturnCode());
        deleteFeaturesAndLafilesFolders(methodName);
        Log.exiting(c, methodName);
    }

    @Test
    public void testInvalidMavenCoordinateFormatting() throws Exception {
        String methodName = "testInvalidMavenCoordinateFormatting";
        ProgramOutput po;
        String output;
        String version = getCurrentWlpVersion();

        deleteFeaturesAndLafilesFolders(methodName);


        String [] param1s = {"if", "groupId:artifactId:"+version+":esa:unsupportedOption"};
        po = runFeatureUtility(methodName, param1s);
        assertEquals(21, po.getReturnCode());
        output = po.getStdout();
        assertTrue("should output CWWKF1397E ", output.indexOf("CWWKF1397E")>=0);

        String [] param2s = {"if", ":::"};
        po = runFeatureUtility(methodName, param2s);
        assertEquals(21, po.getReturnCode());
        output = po.getStdout();
        assertTrue("should output CWWKF1397E ", output.indexOf("CWWKF1397E")>=0);
        

        String [] param3s = {"if", "groupId::" + version};
        po = runFeatureUtility(methodName, param3s);
        assertEquals(21, po.getReturnCode());
        output = po.getStdout();
        assertTrue("should output CWWKF1397E ", output.indexOf("CWWKF1397E")>=0);


        String [] param4s = {"if", "groupId:::esa"};
        po = runFeatureUtility(methodName, param4s);
        assertEquals(21, po.getReturnCode());
        output = po.getStdout();
        assertTrue("should output CWWKF1397E ", output.indexOf("CWWKF1397E")>=0);

        deleteFeaturesAndLafilesFolders(methodName);

    }

    @Test
    public void testBlankFeature() throws Exception {
        String methodName = "testBlankFeature";
        deleteFeaturesAndLafilesFolders(methodName);

        String [] param1s = {"if" , " "};
        ProgramOutput po = runFeatureUtility(methodName, param1s);
        assertEquals(20, po.getReturnCode()); // 20 refers to ReturnCode.BAD_ARGUMENT
        String output = po.getStdout();
        assertTrue("Should refer to ./featureUtility help", output.indexOf("Usage")>=0);
        deleteFeaturesAndLafilesFolders(methodName);


    }

    /**
     * TODO need to set up environmental variables
     */
    @Test
    public void testProxyFeature(){
        assertEquals("", 2, 1 + 1);
    }

}
