/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

package com.ibm.ws.feature.tests.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FeatureReader {
    /**
     * Scan a file set for feature files. Locate all simple files which
     * have the extension ".feature". Read those as {@link FeatureInfo}.
     *
     * @param roots Candidate feature files and directories.
     *
     * @return Feature information read from the feature files.
     *
     * @throws IOException Thrown if any feature file cannot be read.
     */
    public static List<FeatureInfo> readFeatures(Collection<File> featureFiles) throws IOException {
        List<FeatureInfo> features = new ArrayList<>(featureFiles.size());
        for (File featureFile : featureFiles) {
            FeatureInfo feature = new FeatureInfo(featureFile);
            features.add(feature);
        }
        return features;
    }

    /**
     * Select feature files from a file set. Feature files are simple
     * files which have the extension ".feature".
     *
     * Scan directories recursively.
     *
     * @param roots Candidate feature files and directories.
     *
     * @return Feature files located as or as children of the roots.
     */
    public static List<File> selectFeatureFiles(String... roots) {
        List<File> remaining = new ArrayList<>(roots.length);
        for (String root : roots) {
            remaining.add(new File(root));
        }
        return selectFeatureFiles(remaining);
    }

    /**
     * Select feature files from a file set. Feature files are simple
     * files which have the extension ".feature".
     *
     * Scan directories recursively.
     *
     * @param roots Candidate feature files and directories.
     *
     * @return Feature files located as or as children of the roots.
     */
    public static List<File> selectFeatureFiles(File... roots) {
        List<File> remaining = new ArrayList<>(roots.length);
        for (File root : roots) {
            remaining.add(root);
        }
        return selectFeatureFiles(remaining);
    }

    /**
     * Select feature files from a file set. Feature files are simple
     * files which have the extension ".feature".
     *
     * Scan directories recursively.
     *
     * The parameter files list is modified by processing. At
     * the conclusion of processing, the parameter files list will
     * be empty.
     *
     * @param remaining Candidate feature files and directories.
     *
     * @return Feature files located as or as children of the roots.
     */
    protected static List<File> selectFeatureFiles(List<File> remaining) {
        List<File> featureFiles = new ArrayList<>();

        while (!remaining.isEmpty()) {
            File next = remaining.remove(0);

            if (next.isDirectory()) {
                File[] nextList = next.listFiles();
                if (nextList == null) {
                    continue;
                }
                for (File nextChild : nextList) {
                    remaining.add(nextChild);
                }
            } else if (next.getName().endsWith(".feature")) {
                featureFiles.add(next);
            } else {
                // Ignore it.
            }
        }

        return featureFiles;
    }
}
