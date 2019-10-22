package com.ibm.aries.buildtasks.semantic.versioning.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.VersionRange;

import com.ibm.ws.featureverifier.internal.GlobalConfig;

/**
 * For normal usage patterns a unit test for FeatureInfo makes limited sense, since it has to be running in
 * a built liberty server for full functionality, but the load rules use it outside a server, so this
 * makes sure that limited sets of the function work in that environment.
 * 
 * @author holly
 * 
 */
public class FeatureInfoTest {

    protected static File basePath;

    @BeforeClass
    public static void setUp() throws IOException
    {
        GlobalConfig.setIgnoreMissingResources(true);
        // Set a sensible value for the base dir since junit forks and local runs won't have it
        String buildLibertyDir = System.getProperty("build.liberty.dir");
        if (buildLibertyDir == null)
        {
            basePath = new File("..");
        } else
        {
            basePath = new File(buildLibertyDir);
        }
    }

    @Test
    public void testCreatingFromManifest() throws IOException
    {
        Map<VersionedEntity, String> devJars = new HashMap<VersionedEntity, String>();
        String installDir = null;
        File manifest = new File(basePath, "com.ibm.ws.featureverifier/test/feature/com.ibm.websphere.appserver.servlet-3.1" + getManifestFileExtension());
        assertTrue(manifest + "does not exist. Is the base dir," + basePath.getCanonicalPath() + ", correctly pointing to the root of a source extract?", manifest.exists());
        FeatureInfo i = createFromManifest(devJars, installDir, manifest);
        validateFeatureInfoHasSensibleValues(i);
    }

    @Test
    public void testCreatingFromAnotherManifest() throws IOException
    {
        Map<VersionedEntity, String> devJars = new HashMap<VersionedEntity, String>();
        String installDir = null;
        File manifest = new File(basePath, "com.ibm.ws.featureverifier/test/feature/com.ibm.websphere.appserver.wsSecurity-1.1" + getManifestFileExtension());
        assertTrue(manifest + "does not exist. Is the base dir," + basePath.getCanonicalPath() + ", correctly pointing to the root of a source extract?", manifest.exists());
        FeatureInfo i = createFromManifest(devJars, installDir, manifest);
        validateFeatureInfoHasSensibleValues(i);
    }

    @Test
    public void testCreatingFromManifestWithSpecBundles() throws IOException
    {
        Map<VersionedEntity, String> devJars = new HashMap<VersionedEntity, String>();
        String installDir = null;
        File manifest = new File(basePath, "com.ibm.ws.featureverifier/test/feature/com.ibm.websphere.appserver.websocket-1.1" + getManifestFileExtension());
        assertTrue(manifest + "does not exist. Is the base dir," + basePath.getCanonicalPath() + ", correctly pointing to the root of a source extract?", manifest.exists());
        FeatureInfo i = createFromManifest(devJars, installDir, manifest);
        validateFeatureInfoHasSensibleValues(i);
        Map<String, Set<VersionRange>> bundles = i.getContentBundles();
        assertTrue("The bundle list is missing the one we want: " + Arrays.toString(bundles.keySet().toArray()), bundles.containsKey("com.ibm.websphere.javaee.websocket.1.1"));
    }

    /**
     * An entry with type="jar" gets its own line in a new feature manifest, but should be returned as
     * bundle content by both types of FeatureInfo.
     */
    @Test
    public void testCreatingFromManifestWithJarEntry() throws IOException
    {
        Map<VersionedEntity, String> devJars = new HashMap<VersionedEntity, String>();
        String installDir = null;
        File manifest = new File(basePath, "com.ibm.ws.featureverifier/test/feature/com.ibm.websphere.appserver.jaxb-2.2" + getManifestFileExtension());
        assertTrue(manifest + "does not exist. Is the base dir," + basePath.getCanonicalPath() + ", correctly pointing to the root of a source extract?", manifest.exists());
        FeatureInfo i = createFromManifest(devJars, installDir, manifest);
        validateFeatureInfoHasSensibleValues(i);
        Map<String, Set<VersionRange>> bundles = i.getContentBundles();
        assertTrue("The bundle list is missing the one we want: " + Arrays.toString(bundles.keySet().toArray()), bundles.containsKey("com.ibm.ws.jaxb.tools.2.2.6"));
    }

    /**
     * An entry with type="jar" gets its own line in a new feature manifest, but should be returned as
     * bundle content by both types of FeatureInfo.
     */
    /*
    @Test
    public void testCreatingFromManifestWithDirectivesInFeatureContent() throws IOException
    {
        Map<VersionedEntity, String> devJars = new HashMap<VersionedEntity, String>();
        String installDir = null;
        File manifest = new File(basePath, "com.ibm.ws.featureverifier/test/feature/com.ibm.websphere.appserver.jpa-2.0" + getManifestFileExtension());
        assertTrue(manifest + "does not exist. Is the base dir," + basePath.getCanonicalPath() + ", correctly pointing to the root of a source extract?", manifest.exists());
        FeatureInfo i = createFromManifest(devJars, installDir, manifest);
        validateFeatureInfoHasSensibleValues(i);
        Map<String, String> features = i.getContentFeatures();
        assertTrue("The feature list is missing the one we want: " + Arrays.toString(features.keySet().toArray()),
                   features.containsKey("com.ibm.websphere.appserver.beanValidation-1.0"));
    }*/

    /**
     * @param i
     */
    private void validateFeatureInfoHasSensibleValues(FeatureInfo i) {
        // If we got this far we're doing well, but let's make some simple assertions that it found dependencies
        assertTrue(i.getContentBundles().size() > 0);
        assertTrue(i.getContentFeatures().size() > 0);
        assertNotNull(i.getName());
        assertNotNull(i.getShortName());
        assertNotNull(i.getVersion());
    }

    protected FeatureInfo createFromManifest(
                                             Map<VersionedEntity, String> devJars, String installDir,
                                             File manifest) throws IOException {
        FeatureInfo i = FeatureInfo.createFromManifest(manifest, installDir, null, devJars);
        return i;
    }

    protected String getManifestFileExtension() {
        return ".mf";
    }
}
