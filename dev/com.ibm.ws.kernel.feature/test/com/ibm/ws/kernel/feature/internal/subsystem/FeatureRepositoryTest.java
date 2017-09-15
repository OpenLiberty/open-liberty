/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.subsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;
import test.utils.TestUtils;

import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ImmutableAttributes;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ProvisioningDetails;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
public class FeatureRepositoryTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=audit=enabled:featureManager=all=enabled");
    static WsLocationAdmin locSvc;

    static final String serverName = "FeatureRepositoryTest";

    static Field isDirty;

    enum CacheField {
        publicFeatureNameToSymbolicName(false),
        cachedFeatures(false),
        autoFeatures(true),
        knownFeatures(true),
        knownBadFeatures(true);

        final Field field;
        final boolean provisioningOnly;

        CacheField(boolean provisioningOnly) {
            this.provisioningOnly = provisioningOnly;
            try {
                field = FeatureRepository.class.getDeclaredField(this.name());
                field.setAccessible(true);
            } catch (Exception e) {
                e.printStackTrace();
                throw new AssertionFailedError("Unable to initialize enum for test due to exception: " + e);
            }
        }

        Object get(FeatureRepository o) throws Exception {
            return field.get(o);
        }

        Map<?, ?> getMap(FeatureRepository o) throws Exception {
            return (Map<?, ?>) field.get(o);
        }

        List<?> getList(FeatureRepository o) throws Exception {
            return (List<?>) field.get(o);
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        File root = SharedConstants.TEST_DATA_FILE.getCanonicalFile();
        File lib = new File(root, "lib");

        TestUtils.setUtilsInstallDir(root);
        TestUtils.setKernelUtilsBootstrapLibDir(lib);
        TestUtils.clearBundleRepositoryRegistry();

        locSvc = (WsLocationAdmin) SharedLocationManager.createLocations(SharedConstants.TEST_DATA_DIR, serverName);
        TestUtils.recursiveClean(locSvc.getServerResource(null));

        BundleRepositoryRegistry.initializeDefaults(serverName, true);

        isDirty = FeatureRepository.class.getDeclaredField("isDirty");
        isDirty.setAccessible(true);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        TestUtils.setUtilsInstallDir(null);
        TestUtils.setKernelUtilsBootstrapLibDir(null);
        TestUtils.clearBundleRepositoryRegistry();
    }

    @After
    public void tearDown() {
        TestUtils.recursiveClean(locSvc.getServerResource(null));
    }

    @Rule
    public TestRule outputRule = outputMgr;

    @Test
    public void testReadWriteCache() throws Exception {
        WsResource cacheRes = locSvc.getServerOutputResource("testCache");
        System.out.println(BundleRepositoryRegistry.getRepositoryHolder(ExtensionConstants.CORE_EXTENSION).getInstallDir());

        Assert.assertFalse("The cache file should not exist: " + cacheRes, cacheRes.exists());
        Assert.assertFalse("BundleRepositoryRegistry should have been initialized", BundleRepositoryRegistry.isEmpty());

        // Create feature repository with cache file. All provisioning caches should be unallocated
        FeatureRepository repo = new FeatureRepository(cacheRes, null);
        assertNullProvisioningCaches(repo);

        // Read the empty cache _AND_ the manifests based on the locations
        // known to the BundleRepositoryImpl... 
        repo.init();
        assertInitializedProvisioningCaches(repo);

        int numCachedFeatures = CacheField.cachedFeatures.getMap(repo).size();
        int numKnownFeatures = CacheField.knownFeatures.getMap(repo).size();

        Assert.assertTrue("There should be cached features: " + numCachedFeatures, numCachedFeatures > 0);
        Assert.assertEquals("There should be the same number of known features", numCachedFeatures, numKnownFeatures);
        Assert.assertEquals("There should be one known bad manifest", 1, CacheField.knownBadFeatures.getMap(repo).size());
        Assert.assertEquals("There should be one known autofeature", 1, CacheField.autoFeatures.getList(repo).size());
        Assert.assertTrue("isDirty should be true", (Boolean) isDirty.get(repo));

        // Write to file... 
        repo.storeCache();

        Assert.assertFalse("isDirty should be false after the cache is written", (Boolean) isDirty.get(repo));
        Assert.assertTrue("The cache file should exist: " + cacheRes, cacheRes.exists());

        repo.dispose();
        assertNullProvisioningCaches(repo);

        repo.init();
        assertInitializedProvisioningCaches(repo);

        Assert.assertEquals("There should be the same number of cached features as previously", numCachedFeatures, CacheField.cachedFeatures.getMap(repo).size());
        Assert.assertEquals("There should be the same number of known features", numCachedFeatures, CacheField.knownFeatures.getMap(repo).size());
        Assert.assertEquals("There should be one known bad manifest", 1, CacheField.knownBadFeatures.getMap(repo).size());
        Assert.assertEquals("There should be one known autofeature", 1, CacheField.autoFeatures.getList(repo).size());
        Assert.assertFalse("isDirty should be false after the cache is re-read", (Boolean) isDirty.get(repo));

        // now try a fresh repo as if we are in firstInit startup
        repo = new FeatureRepository(cacheRes, null);

        repo.init();
        assertInitializedProvisioningCaches(repo);

        Assert.assertEquals("There should be the same number of cached features as previously", numCachedFeatures, CacheField.cachedFeatures.getMap(repo).size());
        Assert.assertEquals("There should be the same number of known features", numCachedFeatures, CacheField.knownFeatures.getMap(repo).size());
        Assert.assertEquals("There should be one known bad manifest", 1, CacheField.knownBadFeatures.getMap(repo).size());
        Assert.assertEquals("There should be one known autofeature", 1, CacheField.autoFeatures.getList(repo).size());
        Assert.assertFalse("isDirty should be false after the cache is re-read", (Boolean) isDirty.get(repo));

        //Assert.fail("THE END... ");
    }

    @Test
    public void testLowerFeature() {
        String lf = FeatureRepository.lowerFeature(null);
        assertNull("null was not returned", lf);
        lf = FeatureRepository.lowerFeature("A");
        assertTrue("A was not converted to lowercase: " + lf, lf.equals("a"));
        lf = FeatureRepository.lowerFeature("a");
        assertTrue("a was not returned: " + lf, lf.equals("a"));
        lf = FeatureRepository.lowerFeature(":");
        assertTrue(": was not returned: " + lf, lf.equals(":"));
        lf = FeatureRepository.lowerFeature(":a");
        assertTrue(":a was not returned: " + lf, lf.equals(":a"));
        lf = FeatureRepository.lowerFeature(":A");
        assertTrue(":a was not returned: " + lf, lf.equals(":a"));
        lf = FeatureRepository.lowerFeature("P:");
        assertTrue("P: was not returned: " + lf, lf.equals("P:"));
        lf = FeatureRepository.lowerFeature("p:");
        assertTrue("p: was not returned: " + lf, lf.equals("p:"));
        lf = FeatureRepository.lowerFeature("P:X");
        assertTrue("P:x was not returned: " + lf, lf.equals("P:x"));
        lf = FeatureRepository.lowerFeature("p:X");
        assertTrue("p:x was not returned: " + lf, lf.equals("p:x"));
        lf = FeatureRepository.lowerFeature("p:x");
        assertTrue("p:x was not returned: " + lf, lf.equals("p:x"));
        lf = FeatureRepository.lowerFeature("P:x");
        assertTrue("P:x was not returned: " + lf, lf.equals("P:x"));
        lf = FeatureRepository.lowerFeature(" P : X ");
        assertTrue("P:x was not returned: " + lf, lf.equals("P:x"));
    }

    /**
     * This method tests that every public feature has a corresponding OSGi Service registered for it.
     * 
     * @throws Exception
     */
    @Test
    public void testLibertyFeatureOSGiServiceRegistration() throws Exception {

        MockBundleContext mockBundleContext = new MockBundleContext();
        WsResource cacheRes = locSvc.getServerOutputResource("testCache");

        // Create feature repository with cache file. All provisioning caches should be unallocated
        FeatureRepository repo = new FeatureRepository(cacheRes, mockBundleContext);
        repo.init();

        // Reflectively make the updateMaps private method usable.
        Method updateMapsMethod = repo.getClass().getDeclaredMethod("updateMaps", SubsystemFeatureDefinitionImpl.class);
        updateMapsMethod.setAccessible(true);

        // Create all the feature definitions and store them in a set.
        Set<SubsystemFeatureDefinitionImpl> allFeatureDefs = new HashSet<SubsystemFeatureDefinitionImpl>();

        allFeatureDefs.add(createSubsystemFeatureDef("publicFeatureA-1.0", "com.ibm.ws.public.featureA-1.0", "public", "bundleA"));
        allFeatureDefs.add(createSubsystemFeatureDef("publicFeatureB-1.0", "com.ibm.ws.public.featureB-1.0", "public", "bundleB"));
        allFeatureDefs.add(createSubsystemFeatureDef("com.ibm.ws.public.featureC-1.0", "com.ibm.ws.public.featureC-1.0", "public", "bundleC"));
        allFeatureDefs.add(createSubsystemFeatureDef("com.ibm.ws.private.featureD-1.0", "com.ibm.ws.private.featureD-1.0", "private", "bundleD"));
        allFeatureDefs.add(createSubsystemFeatureDef("publicFeature-e-1.0", "com.ibm.ws.public.featuree-1.0", "public", "bundleE"));
        allFeatureDefs.add(createSubsystemFeatureDef("publicFeature-f-1.0", "com.ibm.ws.public.featuref-1.0", "public", "bundleF"));
        allFeatureDefs.add(createSubsystemFeatureDef("com.ibm.ws.public.featureg-1.0", "com.ibm.ws.public.featureg-1.0", "private", "bundleD"));

        // Now iterate through them and run each of them through updateMaps to get them registered in the repo. We also store the public ones
        // in a set to use in the checking at the end of the method. We also add all of the featureNames to the installedFeatures variable that 
        // we will push into the repo as well.

        Set<String> publicFeatureNames = new HashSet<String>();
        Set<String> installedFeatures = new HashSet<String>();
        for (SubsystemFeatureDefinitionImpl featureDef : allFeatureDefs) {
            // Add the featureName to the installedFeatures.
            installedFeatures.add(featureDef.getFeatureName());
            // If it is public add it to the list of public feature names
            if (featureDef.getVisibility().equals(Visibility.PUBLIC))
                publicFeatureNames.add(featureDef.getFeatureName());
            // Run the feature through the updateMaps method
            updateMapsMethod.invoke(repo, featureDef);
        }

        // Store the installedFeatures
        repo.setInstalledFeatures(installedFeatures, installedFeatures, false);

        // Update the services, which will register the OSGi Services.
        repo.updateServices();

        // Get the registered Services from the mocked Bundle Context.
        Set<String> registeredServices = mockBundleContext.getRegisteredServices();

        // Ensure we have the right number of registered service, and that the correct services have been registered
        assertEquals("Number of registered services doesn't match the number of public features",
                     publicFeatureNames.size(), registeredServices.size());
        for (String publicFeatureName : publicFeatureNames) {
            assertTrue("Public feature " + publicFeatureName + " is not listed among the registered services",
                       registeredServices.contains(publicFeatureName));
        }
    }

    private void assertNullProvisioningCaches(FeatureRepository repo) throws Exception {
        for (CacheField mapField : CacheField.values()) {
            if (mapField.provisioningOnly)
                Assert.assertNull(mapField.name() + " should be null before call to init", mapField.get(repo));
        }
    }

    private void assertInitializedProvisioningCaches(FeatureRepository repo) throws Exception {
        for (CacheField mapField : CacheField.values()) {
            if (mapField.provisioningOnly)
                Assert.assertNotNull(mapField.name() + " should be initialized after call to init", mapField.get(repo));
        }
    }

    /*
     * A method to generate a SubsystemFeatureDefinition object from a subset of feature info.
     */
    public static SubsystemFeatureDefinitionImpl createSubsystemFeatureDef(String shortName, String symbolicName, String visibility, String subsystemContentString) throws Exception {
        ProvisioningDetails details = new ProvisioningDetails(null, TestUtils.createValidFeatureManifestStream(shortName, symbolicName + "; visibility:=" + visibility,
                                                                                                               subsystemContentString));
        ImmutableAttributes iAttr = FeatureDefinitionUtils.loadAttributes("", null, details);

        return new SubsystemFeatureDefinitionImpl(iAttr, details);
    }
}
