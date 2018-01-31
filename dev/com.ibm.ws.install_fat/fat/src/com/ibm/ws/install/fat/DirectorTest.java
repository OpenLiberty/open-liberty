package com.ibm.ws.install.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Test;

import test.utils.TestUtils;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.InstallConstants.ExistsAction;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.install.internal.Director;
import com.ibm.ws.install.internal.InstallUtils;

public class DirectorTest extends InstallFATTest {

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        InstallFATTest.tearDownAfterClass();
        File[] cleanupFiles =
                        new File[] {
                                    new File(TestUtils.wlpUserLibDir, "cik.usertest_1.0.0.jar"),
                                    new File(TestUtils.wlpUserLibDir, "cik.usertest.dependency_1.0.0.jar"),
                                    new File(TestUtils.wlpUserFeaturesDir, "cik.usertest.mf"),
                                    new File(TestUtils.wlpUserFeaturesDir, "cik.usertest.dependency.mf"),
                                    new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFTS10001_9.0.0.0.lpmf"),
                                    new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFTS10001_9.0.0.0.xml"),
                                    new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFTS10001_9.0.0.0.zip"),
                                    new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar"),
                                    new File(TestUtils.wlpUserBinDir, "testesa1.bat"),
                                    new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf"),
                                    new File(TestUtils.wlpUserBinDir, "testesa3.bat"),
                                    new File(TestUtils.wlpUserFeaturesDir, "testesa3.mf")
                        };
        for (File f : cleanupFiles) {
            InstallUtils.delete(f);
        }
        TestUtils.cleanLogger();
    }

    private void initializeTestCase(String testName) {
        Log.info(DirectorTest.class, testName, "***********************************************************************************************");
        Log.info(DirectorTest.class, testName, "             " + testName + " " + TestUtils.currentTime());
        Log.info(DirectorTest.class, testName, "***********************************************************************************************");
    }

    private Director getDirector() {
        Director d = new Director(imageDir);
        if (TestUtils.noLogger())
            d.enableConsoleLog(Level.FINEST, true);
        return d;
    }

    @Test
    public void testDirector_installUnavailableFix() {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "_install_uninstall_multiple_user_features";
        initializeTestCase(testCaseName);

        Director d = getDirector();
        try {
            List<String> ifixes = new ArrayList<String>(1);
            ifixes.add("UnavailableFix");
            d.installFixes(ifixes, "userid", "password");
            fail("Director.installFixes() didn't throw exception.");
        } catch (Exception e) {
            assertTrue("Expected " + "< CWWKF1204E: Unable to obtain the following interim fixes: UnavailableFix > but was < " + e.getMessage() + " >",
                       e.getMessage().contains("CWWKF1204E: Unable to obtain the following interim fixes: UnavailableFix"));
        }
    }

    //Disabled for not using java tmp to download
    //@Test
    public void testDirector_failedToDownload() {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "_failedToDownload";
        initializeTestCase(testCaseName);

        String orginialTmp = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", "/../invalidPath");
        Log.info(DirectorTest.class, testCaseName, "java.io.tmpdir set to " + System.getProperty("java.io.tmpdir"));
        System.out.println("java.io.tmpdir set to " + System.getProperty("java.io.tmpdir"));
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("test", "esa");
            Log.info(DirectorTest.class, testCaseName, "test case is skipped because a temp file can be created: " + tmpFile.getAbsolutePath());
            InstallUtils.delete(tmpFile);
            System.setProperty("java.io.tmpdir", orginialTmp);
            Log.info(DirectorTest.class, testCaseName, "java.io.tmpdir skiptest set to " + System.getProperty("java.io.tmpdir"));
            return;
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }

        try {
            Director d = getDirector();
            ArrayList<String> features = new ArrayList<String>(1);
            features.add("com.ibm.genericCoreFeature");
            d.installFeatures(features, "usr", true, null, null);
            fail("InstallUtils.download() did not throw exception.");
        } catch (Exception e) {
            Log.info(DirectorTest.class, testCaseName, e.getMessage());
            e.printStackTrace(System.out);
            assertTrue("InstallUtils.download()", e.getMessage().contains("CWWKF1283E"));
            assertEquals("InstallUtils.download()", "java.io.IOException", e.getCause().getClass().getName());
        } finally {
            System.setProperty("java.io.tmpdir", orginialTmp);
            Log.info(DirectorTest.class, testCaseName, "java.io.tmpdir finally set to " + System.getProperty("java.io.tmpdir"));
        }
    }

    @Test
    public void testDirector_getFeatureMissingDependent() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "_getFeatureMissingDependent";
        initializeTestCase(testCaseName);

        Director d = getDirector();
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("genericCoreFeatureDependancyOnEsaFail");
        try {
            d.getFeatureLicense(features, Locale.ENGLISH, null, null);
            fail("Director.getFeatureLicense() did not throw exception.");
        } catch (InstallException e) {
            assertTrue("should contain CWWKF1221E",
                       e.getMessage().contains("CWWKF1221E"));
        }
    }

    //Disabled for RTC 136527, change esa used
    @Test
    public void testDirector_getFeatureLicense() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "_getFeatureLicensesIfNotAccepted";
        initializeTestCase(testCaseName);

        Director d = getDirector();
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("testesa1");

        assertTrue("Director.getFeatureLicense should return empty collection", d.getFeatureLicense(features, Locale.ENGLISH, null, null).isEmpty());

        features = new ArrayList<String>(1);
        features.add("unknown");
        try {
            d.getFeatureLicense(features, Locale.ENGLISH, null, null);
            fail("Director.getFeatureLicense() did not throw exception.");
        } catch (InstallException e) {
            assertEquals("featuresNeedAcceptLicense()",
                         "CWWKF1203E: Unable to obtain the following features: unknown. Ensure that the features are valid.",
                         e.getMessage());
        }
    }

    @Test
    public void testDirector_install_uninstall_user_features_check_dependency() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "_install_uninstall_user_features_check_dependency";
        initializeTestCase(testCaseName);

        InstallUtils.delete(new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar"));
        InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa1.bat"));
        InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa2.bat"));
        InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf"));
        InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa2.mf"));

        File[] existingFiles = tempDir.listFiles();
        int numOfExistingFiles = existingFiles == null ? 0 : existingFiles.length;
        Log.info(DirectorTest.class, testCaseName, "tempDir has " + numOfExistingFiles + " files");
        Director d = getDirector();
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("testesa1");
        features.add("testesa2");
        d.installFeatures(features, "usr", true, null, null);

        File[] afterDownloadedFiles = tempDir.listFiles();
        int numOfAfterDownloadedFiles = afterDownloadedFiles == null ? 0 : afterDownloadedFiles.length;
        Log.info(DirectorTest.class, testCaseName, "after downloaded, tempDir has " + numOfAfterDownloadedFiles + " files");

        try {
            d.install(ExistsAction.replace, false, false);
            assertTrue("testesa1_1.0.0.jar should be created", new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar").exists());
            assertTrue("testesa1.bat should be created", new File(TestUtils.wlpUserBinDir, "testesa1.bat").exists());
            assertTrue("testesa2.bat should be created", new File(TestUtils.wlpUserBinDir, "testesa2.bat").exists());
            assertTrue("testesa1.mf should be created", new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf").exists());
            assertTrue("testesa2.mf should be created", new File(TestUtils.wlpUserFeaturesDir, "testesa2.mf").exists());
            d.cleanUp();
            Log.info(DirectorTest.class, testCaseName, "testesa1 and testesa2 are installed.");
            ArrayList<String> uninstallFeatures = new ArrayList<String>(1);
            uninstallFeatures.add("testesa1");
            d.uninstallFeatures(uninstallFeatures, null);
            d.uninstall(true, null, null);
            assertFalse("testesa1.bat should be removed", new File(TestUtils.wlpUserBinDir, "testesa1.bat").exists());
            assertFalse("testesa1.mf should be removed", new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf").exists());
            assertTrue("testesa1_1.0.0.jar should not be removed.", new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar").exists());
            Log.info(DirectorTest.class, testCaseName, "testesa1 is uninstalled.  Shared library is left behind.");
            d.cleanUp();
            File[] afterCleanupFiles = tempDir.listFiles();
            int numOfAfterCleanupFiles = afterCleanupFiles == null ? 0 : afterCleanupFiles.length;
            Log.info(DirectorTest.class, testCaseName, "after cleaned up, tempDir has " + numOfAfterCleanupFiles + " files");
            assertEquals("testDirector_install: expected cleaned up", numOfExistingFiles, (afterDownloadedFiles == null ? 0 : afterCleanupFiles.length));
        } finally {
            InstallUtils.delete(new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar"));
            InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa1.bat"));
            InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa2.bat"));
            InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf"));
            InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa2.mf"));
        }
    }

    @Test
    public void testDirector_install_uninstall_multiple_user_features() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "_install_uninstall_multiple_user_features";
        initializeTestCase(testCaseName);

        InstallUtils.delete(new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar"));
        InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa1.bat"));
        InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa2.bat"));
        InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa3.bat"));
        InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa4.bat"));
        InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf"));
        InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa2.mf"));
        InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa3.mf"));
        InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa4.mf"));

        try {
            File[] existingFiles = tempDir.listFiles();
            int numOfExistingFiles = existingFiles == null ? 0 : existingFiles.length;
            Log.info(DirectorTest.class, testCaseName, "tempDir has " + numOfExistingFiles + " files");
            Director d = getDirector();
            ArrayList<String> features = new ArrayList<String>(1);
            features.add("testesa3");
            features.add("testesa4");
            d.installFeatures(features, "usr", true, null, null);

            File[] afterDownloadedFiles = tempDir.listFiles();
            int numOfAfterDownloadedFiles = afterDownloadedFiles == null ? 0 : afterDownloadedFiles.length;
            Log.info(DirectorTest.class, testCaseName, "after downloaded, tempDir has " + numOfAfterDownloadedFiles + " files");

            d.install(ExistsAction.replace, false, false);
            assertTrue("testesa1_1.0.0.jar should be created", new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar").exists());
            assertTrue("testesa1.bat should be created", new File(TestUtils.wlpUserBinDir, "testesa1.bat").exists());
            assertTrue("testesa2.bat should be created", new File(TestUtils.wlpUserBinDir, "testesa2.bat").exists());
            assertTrue("testesa3.bat should be created", new File(TestUtils.wlpUserBinDir, "testesa3.bat").exists());
            assertTrue("testesa4.bat should be created", new File(TestUtils.wlpUserBinDir, "testesa4.bat").exists());

            assertTrue("testesa1.mf should be created", new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf").exists());
            assertTrue("testesa2.mf should be created", new File(TestUtils.wlpUserFeaturesDir, "testesa2.mf").exists());
            assertTrue("testesa3.mf should be created", new File(TestUtils.wlpUserFeaturesDir, "testesa3.mf").exists());
            assertTrue("testesa4.mf should be created", new File(TestUtils.wlpUserFeaturesDir, "testesa4.mf").exists());

            Log.info(DirectorTest.class, testCaseName, "testesa1, testesa2, testesa3, and testesa4 are installed.");

            d.cleanUp();

            ArrayList<String> uninstallFeatures = new ArrayList<String>(1);
            uninstallFeatures.add("testesa1");
            uninstallFeatures.add("testesa2");
            uninstallFeatures.add("testesa3");
            uninstallFeatures.add("testesa4");
            d.uninstallFeatures(uninstallFeatures, null);

            d.uninstall(true, null, null);

            assertFalse("testesa1.bat should be removed", new File(TestUtils.wlpUserBinDir, "testesa1.bat").exists());
            assertFalse("testesa2.bat should be removed", new File(TestUtils.wlpUserBinDir, "testesa2.bat").exists());
            assertFalse("testesa3.bat should be removed", new File(TestUtils.wlpUserBinDir, "testesa3.bat").exists());
            assertFalse("testesa4.bat should be removed", new File(TestUtils.wlpUserBinDir, "testesa4.bat").exists());

            assertFalse("testesa1.mf should be removed", new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf").exists());
            assertFalse("testesa2.mf should be removed", new File(TestUtils.wlpUserFeaturesDir, "testesa2.mf").exists());
            assertFalse("testesa3.mf should be removed", new File(TestUtils.wlpUserFeaturesDir, "testesa3.mf").exists());
            assertFalse("testesa4.mf should be removed", new File(TestUtils.wlpUserFeaturesDir, "testesa4.mf").exists());

            Log.info(DirectorTest.class, testCaseName, "testesa1, testesa2, testesa3, and testesa4 are uninstalled.");

            d.cleanUp();

            File[] afterCleanupFiles = tempDir.listFiles();
            int numOfAfterCleanupFiles = afterCleanupFiles == null ? 0 : afterCleanupFiles.length;
            Log.info(DirectorTest.class, testCaseName, "after cleaned up, tempDir has " + numOfAfterCleanupFiles + " files");
            assertEquals("testDirector_install: expected cleaned up", numOfExistingFiles, (afterDownloadedFiles == null ? 0 : afterCleanupFiles.length));
        } finally {
            InstallUtils.delete(new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar"));
            InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa1.bat"));
            InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa2.bat"));
            InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa3.bat"));
            InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa4.bat"));
            InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf"));
            InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa2.mf"));
            InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa3.mf"));
            InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa4.mf"));
        }
    }

    @Test
    public void testDirector_simple_install_uninstall_multiple_user_features() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "_simple_install_uninstall_multiple_user_features";
        initializeTestCase(testCaseName);

        InstallUtils.delete(new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar"));
        InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa1.bat"));
        InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa3.bat"));
        InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf"));
        InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa3.mf"));

        File[] existingFiles = tempDir.listFiles();
        int numOfExistingFiles = existingFiles == null ? 0 : existingFiles.length;
        Log.info(DirectorTest.class, testCaseName, "tempDir has " + numOfExistingFiles + " files");
        Director d = getDirector();
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("testesa3");
        d.installFeatures(features, "usr", true, null, null);

        File[] afterDownloadedFiles = tempDir.listFiles();
        int numOfAfterDownloadedFiles = afterDownloadedFiles == null ? 0 : afterDownloadedFiles.length;
        Log.info(DirectorTest.class, testCaseName, "after downloaded, tempDir has " + numOfAfterDownloadedFiles + " files");

        d.install(ExistsAction.fail, false, false);
        assertTrue("testesa1_1.0.0.jar should be created", new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar").exists());
        assertTrue("testesa1.bat should be created", new File(TestUtils.wlpUserBinDir, "testesa1.bat").exists());
        assertTrue("testesa3.bat should be created", new File(TestUtils.wlpUserBinDir, "testesa3.bat").exists());

        assertTrue("testesa1.mf should be created", new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf").exists());
        assertTrue("testesa3.mf should be created", new File(TestUtils.wlpUserFeaturesDir, "testesa3.mf").exists());

        Log.info(DirectorTest.class, testCaseName, "testesa1, and testesa3 are installed.");
        d.cleanUp();

        ArrayList<String> uninstallFeatures = new ArrayList<String>(1);
        uninstallFeatures.add("testesa1");
        uninstallFeatures.add("testesa3");
        try {
            d.uninstallFeatures(uninstallFeatures, null);
            d.uninstall(true, null, null);
            assertFalse("testesa1.bat should be removed", new File(TestUtils.wlpUserBinDir, "testesa1.bat").exists());
            assertFalse("testesa3.bat should be removed", new File(TestUtils.wlpUserBinDir, "testesa3.bat").exists());
            assertFalse("testesa1.mf should be removed", new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf").exists());
            assertFalse("testesa3.mf should be removed", new File(TestUtils.wlpUserFeaturesDir, "testesa3.mf").exists());
            Log.info(DirectorTest.class, testCaseName, "testesa1, and testesa3 are uninstalled.");
            d.cleanUp();
        } finally {
            InstallUtils.delete(new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar"));
            InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa1.bat"));
            InstallUtils.delete(new File(TestUtils.wlpUserBinDir, "testesa3.bat"));
            InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf"));
            InstallUtils.delete(new File(TestUtils.wlpUserFeaturesDir, "testesa3.mf"));
        }

        File[] afterCleanupFiles = tempDir.listFiles();
        int numOfAfterCleanupFiles = afterCleanupFiles == null ? 0 : afterCleanupFiles.length;
        Log.info(DirectorTest.class, testCaseName, "after cleaned up, tempDir has " + numOfAfterCleanupFiles + " files");
        assertEquals("testDirector_install: expected cleaned up", numOfExistingFiles, (afterDownloadedFiles == null ? 0 : afterCleanupFiles.length));
    }

    @Test
    public void testDirector_getLicenses() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "_getLicenses";
        initializeTestCase(testCaseName);

        Director d = getDirector();

        ArrayList<String> features = new ArrayList<String>(1);
        features.add("cik.usertest.with.ibm.license");

        Set<String> installedLicenses = d.getInstalledLicense();
        Log.info(DirectorTest.class, testCaseName, "InstalledLicense=" + installedLicenses.toString());

        Collection<InstallLicense> licenses = d.getFeatureLicense(features, Locale.ENGLISH, null, null);
        for (InstallLicense license : licenses) {
            Log.info(DirectorTest.class, testCaseName, license.getId());
            Log.info(DirectorTest.class, testCaseName, license.getType());
            Log.info(DirectorTest.class, testCaseName, license.getInformation());
            Log.info(DirectorTest.class, testCaseName, license.getAgreement());
        }
        assertEquals("Director.getFeatureLicense().size()", 2, licenses.size());
        for (InstallLicense license : licenses) {
            String licenseId = license.getId();
            assertTrue("unexpected license id", licenseId.equals("different") || licenseId.equals("L-JTHS-93TMHH"));
            assertEquals("unexpected license type", "", license.getType());
            String licenseName = license.getName();
            assertTrue("unexpected license name", licenseName.startsWith("MainLicense") || licenseName.startsWith("DifferentLicense"));
            assertEquals("unexpected license program name", "IBM WebSphere Application Server V8.5.Next Beta", license.getProgramName());
            String licenseInfomation = license.getInformation();
            assertTrue("unexpected license information", licenseInfomation.startsWith("LICENSE INFORMATION"));
            String[] lines = licenseInfomation.split("\n");
            for (String line : lines)
                assertTrue("Each license infomation line should not greater than 72 (ESAAsset.LINE_WRAP_COLUMNS)", line.length() <= 72);
            String licenseAgreement = license.getAgreement();
            assertTrue("unexpected license agreement", licenseAgreement.startsWith("DifferentLicense") || licenseAgreement.startsWith("MainLicense"));
            lines = licenseAgreement.split("\n");
            for (String line : lines)
                assertTrue("Each license agreement line should not greater than 72 (ESAAsset.LINE_WRAP_COLUMNS)", line.length() <= 72);
        }
    }

    @Test
    public void testDirector_getLicenseType() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "_getLicenseType";
        initializeTestCase(testCaseName);

        Director d = getDirector();

        Log.info(DirectorTest.class, testCaseName, "Director.getFeatureLicense(\"com.ibm.ws.test.simple\")");
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("com.ibm.ws.test.simple");
        Collection<InstallLicense> licenses = d.getFeatureLicense(features, Locale.ENGLISH, null, null);
        assertEquals("Director.getFeatureLicense(com.ibm.ws.test.simple).size()", 0, licenses.size());

        Log.info(DirectorTest.class, testCaseName, "Director.getFeatureLicense(\"cik.unspecified.license.type\")");
        features = new ArrayList<String>(1);
        features.add("cik.unspecified.license.type");
        licenses = d.getFeatureLicense(features, Locale.ENGLISH, null, null);
        for (InstallLicense license : licenses) {
            Log.info(DirectorTest.class, testCaseName, license.getId());
            Log.info(DirectorTest.class, testCaseName, license.getType());
            Log.info(DirectorTest.class, testCaseName, license.getProgramName());
            Log.info(DirectorTest.class, testCaseName, license.getFeatures().toString());
            Log.info(DirectorTest.class, testCaseName, license.getInformation());
            Log.info(DirectorTest.class, testCaseName, license.getAgreement());
        }
        assertEquals("Director.getFeatureLicense(cik.unspecified.license.type).size()", 1, licenses.size());
        for (InstallLicense license : licenses) {
            assertEquals("expected license id is L-JTHS-93TMHH", "L-JTHS-93TMHH", license.getId());
            assertEquals("expected license type is ILAN", "UNSPECIFIED", license.getType());
            assertEquals("expected license name is empty string", "UNSPECIFIED", license.getName());
            assertTrue("expected license program name contains 'WebSphere Application Server'", license.getProgramName().contains("WebSphere Application Server"));
            Collection<String> featureNames = license.getFeatures();
            assertEquals("license.getFeatures().size()", 1, featureNames.size());
            assertEquals("license.getFeatures() contains cik.unspecified.license.type", "cik.unspecified.license.type", featureNames.iterator().next());
            assertFalse("expected license information is not empty", license.getInformation().isEmpty());
            assertTrue("expected license agreement contains \"UNSPECIFIED\"", license.getAgreement().contains("UNSPECIFIED"));
        }

        Log.info(DirectorTest.class, testCaseName, "Director.getFeatureLicense(\"cik.usertest.with.ibm.license\")");
        features = new ArrayList<String>(1);
        features.add("cik.usertest.with.ibm.license");
        licenses = d.getFeatureLicense(features, Locale.ENGLISH, null, null);
        for (InstallLicense license : licenses) {
            Log.info(DirectorTest.class, testCaseName, license.getId());
            Log.info(DirectorTest.class, testCaseName, license.getType());
            Log.info(DirectorTest.class, testCaseName, license.getProgramName());
            Log.info(DirectorTest.class, testCaseName, license.getFeatures().toString());
            Log.info(DirectorTest.class, testCaseName, license.getInformation());
            Log.info(DirectorTest.class, testCaseName, license.getAgreement());
        }
        assertEquals("Director.getFeatureLicense(cik.usertest.with.ibm.license).size()", 2, licenses.size());
        for (InstallLicense license : licenses) {
            assertTrue("expected license id is L-JTHS-93TMHH or different", license.getId().equals("different") || license.getId().equals("L-JTHS-93TMHH"));
            assertEquals("expected license type is empty string", "", license.getType());
            assertTrue("expected license name is MainLicense or DifferentLicense", license.getName().equals("MainLicense") || license.getName().equals("DifferentLicense"));
            assertEquals("expected license program name is IBM WebSphere Application Server V8.5.Next Beta", "IBM WebSphere Application Server V8.5.Next Beta",
                         license.getProgramName());
            Collection<String> featureNames = license.getFeatures();
            if (featureNames.size() == 1)
                assertTrue("license.getFeatures() contains cik.usertest.with.ibm.license.different", featureNames.contains("cik.usertest.with.ibm.license.different"));
            else if (featureNames.size() == 2)
                assertTrue("license.getFeatures() contains [cik.usertest.with.ibm.license.same, cik.usertest.with.ibm.license]",
                           featureNames.contains("cik.usertest.with.ibm.license") || featureNames.contains("cik.usertest.with.ibm.license.same"));
            else
                fail("unexpected features size");
            assertTrue("expected license information contains \"LICENSE INFORMATION\"", license.getInformation().contains("LICENSE INFORMATION"));
            assertTrue("expected license agreement contains \"International License Agreement for Early Release of Programs\"",
                       license.getAgreement().contains("International License Agreement for Early Release of Programs"));
        }
    }

    @Test
    public void testDirector_installUnavailableFeature() {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "_installUnavailableFeature";
        initializeTestCase(testCaseName);
        Director d = getDirector();
        ArrayList<String> features = new ArrayList<String>(1);
        features.add("UnavailableFeature");
        try {
            d.installFeatures(features, "", true, "userid", "password");
            fail("Director.installFeatures() didn't throw exception.");
        } catch (Exception e) {
            assertEquals("Director.installFeatures()",
                         "CWWKF1203E: Unable to obtain the following features: UnavailableFeature. Ensure that the features are valid.",
                         e.getMessage());
        }
    }
}
