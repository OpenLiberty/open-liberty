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
package com.ibm.ws.test.image.suite;

import static com.ibm.ws.test.image.util.FileUtils.TEST_OUTPUT_PATH_ABS;
import static com.ibm.ws.test.image.util.FileUtils.ensureNonexistence;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.test.image.build.BuildImages;
import com.ibm.ws.test.image.installation.ServerInstallation;

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
public class InstallServerTest {
    public static final String CLASS_NAME = InstallServerTest.class.getSimpleName();
    
    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }

    //

    @Parameters
    public static Iterable<? extends Object> data() {
        return InstallServerData.TEST_DATA;
    }

    //

    public InstallServerTest(
            BuildImages.ImageType imageType, 
            String[][] versionExpectations,
            String[] requiredTemplateElements) {
        
        this.imageType = imageType;

        this.versionExpectations = versionExpectations;
        this.requiredTemplateElements = requiredTemplateElements;
    }

    private final BuildImages.ImageType imageType;
    
    public BuildImages.ImageType getImageType() {
        return imageType;
    }

    private final String[][] versionExpectations;
    
    public String[][] getVersionExpectations() {
        return versionExpectations;
    }

    private final String[] requiredTemplateElements;

    public String[] getRequiredTemplateElements() {
        return requiredTemplateElements;
    }

    //

    public static final String testName = "iServer";

    public static String getTestName() {
        return testName;
    }
    
    public static String outputPath;

    public static String getOutputPath() {
        return outputPath;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        outputPath = TEST_OUTPUT_PATH_ABS + '/' + testName;

        log("Common output [ " + TEST_OUTPUT_PATH_ABS + " ]");
        log("Test name [ " + testName + " ]");
        log("Test output [ " + outputPath + " ]");
    }

    @Before
    public void setUp() throws Exception {
        // Empty
    }

    //

    @Test
    public void testInstallImage() throws Exception {
        BuildImages.ImageType useImageType = getImageType();
        String useName = useImageType.name();
        String installationPath = outputPath + '/' + useName;
        String useDescription = useImageType.getDescription();

        log("Test installation [ " + useName + " ] [ " + useDescription + " ]");
        log("Image [ " + useImageType.getRawImage().getImagePath() + " ]");
        log("Installation [ " + installationPath + " ]");

        ensureNonexistence(installationPath);

        try {
            useImageType.extract(installationPath);

            ServerInstallation installation =
                new ServerInstallation(useName, installationPath);

            installation.validateDefaultTemplate( getRequiredTemplateElements() );
            installation.validateScripts();
            installation.validateDefaultInstance();

        } finally {
            ensureNonexistence(installationPath);
        }
    }
}
