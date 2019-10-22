package com.ibm.aries.buildtasks.semantic.versioning.model;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * This test runs the same tests as FeatureInfoTest, but using the new-style manifests (which should
 * give the same answers)
 */
public class NewFeatureInfoTest extends FeatureInfoTest {

    @Override
    protected FeatureInfo createFromManifest(
                                             Map<VersionedEntity, String> devJars, String installDir,
                                             File manifest) throws IOException {
        FeatureInfo i = NewFeatureInfo.createFromManifest(manifest, installDir, null, devJars);
        return i;
    }

    @Override
    protected String getManifestFileExtension() {
        return ".feature";
    }
}
