package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.internal.ChecksumsManager;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.kernel.boot.cmdline.Utils;

public class ChecksumsManagerTest {

    private static File imageDir;
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        imageDir = new File("build/unittest/wlpDirs/developers/wlp").getAbsoluteFile();
        System.out.println("setUpBeforeClass() imageDir set to " + imageDir);
        if (imageDir == null || !imageDir.exists())
            throw new IllegalArgumentException("Test requires an existing root directory, but it could not be found: " + imageDir.getAbsolutePath());

        Utils.setInstallDir(imageDir);
    }

    @Test
    public void testUpdateChecksums() {
        File featureCSA = null;
        File featureCSB = null;
        File featureCSC = null;

        try {
            File featureDir = new File(imageDir, "lib/features");
            File checksumDir = new File(featureDir, "checksums");
            if (!checksumDir.exists()) {
                checksumDir.mkdir();
            }

            featureCSA = new File(checksumDir, "featureCSA.cs");
            Properties featureCSAProps = new Properties();
            featureCSAProps.put("lib/csaBundle.jar", "1111");
            featureCSAProps.put("lib/commonBundle.jar", "2222");
            featureCSAProps.put("dev/csaFile.zip", "3333");
            featureCSAProps.put("dev/common.zip", "4444");
            ChecksumsManager.saveChecksumFile(featureCSA, featureCSAProps, "init");

            featureCSB = new File(checksumDir, "featureCSB.cs");
            Properties featureCSBProps = new Properties();
            featureCSBProps.put("lib/csbBundle.jar", "5555");
            featureCSBProps.put("lib/commonBundle.jar", "22222");
            featureCSBProps.put("dev/csbFile.zip", "6666");
            featureCSBProps.put("dev/common.zip", "44444");
            featureCSBProps.put("dev/extra.zip", "99");
            ChecksumsManager.saveChecksumFile(featureCSB, featureCSBProps, "init");

            ChecksumsManager csManager = new ChecksumsManager();
            csManager.registerNewChecksums(featureDir, "lib/commonBundle.jar", "222222");
            csManager.registerNewChecksums(new File(imageDir, "lib/features"), "dev/common.zip", "444444");
            csManager.registerNewChecksums(new File(imageDir, "lib/features"), "lib/commonBundle.jar", "22222");
            csManager.registerNewChecksums(featureDir, "dev/common.zip", "44444");
            csManager.updateChecksums();

            featureCSAProps = ChecksumsManager.loadChecksumFile(featureCSA);
            assertEquals("lib/commonBundle.jar should be 22222", featureCSAProps.getProperty("lib/commonBundle.jar"), "22222");
            assertEquals("dev/common.zip should be 44444", featureCSAProps.getProperty("dev/common.zip"), "44444");

            featureCSAProps.put("lib/commonBundle.jar", "222");
            featureCSAProps.put("dev/common.zip", "444");
            ChecksumsManager.saveChecksumFile(featureCSA, featureCSAProps, "init");
            featureCSC = new File(checksumDir, "featureCSC.cs");
            Properties featureCSCProps = new Properties();
            featureCSCProps.put("lib/cscBundle.jar", "7777");
            featureCSCProps.put("lib/commonBundle.jar", "222");
            featureCSCProps.put("dev/cscFile.zip", "8888");
            featureCSCProps.put("dev/common.zip", "444");
            ChecksumsManager.saveChecksumFile(featureCSC, featureCSCProps, "init");

            csManager = new ChecksumsManager();
            csManager.registerExistingChecksums(featureDir, "featureCSB", "lib/commonBundle.jar");
            csManager.registerExistingChecksums(new File(imageDir, "lib/features"), "featureCSB", "dev/common.zip");
            csManager.registerExistingChecksums(new File(imageDir, "lib/features"), "featureCSB", "lib/commonBundle.jar");
            csManager.registerExistingChecksums(featureDir, "featureCSB", "dev/common.zip");
            csManager.registerExistingChecksums(featureDir, "featureCSB", "dev/extra.zip");
            csManager.updateChecksums();

            featureCSBProps = ChecksumsManager.loadChecksumFile(featureCSB);
            assertEquals("lib/commonBundle.jar should be 222", featureCSBProps.getProperty("lib/commonBundle.jar"), "222");
            assertEquals("dev/common.zip should be 444", featureCSBProps.getProperty("dev/common.zip"), "444");
            assertEquals("dev/extra.zip should be 99", featureCSBProps.getProperty("dev/extra.zip"), "99");
        } catch (Exception e) {
            outputMgr.failWithThrowable("testUpdateChecksums", e);
        } finally {
            InstallUtils.delete(featureCSA);
            InstallUtils.delete(featureCSB);
            InstallUtils.delete(featureCSC);
        }
    }

    @Test
    public void testUpdatePlatformChecksums() {
        File platformCSA = null;
        File featureCSB = null;

        try {
            File featureDir = new File(imageDir, "lib/features");
            File featureChecksumDir = new File(featureDir, "checksums");
            if (!featureChecksumDir.exists()) {
                featureChecksumDir.mkdir();
            }

            File platformDir = new File(imageDir, "lib/platform");
            File platformChecksumDir = new File(platformDir, "checksums");
            if (!platformChecksumDir.exists()) {
                platformChecksumDir.mkdir();
            }

            platformCSA = new File(platformChecksumDir, "platformCSA.cs");
            Properties platformCSAProps = new Properties();
            platformCSAProps.put("lib/csaBundle.jar", "1111");
            platformCSAProps.put("lib/commonBundle.jar", "2222");
            platformCSAProps.put("dev/csaFile.zip", "3333");
            platformCSAProps.put("dev/common.zip", "4444");
            ChecksumsManager.saveChecksumFile(platformCSA, platformCSAProps, "init");

            featureCSB = new File(featureChecksumDir, "featureCSB.cs");
            Properties featureCSBProps = new Properties();
            featureCSBProps.put("lib/csbBundle.jar", "5555");
            featureCSBProps.put("lib/commonBundle.jar", "22222");
            featureCSBProps.put("dev/csbFile.zip", "6666");
            featureCSBProps.put("dev/common.zip", "44444");
            featureCSBProps.put("dev/extra.zip", "99");
            ChecksumsManager.saveChecksumFile(featureCSB, featureCSBProps, "init");

            ChecksumsManager csManager = new ChecksumsManager();
            csManager.registerNewChecksums(featureDir, "lib/commonBundle.jar", "222222");
            csManager.registerNewChecksums(new File(imageDir, "lib/features"), "dev/common.zip", "444444");
            csManager.registerNewChecksums(new File(imageDir, "lib/features"), "lib/commonBundle.jar", "22222");
            csManager.registerNewChecksums(featureDir, "dev/common.zip", "44444");
            csManager.updateChecksums();

            platformCSAProps = ChecksumsManager.loadChecksumFile(platformCSA);
            assertEquals("lib/commonBundle.jar should be 22222", platformCSAProps.getProperty("lib/commonBundle.jar"), "22222");
            assertEquals("dev/common.zip should be 44444", platformCSAProps.getProperty("dev/common.zip"), "44444");

            platformCSAProps.put("lib/commonBundle.jar", "222");
            platformCSAProps.put("dev/common.zip", "444");
            ChecksumsManager.saveChecksumFile(platformCSA, platformCSAProps, "init");

            csManager = new ChecksumsManager();
            csManager.registerExistingChecksums(featureDir, "featureCSB", "lib/commonBundle.jar");
            csManager.registerExistingChecksums(new File(imageDir, "lib/features"), "featureCSB", "dev/common.zip");
            csManager.registerExistingChecksums(new File(imageDir, "lib/features"), "featureCSB", "lib/commonBundle.jar");
            csManager.registerExistingChecksums(featureDir, "featureCSB", "dev/common.zip");
            csManager.registerExistingChecksums(featureDir, "featureCSB", "dev/extra.zip");
            csManager.updateChecksums();

            featureCSBProps = ChecksumsManager.loadChecksumFile(featureCSB);
            assertEquals("lib/commonBundle.jar should be 222", featureCSBProps.getProperty("lib/commonBundle.jar"), "222");
            assertEquals("dev/common.zip should be 444", featureCSBProps.getProperty("dev/common.zip"), "444");
            assertEquals("dev/extra.zip should be 99", featureCSBProps.getProperty("dev/extra.zip"), "99");
        } catch (Exception e) {
            outputMgr.failWithThrowable("testUpdateChecksums", e);
        } finally {
            InstallUtils.delete(platformCSA);
            InstallUtils.delete(featureCSB);
        }
    }
}
