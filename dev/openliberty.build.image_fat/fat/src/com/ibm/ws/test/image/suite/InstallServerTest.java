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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.test.image.build.BuildImages;
import com.ibm.ws.test.image.build.BuildImages.RawImage;
import com.ibm.ws.test.image.installation.ServerInstallation;
import com.ibm.ws.test.image.util.FileUtils;

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

    public static final String BANNER =
            "============================================================";
    
    public static void logBeginning(String message) {
        log(message);
        log(BANNER);
    }
    
    public static void logEnding(String message) {
        log(BANNER);
        log(message);
    }

    //

    @Parameters
    public static Iterable<? extends Object> data() {
        return InstallServerData.TEST_DATA;
    }

    //
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        outputPath = TEST_OUTPUT_PATH_ABS + '/' + testName;

        log("Common output [ " + TEST_OUTPUT_PATH_ABS + " ]");
        log("Test name [ " + testName + " ]");
        log("Test output [ " + outputPath + " ]");
        
        logOutput();
    }

    public static long usableKB = FileUtils.NO_LAST_USABLE;

    protected static void logOutput() {
        usableKB = FileUtils.logOutput(usableKB);
    }

    //

    public InstallServerTest(
            BuildImages.ImageType imageType, 
            String[][] versionExpectations,
            String[] requiredTemplateElements,
            boolean supportsDefaultStartup,
            boolean supportsSchemaGen,
            String extraConsoleMessage) {

        this.imageType = imageType;
        this.installationPath =  outputPath + '/' + this.imageType.name();

        this.versionExpectations = versionExpectations;
        this.requiredTemplateElements = requiredTemplateElements;
        this.supportsDefaultStartup = supportsDefaultStartup;
        this.supportsSchemaGen = supportsSchemaGen;
        this.extraConsoleMessage = extraConsoleMessage;
    }

    private final BuildImages.ImageType imageType;
    protected final String installationPath;
    protected ServerInstallation installation;

    public BuildImages.ImageType getImageType() {
        return imageType;
    }

    public String getName() {
        return getImageType().name();
    }

    public String getInstallationPath() {
        return installationPath;
    }

    protected void setInstallation(ServerInstallation installation) {
        this.installation = installation;
    }

    public ServerInstallation getInstallation() {
        return installation;
    }

    private final String[][] versionExpectations;
    
    public String[][] getVersionExpectations() {
        return versionExpectations;
    }

    private final String[] requiredTemplateElements;

    public String[] getRequiredTemplateElements() {
        return requiredTemplateElements;
    }
    
    private final boolean supportsDefaultStartup;
    
    public boolean getSupportsDefaultStartup() {
        return supportsDefaultStartup;
    }
    
    private final boolean supportsSchemaGen;
    
    public boolean getSupportsSchemaGen() {
        return supportsSchemaGen;
    }

    private final String extraConsoleMessage;
    
    public String getExtraConsoleMessage() {
        return extraConsoleMessage;
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

    @Before
    public void setUp() throws Exception {
        logBeginning("Setting up installation");

        BuildImages.ImageType useImageType = getImageType();
        String useName = useImageType.name();
        String useDescription = useImageType.getDescription();
        String useInstallationPath = getInstallationPath();

        log("Install image [ " + useName + " ] [ " + useDescription + " ]");
        log("Install location [ " + useInstallationPath + " ]");

        RawImage rawImage = useImageType.ensureRawImage("extract");
        log("Image [ " + rawImage.getImagePath() + " : " + FileUtils.toKB(rawImage.getImageLength()) + "KB ]");

        log("Supports SchemaGen [ " + getSupportsSchemaGen() + " ]");
        log("Extra console message [ " + getExtraConsoleMessage() + " ]");

        log("Removing prior installation ...");
        logOutput();
        ensureNonexistence(useInstallationPath);
        logOutput();
        log("Removed prior installation");

        log("Installing ...");
        useImageType.extract(useInstallationPath);
        logOutput();
        log("Installed");

        setInstallation( new ServerInstallation(useName, useInstallationPath) );

        logEnding("Setup installation");
    }

    @After
    public void tearDown() throws Exception {
        logBeginning("Tearing down installation ...");

        BuildImages.ImageType useImageType = getImageType();        
        String useName = useImageType.name();
        String useDescription = useImageType.getDescription();
        String useInstallationPath = getInstallationPath();
        log("Image [ " + useName + " ] [ " + useDescription + " ]");
        log("Location [ " + useInstallationPath + " ]");

        logOutput();
        ensureNonexistence( getInstallationPath() );
        logOutput();

        logEnding("Tore down installation");        
    }

    //

    @Test
    public void validateDefaultTemplate() throws Exception {
        logBeginning("Validating default template ...");
        getInstallation().validateDefaultTemplate( getRequiredTemplateElements() );
        logEnding("Validated default template");
    }
    
    @Test
    public void validateScripts() throws Exception {
        logBeginning("Validating scripts ...");
        getInstallation().validateScripts();
        logEnding("Validated scripts");
    }
    
    @Test
    public void validateFeatures() throws Exception {
        logBeginning("Validating features ...");
        getInstallation().validateFeatures();
        logEnding("Validated features");
    }

    @Test
    public void validateDefaultServer() throws Exception {
        logBeginning("Validating default server ...");

        getInstallation().validateDefaultServer(
                getSupportsDefaultStartup(),
                getSupportsSchemaGen(),
                getExtraConsoleMessage() );

        logEnding("Validated default server");
    }
}
