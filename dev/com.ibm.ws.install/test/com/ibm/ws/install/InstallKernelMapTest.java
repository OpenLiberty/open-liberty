/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.install.internal.InstallKernelMap;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

import test.common.SharedOutputManager;

/**
 *
 */
public class InstallKernelMapTest {
    @Rule
    public static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        InstallKernelMap ikm = new InstallKernelMap();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testGet() {
        InstallKernelMap ikm = new InstallKernelMap();
        try {
            ikm.size();
            fail("InstallKernelMap.size() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.keySet();
            fail("InstallKernelMap.keySet() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.entrySet();
            fail("InstallKernelMap.entrySet() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.values();
            fail("InstallKernelMap.values() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.clear();
            fail("InstallKernelMap.clear() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.containsValue("");
            fail("InstallKernelMap.containsValue() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.putAll(new HashMap<String, String>());
            fail("InstallKernelMap.putAll() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        try {
            ikm.remove("");
            fail("InstallKernelMap.remove() didn't throw exception.");
        } catch (UnsupportedOperationException e) {
        }
        assertTrue("InstallKernelMap.containsKey() should return true.", ikm.containsKey("license.accept"));
        assertFalse("InstallKernelMap.containsKey() should return false.", ikm.isEmpty());
    }

    @Test
    public void testPut() {
        InstallKernelMap ikm = new InstallKernelMap();
        try {
            ikm.put("license.accept", "");
            fail("InstallKernelMap.put(license.accept) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("runtime.install.dir", "");
            fail("InstallKernelMap.put(runtime.install.dir) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("repositories.properties", "");
            fail("InstallKernelMap.put(repositories.properties) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("dowload.external.deps", "");
            fail("InstallKernelMap.put(dowload.external.deps) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("user.agent", Boolean.TRUE);
            fail("InstallKernelMap.put(user.agent) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("progress.monitor.message", "");
            fail("InstallKernelMap.put(progress.monitor.message) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("progress.monitor.cancelled", "");
            fail("InstallKernelMap.put(progress.monitor.cancelled) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("target.user.directory", "");
            fail("InstallKernelMap.put(target.user.directory) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("message.locale", "");
            fail("InstallKernelMap.put(message.locale) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("action.install", "");
            fail("InstallKernelMap.put(action.install) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("uninstall.user.features", "");
            fail("InstallKernelMap.put(uninstall.user.features) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("action.uninstall", "");
            fail("InstallKernelMap.put(action.uninstall) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("debug", "");
            fail("InstallKernelMap.put(debug) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("debug", "");
            fail("InstallKernelMap.put(debug) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("local.esa.download.dir", "");
            fail("InstallKernelMap.put(local.esa.download.dir) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("install.local.esa", "");
            fail("InstallKernelMap.put(install.local.esa) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("features.to.resolve", "");
            fail("InstallKernelMap.put(features.to.resolve) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("single.json.file", "");
            fail("InstallKernelMap.put(single.json.file) didn't throw exception.");
        } catch (IllegalArgumentException e) {
        }
        try {
            ikm.put("repositories.properties", new File("unknown"));
            fail("InstallKernelMap.put(repositories.properties) didn't throw exception.");
        } catch (RuntimeException e) {
        }

        ikm.put("action.install", new File("abc.jar"));
        assertEquals("Expected action.result is 1", 1, ikm.get("action.result"));
        assertTrue("Expected CWWKF1502E", ((String) ikm.get("action.error.message")).contains("CWWKF1502E"));

        ikm.put("action.install", new File("abc.esa"));
        assertEquals("Expected action.result is 1", 1, ikm.get("action.result"));
        assertTrue("Expected CWWKF1267E", ((String) ikm.get("action.error.message")).contains("CWWKF1267E"));
    }

    /**
     * Test that already-installed user features are filtered out when installServerFeatures is called.
     * This test verifies the fix for the issue where installServerFeatures with a custom Maven mirror
     * would fail with CWWKF1402E when trying to resolve user features that were already installed.
     */
    @Test
    public void testInstalledUserFeatureFilteredFromInstallServerFeatures() {
        InstallKernelMap ikm = new InstallKernelMap();

        // Create mock installed user feature (ibmProcessServer)
        ProvisioningFeatureDefinition mockUserFeature = mock(ProvisioningFeatureDefinition.class);
        when(mockUserFeature.getIbmShortName()).thenReturn("ibmProcessServer");
        when(mockUserFeature.getFeatureName()).thenReturn("com.ibm.bpm.ibmProcessServer");
        when(mockUserFeature.getBundleRepositoryType()).thenReturn("usr");

        // Features to install: product extension feature + Liberty features
        Collection<String> toInstall = new ArrayList<>(Arrays.asList(
            "ibmProcessServer:ibmProcessServer", // Already installed product extension feature
            "servlet-5.0",                        // Liberty feature
            "ejb-3.2"                             // Liberty feature
        ));

        // Filter out installed user features
        Collection<String> result = ikm.filterOutInstalledUserFeatures(
            toInstall, Arrays.asList(mockUserFeature));

        // Verify user feature was filtered out
        assertFalse("User feature should be filtered out", result.contains("ibmProcessServer:ibmProcessServer"));
        assertTrue("Liberty feature servlet-5.0 should remain", result.contains("servlet-5.0"));
        assertTrue("Liberty feature ejb-3.2 should remain", result.contains("ejb-3.2"));
        assertEquals("Should have 2 features remaining", 2, result.size());
    }

    /**
     * Test that non-user features (core Liberty features) are not filtered out.
     */
    @Test
    public void testNonUserFeatureNotFiltered() {
        InstallKernelMap ikm = new InstallKernelMap();

        // Create mock installed core Liberty feature (not a user feature)
        ProvisioningFeatureDefinition mockCoreFeature = mock(ProvisioningFeatureDefinition.class);
        when(mockCoreFeature.getIbmShortName()).thenReturn("servlet-5.0");
        when(mockCoreFeature.getFeatureName()).thenReturn("com.ibm.websphere.appserver.servlet-5.0");
        when(mockCoreFeature.getBundleRepositoryType()).thenReturn(null); // Core features return null

        Collection<String> toInstall = new ArrayList<>(Arrays.asList("servlet-5.0", "ejb-3.2"));
        Collection<String> result = ikm.filterOutInstalledUserFeatures(
            toInstall, Arrays.asList(mockCoreFeature));

        // Core features should not be filtered even if installed
        assertEquals("Core features should not be filtered", 2, result.size());
        assertTrue("servlet-5.0 should remain", result.contains("servlet-5.0"));
        assertTrue("ejb-3.2 should remain", result.contains("ejb-3.2"));
    }

    /**
     * Test various user feature name format variations are correctly matched.
     * User features can be specified as:
     * - usr:featureName
     * - featureName:featureName (e.g., ibmProcessServer:ibmProcessServer)
     * - featureName
     */
    @Test
    public void testUserFeatureNameFormatVariations() {
        InstallKernelMap ikm = new InstallKernelMap();

        ProvisioningFeatureDefinition mockUserFeature = mock(ProvisioningFeatureDefinition.class);
        when(mockUserFeature.getIbmShortName()).thenReturn("myUserFeature");
        when(mockUserFeature.getFeatureName()).thenReturn("com.example.myUserFeature");
        when(mockUserFeature.getBundleRepositoryType()).thenReturn("usr");

        // Test different name format variations:
        // - "extensionName:featureName" where extensionName is "usr"
        // - plain "featureName"
        Collection<String> testFormats = Arrays.asList(
            "usr:myUserFeature",
            "myUserFeature"
        );

        for (String format : testFormats) {
            Collection<String> toInstall = new ArrayList<>(Arrays.asList(format, "servlet-5.0"));
            Collection<String> result = ikm.filterOutInstalledUserFeatures(
                toInstall, Arrays.asList(mockUserFeature));

            assertFalse("Format '" + format + "' should be filtered out", result.contains(format));
            assertEquals("Should have 1 feature remaining for format: " + format, 1, result.size());
            assertTrue("servlet-5.0 should remain", result.contains("servlet-5.0"));
        }
    }

    /**
     * Test filtering of features from custom product extensions (not just "usr").
     * Custom product extensions have their own bundle repository type.
     */
    @Test
    public void testCustomProductExtensionFeature() {
        InstallKernelMap ikm = new InstallKernelMap();

        // Mock a feature from a custom product extension named "ibmProcessServer"
        ProvisioningFeatureDefinition mockCustomFeature = mock(ProvisioningFeatureDefinition.class);
        when(mockCustomFeature.getIbmShortName()).thenReturn("ibmProcessServer");
        when(mockCustomFeature.getFeatureName()).thenReturn("com.ibm.bpm.ibmProcessServer");
        when(mockCustomFeature.getBundleRepositoryType()).thenReturn("ibmProcessServer");

        // Test format: "extensionName:featureName" where both happen to be "ibmProcessServer"
        Collection<String> toInstall = new ArrayList<>(Arrays.asList(
            "ibmProcessServer:ibmProcessServer",
            "servlet-5.0",
            "ejb-3.2"
        ));

        Collection<String> result = ikm.filterOutInstalledUserFeatures(
            toInstall, Arrays.asList(mockCustomFeature));

        // The custom product extension feature should be filtered out
        assertFalse("ibmProcessServer:ibmProcessServer should be filtered out",
                    result.contains("ibmProcessServer:ibmProcessServer"));
        assertEquals("Should have 2 features remaining", 2, result.size());
        assertTrue("servlet-5.0 should remain", result.contains("servlet-5.0"));
        assertTrue("ejb-3.2 should remain", result.contains("ejb-3.2"));
    }

    /**
     * Test that when no user features are installed, all features remain in the list.
     */
    @Test
    public void testNoUserFeaturesInstalled() {
        InstallKernelMap ikm = new InstallKernelMap();

        Collection<String> toInstall = new ArrayList<>(Arrays.asList(
            "servlet-5.0", "ejb-3.2", "jaxrs-2.1"
        ));

        // No installed features
        Collection<String> result = ikm.filterOutInstalledUserFeatures(
            toInstall, new ArrayList<ProvisioningFeatureDefinition>());

        assertEquals("All features should remain when no user features installed", 3, result.size());
        assertTrue(result.contains("servlet-5.0"));
        assertTrue(result.contains("ejb-3.2"));
        assertTrue(result.contains("jaxrs-2.1"));
    }

    /**
     * Test case-insensitive matching of feature names.
     */
    @Test
    public void testCaseInsensitiveFeatureMatching() {
        InstallKernelMap ikm = new InstallKernelMap();

        ProvisioningFeatureDefinition mockUserFeature = mock(ProvisioningFeatureDefinition.class);
        when(mockUserFeature.getIbmShortName()).thenReturn("MyUserFeature");
        when(mockUserFeature.getFeatureName()).thenReturn("com.example.MyUserFeature");
        when(mockUserFeature.getBundleRepositoryType()).thenReturn("usr");

        // Test with different case
        Collection<String> toInstall = new ArrayList<>(Arrays.asList(
            "myuserfeature",  // lowercase
            "servlet-5.0"
        ));

        Collection<String> result = ikm.filterOutInstalledUserFeatures(
            toInstall, Arrays.asList(mockUserFeature));

        assertFalse("Case-insensitive match should filter out user feature", result.contains("myuserfeature"));
        assertEquals("Should have 1 feature remaining", 1, result.size());
        assertTrue("servlet-5.0 should remain", result.contains("servlet-5.0"));
    }

    /**
     * Test the exact ibmProcessServer scenario from the reported bug.
     * This reproduces the issue where Docker builds with Artifactory mirrors
     * would fail with CWWKF1402E when ibmProcessServer was already installed.
     * ibmProcessServer is a custom product extension, not the default "usr" extension.
     */
    @Test
    public void testIbmProcessServerScenario() {
        InstallKernelMap ikm = new InstallKernelMap();

        ProvisioningFeatureDefinition mockUserFeature = mock(ProvisioningFeatureDefinition.class);
        when(mockUserFeature.getIbmShortName()).thenReturn("ibmProcessServer");
        when(mockUserFeature.getFeatureName()).thenReturn("ibmProcessServer:ibmProcessServer");
        when(mockUserFeature.getBundleRepositoryType()).thenReturn("ibmProcessServer");

        Collection<String> toInstall = new ArrayList<>(Arrays.asList(
            "ibmProcessServer:ibmProcessServer", "ejbHome-3.2", "webProfile-8.0"
        ));

        Collection<String> result = ikm.filterOutInstalledUserFeatures(
            toInstall, Arrays.asList(mockUserFeature));

        assertFalse("ibmProcessServer should be filtered", result.contains("ibmProcessServer:ibmProcessServer"));
        assertEquals("Should have 2 features remaining", 2, result.size());
        assertTrue("ejbHome-3.2 should remain", result.contains("ejbHome-3.2"));
        assertTrue("webProfile-8.0 should remain", result.contains("webProfile-8.0"));
    }

}

