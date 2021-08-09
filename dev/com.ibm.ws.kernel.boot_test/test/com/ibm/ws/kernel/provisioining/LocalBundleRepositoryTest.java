/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioining;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.VersionRange;

import test.common.SharedOutputManager;
import test.shared.TestUtils;

import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;

/**
 *
 */
public class LocalBundleRepositoryTest {
    private final SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");
    @Rule
    public final TestRule outputRule = outputMgr;

    private static File cacheDir;

    @BeforeClass
    public static void setupCache() throws IOException {
        cacheDir = TestUtils.createTempDirectory("cache.data");
    }

    /**
     * This method checks that if the location attribute identifies an exact file we use that rather than applying the iFix selection rules.
     */
    @Test
    public void testFullySpecifiedLocation() {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        ContentBasedLocalBundleRepository lbr = new ContentBasedLocalBundleRepository(new File(testClassesDir + "/test data/lbr"), cacheDir, true);
        File f = lbr.selectBundle("lib/a.b_1.0.jar", "a.b", new VersionRange("0.0.0"));
        assertNotNull("A file could not be located", f);
        assertEquals("The file name is not correct", "a.b_1.0.jar", f.getName());
        assertTrue("The file is not a file", f.isFile());
        assertTrue("The file does not exist", f.exists());
        lbr.dispose();
    }

    /**
     * This method tests that if we don't have iFixes, or fix pack updates to a bundle we install the correct bundle. This
     * is very low level belts and braces. If we don't do this right all BVTs are going to fail.
     */
    @Test
    public void testSelectSingleMatch() {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        ContentBasedLocalBundleRepository lbr = new ContentBasedLocalBundleRepository(new File(testClassesDir + "/test data/lbr"), cacheDir, true);
        File f = lbr.selectBundle("", "x.y", new VersionRange("[1.0.0,1.0.100)"));
        assertNotNull("A file could not be located", f);
        assertEquals("The file name is not correct", "x.y_1.0.jar", f.getName());
        assertTrue("The file is not a file", f.isFile());
        assertTrue("The file does not exist", f.exists());
        lbr.dispose();
    }

    /**
     * This is the key iFix install routine. It is testing that when we have multiple iFixes for a fix pack we select the most
     * recent one. It also checks that we ignore iFixes for fixpack jars that have not been installed. So this is really important.
     */
    @Test
    public void testSelectMultiMatch() {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        ContentBasedLocalBundleRepository lbr = new ContentBasedLocalBundleRepository(new File(testClassesDir + "/test data/lbr"), cacheDir, true);
        File f = lbr.selectBundle("", "a.b", new VersionRange("[1.0.0,1.0.100)"));
        assertNotNull("A file could not be located", f);
        assertEquals("The file name is not correct", "a.b_1.0.1.v2.jar", f.getName());
        assertTrue("The file is not a file", f.isFile());
        assertTrue("The file does not exist", f.exists());
        assertTrue("The ifix warning message (CWWKE0060W) was not output", outputMgr.checkForMessages("CWWKE0060W"));
        assertTrue("The ifix warning message did not identify the iFix a.b_1.0.2.v1.jar", outputMgr.checkForMessages("a.b_1.0.2.v1.jar"));
        assertTrue("The ifix warning message did not identify the base a.b_1.0.2.v1.jar", outputMgr.checkForMessages("a.b_1.0.2.jar"));
        lbr.dispose();
    }

    /**
     * This test ensures that if you have different bundles with the same symbolic name but different versions, and the bundle versions don't have
     * qualifiers i.e. they are not ifix/apar bundles, that the bundle with the highest version is selected.
     */
    @Test
    public void testMultipleVersionedBundlesWithNoQualifiers() {
        // We specifically don't want the cache because we are using a different bundle repository. If we use the cache this goes boom.
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        ContentBasedLocalBundleRepository lbr = new ContentBasedLocalBundleRepository(new File(testClassesDir + "/test data/nonIFixLBR"), null, true);
        File f = lbr.selectBundle("", "a.b", new VersionRange("[1.0.0,1.0.100)"));
        assertNotNull("A file could not be located", f);
        assertEquals("The file name is not correct", "a.b_1.0.1.jar", f.getName());
        assertTrue("The file is not a file", f.isFile());
        assertTrue("The file does not exist", f.exists());
        lbr.dispose();
    }

    /**
     * This method checks that we can select jars from two locations. We should pick the most recent from both locations. In
     * the test the base jar 1.0.2 is in dev and the iFixes are in lib. We should pick up the iFixes and ignore the ones in
     * dev.
     */
    @Test
    public void testMultipleLocations() {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        ContentBasedLocalBundleRepository lbr = new ContentBasedLocalBundleRepository(new File(testClassesDir + "/test data/lbr"), cacheDir, true);
        File f = lbr.selectBundle("dev/,lib/", "a.b", new VersionRange("[1.0.0,1.0.100)"));
        assertNotNull("A file could not be located", f);
        assertEquals("The file name is not correct", "a.b_1.0.2.v1.jar", f.getName());
        assertTrue("The file is not a file", f.isFile());
        assertTrue("The file does not exist", f.exists());
        f = lbr.selectBundle("dev/", "a.b", new VersionRange("[1.0.0,1.0.100)"));
        assertNotNull("A file could not be located", f);
        assertEquals("The file name is not correct", "bad.jar", f.getName());
        assertTrue("The file is not a file", f.isFile());
        assertTrue("The file does not exist", f.exists());
        lbr.dispose();
    }

    /**
     * This method checks that we can select and return jars that don't match the naming convetion, but where the content
     * correctly identifies the jar. In this case we select dev and lib and select bundles up to 1.0.2, excluding iFixes.
     * The jar is called bad.jar, but the SymbolicName is a.b and version is 1.0.2.
     */
    @Test
    public void testBadlyNamedJar() {
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        ContentBasedLocalBundleRepository lbr = new ContentBasedLocalBundleRepository(new File(testClassesDir + "/test data/lbr"), cacheDir, true);
        File f = lbr.selectBundle("dev/,lib/", "a.b", new VersionRange("[1.0.0,1.0.2]"));
        assertNotNull("A file could not be located", f);
        assertEquals("The file name is not correct", "bad.jar", f.getName());
        assertTrue("The file is not a file", f.isFile());
        assertTrue("The file does not exist", f.exists());
        lbr.dispose();
    }
}