/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.util.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 * Tests for the password utility class.
 */
public class CustomManifestTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private static final String testBuildDir = System.getProperty("test.buildDir", "generated");
    private static final String VALUE_EXTENSION_JAR_LOCATION = testBuildDir + "/test/test_data/custom_encryption/bin/tools/extensions/ws-customPasswordEncryption";
    private static final String JAR_VALID = "/valid.jar";
    private static final String JAR_NO_IMPLCLASS = "/noImplClass.jar";
    private static final String JAR_NO_FEATURE = "/noFeature.jar";
    private static final String JAR_NO_DESCRIPTION = "/noDescription.jar";
    private static final String KEY_PROP_INSTALL_DIR = "wlp.install.dir";
    private static final String VALUE_PROP_INSTALL_DIR = testBuildDir + "/test/test_data/custom_encryption";
    private static final String MSG_NO_IMPLCLASS = "IBM-ImplementationClass";
    private static final String VALUE_NAME = "valid.jar";
    private static final String VALUE_LOCATION = "bin/tools/extensions/ws-customPasswordEncryption/valid.jar";
    private static final String VALUE_IMPLCLASS = "com.ibm.ws.crypto.util.custom.SimpleCustomEncryption";
    private static final String VALUE_ALGORITHM = "custom";
    private static final String VALUE_FEATURENAME = "valid-1.0";
    private static final String VALUE_DESCRIPTION = "valid default resource";
    private static final String VALUE_DEFAULT_DESCRIPTION = "Custom password encryption";

    private static final String NO_MANIFEST_MF = testBuildDir + "/test/test_data/custom_encryption/user/extension/lib/features/noManifest.mf";

    @BeforeClass
    public static void traceSetUp() {
        outputMgr.trace("*=all");
    }

    @AfterClass
    public static void traceTearDown() {
        outputMgr.trace("*=all=disabled");
    }

    /**
     * Test CustomManifest constructor with the valid data.
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     */
    @Test
    public void testCustomManifestNormal() throws IllegalArgumentException, IOException {
        String dir = VALUE_PROP_INSTALL_DIR;
        String currentValue = System.setProperty(KEY_PROP_INSTALL_DIR, dir);
        CustomManifest cm = new CustomManifest(new File(VALUE_EXTENSION_JAR_LOCATION + JAR_VALID));
        if (currentValue != null) {
            System.setProperty(KEY_PROP_INSTALL_DIR, currentValue);
        }
        assertNotNull("CustomManifest should be constructed with the valid data.", cm);
        assertEquals("The name is invalid.", VALUE_NAME, cm.getName());
        String expectedLocation = dir.replace('\\', '/') + '/' + VALUE_LOCATION;
        assertEquals("The location is invalid.", expectedLocation, cm.getLocation().replace('\\', '/'));
        assertEquals("The implclass is invalid.", VALUE_IMPLCLASS, cm.getImplClass());
        assertEquals("The algorithm is invalid.", VALUE_ALGORITHM, cm.getAlgorithm());
        assertEquals("The feature name is invalid.", VALUE_FEATURENAME, cm.getFeatureName());
        assertEquals("The description is invalid.", VALUE_DESCRIPTION, cm.getDescription());
    }

    /**
     * Test CustomManifest constructor with the valid data.
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     */
    @Test
    public void testCustomManifestNormalNoDescription() throws IllegalArgumentException, IOException {
        String dir = VALUE_PROP_INSTALL_DIR;
        String currentValue = System.setProperty(KEY_PROP_INSTALL_DIR, dir);
        CustomManifest cm = new CustomManifest(new File(VALUE_EXTENSION_JAR_LOCATION + JAR_NO_DESCRIPTION));
        if (currentValue != null) {
            System.setProperty(KEY_PROP_INSTALL_DIR, currentValue);
        }
        assertNotNull("CustomManifest should be constructed with the valid data.", cm);
        assertEquals("The description is invalid.", VALUE_DEFAULT_DESCRIPTION, cm.getDescription());
    }

    /**
     * Test CustomManifest constructor with the invalid data.
     * 
     * @throws IOException
     */
    @Test
    public void testCustomManifestNoImplClass() throws IOException {
        String currentValue = null;
        try {
            String dir = VALUE_PROP_INSTALL_DIR;
            currentValue = System.setProperty(KEY_PROP_INSTALL_DIR, dir);
            new CustomManifest(new File(VALUE_EXTENSION_JAR_LOCATION + JAR_NO_IMPLCLASS));
            fail("IllegalArgumentException should be thrown when IBM-ImplementationClass header does not exist.");
        } catch (IllegalArgumentException iae) {
            // this is expected.
            assertTrue("The message should mention about IBM-ImplementationClass header.", iae.getMessage().contains(MSG_NO_IMPLCLASS));
        } finally {
            if (currentValue != null) {
                System.setProperty(KEY_PROP_INSTALL_DIR, currentValue);
            }
        }
    }

    /**
     * Test CustomManifest constructor with the invalid data.
     * 
     * @throws IOException
     */
    @Test
    public void testCustomManifestNoFeature() throws IOException {
        String currentValue = null;
        try {
            String dir = VALUE_PROP_INSTALL_DIR;
            currentValue = System.setProperty(KEY_PROP_INSTALL_DIR, dir);
            new CustomManifest(new File(VALUE_EXTENSION_JAR_LOCATION + JAR_NO_FEATURE));
            fail("IllegalArgumentException should be thrown when the feature manifest file is not found.");
        } catch (IllegalArgumentException iae) {
            // this is expected.
        } finally {
            if (currentValue != null) {
                System.setProperty(KEY_PROP_INSTALL_DIR, currentValue);
            }
        }
    }

    /**
     * Test findFeatureManifest method
     * 
     * @throws IOException
     */
    @Test
    public void testFindFeatureManifest() throws IOException {
        CustomManifest cm = new CustomManifest();
        // if no file is set, make sure that the output is null;
        assertNull("the output should be null", cm.findFeatureManifest());
    }

    /**
     * Test containsToolsJar method
     * 
     * @throws IOException
     */
    @Test
    public void testGetFileLocationsFromSubsystemContent() throws IOException {
        CustomManifest cm = new CustomManifest();
        final String VALUE1 = "com.ibm.ws.crypto.util.custom; version=\"[1,1.0.100)\"; start-phase:=\"SERVICE_EARLY\", valid.jar; location:=\"bin/tools/extensions/ws-customPasswordEncryption/valid.jar\"; type=file";
        final String VALUE1_LOCATION = "bin/tools/extensions/ws-customPasswordEncryption/valid.jar";
        final String VALUE2 = "different.jar; type=file; location:=\"bin/tools/extensions/ws-customPasswordEncryption/different.jar\", com.ibm.ws.crypto.util.custom; version=\"[1,1.0.100)\"; start-phase:=\"SERVICE_EARLY\"";
        final String VALUE2_LOCATION = "bin/tools/extensions/ws-customPasswordEncryption/different.jar";
        final String VALUE3 = "different.jar; type=jar location:=\"bin/tools/extensions/ws-customPasswordEncryption/different.jar\"";
        final String VALUE4 = VALUE1 + "," + VALUE2;

        assertTrue("the output should be empty if no value is set.", cm.getFileLocationsFromSubsystemContent(null).isEmpty());
        assertEquals("the output should contain one item", 1, cm.getFileLocationsFromSubsystemContent(VALUE1).size());
        assertEquals("the output should contain the expected result", VALUE1_LOCATION, cm.getFileLocationsFromSubsystemContent(VALUE1).get(0));
        assertEquals("the output should contain one item", 1, cm.getFileLocationsFromSubsystemContent(VALUE2).size());
        assertEquals("the output should contain the expected result", VALUE2_LOCATION, cm.getFileLocationsFromSubsystemContent(VALUE2).get(0));
        assertTrue("the output should be empty because type is not file.", cm.getFileLocationsFromSubsystemContent(VALUE3).isEmpty());
        assertEquals("the output should contain two items", 2, cm.getFileLocationsFromSubsystemContent(VALUE4).size());
    }

    /**
     * Test getAttributes method
     * 
     * @throws IOException
     */
    @Test
    public void testGetAttributes() throws IOException {
        CustomManifest cm = new CustomManifest();
        try {
            cm.getAttributes(new File(NO_MANIFEST_MF));
            fail("IOException should be thorwn.");
        } catch (IOException ioe) {
            //success
        }
    }

}
