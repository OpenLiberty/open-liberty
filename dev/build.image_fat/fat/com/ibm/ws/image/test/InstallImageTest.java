/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.image.test;

import static com.ibm.ws.image.test.topo.BuildProperties.CREATE_IM_REPO;
import static com.ibm.ws.image.test.util.FileUtils.TEST_OUTPUT_PATH_ABS;
import static com.ibm.ws.image.test.util.FileUtils.ensureNonexistence;
import static com.ibm.ws.image.test.topo.ServerImages.getRawImage;
import static com.ibm.ws.image.test.topo.ServerImages.RawImage;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.image.test.topo.ServerInstallation;

/**
 * Basic installation tests.
 *
 * This does a JAR based installation of a target install image.
 *
 * Tests have three parameters: A prefix, a name, and a list of
 * required default configuration elements.
 *
 * The test name is descriptive and is used only for logging.
 *
 * The test prefix is used to select the installation image.
 * The required default configuration elements are a list of
 * configuration elements which are required to be present in
 * the default configuration of the installed image.
 *
 * These tests are not run unless {@link #CREATE_IM_REPO} is set.
 * Usually, this is set by system property {@link #REATE_IM_REPO_PROPERTY_NAME}.
 */
@RunWith(Parameterized.class)
public class InstallImageTest {
    public static final String CLASS_NAME = InstallImageTest.class.getSimpleName();
    
    public static void log(String message) {
        System.out.println(message);
    }

    //

    @Parameters
    public static Iterable<? extends Object> data() {
        return InstallImageData.TEST_DATA;
    }

    //

    public InstallImageTest(
        String imagePrefix, String imageName,
        String[] requiredElements) {

        this.imagePrefix = imagePrefix;
        this.imageName = imageName;
        this.requiredElements = requiredElements;
    }

    private final String imagePrefix;
    
    public String getImagePrefix() {
        return imagePrefix;
    }

    private final String imageName;

    public String getImageName() {
        return imageName;
    }    

    private final String[] requiredElements;

    public String[] getRequiredElements() {
        return requiredElements;
    }

    //

    private static boolean doRun;

    public static final String testName = "installImageTest";
    public static String outputPath;

    @BeforeClass
    public static void setUpClass() throws Exception {
        doRun = CREATE_IM_REPO;
        if ( doRun ) {
            log("Installation tests are enabled");
        } else {
            log("Installation tests are not enabled");
            return;
        }

        log("Common test directory [ " + TEST_OUTPUT_PATH_ABS + " ]");

        outputPath = TEST_OUTPUT_PATH_ABS + '/' + testName;
        log("Test directory [ " + outputPath + " ]");
    }

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(doRun);
    }

    //

    @Test
    public void testInstallImage() throws Exception {
        String useImageName = getImageName();
        String useImagePrefix = getImagePrefix();
        log("Test installation of image [ " + useImagePrefix + " ] [ " + useImageName + " ]");

        RawImage rawImage = getRawImage(useImagePrefix);
        log("Image path [ " + rawImage.getImagePath() + " ]");

        String installationPath = outputPath + '/' + getImageName();
        log("Installation path [ " + installationPath + " ]");

        ensureNonexistence(installationPath);

        try {
            ServerInstallation installation = rawImage.installFromJar(installationPath);
            installation.verifyRequiredElements( getRequiredElements() );

        } finally {
            ensureNonexistence(installationPath);
        }
    }
}