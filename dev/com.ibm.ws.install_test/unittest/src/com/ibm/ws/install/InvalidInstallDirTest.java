package com.ibm.ws.install;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.kernel.feature.internal.cmdline.FeatureToolException;

public class InvalidInstallDirTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testInstallKernel_getInstalledFeaturesForNotExistDirectory() {
        File installDir = new File("non-exists");
        assertFalse(installDir.getAbsolutePath() + " should not exist", installDir.exists());
        InstallKernelImpl installKernel = new InstallKernelImpl(installDir);
        try {
            installKernel.getInstalledFeatures(InstallConstants.TO_CORE);
            fail("Expected FeatureToolException");
        } catch (FeatureToolException rte) {
        }
        assertNull("InstallKernel.getInstalledFeatures() is expected to return null", installKernel.getInstalledFeatures());
        assertTrue("InstallKernel.getInstalledFeatureCollections() is expected to return empty", installKernel.getInstalledFeatureCollections().isEmpty());
    }
}
