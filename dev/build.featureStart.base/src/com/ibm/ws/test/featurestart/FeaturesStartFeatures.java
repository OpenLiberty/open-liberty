/*******************************************************************************
 * Copyright (c) 2023,2026 IBM Corporation and others.
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
package com.ibm.ws.test.featurestart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.ibm.ws.test.featurestart.features.FeatureData;
import com.ibm.ws.test.featurestart.features.FeatureErrors;
import com.ibm.ws.test.featurestart.features.FeatureFilter;
import com.ibm.ws.test.featurestart.features.FeatureLevels;
import com.ibm.ws.test.featurestart.features.FeatureReports;
import com.ibm.ws.test.featurestart.features.FeatureStability;

import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.custom.junit.runner.Mode.TestMode;

public class FeaturesStartFeatures {
    // Logging ...
    
    public static void logInfo(String m, String msg) {
        FeaturesStartTestBase.logInfo(m, msg);
    }

    // Display utility ...
    
    protected static void display(
        String m,
        String head, int length,
        Collection<String> featureShortNames,
        StringBuilder builder) {
                
        FeaturesStartReporting.display(m, head, length, featureShortNames, builder);
    }

    public static void display(String m,
            String head, String middle, String tail,
            List<String> keys, Map<String, String> values,
            StringBuilder builder) {

        FeaturesStartReporting.display(m, head, middle, tail, keys, values, builder);
    }
    
    protected static void display(String m,
        String prefix, String nestedPrefix, int length,
        Map<String, ? extends Collection<String>> values,
        StringBuilder builder) {
            
        FeaturesStartReporting.display(m, prefix, nestedPrefix, length, values, builder);
    }
    
    // Feature data ...
    //
    // This is a combination of static feature data and server feature data.
    //
    // Maybe, the data should be managed separately, since part is obtained
    // as static feature data, and part is obtained from the target server.
    // The data is managed together because of the strong coupling of the
    // use of the data.
    
    /**
     * Do feature related setup steps Read all feature data and select the features
     * which are to be tested for the previously specified test bucket.
     *
     * This method performs feature related setup steps. Basic parameter steps
     * and server related steps are performed by {@link #setParameters}.
     */
    public FeaturesStartFeatures(FeaturesStartServer server) throws Exception {
        String m = "setupFeatures";

        // Server specific feature data ...

        this.serverJavaLevel = server.getJavaLevel();

        this.featureData = FeatureData.readFeatures( server.getFeaturesDir() );

        logInfo(m, "Read [ " + featureData.size() + " ] features for server [ " + server.serverGetName() + " ] at [ " + server.getInstallRoot() + " ]");
        logInfo(m, "");
        
        // Static feature data ...

        this.stableFeatures = FeatureStability.readStableFeatures();

        this.requiredLevels = FeatureLevels.getRequiredLevels();

        this.featureFilter = (name) -> FeatureFilter.skipFeature(name);
        this.featureZOSFilter = (name) -> FeatureFilter.zosSkip(name, server.isZOS());

        this.allowedErrorsRegEx = FeatureErrors.getAllowedErrorsRegEx();

        //

        BiFunction<String, Boolean, String> zosFilter = (name, isZOS) -> FeatureFilter.zosSkip(name, isZOS.booleanValue());

        (new FeatureReports(featureData, stableFeatures, requiredLevels, featureFilter, zosFilter, allowedErrorsRegEx)).display();

        //

        RunnableFeatures selectedFeatures = selectRunnableFeatures( featureData.keySet() );
        
        this.runnableFeatureNames = selectedFeatures.runnable;
        this.runnableFeatures = selectedFeatures.runnableData;
        this.outOfLevelFeatureNames = selectedFeatures.outOfLevel;
    }

    //

    protected final int serverJavaLevel;

    //
    
    public final Map<String, FeatureData> featureData;

    public FeatureData getFeatureData(String name) {
        return featureData.get(name);
    }

    public final FeatureStability stableFeatures;

    public FeatureStability getStableFeatures() {
        return stableFeatures;
    }

    public boolean isStable(String name) {
        return stableFeatures.isStable(name);
    }

    public Map<String, Integer> requiredLevels;

    public Integer getRequiredLevel(String name) {
        return requiredLevels.get(name);
    }

    public String isLevelFiltered(String name) {
        return isLevelFiltered(name, serverJavaLevel);
    }

    public String isLevelFiltered(String name, int currentLevel) {
        Integer requiredLevel = getRequiredLevel(name);
        if ((requiredLevel == null) || (currentLevel >= requiredLevel.intValue())) {
            return null;
        } else {
            return "Required level [ " + requiredLevel + " ] greater than the current level [ " + currentLevel + " ]";
        }
    }

    public Map<String, String[]> allowedErrorsRegEx;

    public String[] getAllowedErrorsRegEx(String name) {
        return allowedErrorsRegEx.get(name);
    }

    public Function<String, String> featureFilter;

    public String isFiltered(String name) {
        return featureFilter.apply(name);
    }

    public Function<String, String> featureZOSFilter;

    public String isZOSFiltered(String name) {
        return featureZOSFilter.apply(name);
    }

    //

    public final List<String> runnableFeatureNames;
    public final Map<String, FeatureData> runnableFeatures;

    public final Set<String> outOfLevelFeatureNames;

    public static class RunnableFeatures {
        public final List<String> runnable;
        public final Map<String, FeatureData> runnableData;
        public final Set<String> outOfLevel;
        
        public RunnableFeatures(
            List<String> runnable,
            Map<String, FeatureData> runnableData,
            Set<String> outOfLevel) {
            this.runnable = runnable;
            this.runnableData = runnableData;
            this.outOfLevel = outOfLevel;
        }
    }

    /**
     * Select the runnable features. Populate {@link #runnableFeatureNames},
     * {@link #runnableFeatures}, and {@link #outOfLevelFeatureNames}.
     *
     * The base feature data was read from the features folder of the test server.
     *
     * Features are later partitioned across the several feature start test
     * buckets.
     * 
     * Filtering is performed as follows:
     * 
     * Do filter client, test, and non-public features.
     *
     * In LITE mode, do filter stable features.
     *
     * Do filter features according to static test data. For example, many features
     * cannot be started by themselves, and many features require configuration which
     * cannot be provided by the feature start tests.
     *
     * Do not filter out-of-level features.  An attempt is made to start these, and
     * and the test checks that the correct java level error occurs.
     *
     * @throws IOException Thrown if the feature directory could not be read,
     *     or if no features are available.
     */
    public RunnableFeatures selectRunnableFeatures(Set<String> featureNames) throws IOException {
        String m = "selectRunnableFeatures";

        String[] featureNamesArray = featureNames.toArray(new String[featureNames.size()]);
        Arrays.sort(featureNamesArray);

        // Generic filters: Client, test, and non-public features are never tested.
        // Stable features are not tested in LITE mode.

        List<String> clientFeatures = new ArrayList<>();
        List<String> testFeatures = new ArrayList<>();
        List<String> nonPublicFeatures = new ArrayList<>();
        List<String> useStableFeatures = new ArrayList<>();

        // Features may be skipped for feature specific reasons.
        // For example, some features cannot be started by themselves.

        List<String> filteredNames = new ArrayList<>();
        Map<String, String> filterReasons = new HashMap<>();
        List<String> zosFilteredNames = new ArrayList<>();
        Map<String, String> zosFilterReasons = new HashMap<>();

        // What is left are the features features.

        List<String> selectedNames = new ArrayList<>(featureNamesArray.length);
        Map<String, FeatureData> selectedFeatures = new HashMap<>(featureNamesArray.length);

        // Of the selected features, some may be out-of-level.  Those are still
        // tested, but the expected server startup result changes.

        List<String> outOfLevelNames = new ArrayList<>();
        Map<String, String> outOfLevelReasons = new HashMap<>();

        for (String featureShortName : featureNamesArray) {
            FeatureData useFeatureData = getFeatureData(featureShortName);
            
            // Filter client, test, and non-public features.
            //
            // In LITE mode, filter stable features.
            //
            // Filter specific features, according to static test data. For example,
            // many features cannot be started by themselves, and many features require
            // configuration which cannot be provided by the feature start tests.
            //
            // Do not filter out-of-level features.  An attempt is made to start these,
            // and the test checks that the correct java level error occurs.

            if (useFeatureData.isClientOnly()) {
                clientFeatures.add(featureShortName);
                continue;
            } else if (useFeatureData.isTest()) {
                testFeatures.add(featureShortName);
                continue;
            } else if (!useFeatureData.isPublic()) {
                nonPublicFeatures.add(featureShortName);
                continue;

            } else if ((TestModeFilter.FRAMEWORK_TEST_MODE == TestMode.LITE) && isStable(featureShortName)) {
                useStableFeatures.add(featureShortName);
                continue;
            }

            String filterReason = isFiltered(featureShortName);
            if (filterReason != null) {
                filteredNames.add(featureShortName);
                filterReasons.put(featureShortName, filterReason);
                continue;
            }
            String zosFilterReason = isZOSFiltered(featureShortName);
            if (zosFilterReason != null) {
                zosFilteredNames.add(featureShortName);
                zosFilterReasons.put(featureShortName, zosFilterReason);
                continue;
            }

            selectedNames.add(featureShortName);
            selectedFeatures.put(featureShortName, useFeatureData);

            String outOfLevelReason = isLevelFiltered(featureShortName);
            if (outOfLevelReason != null) {
                outOfLevelNames.add(featureShortName);
                outOfLevelReasons.put(featureShortName, outOfLevelReason);
            }
        }

        StringBuilder builder = new StringBuilder();
        
        if (!clientFeatures.isEmpty()) {
            logInfo(m, "Skip client-only features [ " + clientFeatures.size() + " ]:");
            display(m, "    ", 80, clientFeatures, builder);
        }
        if (!nonPublicFeatures.isEmpty()) {
            logInfo(m, "Skip non-public features [ " + nonPublicFeatures.size() + " ]:");
            display(m, "    ", 80, nonPublicFeatures, builder);
        }
        if (!testFeatures.isEmpty()) {
            logInfo(m, "Skip test features [ " + testFeatures.size() + " ]:");
            display(m, "    ", 80, testFeatures, builder);
        }

        if (!useStableFeatures.isEmpty()) {
            logInfo(m, "LITE mode: Skip stable features [ " + useStableFeatures.size() + " ]:");
            display(m, "    ", 80, useStableFeatures, builder);
        }

        if (!filterReasons.isEmpty()) {
            logInfo(m, "Skip filtered features [ " + filterReasons.size() + " ]:");
            display(m, "    ", ": ", "", filteredNames, filterReasons, builder);
        }
        if (!zosFilterReasons.isEmpty()) {
            logInfo(m, "Skip ZOS filtered features [ " + zosFilterReasons.size() + " ]:");
            display(m, "    ", ": ", "", zosFilteredNames, zosFilterReasons, builder);
        }

        if (selectedFeatures.isEmpty()) {
            throw new IllegalArgumentException("No testable features are present.");
        }
        logInfo(m, "In-level features [ " + selectedFeatures.size() + " ]:");
        display(m, "    ", 80, selectedFeatures.keySet(), builder);

        if (!outOfLevelReasons.isEmpty()) {
            logInfo(m, "Out-of-level features [ " + outOfLevelReasons.size() + " ]:");
            display(m, "    ", ": ", "", outOfLevelNames, outOfLevelReasons, builder);
        }

        return new RunnableFeatures(
            selectedNames,
            selectedFeatures,
            new HashSet<>(outOfLevelNames));
    }
}
