/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.fat_bvt.test;

import com.ibm.ws.artifact.fat_bvt.test.utils.FATBundleDef;
import com.ibm.ws.artifact.fat_bvt.test.utils.FATFeatureDef;
import com.ibm.ws.artifact.fat_bvt.test.utils.FATWebArchiveDef;

/**
 * Artifact BVT server definition.
 */
public interface FATArtifactBVTServer {
    String SERVER_NAME = "com.ibm.ws.artifact.fat_bvt";
    String CONTEXT_ROOT = "/artifactapi";

    //

    String WAR_NAME = "artifactapi.war";
    String WAR_PACKAGE_NAME = "com.ibm.ws.artifact.fat_bvt.servlet";

    String ROOT_RESOURCES_PATH = "test-applications/" + WAR_NAME + "/resources";

    FATWebArchiveDef WAR_DEF =
        new FATWebArchiveDef(
            WAR_NAME,
            new String[] { WAR_PACKAGE_NAME },
            ROOT_RESOURCES_PATH,
            new String[] { },
            new String[] { "web.xml" }
    );

    //

    String SOURCE_BUNDLE_MANIFEST_PATH = "features/artifactinternals-1.0.mf";
    String SOURCE_BUNDLE_JAR_PATH = "bundles/artifactinternals.jar";

    public static final FATFeatureDef FEATURE_DEF =
        new FATFeatureDef(
            SOURCE_BUNDLE_MANIFEST_PATH, SOURCE_BUNDLE_JAR_PATH,
            FATFeatureDef.IS_SERVER_FEATURE);

    // public static final FATBundleDef BUNDLE_DEF =
    //     new FATBundleDef(SOURCE_BUNDLE_JAR_PATH);

    //

    String STANDARD_RESPONSE = "This is WOPR. Welcome Dr Falken.";
}

