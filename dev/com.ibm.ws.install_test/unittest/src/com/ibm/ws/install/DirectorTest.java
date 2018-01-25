/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.internal.Director;

public class DirectorTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static File imageDir;
    private static File tempDir;
    private static String orginialTmpDir;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        imageDir = new File("build/unittest/wlpDirs/developers/wlp").getAbsoluteFile();
        System.out.println("setUpBeforeClass() imageDir set to " + imageDir);
        if (imageDir == null || !imageDir.exists())
            throw new IllegalArgumentException("Test requires an existing root directory, but it could not be found: " + imageDir.getAbsolutePath());

        tempDir = new File("build/unittest/tmp");
        if (tempDir == null || !tempDir.exists())
            throw new IllegalArgumentException("Test requires an existing temp directory, but it could not be found: " + tempDir.getAbsolutePath());

        orginialTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", tempDir.getAbsolutePath());
        System.out.println("setUpBeforeClass() java.io.tmpdir set to " + System.getProperty("java.io.tmpdir"));
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.setProperty("java.io.tmpdir", orginialTmpDir);
        System.out.println("tearDownAfterClass() java.io.tmpdir set to " + System.getProperty("java.io.tmpdir"));
    }

    @Test
    public void testDirector_installFeaturesWithNullOrEmpty() {
        Director d = new Director(imageDir);
        try {
            d.installFeatures(null, "", true, "userid", "password");
        } catch (Exception e) {
            assertEquals("Director.installFeatures()", "CWWKF1200E: The provided features list is null or empty.", e.getMessage());
        }
        try {
            d.installFeatures(new ArrayList<String>(0), "", true, "userid", "password");
        } catch (Exception e) {
            assertEquals("Director.installFeatures()", "CWWKF1200E: The provided features list is null or empty.", e.getMessage());
        }
    }

    @Test
    public void testDirector_getExtensionNames() {
        Director d = new Director(imageDir);
        File extensionDir = new File(imageDir, "etc/extensions");
        int expectedExtension = 0;
        if (extensionDir.exists()) {
            String[] extensionProps = extensionDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".properties");
                }
            });
            expectedExtension = extensionProps.length;
        }
        assertEquals("Director.getExtensionNames()", expectedExtension, d.getExtensionNames().size());
    }

    /**
     * Uninstall a feature with ibm shortname
     */
    @Test
    public void testDirector_uninstallFeatureWithShortName() throws Exception {
        Director d = new Director(imageDir);
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("appSecurity-1.0");
        d.uninstallFeatures(features, null);
    }

    /**
     * Uninstall a feature with ibm symbolic name
     */
    @Test
    public void testDirector_uninstallFeatureWithSymName() throws Exception {
        Director d = new Director(imageDir);
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("com.ibm.websphere.appserver.appSecurity-1.0");
        d.uninstallFeatures(features, null);
    }

    /**
     * Uninstall feature which is required by another feature
     */
    @Test
    public void testDirector_uninstallMultipleFeatures() {
        Director d = new Director(imageDir);
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("ldapRegistry-3.0");
        try {
            d.uninstallFeaturesPrereqChecking(features, false, false);
            fail("Director.uninstallFeatures() didn't throw exception.");
        } catch (Exception e) {
            assertTrue("CWWKF1263E: Uninstall feature which is required by another feature.", e.getMessage().contains("CWWKF1263E"));
        }
    }

    /**
     * Uninstall an invalid feature
     */
    @Test
    public void testDirector_uninstallInvalidFeature() {
        Director d = new Director(imageDir);
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("xxx");
        try {
            d.uninstallFeaturesPrereqChecking(features, false, false);
            fail("Director.uninstallFeatures() didn't throw exception.");
        } catch (Exception e) {
            assertEquals("Uninstall an invalid feature.", "CWWKF1207E: The feature xxx is not installed.", e.getMessage());
        }
    }

    /**
     * Uninstall multiple invalid features
     */
    @Test
    public void testDirector_uninstallInvalidFeatures() {
        Director d = new Director(imageDir);
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("xxx");
        features.add("appSecurity-1.0");
        try {
            d.uninstallFeaturesPrereqChecking(features, false, false);
            fail("Director.uninstallFeatures() didn't throw exception.");
        } catch (Exception e) {
            assertEquals("Uninstall an invalid feature.", "CWWKF1207E: The feature xxx is not installed.", e.getMessage());
        }
    }

    @Test
    public void testDirector_uninstallInvalidFix() {
        Director d = new Director(imageDir);
        ArrayList<String> fixes = new ArrayList<String>(1);
        fixes.add("xxx");
        try {
            d.uninstallFix(fixes);
        } catch (Exception e) {
            assertEquals("Uninstall an invalid fix xxx.", "CWWKF1209E: Fix xxx is not installed.", e.getMessage());
        }
    }

    @Test
    public void testDirector_uninstallFailSupersededFixWithCommonFiles() {
        Director d = new Director(imageDir);
        ArrayList<String> fixes = new ArrayList<String>(1);
        fixes.add("8550-wlp-archive-IFPM0002");
        try {
            d.uninstallFix(fixes);
        } catch (Exception e) {
            assertEquals("Uninstall a superseded fix 8550-wlp-archive-IFPM0002 with common files(Expected to fail)",
                         "CWWKF1210E: Fix 8550-wlp-archive-IFPM0002 cannot be uninstalled.",
                         e.getMessage());
        }
    }

    @Test
    public void testDirector_uninstallNonSupersededFixWithCommonFiles() throws Exception {
        Director d = new Director(imageDir);
        ArrayList<String> fixes = new ArrayList<String>(1);
        fixes.add("8550-wlp-archive-IFPM0003");
        d.uninstallFix(fixes);
    }

    @Test
    public void testDirector_uninstallSupersededFixWithNoCommonFiles() throws Exception {
        Director d = new Director(imageDir);
        ArrayList<String> fixes = new ArrayList<String>(1);
        fixes.add("8550-wlp-archive-IFPM0001");
        d.uninstallFix(fixes);
    }

    @Test
    public void testDirector_getFeatureLicenseWithNullOrEmpty() throws Exception {
        Director d = new Director(imageDir);
        assertTrue(d.getFeatureLicense(null, Locale.ENGLISH, "userid", "password").isEmpty());
        assertTrue(d.getFeatureLicense(new ArrayList<String>(0), Locale.ENGLISH, "userid", "password").isEmpty());
    }

    @Test
    public void testDirector_uninstallFailMultipleFixesWithSupersededFix() {
        Director d = new Director(imageDir);
        ArrayList<String> fixes = new ArrayList<String>(2);
        fixes.add("8550-wlp-archive-IFPM0001");
        fixes.add("8550-wlp-archive-IFPM0002");
        try {
            d.uninstallFix(fixes);
        } catch (Exception e) {
            assertEquals("Uninstall a superseded fix 8550-wlp-archive-IFPM0002 with common files(Expected to fail)",
                         "CWWKF1210E: Fix 8550-wlp-archive-IFPM0002 cannot be uninstalled.",
                         e.getMessage());
        }
    }

    @Test
    public void testDirector_uninstallAllMultipleFixes() throws Exception {
        Director d = new Director(imageDir);
        ArrayList<String> fixes = new ArrayList<String>(2);
        fixes.add("8550-wlp-archive-IFPM0001");
        fixes.add("8550-wlp-archive-IFPM0002");
        fixes.add("8550-wlp-archive-IFPM0003");
        d.uninstallFix(fixes);
    }

    @Test
    public void testDirector_installFeatureESAFileNotExist() {
        Director d = new Director(imageDir);
        try {
            d.installFeature("unknown", "", true);
            fail("Director.installFeature() did not throw exception");
        } catch (InstallException e) {
            assertEquals("Director.installFeature() should throw exception",
                         "CWWKF1009E: The file unknown does not exist.",
                         e.getMessage());
        }
    }

    @Test
    public void testDirector_installLocalFeatureESAInvalidURL() {
        Director d = new Director(imageDir);
        try {
            d.installFeature("htp://unknown", "", true);
            fail("Director.installFeature() did not throw exception");
        } catch (InstallException e) {
            if (!e.getMessage().contains("CWWKF1009E")) {
                outputMgr.failWithThrowable("testDirector_installLocalFeatureESAInvalidURL", e);
            }
        }
    }

    @Test
    public void testDirector_installLocalFeatureESAURLNotExist() {
        Director d = new Director(imageDir);
        try {
            d.installFeature("http://www.cik.unknown.com/cik.user.esa", "", true);
            fail("Director.installFeature() did not throw exception");
        } catch (InstallException e) {
            // For some platforms which may fail at create temp file before download, check CWWKF1008E too
            if (!e.getMessage().contains("CWWKF1008E") && !e.getMessage().contains("CWWKF1007E")) {
                outputMgr.failWithThrowable("testDirector_installLocalFeatureESAURLNotExist", e);
            }
        }
    }

    @Test
    public void testDirector_installLocalFeatureFailedCreateTemp() {
        Director d = new Director(imageDir);
        System.out.println("testDirector_installLocalFeatureFailedCreateTemp() java.io.tmpdir is " + System.getProperty("java.io.tmpdir"));
        System.setProperty("java.io.tmpdir", "./invalidPath");
        System.out.println("testDirector_installLocalFeatureFailedCreateTemp() java.io.tmpdir set to " + System.getProperty("java.io.tmpdir"));
        try {
            d.installFeature("http://www.cik.unknown.com/cik.user.esa", "", true);
            fail("Director.installFeature() did not throw exception");
        } catch (InstallException e) {
            // For some platforms which cannot set the java.io.tmpdir, check CWWKF1007E
            if (!e.getMessage().contains("CWWKF1008E") && !e.getMessage().contains("CWWKF1007E")) {
                outputMgr.failWithThrowable("testDirector_installLocalFeatureFailedCreateTemp", e);
            }
        } finally {
            System.setProperty("java.io.tmpdir", tempDir.getAbsolutePath());
            System.out.println("testDirector_installLocalFeatureFailedCreateTemp() java.io.tmpdir finally set to " + System.getProperty("java.io.tmpdir"));
            d.cleanUp();
        }
    }
}
