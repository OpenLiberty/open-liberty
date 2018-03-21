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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.zip.ZipException;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.install.internal.InstallUtils.InputStreamFileWriter;
import com.ibm.ws.install.internal.asset.FixAsset;

/**
 *
 */
public class FixAssetTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Test
    public void testNonExistFix() {
        File jarFile = new File("unknown");
        try {
            new FixAsset("unknown", jarFile, true);
            fail("FixAsset should not be created");
        } catch (ZipException e) {
            // Expected exception for other platforms
        } catch (IOException e) {
            // Expected exception for Win32
        }
    }

    @Test
    public void testInvalidFix() {
        final String m = "testInvalidFix";
        File jarFile = new File("publish/massiveRepo/features/invalid.esa");
        try {
            new FixAsset("invalid", jarFile, true);
            fail("FixAsset should not be created");
        } catch (ZipException e) {
            // Expected exception
        } catch (IOException e) {
            outputMgr.failWithThrowable(m, e);
        }
    }

    @Test
    public void testFixAsset() throws MalformedURLException, IOException {
        final String m = "testFixAsset";
        File srcFile = new File("publish/massiveRepo/fixes/8550-wlp-archive-ifpm89011.jar");
        File fixFile = new File("build/unittest/tmp/8550-wlp-archive-ifpm89011-temp.jar");
        new InputStreamFileWriter(srcFile.getCanonicalFile().toURI().toURL().openConnection().getInputStream()).writeToFile(fixFile);
        try {
            FixAsset fixAsset = new FixAsset("8550-wlp-archive-ifpm89011", fixFile, true);

            assertFalse("FixAsset.isFeature() should return false.", fixAsset.isFeature());

            assertTrue("FixAsset.isFix() should return true.", fixAsset.isFix());

            assertTrue("FixAsset.getJar().getName()", fixAsset.getJar().getName().endsWith("8550-wlp-archive-ifpm89011-temp.jar"));

            assertEquals("FixAsset.getMainAttributes().getValue(\"Bundle-Vendor\")", "IBM", fixAsset.getMainAttributes().getValue("Bundle-Vendor"));

            fixAsset.delete();
            assertFalse("FixAsset should be deleted", fixFile.exists());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
