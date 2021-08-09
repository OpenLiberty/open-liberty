/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.install.internal.InstallKernelImpl;
import com.ibm.ws.kernel.feature.internal.cmdline.FeatureToolException;

import test.common.SharedOutputManager;

public class InvalidInstallDirTest {

    @Rule
    public static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

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
