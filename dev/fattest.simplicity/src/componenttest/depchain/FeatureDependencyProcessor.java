/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.depchain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.depchain.Feature.Type;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;

public class FeatureDependencyProcessor {

    private static final Class<?> c = FeatureDependencyProcessor.class;
    public static final boolean DEBUG = false;

    // If dependency validation fails, one reason could be an out of date featureList.xml.
    // Allow for the featureList.xml to be recomputed at maximum once per JVM lifespan
    private static boolean hasRetry = true;

    public static void validateTestedFeatures(LibertyServer server, RemoteFile serverLog) throws Exception {
        final String m = "validateTestedFeatures";
        // Load the tested feature data, if it exists
        File testedFeaturesFile = new File("fat-metadata.json");
        if (!testedFeaturesFile.exists()) {
            Exception noMetadata = new Exception("Unable to locate FAT metadata at: " + testedFeaturesFile.getAbsolutePath());
            Log.error(c, m, noMetadata);
            throw noMetadata;
        }

        // Scrape messages.log to see what features were installed
        List<String> installedFeaturesRaw = LibertyFileManager.findStringsInFile("CWWKF0012I: .*", serverLog);
        if (installedFeaturesRaw == null || installedFeaturesRaw.size() == 0)
            return;
        Set<String> installedFeatures = new HashSet<String>();
        for (String f : installedFeaturesRaw)
            for (String installedFeature : f.substring(0, f.lastIndexOf(']')).substring(f.lastIndexOf('[') + 1).split(","))
                installedFeatures.add(installedFeature.trim().toLowerCase());

        // Make sure that any features installed in the server are known to the test dependency graph
        File featureListFile = FeatureList.get(server);
        Set<String> testedFeatures = getTestedFeatures(testedFeaturesFile, featureListFile);

        if (testedFeatures.contains("ALL_FEATURES"))
            return;

        Set<String> untestedFeatures = new HashSet<String>();
        for (String installedFeature : installedFeatures) {
            if (installedFeature.startsWith("usr:") || installedFeature.contains("test"))
                continue; // Don't need to validate user/test features
            if (!testedFeatures.contains(installedFeature))
                untestedFeatures.add(installedFeature);
        }

        if (untestedFeatures.isEmpty() || testedFeatures.contains("all_features")) {
            Log.info(c, m, "Validated that all installed features were present in test dependencies JSON file.");
        } else {
            if (hasRetry) {
                prepForRetry(featureListFile);
                validateTestedFeatures(server, serverLog);
            } else {
                Exception e = new Exception("Installed feature(s) " + untestedFeatures +
                                            " were not defined in the autoFVT/fat-metadata.json file! " +
                                            "To correct this, add " + untestedFeatures + " to the 'tested.features' " +
                                            "property in the bnd.bnd or build-test.xml file for this FAT so that an accurate test depdendency " +
                                            "graph can be generated in the future.");
                Log.error(c, m, e);
                throw e;
            }
        }
    }

    private static Set<String> getTestedFeatures(File testedFeaturesFile, File featureList) throws Exception {
        final String m = "getTestedFeatures";

        JsonProvider provider = new org.glassfish.json.JsonProviderImpl();
        JsonObject fatMetadata = provider.createReader(new FileInputStream(testedFeaturesFile)).readObject();
        JsonArray testedFeaturesJson = fatMetadata.getJsonArray("feature-deps");

        Set<String> staticTestedFeatures = new HashSet<String>();
        staticTestedFeatures.add("timedexit-1.0"); // Manually add timedexit because it's included from an external location
        for (JsonValue testedFeature : testedFeaturesJson)
            staticTestedFeatures.add(((JsonString) testedFeature).getString().trim().toLowerCase());

        FeatureMap featureMap = FeatureMap.instance(featureList);

        Set<String> testedFeatures = new HashSet<String>();
        for (String staticFeature : staticTestedFeatures)
            testedFeatures.addAll(getEnabledFeatures(staticFeature, testedFeatures, featureMap));
        // Account for auto-features enabling other public features
        // Continue to iterate auto-feature resolution until no more features are enabled
        boolean featuresAdded = true;
        while (featuresAdded) {
            for (Feature f : featureMap.values()) {
                if (DEBUG && f.getFeatureType() == Type.AUTO_FEATURE)
                    Log.info(c, m, "Found auto feature: " + f);
                if (f.getFeatureType() == Type.AUTO_FEATURE && f.isProvisioned(featureMap, testedFeatures))
                    featuresAdded = testedFeatures.addAll(getEnabledFeatures(f.getSymbolicName(), testedFeatures, featureMap));
            }
            if (DEBUG)
                Log.info(c, m, "After auto-feature calculation: " + testedFeatures);
        }

        // In case there were hard-coded features that were not present in the featureList.xml
        testedFeatures.addAll(staticTestedFeatures);

        if (DEBUG)
            Log.info(c, m, "Static tested features are:   " + staticTestedFeatures);
        return testedFeatures;
    }

    private static Set<String> getEnabledFeatures(String feature, Set<String> enabledFeatures, FeatureMap featureMap) {
        Feature f = featureMap.get(feature);

        // Sometimes features in OpenLiberty tolerate features that exist in WAS Liberty
        // so feature definitions may not be found for these features
        if (f == null)
            return Collections.emptySet();

        // If we have already processed this feature, don't process it again
        if (enabledFeatures.contains(feature.toLowerCase()))
            return enabledFeatures;

        if (DEBUG)
            Log.info(c, "getEnabledFeatures", "For feature=" + feature + " got obj: " + f);
        enabledFeatures.add(f.getSymbolicName().toLowerCase());
        if (f.getShortName() != null)
            enabledFeatures.add(f.getShortName().toLowerCase());
        for (String enabledFeature : f.getEnables())
            enabledFeatures.addAll(getEnabledFeatures(enabledFeature, enabledFeatures, featureMap));
        for (String includedFeature : f.getInclude())
            enabledFeatures.addAll(getEnabledFeatures(includedFeature, enabledFeatures, featureMap));
        return enabledFeatures;
    }

    private static void prepForRetry(File featureList) throws IOException {
        final String m = "prepForRetry";
        Log.info(c, m, "WARNING: Tested feature validation failed.  Prepping for retry.");

        hasRetry = false;
        FeatureMap.reset();
        FeatureList.reset();
    }

}
