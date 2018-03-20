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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.internal.InstallUtils.InputStreamFileWriter;
import com.ibm.ws.install.internal.asset.JarAsset;
import com.ibm.ws.install.internal.asset.OpenSourceAsset;
import com.ibm.ws.install.internal.asset.SampleAsset;

public class JarAssetTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testSampleAsset() throws MalformedURLException, IOException {
        final String m = "testSampleAsset";
        File srcFile = new File("publish/massiveRepo/samples/SampleX.jar");
        File jarFile = new File("build/unittest/tmp/SampleX.jar");
        new InputStreamFileWriter(srcFile.getCanonicalFile().toURI().toURL().openConnection().getInputStream()).writeToFile(jarFile);
        try {
            SampleAsset sampleAsset = new SampleAsset("SampleX", "SampleX", jarFile, true);

            assertFalse("SampleAsset.isFeature() should return false.", sampleAsset.isFeature());

            assertFalse("SampleAsset.isFix() should return false.", sampleAsset.isFix());

            assertTrue("SampleAsset.isSample() should return true.", sampleAsset.isSample());

            assertFalse("SampleAsset.isOpenSource() should return false.", sampleAsset.isOpenSource());

            assertTrue("SampleAsset.getJar().getName()", sampleAsset.getJar().getName().endsWith("SampleX.jar"));

            assertTrue("SampleAsset.getShortName()", sampleAsset.getShortName().equals("SampleX"));

            JarAsset jarAsset = sampleAsset;

            assertTrue("JarAsset.isSample() should return true.", jarAsset.isSample());

            assertFalse("JarAsset.isOpenSource() should return false.", jarAsset.isOpenSource());

            sampleAsset.delete();
            assertFalse("JarAsset should be deleted", jarFile.exists());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testOpenSourceAsset() throws MalformedURLException, IOException {
        final String m = "testOpenSourceAsset";
        File srcFile = new File("publish/massiveRepo/samples/SampleX.jar");
        File jarFile = new File("build/unittest/tmp/SampleX.jar");
        new InputStreamFileWriter(srcFile.getCanonicalFile().toURI().toURL().openConnection().getInputStream()).writeToFile(jarFile);
        try {
            OpenSourceAsset openSourceAsset = new OpenSourceAsset("SampleX", "SampleX", jarFile, true);

            assertFalse("OpenSourceAsset.isFeature() should return false.", openSourceAsset.isFeature());

            assertFalse("OpenSourceAsset.isFix() should return false.", openSourceAsset.isFix());

            assertFalse("OpenSourceAsset.isSample() should return true.", openSourceAsset.isSample());

            assertTrue("OpenSourceAsset.isOpenSource() should return false.", openSourceAsset.isOpenSource());

            assertTrue("OpenSourceAsset.getJar().getName()", openSourceAsset.getJar().getName().endsWith("SampleX.jar"));

            assertTrue("OpenSourceAsset.getShortName()", openSourceAsset.getShortName().equals("SampleX"));

            JarAsset jarAsset = openSourceAsset;

            assertFalse("JarAsset.isSample() should return true.", jarAsset.isSample());

            assertTrue("JarAsset.isOpenSource() should return false.", jarAsset.isOpenSource());

            openSourceAsset.delete();
            assertFalse("JarAsset should be deleted", jarFile.exists());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
