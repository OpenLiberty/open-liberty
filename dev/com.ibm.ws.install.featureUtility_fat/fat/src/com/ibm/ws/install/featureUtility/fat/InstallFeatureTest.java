/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.InstallException;

import componenttest.containers.ImageBuilder;
import componenttest.containers.SimpleLogConsumer;

public class InstallFeatureTest extends FeatureUtilityToolTest {

    private static final Class<?> c = InstallFeatureTest.class;
    private static String userFeatureSigPath = "/com/ibm/ws/userFeature/testesa1/19.0.0.8/testesa1-19.0.0.8.esa.asc";
    static Network network = Network.newNetwork();
    
    
//  @ClassRule
    /*
     * Increased startup timeout because nexus container might take more than 60
     * seconds (default start up time) to start. Also wait for log
     * "Started Sonatype Nexus.*" to make sure repository is up and running.
     * Disabling due to intermittent time out
     */
    // TODO publish Dockerfile for this custom image
//  public static GenericContainer<?> nexusContainer = new GenericContainer<>("jiwoo/nexus:1.0")
//      .withStartupTimeout(Duration.of(5, ChronoUnit.MINUTES))
//      .waitingFor(Wait.forLogMessage("Started Sonatype Nexus.*", 1))
//      .withNetwork(network)
//      .withNetworkAliases("nexus");
    
    //TODO Start using ImageBuilder
//  private static final DockerImageName KEYSERVER_SIMPLE = ImageBuilder.build("keyserver-simple:3.11.6").getDockerImageName();
    
    private static final DockerImageName KEYSERVER_SIMPLE = DockerImageName.parse("jiwoo/simple-keyserver:1.0");

    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>(KEYSERVER_SIMPLE)
        .withExposedPorts(8080).waitingFor(Wait.forHttp("/"))
        .withLogConsumer(new SimpleLogConsumer(InstallFeatureTest.class, "keyserver")).withNetwork(network)
        .withNetworkAliases("keyserver");

