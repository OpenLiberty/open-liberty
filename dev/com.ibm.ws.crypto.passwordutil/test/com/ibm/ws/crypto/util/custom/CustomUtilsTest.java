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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

/**
 * Tests for the password utility class.
 */
public class CustomUtilsTest {
    private static final String KEY_WLP_PROCESS_TYPE = "wlp.process.type";
    private static final String testBuildDir = System.getProperty("test.buildDir", "generated");
    private static final String VALUE_DIR = testBuildDir + "/test/test_data";
    private static final String KEY_PROP_INSTALL_DIR = "wlp.install.dir";
    private static final String KEY_ENV_INSTALL_DIR = "WLP_INSTALL_DIR";
    private static final String VALUE_RESOURCE_DIR = VALUE_DIR + "/resource_files";
    private static final String VALUE_RESOURCE_FILE = "customResource";
    private static final String VALUE_PROP_INSTALL_DIR = testBuildDir + "/test/test_data/custom_encryption";
    private static final String VALUE_EXTENSION_JAR = testBuildDir + "/test/test_data/custom_encryption/bin/tools/extensions/ws-customPasswordEncryption/valid.jar";

    /**
     * Test isCommandLine method
     * 
     * @throws
     */
    @Test
    public void testIsCommandLine() {
        String currentValue = System.setProperty(KEY_WLP_PROCESS_TYPE, "server");
        assertFalse("if wlp.process.type property is set as server, it should return false.", CustomUtils.isCommandLine());
        System.setProperty(KEY_WLP_PROCESS_TYPE, "client");
        assertFalse("if wlp.process.type property is set as client, it should return false.", CustomUtils.isCommandLine());
        System.setProperty(KEY_WLP_PROCESS_TYPE, "something");
        assertTrue("if wlp.process.type property is set as other than client or server, it should return true.", CustomUtils.isCommandLine());
        System.clearProperty(KEY_WLP_PROCESS_TYPE);
        assertTrue("if wlp.process.type property is not set, it should return true.", CustomUtils.isCommandLine());
        if (currentValue != null) {
            System.setProperty(KEY_WLP_PROCESS_TYPE, currentValue);
        }
    }

    /**
     * Test getInstallRoot method
     * 
     * @throws
     */
    @Test
    public void testGetInstallRoot() {
        String currentValue = System.setProperty(KEY_PROP_INSTALL_DIR, VALUE_DIR);
        assertEquals("The directory should be taken from wlp.install.dir SystemProperty.", VALUE_DIR, CustomUtils.getInstallRoot());
        System.clearProperty(KEY_PROP_INSTALL_DIR);
        assertEquals("The directory should be taken from WLP_INSTALL_DIR environment variable.", System.getenv(KEY_ENV_INSTALL_DIR), CustomUtils.getInstallRoot());
        if (currentValue != null) {
            System.setProperty(KEY_PROP_INSTALL_DIR, currentValue);
        }
    }

    /**
     * Test findCustomEncryption method
     * 
     * @throws IOException
     * 
     */
    @Test
    public void testFindCustomEncryption() throws IOException {
        String dir = VALUE_PROP_INSTALL_DIR;
        String currentValue = System.setProperty(KEY_PROP_INSTALL_DIR, dir);
        List<CustomManifest> output = CustomUtils.findCustomEncryption(CustomUtils.CUSTOM_ENCRYPTION_DIR);

        assertNotNull("findCustomEncryption should return the value", output);
        assertEquals("findCustomEncryption should return two custom encryption", 2, output.size());

        output = CustomUtils.findCustomEncryption("doesNotExist");
        if (currentValue != null) {
            System.setProperty(KEY_PROP_INSTALL_DIR, currentValue);
        }
        assertTrue("findCustomEncryption should return null when there is no valid file.", output.isEmpty());
    }

    /**
     * Test getResourceBundle method
     * 
     */
    @Test
    public void testGetResourceBundle() {
        assertNull("If resource file does not exist, null should be returned.", CustomUtils.getResourceBundle(new File(VALUE_RESOURCE_DIR), "invalidName", Locale.ENGLISH));
        assertNotNull("If resource file exists, the object should be returned.", CustomUtils.getResourceBundle(new File(VALUE_RESOURCE_DIR), VALUE_RESOURCE_FILE, Locale.ENGLISH));
    }

    /**
     * Test toJSON method
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     */
    @Test
    public void testToJSON() throws IllegalArgumentException, IOException {
        final String expected = "{\"name\":\"custom\",\"featurename\":\"usr:valid-1.0\",\"description\":\"valid default resource\"}";
        String dir = VALUE_PROP_INSTALL_DIR;
        String currentValue = System.setProperty(KEY_PROP_INSTALL_DIR, dir);
        CustomManifest cm = new CustomManifest(new File(VALUE_EXTENSION_JAR));
        if (currentValue != null) {
            System.setProperty(KEY_PROP_INSTALL_DIR, currentValue);
        }
        List<CustomManifest> cms = new ArrayList<CustomManifest>();
        cms.add(cm);
        String output = CustomUtils.toJSON(cms);
        assertNotNull("toJSON should return the value", output);
        assertEquals("toJSON should return the valid value", "[" + expected + "]", output);
        // two items.
        cms.add(cm);
        output = CustomUtils.toJSON(cms);
        assertNotNull("toJSON should return the value", output);
        assertEquals("toJSON should return the valid value", "[" + expected + "," + expected + "]", output);
    }
}
