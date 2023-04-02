/*******************************************************************************n * Copyright (c) 2022 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License 2.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0n *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.ibm.ws.install.internal.UninstallDirector;
import com.ibm.ws.install.internal.asset.UninstallAsset;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;

import test.common.SharedOutputManager;

/**
 *
 */
public class UninstallDirectorTest {
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

    @Before
    public void mockitoSetup() { 
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLocationParsing() {
        UninstallDirector ud = new UninstallDirector(null, null, null, null);
        String locations = null;
        List<String> output = ud.getFirstChildSubdirectoryFromLocations(locations);
        assertTrue(output.isEmpty());

        locations = "";
        output = ud.getFirstChildSubdirectoryFromLocations(locations);
        assertTrue(output.isEmpty());

        locations = "/";
        output = ud.getFirstChildSubdirectoryFromLocations(locations);
        assertTrue(output.size() == 1);
        assertEquals("", output.get(0));

        locations = "/home";
        output = ud.getFirstChildSubdirectoryFromLocations(locations);
        assertTrue(output.size() == 1);
        assertEquals("home", output.get(0));

        locations = "/home/test";
        output = ud.getFirstChildSubdirectoryFromLocations(locations);
        assertTrue(output.size() == 1);
        assertEquals("home", output.get(0));

        locations = "/home/test/script.sh";
        output = ud.getFirstChildSubdirectoryFromLocations(locations);
        assertTrue(output.size() == 1);
        assertEquals("home", output.get(0));

        locations = "/home/test/script.sh,";
        output = ud.getFirstChildSubdirectoryFromLocations(locations);
        assertTrue(output.size() == 1);
        assertEquals("home", output.get(0));
    }

    @Test
    public void testMultipleLocationString() {
        UninstallDirector ud = new UninstallDirector(null, null, null, null);

        String locations = "bin/tools/tools.zip,etc/files/files.zip";
        List<String> output = ud.getFirstChildSubdirectoryFromLocations(locations);
        assertTrue(output.size() == 2);
        assertEquals("bin", output.get(0));
        assertEquals("etc", output.get(1));

        locations = "/home/test/script.sh,etc/files/files.zip,bin/installUtility.sh";
        output = ud.getFirstChildSubdirectoryFromLocations(locations);
        assertTrue(output.size() == 3);
        assertEquals("home", output.get(0));
        assertEquals("etc", output.get(1));
        assertEquals("bin", output.get(2));

    }

    @Test
    public void testGetAssetLocations() {
        SubsystemContentType[] allowedTypes = new SubsystemContentType[] { SubsystemContentType.BOOT_JAR_TYPE, SubsystemContentType.BUNDLE_TYPE, SubsystemContentType.FILE_TYPE,
                                                                           SubsystemContentType.JAR_TYPE };
        UninstallDirector ud = new UninstallDirector(null, null, null, null);

        UninstallAsset uninstallAsset = mock(UninstallAsset.class);
        ProvisioningFeatureDefinition featureDef = mock(ProvisioningFeatureDefinition.class);

        Collection<FeatureResource> resources = new ArrayList<FeatureResource>();

        for (SubsystemContentType type : SubsystemContentType.values()) {
            FeatureResource resource = mock(FeatureResource.class);
            when(resource.getType()).thenReturn(type);
            when(resource.getLocation()).thenReturn(generateLocationString(type));
            resources.add(resource);
        }

        when(uninstallAsset.getProvisioningFeatureDefinition()).thenReturn(featureDef);
        when(featureDef.getConstituents(null)).thenReturn(resources);

        Set<String> assetLocations = ud.getAssetLocations(uninstallAsset);

        for (SubsystemContentType allowed : allowedTypes) {
            assertTrue(assetLocations.contains(generateLocationString(allowed)));
        }
        assertEquals(allowedTypes.length, assetLocations.size());

    }

    @Test
    public void testGetAssetLocationsSubdirectory() {
        final SubsystemContentType[] allowedTypes = new SubsystemContentType[] { SubsystemContentType.BOOT_JAR_TYPE, SubsystemContentType.BUNDLE_TYPE,
                                                                                 SubsystemContentType.FILE_TYPE,
                                                                                 SubsystemContentType.JAR_TYPE };
        UninstallDirector ud = new UninstallDirector(null, null, null, null);

        UninstallAsset uninstallAsset = mock(UninstallAsset.class);
        ProvisioningFeatureDefinition featureDef = mock(ProvisioningFeatureDefinition.class);

        Collection<FeatureResource> resources = new ArrayList<FeatureResource>();

        for (SubsystemContentType type : SubsystemContentType.values()) {
            FeatureResource resource = mock(FeatureResource.class);
            when(resource.getType()).thenReturn(type);
            when(resource.getLocation()).thenReturn(generateLocationString(type));
            resources.add(resource);
        }

        when(uninstallAsset.getProvisioningFeatureDefinition()).thenReturn(featureDef);
        when(featureDef.getConstituents(null)).thenReturn(resources);

        Set<String> assetLocations = ud.getAssetLocations(uninstallAsset);
        Set<String> allChildSubdirs = new HashSet<String>();
        for (String asset : assetLocations) {
            allChildSubdirs.addAll(ud.getFirstChildSubdirectoryFromLocations(asset));
        }

        assertEquals(allowedTypes.length + 2, allChildSubdirs.size());
        assertTrue(allChildSubdirs.contains("etc"));
        assertTrue(allChildSubdirs.contains("bin"));
        for (SubsystemContentType type : allowedTypes) {
            assertTrue(allChildSubdirs.contains(type.name()));
        }
    }

    @Test
    public void testRemovePathExceptions() {
        UninstallDirector ud = new UninstallDirector(null, null, null, null);
        String[] testPaths = new String[] { "/test/script.sh", "etc/files/files.zip", "bin/installUtility.sh", "bin/installUtility.bat", "bin", "bin/featureUtility.bat",
                                            "bin/featureManager.bat", "bin/tools/ws-featureManager.jar", "bin/tools/ws-featureUtility.jar",
                                            "bin/tools/whatever/ws-featureUtility.jar", "bin/", "bin" };
        List<Path> paths = new ArrayList<>();
        for (String test : testPaths) {
            Path path = FileSystems.getDefault().getPath(test);
            System.out.println("testPath: " + path.toString());
            paths.add(path);
        }
        paths = ud.removePathExceptions(paths);
        List<Path> expectedPaths = new ArrayList<Path>();
        expectedPaths.add(FileSystems.getDefault().getPath("/test/script.sh"));
        expectedPaths.add(FileSystems.getDefault().getPath("etc/files/files.zip"));
        expectedPaths.add(FileSystems.getDefault().getPath("bin/installUtility.sh"));
        assertEquals(expectedPaths.size(), paths.size());
        assertTrue(expectedPaths.containsAll(paths));

    }

    /**
     * @param type
     * @return
     */
    private String generateLocationString(SubsystemContentType type) {
        return "/" + type.name() + "/test/script.sh,etc/files/files.zip,bin/installUtility.sh";
    }

}
