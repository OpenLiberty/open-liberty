/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.suite.staging;

import static com.ibm.ws.test.image.build.BuildProperties.GA_VERSION;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.test.image.build.BuildImages;
import com.ibm.ws.test.image.installation.ServerInstallation;
import com.ibm.ws.test.image.util.FileUtils;

@RunWith(Parameterized.class)
public class InstallProfileTest {
    public static final String CLASS_NAME =
        InstallProfileTest.class.getSimpleName();

    private static void log(String msg) {
        System.out.println(msg);
    }

    //

    @Parameters
    public static Iterable<? extends Object> data() {
        return InstallProfileData.TEST_DATA;
    }

    public static final boolean DO_SCHEMA_TEST = true;
    
    public InstallProfileTest(String tag, String prefix, String suffix, Boolean doSchemaTest) {
        this.tag = tag;
        this.prefix = prefix;
        this.suffix = suffix;
        this.doSchemaTest = doSchemaTest.booleanValue();
    }

    private final String tag;
    
    public String getTag() {
        return tag;
    }

    private final String prefix;
    
    public String getPrefix() {
        return prefix;
    }
    
    private final String suffix;
    
    public String getSuffix() {
        return suffix;
    }    

    private final boolean doSchemaTest;

    public boolean getDoSchemaTest() {
        return doSchemaTest;
    }

    //

    private static boolean doRun;

    @BeforeClass
    public static void setUpClass() throws Exception {
        doRun = Boolean.getBoolean("run.createImage");
        if ( doRun ) {
            log("Installation tests are enabled");
        } else {
            log("Installation tests are not enabled");
            return;
        }
    }

    //

    private String baseInstallationPath;
    
    public String getBaseInstallationPath() {
        return baseInstallationPath;
    }
    
    public BuildImages.RawImages rawImages;
    
    public BuildImages.RawImages getRawImages() {
        return rawImages;
    }
    
    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(doRun);

        baseInstallationPath = FileUtils.TEST_OUTPUT_PATH_ABS + "/profile_" + getTag();
        rawImages = BuildImages.getRawImages( getPrefix(), getSuffix() );
    }

    //

    @Test
    public void testProfiles() throws Exception {
        log("Validating profile installations [ " + getPrefix() + "*" + getSuffix() + " ]");
        
        String useBaseInstallationPath = getBaseInstallationPath();
        log("Base installation path [ " + useBaseInstallationPath + " ]");
        
        boolean useDoSchemaTest = getDoSchemaTest();
        if ( useDoSchemaTest ) {
            log("The server configuration will be validated");
        } else {
            log("The server configuration will not be validated");
        }

        int imageNo = 0;
        for ( String imagePath : getRawImages().getImagePaths() ) {
            testProfile(imageNo++, imagePath);
        }
    }
    
    protected void testProfile(int imageNo, String imagePath) throws Exception {
        String installationPath = getBaseInstallationPath() + Integer.toString(imageNo);

        ServerInstallation installation = BuildImages.install(imagePath, installationPath);

        installation.validate( GA_VERSION, getDoSchemaTest() );
    }
}
