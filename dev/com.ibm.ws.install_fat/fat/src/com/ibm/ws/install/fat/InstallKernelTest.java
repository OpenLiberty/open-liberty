package com.ibm.ws.install.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallConstants.ExistsAction;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallKernel;
import com.ibm.ws.install.InstallKernelFactory;
import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.install.ReapplyFixException;
import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.liberty.MainRepository;
import com.ibm.ws.repository.exceptions.RepositoryBackendIOException;

import componenttest.topology.utils.FileUtils;
import test.utils.TestUtils;

public class InstallKernelTest extends InstallFATTest {

    private void initializeTestCase(String testName) {
        Log.info(InstallKernelTest.class, testName, "***********************************************************************************************");
        Log.info(InstallKernelTest.class, testName, "             " + testName + " " + TestUtils.currentTime());
        Log.info(InstallKernelTest.class, testName, "***********************************************************************************************");
    }

    private InstallKernel getInstallKernel() {
        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        if (TestUtils.noLogger())
            installKernel.enableConsoleLog(Level.ALL);
        return installKernel;
    }

    private void checkInstalledFiles(Set<File> fileList, boolean afterInstall) {
        ArrayList<File> missingFiles = new ArrayList<File>();
        for (File file : fileList) {
            Log.info(InstallKernelTest.class, "checkInstalledFiles", "Path= " + file.getAbsolutePath() + (file.exists() ? " EXISTS!" : " DOES NOT EXIST!"));
            if (afterInstall != file.exists()) {
                missingFiles.add(file);
            }
        }
        assertTrue("Files List : " + missingFiles + (afterInstall ? " doesn't exist" : " exists"), missingFiles.isEmpty());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        TestUtils.cleanLogger();
    }

    @Test
    public void testGetInstalledLicense() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testGetInstalledLicense";
        initializeTestCase(testCaseName);

