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

import static com.ibm.ws.image.test.InstallBundlesData.BUNDLE_NAMES;
import static com.ibm.ws.image.test.InstallBundlesData.getBundleLocation;
import static com.ibm.ws.test.image.build.BuildProperties.IFIX_BUILD;
import static com.ibm.ws.test.image.suite.images.BuildProperties.BUILD_ALWAYS;
import static com.ibm.ws.test.image.suite.images.BuildProperties.BUILD_LICENSE_ZIP;
import static com.ibm.ws.test.image.suite.images.BuildProperties.CREATE_IM_REPO;
import static com.ibm.ws.test.image.suite.images.Images.getFeatureRepositoryPath;
import static com.ibm.ws.test.image.util.FileUtils.FLATTEN;
import static com.ibm.ws.test.image.util.FileUtils.TEST_OUTPUT_PATH_ABS;
import static com.ibm.ws.test.image.util.FileUtils.ensureNonexistence;
import static com.ibm.ws.test.image.util.FileUtils.extract;
import static com.ibm.ws.test.image.util.FileUtils.verifySpace;
import static com.ibm.ws.test.image.util.ProcessRunner.getJavaHomePathAbs;
import static com.ibm.ws.test.image.util.ProcessRunner.runJar;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstalledFeature;
import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.test.image.build.BuildImages;
import com.ibm.ws.test.image.build.BuildImages.ImageType;
import com.ibm.ws.test.image.installation.ServerInstallation;

@RunWith(Parameterized.class)
public class InstallBundlesTest {
    public static final String CLASS_NAME = InstallBundlesTest.class.getSimpleName();

    public static void log(String message) {
        System.out.println(message);
    }

    //

    public static String getTestName() {
        return "installBundles";
    }

    @Parameters
    public static Iterable<? extends Object> data() {
        return InstallBundlesData.TEST_DATA;
    }

    public InstallBundlesTest(String tag, ImageType license, ImageType content, List<String> bundles) {
        this.tag = tag;

        this.license = license;
        this.content = content;

        this.bundles = bundles;
    }

    private final String tag;

    public String getTag() {
        return tag;
    }

    private final ImageType license;
    
    public ImageType getLicense() {
        return license;
    }

    private final ImageType content;
    
    public ImageType getContent() {
        return content;
    }

    private final List<String> bundles;
    
    public List<String> getBundles() {
        return bundles;
    }

    //

    private static boolean doRun;

    private static String baseLocalBuildPath;
    private static String baseLocalTmpPath;
    private static String baseLocalWorkPath;

    @BeforeClass
    public static void setUpClass() throws Exception {
        doRun = ( !IFIX_BUILD && (CREATE_IM_REPO || BUILD_LICENSE_ZIP || BUILD_ALWAYS) );
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

        ImageType.ensureImageFiles();
        BuildImages.createRepository();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ensureNonexistence(baseLocalTmpPath);
        ensureNonexistence(baseLocalWorkPath);
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

        ImageType useKernel = ImageType.KERNEL_ZIP;
        useKernel.ensureImageFile();

        ImageType useLicense = getLicense();
        useLicense.ensureImageFile();

        ImageType useContent = getContent();
        useContent.ensureImageFile();

        long contentLength = useContent.getImageLength();
        verifySpace( contentLength * 2 );

        localRuntimePath = localBuildPath + "/runtime";
        localAllPath = localBuildPath + "/all";

        log("Runtime [ " + localRuntimePath + " ]");
        log("All [ " + localAllPath + " ]");
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

    private String localRuntimePath;
    
    public String getLocalRuntimePath() {
        return localRuntimePath;
    }
    
    private String localAllPath;
    
    public String getLocalAllPath() {
        return localAllPath;
    }
    
    //

    @Test
    public void testInstallBundles() throws Exception {
        log("Verifying installation [ " + getTag() + " ]");

        String kernelPath = ImageType.KERNEL_ZIP.extract( getLocalBuildPath() );

        ImageType useLicense = getLicense();
        ImageType useContent = getContent();

        ServerInstallation installation = new ServerInstallation(kernelPath);

        Properties coreProperties = new Properties();
        coreProperties.put("com.ibm.websphere.productEdition", "LIBERTY_CORE");

        installation.updateProperties(coreProperties);

        runJar( useLicense.getImagePath(), "--acceptLicense", kernelPath );

        installation.installBundles(
            getBundles(),
            getFeatureRepositoryPath(),
            getJavaHomePathAbs(), getLocalTmpPath(), getLocalWorkPath() );

        InstallKernel runtimeQuery = new InstallKernelImpl( new File(kernelPath, "wlp") );
        Map<String, InstalledFeature> runtimeFeatures = runtimeQuery.getInstalledFeatures();

        String kernelLibPath = kernelPath + "/wlp/lib";
        ensureNonexistence(kernelLibPath);

        String useAllPath = getLocalAllPath();

        extract( useContent.getImagePath(), useAllPath, ".mf", !FLATTEN );

        InstallKernel allQuery = new InstallKernelImpl( new File(useAllPath, "wlp") );
        Map<String, InstalledFeature> allFeatures = allQuery.getInstalledFeatures();

        validateFeatures(kernelPath, runtimeFeatures, useContent, useAllPath, allFeatures);
    }
    
    public void validateFeatures(
        String runtimePath, Map<String, InstalledFeature> runtimeFeatures,
        ImageType image,
        String allPath, Map<String, InstalledFeature> allFeatures) {

        log("Validating available features against runtime features");

        log("All [ " + allFeatures.size() + " ] in [ " + allPath + " ]"); 
        log("Image [ " + image.getImagePath() + " ]");        
        log("Runtime [ " + runtimeFeatures.size() + " ] in [ " + runtimePath + " ]"); 

        Set<String> allOnly = new HashSet<>();        
        for ( String allKey : allFeatures.keySet() ) {
            if ( !runtimeFeatures.containsKey(allKey) ) {
                allOnly.add(allKey);
            }
        }

        Set<String> runtimeOnly = new HashSet<>();        
        for ( String runtimeKey : runtimeFeatures.keySet() ) {
            if ( !allFeatures.containsKey(runtimeKey) ) {
                runtimeOnly.add(runtimeKey);
            }
        }

        if ( runtimeOnly.isEmpty() && allOnly.isEmpty() ) {
            log("Features were verified.");
            return;
        }

        log("Features were not verified!");

        if ( !allOnly.isEmpty() ) {
            log("Extra all features:");
            for ( String allKey : allOnly ) {
                log("[ " + allKey + " ]");
            }
        }        

        if ( !runtimeOnly.isEmpty() ) {
            log("Extra runtime features:");
            for ( String runtimeKey : runtimeOnly ) {
                log("[ " + runtimeKey + " ]");
            }
        }

        for ( String bundleName : BUNDLE_NAMES ) {
            log("Verify bundle [ " + bundleName + " ] at [ " + getBundleLocation(bundleName) + " ]");
        }
        log("Possible inconsistency between repo.xml and a bundle.feature file.");

        fail("Incorrect features in image [ " + image.getImagePath() + " ]");
    }
}