    //TODO publish Dockerfile for this custom image
    @ClassRule
    public static GenericContainer<?> proxyContainer = new GenericContainer<>("jiwoo/squid-proxy:1.0")
        .withExposedPorts(3128).withNetwork(network);


    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        final String methodName = "beforeClassSetup";
        /* Enable tests only if running on a zOS machine, otherwise skip class */
        Assume.assumeTrue(!isZos);
        Log.entering(c, methodName);
        deleteFeaturesAndLafilesFolders("beforeClassSetup");
        replaceWlpProperties(libertyVersion);
        Log.exiting(c, methodName);
    }

    @Before
    public void beforeSetUp() throws Exception {
        copyFileToMinifiedRoot("etc",
            "publish/propertyFiles/publishRepoOverrideProps/featureUtility.properties");
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

        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.jsp-2.2.mf",
            "/lib/features/com.ibm.websphere.appserver.jsp-2.3.mf" };

        // Begin Test
        String[] param1s = { "installFeature", "jsp-2.2", "jsp-2.3", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        try{
            checkCommandOutput(po, 0, null, filesList);
        }catch(AssertionError e) {
            retryFeatureUtility(METHOD_NAME);
        }
    }

    /**
     * Test installation of feature json-1.0 from local repository. Verifies feature
     * signature.
     *
     * @throws Exception
     */
    @Test
    public void testInstallFeature() throws Exception {
        final String METHOD_NAME = "testInstallFeature";
        Log.entering(c, METHOD_NAME);

        // Begin Test
        String[] param1s = { "installFeature", "json-1.0", "--verbose" };
        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.json-1.0.mf" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        checkCommandOutput(po, 0, null, filesList);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test installation of feature json-1.0.esa from local file.
     * 
     *
     * @throws Exception
     */
    @Test
    public void testInstallFeatureESA() throws Exception {
        final String METHOD_NAME = "testInstallFeatureESA";
        Log.entering(c, METHOD_NAME);

        String esaFile = String.format("/io/openliberty/features/json-1.0/%s/json-1.0-%s.esa", libertyVersion,
            libertyVersion);
        //copy json esa file from local Maven repo to a temporary location (wlp/tmp)
        copyFileToMinifiedRoot("tmp", mavenLocalRepo1 + esaFile);
        // Begin Test
        String[] param1s = { "installFeature", minifiedRoot + "/tmp/" + String.format("json-1.0-%s.esa", libertyVersion), "--verbose" };
        String[] filesList = { "lib/features/com.ibm.websphere.appserver.json-1.0.mf" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        checkCommandOutput(po, 0, null, filesList);

        Log.exiting(c, METHOD_NAME);
    }
    
    /**
     * Test installation of feature usertest.with.api.esa from local. 
     * 
     *
     * @throws Exception
     */
    @Test
    public void testInstallUsrFeatureESA() throws Exception {
        final String METHOD_NAME = "testInstallUsrFeatureESA";
        Log.entering(c, METHOD_NAME);
        copyFileToMinifiedRoot("tmp", "publish/features/usertest.with.api-1.0.esa");
        // Begin Test
        String[] param1s = { "installFeature", minifiedRoot + "/tmp/usertest.with.api-1.0.esa", "--verbose" };
        String[] filesList = { "usr/extension/lib/features/usertest.with.api.mf" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        checkCommandOutput(po, 0, null, filesList);

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

        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.eventLogging-1.0.mf",
            "/lib/features/com.ibm.websphere.appserver.osgiConsole-1.0.mf" };

        // Begin Test
        String[] param1s = { "installFeature", "eventLogging-1.0", "osgiConsole-1.0", "--verbose" };

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        checkCommandOutput(po, 0, null, filesList);

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

        if (FeatureUtilityToolTest.isClosedLiberty) {
        Log.info(c, METHOD_NAME, "Wlp is already Closed liberty. This test case will not be run.");
        Log.exiting(c, METHOD_NAME);
        return;
        }

        // Begin Test
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "wlptestjson.featuresbom",
            String.format("com.ibm.websphere.appserver.features:features:%s", libertyVersion));
        String[] param1s = { "installFeature", "rtcomm-1.0", "--acceptLicense", "-verbose" };

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

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "wlptestjson.featuresbom",
            String.format("com.ibm.websphere.appserver.features:features:%s", libertyVersion));
        String[] param1s = { "installFeature", "adminCenter-1.0", "deploy-1.0", "--acceptLicense" };

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String edition = getClosedLibertyWlpEdition();
        assertTrue("Edition should be found in the WebSphereApplicationServer.properties file", edition != null);
        assertTrue("Should be edition ND", (edition.contains("ND")));

        deleteProps(METHOD_NAME);

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

        String[] param1s = { "installFeature",
            "veryClearlyMadeUpFeatureThatNoOneWillEverThinkToCreateThemselvesAbCxYz-1.0", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String output = po.getStdout();

        if (FeatureUtilityToolTest.isClosedLiberty) {
        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1203E", null);
        } else {
        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1402E", null);
        }

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Try to install a feature (json-1.0) twice. Expected to fail.
     */
    @Test
    public void testAlreadyInstalledFeature() throws Exception {
        final String METHOD_NAME = "testAlreadyInstalledFeature";
        Log.entering(c, METHOD_NAME);
        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.json-1.0.mf" };

        // Begin Test
        String[] param1s = { "installFeature", "json-1.0", "--verbose" };

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, 0, null, filesList);

        // Run command again
        po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, InstallException.ALREADY_EXISTS, "CWWKF1250I", null);

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInvalidMavenCoordinateArtifactId() throws Exception {
        String methodName = "testInvalidMavenCoordinateArtifactId";

        String[] param1s = { "if", "io.openliberty.features:mpHealth", "--verbose" };
        ProgramOutput po = runFeatureUtility(methodName, param1s);

        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1402E", null);

    }

    /**
     * Test the output when passing in Maven coordinate of the older version of
     * Liberty feature
     * 
     *
     * @throws Exception
     */
    @Test
    public void testInvalidMavenCoordinateVersion() throws Exception {
        String methodName = "testInvalidMavenCoordinateVersion";
        // version mismatch. get an old Liberty version.

        String oldVersion = "22.0.0.13";
        String[] param1s = { "if", "io.openliberty.features:mpHealth-2.0:" + oldVersion, "--verbose" };
        ProgramOutput po = runFeatureUtility(methodName, param1s);

        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1395E", null);

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

        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.json-1.0.mf" };

        // test with invalid packaging
        String[] param1s = { "if", "io.openliberty.features:json-1.0:" + libertyVersion + ":jar", "--verbose" };
        ProgramOutput po = runFeatureUtility(methodName, param1s);
        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1396E", null);

        // now try with valid packaging
        String[] param2s = { "if", "io.openliberty.features:json-1.0:" + libertyVersion + ":esa", "--verbose" };
        po = runFeatureUtility(methodName, param2s);
        assertEquals("Should install successfully.", 0, po.getReturnCode());
        checkCommandOutput(po, 0, null, filesList);
        Log.exiting(c, methodName);
    }

    @Test
    public void testInvalidMavenCoordinateFormatting() throws Exception {
        String methodName = "testInvalidMavenCoordinateFormatting";
        ProgramOutput po;
        String output;

        String[] param1s = { "if", "groupId:artifactId:" + libertyVersion + ":esa:unsupportedOption", "--verbose" };
        po = runFeatureUtility(methodName, param1s);
        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1397E", null);

        String[] param2s = { "if", ":::", "--verbose" };
        po = runFeatureUtility(methodName, param2s);
        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1397E", null);

        String[] param3s = { "if", "groupId::" + libertyVersion, "--verbose" };
        po = runFeatureUtility(methodName, param3s);
        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1397E", null);

        String[] param4s = { "if", "groupId:::esa", "--verbose" };
        po = runFeatureUtility(methodName, param4s);
        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1397E", null);
    }

    @Test
    public void testBlankFeature() throws Exception {
        String methodName = "testBlankFeature";

        String[] param1s = { "if", " ", "--verbose" };
        ProgramOutput po = runFeatureUtility(methodName, param1s);
        String output = po.getStdout();

        assertTrue("Should refer to ./featureUtility help", output.indexOf("Usage") >= 0);
        assertEquals(InstallException.BAD_ARGUMENT, po.getReturnCode()); // 20 refers to ReturnCode.BAD_ARGUMENT
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
        copyFileToMinifiedRoot("etc", "publish/tmp/cleanPropertyFile/featureUtility.properties");
        String[] param1s = { "viewSettings", "--viewvalidationmessages" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String output = po.getStdout();

        assertTrue("Should pass validation",
            output.contains("Validation Results: The properties file successfully passed the validation."));
        assertEquals(0, po.getReturnCode());

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
        copyFileToMinifiedRoot("etc", "publish/tmp/invalidPropertyFile/featureUtility.properties");
        String[] param1s = { "viewSettings", "--viewvalidationmessages" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String output = po.getStdout();

        assertTrue("Shouldnt pass validation", output.contains("Number of errors"));
        assertEquals(InstallException.BAD_ARGUMENT, po.getReturnCode());
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Install an user feature with the "--featuresBom" parameters. By default, it
     * does not verify feature signature.
     */
    @Test
    public void testFeatureInstallUserFeature() throws Exception {
        final String METHOD_NAME = "testFeatureInstallUserFeature";
        Log.entering(c, METHOD_NAME);
        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, 0, null, filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Install an User feature with the "--to=Extension" parameters. By default, it
     * does not verify feature signature.
     */
    @Test
    public void testFeatureInstallUserFeatureToExtension() throws Exception {
        final String METHOD_NAME = "testFeatureInstallUserFeatureToExtension";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--to=ext.test", "--verbose" };

        createExtensionDirs("ext.test");

        String[] filesList = { "usr/cik/extensions/ext.test/lib/features/testesa1.mf",
            "usr/cik/extensions/ext.test/bin/testesa1.bat" };

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, 0, null, filesList);

        deleteUsrToExtFolder(METHOD_NAME);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test if user Features.json file exists on provided featuresBOM coordinate
     */
    @Test
    public void testNonExistentUserFeaturesJson() throws Exception {
        final String METHOD_NAME = "testNonExistentUserFeaturesJson";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "testesa1", "--featuresBOM=invalid:invalid:19.0.0.8", "--verbose" };

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1409E", null);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test invalid featuresBOM Maven coordinate
     */
    @Test
    public void testInvalidUserFeatureBomCoordinate() throws Exception {
        final String METHOD_NAME = "testFearureInstallUserFeatureToExtension";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "testesa1", "--featuresBOM=com.ibm.ws.userFeature:invalid",
            "--verbose" };

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, InstallException.RUNTIME_EXCEPTION, "CWWKF1503E", null);

        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature with iFix applied. With iFix applied, it will usually
     * copy the new jar into wlp/lib folder without modifying the original jar (by
     * appending date to the file name). However, some iFixes will directly replace
     * the jar inside wlp/lib and the file needs to be revalidated. This test case
     * will mimic this case.
     */
    @Test
    public void testInstallFeatureWithIfix() throws Exception {
        final String METHOD_NAME = "testInstallFeatureWithIfix";
        Log.entering(c, METHOD_NAME);

        // Set up test iFix
        copyFileToMinifiedRoot("lib", "publish/tmp/iFix/com.ibm.ws.install.testIfix_1.0.jar");
        // Feature manifest file so the tool picks up the new testIfix_1.0.jar
        copyFileToMinifiedRoot("lib/platform", "publish/tmp/iFix/testIfix-1.0.mf");
        // Checksum file for testIfix_1.0.jar - Has incorrect checksum so that it fails
        // initial validation
        // If 1st check fails, then the tool looks up xml and lpmf files
        copyFileToMinifiedRoot("lib/platform/checksums",
            "publish/tmp/iFix/com.ibm.websphere.appserver.testIfix-1.0.cs");
        // These files will have the correct checksum.
        copyFileToMinifiedRoot("lib/fixes", "publish/tmp/iFix/xml.xml");
        copyFileToMinifiedRoot("lib/fixes", "publish/tmp/iFix/lpmf.lpmf");

        // Begin Test
        String[] param1s = { "installFeature", "json-1.0", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        // delete manifest file so the tool doesn't pick up.
        deleteFiles(METHOD_NAME, "testIfix-1.0",
            new String[] { relativeMinifiedRoot + "/wlp/lib/platform/testIfix-1.0.mf" });

        checkCommandOutput(po, 0, null, null);

        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=enforce with user feature. Default verify option
     * is "enforce". Only IBM Liberty feature will be verified. User feature
     * signature verification is expected to fail as we are not providing public key
     * for the user feature but should install all features successfully.
     */
    @Test
    public void testFeatureVerifyENFORCE() throws Exception {
        final String METHOD_NAME = "testFeatureVerifyENFORCE";
        Log.entering(c, METHOD_NAME);

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1", "json-1.0",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, 0, null, filesList);
        Log.exiting(c, METHOD_NAME);
    }
    
    /*
     * Test installFeature --verify=enforce with user feature and no signature (userFeature.asc) file. Default verify option
     * is "enforce". Only IBM Liberty feature will be verified. User feature
     * signature verification is expected to fail as there are no signature file and public key to verify,
     *  but should install all features successfully.
     */
    
    @Test
    public void testFeatureVerifyENFORCEnoSig() throws Exception {
        final String METHOD_NAME = "testFeatureVerifyENFORCEnoSig";
        Log.entering(c, METHOD_NAME);

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };
        
        //copy testesa1 esa file from local Maven repo to a temporary location (wlp/tmp)
        copyFileToMinifiedRoot("tmp", mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/19.0.0.8/testesa1-19.0.0.8.esa");
        
        // Begin Test
        String[] param1s = { "installFeature", minifiedRoot + "/tmp/testesa1-19.0.0.8.esa", "json-1.0",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, 0, null, filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=all with user feature. Expected to verify both
     * Liberty feature and user feature. Provided valid public key and signature to
     * verify the user feature.
     */
    @Test
    public void testFeatureVerifyALL() throws Exception {
        final String METHOD_NAME = "testFeatureVerifyALL";
        Log.entering(c, METHOD_NAME);

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
            mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/valid/validKey.asc");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "71f8e6239b6834aa");

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verify=all", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, 0, null, filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=skip with user feature. Should skip the whole
     * verification process (downloading the public key and verifying the feature
     * signatures)
     */
    @Test
    public void testFeatureVerifySKIP() throws Exception {
        final String METHOD_NAME = "testFeatureVerifySKIP";
        Log.entering(c, METHOD_NAME);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
            mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/valid/validKey.asc");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "71f8e6239b6834aa");

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verify=skip", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        String output = po.getStdout();

        assertFalse("Verified feature signatures ", output.contains("Verfying signatures ..."));

        checkCommandOutput(po, 0, null, filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=warning with user feature. Feature verification
     * should fail, but the user feature should be installed successfully.
     */
    @Test
    public void testFeatureVerifyWARN() throws Exception {
        final String METHOD_NAME = "testFeatureVerifyWARN";
        Log.entering(c, METHOD_NAME);

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verify=warn", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, 0, null, filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=invalid option
     */
    @Test
    public void testFeatureVerifyInvalid() throws Exception {
        final String METHOD_NAME = "testFeatureVerifyInvalid";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verify=invalid", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, InstallException.SIGNATURE_VERIFICATION_FAILED, "CWWKF1505E", null);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature verify option mismatch from the command line vs
     * properties file --verify=all vs feature.verify=skip
     */
    @Test
    public void testFeatureVerifyOptionMismatch() throws Exception {
        final String METHOD_NAME = "testFeatureVerifyOptionMismatch";
        Log.entering(c, METHOD_NAME);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "skip");
        ;

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verify=all", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, InstallException.SIGNATURE_VERIFICATION_FAILED, "CWWKF1504E", null);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature with verify=all set in featureUtility.properties file.
     * 
     */
    @Test
    public void testFeatureVerifyALLProps() throws Exception {
        final String METHOD_NAME = "testFeatureVerifyALLProps";
        Log.entering(c, METHOD_NAME);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
            mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/valid/validKey.asc");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "71f8e6239b6834aa");

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1", "json-1.0",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, 0, null, filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature with verify=all set in ENV.
     */
    @Test
    public void testFeatureVerifyALLEnv() throws Exception {
        final String METHOD_NAME = "testFeatureVerifyALLEnv";
        Log.entering(c, METHOD_NAME);
        Properties envProps = new Properties();
        envProps.put("FEATURE_VERIFY", "all");

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
            mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/valid/validKey.asc");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "71f8e6239b6834aa");

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1", "json-1.0",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s, envProps);
        checkCommandOutput(po, 0, null, filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=all from external test container
     */
    @Test
    public void testVerifyhttpKeyServer() throws Exception {
        final String METHOD_NAME = "testVerifyhttpKeyServer";
        Log.entering(c, METHOD_NAME);

        String containerUrl = "http://" + container.getHost() + ":" + container.getMappedPort(8080)
            + "/validKey.asc";

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "0x71f8e6239b6834aa");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl", containerUrl);

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1", "json-1.0",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        checkCommandOutput(po, 0, null, filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=all with revoked key
     */
    /**
     * @throws Exception
     */
    @Test
    public void testVerifyRevokedKey() throws Exception {
        final String METHOD_NAME = "testVerifyRevokedKey";
        Log.entering(c, METHOD_NAME);
        Properties envProps = new Properties();
        envProps.put("FEATURE_VERIFY", "all");

        // backup the valid user feature signature
        Files.move(Paths.get(mavenLocalRepo1 + userFeatureSigPath),
            Paths.get(mavenLocalRepo1 + userFeatureSigPath + ".bck"));
        // overwrite with signature signed by revoked key
        Files.copy(Paths.get(mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/revoked/testesa1-19.0.0.8.esa.asc"),
            Paths.get(mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/19.0.0.8/testesa1-19.0.0.8.esa.asc"));

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
            mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/revoked/revokedKey.asc");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "2CB7FEADC826EA27");

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        // Change back to valid signature
        Files.move(Paths.get(mavenLocalRepo1 + userFeatureSigPath + ".bck"),
            Paths.get(mavenLocalRepo1 + userFeatureSigPath),
            StandardCopyOption.REPLACE_EXISTING);

        checkCommandOutput(po, InstallException.SIGNATURE_VERIFICATION_FAILED, "CWWKF1510E", null);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=all with expired key
     * 
     * 
     */
    @Test
    public void testVerifyExpiredKey() throws Exception {
        final String METHOD_NAME = "testVerifyMITKeyServer";
        Log.entering(c, METHOD_NAME);
        Properties envProps = new Properties();
        envProps.put("FEATURE_VERIFY", "all");

        // backup the valid user feature signature
        Files.move(Paths.get(mavenLocalRepo1 + userFeatureSigPath),
            Paths.get(mavenLocalRepo1 + userFeatureSigPath + ".bck"));
        // overwrite with signature signed by expired key
        Files.copy(Paths.get(mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/expired/testesa1-19.0.0.8.esa.asc"),
            Paths.get(mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/19.0.0.8/testesa1-19.0.0.8.esa.asc"));

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
            mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/expired/expiredKey.asc");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "61B792CE2DAA8C02");

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        // Change back to valid signature
        Files.move(Paths.get(mavenLocalRepo1 + userFeatureSigPath + ".bck"),
            Paths.get(mavenLocalRepo1 + userFeatureSigPath), StandardCopyOption.REPLACE_EXISTING);

        checkCommandOutput(po, InstallException.SIGNATURE_VERIFICATION_FAILED, "CWWKF1511E", null);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=all with no keyID provided.
     * 
     */
    @Test
    public void testVerifyNoKeyId() throws Exception {
        final String METHOD_NAME = "testVerifyNoKeyId";
        Log.entering(c, METHOD_NAME);
        Properties envProps = new Properties();
        envProps.put("FEATURE_VERIFY", "all");

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
            mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/valid/validKey.asc");

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        checkCommandOutput(po, InstallException.SIGNATURE_VERIFICATION_FAILED, "CWWKF1508E", null);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=warn with invalid keyserver. Expected to exit
     * with error. If the keyserver that user provided is not valid (not reachable,
     * wrong url, etc), then we do not have a key to verify their user feature.
     * 
     */
    @Test
    public void testInvalidKeyServerWARN() throws Exception {
        final String METHOD_NAME = "testInvalidKeyServer";
        Log.entering(c, METHOD_NAME);
        Properties envProps = new Properties();
        envProps.put("FEATURE_VERIFY", "all");

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "warn");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
            "https://keyserver.invalid.ibm.com");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "71f8e6239b6834aa");

        String[] param1s = { "installFeature", "testesa1", "json-1.0",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        checkCommandOutput(po, InstallException.SIGNATURE_VERIFICATION_FAILED, "CWWKF1506E", null);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=all with unsupported keyserver protocol.
     * Supported protocols are HTTP, HTTPS, and file.
     * 
     */
    @Test
    public void testInvalidKeyServerProtocol() throws Exception {
        final String METHOD_NAME = "testInvalidKeyServerProtocol";
        Log.entering(c, METHOD_NAME);
        Properties envProps = new Properties();
        envProps.put("FEATURE_VERIFY", "all");

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "71f8e6239b6834aa");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
            "ftp:///repo/com/ibm/ws/userFeature/testesa1/valid/validKey.asc");

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        checkCommandOutput(po, InstallException.SIGNATURE_VERIFICATION_FAILED, "CWWKF1509E", null);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=all with bad signature.
     * 
     */
    @Test
    public void testVerifyBadSignature() throws Exception {
        final String METHOD_NAME = "testVerifyBadSignature";
        Log.entering(c, METHOD_NAME);
        Properties envProps = new Properties();
        envProps.put("FEATURE_VERIFY", "all");

        // backup the valid user feature signature
        Files.move(Paths.get(mavenLocalRepo1 + userFeatureSigPath),
            Paths.get(mavenLocalRepo1 + userFeatureSigPath + ".bck"));
        // overwrtie valid signature to invalid signature
        Files.copy(
            Paths.get(mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/invalidSig/testesa1-19.0.0.8.esa.asc"),
            Paths.get(mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/19.0.0.8/testesa1-19.0.0.8.esa.asc"));

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "71f8e6239b6834aa");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl",
            mavenLocalRepo1 + "/com/ibm/ws/userFeature/testesa1/valid/validKey.asc");

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        // Change back to valid signature
        Files.move(Paths.get(mavenLocalRepo1 + userFeatureSigPath + ".bck"),
            Paths.get(mavenLocalRepo1 + userFeatureSigPath), StandardCopyOption.REPLACE_EXISTING);


        checkCommandOutput(po, InstallException.SIGNATURE_VERIFICATION_FAILED, "CWWKF1512E", null);
        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test the updated resolve() api which returns all tolerated dependencies of
     * all features to be installed. Test with "RestfulWS-3.0".
     */
    @Test
    public void testComplexVersionTolerates() throws Exception {
        final String METHOD_NAME = "testComplexVersionTolerates";
        Log.entering(c, METHOD_NAME);

        // Begin Test
        String[] param1s = { "installFeature", "RestfulWS-3.0" };
        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.eeCompatible-6.0.mf",
            "/lib/features/com.ibm.websphere.appserver.eeCompatible-7.0.mf",
            "/lib/features/com.ibm.websphere.appserver.eeCompatible-8.0.mf",
            "/lib/features/com.ibm.websphere.appserver.eeCompatible-9.0.mf",
            "/lib/features/io.openliberty.servlet.api-3.0.mf",
            "/lib/features/io.openliberty.servlet.api-3.1.mf",
            "/lib/features/io.openliberty.servlet.api-4.0.mf",
            "/lib/features/io.openliberty.servlet.api-5.0.mf" };

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        checkCommandOutput(po, 0, null, filesList);

        Log.exiting(c, METHOD_NAME);
    }

    /*
     * Test installFeature --verify=all from external test container with proxy through properties
     */
    @Test
    public void testProxyAuth() throws Exception {
        final String METHOD_NAME = "testProxyAuth";
        Log.entering(c, METHOD_NAME);

        String proxyHost = "http://" + proxyContainer.getHost();
        String proxyPort = proxyContainer.getMappedPort(3128).toString();

        String containerUrl = "http://keyserver:8080/validKey.asc";

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "0x71f8e6239b6834aa");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl", containerUrl);

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyHost", proxyHost);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyPort", proxyPort);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyUser", "wasngi");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyPassword", "test");

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1", "json-1.0",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
      
        try {
        checkCommandOutput(po, 0, null, filesList);
        } catch (AssertionError e) {
        checkProxyLog(METHOD_NAME, proxyContainer);
          throw e;
        }
        
        Log.exiting(c, METHOD_NAME);
    }
    
    /*
     * Test installFeature --verify=all from external test container with proxy through env
     */
    @Test
    public void testProxyEnv() throws Exception {
        final String METHOD_NAME = "testProxyEnv";
        Log.entering(c, METHOD_NAME);

        String proxyHost = proxyContainer.getHost();
        String proxyPort = proxyContainer.getMappedPort(3128).toString();

        String containerUrl = "http://keyserver:8080/validKey.asc";

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "0x71f8e6239b6834aa");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl", containerUrl);
        
        Properties envProps = new Properties();
        envProps.put("http_proxy", "http://wasngi:test@" + proxyHost + ":" + proxyPort);


        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1", "json-1.0",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s, envProps);
      
        try {
                checkCommandOutput(po, 0, null, filesList);
        } catch (AssertionError e) {
                checkProxyLog(METHOD_NAME, proxyContainer);
                throw e;
        }
        
        Log.exiting(c, METHOD_NAME);

    }
    
    /*
     * Test installFeature --verify=all from external test container with proxy through env
     */
    @Test
    public void testProxyWrongFormatEnv() throws Exception {
        final String METHOD_NAME = "testProxyWrongFormatEnv";
        Log.entering(c, METHOD_NAME);

        String proxyHost = proxyContainer.getHost();
        String proxyPort = proxyContainer.getMappedPort(3128).toString();

        String containerUrl = "http://keyserver:8080/validKey.asc";

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "0x71f8e6239b6834aa");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl", containerUrl);
        
        Properties envProps = new Properties();
        envProps.put("http_proxy", "http://wasngi:test@" + "://" + proxyHost + ":" + proxyPort);


        String[] param1s = { "installFeature", "testesa1", "json-1.0",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s, envProps);
      
        try {
                checkCommandOutput(po, 21, "CWWKF1401E" , null);
        } catch (AssertionError e) {
                throw e;
        }
        
        Log.exiting(c, METHOD_NAME);

    }
    
    @Test
    @Ignore("Disabling due to intermittent nexusContianer startup timeout")
    public void testProxyAndRepoAuth() throws Exception {
        final String METHOD_NAME = "testProxyAndRepoAuth";
        Log.entering(c, METHOD_NAME);
        
//        container.dependsOn(nexusContainer).start();
        
        String proxyHost = "http://" + proxyContainer.getHost();
        String proxyPort = proxyContainer.getMappedPort(3128).toString();
        String nexusURL = "http://nexus:8081/repository/maven-central/";
        
        //overwrite the local maven repo so it can fetch the artifact from nexus repo
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", "tmp");
            
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyHost", proxyHost);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyPort", proxyPort);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyUser", "wasngi");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyPassword", "test");
        
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "mavenCentralMirror.url", nexusURL);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "mavenCentralMirror.user", "admin");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "mavenCentralMirror.password", "golf");
        
        String[] param1s = { "installFeature", "json-1.0", "--verbose", "--noCache" };
        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.json-1.0.mf" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        try {
        checkCommandOutput(po, 0, null, filesList);
        } catch (AssertionError e) {
        checkProxyLog(METHOD_NAME, proxyContainer);
          throw e;
        }
        
        Log.exiting(c, METHOD_NAME);

    }

    @Test
    @Ignore("Disabling due to intermittent nexusContianer startup timeout")
    public void testProxyAndRepoAuthEncoded() throws Exception {
        final String METHOD_NAME = "testProxyAndRepoAuthEncoded";
        Log.entering(c, METHOD_NAME);

//        container.dependsOn(nexusContainer).start();

        String proxyHost = "http://" + proxyContainer.getHost();
        String proxyPort = proxyContainer.getMappedPort(3128).toString();
        String nexusURL = "http://nexus:8081/repository/maven-central/";

        // overwrite the local maven repo so it can fetch the artifact from nexus repo
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "featureLocalRepo", "tmp");

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyHost", proxyHost);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyPort", proxyPort);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyUser", "wasngi");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyPassword", "{xor}KzosKw==");

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "mavenCentralMirror.url", nexusURL);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "mavenCentralMirror.user", "admin");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "mavenCentralMirror.password",
            "{xor}ODAzOQ==");

        String[] param1s = { "installFeature", "json-1.0", "--verbose", "--noCache" };
        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.json-1.0.mf" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        try {
        checkCommandOutput(po, 0, null, filesList);
        } catch (AssertionError e) {
        checkProxyLog(METHOD_NAME, proxyContainer);
        throw e;
        }
        
        Log.exiting(c, METHOD_NAME);
        
    }

    /*
     * Test no proxy using environment variable Try downloading public key from
     * external key server. It should fail because keyserver can only be connected
     * through proxy. (testcontainer network)
     */
    @Test
    public void testnoProxyEnv() throws Exception {
        final String METHOD_NAME = "testnoProxyEnv";
        Log.entering(c, METHOD_NAME);

        String proxyHost = "http://" + proxyContainer.getHost();
        String proxyPort = proxyContainer.getMappedPort(3128).toString();
        String containerUrl = "http://keyserver:8080/validKey.asc";

        Properties envProps = new Properties();
        envProps.put("no_proxy", "keyserver");


        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyHost", proxyHost);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyPort", proxyPort);
        
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "0x71f8e6239b6834aa");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl", containerUrl);

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };

        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s, envProps);

        try {
        checkCommandOutput(po, InstallException.SIGNATURE_VERIFICATION_FAILED, "CWWKF1506E", null);
        } catch (AssertionError e) {
        checkProxyLog(METHOD_NAME, proxyContainer);
        throw e;
        }

        Log.exiting(c, METHOD_NAME);

    }

    /*
     * Test no proxy using properties
     */
    @Test
    public void testnoProxyProps() throws Exception {
        final String METHOD_NAME = "testnoProxyProps";
        Log.entering(c, METHOD_NAME);

        String proxyHost = "http://" + proxyContainer.getHost();
        String proxyPort = proxyContainer.getMappedPort(3128).toString();
        String containerUrl = "http://keyserver:8080/validKey.asc";

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyHost", proxyHost);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "proxyPort", proxyPort);
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "http.nonProxyHosts", "keyserver");

        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "feature.verify", "all");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyid", "0x71f8e6239b6834aa");
        writeToProps(minifiedRoot + "/etc/featureUtility.properties", "myKey.keyurl", containerUrl);

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat" };

        String[] param1s = { "installFeature", "testesa1",
            "--featuresBOM=com.ibm.ws.userFeature:features-bom:19.0.0.8", "--verbose" };
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);

        try {
        checkCommandOutput(po, InstallException.SIGNATURE_VERIFICATION_FAILED, "CWWKF1506E", null);
        } catch (AssertionError e) {
        checkProxyLog(METHOD_NAME, proxyContainer);
        throw e;
        }

        Log.exiting(c, METHOD_NAME);

    }

}
