/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;
import com.ibm.ws.logging.internal.BundleManifest;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.extension.ifix.xml.BundleFile;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.utils.TestUtils;

/**
 *
 */
public class IFixUtilsTest {
    final static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    // This is used to create a unique directory for each test. We sometimes get failures deleting files/dirs which can
    // cause the next test to fail.
    private static int testInstallDirCount = 0;
    // Some default File names that we'll use in the tests.
    private static final File tmpDir = new File("build/unittest/tmp");
    private static File wlpDir;
    private static File fixesDir;
    private static File libDir;
    private static File binDir;
    private static File featuresDir;
    private static File platformDir;
    private static File toolsDir;

    public static final ValidateCommandTask task = new ValidateCommandTask();
    private static CommandConsole console = new CommandConsole() {
        @Override
        public boolean isInputStreamAvailable() {
            return false;
        }

        @Override
        public String readMaskedText(String prompt) {
            return null;
        }

        @Override
        public String readText(String prompt) {
            return null;
        }

        @Override
        public void printInfoMessage(String message) {
            System.out.print(message);
        }

        @Override
        public void printlnInfoMessage(String message) {
            System.out.println(message);
        }

        @Override
        public void printErrorMessage(String errorMessage) {
            System.err.print(errorMessage);
        }

        @Override
        public void printlnErrorMessage(String errorMessage) {
            System.err.print(errorMessage);
        }
    };

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void classSetup() {}

    @Before
    public void testSetup() {

        // Initially clean up.
        TestUtils.delete(tmpDir);

        // We have to specify the absolute path in the wlpdir parent dir, because otherwise we get relative entries.
        wlpDir = new File(tmpDir.getAbsolutePath(), "Test" + testInstallDirCount++ + "/wlp");
        fixesDir = new File(wlpDir, "lib/fixes");
        libDir = new File(wlpDir, "lib");
        binDir = new File(wlpDir, "bin");
        featuresDir = new File(libDir, "features");
        platformDir = new File(libDir, "platform");
        toolsDir = new File(libDir, "tools-internal");
        // Configure all of the required directories for the tests.
        binDir.mkdirs();
        fixesDir.mkdirs();
        featuresDir.mkdirs();
        platformDir.mkdirs();
        toolsDir.mkdirs();

        Utils.setInstallDir(wlpDir);
    }

    @After
    public void teardown() {
        // We clear the Bundle Repositories to clear any locks on files that we need to remove.
        TestUtils.refreshBundleRepository();
        TestUtils.delete(tmpDir);
    }

