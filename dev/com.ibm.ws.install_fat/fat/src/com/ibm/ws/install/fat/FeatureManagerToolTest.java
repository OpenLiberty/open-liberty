/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.internal.InstallUtils;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServerFactory;
import test.utils.TestUtils;

/**
 *
 */
public class FeatureManagerToolTest extends ToolTest {

    private static final Class<?> c = FeatureManagerToolTest.class;

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        setupEnv(LibertyServerFactory.getLibertyServer("com.ibm.ws.install_fat"));
    }

    @AfterClass
    public static void cleanup() throws Exception {
        final String METHOD_NAME = "cleanup";
        Log.entering(c, METHOD_NAME);
        server.deleteFileFromLibertyInstallRoot("lib/features/com.ibm.genericCoreFeature.mf");
        server.deleteFileFromLibertyInstallRoot("lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf");
        server.deleteFileFromLibertyInstallRoot("lib/features/com.ibm.genericCoreFeatureH.mf");
        server.deleteFileFromLibertyInstallRoot("lib/features/com.ibm.genericCoreFeatureA.mf");
        server.deleteFileFromLibertyInstallRoot("lib/features/l10n/com.ibm.genericCoreFeature.properties");
        server.deleteFileFromLibertyInstallRoot("lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties");
        server.deleteFileFromLibertyInstallRoot("lib/features/l10n/com.ibm.genericCoreFeatureH.properties");
        server.deleteFileFromLibertyInstallRoot("lib/features/l10n/com.ibm.genericCoreFeatureA.properties");
        server.deleteFileFromLibertyInstallRoot("lib/features/checksums/com.ibm.genericCoreFeature.cs");
        server.deleteFileFromLibertyInstallRoot("lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs");
        server.deleteFileFromLibertyInstallRoot("lib/features/checksums/com.ibm.genericCoreFeatureH.cs");
        server.deleteFileFromLibertyInstallRoot("lib/features/checksums/com.ibm.genericCoreFeatureA.cs");
        server.deleteFileFromLibertyInstallRoot("lib/com.ibm.genericCoreFeature_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/com.ibm.genericCoreFeatureDependancyOnEsaPass_1.0.0.jar");
        server.deleteFileFromLibertyInstallRoot("lib/com.ibm.genericCoreFeatureH.jar");
        server.deleteFileFromLibertyInstallRoot("lib/com.ibm.genericCoreFeatureA.jar");
        server.deleteDirectoryFromLibertyInstallRoot("lafiles/com.ibm.genericCoreFeature");
        server.deleteDirectoryFromLibertyInstallRoot("lafiles/com.ibm.genericCoreFeatureDependancyOnEsaPass");
        server.deleteDirectoryFromLibertyInstallRoot("lafiles/com.ibm.genericCoreFeatureH");
        server.deleteDirectoryFromLibertyInstallRoot("lafiles/com.ibm.genericCoreFeatureA");
        server.deleteDirectoryFromLibertyInstallRoot("usr/extension");
        server.deleteDirectoryFromLibertyInstallRoot("usr/download");
        server.deleteDirectoryFromLibertyInstallRoot("etc/extensions");
        server.deleteDirectoryFromLibertyInstallRoot("../cik");
        ToolTest.cleanup();
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Basic feature install and uninstall
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testFeatureInstallUninstall() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeatureInstallUninstall";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "install", "webCacheMonitor-1.0", "--acceptLicense", "--when-file-exists=replace" };

        String[] fileLists = { "lib/features/com.ibm.websphere.appserver.webCacheMonitor-1.0.mf" };
        deleteFiles(METHOD_NAME, "webCacheMonitor-1.0", fileLists);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, param1s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertFalse("stabilized", po.getStdout().indexOf("stabilized") >= 0);

        // Test feature uninstall
        String[] param2s = { "uninstall", "webCacheMonitor-1.0", "--noPrompts" };
        po = runFeatureManager(METHOD_NAME, param2s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("One or more features uninstalled successfully: webCacheMonitor-1.0",
                   po.getStdout().indexOf("One or more features uninstalled successfully: webCacheMonitor-1.0") >= 0);

        deleteFiles(METHOD_NAME, "webCacheMonitor-1.0", fileLists);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test multiple features install using comma and space separator
     *
     * @throws Exception
     */
    @Test
    public void testMultipleFeaturesInstallUsingMixedSeparator() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testMultipleFeaturesInstallUsingMixedSeparator";
        Log.entering(c, METHOD_NAME);
        //testesa3 depends on testesa1
        String[] params = { "install", "testesa1,testesa2", "testesa3", "--acceptLicense", "--when-file-exists=replace" };

        String[] testesa1FilesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat", "usr/extension/lib/testesa1_1.0.0.jar" };
        String[] testesa2and3FilesList = { "usr/extension/lib/features/testesa2.mf", "usr/extension/lib/features/testesa3.mf",
                                           "usr/extension/bin/testesa2.bat", "usr/extension/bin/testesa3.bat" };
        String[] allFilesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/lib/features/testesa2.mf", "usr/extension/lib/features/testesa3.mf",
                                  "usr/extension/bin/testesa1.bat", "usr/extension/bin/testesa2.bat", "usr/extension/bin/testesa3.bat",

                                  "usr/extension/lib/testesa1_1.0.0.jar", "lib/features/testesa1.mf", "bin/testesa1.bat", "lib/testesa1_1.0.0.jar",
                                  "usr/cik/extensions/cik.ext.product1/lib/features/testesa1.mf",
                                  "usr/cik/extensions/cik.ext.product1/bin/testesa1.bat", "usr/cik/extensions/cik.ext.product1/lib/testesa1_1.0.0.jar",

                                  "usr/extension/lib/testesa2_1.0.0.jar", "lib/features/testesa2.mf", "bin/testesa2.bat", "lib/testesa2_1.0.0.jar",
                                  "usr/cik/extensions/cik.ext.product1/lib/features/testesa2.mf",
                                  "usr/cik/extensions/cik.ext.product1/bin/testesa2.bat", "usr/cik/extensions/cik.ext.product1/lib/testesa2_1.0.0.jar" };
        deleteFiles(METHOD_NAME, "testesa1, testesa2 and testesa3", allFilesList);

        ProgramOutput po;

        try {
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(testesa1FilesList);
            assertFilesExist(testesa2and3FilesList);

            po = runFeatureManager(METHOD_NAME, new String[] { "uninstall", "testesa2,testesa3", "--noPrompts" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("The features should successfully uninstalled but not",
                       po.getStdout().indexOf("One or more features uninstalled successfully: testesa") >= 0);
            assertFilesExist(testesa1FilesList);
            assertFilesNotExist(testesa2and3FilesList);
        } finally {
            deleteFiles(METHOD_NAME, "testesa1, testesa2 and testesa3", allFilesList);
            Log.exiting(c, METHOD_NAME);
        }
    }

    /**
     * Test multiple features install using space separator
     *
     * @throws Exception
     */
    @Test
    public void testMultipleFeaturesInstallUsingSpaceSeparator() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testMultipleFeaturesInstallUsingSpaceSeparator";
        Log.entering(c, METHOD_NAME);
        //testesa3 depends on testesa1
        String[] params = { "install", "testesa2", "testesa3", "--acceptLicense", "--when-file-exists=replace" };

        String[] testesa1FilesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat", "usr/extension/lib/testesa1_1.0.0.jar" };
        String[] testesa2and3FilesList = { "usr/extension/lib/features/testesa2.mf", "usr/extension/lib/features/testesa3.mf",
                                           "usr/extension/bin/testesa2.bat", "usr/extension/bin/testesa3.bat" };
        String[] allFilesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/lib/features/testesa2.mf", "usr/extension/lib/features/testesa3.mf",
                                  "usr/extension/bin/testesa1.bat", "usr/extension/bin/testesa2.bat", "usr/extension/bin/testesa3.bat",

                                  "usr/extension/lib/testesa1_1.0.0.jar", "lib/features/testesa1.mf", "bin/testesa1.bat", "lib/testesa1_1.0.0.jar",
                                  "usr/cik/extensions/cik.ext.product1/lib/features/testesa1.mf",
                                  "usr/cik/extensions/cik.ext.product1/bin/testesa1.bat", "usr/cik/extensions/cik.ext.product1/lib/testesa1_1.0.0.jar",

                                  "usr/extension/lib/testesa2_1.0.0.jar", "lib/features/testesa2.mf", "bin/testesa2.bat", "lib/testesa2_1.0.0.jar",
                                  "usr/cik/extensions/cik.ext.product1/lib/features/testesa2.mf",
                                  "usr/cik/extensions/cik.ext.product1/bin/testesa2.bat", "usr/cik/extensions/cik.ext.product1/lib/testesa2_1.0.0.jar" };
        deleteFiles(METHOD_NAME, "testesa2 and  testesa3", allFilesList);

        ProgramOutput po;
        try {
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(testesa1FilesList);
            assertFilesExist(testesa2and3FilesList);

            po = runFeatureManager(METHOD_NAME, new String[] { "uninstall", "testesa2", "testesa3", "--noPrompts" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("The features should successfully uninstalled but not",
                       po.getStdout().indexOf("One or more features uninstalled successfully: testesa") >= 0);
            assertFilesExist(testesa1FilesList);
            assertFilesNotExist(testesa2and3FilesList);
        } finally {
            deleteFiles(METHOD_NAME, "testesa2 and testesa3", allFilesList);
            Log.exiting(c, METHOD_NAME);
        }
    }

    /**
     * Test multiple features install
     *
     * @throws Exception
     */
    @Test
    public void testMultipleFeaturesInstall() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testMultipleFeaturesInstall";
        Log.entering(c, METHOD_NAME);
        String[] params = { "install", "testesa1,testesa3", "--acceptLicense" };

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/lib/features/testesa3.mf",
                               "usr/extension/bin/testesa1.bat", "usr/extension/bin/testesa3.bat",
                               "usr/extension/lib/testesa1_1.0.0.jar" };
        String[] filesList2 = { "usr/extension/lib/features/testesa1.mf", "usr/extension/lib/features/testesa3.mf",
                                "usr/extension/bin/testesa1.bat", "usr/extension/bin/testesa3.bat",
                                "usr/extension/lib/testesa1_1.0.0.jar", "lib/features/testesa1.mf", "bin/testesa1.bat", "lib/testesa1_1.0.0.jar",
                                "usr/cik/extensions/cik.ext.product1/lib/features/testesa1.mf",
                                "usr/cik/extensions/cik.ext.product1/bin/testesa1.bat", "usr/cik/extensions/cik.ext.product1/lib/testesa1_1.0.0.jar"
        };
        deleteFiles(METHOD_NAME, "testesa1 and testesa3", filesList2);

        ProgramOutput po;
        try {
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(filesList);

            po = runFeatureManager(METHOD_NAME, new String[] { "uninstall", "testesa1,testesa3", "--noPrompts" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("The features should successfully uninstalled but not",
                       po.getStdout().indexOf("One or more features uninstalled successfully: testesa") >= 0);
            assertFilesNotExist(filesList);
        } finally {
            deleteFiles(METHOD_NAME, "testesa1 and testesa3", filesList);;
            Log.exiting(c, METHOD_NAME);
        }
    }

    /**
     * Uninstall multiple features using space separator
     *
     * @throws Exception
     */
    @Test
    public void testMultipleFeaturesInstallUinstallUsingSpaceSeparator() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testMultipleFeaturesInstallUinstallUsingSpaceSeparator";
        Log.entering(c, METHOD_NAME);
        String[] install_params = { "install", "com.ibm.genericCoreFeatureA", "com.ibm.genericCoreFeatureB", "--acceptLicense", "--when-file-exists=replace" };
        String[] uninstall_params = { "uninstall", "com.ibm.genericCoreFeatureA", "com.ibm.genericCoreFeatureB", "--noPrompts" };

        String[] filesList = { "lib/features/com.ibm.genericCoreFeatureA.mf", "lib/features/genericCoreFeatureB.mf" };

        //test features install
        deleteFiles(METHOD_NAME, "genericCoreFeatureA and genericCoreFeatureB", filesList);

        ProgramOutput po;

        po = runFeatureManager(METHOD_NAME, install_params);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        //Log.info(c, METHOD_NAME, po.getStdout());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));

        // Test feature uninstall
        po = runFeatureManager(METHOD_NAME, uninstall_params);
        assertEquals("Expected exit code", 0, po.getReturnCode());

        deleteFiles(METHOD_NAME, "genericCoreFeatureA and genericCoreFeatureB", filesList);
        Log.exiting(c, METHOD_NAME);

    }

    /**
     * Uninstall multiple features using comma and space separator
     *
     * @throws Exception
     */
    @Test
    public void testMultipleFeaturesInstallUinstallUsingMixedSeparator() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testMultipleFeaturesInstallUinstallUsingMixedSeparator";
        Log.entering(c, METHOD_NAME);
        String[] install_params = { "install", "com.ibm.genericCoreFeature,com.ibm.genericCoreFeatureA", "com.ibm.genericCoreFeatureB", "--acceptLicense",
                                    "--when-file-exists=replace" };
        String[] uninstall_params = { "uninstall", "com.ibm.genericCoreFeature,com.ibm.genericCoreFeatureA", "com.ibm.genericCoreFeatureB", "--noPrompts" };

        String[] filesList = { "lib/features/com.ibm.genericCoreFeature.mf", "lib/features/com.ibm.genericCoreFeatureA.mf", "lib/features/com.ibm.genericCoreFeatureB.mf" };

        //test features install
        deleteFiles(METHOD_NAME, "genericCoreFeature, genericCoreFeatureA and genericCoreFeatureB", filesList);

        ProgramOutput po;

        po = runFeatureManager(METHOD_NAME, install_params);

        assertEquals("Expected exit code", 0, po.getReturnCode());
        //Log.info(c, METHOD_NAME, po.getStdout());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));

        // Test feature uninstall
        po = runFeatureManager(METHOD_NAME, uninstall_params);
        assertEquals("Expected exit code", 0, po.getReturnCode());

        deleteFiles(METHOD_NAME, "genericCoreFeature, genericCoreFeatureA and genericCoreFeatureB", filesList);
        Log.exiting(c, METHOD_NAME);

    }

    /**
     * Test --viewLicenseInfo and verify the expected license text
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testViewLicenseInfoOption() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testViewLicenseInfoOption";
        Log.entering(c, METHOD_NAME);
        String[] params = { "install", "webCacheMonitor-1.0",
                            "--viewLicenseInfo" };

        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.webCacheMonitor-1.0.mf" };
        deleteFiles(METHOD_NAME, "webCacheMonitor", filesList);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, params);
        String stdout = po.getStdout();
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("Correct info : Program Name: Web Cache Monitor for WebSphere Application Server",
                   stdout.indexOf("Program Name: Web Cache Monitor for WebSphere Application Server") >= 0);

        deleteFiles(METHOD_NAME, "webCacheMonitor", filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test --viewLicenseAgreement and verify the expected license text
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testViewLicenseAgreementOption() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testViewLicenseAgreementOption";
        Log.entering(c, METHOD_NAME);
        String[] params = { "install", "webCacheMonitor-1.0", "--viewLicenseAgreement" };

        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.webCacheMonitor-1.0.mf" };
        deleteFiles(METHOD_NAME, "webCacheMonitor", filesList);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, params);
        String stdout = po.getStdout();
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("Correct info : International License Agreement for Non-Warranted Programs",
                   stdout.indexOf("International License Agreement for Non-Warranted Programs") >= 0);

        deleteFiles(METHOD_NAME, "webCacheMonitor", filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test --viewLicenseInfo for ESA of license type unspecified
     *
     * @throws Exception
     */
    //Make esa editions applicable=null
    //new version of esa with editions=null created
    //Having trouble replacing in online repos, keep test disabled until asset can be replaced
    //@Test
    public void testAcknowledgeUnspecifiedLicenceType() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testAcknowledgeUnspecifiedLicenceType()";
        Log.entering(c, METHOD_NAME);

        String[] params = { "install", "cik.unspecified.license.type", "--viewLicenseAgreement" };

        ProgramOutput po;

        po = runFeatureManager(METHOD_NAME, params);
        String stdout = po.getStdout();
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("Correct info expected : UNSPECIFIED Licensee acknowledges and agrees",
                   stdout.indexOf("UNSPECIFIED") >= 0);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Basic feature install invalid feature
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallInvalid() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeatureInstallInvalid";
        Log.entering(c, METHOD_NAME);
        String[] params = { "install", "abc-1.0", "--acceptLicense" };

        ProgramOutput po = runFeatureManager(METHOD_NAME, params);
        assertTrue("stabilized", po.getStdout().indexOf("stabilized") >= 0);
        assertTrue("CWWKF1203E: Failed to obtain the following features: [abc-1.0]. Ensure that the system can access the IBM WebSphere Liberty Repository and that the features are valid.",
                   po.getStdout().indexOf("CWWKF1203E:") >= 0);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Basic feature install invalid feature
     *
     * @throws Exception
     */
    @Test
    public void testFeatureInstallTrailingEscapeChars() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeatureInstallTrailingEscapeChars";
        Log.entering(c, METHOD_NAME);
        String[] params = { "install", "abc-1.0\\\\", "--acceptLicense" };

        ProgramOutput po = runFeatureManager(METHOD_NAME, params);
        assertTrue("CWWKF1203E: Failed to obtain the following features: [abc-1.0\\]. Ensure that the system can access the IBM WebSphere Liberty Repository and that the features are valid.",
                   po.getStdout().indexOf("CWWKF1203E:") >= 0);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Install the feature twice to ensure that the install fails the second time
     */
    @Test
    public void testInstallFeatureTwice() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallFeatureTwice";
        Log.entering(c, METHOD_NAME);
        String[] params = { "install", "testesa1", "--acceptLicense" };

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/bin/testesa1.bat",
                               "usr/extension/lib/testesa1_1.0.0.jar" };
        String[] filesList2 = { "usr/extension/lib/features/testesa1.mf",
                                "usr/extension/bin/testesa1.bat",
                                "usr/extension/lib/testesa1_1.0.0.jar", "lib/features/testesa1.mf", "bin/testesa1.bat", "lib/testesa1_1.0.0.jar",
                                "/usr/extension/lib/features/testesa1.mf",
                                "/usr/extension/bin/testesa1.bat",
                                "/usr/extension/lib/testesa1_1.0.0.jar", "/lib/features/testesa1.mf", "/bin/testesa1.bat", "/lib/testesa1_1.0.0.jar"
        };
        deleteFiles(METHOD_NAME, "testesa1", filesList2);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, params);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertFilesExist(filesList);

        po = runFeatureManager(METHOD_NAME, params);
        assertTrue("CWWKF1216I: The feature should already exist.", po.getStdout().indexOf("CWWKF1216I") >= 0);

        deleteFiles(METHOD_NAME, "testesa1", filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This test ensures that you can install a user feature that has a dependency to
     * another feature in the same install location
     *
     * @throws Exception
     */
    //Disabled for RTC 136527
    @Test
    public void testFeatureInstallWithDependency() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeatureInstallWithDependency";
        Log.entering(c, METHOD_NAME);

        String[] params = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense" };

        String[] filesList = { "/lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                               "/lib/features/com.ibm.genericCoreFeature.mf", };
        deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, params);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("stabilized", po.getStdout().indexOf("stabilized") >= 0);
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertFilesExist(filesList);

        deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    /**
     * Install an IBM features which has a dependency that does not exist in Massive
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void testFeaturesInstallIBMFeature_DependantOnNonExistantFeature() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeaturesInstallFailIBMFeature_DependantOnNonExistantFeature";
        Log.entering(c, METHOD_NAME);

        String featureName = "com.ibm.genericCoreFeatureDependancyOnEsaFail";
        String[] param1s = { "install", featureName, "--to=core", "--acceptLicense" };

        String[] filesList = { "/lib/features/com.ibm.genericCoreFeatureDependancyFail_1.0.mf" };
        deleteFiles(METHOD_NAME, featureName, filesList);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, param1s);
        assertTrue("Failed to recognize the missing dependent feature in massive",
                   po.getStdout().indexOf("CWWKF1221E:") >= 0);

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Install an IBM feature with the "--to core" parameters
     */
    @Test
    @Ignore
    public void testFeatureInstallIBMFeatureToCore() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeatureInstallIBMFeatureToCore";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "install", "webCacheMonitor-1.0", "--to=core", "--acceptLicense",
                             "--when-file-exists=replace" };

        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.webCacheMonitor-1.0.mf",
                               "/lib/com.ibm.ws.dynacache.cachemonitor_1.0.0.jar" };
        deleteFiles(METHOD_NAME, "webCacheMonitor-1.0", filesList);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, param1s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertFilesExist(filesList);

        deleteFiles(METHOD_NAME, "webCacheMonitor-1.0", filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Install an IBM feature with the "--to usr" parameters and make sure that the IBM headers
     * override the function and feature is installed to core
     */
    @Test
    @Ignore
    public void testFeatureInstallIBMFeatureToUsr() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeatureInstallIBMFeatureToUsr";
        Log.entering(c, METHOD_NAME);
        String[] param1s = { "install", "webCacheMonitor-1.0", "--to=usr", "--acceptLicense",
                             "--when-file-exists=replace" };

        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.webCacheMonitor-1.0.mf",
                               "/lib/com.ibm.ws.dynacache.cachemonitor_1.0.0.jar" };
        deleteFiles(METHOD_NAME, "webCacheMonitor-1.0", filesList);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, param1s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertFilesExist(filesList);

        deleteFiles(METHOD_NAME, "webCacheMonitor-1.0", filesList);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Install an IBM feature with the "--to=Extension" parameters
     */
    @Test
    @Ignore
    public void testFearureInstallIBMFeatureToExtension() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFearureInstallIBMFeatureToExtension";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "install", "webCacheMonitor-1.0", "--to=cik.ext.product1",
                             "--acceptLicense", "--when-file-exists=replace" };

        String[] filesList = { "/lib/features/com.ibm.websphere.appserver.webCacheMonitor-1.0.mf",
                               "/lib/com.ibm.ws.dynacache.cachemonitor_1.0.0.jar" };
        deleteFiles(METHOD_NAME, "webCacheMonitor-1.0", filesList);

        ProgramOutput po;
        createExtensionDirs("cik.ext.product1", server.getInstallRoot() + "/../cik");

        try {
            po = runFeatureManager(METHOD_NAME, param1s);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(filesList);
        } finally {
            deleteFiles(METHOD_NAME, "webCacheMonitor-1.0", filesList);
            server.deleteDirectoryFromLibertyInstallRoot("usr/cik");
            Log.exiting(c, METHOD_NAME);
        }
    }

    /**
     * Install an user feature with the "--to core" parameters
     */
    @Test
    public void testFeatureInstallUserFeatureToCore() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeatureInstallUserFeatureToCore";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "install", "testesa1", "--to=core", "--acceptLicense", "--when-file-exists=replace", "--verbose=debug" };

        String[] filesList = { "lib/features/testesa1.mf", "bin/testesa1.bat", "lib/testesa1_1.0.0.jar" };
        String[] filesList2 = { "usr/extension/lib/features/testesa1.mf",
                                "usr/extension/bin/testesa1.bat",
                                "usr/extension/lib/testesa1_1.0.0.jar", "lib/features/testesa1.mf", "bin/testesa1.bat", "lib/testesa1_1.0.0.jar",
                                "usr/cik/extensions/cik.ext.product1/lib/features/testesa1.mf",
                                "usr/cik/extensions/cik.ext.product1/bin/testesa1.bat", "usr/cik/extensions/cik.ext.product1/lib/testesa1_1.0.0.jar"
        };
        deleteFiles(METHOD_NAME, "testesa1", filesList2);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, param1s);
        assertEquals("Expected exit code", 36, po.getReturnCode());
        assertTrue("CWWKF1036E should have been printed", po.getStdout().contains("CWWKF1036E:"));
        assertTrue("User feature cannot be installed to core", po.getStdout().contains("user feature cannot be installed to core."));
        Log.exiting(c, METHOD_NAME);
        //     assertFilesExist(filesList);

        //    deleteFiles(METHOD_NAME, "testesa1", filesList);
    }

    /**
     * Install an user feature with the "--to=usr" parameters
     */
    @Test
    public void testFeatureInstallUserFeatureToUsr() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeatureInstallUserFeatureToUsr";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "install", "testesa1", "--to=usr", "--acceptLicense", "--when-file-exists=replace", "--verbose=debug" };

        String[] filesList = { "/usr/extension/lib/features/testesa1.mf", "/usr/extension/bin/testesa1.bat", "/usr/extension/lib/testesa1_1.0.0.jar" };
        deleteFiles(METHOD_NAME, "testesa1", filesList);

        ProgramOutput po;
        try {
            po = runFeatureManager(METHOD_NAME, param1s);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1017I: The feature has been successfully installed.",
                       po.getStdout().indexOf("CWWKF1017I:") >= 0);
            assertFilesExist(filesList);

            po = runFeatureManager(METHOD_NAME, new String[] { "uninstall", "testesa1", "--noPrompts" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("The feature should successfully uninstalled but not",
                       po.getStdout().indexOf("One or more features uninstalled successfully: testesa1") >= 0);
            assertFilesNotExist(filesList);
        } finally {
            deleteFiles(METHOD_NAME, "testesa1", filesList);
            Log.exiting(c, METHOD_NAME);
        }
    }

    /**
     * Install an user feature with the "--to EXTENSION" parameters
     */
    @Test
    public void testFeatureInstallUserFeatureToExtension() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeatureInstallUserFeatureToExtension";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "install", "testesa1", "--to=cik.ext.product1", "--acceptLicense",
                             "--when-file-exists=replace", "--verbose=debug" };

        server.deleteDirectoryFromLibertyInstallRoot("/../cik");
        String extensionPath = server.getInstallRoot() + "/../cik";
        createExtensionDirs("/cik.ext.product1", extensionPath);

        String[] filesList = { "/../cik/cik.ext.product1/lib/features/testesa1.mf",
                               "/../cik/cik.ext.product1/lib/testesa1_1.0.0.jar",
                               "/../cik/cik.ext.product1/bin/testesa1.bat" };

        ProgramOutput po;
        try {
            po = runFeatureManager(METHOD_NAME, param1s);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1017I: The feature has been successfully installed.", po.getStdout().indexOf("CWWKF1017I:") >= 0);
            assertFilesExist(filesList);

            po = runFeatureManager(METHOD_NAME, new String[] { "uninstall", "testesa1", "--noPrompts" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("The feature should successfully uninstalled but not",
                       po.getStdout().indexOf("One or more features uninstalled successfully: testesa1") >= 0);
            assertFilesNotExist(filesList);
        } finally {
            server.deleteDirectoryFromLibertyInstallRoot("../cik");
            Log.exiting(c, METHOD_NAME);
        }
    }

    @Test
    public void testUninstallDependentUserFeature() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testUninstallDependentUserFeature";
        Log.entering(c, METHOD_NAME);
        String[] params = { "install", "testesa3", "--acceptLicense" };

        String[] filesList = { "usr/extension/lib/features/testesa1.mf", "usr/extension/lib/features/testesa3.mf",
                               "usr/extension/bin/testesa1.bat", "usr/extension/bin/testesa3.bat",
                               "usr/extension/lib/testesa1_1.0.0.jar" };
        String[] filesList2 = { "usr/extension/lib/features/testesa1.mf", "usr/extension/lib/features/testesa3.mf",
                                "usr/extension/bin/testesa1.bat", "usr/extension/bin/testesa3.bat",
                                "usr/extension/lib/testesa1_1.0.0.jar", "lib/features/testesa1.mf", "bin/testesa1.bat", "lib/testesa1_1.0.0.jar",
                                "usr/cik/extensions/cik.ext.product1/lib/features/testesa1.mf",
                                "usr/cik/extensions/cik.ext.product1/bin/testesa1.bat", "usr/cik/extensions/cik.ext.product1/lib/testesa1_1.0.0.jar"
        };
        deleteFiles(METHOD_NAME, "testesa1 and testesa3", filesList2);

        ProgramOutput po;
        try {
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(filesList);

            po = runFeatureManager(METHOD_NAME, new String[] { "uninstall", "testesa1", "--noPrompts" });
            assertEquals("Expected exit code", 21, po.getReturnCode());
            assertTrue("Expected exception CWWKF1263E", po.getStdout().contains("CWWKF1263E"));
        } finally {
            deleteFiles(METHOD_NAME, "testesa1 and testesa3", filesList);;
            Log.exiting(c, METHOD_NAME);
        }
    }

    public static void checkInstalledFiles(String[] fileList, boolean installed) {
        if (installed) {
            ArrayList<String> missingFiles = new ArrayList<String>();
            for (String fileName : fileList) {
                if (!(new File(TestUtils.wlpDir, fileName).exists()))
                    missingFiles.add(fileName);
            }
            assertTrue("Files List : " + missingFiles.toString() + " should exist", missingFiles.isEmpty());
        } else {
            ArrayList<String> extraFiles = new ArrayList<String>();
            for (String fileName : fileList) {
                if (new File(TestUtils.wlpDir, fileName).exists())
                    extraFiles.add(fileName);
            }
            assertTrue("Files List : " + extraFiles.toString() + " should not exist", extraFiles.isEmpty());
        }
    }

    /**
     * Install two ESAs with a common file: user features A and then install B with fail option
     * resulting in the installation of B to fail. Ensure that the common file is not replaced
     *
     * @throws Exception
     */
    @Test
    public void testInstallFeature_whenFileExistsfailOption() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testInstallFeature_whenFileExistsfailOption";
        Log.entering(c, METHOD_NAME);

        String featureA = "com.ibm.genericCoreFeatureModifyA";
        String featureB = "com.ibm.genericCoreFeatureModifyB";

        String[] param1s = { "install", featureA, "--to=core", "--acceptLicense", "--when-file-exists=replace" };
        String[] filesListA = { "/lib/features/com.ibm.genericCoreFeatureModifyA.mf", "/lib/com.ibm.genericCoreFeatureModifyA_1.0.0.jar",
                                "lib/features/l10n/com.ibm.genericCoreFeatureModifyA.properties",
                                "/lib/features/checksums/com.ibm.genericCoreFeatureModifyA.cs" };

        String[] param2s = { "install", featureB, "--to=core", "--acceptLicense", "--when-file-exists=fail" };
        String[] filesListB = { "/lib/features/com.ibm.genericCoreFeatureModifyB.mf", "bin/ModifyFile.txt",
                                "/lib/com.ibm.genericCoreFeatureModifyB_1.0.0.jar",
                                "lib/features/l10n/com.ibm.genericCoreFeatureModifyB.properties",
                                "/lib/features/checksums/com.ibm.genericCoreFeatureModifyB.cs" };

        deleteFiles(METHOD_NAME, featureA, filesListA);
        deleteFiles(METHOD_NAME, featureB, filesListB);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, param1s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertTrue("The feature manifest  should exist",
                   server.fileExistsInLibertyInstallRoot("lib/features/com.ibm.genericCoreFeatureModifyA.mf"));
        assertTrue("The common file should exist", server.fileExistsInLibertyInstallRoot("bin/ModifyFile.txt"));

        po = runFeatureManager(METHOD_NAME, param2s);
        assertTrue("The feature installation when -whenfileExists=fail option was used was not blocked",
                   po.getStdout().indexOf("CWWKF1015E") >= 0);

        TestUtils.verifyFirstLine(new File(server.getInstallRoot(), "bin/ModifyFile.txt"), "genericCoreModifyA");

        deleteFiles(METHOD_NAME, featureA, filesListA);
        deleteFiles(METHOD_NAME, featureB, filesListB);
        Log.exiting(c, METHOD_NAME);

    }

    /**
     * Install user features A and then install B with ignore option resulting in the installation
     * of B to succeed but with the common file not being replaced
     *
     * @throws Exception
     */
    @Test
    public void testInstallFeature_whenFileExistsIgnoreOption() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testInstallFeature_whenFileExistsIgnoreOption";
        Log.entering(c, METHOD_NAME);

        String featureA = "com.ibm.genericCoreFeatureModifyA";
        String featureB = "com.ibm.genericCoreFeatureModifyB";

        String[] param1s = { "install", featureA, "--to=core", "--acceptLicense", "--when-file-exists=replace" };
        String[] filesListA = { "/lib/features/com.ibm.genericCoreFeatureModifyA.mf", "bin/ModifyFile.txt",
                                "/lib/com.ibm.genericCoreFeatureModifyA_1.0.0.jar",
                                "lib/features/l10n/com.ibm.genericCoreFeatureModifyA.properties",
                                "/lib/features/checksums/com.ibm.genericCoreFeatureModifyA.cs" };

        String[] param2s = { "install", featureB, "--to=core", "--acceptLicense", "--when-file-exists=ignore", "--verbose=debug" };
        String[] filesListB = { "/lib/features/com.ibm.genericCoreFeatureModifyB.mf", "bin/ModifyFile.txt",
                                "/lib/com.ibm.genericCoreFeatureModifyB_1.0.0.jar",
                                "lib/features/l10n/com.ibm.genericCoreFeatureModifyB.properties",
                                "/lib/features/checksums/com.ibm.genericCoreFeatureModifyB.cs" };

        deleteFiles(METHOD_NAME, featureA, filesListA);
        deleteFiles(METHOD_NAME, featureB, filesListB);

        ProgramOutput po;

        po = runFeatureManager(METHOD_NAME, param1s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertTrue("The feature manifest  should exist",
                   server.fileExistsInLibertyInstallRoot("lib/features/com.ibm.genericCoreFeatureModifyA.mf"));
        assertTrue("The common file should exist", server.fileExistsInLibertyInstallRoot("bin/ModifyFile.txt"));

        po = runFeatureManager(METHOD_NAME, param2s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("CWWKF1017I: The feature has been successfully installed.", po.getStdout().indexOf("CWWKF1017I:") >= 0);
        assertTrue("The feature manifest  should exist",
                   server.fileExistsInLibertyInstallRoot("lib/features/com.ibm.genericCoreFeatureModifyB.mf"));
        assertTrue("The common file should exist", server.fileExistsInLibertyInstallRoot("bin/ModifyFile.txt"));

        TestUtils.verifyFirstLine(new File(server.getInstallRoot(), "bin/ModifyFile.txt"), "genericCoreModifyA");

        deleteFiles(METHOD_NAME, featureA, filesListA);
        deleteFiles(METHOD_NAME, featureB, filesListB);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Install user features A and then install B with ignore option resulting in the installation
     * of B to succeed and with common file replaced
     *
     * @throws Exception
     */
    @Test
    public void testInstallFeature_whenFileExistsReplaceOption() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testInstallFeature_whenFileExistsIgnoreOption";

        Log.entering(c, METHOD_NAME);

        String featureA = "com.ibm.genericCoreFeatureModifyA";
        String featureB = "com.ibm.genericCoreFeatureModifyB";

        String[] param1s = { "install", featureA, "--to=core", "--acceptLicense",
                             "--when-file-exists=replace" };
        String[] filesListA = { "/lib/features/com.ibm.genericCoreFeatureModifyA.mf", "bin/ModifyFile.txt",
                                "/lib/com.ibm.genericCoreFeatureModifyA_1.0.0.jar",
                                "/lib/features/l10n/com.ibm.genericCoreFeatureModifyA.properties",
                                "/lib/features/checksums/com.ibm.genericCoreFeatureModifyA.cs " };

        String[] param2s = { "install", featureB, "--to=core", "--acceptLicense", "--when-file-exists=replace", "--verbose=debug" };
        String[] filesListB = { "/lib/features/com.ibm.genericCoreFeatureModifyB.mf", "bin/ModifyFile.txt",
                                "/lib/com.ibm.genericCoreFeatureModifyB_1.0.0.jar",
                                "lib/features/l10n/com.ibm.genericCoreFeatureModifyB.properties",
                                "/lib/features/checksums/com.ibm.genericCoreFeatureModifyB.cs" };

        deleteFiles(METHOD_NAME, featureA, filesListA);
        deleteFiles(METHOD_NAME, featureB, filesListB);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, param1s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertTrue("The feature manifest  should exist",
                   server.fileExistsInLibertyInstallRoot("lib/features/com.ibm.genericCoreFeatureModifyA.mf"));
        assertTrue("The common file should exist", server.fileExistsInLibertyInstallRoot("bin/ModifyFile.txt"));

        po = runFeatureManager(METHOD_NAME, param2s);
        assertEquals("Expected exit code", 0, po.getReturnCode());
        assertTrue("CWWKF1017I: The feature has been successfully installed.",
                   po.getStdout().indexOf("CWWKF1017I:") >= 0);
        assertTrue("The feature manifest  should exist",
                   server.fileExistsInLibertyInstallRoot("lib/features/com.ibm.genericCoreFeatureModifyB.mf"));
        assertTrue("The common file should exist",
                   server.fileExistsInLibertyInstallRoot("bin/ModifyFile.txt"));

        TestUtils.verifyFirstLine(new File(server.getInstallRoot(), "bin/ModifyFile.txt"),
                                  "genericCoreModifyB");

        deleteFiles(METHOD_NAME, featureA, filesListA);
        deleteFiles(METHOD_NAME, featureB, filesListB);
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallFeatureWithScripts() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String METHOD_NAME = "testInstallFeatureWithScripts";
        Log.entering(c, METHOD_NAME);

        String serverInstallRoot = server.getInstallRoot();
        Log.info(c, METHOD_NAME, "serverInstallRoot=" + serverInstallRoot);
        LibertyFileManager.copyFileIntoLiberty(server.getMachine(), serverInstallRoot + "/usr", "publish/features/com.ibm.scripts.feature.esa");

        Collection<String> filesToTidy = new HashSet<String>();
        filesToTidy.add("/lib/features/cik.scripts.feature.mf");
        filesToTidy.add("/bin/cikEchoJREHOME");
        filesToTidy.add("/bin/cikEchoJREHOME.bat");
        filesToTidy.add("/bin/cik/cikEchoJAVAHOME");
        filesToTidy.add("/bin/cik/cikEchoJAVAHOME.bat");
        try {
            ProgramOutput po = runFeatureManager(METHOD_NAME, new String[] { "install", serverInstallRoot + "/usr/com.ibm.scripts.feature.esa", "--acceptLicense", "--to=core",
                                                                             "--verbose=debug" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Should have trace \"Setting scripts permission ...\"", po.getStdout().contains("Setting scripts permission ..."));

            boolean isZOS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).equalsIgnoreCase("z/OS");
            Log.info(c, METHOD_NAME, "isZOS=" + isZOS);
            for (String pathName : filesToTidy) {
                LibertyFileManager.libertyFileExists(server.getMachine(), serverInstallRoot + pathName);
            }
            if (!isZOS) {
                po = runCommand(METHOD_NAME, "cikEchoJREHOME", new String[] {}, new Properties());
                assertTrue("Output should contain JRE_HOME=", po.getStdout().contains("JRE_HOME="));
                po = runCommand(METHOD_NAME, "cik/cikEchoJAVAHOME", new String[] {}, new Properties());
                assertTrue("Output should contain JAVA_HOME=", po.getStdout().contains("JAVA_HOME="));
            }
        } finally {
            // Remove files
            LibertyFileManager.deleteLibertyFile(server.getMachine(), serverInstallRoot + "/usr/com.ibm.scripts.feature.esa");
            for (String pathName : filesToTidy) {
                LibertyFileManager.deleteLibertyFile(server.getMachine(), serverInstallRoot + pathName);
            }
        }
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallTypeNotApplicable() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallTypeNotApplicable";
        Log.entering(c, METHOD_NAME);
        String[] params = { "install", "a8552.usertest.IM", "--acceptLicense" };
        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, params);
        assertEquals("Expected exit code", 29, po.getReturnCode());
        assertTrue("CWWKF1226E should have been printed", po.getStdout().contains("CWWKF1226E:"));
        assertTrue("\"Archive\" should have been printed", po.getStdout().contains("Archive"));
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testProductVersionNotApplicable() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testProductVersionNotApplicable";
        Log.entering(c, METHOD_NAME);
        String[] params = { "install", "a8552.usertest.dependant", "--acceptLicense" };
        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, params);
        assertEquals("Expected exit code", 29, po.getReturnCode());
        assertTrue("CWWKF1296E should have been printed", po.getStdout().contains("CWWKF1296E:"));
        assertTrue("version: " + originalWlpVersion + " should have been printed", po.getStdout().contains(originalWlpVersion));
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testProductBetaVersionNotApplicable() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testProductBetaVersionNotApplicable";
        Log.entering(c, METHOD_NAME);
        String[] params = { "install", "beta.usertest", "--acceptLicense" };
        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, params);
        assertEquals("Expected exit code", 29, po.getReturnCode());
        assertTrue("CWWKF1296E should have been printed", po.getStdout().contains("CWWKF1296E:"));
        assertTrue("version: 2014.2.0.0 should have been printed", po.getStdout().contains("2014.2.0.0"));
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testProductEditionNotApplicable() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testProductEditionNotApplicable";
        Log.entering(c, METHOD_NAME);
        replaceWlpProperties("8.5.5.3", "FAT_EDITION", null);
        String[] params = { "install", "genericCoreVersionIndependent", "--acceptLicense" };
        ProgramOutput po;
        try {
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 29, po.getReturnCode());
            assertTrue("CWWKF1225E should have been printed", po.getStdout().contains("CWWKF1225E:"));
            Log.exiting(c, METHOD_NAME);
        } finally {
            resetOriginalWlpProps();
        }
    }

    @Test
    public void testInstallDownloadOnly() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallDownloadOnly";
        Log.entering(c, METHOD_NAME);

        String[] params = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--downloadOnly", "--location=" + installRoot + "/usr/download",
                            "--verbose=debug" };

        String[] esasList = { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa",
                              "usr/download/com.ibm.genericCoreFeature.esa" };

        String[] filesList = new String[] { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                                            "lib/features/com.ibm.genericCoreFeature.mf",
                                            "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                                            "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                                            "lib/features/checksums/com.ibm.genericCoreFeature.cs",
                                            "lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs" };

        deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", esasList);
        deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);

        try {
            // Download
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertFalse("stabilized", po.getStdout().indexOf("stabilized") >= 0);
            assertTrue("CWWKF1501I should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I"));
            assertFilesExist(esasList);

            // Install the downloaded feature
            String[] featureFilesList = { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                                          "lib/features/com.ibm.genericCoreFeature.mf", };
            String[] installParams = new String[] { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--location=" + installRoot + "/usr/download", "--offlineOnly",
                                                    "--acceptLicense",
                                                    "--when-file-exists=ignore" };
            po = runFeatureManager(METHOD_NAME, installParams);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFilesList);

            // Redownload and expect already installed
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 22, po.getReturnCode());
            assertTrue("CWWKF1243E should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1243E"));

        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", esasList);
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallDownloadOnly_alreadyExists() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallDownloadOnly_alreadyExists";
        Log.entering(c, METHOD_NAME);

        String[] params = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--downloadOnly", "--location=" + installRoot + "/usr/download",
                            "--verbose=debug" };

        String[] filesList = { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa",
                               "usr/download/com.ibm.genericCoreFeature.esa" };

        deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);

        try {
            // First download
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I"));
            assertFilesExist(filesList);

            // Redownload and expect no error
            po = runFeatureManager(METHOD_NAME, params);
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I or CWWKF1500I  should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I") || po.getStdout().contains("CWWKF1500I"));
            assertFilesExist(filesList);
            // Redownload with ignore option and expect no error
            params = new String[] { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--downloadOnly",
                                    "--location=" + installRoot + "/usr/download",
                                    "--when-file-exists=ignore", "--verbose=debug" };
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I or CWWKF1500I  should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I") || po.getStdout().contains("CWWKF1500I"));
            assertFilesExist(filesList);

            // Redownload with replace option and expect no error
            params = new String[] { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--downloadOnly",
                                    "--location=" + installRoot + "/usr/download",
                                    "--when-file-exists=replace", "--verbose=debug" };
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I or CWWKF1500I  should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I") || po.getStdout().contains("CWWKF1500I"));
            assertFilesExist(filesList);

            // Redownload with fail option and expect error
            params = new String[] { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--downloadOnly",
                                    "--location=" + installRoot + "/usr/download",
                                    "--when-file-exists=fail", "--verbose=debug" };
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 25, po.getReturnCode());
            assertTrue("CWWKF1015E should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1015E"));

        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallDownloadOnly_multipleFeatures() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallDownloadOnly_multipleFeatures";
        Log.entering(c, METHOD_NAME);
        final String featureShortName = "genericCoreFeatureA";
        final String featureShortName2 = "genericCoreFeatureB";

        final String[] params = { "install", String.format("com.ibm.%s,com.ibm.%s", featureShortName, featureShortName2), "--acceptLicense",
                                  "--downloadOnly", "--location=" + installRoot + "/usr/download", "--verbose=debug" };

        String[] filesList = { String.format("usr/download/com.ibm.%s.esa", featureShortName),
                               String.format("usr/download/com.ibm.%s.esa", featureShortName2) };
        try {
            //Download and expect successful
            ProgramOutput po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Exit Code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I should have been printed:\r\n" + po.getStdout(), po.getStdout().contains("CWWKF1501I"));
            assertFilesExist(filesList);

            // Redownload and expect no error
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I or CWWKF1500I  should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I") || po.getStdout().contains("CWWKF1500I"));
            assertFilesExist(filesList);
        } finally {
            deleteFiles(METHOD_NAME, featureShortName, filesList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallDownloadOnly_useShortNames() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallDownloadOnly_useShortNames";
        Log.entering(c, METHOD_NAME);

        final String featureName = "com.ibm.genericCoreFeature";

        final String[] params = { "install", "genericCoreFeature", "--acceptLicense", "--downloadOnly", "--location=" + installRoot + "/usr/download", "--verbose=debug" };

        String[] filesList = { String.format("usr/download/%s.esa", featureName) };

        try {
            //Download and expect successfull
            ProgramOutput po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Exit Code", 0, po.getReturnCode());
            Log.info(c, METHOD_NAME, po.getStdout());
            assertTrue("CWWKF1500I should have been printed:\r\n" + po.getStdout(), po.getStdout().contains("CWWKF1500I"));
            assertFilesExist(filesList);

            // Install the downloaded feature and expect all the files are successfully installed
            String[] featureFilesList = { String.format("lib/features/%s.mf", featureName) };
            String[] installParams = new String[] { "install", featureName, "--location=" + installRoot + "/usr/download", "--offlineOnly",
                                                    "--acceptLicense",
                                                    "--when-file-exists=ignore" };
            po = runFeatureManager(METHOD_NAME, installParams);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFilesList);

            // Redownload and expect already installed
            po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Expected exit code", 22, po.getReturnCode());
            assertTrue("CWWKF1243E should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1243E"));

        } finally {
            deleteFiles(METHOD_NAME, featureName, filesList);
            filesList = new String[] { String.format("lib/features/%s.mf", featureName),
                                       String.format("lib/features/l10n/%s.properties", featureName),
                                       String.format("lib/features/checksums/%s.cs", featureName) };
            deleteFiles(METHOD_NAME, featureName, filesList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallFrom_multipleFeatures() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallFrom_multipleFeatures";
        Log.entering(c, METHOD_NAME);

        final String featureShortName = "genericCoreFeatureA";
        final String featureShortName2 = "genericCoreFeatureB";

        final String[] params = { "install", String.format("com.ibm.%s,com.ibm.%s", featureShortName, featureShortName2), "--acceptLicense",
                                  "--downloadOnly", "--location=" + installRoot + "/usr/download", "--verbose=debug" };

        String[] filesList = { String.format("usr/download/com.ibm.%s.esa", featureShortName),
                               String.format("usr/download/com.ibm.%s.esa", featureShortName2) };
        try {
            //Download and expect successful
            ProgramOutput po = runFeatureManager(METHOD_NAME, params);
            assertEquals("Exit Code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I should have been printed:\r\n" + po.getStdout(), po.getStdout().contains("CWWKF1501I"));
            assertFilesExist(filesList);

            // Install the downloaded feature and expect successful install
            String[] featureFilesList = { String.format("lib/features/com.ibm.%s.mf", featureShortName),
                                          String.format("lib/features/com.ibm.%s.mf", featureShortName2) };
            String[] installParams = new String[] { "install", String.format("com.ibm.%s,com.ibm.%s", featureShortName, featureShortName2),
                                                    "--location=" + installRoot + "/usr/download", "--offlineOnly", "--acceptLicense",
                                                    "--when-file-exists=ignore" };
            po = runFeatureManager(METHOD_NAME, installParams);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFilesList);
        } finally {
            deleteFiles(METHOD_NAME, String.format("%s and %s", featureShortName, featureShortName2), filesList);
            filesList = new String[] { String.format("lib/features/com.ibm.%s.mf", featureShortName),
                                       String.format("lib/features/com.ibm.%s.mf", featureShortName2),
                                       String.format("lib/features/l10n/com.ibm.%s.properties", featureShortName),
                                       String.format("lib/features/l10n/com.ibm.%s.properties", featureShortName2),
                                       String.format("lib/features/checksums/com.ibm.%s.cs", featureShortName),
                                       String.format("lib/features/checksums/com.ibm.%s.cs", featureShortName2) };
            deleteFiles(METHOD_NAME, String.format("%s and %s", featureShortName, featureShortName2), filesList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * downloadOnly=all tests;
     */
    @Test
    public void testInstallDownloadOnlyMissingDirectory() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallDownloadOnlyMissingDirectory";
        Log.entering(c, METHOD_NAME);

        String[] params1 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--downloadOnly" };
        String[] params2 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--offlineOnly" };
        try {
            // Download
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, params1);
            assertEquals("Expected exit code", 20, po.getReturnCode());
            assertTrue("CWWKF1358E should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1358E"));

            po = runFeatureManager(METHOD_NAME, params2);
            assertEquals("Expected exit code", 20, po.getReturnCode());
            assertTrue("CWWKF1358E should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1358E"));

        } finally {
            // Nothing to clean up

        }
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallDownloadOnlyInvalidOption() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallDownloadOnlyInvalidOption";
        Log.entering(c, METHOD_NAME);

        String[] params1 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--downloadOnly=invalidOption",
                             "--location=" + installRoot + "/usr/download" };
        try {
            // Download
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, params1);
            assertEquals("Expected exit code", 20, po.getReturnCode());
            assertTrue("CWWKF1359E should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1359E"));

        } finally {
            // Nothing to clean up

        }
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallDownloadOnlyNoDep() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallDownloadOnlyNoDep";
        Log.entering(c, METHOD_NAME);

        String[] params1 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--downloadOnly=none",
                             "--location=" + installRoot + "/usr/download", "--verbose=debug" };
        String[] featureFilesList = { "lib/features/com.ibm.genericCoreFeature.mf", "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf" };
        String[] installParams = new String[] { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense",
                                                "--when-file-exists=ignore" };
        String[] filesList = { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa", "usr/download/com.ibm.genericCoreFeature.esa" };
        String[] downloaded = { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa" };
        String[] shouldNotBeDownloaded = { "usr/download/com.ibm.genericCoreFeature.esa" };
        try {
            ProgramOutput po;
            //install genericCoreFeatureDependancyOnEsaPass
            po = runFeatureManager(METHOD_NAME, installParams);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFilesList);

            //delete genericCoreFeatureDependancyOnEsaPass.esa and genericCoreFeature.esa
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeature, com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);

            //download genericCoreFeatureDependancyOnEsaPass
            po = runFeatureManager(METHOD_NAME, params1);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1500I should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1500I"));
            assertFilesExist(downloaded);
            assertFilesNotExist(shouldNotBeDownloaded);
        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
            filesList = new String[] { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                                       "lib/features/com.ibm.genericCoreFeature.mf",
                                       "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                                       "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                                       "lib/features/checksums/com.ibm.genericCoreFeature.cs",
                                       "lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs" };
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInstallDownloadOnlyNoDepWithMultipleFeatures() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallDownloadOnlyNoDepWithMultipleFeatures";
        Log.entering(c, METHOD_NAME);

        replaceWlpProperties(null, "ND", "InstallationManager");

        String[] params1 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass,com.ibm.installExtendedPackage-1.0", "--acceptLicense", "--downloadOnly=none",
                             "--location=" + installRoot + "/usr/download", "--verbose" };
        String[] featureFilesList = { "lib/features/com.ibm.genericCoreFeature.mf", "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf" };
        String[] installParams = new String[] { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense",
                                                "--when-file-exists=ignore" };
        String[] filesList = { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa", "usr/download/com.ibm.genericCoreFeature.esa" };
        String[] downloaded = { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa", "usr/download/com.ibm.installExtendedPackage-1.0.esa" };
        String[] shouldNotBeDownloaded = { "usr/download/com.ibm.genericCoreFeature.esa", "com.ibm.genericCoreFeatureModifyA.esa" };
        try {
            ProgramOutput po;
            //install genericCoreFeatureDependancyOnEsaPass
            po = runFeatureManager(METHOD_NAME, installParams);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFilesList);

            //delete genericCoreFeatureDependancyOnEsaPass.esa and genericCoreFeature.esa
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeature, com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);

            //download genericCoreFeatureDependancyOnEsaPass
            po = runFeatureManager(METHOD_NAME, params1);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I"));
            assertFilesExist(downloaded);
            assertFilesNotExist(shouldNotBeDownloaded);
        } finally {
            // clean up
            resetOriginalWlpProps();
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
            filesList = new String[] { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                                       "lib/features/com.ibm.genericCoreFeature.mf",
                                       "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                                       "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                                       "lib/features/checksums/com.ibm.genericCoreFeature.cs",
                                       "lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs" };
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testInstallDownloadOnlyAllDepWithInstalledDependent() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallDownloadOnlyAllDepInstallDependent";
        Log.entering(c, METHOD_NAME);

        String[] params1 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--downloadOnly=all",
                             "--location=" + installRoot + "/usr/download", "--verbose=debug" };
        String[] featureFilesList = { "lib/features/com.ibm.genericCoreFeature.mf", "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf" };
        String[] installParams = new String[] { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense",
                                                "--when-file-exists=ignore", "--verbose=debug" };
        String[] filesList = { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa", "usr/download/com.ibm.genericCoreFeature.esa" };
        try {
            ProgramOutput po;
            //install genericCoreFeatureDependancyOnEsaPass
            po = runFeatureManager(METHOD_NAME, installParams);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1017I: The feature should have been successfully installed.",
                       po.getStdout().indexOf("CWWKF1017I:") >= 0);
            assertFilesExist(featureFilesList);

            //delete genericCoreFeatureDependancyOnEsaPass.esa and genericCoreFeature.esa
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeature, com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);

            //download genericCoreFeatureDependancyOnEsaPass
            filesList = new String[] { "usr/download/com.ibm.genericCoreFeature.esa", "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa" };
            po = runFeatureManager(METHOD_NAME, params1);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I"));
            assertFilesExist(filesList);
        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
            filesList = new String[] { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                                       "lib/features/com.ibm.genericCoreFeature.mf",
                                       "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                                       "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                                       "lib/features/checksums/com.ibm.genericCoreFeature.cs",
                                       "lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs" };
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInstallDownloadOnlyAllDepWithInstalledDependency() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallDownloadOnlyAllDepWithInstalledDependency";
        Log.entering(c, METHOD_NAME);

        String[] params1 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--downloadOnly=all",
                             "--location=" + installRoot + "/usr/download", "--verbose" };
        String[] featureFilesList = { "lib/features/com.ibm.genericCoreFeature.mf" };
        String[] installParams = new String[] { "install", "com.ibm.genericCoreFeature", "--acceptLicense",
                                                "--when-file-exists=ignore" };
        String[] filesList = { "usr/download/com.ibm.genericCoreFeature.esa" };
        try {
            ProgramOutput po;

            //install genericCoreFeature
            po = runFeatureManager(METHOD_NAME, installParams);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFilesList);

            //delete genericCoreFeature.esa
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeature", filesList);

            //download genericCoreFeatureDependancyOnEsaPass
            filesList = new String[] { "usr/download/com.ibm.genericCoreFeature.esa", "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa" };
            po = runFeatureManager(METHOD_NAME, params1);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I"));
            assertFilesExist(filesList);
        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeature", filesList);
            filesList = new String[] { "lib/features/com.ibm.genericCoreFeature.mf",
                                       "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                                       "lib/features/checksums/com.ibm.genericCoreFeature.cs",
            };
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeature, com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Feature A: genericCoreFeatureDependancyOnEsaPass
     * Feature B: genericCoreFeature
     *
     * @throws Exception
     */
    @Test
    public void testInstallMissingFeatureAndDependent() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallMissingFeatureAndDependent";
        Log.entering(c, METHOD_NAME);

        String[] params1 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense",
                             "--location=" + installRoot + "/usr/download" };

        String[] filesList = { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa",
                               "usr/download/com.ibm.genericCoreFeature.esa" };
        String[] featureFileList = { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                                     "lib/features/com.ibm.genericCoreFeature.mf",
                                     "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                                     "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                                     "lib/features/checksums/com.ibm.genericCoreFeature.cs",
                                     "lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs" };
        try {
            // Install and expect successful installation
            new File(installRoot + "/usr/download").mkdir(); //creating an empty installation folder
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, params1);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFileList);

        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", featureFileList);

        }
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Feature A: genericCoreFeatureDependancyOnEsaPass (Dependent)
     * Feature B: genericCoreFeature (Feature)
     *
     * @throws Exception
     */
    @Test
    public void testInstallMissingDependent() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallMissingDependent";
        Log.entering(c, METHOD_NAME);

        String LocalEsa = "com.ibm.genericCoreFeature.esa";
        server.copyFileToLibertyInstallRoot("/usr/download", "../../publish/wlpDirs/wlp/usr/temp/" + LocalEsa);

        String[] params2 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense",
                             "--location=" + installRoot + "/usr/download" };

        String[] filesList = { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa",
                               "usr/download/com.ibm.genericCoreFeature.esa" };
        String[] featureFileList = { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                                     "lib/features/com.ibm.genericCoreFeature.mf",
                                     "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                                     "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                                     "lib/features/checksums/com.ibm.genericCoreFeature.cs",
                                     "lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs" };
        try {
            // Install and expect successful installation
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, params2);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFileList);

        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", featureFileList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Feature A: genericCoreFeatureDependancyOnEsaPass (Dependent)
     * Feature B: genericCoreFeature (Feature)
     *
     * @throws Exception
     */

    @Test
    public void testInstallMissingFeature() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallMissingFeature";
        Log.entering(c, METHOD_NAME);

        String LocalEsa = "com.ibm.genericCoreFeatureDependancyOnEsaPass.esa";
        server.copyFileToLibertyInstallRoot("/usr/download", "../../publish/wlpDirs/wlp/usr/temp/" + LocalEsa);
        String[] params2 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass", "--acceptLicense", "--location=" + installRoot + "/usr/download" };
        String[] filesList = { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa",
                               "usr/download/com.ibm.genericCoreFeature.esa" };
        String[] featureFileList = { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                                     "lib/features/com.ibm.genericCoreFeature.mf",
                                     "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                                     "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                                     "lib/features/checksums/com.ibm.genericCoreFeature.cs",
                                     "lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs" };

        try {
            // Install and expect successful installation
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, params2);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFileList);

        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", filesList);
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", featureFileList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Feature A: genericCoreFeatureDependancyOnEsaPass (Dependent)
     * Feature B: genericCoreFeature (Feature)
     * Feature C: genericCoreFeatureH (Dependent)
     * Feature D: genericCoreFeatureA (Feature)
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testInstallTwoFeaturesMissingOneDependency() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallTwoFeaturesMissingOneDependency";
        Log.entering(c, METHOD_NAME);

        String[] LocalEsa = { "com.ibm.genericCoreFeatureDependancyOnEsaPass.esa", "com.ibm.genericCoreFeatureH.esa", "com.ibm.genericCoreFeatureA.esa" };
        for (String esa : LocalEsa) {
            server.copyFileToLibertyInstallRoot("/usr/download", "../../publish/wlpDirs/wlp/usr/temp/" + esa);
        }
        String[] params2 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass,com.ibm.genericCoreFeatureH", "--acceptLicense",
                             "--location=" + installRoot + "/usr/download" };
        String[] fileList = { "usr/download/com.ibm.genericCoreFeature.esa", "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa",
                              "usr/download/com.ibm.genericCoreFeatureH.esa", "usr/download/com.ibm.genericCoreFeatureA.esa" };
        String[] featureFileList = { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                                     "lib/features/com.ibm.genericCoreFeature.mf",
                                     "lib/features/com.ibm.genericCoreFeatureH.mf",
                                     "lib/features/com.ibm.genericCoreFeatureA.mf",
                                     "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                                     "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                                     "lib/features/l10n/com.ibm.genericCoreFeatureH.properties",
                                     "lib/features/l10n/com.ibm.genericCoreFeatureA.properties",
                                     "lib/features/checksums/com.ibm.genericCoreFeature.cs",
                                     "lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs",
                                     "lib/features/checksums/com.ibm.genericCoreFeatureH.cs",
                                     "lib/features/checksums/com.ibm.genericCoreFeatureA.cs" };

        try {
            // Install and expect successful installation
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, params2);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFileList);

        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", fileList);
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", featureFileList);
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureH", fileList);
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureH", featureFileList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Feature A: genericCoreFeatureDependancyOnEsaPass (Dependent)
     * Feature B: genericCoreFeature (Feature)
     * Feature C: genericCoreFeatureH (Dependent)
     * Feature D: genericCoreFeatureA (Feature)
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testInstallTwoFeaturesMissingTwoDependency() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallTwoFeaturesMissingTwoDependency";
        Log.entering(c, METHOD_NAME);

        String[] LocalEsa = { "com.ibm.genericCoreFeatureDependancyOnEsaPass.esa", "com.ibm.genericCoreFeatureH.esa" };
        for (String esa : LocalEsa) {
            server.copyFileToLibertyInstallRoot("/usr/download", "../../publish/wlpDirs/wlp/usr/temp/" + esa);
        }
        String[] params2 = { "install", "com.ibm.genericCoreFeatureDependancyOnEsaPass,com.ibm.genericCoreFeatureH", "--acceptLicense",
                             "--location=" + installRoot + "/usr/download" };
        String[] fileList = { "usr/download/com.ibm.genericCoreFeature.esa", "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa",
                              "usr/download/com.ibm.genericCoreFeatureH.esa", "usr/download/com.ibm.genericCoreFeatureA.esa" };
        String[] featureFileList = { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                                     "lib/features/com.ibm.genericCoreFeature.mf",
                                     "lib/features/com.ibm.genericCoreFeatureH.mf",
                                     "lib/features/com.ibm.genericCoreFeatureA.mf",
                                     "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                                     "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                                     "lib/features/l10n/com.ibm.genericCoreFeatureH.properties",
                                     "lib/features/l10n/com.ibm.genericCoreFeatureA.properties",
                                     "lib/features/checksums/com.ibm.genericCoreFeature.cs",
                                     "lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs",
                                     "lib/features/checksums/com.ibm.genericCoreFeatureH.cs",
                                     "lib/features/checksums/com.ibm.genericCoreFeatureA.cs" };

        try {
            // Install and expect successful installation
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, params2);
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(featureFileList);

        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", fileList);
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass", featureFileList);
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureH", fileList);
            deleteFiles(METHOD_NAME, "com.ibm.genericCoreFeatureH", featureFileList);
        }
        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testReapplyEnablementIfix() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String methodName = "testReapplyEnablementIfix";
        String feature = "com.ibm.genericCoreDependancyOnEsaIfixPass";
        String ifix = "9000-wlp-archive-IFTS99999";

        String[] files = { "/lib/features/com.ibm.genericCoreDependancyOnEsaIfixPass.mf",
                           "/lib/com.ibm.genericCoreDependancyOnEsaIfixPass_1.0.0.jar",
                           "/lib/features/l10n/com.ibm.genericCoreDependancyOnEsaIfixPass.properties",
                           "/lib/features/checksums/com.ibm.genericCoreDependancyOnEsaIfixPass.cs" };

        String[] ifixFiles = { "/lib/fixes/9000-wlp-archive-IFTS10001_9.0.0.0.lpmf",
                               "/lib/fixes/9000-wlp-archive-IFTS10001_9.0.0.0.xml" };

        deleteFiles(methodName, feature, files);
        deleteFiles(methodName, ifix, ifixFiles);

        LocalFile backupReadme = server.copyInstallRootFileToTempDir("README.TXT", methodName + "/README.TXT");
        assertTrue("README.TXT backup does not exist: " + backupReadme.getAbsolutePath(), backupReadme.exists());
        Log.info(c, methodName, "backup README.TXT=" + backupReadme.getAbsolutePath());

        try {
            replaceWlpProperties("9.0.0.0", "ND", null);
            String serverInstallRoot = server.getInstallRoot();
            Log.info(c, methodName, "serverInstallRoot=" + serverInstallRoot);
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(), serverInstallRoot + "/lib/fixes", "publish/massiveRepo/files/9000-wlp-archive-IFTS10001_9.0.0.0.lpmf");
            LibertyFileManager.copyFileIntoLiberty(server.getMachine(), serverInstallRoot + "/lib/fixes", "publish/massiveRepo/files/9000-wlp-archive-IFTS10001_9.0.0.0.xml");
            assertFilesExist(ifixFiles);

            ProgramOutput po;
            po = runFeatureManager(methodName, new String[] { "install", feature, "--to=core", "--acceptLicense" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));

            TestUtils.verifyFirstLine(new File(server.getInstallRoot(), "README.TXT"), "IFTS99999");
            assertFilesExist(files);
            assertFilesExist(ifixFiles);
        } finally {
            resetOriginalWlpProps();
            if (backupReadme.exists()) {
                server.deleteFileFromLibertyInstallRoot("README.TXT");
                server.copyFileToLibertyInstallRoot("/tmp/" + methodName + "/README.TXT");
                backupReadme.getParentFile().delete();
            }
            deleteFiles(methodName, feature, files);
            deleteFiles(methodName, ifix, ifixFiles);
        }
    }

    private void testInstallFeatureCollection(String methodName, String features, boolean testDownload) throws Exception {
        replaceWlpProperties(null, "ND", "InstallationManager");

        String feature1 = "com.ibm.genericCoreFeatureDependancyOnEsaPass";
        String feature2 = "com.ibm.genericCoreFeature";
        String feature3 = "com.ibm.genericCoreFeatureModifyA";
        String feature4 = "com.ibm.installExtendedPackage-1.0";

        String[] files1 = { "lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                            "lib/com.ibm.genericCoreFeatureDependancyOnEsaPass_1.0.0.jar",
                            "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                            "lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs" };

        String[] files2 = { "lib/features/com.ibm.genericCoreFeature.mf",
                            "lib/com.ibm.genericCoreFeature_1.0.0.jar",
                            "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                            "lib/features/checksums/com.ibm.genericCoreFeature.cs" };

        String[] files3 = { "lib/features/com.ibm.genericCoreFeatureModifyA.mf",
                            "lib/com.ibm.genericCoreFeatureModifyA_1.0.0.jar",
                            "lib/features/l10n/com.ibm.genericCoreFeatureModifyA.properties",
                            "lib/features/checksums/com.ibm.genericCoreFeatureModifyA.cs" };

        String[] files4 = { "lib/assets/com.ibm.installExtendedPackage-1.0.mf",
                            "lib/assets/l10n/com.ibm.installExtendedPackage-1.0.properties" };

        deleteFiles(methodName, feature1, files1);
        deleteFiles(methodName, feature2, files2);
        deleteFiles(methodName, feature3, files3);
        deleteFiles(methodName, feature4, files4);

        try {
            ProgramOutput po;
            po = runFeatureManager(methodName, new String[] { "install", features, "--to=core", "--acceptLicense" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(files1);
            assertFilesExist(files2);
            assertFilesExist(files3);
            assertFilesExist(files4);
            po = runFeatureManager(methodName, new String[] { "install", features, "--to=core", "--acceptLicense" });
            assertEquals("Expected exit code", 22, po.getReturnCode());
            assertTrue("Expected CWWKF1216I", po.getStdout().contains("CWWKF1216I"));

            deleteFiles(methodName, feature1, files1);
            deleteFiles(methodName, feature2, files2);

            if (testDownload) {
                testDownloadAddonWithMissingRequiredFeatures();
            }

            po = runFeatureManager(methodName, new String[] { "install", features, "--to=core", "--acceptLicense" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("All features were successfully installed."));
            assertFilesExist(files1);
            assertFilesExist(files2);
            assertFilesExist(files3);
            assertFilesExist(files4);
        } finally {
            // clean up
            resetOriginalWlpProps();
            deleteFiles(methodName, feature1, files1);
            deleteFiles(methodName, feature2, files2);
            deleteFiles(methodName, feature3, files3);
            deleteFiles(methodName, feature4, files4);
        }
    }

    private void testDownloadAddonWithMissingRequiredFeatures() throws Exception {
        final String METHOD_NAME = "testDownloadAddonWithMissingRequiredFeatures";
        Log.entering(c, METHOD_NAME);
        File downloadDir = new File(installRoot, "usr/download");
        String[] filesList = { "usr/download/com.ibm.installExtendedPackage-1.0.esa",
                               "usr/download/com.ibm.genericCoreFeatureModifyA.esa",
                               "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa",
                               "usr/download/com.ibm.genericCoreFeature.esa" };

        deleteFiles(METHOD_NAME, "com.ibm.installExtendedPackage-1.0", filesList);

        try {
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME,
                                   new String[] { "install", "com.ibm.installExtendedPackage-1.0", "--acceptLicense", "--downloadOnly",
                                                  "--location=" + downloadDir.getAbsolutePath(), "--verbose=debug" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I"));
            assertFilesExist(new String[] { "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa",
                                            "usr/download/com.ibm.genericCoreFeature.esa" });
            assertFilesNotExist(new String[] { "usr/download/com.ibm.genericCoreFeatureModifyA.esa",
                                               "usr/download/com.ibm.installExtendedPackage-1.0.esa" });
            deleteFiles(METHOD_NAME, "com.ibm.installExtendedPackage-1.0", filesList);

            po = runFeatureManager(METHOD_NAME,
                                   new String[] { "install", "com.ibm.installExtendedPackage-1.0", "--acceptLicense", "--downloadOnly=none",
                                                  "--location=" + downloadDir.getAbsolutePath(), "--verbose=debug" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1500I should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1500I"));
            assertFilesExist(new String[] { "usr/download/com.ibm.installExtendedPackage-1.0.esa" });
            assertFilesNotExist(new String[] { "usr/download/com.ibm.genericCoreFeatureModifyA.esa",
                                               "usr/download/com.ibm.genericCoreFeatureDependancyOnEsaPass.esa",
                                               "usr/download/com.ibm.genericCoreFeature.esa" });
            deleteFiles(METHOD_NAME, "com.ibm.installExtendedPackage-1.0", filesList);

            po = runFeatureManager(METHOD_NAME,
                                   new String[] { "install", "com.ibm.installExtendedPackage-1.0", "--acceptLicense", "--downloadOnly=all",
                                                  "--location=" + downloadDir.getAbsolutePath(), "--verbose=debug" });
            assertEquals("Expected exit code", 0, po.getReturnCode());
            assertTrue("CWWKF1501I should have been printed:\r\n" + po.getStdout(),
                       po.getStdout().contains("CWWKF1501I"));
            assertFilesExist(filesList);
        } finally {
            // clean up
            deleteFiles(METHOD_NAME, "com.ibm.installExtendedPackage-1.0", filesList);
        }
    }

    @Test
    public void testInstallFeatureCollection() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallFeatureCollection";
        Log.entering(c, METHOD_NAME);
        testInstallFeatureCollection(METHOD_NAME, "com.ibm.installExtendedPackage-1.0", true);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Installs feature with proxy server support enabled with an invalid proxy hostname
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testInstallFeature_WithInvalidProxyHost() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallFeature_WithInvalidProxyHost";
        Log.entering(c, METHOD_NAME);

        //Create repositories properties file in wlp/etc directory
        File propsFile = new File(installRoot + "/etc", "repositories.properties");
        boolean success = propsFile.createNewFile();
        Log.info(c, METHOD_NAME, "Repositories properties file created= " + success);

        //Store proxy properties with invalid proxy hostname in file
        FileOutputStream fOut = null;
        Properties proxyInfo = new Properties();
        try {
            fOut = new FileOutputStream(propsFile);
            proxyInfo.setProperty("proxyHost", "unknownproxyhost123.testproxy123.com");
            proxyInfo.setProperty("proxyPort", "8080");
            proxyInfo.setProperty("proxyUser", "testUser");
            proxyInfo.setProperty("proxyUserPassword", "{xor}KzosKw8oOw==");
            proxyInfo.store(fOut, null);
        } finally {
            InstallUtils.close(fOut);
        }

        String[] param1s = { "install", "webCacheMonitor-1.0", "--acceptLicense" };

        String[] fileLists = { "/lib/features/com.ibm.websphere.appserver.webCacheMonitor-1.0.mf" };
        deleteFiles(METHOD_NAME, "webCacheMonitor-1.0", fileLists);

        try {
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, param1s);
            assertEquals("Expected exit code", 21, po.getReturnCode());
            assertTrue("Should throw exception with message CWWKF1370E", po.getStdout().contains("CWWKF1370E"));
        } finally {
            //clean up properties file
            server.deleteFileFromLibertyInstallRoot("etc/" + propsFile.getName());
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInstallFeatureCollectionWithFeatureBefore() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallFeatureCollectionWithFeatureBefore";
        Log.entering(c, METHOD_NAME);
        testInstallFeatureCollection(METHOD_NAME, "com.ibm.genericCoreFeatureDependancyOnEsaPass,com.ibm.installExtendedPackage-1.0", false);
        Log.exiting(c, METHOD_NAME);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInstallFeatureCollectionWithFeatureAfter() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallFeatureCollectionWithFeatureAfter";
        Log.entering(c, METHOD_NAME);
        testInstallFeatureCollection(METHOD_NAME, "com.ibm.installExtendedPackage-1.0,com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Basic Addon install and uninstall
     * A = "installExtendedPackage-1.0", B = "genericCoreFeatureDependancyOnEsaPass", C = "genericCoreFeatureModifyA"
     * Addon A depends on Feature B and C
     *
     * @throws Exception
     */
    @Test
    public void testAddonInstallUninstall() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testAddonInstallUninstall";
        Log.entering(c, METHOD_NAME);

        replaceWlpProperties(originalWlpVersion, "ND", "InstallationManager");

        String[] param1s = { "install", "installExtendedPackage-1.0", "--acceptLicense", "--when-file-exists=replace" };

        String[] fileLists = { "/lib/assets/com.ibm.installExtendedPackage-1.0.mf", "/lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                               "/lib/features/com.ibm.genericCoreFeatureModifyA.mf" };
        deleteFiles(METHOD_NAME, "installExtendedPackage-1.0", fileLists);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, param1s);
        assertEquals("ERROR: Exit code is not 0", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertFilesExist(fileLists);

        // Test addon uninstall
        String[] param2s = { "uninstall", "installExtendedPackage-1.0", "--noPrompts" };
        po = runFeatureManager(METHOD_NAME, param2s);
        assertEquals("ERROR: Exit code is not 0", 0, po.getReturnCode());
        assertTrue("ERROR: The feature installExtendedPackage-1.0 is not successfully uninstalled.",
                   po.getStdout().indexOf("One or more features uninstalled successfully: installExtendedPackage-1.0") >= 0);
        assertFilesNotExist(fileLists);

        deleteFiles(METHOD_NAME, "installExtendedPackage-1.0", fileLists);

        resetOriginalWlpProps();
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Uninstall addon when some of sub-features are still required by other features
     * A = "installExtendedPackage-1.0", B = "genericCoreFeatureDependancyOnEsaPass", C = "genericCoreFeatureModifyA", D = "genericCoreFeatureDependancyOnModifyA"
     * 1. Install Addon A which depends on Feature B and C
     * 2. Install Feature D which also depends on C
     * 3. Uninstall Addon A. A and B should be uninstalled, but C should be kept since it is required by D
     * 4. Uninstall D. C and D should both be removed
     *
     * @throws Exception
     */
    @Test
    public void testAddonInstallUninstallWhenDependentFeatureRequired() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testAddonInstallUninstallWhenDependentFeatureRequired";
        Log.entering(c, METHOD_NAME);

        replaceWlpProperties(originalWlpVersion, "ND", "InstallationManager");

        String[] param1s = { "install", "installExtendedPackage-1.0", "--acceptLicense", "--when-file-exists=replace" };

        String[] fileLists = { "/lib/assets/com.ibm.installExtendedPackage-1.0.mf", "/lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                               "/lib/features/com.ibm.genericCoreFeatureModifyA.mf" };
        deleteFiles(METHOD_NAME, "installExtendedPackage-1.0", fileLists);

        String[] param2s = { "install", "genericCoreFeatureDependancyOnModifyA", "--acceptLicense", "--when-file-exists=replace" };

        String[] fileLists2 = { "/lib/features/com.ibm.genericCoreFeatureDependancyOnModifyA.mf", "/lib/features/com.ibm.genericCoreFeatureModifyA.mf" };
        deleteFiles(METHOD_NAME, "genericCoreFeatureDependancyOnModifyA", fileLists2);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, param1s);
        assertEquals("ERROR: Exit code is not 0", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertFilesExist(fileLists);

        po = runFeatureManager(METHOD_NAME, param2s);
        assertEquals("ERROR: Exit code is not 0", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));
        assertFilesExist(fileLists2);

        // Test addon uninstall
        String[] param3s = { "uninstall", "installExtendedPackage-1.0", "--noPrompts" };
        po = runFeatureManager(METHOD_NAME, param3s);
        assertEquals("ERROR: Exit code is not 0", 0, po.getReturnCode());
        assertTrue("ERROR: The feature installExtendedPackage-1.0 is not successfully uninstalled.",
                   po.getStdout().indexOf("One or more features uninstalled successfully: installExtendedPackage-1.0") >= 0);
        String[] fileLists3 = { "/lib/assets/com.ibm.installExtendedPackage-1.0.mf", "/lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf" };
        String[] fileLists4 = { "/lib/features/com.ibm.genericCoreFeatureModifyA.mf" };
        assertFilesNotExist(fileLists3);
        assertFilesExist(fileLists4);

        String[] param4s = { "uninstall", "genericCoreFeatureDependancyOnModifyA", "--noPrompts" };
        po = runFeatureManager(METHOD_NAME, param4s);
        assertEquals("ERROR: Exit code is not 0", 0, po.getReturnCode());
        assertTrue("ERROR: The feature genericCoreFeatureDependancyOnModifyA is not successfully uninstalled.",
                   po.getStdout().indexOf("One or more features uninstalled successfully: genericCoreFeatureDependancyOnModifyA") >= 0);
        String[] fileLists5 = { "/lib/features/com.ibm.genericCoreFeatureDependancyOnModifyA.mf" };
        assertFilesNotExist(fileLists5);

        // Clean up
        deleteFiles(METHOD_NAME, "installExtendedPackage-1.0", fileLists);
        deleteFiles(METHOD_NAME, "genericCoreFeatureDependancyOnModifyA", fileLists);

        resetOriginalWlpProps();
        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Uninstall a feature that is dependent by other features.
     * A = "installExtendedPackage-1.0", B = "genericCoreFeatureModifyA", C = "genericCoreFeatureDependancyOnModifyA"
     * Addon A and feature C both depends on feature B
     *
     * @throws Exception
     */
    @Test
    public void testFeatureUninstallReturnListOfDependentFeaturesOnBlock() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testFeatureUninstallReturnListOfDependentFeaturesOnBlock";
        Log.entering(c, METHOD_NAME);

        replaceWlpProperties(originalWlpVersion, "ND", "InstallationManager");

        String addonA = "installExtendedPackage-1.0";
        String featureB = "genericCoreFeatureModifyA";
        String featureC = "genericCoreFeatureDependancyOnModifyA";

        String[] param1s = { "install", addonA, "--acceptLicense", "--when-file-exists=replace" };
        String[] param2s = { "uninstall", addonA, "--noPrompts" };
        String[] param3s = { "install", featureC, "--acceptLicense", "--when-file-exists=replace" };
        String[] param4s = { "uninstall", featureC, "--noPrompts" };
        String[] param5s = { "uninstall", featureB, "--noPrompts" };
        String[] fileLists = { "/lib/assets/com.ibm.installExtendedPackage-1.0.mf", "/lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf",
                               "/lib/features/com.ibm.genericCoreFeatureModifyA.mf" };
        String[] fileLists2 = { "/lib/features/com.ibm.genericCoreFeatureDependancyOnModifyA.mf", "/lib/features/com.ibm.genericCoreFeatureModifyA.mf" };
        deleteFiles(METHOD_NAME, "installExtendedPackage-1.0", fileLists);

        ProgramOutput po;
        po = runFeatureManager(METHOD_NAME, param1s);
        assertEquals("ERROR: Exit code is not 0.", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));

        assertFilesExist(fileLists);

        po = runFeatureManager(METHOD_NAME, param3s);
        assertEquals("ERROR: Exit code is not 0.", 0, po.getReturnCode());
        assertTrue("Successfully installed message should have been printed:\r\n" + po.getStdout(),
                   po.getStdout().contains("All features were successfully installed."));

        assertFilesExist(fileLists2);

        po = runFeatureManager(METHOD_NAME, param5s);
        assertTrue(
                   "ERROR: The feature installExtendedPackage-1.0 is not successfully uninstalled.",
                   po.getStdout().indexOf("[genericCoreFeatureModifyA] is required by: genericCoreFeatureDependancyOnModifyA.") >= 0);

        po = runFeatureManager(METHOD_NAME, param4s);
        assertEquals("ERROR: Exit code is not 0.", 0, po.getReturnCode());

        po = runFeatureManager(METHOD_NAME, param2s);
        assertEquals("ERROR: Exit code is not 0.", 0, po.getReturnCode());

        assertFilesNotExist(fileLists);
        assertFilesNotExist(fileLists2);

        deleteFiles(METHOD_NAME, "installExtendedPackage-1.0", fileLists);
        deleteFiles(METHOD_NAME, "genericCoreFeatureDependancyOnModifyA", fileLists2);

        resetOriginalWlpProps();

        Log.exiting(c, METHOD_NAME);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInstallExtColonNonFeature() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallExtColonNonFeature";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "install", "cik.ext.product1:SampleX", "--acceptLicense", "--verbose=debug" };

        server.deleteDirectoryFromLibertyInstallRoot("/../cik");
        String extensionPath = server.getInstallRoot() + "/../cik";
        createExtensionDirs("/cik.ext.product1", extensionPath);

        try {
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, param1s);
            assertEquals("Expected exit code", 21, po.getReturnCode());
            /*
             * featureManager does not support usr:feature and ext:feature syntax *
             * assertTrue("CWWKF1298E is expected", po.getStdout().contains("CWWKF1298E"));
             * so verified as feature not found
             */
            assertTrue("CWWKF1203E is expected", po.getStdout().contains("CWWKF1203E"));
        } finally {
            server.deleteDirectoryFromLibertyInstallRoot("etc/extensions");
            Log.exiting(c, METHOD_NAME);
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInstallUnknownExtColonFeature() throws Exception {
        final String METHOD_NAME = "testInstallUnknownExtColonFeature";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "install", "unknown.ext:testesa1", "--acceptLicense", "--verbose=debug" };

        try {
            ProgramOutput po;
            po = runFeatureManager(METHOD_NAME, param1s);
            assertEquals("Expected exit code", 21, po.getReturnCode());

            /*
             * featureManager does not support usr:feature and ext:feature syntax *
             * assertTrue("CWWKF1297E is expected", po.getStdout().contains("CWWKF1297E"));
             * assertTrue("unknown.ext is expected in the error message", po.getStdout().contains("unknown.ext"));
             * assertTrue("testesa1 is expected in the error message", po.getStdout().contains("CWWKF1297E"));
             * so verified as feature not found
             */
            assertTrue("CWWKF1203E is expected", po.getStdout().contains("CWWKF1203E"));
        } finally {
            Log.exiting(c, METHOD_NAME);
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInstallExtColonFeature() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallExtColonFeature";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "install", "cik.ext.product1:testesa1", "--to=usr", "--acceptLicense",
                             "--when-file-exists=replace", "--verbose=debug" };

        server.deleteDirectoryFromLibertyInstallRoot("/../cik");
        String extensionPath = server.getInstallRoot() + "/../cik";
        createExtensionDirs("/cik.ext.product1", extensionPath);

        String[] filesList = { "/../cik/cik.ext.product1/lib/features/testesa1.mf",
                               "/../cik/cik.ext.product1/lib/testesa1_1.0.0.jar",
                               "/../cik/cik.ext.product1/bin/testesa1.bat" };

        ProgramOutput po;
        try {
            po = runFeatureManager(METHOD_NAME, param1s);
            /*
             * featureManager does not support usr:feature and ext:feature syntax *
             * assertEquals("Expected exit code", 0, po.getReturnCode());
             * assertTrue("CWWKF1017I: The feature has been successfully installed.", po.getStdout().indexOf("CWWKF1017I:") >= 0);
             * assertFilesExist(filesList);
             * so verified as feature not found
             */
            assertTrue("CWWKF1203E is expected", po.getStdout().contains("CWWKF1203E"));
        } finally {
            server.deleteDirectoryFromLibertyInstallRoot("../cik");
            Log.exiting(c, METHOD_NAME);
        }
    }

    @Mode(TestMode.FULL)
    @Test
    public void testInstallUsrColonFeature() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        final String METHOD_NAME = "testInstallUsrColonFeature";
        Log.entering(c, METHOD_NAME);

        String[] param1s = { "install", "usr:testesa1", "--to=cik.ext.product1", "--acceptLicense", "--when-file-exists=replace", "--verbose=debug" };

        String[] filesList = { "/usr/extension/lib/features/testesa1.mf", "/usr/extension/bin/testesa1.bat", "/usr/extension/lib/testesa1_1.0.0.jar" };
        deleteFiles(METHOD_NAME, "testesa1", filesList);

        ProgramOutput po;
        try {
            po = runFeatureManager(METHOD_NAME, param1s);
            /*
             * featureManager does not support usr:feature and ext:feature syntax *
             * assertEquals("Expected exit code", 0, po.getReturnCode());
             * assertTrue("CWWKF1017I: The feature has been successfully installed.",
             * po.getStdout().indexOf("CWWKF1017I:") >= 0);
             * assertFilesExist(filesList);
             * so verified as feature not found
             */
            assertTrue("CWWKF1203E is expected", po.getStdout().contains("CWWKF1203E"));
        } finally {
            deleteFiles(METHOD_NAME, "testesa1", filesList);
            Log.exiting(c, METHOD_NAME);
        }
    }
}
