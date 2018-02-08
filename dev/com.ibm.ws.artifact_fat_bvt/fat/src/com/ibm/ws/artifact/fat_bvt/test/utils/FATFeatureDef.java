/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested 
* of its trade secrets, irrespective of what has been deposited with the 
* U.S. Copyright Office.
*/
package com.ibm.ws.artifact.fat_bvt.test.utils;

/**
 * Feature definition.  Used when preparing liberty servers.
 */
public class FATFeatureDef {
    public final String sourceFeatureManifestPath;
    public final String sourceFeatureJarPath;
    public final boolean isUserFeature;

    public static final boolean IS_USER_FEATURE = true;
    public static final boolean IS_SERVER_FEATURE = false;

    public FATFeatureDef(
        String featureManifestPath,
        String featureJarPath,
        boolean isUserFeature) {

        this.sourceFeatureManifestPath = featureManifestPath;
        this.sourceFeatureJarPath = featureJarPath;
        this.isUserFeature = isUserFeature;
    }
}