    /**
     * This test ensures that when we have a number of ifix xmls, the files that are stored are the latest versions of the files.
     * 
     * @throws Exception
     */
    @Test
    public void testIfixFileCreationDateFiltering() throws Exception {

        // These are the files that are the ifix jars that have been installed into the runtime.
        File test1IFixJar = TestUtils.createJarFile(libDir, "test1", "1.0.0.20130101", true, true);
        File test2IFixJar = TestUtils.createJarFile(libDir, "test2", "1.0.0.20130101", true, false);
        File test3IFixJar = TestUtils.createJarFile(libDir, "test3", "1.0.0.20130101", true, true);
        File test4IFixJar = TestUtils.createJarFile(libDir, "test4", "1.0.0.20130101", true, false);

        // The relative paths that get stored in the xmls for each jar.
        String test1IFixJarRelativePath = TestUtils.createRelativePath(test1IFixJar, wlpDir);
        String test2IFixJarRelativePath = TestUtils.createRelativePath(test2IFixJar, wlpDir);
        String test3IFixJarRelativePath = TestUtils.createRelativePath(test3IFixJar, wlpDir);
        String test4IFixJarRelativePath = TestUtils.createRelativePath(test4IFixJar, wlpDir);

        // Create the static files
        StringBuffer fileContents = new StringBuffer();
        fileContents.append("This is the 1st line of file \n");

        // These are the files that are the ifix static files that have been installed into the runtime.
        File test1StaticFile = TestUtils.createFile(new File(binDir, "staticFile1"), fileContents);
        File test2StaticFile = TestUtils.createFile(new File(binDir, "staticFile2"), fileContents);
        File test3StaticFile = TestUtils.createFile(new File(binDir, "staticFile3"), fileContents);
        File test4StaticFile = TestUtils.createFile(new File(binDir, "staticFile4"), fileContents);

        // The relative paths that get stored in the xmls for each static file.
        String test1StaticFileRelativePath = TestUtils.createRelativePath(test1StaticFile, wlpDir);
        String test2StaticFileRelativePath = TestUtils.createRelativePath(test2StaticFile, wlpDir);
        String test3StaticFileRelativePath = TestUtils.createRelativePath(test3StaticFile, wlpDir);
        String test4StaticFileRelativePath = TestUtils.createRelativePath(test4StaticFile, wlpDir);

        // Create the first ifix xml. 
        Set<String> updateFiles = new HashSet<String>();
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixJar) + "\" size=\"" + test1IFixJar.length() + "\" id=\"" +
                        test1IFixJarRelativePath + "\"/>");
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test2IFixJar) + "\" size=\"" + test2IFixJar.length() + "\" id=\"" +
                        test2IFixJarRelativePath + "\"/>");
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test1StaticFile) + "\" size=\"" + test1StaticFile.length() + "\" id=\"" +
                        test1StaticFileRelativePath + "\"/>");
        TestUtils.createIfixXML(new File(fixesDir, "ifix1.xml"), "Test-Ifix1", updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        // Create the second ifix xml.
        updateFiles.clear();
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test3IFixJar) + "\" size=\"" + test3IFixJar.length() + "\" id=\"" +
                        test3IFixJarRelativePath + "\"/>");
        TestUtils.createIfixXML(new File(fixesDir, "ifix2.xml"), "Test-Ifix2", updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));
        // Create the third ifix xml.
        updateFiles.clear();
        updateFiles.add("<file date=\"2013-02-01 12:00\" hash=\"" + TestUtils.generateHash(test2IFixJar) + "\" size=\"" + test2IFixJar.length() + "\" id=\"" +
                        test2IFixJarRelativePath + "\"/>");
        updateFiles.add("<file date=\"2013-02-01 12:00\" hash=\"" + TestUtils.generateHash(test4IFixJar) + "\" size=\"" + test4IFixJar.length() + "\" id=\"" +
                        test4IFixJarRelativePath + "\"/>");
        updateFiles.add("<file date=\"2013-02-01 12:00\" hash=\"" + TestUtils.generateHash(test2StaticFile) + "\" size=\"" + test2StaticFile.length() + "\" id=\"" +
                        test2StaticFileRelativePath + "\"/>");
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test3StaticFile) + "\" size=\"" + test3StaticFile.length() + "\" id=\"" +
                        test3StaticFileRelativePath + "\"/>");
        TestUtils.createIfixXML(new File(fixesDir, "ifix3.xml"), "Test-Ifix3", updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        // Create the fourth ifix xml.
        updateFiles.clear();
        updateFiles.add("<file date=\"2013-02-01 12:00\" hash=\"" + TestUtils.generateHash(test4StaticFile) + "\" size=\"" + test4StaticFile.length() + "\" id=\"" +
                        test4StaticFileRelativePath + "\"/>");
        TestUtils.createIfixXML(new File(fixesDir, "ifix4.xml"), "Test-Ifix4", updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        // Create the fifth ifix xml.
        updateFiles.clear();
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test2StaticFile) + "\" size=\"" + test2StaticFile.length() + "\" id=\"" +
                        test2StaticFileRelativePath + "\"/>");
        updateFiles.add("<file date=\"2013-02-01 12:00\" hash=\"" + TestUtils.generateHash(test3IFixJar) + "\" size=\"" + test3IFixJar.length() + "\" id=\"" +
                        test3IFixJarRelativePath + "\"/>");
        TestUtils.createIfixXML(new File(fixesDir, "ifix5.xml"), "Test-Ifix5", updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        // Create the sixth ifix xml.
        updateFiles.clear();
        updateFiles.add("<file date=\"2013-01-01 12:00\" hash=\"" + TestUtils.generateHash(test4StaticFile) + "\" size=\"" + test4StaticFile.length() + "\" id=\"" +
                        test4StaticFileRelativePath + "\"/>");
        updateFiles.add("<file date=\"2013-02-01 12:00\" hash=\"" + TestUtils.generateHash(test1IFixJar) + "\" size=\"" + test1IFixJar.length() + "\" id=\"" +
                        test1IFixJarRelativePath + "\"/>");
        updateFiles.add("<file date=\"2013-03-01 12:00\" hash=\"" + TestUtils.generateHash(test2IFixJar) + "\" size=\"" + test2IFixJar.length() + "\" id=\"" +
                        test2IFixJarRelativePath + "\"/>");
        TestUtils.createIfixXML(new File(fixesDir, "ifix6.xml"), "Test-Ifix6", updateFiles, TestUtils.generateSet("Apar1", "Apar2", "Apar3"));

        // Now we have to reflectively allow the processIfixXmls method to be accessed and called, as it is a private method.
        Method processIFixXmlsMethod = IFixUtils.class.getDeclaredMethod("processIFixXmls", File.class, Map.class, CommandConsole.class);
        processIFixXmlsMethod.setAccessible(true);
        Method processLPMFXmlsMethod = IFixUtils.class.getDeclaredMethod("processLPMFXmls", File.class, CommandConsole.class);
        processLPMFXmlsMethod.setAccessible(true);
        Map<String, BundleFile> bundleFiles = (Map<String, BundleFile>) processLPMFXmlsMethod.invoke(null, wlpDir, console);
        Map<String, IFixInfo> latestFiles = (Map<String, IFixInfo>) processIFixXmlsMethod.invoke(null, wlpDir, bundleFiles, console);

        // Assert that we're getting the latest versions of the jars from the expected xml files.
        Assert.assertTrue("latest version of test1.1.0.0.20130101.jar should be in Test-Ifix6, but is in " + latestFiles.get(test1IFixJarRelativePath).getId(),
                          "Test-Ifix6".equals(latestFiles.get(test1IFixJarRelativePath).getId()));
        Assert.assertTrue("latest version of test2.1.0.0.20130101.jar should be in Test-Ifix6, but is in " + latestFiles.get(test2IFixJarRelativePath).getId(),
                          "Test-Ifix6".equals(latestFiles.get(test2IFixJarRelativePath).getId()));
        Assert.assertTrue("latest version of test3.1.0.0.20130101.jar should be in Test-Ifix5, but is in " + latestFiles.get(test3IFixJarRelativePath).getId(),
                          "Test-Ifix5".equals(latestFiles.get(test3IFixJarRelativePath).getId()));
        Assert.assertTrue("latest version of test4.1.0.0.20130101.jar should be in Test-Ifix3, but is in " + latestFiles.get(test4IFixJarRelativePath).getId(),
                          "Test-Ifix3".equals(latestFiles.get(test4IFixJarRelativePath).getId()));

        Assert.assertTrue("latest version of staticFile1 should be in Test-Ifix1, but is in " + latestFiles.get(test1StaticFileRelativePath).getId(),
                          "Test-Ifix1".equals(latestFiles.get(test1StaticFileRelativePath).getId()));
        Assert.assertTrue("latest version of staticFile2 should be in Test-Ifix3, but is in " + latestFiles.get(test2StaticFileRelativePath).getId(),
                          "Test-Ifix3".equals(latestFiles.get(test2StaticFileRelativePath).getId()));
        Assert.assertTrue("latest version of staticFile3 should be in Test-Ifix3, but is in " + latestFiles.get(test3StaticFileRelativePath).getId(),
                          "Test-Ifix3".equals(latestFiles.get(test3StaticFileRelativePath).getId()));
        Assert.assertTrue("latest version of staticFile4 should be in Test-Ifix4, but is in " + latestFiles.get(test4StaticFileRelativePath).getId(),
                          "Test-Ifix4".equals(latestFiles.get(test4StaticFileRelativePath).getId()));

    }

    /**
     * This method tests the gathering of static content from different feature manifest Subsystem content headers.
     * It tests a number of different types/styles of location and ensures that only the static content files are returned.
     * 
     * @throws Exception
     */
    @Test
    public void testSubsystemContentStaticFiles() throws Throwable {

        Map<String, ProvisioningFeatureDefinition> features = new TreeMap<String, ProvisioningFeatureDefinition>();

        // Configure the files we're going to check for.
        File static1Bat = TestUtils.createFile(new File(binDir, "static1.bat"), new StringBuffer());
        File static2Bat = TestUtils.createFile(new File(binDir, "static2.bat"), new StringBuffer());
        File static2 = TestUtils.createFile(new File(binDir, "static2"), new StringBuffer());
        File static3Bat = TestUtils.createFile(new File(binDir, "static3.bat"), new StringBuffer());
        File staticTools3Bat = TestUtils.createFile(new File(toolsDir, "static3.bat"), new StringBuffer());
        File static4Bat = TestUtils.createFile(new File(binDir, "static4.bat"), new StringBuffer());
        File staticTool4Bat = TestUtils.createFile(new File(toolsDir, "static4.bat"), new StringBuffer());
        File static4 = TestUtils.createFile(new File(binDir, "static4"), new StringBuffer());
        File staticTools4 = TestUtils.createFile(new File(toolsDir, "static4"), new StringBuffer());
        File static5Bat = TestUtils.createFile(new File(binDir, "static5.bat"), new StringBuffer());
        File staticTools5Bat = TestUtils.createFile(new File(toolsDir, "static5.bat"), new StringBuffer());
        // We'll create this file, but it shouldn't be included in the static content.
        File nonStatic5 = TestUtils.createFile(new File(toolsDir, "nonstatic5"), new StringBuffer());
        File test1Jar = TestUtils.createJarFile(libDir, "test1", "1.0.0", false, false);
        File test1IfixJar = TestUtils.createJarFile(libDir, "test1", "1.0.0.20130101", true, true);
        File test2IfixJar = TestUtils.createJarFile(libDir, "test2", "1.0.0.20130101", true, false);

        // Create the 1st feature
        Map<String, String> feature1Files = new HashMap<String, String>();
        feature1Files.put("bin/static1.bat", "static1.bat");
        File feature1 = TestUtils.createFeature(new File(featuresDir, "testFeature1-1.0.mf"), "testFeature1-1.0", feature1Files);

        // Once we've created the manifest, create a SubsystemFeatureDef object and then load that into the list of features that we 
        // pass into the processSubsystemContent method
        ProvisioningFeatureDefinition fd = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, feature1);
        features.put(fd.getSymbolicName(), fd);

        // Create the 2nd feature
        Map<String, String> feature2Files = new HashMap<String, String>();
        feature2Files.put("bin/static2.bat", "static2.bat");
        feature2Files.put("bin/static2", "static2");
        File feature2 = TestUtils.createFeature(new File(featuresDir, "testFeature2-1.0.mf"), "testFeature2-1.0", feature2Files);
        ProvisioningFeatureDefinition fd2 = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, feature2);
        features.put(fd2.getSymbolicName(), fd2);

        // Create the 2nd feature
        Map<String, String> feature3Files = new HashMap<String, String>();
        feature3Files.put("bin/static3.bat", "static3.bat");
        feature3Files.put("lib/tools-internal/static3.bat", "static3.bat.internal");
        File feature3 = TestUtils.createFeature(new File(featuresDir, "testFeature3-1.0.mf"), "testFeature3-1.0", feature3Files);
        ProvisioningFeatureDefinition fd3 = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, feature3);
        features.put(fd3.getSymbolicName(), fd3);

        // Feature 4 is put in the lib/platform dir like the kernel features
        Map<String, String> feature4Files = new HashMap<String, String>();
        feature4Files.put("bin/static4.bat", "static4.bat");
        feature4Files.put("lib/tools-internal/static4.bat", "static4.bat.internal");
        feature4Files.put("bin/static4", "static4");
        feature4Files.put("lib/tools-internal/static4", "static4.internal");
        feature4Files.put("lib/test1_1.0.0.jar", "test1_1.0.0.jar");
        File feature4 = TestUtils.createFeature(new File(platformDir, "testFeature4-1.0.mf"), "testFeature4-1.0", feature4Files);
        ProvisioningFeatureDefinition fd4 = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, feature4);
        features.put(fd4.getSymbolicName(), fd4);

        // This feature has a some static content and some non-static content.
        Map<String, String> feature5Files = new HashMap<String, String>();
        feature5Files.put("bin/static5.bat", "static5.bat");
        feature5Files.put("lib/tools-internal/static5.bat", "static5.bat.internal");
        feature5Files.put("lib/features, lib/tools-internal", "nonstatic5.jar");
        File feature5 = TestUtils.createFeature(new File(platformDir, "testFeature5-1.0.mf"), "testFeature5-1.0", feature5Files);
        ProvisioningFeatureDefinition fd5 = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, feature5);
        features.put(fd5.getSymbolicName(), fd5);

        // Remove any existing and recreate the bundle repository that will work out the latest versions of the jars.
        ContentBasedLocalBundleRepository repo = TestUtils.refreshBundleRepository();

        // Now we have to reflectively allow the processIfixXmls method to be accessed and called, as it is a private method.
        Method processSubsysteContentMethod = IFixUtils.class.getDeclaredMethod("processSubsystemContent", File.class, Map.class,
                                                                                ContentBasedLocalBundleRepository.class, Map.class,
                                                                                Set.class, Set.class, CommandConsole.class);
        processSubsysteContentMethod.setAccessible(true);

        Map<File, BundleManifest> allBaseBundleJarContent = new HashMap<File, BundleManifest>();
        Set<File> allBundleJarContent = new HashSet<File>();
        Set<File> allStaticFileContent = new HashSet<File>();
        processSubsysteContentMethod.invoke(null, wlpDir, features, repo, allBaseBundleJarContent, allBundleJarContent, allStaticFileContent, console);

        File[] expectedFiles = { static1Bat, static2Bat, static2, static3Bat, staticTools3Bat, static4Bat, static4, staticTools4, staticTool4Bat,
                                static5Bat, staticTools5Bat, feature1, feature2, feature3, feature4, feature5 };

        File[] unExpectedFiles = { nonStatic5, test1Jar, test1IfixJar, test2IfixJar };

        // Iterate over the expected files making sure they exist, and then over the unexpected file ensuring that they don't.
        for (File chkFile : expectedFiles) {
            Assert.assertTrue(chkFile.getAbsolutePath() + " not found in static content list", allStaticFileContent.contains(chkFile));
        }

        for (File chkFile : unExpectedFiles) {
            Assert.assertFalse(chkFile.getAbsolutePath() + " was unexpectedly found in static content list", allStaticFileContent.contains(chkFile));
        }
    }

    /**
     * This method tests the gathering of Bundle and jar information from different feature manifest Subsystem content headers.
     * It tests a number of different types/styles of location and ensures that only the jar content files are returned.
     * 
     * @throws Exception
     */
    @Test
    public void testSubsystemContentBundleJarFiles() throws Throwable {

        Map<String, ProvisioningFeatureDefinition> features = new TreeMap<String, ProvisioningFeatureDefinition>();

        File test1Jar = TestUtils.createJarFile(libDir, "test1", "1.0.0", false, false);
        File test2Jar = TestUtils.createJarFile(libDir, "test2", "1.0.1", false, false);

        File test1staticJar = TestUtils.createJarFile(libDir, "test1static", "1.0.0", false, false);
        File test2staticJar = TestUtils.createJarFile(libDir, "test2static", "1.0.1", false, false);

        File test1staticfile = TestUtils.createFile(new File(binDir, "static1.bat"), new StringBuffer());
        File test2staticfile = TestUtils.createFile(new File(binDir, "static2"), new StringBuffer());
        File test3staticfile = TestUtils.createFile(new File(toolsDir, "static5.bat"), new StringBuffer());

        // Create the 1st feature
        Map<String, String> feature1Files = new HashMap<String, String>();
        feature1Files.put(null, "test1_1.0.0.jar");
        feature1Files.put("bin/static2.bat", "static2.bat");
        feature1Files.put("lib/test1static_1.0.0.jar", "test1static_1.0.0.jar");
        File feature1 = TestUtils.createFeature(new File(featuresDir, "testFeature1-1.0.mf"), "testFeature1-1.0", feature1Files);

        // Once we've created the manifest, create a SubsystemFeatureDef object and then load that into the list of features that we 
        // pass into the processSubsystemContent method
        ProvisioningFeatureDefinition fd = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, feature1);
        features.put(fd.getSymbolicName(), fd);

        // Create the 1st feature
        Map<String, String> feature2Files = new HashMap<String, String>();
        feature2Files.put(null, "test2_1.0.1.jar");
        feature2Files.put("bin/static2", "static2");
        feature2Files.put("lib/test2static_1.0.1.jar", "test1static_1.0.1.jar");
        File feature2 = TestUtils.createFeature(new File(featuresDir, "testFeature2-1.0.mf"), "testFeature2-1.0", feature2Files);

        ProvisioningFeatureDefinition fd2 = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, feature2);
        features.put(fd2.getSymbolicName(), fd2);

        // Create the 1st feature
        Map<String, String> feature3Files = new HashMap<String, String>();
        feature3Files.put("lib/tools-internal/static5.bat", "static5.bat");
        File feature3 = TestUtils.createFeature(new File(featuresDir, "testFeature3-1.0.mf"), "testFeature3-1.0", feature3Files);

        ProvisioningFeatureDefinition fd3 = new SubsystemFeatureDefinitionImpl(ExtensionConstants.CORE_EXTENSION, feature3);
        features.put(fd3.getSymbolicName(), fd3);

        // Remove any existing and recreate the bundle repository that will work out the latest versions of the jars.
        ContentBasedLocalBundleRepository repo = TestUtils.refreshBundleRepository();

        // Now we have to reflectively allow the processIfixXmls method to be accessed and called, as it is a private method.
        Method processSubsysteContentMethod = IFixUtils.class.getDeclaredMethod("processSubsystemContent", File.class, Map.class,
                                                                                ContentBasedLocalBundleRepository.class, Map.class,
                                                                                Set.class, Set.class, CommandConsole.class);
        processSubsysteContentMethod.setAccessible(true);

        Map<File, BundleManifest> allBaseBundleJarContent = new HashMap<File, BundleManifest>();
        Set<File> allBundleJarContent = new HashSet<File>();
        Set<File> allStaticFileContent = new HashSet<File>();
        processSubsysteContentMethod.invoke(null, wlpDir, features, repo, allBaseBundleJarContent, allBundleJarContent, allStaticFileContent, console);

        File[] expectedFiles = { test1Jar, test2Jar, test1staticJar, test2staticJar };

        File[] unExpectedFiles = { test1staticfile, test2staticfile, test3staticfile, feature1, feature2, feature3 };

        // Iterate over the expected files making sure they exist, and then over the unexpected file ensuring that they don't.
        for (File chkFile : expectedFiles) {
            Assert.assertTrue(chkFile.getAbsolutePath() + " not found in jar content list", allBundleJarContent.contains(chkFile));
        }

        for (File chkFile : unExpectedFiles) {
            Assert.assertFalse(chkFile.getAbsolutePath() + " was unexpectedly found in static content list", allBundleJarContent.contains(chkFile));
        }
    }

}
