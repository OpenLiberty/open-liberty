/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.suite.staging;

import static com.ibm.ws.test.image.suite.images.BuildProperties.GA_FEATURE_NAMES;
import static com.ibm.ws.test.image.suite.images.Images.MAVEN_FEATURE_NAMES;
import static com.ibm.ws.test.image.suite.images.Images.OL_MAVEN_FEATURES;
import static com.ibm.ws.test.image.suite.images.Images.getFeatureRepositoryPath;
import static com.ibm.ws.test.image.util.FileUtils.TEST_OUTPUT_PATH_ABS;
import static com.ibm.ws.test.image.util.FileUtils.ensureNonexistence;
import static com.ibm.ws.test.image.util.FileUtils.verify;
import static com.ibm.ws.test.image.util.ProcessRunner.getJavaHomePathAbs;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipException;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.test.image.build.BuildImages;
import com.ibm.ws.test.image.build.BuildImages.ImageType;
import com.ibm.ws.test.image.installation.ServerInstallation;

@RunWith(Parameterized.class)
public class InstallFeaturesTest {
    public static final String CLASS_NAME =
        InstallFeaturesTest.class.getSimpleName();

    public static void log(String msg) {
        System.out.println(msg);
    }

    //
    
    public static String getTestName() {
        return "installFeatures";
    }

    @Parameters
    public static Iterable<? extends Object> data() {
        return InstallFeaturesData.TEST_DATA;
    }

    public static final List<String> RAW_FEATURES = null;
    
    public InstallFeaturesTest(String tag, List<String> features) {
        this.tag = tag;
        this.features = features;
    }

    private final String tag;
    
    public String getTag() {
        return tag;
    }

    private final List<String> features;
    
    public boolean hasFeatures() {
        return ( features != null );
    }

    public List<String> getFeatures() {
        return features;
    }
    
    //

    private static boolean doRun;

    private static String baseLocalBuildPath;
    private static String baseLocalTmpPath;
    private static String baseLocalWorkPath;

    @BeforeClass
    public static void setUpClass() throws Exception {
        doRun = Boolean.getBoolean("create.im.repo");
        if ( doRun ) {
            log("Installation tests are enabled");
        } else {
            log("Installation tests are not enabled");
            return;
        }

        String useTestName = getTestName();

        baseLocalBuildPath = "./build/test/" + useTestName;
        log("Base build [ " + baseLocalBuildPath + " ]");
        ensureNonexistence(baseLocalBuildPath);        

        baseLocalTmpPath = TEST_OUTPUT_PATH_ABS + "/" + useTestName;
        log("Base local temp [ " + baseLocalTmpPath + " ]");
        ensureNonexistence(baseLocalTmpPath);        

        baseLocalWorkPath = TEST_OUTPUT_PATH_ABS + "/" + useTestName + "Work";
        log("Base local work [ " + baseLocalWorkPath + " ]");
        ensureNonexistence(baseLocalWorkPath);

        BuildImages.setMavenFeatures();
        BuildImages.setOLMavenFeatures();
        BuildImages.createRepository();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ensureNonexistence(baseLocalTmpPath);
        ensureNonexistence(baseLocalWorkPath);
    }

    //
    
    private String localBuildPath;

    public String getLocalBuildPath() {
        return localBuildPath;
    }

    private String localTmpPath;

    public String getLocalTmpPath() {
        return localTmpPath;
    }

    private String localWorkPath;

    public String getLocalWorkPath() {
        return localWorkPath;
    }

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(doRun);

        localBuildPath = baseLocalBuildPath + '/' + getTag();
        log("Local build [ " + localBuildPath + " ]");
        ensureNonexistence(localBuildPath);        

        localTmpPath = baseLocalTmpPath + '/' + getTag();
        log("Local temp [ " + localTmpPath + " ]");
        ensureNonexistence(localTmpPath);

        localWorkPath = baseLocalWorkPath + '/' + getTag();
        log("Local work [ " + localWorkPath + " ]");
        ensureNonexistence(localWorkPath);
    }

    //
    
    private String extractKernel() throws ZipException, IOException {
        String kernelPath = getLocalBuildPath() + "/kernel";
        log("Kernal [ " + kernelPath + " ]");
        return ImageType.KERNEL_ZIP.extract(kernelPath);
    }

    //

    @Test
    public void testFeatures() throws Exception {
        if ( !hasFeatures() ) {
            testRawFeatures();
        } else {
            testInstallFeatures();
        }
    }

    public void testRawFeatures() throws Exception {
        verify("Raw features [ " + getTag() + " ]", MAVEN_FEATURE_NAMES, GA_FEATURE_NAMES);
    }

    private void testInstallFeatures() throws Exception {
        log("Test feature installation [ " + getTag() + " ]:");

        List<String> installFeatures = getFeatures();
        for ( String feature : installFeatures ) {
            log("  [ " + feature + " ]");
        }

        String kernelPath = extractKernel();

        ServerInstallation serverInstallation = new ServerInstallation(kernelPath);

        List<String> resolvedFeatures =
            serverInstallation.resolveFeatures(OL_MAVEN_FEATURES, installFeatures);

        serverInstallation.installFeatures(
            installFeatures, getFeatureRepositoryPath(),
            getJavaHomePathAbs(), getLocalTmpPath(), getLocalWorkPath() );

        List<String> installedFeatures =
            serverInstallation.getInstalledFeatures();

        verify("Installed features [ " + getTag() + " ]", resolvedFeatures, installedFeatures);
    }
}   
