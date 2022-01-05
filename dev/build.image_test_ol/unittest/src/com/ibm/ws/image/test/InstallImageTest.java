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
package com.ibm.ws.image.test;

import static com.ibm.ws.image.test.topo.BuildProperties.CREATE_IM_REPO;
import static com.ibm.ws.image.test.topo.ServerImages.IGNORE_SUFFIX;
import static com.ibm.ws.image.test.topo.ServerImages.getImage;
import static com.ibm.ws.image.test.util.FileUtils.LOCAL_TMP_PATH_ABS;
import static com.ibm.ws.image.test.util.FileUtils.ensureNonexistence;
import static com.ibm.ws.image.test.util.FileUtils.load;
import static com.ibm.ws.image.test.util.FileUtils.selectMissing;
import static com.ibm.ws.image.test.util.ProcessRunner.runJar;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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

    // This test should not run if run.createImage is false.
    // This Ant property is passed in via the build-unittest.xml.
    // See that file for details on how these properties are exposed.
    
    private static boolean doRun;

    public static final String LOCAL_NAME = "installImage";
    public static String LOCAL_PATH;

    @BeforeClass
    public static void setUpClass() throws Exception {
        doRun = CREATE_IM_REPO;
        if ( doRun ) {
            log("Installation tests are enabled");
        } else {
            log("Installation tests are not enabled");
            return;
        }
        
        log("Common test directory [ " + LOCAL_TMP_PATH_ABS + " ]");

        LOCAL_PATH = LOCAL_TMP_PATH_ABS + '/' + LOCAL_NAME;
        log("Test directory [ " + LOCAL_PATH + " ]");
    }

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(doRun);
    }

    //

    @Test
    public void testInstallImage() throws Exception {
        String imagePrefix = getImagePrefix();
        log("Test image [ " + imagePrefix + " ]");

        String imagePath = getImage(imagePrefix, IGNORE_SUFFIX);
        log("Image [ " + imagePath + " ]");

        String installPath = LOCAL_PATH + '/' + getImageName();
        log("Installation [ " + installPath + " ]");

        ensureNonexistence(installPath);

        try {
            runJar(imagePath, "--acceptLicense", installPath);

            String wlpPath = installPath + "/wlp";
            File wlpRoot = new File(wlpPath);
            if ( !wlpRoot.exists() ) {
                fail("Installation [ " + wlpPath + " ] does not exist");
            }
            log("Installation [ " + wlpPath + " ] exists");

            String defaultTemplatePath = wlpPath + "/templates/servers/defaultServer/server.xml"; 
            File defaultTemplate = new File(defaultTemplatePath);
            if ( !defaultTemplate.exists() ) {
                fail("Default template [ " + defaultTemplatePath + " ] does not exist");
            }
            log("Default template [ " + defaultTemplatePath + " ] exists");

            String[] useRequiredElements = getRequiredElements();
            log("Required elements:");
            for ( String element : useRequiredElements ) {
                log("[ " + element + " ]");
            }

            List<String> defaultTemplateLines = load(defaultTemplate);
            List<String> missingElements = selectMissing(defaultTemplateLines, useRequiredElements);

            if ( (missingElements != null) && !missingElements.isEmpty() ) {
                log("Missing default template elements:");
                for ( String missing : missingElements ) {
                    log("[ " + missing + " ]");
                }
                fail("Missing default template elements");
            } else {
                log("Default template [ " + defaultTemplatePath + " ] is valid");
            }

        } finally {
            ensureNonexistence(installPath);
        }
    }
}