        InstallKernel installKernel = getInstallKernel();
        int initialLicenseCount = installKernel.getInstalledLicense().size();
        installKernel.installFeature("cik.usertest.with.ibm.license",
                                     InstallConstants.TO_USER,
                                     true,
                                     ExistsAction.fail);
        Set<String> installedLicenses = installKernel.getInstalledLicense();
        Log.info(InstallKernelTest.class, testCaseName, "InstalledLicense=" + installedLicenses.toString());
        assertEquals("InstallKernel.getInstalledLicense() should return " + (initialLicenseCount + 2) + " licenses", initialLicenseCount + 2, installedLicenses.size());
        assertTrue("InstallKernel.getInstalledLicense() return should contain \"different\"", installedLicenses.contains("different"));
        assertTrue("InstallKernel.getInstalledLicense() return should contain \"L-JTHS-93TMHH\"", installedLicenses.contains("L-JTHS-93TMHH"));
        installKernel.uninstallFeature("cik.usertest.with.ibm.license", false);
        installKernel.uninstallFeature("cik.usertest.with.ibm.license.same", false);
        installKernel.uninstallFeature("cik.usertest.with.ibm.license.different", false);
    }

    @Test
    public void testGetFeatureLicense() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testGetFeatureLicense";
        initializeTestCase(testCaseName);

        InstallKernel installKernel = getInstallKernel();
        Set<InstallLicense> licenses = installKernel.getFeatureLicense("cik.usertest.with.ibm.license",
                                                                       Locale.ENGLISH);
        for (InstallLicense license : licenses) {
            Log.info(InstallKernelTest.class, testCaseName, license.getId());
            Log.info(InstallKernelTest.class, testCaseName, license.getType());
            Log.info(InstallKernelTest.class, testCaseName, license.getInformation());
            Log.info(InstallKernelTest.class, testCaseName, license.getAgreement());
        }
        assertEquals("InstallKernel.getFeatureLicense()", 2, licenses.size());
        for (InstallLicense license : licenses) {
            String licenseId = license.getId();
            assertTrue("unexpected license id", licenseId.equals("different") || licenseId.equals("L-JTHS-93TMHH"));
            assertEquals("unexpected license type", "", license.getType());
            String licenseName = license.getName();
            assertTrue("unexpected license name", licenseName.startsWith("MainLicense") || licenseName.startsWith("DifferentLicense"));
            assertEquals("unexpected license program name", "IBM WebSphere Application Server V8.5.Next Beta", license.getProgramName());
            String licenseInfomation = license.getInformation();
            assertTrue("unexpected license information", licenseInfomation.startsWith("LICENSE INFORMATION"));
            String licenseAgreement = license.getAgreement();
            assertTrue("unexpected license agreement", licenseAgreement.startsWith("DifferentLicense") || licenseAgreement.startsWith("MainLicense"));
        }

        Collection<String> features = new ArrayList<String>(3);
        features.add("cik.usertest.with.ibm.license.same");
        features.add("cik.usertest.with.ibm.license.different");
        features.add("cik.usertest.with.ibm.license");
        licenses = installKernel.getFeatureLicense(features, Locale.ENGLISH);
        for (InstallLicense license : licenses) {
            Log.info(InstallKernelTest.class, testCaseName, license.getId());
        }
        assertEquals("InstallKernel.getFeatureLicense()", 2, licenses.size());
    }

    @Test
    public void testGetFeatureLicenseType() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testGetFeatureLicenseType";
        initializeTestCase(testCaseName);

        InstallKernel installKernel = getInstallKernel();
        Log.info(InstallKernelTest.class, testCaseName, "InstallKernel.getFeatureLicense(\"com.ibm.ws.test.simple\")");
        Set<InstallLicense> licenses = installKernel.getFeatureLicense("com.ibm.ws.test.simple",
                                                                       Locale.ENGLISH);
        assertEquals("InstallKernel.getFeatureLicense(com.ibm.ws.test.simple).size()", 0, licenses.size());

        Log.info(InstallKernelTest.class, testCaseName, "InstallKernel.getFeatureLicense(\"cik.unspecified.license.type\")");
        licenses = installKernel.getFeatureLicense("cik.unspecified.license.type",
                                                   Locale.ENGLISH);
        for (InstallLicense license : licenses) {
            Log.info(InstallKernelTest.class, testCaseName, license.getId());
            Log.info(InstallKernelTest.class, testCaseName, license.getType());
            Log.info(InstallKernelTest.class, testCaseName, license.getProgramName());
            Log.info(InstallKernelTest.class, testCaseName, license.getFeatures().toString());
            Log.info(InstallKernelTest.class, testCaseName, license.getInformation());
            Log.info(InstallKernelTest.class, testCaseName, license.getAgreement());
        }
        assertEquals("InstallKernel.getFeatureLicense(cik.unspecified.license.type).size()", 1, licenses.size());
        for (InstallLicense license : licenses) {
            assertEquals("expected license id is L-JTHS-93TMHH", "L-JTHS-93TMHH", license.getId());
            assertEquals("expected license type is ILAN", "UNSPECIFIED", license.getType());
            assertEquals("expected license name is empty string", "UNSPECIFIED", license.getName());
            assertTrue("expected license program name contains 'WebSphere Application Server'", license.getProgramName().contains("WebSphere Application Server"));
            Collection<String> featureNames = license.getFeatures();
            assertEquals("license.getFeatures().size()", 1, featureNames.size());
            assertEquals("license.getFeatures() contains cik.unspecified.license.type", "cik.unspecified.license.type", featureNames.iterator().next());
            assertTrue("expected license information is not empty", !license.getInformation().isEmpty());
            assertTrue("expected license agreement contains \"UNSPECIFIED\"", license.getAgreement().contains("UNSPECIFIED"));
        }

        Log.info(InstallKernelTest.class, testCaseName, "InstallKernel.getFeatureLicense(\"cik.usertest.with.ibm.license\")");
        licenses = installKernel.getFeatureLicense("cik.usertest.with.ibm.license",
                                                   Locale.ENGLISH);
        for (InstallLicense license : licenses) {
            Log.info(InstallKernelTest.class, testCaseName, license.getId());
            Log.info(InstallKernelTest.class, testCaseName, license.getType());
            Log.info(InstallKernelTest.class, testCaseName, license.getProgramName());
            Log.info(InstallKernelTest.class, testCaseName, license.getFeatures().toString());
            Log.info(InstallKernelTest.class, testCaseName, license.getInformation());
            Log.info(InstallKernelTest.class, testCaseName, license.getAgreement());
        }
        assertEquals("InstallKernel.getFeatureLicense(cik.usertest.with.ibm.license).size()", 2, licenses.size());
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

    //Disabled for RTC 136527, use different esa
    @Test
    public void testInstallInvalidUserExtension() {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallInvalidUserExtension";
        initializeTestCase(testCaseName);

        InstallKernel installKernel = InstallKernelFactory.getInstance(imageDir);
        final List<String> logMsgs = new ArrayList<String>();
        Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
        logger.setLevel(Level.FINE);
        Handler h = new Handler() {
            @Override
            public void close() {}

            @Override
            public void flush() {}

            @Override
            public void publish(LogRecord record) {
                logMsgs.add(record.getLevel() + ">" + record.getMessage());
            }
        };
        logger.addHandler(h);

        try {
            installKernel.installFeature("com.ibm.genericCoreFeature", "unknown", true, ExistsAction.fail);
            fail("InstallKernel.installFeature() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("InstallKernel.installFeature()", "CWWKF1003E: The product extension unknown does not exist.", e.getMessage());
        }
        try {
            installKernel.installFeature("com.ibm.genericCoreFeature", "cik.ext.missing.path.product", true, ExistsAction.fail);
            fail("InstallKernel.installFeature() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("InstallKernel.installFeature()", "CWWKF1004E: The product extension cik.ext.missing.path.product does not specify a location.", e.getMessage());
        }
        TestUtils.verifyContains(logMsgs,
                                 new String[] {
                                                "FINE>CWWKF1300I: Installing the following features: [com.ibm.genericCoreFeature].",
                                                "FINE>Successfully downloaded feature com.ibm.genericCoreFeature.",
                                                "SEVERE>CWWKF1003E: The product extension unknown does not exist.",
                                                "FINE>CWWKF1300I: Installing the following features: [com.ibm.genericCoreFeature].",
                                                "SEVERE>CWWKF1004E: The product extension cik.ext.missing.path.product does not specify a location." });
        logger.removeHandler(h);
    }

    //Disabled for RTC 136527, use differenty esa
    //@Test
    public void testInstallUserExtension() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallUserExtension";
        initializeTestCase(testCaseName);

        InstallKernel installKernel = getInstallKernel();

        final List<String> logMsgs = new ArrayList<String>();
        Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
        logger.setLevel(Level.FINE);
        Handler h = new Handler() {
            @Override
            public void close() {}

            @Override
            public void flush() {}

            @Override
            public void publish(LogRecord record) {
                logMsgs.add(record.getLevel() + ">" + record.getMessage());
            }
        };
        logger.addHandler(h);

        File cikProduct = new File(imageDir, "../cik");
        try {
            Set<String> features = installKernel.getInstalledFeatures("cik.ext.product1");
            assertFalse("installed features should not contain com.ibm.genericUserFeature", features.contains("com.ibm.genericUserFeature"));

            installKernel.installFeature("com.ibm.genericUserFeature",
                                         "cik.ext.product1",
                                         true,
                                         ExistsAction.fail);
            assertTrue("com.ibm.genericUserFeature_1.0.0.jar should be created", new File(cikProduct, "ext.product1/lib/com.ibm.genericUserFeature_1.0.0.jar").exists());
            assertTrue("com.ibm.genericUserFeature.mf should be created",
                       new File(cikProduct, "ext.product1/lib/features/com.ibm.genericUserFeature.mf").exists());

//            assertTrue("9000-wlp-archive-IFTS10001_9.0.0.0.lpmf should be created", new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFTS10001_9.0.0.0.lpmf").exists());
//            assertTrue("9000-wlp-archive-IFTS10001_9.0.0.0.xml should be created", new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFTS10001_9.0.0.0.xml").exists());
//            assertTrue("9000-wlp-archive-IFTS10001_9.0.0.0.zip should be created", new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFTS10001_9.0.0.0.zip").exists());
            features = installKernel.getInstalledFeatures("cik.ext.product1");

            assertTrue("installed features should contain com.ibm.genericUserFeature", features.contains("com.ibm.genericUserFeature"));

            installKernel.uninstallFeature("com.ibm.genericUserFeature", false);
//            installKernel.uninstallFix("9000-wlp-archive-IFTS10001");

            assertFalse("com.ibm.genericUserFeature should be removed", new File(cikProduct, "ext.product1/lib/features/com.ibm.genericUserFeature").exists());
            features = installKernel.getInstalledFeatures("cik.ext.product1");
            assertFalse("installed features should not contain com.ibm.genericUserFeature", features.contains("com.ibm.genericUserFeature"));

//            TestUtils.verifyContains(logMsgs,
//                                     new String[] {
//                                                   "INFO>CWWKF1300I: Installing the following features: [cik.usertest].",
//                                                   "FINE>Starting installation...",
//                                                   "FINE>Checking...",
//                                                   "FINE>Downloading cik.usertest.dependency...",
//                                                   "FINE>Downloading cik.usertest...",
//                                                   "FINE>Installing cik.usertest.dependency...",
//                                                   "INFO>CWWKF1304I: Successfully installed feature cik.usertest.dependency.",
//                                                   "FINE>Installing cik.usertest...",
//                                                   "INFO>CWWKF1304I: Successfully installed feature cik.usertest.",
//                                                   "FINE>Cleaning...",
//                                                   "FINE>Installation completed",
//                                                   "INFO>CWWKF1302I: Uninstalling the following features: [cik.usertest].",
//                                                   "FINE>Starting uninstallation...",
//                                                   "FINE>Checking...",
//                                                   "FINE>Uninstalling cik.usertest...",
//                                                   "INFO>CWWKF1306I: Successfully uninstalled feature cik.usertest.",
//                                                   "FINE>Cleaning...",
//                                                   "FINE>Uninstallation completed",
//                                                   "INFO>CWWKF1302I: Uninstalling the following features: [cik.usertest.dependency].",
//                                                   "FINE>Starting uninstallation...",
//                                                   "FINE>Checking...",
//                                                   "FINE>Uninstalling cik.usertest.dependency...",
//                                                   "INFO>CWWKF1306I: Successfully uninstalled feature cik.usertest.dependency.",
//                                                   "FINE>Cleaning...",
//                                                   "FINE>Uninstallation completed", });
        } finally {
            if (cikProduct.exists()) {
                //FileUtils.recursiveDelete(cikProduct);
//                InstallUtils.delete(new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFTS10001_9.0.0.0.lpmf"));
//                InstallUtils.delete(new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFTS10001_9.0.0.0.xml"));
//                InstallUtils.delete(new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFTS10001_9.0.0.0.zip"));
                Log.info(InstallKernelTest.class, testCaseName, "remove manually " + cikProduct.getAbsolutePath());
            }
            logger.removeHandler(h);
        }
    }

    @Test
    public void testUninstallUserExtensionWithStaticFile() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testUninstallUserExtension";
        initializeTestCase(testCaseName);

        InstallKernel installKernel = getInstallKernel();

        final List<String> logMsgs = new ArrayList<String>();
        Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
        logger.setLevel(Level.FINEST);
        Handler h = new Handler() {
            @Override
            public void close() {}

            @Override
            public void flush() {}

            @Override
            public void publish(LogRecord record) {
                logMsgs.add(record.getLevel() + ">" + record.getMessage());
            }
        };
        logger.addHandler(h);

        String[] filesList = { "/usr/extension/lib/features/testesa1.mf",
                               "/usr/extension/bin/testesa1.bat",
                               "usr/extension/lib/testesa1_1.0.0.jar", "/lib/features/testesa1.mf", "/bin/testesa1.bat", "/lib/testesa1_1.0.0.jar" };
        //Deleting everything associated with testesa1
        try {
            deleteFiles(testCaseName, "testesa1", filesList);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }

        File cikProduct = new File(imageDir, "../cik");
        if (cikProduct.exists()) {
            FileUtils.recursiveDelete(cikProduct);
            Log.info(InstallKernelTest.class, testCaseName, "remove manually " + cikProduct.getAbsolutePath());
        }
        try {
            installKernel.installFeature("testesa1",
                                         "cik.ext.product1",
                                         true,
                                         ExistsAction.fail);

            assertTrue("testesa1_1.0.0.jar should be created", new File(cikProduct, "ext.product1/lib/testesa1_1.0.0.jar").exists());
            assertTrue("testesa1.bat should be created", new File(cikProduct, "ext.product1/bin/testesa1.bat").exists());
            assertTrue("testesa1.mf should be created", new File(cikProduct, "ext.product1/lib/features/testesa1.mf").exists());

            installKernel.uninstallFeature("testesa1", false);

            assertFalse("testesa1.bat should be removed", new File(cikProduct, "ext.product1/bin/testesa1.bat").exists());
            assertFalse("cik.usertest.mf should be removed", new File(cikProduct, "ext.product1/lib/features/testesa1.mf").exists());

            TestUtils.verifyContains(logMsgs,
                                     new String[] {
                                                    "CWWKF1300I: Installing the following features: [testesa1].",
                                                    "Starting installation ...",
                                                    "Checking features ...",
                                                    "Downloading testesa1 ...",
                                                    "Installing testesa1 ...",
                                                    "Successfully installed feature testesa1.",
                                                    "Installation completed",
                                                    "Starting uninstallation ...",
                                                    "Uninstalling testesa1 ...",
                                                    "Successfully uninstalled feature testesa1." });
        } finally {
            if (cikProduct.exists()) {
                FileUtils.recursiveDelete(cikProduct);
                Log.info(InstallKernelTest.class, testCaseName, "remove manually " + cikProduct.getAbsolutePath());
            }
            logger.removeHandler(h);
        }
    }

    private void verifyTestesa1(Collection<String> installedFeatures, boolean fileExists) {
        if (installedFeatures != null) {
            assertEquals("Expected one feature was installed", 1, installedFeatures.size());
            assertEquals("Expected testesa1 was installed", "testesa1", installedFeatures.iterator().next());
        }
        File testesa1Jar = new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar");
        File testesa1Bat = new File(TestUtils.wlpUserBinDir, "testesa1.bat");
        File testesa1Mf = new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf");
        assertTrue("testesa1_1.0.0.jar extistence should be " + fileExists, testesa1Jar.exists() == fileExists);
        assertTrue("testesa1.bat extistence should be " + fileExists, testesa1Bat.exists() == fileExists);
        assertTrue("testesa1.mf extistence should be " + fileExists, testesa1Mf.exists() == fileExists);
    }

    private void verifyTestesa2(Collection<String> installedFeatures, boolean fileExists) {
        if (installedFeatures != null) {
            assertEquals("Expected one feature was installed", 1, installedFeatures.size());
            assertEquals("Expected testesa2 was installed", "testesa2", installedFeatures.iterator().next());
        }
        File testesa1Jar = new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar");
        File testesa2Bat = new File(TestUtils.wlpUserBinDir, "testesa2.bat");
        File testesa2Mf = new File(TestUtils.wlpUserFeaturesDir, "testesa2.mf");
        assertTrue("testesa1_1.0.0.jar should exist", testesa1Jar.exists());
        assertTrue("testesa1.bat extistence should be " + fileExists, testesa2Bat.exists() == fileExists);
        assertTrue("testesa1.mf extistence should be " + fileExists, testesa2Mf.exists() == fileExists);
    }

    @Test
    public void testInstallExistActionIsFailAndIgnoreForBundle() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallExistActionIsFailAndIgnoreForBundle";
        initializeTestCase(testCaseName);

        InstallKernel installKernel = getInstallKernel();

        File testesa1Jar = new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar");
        File testesa1Bat = new File(TestUtils.wlpUserBinDir, "testesa1.bat");
        File testesa1Mf = new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf");

        InstallUtils.delete(testesa1Jar);
        InstallUtils.delete(testesa1Bat);
        InstallUtils.delete(testesa1Mf);

        Collection<String> installedFeatures = installKernel.installFeature("testesa1",
                                                                            null,
                                                                            true,
                                                                            ExistsAction.fail);
        verifyTestesa1(installedFeatures, true);

        try {
            installedFeatures = installKernel.installFeature("testesa2",
                                                             null,
                                                             true,
                                                             ExistsAction.fail);
            fail("Excepted failed to install testesa2");
        } catch (InstallException e) {
            assertTrue("Expected exception message started with \"CWWKF1015E: The file\"", e.getMessage().startsWith("CWWKF1015E: The file"));
            assertTrue("Expected exception message ended with \"jar already exists.\"", e.getMessage().endsWith("jar already exists."));
        }

        verifyTestesa1(null, true);
        verifyTestesa2(null, false);

        try {
            installedFeatures = installKernel.installFeature("testesa2",
                                                             null,
                                                             true,
                                                             ExistsAction.ignore);

            verifyTestesa1(null, true);
            verifyTestesa2(installedFeatures, true);
        }

        finally {
            InstallUtils.delete(testesa1Jar);
            InstallUtils.delete(testesa1Bat);
            InstallUtils.delete(testesa1Mf);
        }
    }

    @Test
    public void testInstallExistActionIsReplaceForBundle() throws InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallExistActionIsReplaceForBundle";
        initializeTestCase(testCaseName);

        InstallKernel installKernel = getInstallKernel();

        File testesa1Jar = new File(TestUtils.wlpUserLibDir, "testesa1_1.0.0.jar");
        File testesa1Bat = new File(TestUtils.wlpUserBinDir, "testesa1.bat");
        File testesa1Mf = new File(TestUtils.wlpUserFeaturesDir, "testesa1.mf");

        InstallUtils.delete(testesa1Jar);
        InstallUtils.delete(testesa1Bat);
        InstallUtils.delete(testesa1Mf);

        File testesa2Jar = new File(TestUtils.wlpUserLibDir, "testesa2_1.0.0.jar");
        File testesa2Bat = new File(TestUtils.wlpUserBinDir, "testesa2.bat");
        File testesa2Mf = new File(TestUtils.wlpUserFeaturesDir, "testesa2.mf");

        InstallUtils.delete(testesa2Jar);
        InstallUtils.delete(testesa2Bat);
        InstallUtils.delete(testesa2Mf);

        try {
            Collection<String> installedFeatures = installKernel.installFeature("testesa1",
                                                                                null,
                                                                                true,
                                                                                ExistsAction.fail);
            verifyTestesa1(installedFeatures, true);

            installedFeatures = installKernel.installFeature("testesa2",
                                                             null,
                                                             true,
                                                             ExistsAction.replace);
            verifyTestesa1(null, true);
        } finally {
            InstallUtils.delete(testesa1Jar);
            InstallUtils.delete(testesa1Bat);
            InstallUtils.delete(testesa1Mf);

            InstallUtils.delete(testesa2Jar);
            InstallUtils.delete(testesa2Bat);
            InstallUtils.delete(testesa2Mf);
        }
    }

    @Test
    public void testAlreadyInstalledByShortName() {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testAlreadyInstallByShortName";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();
        try {
            installKernel.installFeature("ADMINCenter-1.0", null, true, ExistsAction.fail);
            fail("InstallKernelImpl.installFeature(\"ADMINCenter-1.0\") should be failed.");
        } catch (InstallException e) {
            assertEquals("InstallKernel.installFeature(\"ADMINCenter-1.0\") should throw exception",
                         "CWWKF1216I: The following features already exist: [adminCenter-1.0]. They will not be reinstalled. To modify an existing feature, you must manually uninstall it first.",
                         e.getMessage());
        }
    }

    private void verifyGenericCoreFeatureModifyA(Collection<String> installedFeatures, String txt, boolean fileExists) throws Exception {
        if (installedFeatures != null) {
            assertEquals("Expected one feature was installed", 1, installedFeatures.size());
            assertEquals("Expected Generic Core Feature Modify A was installed", "genericCoreFeatureModifyA", installedFeatures.iterator().next());
        }
        File modifyTxt = new File(TestUtils.wlpBin, "ModifyFile.txt");
        File modifyAJar = new File(TestUtils.wlpLib, "com.ibm.genericCoreFeatureModifyA_1.0.0.jar");
        File modifyAMf = new File(TestUtils.wlpLibFeatures, "com.ibm.genericCoreFeatureModifyA.mf");
        File modifyACs = new File(TestUtils.libFeaturesCheckSums, "com.ibm.genericCoreFeatureModifyA.cs");
        File modifyAI10n = new File(TestUtils.libFeaturesl10n, "com.ibm.genericCoreFeatureModifyA.properties");
        assertTrue("com.ibm.genericCoreFeatureModifyA.mf existence should be " + fileExists, modifyAMf.exists() == fileExists);
        assertTrue("com.ibm.genericCoreFeatureModifyA.cs existence should be " + fileExists, modifyACs.exists() == fileExists);
        assertTrue("com.ibm.genericCoreFeatureModifyA.properties existence should be " + fileExists, modifyAI10n.exists() == fileExists);
        // TODO: when uninstall can remove jar, only perform the assertTrue statement
        if (fileExists)
            assertTrue("com.ibm.genericCoreFeatureModifyA_1.0.0.jar existence should be " + fileExists, modifyAJar.exists() == fileExists);
        else {
            if (modifyAJar.exists())
                InstallUtils.delete(modifyAJar);
        }

        assertTrue("ModifyFile.txt should exist", modifyTxt.exists() == fileExists);
        if (modifyTxt.exists())
            TestUtils.verifyFirstLine(modifyTxt, txt);
    }

    private void verifyGenericCoreFeatureModifyB(Collection<String> installedFeatures, String txt, boolean fileExists) throws Exception {
        if (installedFeatures != null) {
            assertEquals("Expected one feature was installed", 1, installedFeatures.size());
            assertEquals("Expected Generic Core Feature Modify B was installed", "genericCoreFeatureModifyB", installedFeatures.iterator().next());
        }
        File modifyTxt = new File(TestUtils.wlpBin, "ModifyFile.txt");
        File modifyBJar = new File(TestUtils.wlpLib, "com.ibm.genericCoreFeatureModifyB_1.0.0.jar");
        File modifyBMf = new File(TestUtils.wlpLibFeatures, "com.ibm.genericCoreFeatureModifyB.mf");
        File modifyBCs = new File(TestUtils.libFeaturesCheckSums, "com.ibm.genericCoreFeatureModifyB.cs");
        File modifyBI10n = new File(TestUtils.libFeaturesl10n, "com.ibm.genericCoreFeatureModifyB.properties");
        assertTrue("com.ibm.genericCoreFeatureModifyB.mf existence should be " + fileExists, modifyBMf.exists() == fileExists);
        assertTrue("com.ibm.genericCoreFeatureModifyB.cs existence should be " + fileExists, modifyBCs.exists() == fileExists);
        assertTrue("com.ibm.genericCoreFeatureModifyB.properties existence should be " + fileExists, modifyBI10n.exists() == fileExists);
        // TODO: when uninstall can remove jar, only perform the assertTrue statement
        if (fileExists)
            assertTrue("com.ibm.genericCoreFeatureModifyB_1.0.0.jar existence should be " + fileExists, modifyBJar.exists() == fileExists);
        else {
            if (modifyBJar.exists())
                InstallUtils.delete(modifyBJar);
        }
        assertTrue("ModifyFile.txt should exist", modifyTxt.exists());
        if (modifyTxt.exists())
            TestUtils.verifyFirstLine(modifyTxt, txt);
    }

    @Test
    public void testInstallExistActionIsFailAndIgnoreForFile() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallExistActionIsFailAndIgnoreForFile";
        initializeTestCase(testCaseName);

        InstallKernel installKernel = getInstallKernel();

        Collection<String> installedFeatures = installKernel.installFeature("com.ibm.genericCoreFeatureModifyA",
                                                                            null,
                                                                            true,
                                                                            ExistsAction.fail);
        verifyGenericCoreFeatureModifyA(installedFeatures, "genericCoreModifyA", true);

        try {
            installedFeatures = installKernel.installFeature("com.ibm.genericCoreFeatureModifyB",
                                                             null,
                                                             true,
                                                             ExistsAction.fail);
            fail("Excepted failed to install com.ibm.genericCoreFeatureModifyB");
        } catch (InstallException e) {
            assertTrue("Expected exception message started with \"CWWKF1015E: The file\"", e.getMessage().startsWith("CWWKF1015E: The file"));
            assertTrue("Expected exception message ended with \"jar already exists.\"", e.getMessage().endsWith("ModifyFile.txt already exists."));
        }

        verifyGenericCoreFeatureModifyA(null, "genericCoreModifyA", true);
        verifyGenericCoreFeatureModifyB(null, "genericCoreModifyA", false);

        installedFeatures = installKernel.installFeature("com.ibm.genericCoreFeatureModifyB",
                                                         null,
                                                         true,
                                                         ExistsAction.ignore);

        verifyGenericCoreFeatureModifyA(null, "genericCoreModifyA", true);
        verifyGenericCoreFeatureModifyB(installedFeatures, "genericCoreModifyA", true);

        installKernel.uninstallFeature("com.ibm.genericCoreFeatureModifyB", false);
        verifyGenericCoreFeatureModifyA(null, "genericCoreModifyA", true);
        verifyGenericCoreFeatureModifyB(null, "genericCoreModifyA", false);

        installKernel.uninstallFeature("com.ibm.genericCoreFeatureModifyA", false);
        verifyGenericCoreFeatureModifyA(null, "genericCoreModifyA", false);
    }

    @Test
    public void testInstallExistActionIsReplaceForFile() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallExistActionIsReplaceForFile";
        initializeTestCase(testCaseName);

        InstallKernel installKernel = getInstallKernel();

        Collection<String> installedFeatures = installKernel.installFeature("com.ibm.genericCoreFeatureModifyA",
                                                                            null,
                                                                            true,
                                                                            ExistsAction.fail);
        verifyGenericCoreFeatureModifyA(installedFeatures, "genericCoreModifyA", true);

        installedFeatures = installKernel.installFeature("com.ibm.genericCoreFeatureModifyB",
                                                         null,
                                                         true,
                                                         ExistsAction.replace);

        verifyGenericCoreFeatureModifyA(null, "genericCoreModifyB", true);
        verifyGenericCoreFeatureModifyB(installedFeatures, "genericCoreModifyB", true);

        installKernel.uninstallFeature("com.ibm.genericCoreFeatureModifyB", false);
        verifyGenericCoreFeatureModifyA(null, "genericCoreModifyB", true);
        verifyGenericCoreFeatureModifyB(null, "genericCoreModifyB", false);

        installKernel.uninstallFeature("com.ibm.genericCoreFeatureModifyA", false);
        verifyGenericCoreFeatureModifyA(null, "genericCoreModifyB", false);
    }

    @Test
    public void testInstallFeatueMissingDependent() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallFeatueMissingDependent";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();
        try {
            installKernel.installFeature("genericCoreFeatureDependancyOnEsaFail", null, true, ExistsAction.replace);
            fail("InstallKernel.installFeature() did not throw exception.");
        } catch (InstallException e) {
            assertTrue("should contain CWWKF1221E",
                       e.getMessage().contains("CWWKF1221E"));
        }
    }

    @Test
    public void testInstallWithoutUser() {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallWithoutUser";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();
        try {
            installKernel.installFeature(new ArrayList<String>(0), null, true, ExistsAction.fail);
        } catch (Exception e) {
            assertEquals("InstallKernel.installFeature()", "CWWKF1200E: The provided features list is null or empty.", e.getMessage());
        }

        try {
            installKernel.installFeature("UnavailableFeature", null, true, ExistsAction.ignore);
            fail("InstallKernel.installFeature() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("InstallKernel.installFeature()",
                         "CWWKF1203E: Unable to obtain the following features: UnavailableFeature. Ensure that the features are valid.",
                         e.getMessage());
        }

        try {
            installKernel.installFix("UnavailableFix");
            fail("InstallKernel.installFix() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("InstallKernel.installFeature()",
                         "CWWKF1204E: Unable to obtain the following interim fixes: UnavailableFix. Ensure that the system can access the IBM WebSphere Liberty Repository and that the interim fix ID's are correct.",
                         e.getMessage());
        }

        try {
            assertTrue(installKernel.getFeatureLicense(new ArrayList<String>(0), Locale.ENGLISH).isEmpty());
            installKernel.getFeatureLicense("unknown.feature", Locale.ENGLISH);
            fail("InstallKernel.getFeatureLicense() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("InstallKernel.getFeatureLicense()",
                         "CWWKF1203E: Unable to obtain the following features: unknown.feature. Ensure that the features are valid.",
                         e.getMessage());
        }
    }

    @Test
    public void testInstallUnavailableFeature() {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallUnavailableFeature";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();
        try {
            installKernel.installFeature("UnavailableFeature", null, true, ExistsAction.ignore, "userid", "password");
            fail("InstallKernel.installFeature() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("InstallKernel.installFeature()",
                         "CWWKF1203E: Unable to obtain the following features: UnavailableFeature. Ensure that the features are valid.",
                         e.getMessage());
        }
    }

    @Test
    public void testGetFeatureLicenseInvalidUser() {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testGetFeatureLicenseInvalidUser";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();
        try {
            assertTrue(installKernel.getFeatureLicense((String) null, Locale.ENGLISH, "userid", "password").isEmpty());
            assertTrue(installKernel.getFeatureLicense(new ArrayList<String>(0), Locale.ENGLISH, "userid", "password").isEmpty());
            installKernel.getFeatureLicense("cik.usertest.with.ibm.license", Locale.ENGLISH, "userid", "password");
            fail("InstallKernel.getFeatureLicense() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("InstallKernel.getFeatureLicense()",
                         "CWWKF1203E: Unable to obtain the following features: cik.usertest.with.ibm.license. Ensure that the features are valid.",
                         e.getMessage());
        }
    }

    @Test
    public void testInstallFeatureTwice() {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallFeatureTwice";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();
        boolean installed = false;
        try {
            installKernel.installFeature("CLusTermemBer-1.0", null, true, ExistsAction.ignore);
            installed = true;
        } catch (InstallException e) {
            // Do not care
        }
        try {
            installKernel.installFeature("CLusTermemBer-1.0", null, true, ExistsAction.ignore);
            fail("InstallKernel.installFeature() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("InstallKernel.installFeature()",
                         "CWWKF1216I: The following features already exist: [clusterMember-1.0]. They will not be reinstalled. To modify an existing feature, you must manually uninstall it first.",
                         e.getMessage());
        }
        try {
            Collection<String> featureNames = new ArrayList<String>(2);
            featureNames.add("CLusTermemBer-1.0");
            featureNames.add("com.ibm.websphere.appserver.clusterMember-1.0");
            installKernel.installFeature(featureNames, null, true, ExistsAction.ignore);
            fail("InstallKernel.installFeature() didn't throw exception.");
        } catch (InstallException e) {
            assertEquals("InstallKernel.installFeature()",
                         "CWWKF1216I: The following features already exist: [clusterMember-1.0, com.ibm.websphere.appserver.clusterMember-1.0]. They will not be reinstalled. To modify an existing feature, you must manually uninstall it first.",
                         e.getMessage());
        }
        if (installed) {
            try {
                installKernel.uninstallFeature("clusterMember-1.0", false);
            } catch (InstallException e) {
                // Do not care
            }
        }
    }

    //Disabled for RTC 136527, use local ifix install
    //Re-enable with RTC 142782, update resolver to deal with locally installed ifix reapply
    //@Test
    public void testReapplyFix() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testReapplyFix";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();

        //ifix info
        String ifixVersion = TestUtils.wlpVersion;
        String ifixVersionNoDots = ifixVersion.replaceAll(".", "");
        String IFPM0222 = "IFPM0222";//IFPM0020
        String IFPM0222MfId = "PM0222";
        final String ifixName = ifixVersionNoDots + "-wlp-archive-" + IFPM0222;

        final int counter[] = { 0, 0, 0 };
        Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
        logger.setLevel(Level.FINE);
        Handler h = new Handler() {
            @Override
            public void close() {}

            @Override
            public void flush() {}

            @Override
            public void publish(LogRecord record) {
                System.out.println(record.getMessage());

//                if (record.getMessage().equalsIgnoreCase("Downloading " + ifixName + "..."))
//                    counter[0]++;
//                else if (record.getMessage().equalsIgnoreCase("Successfully reapplied the following fixes: [" + ifixName + "]"))
//                    counter[1]++;
//                else
                if (record.getMessage().equalsIgnoreCase("Reapplying fixes [" + ifixName + "]..."))
                    counter[2]++;
            }
        };
        logger.addHandler(h);

        File reapplyJar = new File(TestUtils.wlpLib, "com.ibm.ws.mongo_1.0.0.1.jar");
        //File reapplyTxt = new File(TestUtils.wlpLib, "platform/kernel-1.0.mf");
        try {
//            try {
//                installKernel.installFix(ifixName);
//            } catch (InstallException e) {
//                fail("Failed to install " + ifixName);
//            }
            int rc1 = TestUtils.localIfixInstall(IFPM0222, IFPM0222MfId, ifixVersion);
            assertTrue("rc1 =" + rc1, rc1 == 0);
            assertTrue(reapplyJar.getName() + " should exist.", reapplyJar.exists());
            reapplyJar.delete();
            assertFalse(reapplyJar.getName() + " should not exist.", reapplyJar.exists());
            try {
                installKernel.installFeature("com.ibm.genericCoreFeature",
                                             InstallConstants.TO_CORE,
                                             true,
                                             ExistsAction.fail);
            } catch (InstallException e) {
                fail("Failed to install com.ibm.genericCoreFeature");
            }
            assertTrue(reapplyJar.getName() + " should exist after install.", reapplyJar.exists());
            try {
                installKernel.uninstallFix(ifixName);
                installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
            } catch (InstallException e) {
                fail("Failed to uninstall " + ifixName + " and/or com.ibm.genericCoreFeature");
            }
        } finally {
            logger.removeHandler(h);
            InstallUtils.delete(new File(TestUtils.wlpLib, "com.ibm.genericCoreFeature_1.0.0.jar"));
        }
        assertEquals("Should have 3 download messages.", 3, counter[0]);
        assertEquals("Should have 2 reapplied message.", 2, counter[1]);
        assertEquals("Should have 2 reapplying message.", 2, counter[2]);
    }

    //Disabled for RTC 136527, use local ifix install
    //Re-enable with RTC 142782, update resolver to deal with locally installed ifix reapply
    //@Test
    public void testReapplyFixException() {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testReapplyFixException";
        initializeTestCase(testCaseName);
        File fixLpmf = new File(TestUtils.massiveRepoFiles, "9000-wlp-archive-IFPM0111_9.0.0.0.lpmf");
        File destFixLpmf = new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFPM0111_9.0.0.0.lpmf");
        TestUtils.copyFile(fixLpmf, destFixLpmf);

        File fixXml = new File(TestUtils.massiveRepoFiles, "9000-wlp-archive-IFPM0111_9.0.0.0.xml");
        File destFixXml = new File(TestUtils.wlpFixesDir, "9000-wlp-archive-IFPM0111_9.0.0.0.xml");
        TestUtils.copyFile(fixXml, destFixXml);

        assertTrue("files copied fail", (destFixLpmf.exists() && destFixXml.exists()));

        InstallKernel installKernel = getInstallKernel();
        try {
            installKernel.installFeature("com.ibm.genericCoreFeature",
                                         InstallConstants.TO_CORE,
                                         true,
                                         ExistsAction.fail);
            fail("Should get ReapplyFixException");
        } catch (ReapplyFixException e) {
            e.getCause().printStackTrace();
            Log.info(InstallKernelTest.class, testCaseName, "reapplyFixException msg= " + e.getMessage());
            Boolean checkMsg = e.getMessage().contains("Failed to reapply the following fixes:") && e.getMessage().contains("9000-wlp-archive-IFPM0111");
            assertTrue("ReapplyFixException is expected.", checkMsg);
        } catch (InstallException e) {
            e.printStackTrace();
            fail("Failed to install com.ibm.genericCoreFeature by other reason.");
        } finally {
            InstallUtils.delete(destFixLpmf);
            InstallUtils.delete(destFixXml);
        }
        try {
            installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
        } catch (InstallException e) {
            fail("Failed to uninstall com.ibm.genericCoreFeature");
        } finally {
            InstallUtils.delete(new File(TestUtils.wlpLib, "com.ibm.genericCoreFeature_1.0.0.jar"));
        }
    }

    @Test
    public void testInstallIBMFeature_SymbolicAndShortName() throws InterruptedException, InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallIBMFeature_SymbolicAndShortName";
        String featureShortName = "genericCoreFeature"; //short name for com.ibm.genericCoreFeature
        String featureSymbolicName = "com.ibm.genericCoreNoShortName"; // symbolic name for com.ibm.genericCoreNoShortName
        initializeTestCase(testCaseName);

        // create a list with two features
        ArrayList<String> features = new ArrayList<String>(2);
        features.add(featureShortName);
        features.add(featureSymbolicName);

        // install the features with installKernel
        InstallKernel installKernel = getInstallKernel();
        installKernel.installFeature(features, InstallConstants.TO_CORE, true, ExistsAction.fail);

        Set<File> filesShortName = new HashSet<File>();
        filesShortName.add(new File(TestUtils.wlpLibFeatures, "com.ibm.genericCoreFeature.mf"));
        filesShortName.add(new File(TestUtils.wlpLib, "com.ibm.genericCoreFeature_1.0.0.jar"));
        filesShortName.add(new File(TestUtils.libFeaturesl10n, "com.ibm.genericCoreFeature.properties"));
        filesShortName.add(new File(TestUtils.libFeaturesCheckSums, "com.ibm.genericCoreFeature.cs"));

        Set<File> filesSymbolicName = new HashSet<File>();
        filesSymbolicName.add(new File(TestUtils.wlpLibFeatures, "com.ibm.genericCoreNoShortName.mf"));
        filesSymbolicName.add(new File(TestUtils.wlpLib, "com.ibm.genericCoreNoShortName_1.0.0.jar"));
        filesSymbolicName.add(new File(TestUtils.libFeaturesl10n, "com.ibm.genericCoreNoShortName.properties"));
        filesSymbolicName.add(new File(TestUtils.libFeaturesCheckSums, "com.ibm.genericCoreNoShortName.cs"));

        // check if the files are installed
        checkInstalledFiles(filesSymbolicName, true);
        checkInstalledFiles(filesShortName, true);

        // clean up, try with short name or symbolic name?
        installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
        installKernel.uninstallFeature("com.ibm.genericCoreNoShortName", false);

        Log.info(InstallKernelTest.class, testCaseName, testCaseName + " complete");
    }

    @Test
    public void testInstallAssetNullOrEmptyList() throws RepositoryBackendIOException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallAssetNullOrEmptyList";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();
        try {
            installKernel.installAsset(null, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null);
            fail("InstallKernel.installAsset() didn't throw exception.");
        } catch (InstallException e) {
            assertTrue("CWWKF1249E is expected", e.getMessage().contains("CWWKF1249E"));
        }
        try {
            Collection<String> assets = new ArrayList<String>(0);
            installKernel.installAsset(assets, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null);
            fail("InstallKernel.installAsset() didn't throw exception.");
        } catch (InstallException e) {
            assertTrue("CWWKF1249E is expected", e.getMessage().contains("CWWKF1249E"));
        }
        Log.info(InstallKernelTest.class, testCaseName, testCaseName + " complete");
    }

    @Test
    public void testInstallSample() throws RepositoryBackendIOException, InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallSample";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();

        File server = new File(TestUtils.wlpDir, "usr/servers/SampleX");
        File commonKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/common.keys");
        File sampleXKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/sampleX.keys");
        List<File> serverFiles = new ArrayList<File>(3);
        serverFiles.add(server);
        serverFiles.add(commonKeys);
        serverFiles.add(sampleXKeys);

        // clean up
        try {
            installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
            installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
        } catch (InstallException e) {
        }
        TestUtils.cleanupFiles(serverFiles);

        // Install with dependents
        Collection<String> assets = new ArrayList<String>(0);
        assets.add("SampleX");
        Map<String, Collection<String>> installed = installKernel.installAsset(assets, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null);
        assertEquals("2 features should be installed", 2, installed.get(InstallConstants.FEATURE).size());
        assertEquals("1 sample should be installed", 1, installed.get(InstallConstants.SAMPLE).size());
        assertEquals("No addon should be installed", 0, installed.get(InstallConstants.ADDON).size());
        assertEquals("No ifix should be installed", 0, installed.get(InstallConstants.IFIX).size());
        assertEquals("No opensource should be installed", 0, installed.get(InstallConstants.OPENSOURCE).size());

        // Install where dependents already exist
        TestUtils.cleanupFiles(serverFiles);
        try {
            installed = installKernel.installAsset(assets, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null);
            assertEquals("No feature should be installed", 0, installed.get(InstallConstants.FEATURE).size());
            assertEquals("1 sample should be installed", 1, installed.get(InstallConstants.SAMPLE).size());
            assertEquals("No addon should be installed", 0, installed.get(InstallConstants.ADDON).size());
            assertEquals("No ifix should be installed", 0, installed.get(InstallConstants.IFIX).size());
            assertEquals("No opensource should be installed", 0, installed.get(InstallConstants.OPENSOURCE).size());
            TestUtils.verifyFiles(serverFiles, true);
        } finally {
            // clean up
            TestUtils.cleanupFiles(serverFiles);
            try {
                installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
                installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
            } catch (InstallException e) {
            }
        }
        Log.info(InstallKernelTest.class, testCaseName, testCaseName + " complete");
    }

    @Test
    public void testInstallMultipleSamplesAndFeature() throws RepositoryBackendIOException, InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallMultipleSamplesAndFeature";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();

        File serverX = new File(TestUtils.wlpDir, "usr/servers/SampleX");
        File serverY = new File(TestUtils.wlpDir, "usr/servers/SampleY");
        File commonKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/common.keys");
        File sampleXKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/sampleX.keys");
        File sampleYKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/sampleY.keys");
        List<File> serverFiles = new ArrayList<File>(3);
        serverFiles.add(serverX);
        serverFiles.add(serverY);
        serverFiles.add(commonKeys);
        serverFiles.add(sampleXKeys);
        serverFiles.add(sampleYKeys);

        // clean up
        try {
            installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
            installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
        } catch (InstallException e) {
        }
        TestUtils.cleanupFiles(serverFiles);

        try {
            Collection<String> assets = new ArrayList<String>(0);
            assets.add("SampleX");
            assets.add("SampleY");
            assets.add("genericCoreFeatureDependancyOnEsaPass");
            Map<String, Collection<String>> installed = installKernel.installAsset(assets, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null);
            assertEquals("2 features should be installed", 2, installed.get(InstallConstants.FEATURE).size());
            assertEquals("2 sample should be installed", 2, installed.get(InstallConstants.SAMPLE).size());
            assertEquals("No addon should be installed", 0, installed.get(InstallConstants.ADDON).size());
            assertEquals("No ifix should be installed", 0, installed.get(InstallConstants.IFIX).size());
            assertEquals("No opensource should be installed", 0, installed.get(InstallConstants.OPENSOURCE).size());
            TestUtils.verifyFiles(serverFiles, true);
        } finally {
            // clean up
            TestUtils.cleanupFiles(serverFiles);
            try {
                installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
                installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
            } catch (InstallException e) {
            }
        }
        Log.info(InstallKernelTest.class, testCaseName, testCaseName + " complete");
    }

    @Test
    public void testInstallAddon() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallAddon";
        initializeTestCase(testCaseName);

        replaceWlpProperties(null, "ND", "InstallationManager");

        InstallKernel installKernel = new InstallKernelImpl(TestUtils.wlpDir);
        String feature1 = "com.ibm.genericCoreFeatureDependancyOnEsaPass";
        String feature2 = "com.ibm.genericCoreFeature";
        String feature3 = "com.ibm.genericCoreFeatureModifyA";
        String feature4 = "com.ibm.installExtendedPackage-1.0";

        String[] files1 = { "/lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf", "/lib/com.ibm.genericCoreFeatureDependancyOnEsaPass_1.0.0.jar",
                            "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                            "/lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs" };

        String[] files2 = { "/lib/features/com.ibm.genericCoreFeature.mf",
                            "/lib/com.ibm.genericCoreFeature_1.0.0.jar",
                            "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                            "/lib/features/checksums/com.ibm.genericCoreFeature.cs" };

        String[] files3 = { "/lib/features/com.ibm.genericCoreFeatureModifyA.mf",
                            "/lib/com.ibm.genericCoreFeatureModifyA_1.0.0.jar",
                            "lib/features/l10n/com.ibm.genericCoreFeatureModifyA.properties",
                            "/lib/features/checksums/com.ibm.genericCoreFeatureModifyA.cs" };

        String[] files4 = { "/lib/assets/com.ibm.installExtendedPackage-1.0.mf",
                            "lib/assets/l10n/com.ibm.installExtendedPackage-1.0.properties" };

        deleteFiles(testCaseName, feature1, files1);
        deleteFiles(testCaseName, feature2, files2);
        deleteFiles(testCaseName, feature3, files3);
        deleteFiles(testCaseName, feature4, files4);

        try {
            Collection<String> assets = new ArrayList<String>(0);
            assets.add(feature4);
            Map<String, Collection<String>> installed = installKernel.installAsset(assets, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null);
            assertEquals("3 features should be installed", 3, installed.get(InstallConstants.FEATURE).size());
            assertEquals("No sample should be installed", 0, installed.get(InstallConstants.SAMPLE).size());
            assertEquals("1 addon should be installed", 1, installed.get(InstallConstants.ADDON).size());
            assertEquals("No ifix should be installed", 0, installed.get(InstallConstants.IFIX).size());
            assertEquals("No opensource should be installed", 0, installed.get(InstallConstants.OPENSOURCE).size());

            assertFilesExist(files1);
            assertFilesExist(files2);
            assertFilesExist(files3);
            assertFilesExist(files4);
        } finally {
            // clean up
            resetOriginalWlpProps();
            deleteFiles(testCaseName, feature1, files1);
            deleteFiles(testCaseName, feature2, files2);
            deleteFiles(testCaseName, feature3, files3);
            deleteFiles(testCaseName, feature4, files4);
        }
    }

    @Test
    public void testInstallSampleServerAlreadyExists() throws RepositoryBackendIOException, InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallSampleServerAlreadyExists";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();

        File server = new File(TestUtils.wlpDir, "usr/servers/SampleX");
        File commonKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/common.keys");
        File sampleXKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/sampleX.keys");
        List<File> serverFiles = new ArrayList<File>(3);
        serverFiles.add(server);
        serverFiles.add(commonKeys);
        serverFiles.add(sampleXKeys);

        // clean up
        try {
            installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
            installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
        } catch (InstallException e) {
        }
        TestUtils.cleanupFiles(serverFiles);

        // Install
        Collection<String> assets = new ArrayList<String>(0);
        assets.add("SampleX");
        Map<String, Collection<String>> installed = installKernel.installAsset(assets, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null);
        assertEquals("2 features should be installed", 2, installed.get(InstallConstants.FEATURE).size());
        assertEquals("1 sample should be installed", 1, installed.get(InstallConstants.SAMPLE).size());
        assertEquals("No addon should be installed", 0, installed.get(InstallConstants.ADDON).size());
        assertEquals("No ifix should be installed", 0, installed.get(InstallConstants.IFIX).size());
        assertEquals("No opensource should be installed", 0, installed.get(InstallConstants.OPENSOURCE).size());

        // Reinstall
        try {
            installed = installKernel.installAsset(assets, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null);
            fail("SampleX should not be installed.");
        } catch (InstallException e) {
            assertTrue("CWWKF1261E is expected", e.getMessage().contains("CWWKF1261E"));
        } finally {
            // clean up
            TestUtils.cleanupFiles(serverFiles);
            try {
                installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
                installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
            } catch (InstallException e) {
            }
        }
        Log.info(InstallKernelTest.class, testCaseName, testCaseName + " complete");
    }

    @Test
    public void testInstallMultipleSamplesAndFeatureWithNullLocalDir() throws RepositoryBackendIOException, InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallMultipleSamplesAndFeatureWithNullLocalDir";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();

        File serverX = new File(TestUtils.wlpDir, "usr/servers/SampleX");
        File serverY = new File(TestUtils.wlpDir, "usr/servers/SampleY");
        File commonKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/common.keys");
        File sampleXKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/sampleX.keys");
        File sampleYKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/sampleY.keys");
        List<File> serverFiles = new ArrayList<File>(3);
        serverFiles.add(serverX);
        serverFiles.add(serverY);
        serverFiles.add(commonKeys);
        serverFiles.add(sampleXKeys);
        serverFiles.add(sampleYKeys);

        // clean up
        try {
            installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
            installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
        } catch (InstallException e) {
        }
        TestUtils.cleanupFiles(serverFiles);

        try {
            Collection<String> assets = new ArrayList<String>(0);
            assets.add("SampleX");
            assets.add("SampleY");
            assets.add("genericCoreFeatureDependancyOnEsaPass");
            Map<String, Collection<String>> installed = installKernel.installAsset(assets, null, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null,
                                                                                   null);
            assertEquals("2 features should be installed", 2, installed.get(InstallConstants.FEATURE).size());
            assertEquals("2 sample should be installed", 2, installed.get(InstallConstants.SAMPLE).size());
            assertEquals("No addon should be installed", 0, installed.get(InstallConstants.ADDON).size());
            assertEquals("No ifix should be installed", 0, installed.get(InstallConstants.IFIX).size());
            assertEquals("No opensource should be installed", 0, installed.get(InstallConstants.OPENSOURCE).size());
            TestUtils.verifyFiles(serverFiles, true);
        } finally {
            // clean up
            TestUtils.cleanupFiles(serverFiles);
            try {
                installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
                installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
            } catch (InstallException e) {
            }
        }
        Log.info(InstallKernelTest.class, testCaseName, testCaseName + " complete");
    }

    @Test
    public void testInstallAddonWithLocalEmptyDir() throws Exception {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallAddonWithLocalEmptyDir";
        initializeTestCase(testCaseName);

        replaceWlpProperties(null, "ND", "InstallationManager");

        InstallKernel installKernel = new InstallKernelImpl(TestUtils.wlpDir);
        String feature1 = "com.ibm.genericCoreFeatureDependancyOnEsaPass";
        String feature2 = "com.ibm.genericCoreFeature";
        String feature3 = "com.ibm.genericCoreFeatureModifyA";
        String feature4 = "com.ibm.installExtendedPackage-1.0";

        String[] files1 = { "/lib/features/com.ibm.genericCoreFeatureDependancyOnEsaPass.mf", "/lib/com.ibm.genericCoreFeatureDependancyOnEsaPass_1.0.0.jar",
                            "lib/features/l10n/com.ibm.genericCoreFeatureDependancyOnEsaPass.properties",
                            "/lib/features/checksums/com.ibm.genericCoreFeatureDependancyOnEsaPass.cs" };

        String[] files2 = { "/lib/features/com.ibm.genericCoreFeature.mf",
                            "/lib/com.ibm.genericCoreFeature_1.0.0.jar",
                            "lib/features/l10n/com.ibm.genericCoreFeature.properties",
                            "/lib/features/checksums/com.ibm.genericCoreFeature.cs" };

        String[] files3 = { "/lib/features/com.ibm.genericCoreFeatureModifyA.mf",
                            "/lib/com.ibm.genericCoreFeatureModifyA_1.0.0.jar",
                            "lib/features/l10n/com.ibm.genericCoreFeatureModifyA.properties",
                            "/lib/features/checksums/com.ibm.genericCoreFeatureModifyA.cs" };

        String[] files4 = { "/lib/assets/com.ibm.installExtendedPackage-1.0.mf",
                            "lib/assets/l10n/com.ibm.installExtendedPackage-1.0.properties" };

        deleteFiles(testCaseName, feature1, files1);
        deleteFiles(testCaseName, feature2, files2);
        deleteFiles(testCaseName, feature3, files3);
        deleteFiles(testCaseName, feature4, files4);

        try {
            Collection<String> assets = new ArrayList<String>(0);
            assets.add(feature4);
            Map<String, Collection<String>> installed = installKernel.installAsset(assets, TestUtils.wlpEtc, new RepositoryConnectionList(MainRepository.createConnection()), null,
                                                                                   null, null, null);
            assertEquals("3 features should be installed", 3, installed.get(InstallConstants.FEATURE).size());
            assertEquals("No sample should be installed", 0, installed.get(InstallConstants.SAMPLE).size());
            assertEquals("1 addon should be installed", 1, installed.get(InstallConstants.ADDON).size());
            assertEquals("No ifix should be installed", 0, installed.get(InstallConstants.IFIX).size());
            assertEquals("No opensource should be installed", 0, installed.get(InstallConstants.OPENSOURCE).size());

            assertFilesExist(files1);
            assertFilesExist(files2);
            assertFilesExist(files3);
            assertFilesExist(files4);
        } finally {
            // clean up
            resetOriginalWlpProps();
            deleteFiles(testCaseName, feature1, files1);
            deleteFiles(testCaseName, feature2, files2);
            deleteFiles(testCaseName, feature3, files3);
            deleteFiles(testCaseName, feature4, files4);
        }
    }

    @Test
    public void testInstallSampe() throws RepositoryBackendIOException, InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallSampe";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();

        File server = new File(TestUtils.wlpDir, "usr/servers/SampleUnreach3rdPartyLib");
        File commonKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/common.keys");
        File sampleUnreach3rdPartyLibKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/SampleUnreach3rdPartyLib.keys");
        List<File> serverFiles = new ArrayList<File>(3);
        serverFiles.add(server);
        serverFiles.add(commonKeys);
        serverFiles.add(sampleUnreach3rdPartyLibKeys);

        // clean up
        try {
            installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
            installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
        } catch (InstallException e) {
        }
        TestUtils.cleanupFiles(serverFiles);

        // Install
        Collection<String> assets = new ArrayList<String>(0);
        assets.add("SampleUnreach3rdPartyLib");
        try {
            installKernel.installAsset(assets, null, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null, true);
            fail("SampleUnreach3rdPartyLib should not be installed.");
        } catch (InstallException e) {
            assertTrue("Expected CWWKF1264E", e.getMessage().contains("CWWKF1264E"));
        }

        try {
            Map<String, Collection<String>> installed = installKernel.installAsset(assets, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null);
            assertEquals("2 features should be installed", 2, installed.get(InstallConstants.FEATURE).size());
            assertEquals("1 sample should be installed", 1, installed.get(InstallConstants.SAMPLE).size());
            assertEquals("No addon should be installed", 0, installed.get(InstallConstants.ADDON).size());
            assertEquals("No ifix should be installed", 0, installed.get(InstallConstants.IFIX).size());
            assertEquals("No opensource should be installed", 0, installed.get(InstallConstants.OPENSOURCE).size());
            TestUtils.verifyFiles(serverFiles, true);
        } finally {
            // clean up
            TestUtils.cleanupFiles(serverFiles);
            try {
                installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
                installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
            } catch (InstallException e) {
            }
        }
        Log.info(InstallKernelTest.class, testCaseName, testCaseName + " complete");
    }

    @Test
    public void testInstallOpenSource() throws RepositoryBackendIOException, InstallException {
        Assume.assumeTrue(TestUtils.connectedToRepo);
        String testCaseName = "testInstallOpenSource";
        initializeTestCase(testCaseName);
        InstallKernel installKernel = getInstallKernel();

        File server = new File(TestUtils.wlpDir, "usr/servers/OpenSourceUnreach3rdPartyLib");
        File commonKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/common.keys");
        File openSourceUnreach3rdPartyLibKeys = new File(TestUtils.wlpDir, "usr/shared/resources/security/OpenSourceUnreach3rdPartyLib.keys");
        List<File> serverFiles = new ArrayList<File>(3);
        serverFiles.add(server);
        serverFiles.add(commonKeys);
        serverFiles.add(openSourceUnreach3rdPartyLibKeys);

        // clean up
        try {
            installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
            installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
        } catch (InstallException e) {
        }
        TestUtils.cleanupFiles(serverFiles);

        // Install
        Collection<String> assets = new ArrayList<String>(0);
        assets.add("OpenSourceUnreach3rdPartyLib");
        try {
            Log.info(InstallKernelTest.class, testCaseName, "calling installKernel.installAssets to install OpenSourceUnreach3rdPartyLib. Exception should be thrown.");
            installKernel.installAsset(assets, null, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null, null, true);
            fail("OpenSourceUnreach3rdPartyLib should not be installed.");
        } catch (InstallException e) {
            // Adding debugging/logging info
            Log.info(InstallKernelTest.class, testCaseName, "Exception.getMessage:" + e.getMessage());
            Log.info(InstallKernelTest.class, testCaseName, "Exception.RC:" + e.getRc());
            assertTrue("Expected CWWKF1264E", e.getMessage().contains("CWWKF1264E"));
        }

        try {
            Map<String, Collection<String>> installed = installKernel.installAsset(assets, null, new RepositoryConnectionList(MainRepository.createConnection()), null, null, null,
                                                                                   null, false);
            assertEquals("2 features should be installed", 2, installed.get(InstallConstants.FEATURE).size());
            assertEquals("No sample should be installed", 0, installed.get(InstallConstants.SAMPLE).size());
            assertEquals("No addon should be installed", 0, installed.get(InstallConstants.ADDON).size());
            assertEquals("No ifix should be installed", 0, installed.get(InstallConstants.IFIX).size());
            assertEquals("1 opensource should be installed", 1, installed.get(InstallConstants.OPENSOURCE).size());
            TestUtils.verifyFiles(serverFiles, true);
        } finally {
            // clean up
            TestUtils.cleanupFiles(serverFiles);
            try {
                installKernel.uninstallFeature("com.ibm.genericCoreFeatureDependancyOnEsaPass", false);
                installKernel.uninstallFeature("com.ibm.genericCoreFeature", false);
            } catch (InstallException e) {
            }
        }
        Log.info(InstallKernelTest.class, testCaseName, testCaseName + " complete");
    }
}
