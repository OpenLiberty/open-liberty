/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.Test;

import com.ibm.ws.install.internal.InstallKernelImpl;

/**
 *
 */
public class UninstallKernelTest {

    private static File getInstallDir(String testName) throws Exception {
        File installDir = new File("build/unittest/wlpDirs/" + testName + "/developers/wlp").getAbsoluteFile();
        System.out.println("getInstallDir() installDir set to " + installDir);
        if (installDir == null || !installDir.exists())
            throw new IllegalArgumentException("Test requires an existing root directory, but it could not be found: " + installDir.getAbsolutePath());

        File libFeatureDir = new File(String.format("%s/lib/features", installDir.getAbsoluteFile()));
        File libPlatformDir = new File(String.format("%s/lib/platform", installDir.getAbsoluteFile()));

        FilenameFilter mfFilenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".mf");
            }
        };
        int numFeatureMF = libFeatureDir.list(mfFilenameFilter).length;
        int numPlatformMF = libPlatformDir.list(mfFilenameFilter).length;
        if (numFeatureMF == 0 || numPlatformMF == 0)
            throw new IllegalArgumentException(String.format("Some features required by this test are missing"));
        System.out.println("getInstallDir() java.io.tmpdir set to " + System.getProperty("java.io.tmpdir"));
        return installDir;
    }

    @Test
    public void testUninstallProduct() throws Exception {
        File installDir = getInstallDir("testUninstallProduct");
        InstallKernel installKernel = new InstallKernelImpl(installDir);
        installKernel.uninstallFeaturesByProductId("com.ibm.websphere.appserver");
        File featureDir = new File(installDir, "lib/features");
        File[] mfFiles = featureDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mf");
            }
        });
        assertNotNull("Failed to get " + featureDir.getAbsolutePath(), mfFiles);
        assertEquals("Expect no mf files lib/features", 0, mfFiles.length);

        File platformDir = new File(installDir, "lib/platform");
        mfFiles = platformDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mf");
            }
        });
        assertNotNull("Failed to get " + platformDir.getAbsolutePath(), mfFiles);
        assertEquals("Expect no mf files at lib/platform", 0, mfFiles.length);

        File assetDir = new File(installDir, "lib/assets");
        mfFiles = assetDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mf");
            }
        });
        assertNotNull("Failed to get " + assetDir.getAbsolutePath(), mfFiles);
        assertEquals("Expect no mf files at lib/assets", 0, mfFiles.length);

        File usrfeatureDir = new File(installDir, "usr/extension/lib/features");
        mfFiles = usrfeatureDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mf");
            }
        });
        assertNotNull("Failed to get " + usrfeatureDir.getAbsolutePath(), mfFiles);
        assertEquals("Expect no mf files usr/extension/lib/features", 2, mfFiles.length);
    }

    @Test
    public void testUninstallProductFeatures() throws Exception {
        File installDir = getInstallDir("testUninstallProductFeatures");
        InstallKernel installKernel = new InstallKernelImpl(installDir);
        installKernel.uninstallProductFeatures("com.ibm.websphere.appserver", null);
        File featureDir = new File(installDir, "lib/features");
        File[] mfFiles = featureDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mf");
            }
        });
        assertNotNull("Failed to get " + featureDir.getAbsolutePath(), mfFiles);
        assertEquals("Expect no mf files lib/features", 0, mfFiles.length);

        File platformDir = new File(installDir, "lib/platform");
        mfFiles = platformDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mf");
            }
        });
        assertNotNull("Failed to get " + platformDir.getAbsolutePath(), mfFiles);
        assertEquals("Expect some mf files at lib/platform", 6, mfFiles.length);

        File assetDir = new File(installDir, "lib/assets");
        mfFiles = assetDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mf");
            }
        });
        assertNotNull("Failed to get " + assetDir.getAbsolutePath(), mfFiles);
        assertEquals("Expect no mf files at lib/assets", 0, mfFiles.length);

        File usrfeatureDir = new File(installDir, "usr/extension/lib/features");
        mfFiles = usrfeatureDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mf");
            }
        });
        assertNotNull("Failed to get " + usrfeatureDir.getAbsolutePath(), mfFiles);
        assertEquals("Expect no mf files usr/extension/lib/features", 2, mfFiles.length);
    }
}
