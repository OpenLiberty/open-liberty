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
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.ibm.websphere.simplicity.log.Log;

public class FeatureData {
    protected static final Class<?> CLASS = FeatureData.class;

    @SuppressWarnings("unused")
    private static void logInfo(String m, String msg) {
        Log.info(CLASS, m, msg);
    }

    private static void logError(String m, String msg) {
        Log.error(CLASS, m, null, msg);
    }

    private static void logError(String m, String msg, Throwable th) {
        Log.error(CLASS, m, th, msg);
    }

    /**
     * Read feature manifest files from a server features directory.
     * All files with the extension ".mf" are read.
     *
     * A failure to list files will result in a logged error, and
     * an empty collection will be returned.
     *
     * A failure to read a feature manifest will result in a logged
     * error and that feature manifest will be ignored.
     *
     * @param featuresDir A directory containing feature manifest files.
     *
     * @return The table of features which were read, keyed by feature
     *         short name.
     */
    public static Map<String, FeatureData> readFeatures(File featuresDir) {
        String m = "readFeatures";

        Map<String, FeatureData> features = new HashMap<>();

        String featuresPath = featuresDir.getAbsolutePath();

        if (!featuresDir.exists()) {
            logError(m, "Folder [ " + featuresPath + " ] does not exist");
            return features;
        }

        File[] featureFiles = featuresDir.listFiles((dir, name) -> name.endsWith(".mf"));
        if (featureFiles == null) {
            logError(m, "Folder [ " + featuresPath + " ] could not be read");
            return features;
        } else if (featureFiles.length == 0) {
            logError(m, "Folder [ " + featuresPath + " ] could not be read");
            return features;
        }

        for (File featureFile : featureFiles) {
            FeatureData featureData = parseFeature(featureFile);
            if (featureData != null) {
                features.put(featureData.getName(), featureData);
            }
        }

        return features;
    }

    private static final int SYM_PREFIX_LEN = "Subsystem-SymbolicName:".length();
    private static final int SHORT_PREFIX_LEN = "IBM-ShortName:".length();

    /**
     * Parse a single feature manifest file.
     *
     * Answer null if the feature manifest cannot be read, or if the
     * feature data is not valid.
     *
     * The feature file must contain a short name and a symbolic name.
     *
     * @param file A feature manifest which is to be read.
     *
     * @return Feature data read from the feature manifest. Null in case
     *         the feature data could not be read, or is not valid.
     */
    public static FeatureData parseFeature(File file) {
        String m = "parseFeature";

        try (Scanner scanner = new Scanner(file)) {
            String shortName = null;
            String symbolicName = null;
            boolean isClientOnly = false;
            boolean isTest = false;
            boolean isPublic = false;

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("IBM-ShortName:")) {
                    shortName = line.substring(SHORT_PREFIX_LEN).trim();
                    if (shortName.toLowerCase().contains("client")) {
                        isClientOnly = true;
                    }

                } else if (line.startsWith("IBM-Test-Feature:")) {
                    if (line.toLowerCase().contains("true")) {
                        isTest = true;
                    }

                } else if (line.startsWith("Subsystem-SymbolicName:")) {
                    // Subsystem-SymbolicName:
                    // io.openliberty.data-1.0;
                    // visibility:=public;
                    // singleton:=true

                    int semiOffset = line.indexOf(';', SYM_PREFIX_LEN);
                    symbolicName = ((semiOffset == -1) ? line.substring(SYM_PREFIX_LEN) : line.substring(SYM_PREFIX_LEN, semiOffset));
                    symbolicName = symbolicName.trim();

                    if (line.contains("visibility:=public")) {
                        isPublic = true;
                    }
                }
            }

            if (shortName == null) {
                // logError(m, "Failed to read [ " + file.getAbsolutePath() + " ]: No short name");
                return null; // Not an error: The feature is internal.
            } else if (symbolicName == null) {
                logError(m, "Failed to read [ " + file.getAbsolutePath() + " ]: No symbolic name");
                return null;
            }

            return new FeatureData(shortName, symbolicName, isClientOnly, isTest, isPublic);

        } catch (FileNotFoundException e) {
            logError(m, "Failed to read feature [ " + file.getAbsolutePath() + " ]", e);
            return null;
        }
    }

    /**
     * A subset of feature data, as parsed from a feature manifest file.
     *
     * Name and symbolic name are required.
     *
     * A feature is public only if it explicitly marked as public.
     */
    public FeatureData(String name, String symbolicName,
                       boolean isClientOnly, boolean isTest, boolean isPublic) {

        this.name = name; // "IBM-ShortName:"
        this.symbolicName = symbolicName; // "Subsystem-SymbolicName:"

        this.isClientOnly = isClientOnly; // "client", packed with the short name
        this.isTest = isTest; // "IBM-Test-Feature: true"
        this.isPublic = isPublic; // "visibility:=public", packed with the symbolic name
    }

    public final String name;

    public String getName() {
        return name;
    }

    public final String symbolicName;

    public String getSymbolicName() {
        return symbolicName;
    }

    public final boolean isClientOnly;

    public boolean isClientOnly() {
        return isClientOnly;
    }

    public final boolean isTest;

    public boolean isTest() {
        return isTest;
    }

    public final boolean isPublic;

    public boolean isPublic() {
        return isPublic;
    }
}