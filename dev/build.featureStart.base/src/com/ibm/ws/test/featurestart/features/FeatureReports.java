/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.test.featurestart.features;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.ibm.websphere.simplicity.log.Log;

public class FeatureReports {
    private static Class<?> c = FeatureReports.class;

    private static void logInfo(String m, String msg) {
        Log.info(c, m, msg);
    }

    // c:/dev/repos-pub/o-l/dev/build.image/wlp
    // c:/dev/repos-pri/WS-CD-Open/dev/build.image/wlp

    public static final int USAGE_RC = 1;
    public static final int READ_ERROR_RC = 2;
    public static final int OK_RC = 0;

    public static void main(String[] parms) {
        if ((parms == null) || (parms.length == 0)) {
            System.out.println("Usage: " + FeatureReports.class.getName() + " <serverHome>");
            return;
        }

        String serverHome = parms[0];

        System.out.println("Reading features for server [ " + serverHome + " ] ...");

        Map<String, FeatureData> featureData = FeatureData.readFeatures(new File(serverHome));

        FeatureStability stableFeatures;
        try {
            stableFeatures = FeatureStability.readStableFeatures();
        } catch (IOException e) {
            System.err.println("Error: Failed to read stable features [ " + FeatureStability.STABLE_FEATURES_NAME + " ]");
            e.printStackTrace(System.err);
            return;
        }

        Map<String, Integer> requiredLevels;
        try {
            requiredLevels = FeatureLevels.getRequiredLevels();
        } catch (IOException e) {
            System.err.println("Error: Failed to read required levels [ " + FeatureLevels.REQUIRED_LEVELS_NAME + " ]");
            e.printStackTrace(System.err);
            return;
        }

        Function<String, String> featureFilter = (name) -> FeatureFilter.skipFeature(name);
        BiFunction<String, Boolean, String> featureZOSFilter = (name, isZOS) -> FeatureFilter.zosSkip(name, isZOS.booleanValue());

        Map<String, String[]> allowedErrors = FeatureErrors.getAllowedErrors();

        System.out.println("Read features for server [ " + serverHome + " ] ... [ " + featureData.size() + " ] features read.");
        System.out.println();

        (new FeatureReports(featureData, stableFeatures, requiredLevels, featureFilter, featureZOSFilter, allowedErrors)).display();

        return;
    }

    public FeatureReports(Map<String, FeatureData> featureData,
                          FeatureStability stableFeatures,
                          Map<String, Integer> requiredLevels,
                          Function<String, String> featureFilter,
                          BiFunction<String, Boolean, String> featureZOSFilter,
                          Map<String, String[]> allowedErrors) {

        this.featureData = featureData;
        this.stableFeatures = stableFeatures;
        this.requiredLevels = requiredLevels;
        this.featureFilter = featureFilter;
        this.featureZOSFilter = featureZOSFilter;
        this.allowedErrors = allowedErrors;
    }

    protected Map<String, FeatureData> featureData;

    protected FeatureStability stableFeatures;
    protected Map<String, Integer> requiredLevels;
    protected Function<String, String> featureFilter;
    protected BiFunction<String, Boolean, String> featureZOSFilter;

    public boolean isStable(String name) {
        return stableFeatures.isStable(name);
    }

    public Integer getRequiredLevel(String name) {
        return requiredLevels.get(name);
    }

    public String isFiltered(String name) {
        return featureFilter.apply(name);
    }

    public String isZOSFiltered(String name, boolean isZOS) {
        return featureZOSFilter.apply(name, Boolean.valueOf(isZOS));
    }

    protected Map<String, String[]> allowedErrors;

    public String[] getAllowedErrors(String name) {
        return allowedErrors.get(name);
    }

    //

    public static final String BIG_BANNER = "========================================" +
                                            "========================================";

    public static final String SMALL_BANNER = "----------------------------------------" +
                                              "--------------------";

    public void display() {
        String m = "display";

        logInfo(m, "Features");
        logInfo(m, BIG_BANNER);

        String[] featureNames = featureData.keySet().toArray(new String[featureData.size()]);
        Arrays.sort(featureNames);

        for (int featureNo = 0; featureNo < featureNames.length; featureNo++) {
            if (featureNo != 0) {
                logInfo(m, SMALL_BANNER);
            }
            display(featureData.get(featureNames[featureNo]));
        }

        logInfo(m, BIG_BANNER);
    }

    protected char flag(boolean value) {
        return (value ? ' ' : '!');
    }

    protected String flagLevel(Integer requiredLevel) {
        return ((requiredLevel == null) ? "DEF" : requiredLevel.toString());
    }

    public void display(FeatureData featureData) {
        String m = "display";

        String name = featureData.getName();

        String featureLine1 = String.format("  %-20s : %s", name, featureData.symbolicName);

        String featureLine2 = "  " +
                              "  " + (flag(featureData.isClientOnly) + "isClient") +
                              " " + (flag(featureData.isPublic) + "isPublic") +
                              " " + (flag(featureData.isTest) + "isTest") +
                              " " + (flag(isStable(name)) + "isStable") +
                              " " + ("java " + flagLevel(getRequiredLevel(name)));

        String filterReason = isFiltered(name);
        String nonzosFilterReason = isZOSFiltered(name, false);
        String zosFilterReason = isZOSFiltered(name, true);

        String[] ignoredErrors = getAllowedErrors(name);

        logInfo(m, featureLine1);
        logInfo(m, featureLine2);

        if (filterReason != null) {
            String featureLine = "  Filtered: " + filterReason;
            logInfo(m, featureLine);
        }

        if (nonzosFilterReason != null) {
            String featureLine = "  Non-ZOS filtered: " + nonzosFilterReason;
            logInfo(m, featureLine);
        }

        if (zosFilterReason != null) {
            String featureLine = "  ZOS filtered: " + zosFilterReason;
            logInfo(m, featureLine);
        }

        if (ignoredErrors != null) {
            String errorsLine = "  Allowed errors: " + Arrays.toString(ignoredErrors);
            logInfo(m, errorsLine);
        }
    }
}
