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

package com.ibm.ws.feature.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FeatureRepo {

    /**
     * Create and populate a feature repository with features
     * read from a root directory.
     *
     * Thrown an exception if the read fails, or if any duplicate features
     * are detected.
     *
     * @param root The root folder containing feature files.
     *
     * @return The populated feature repository.
     *
     * @throws IOException Thrown if the read fails, or if there are
     *             duplicate features in different files.
     */
    public static FeatureRepo readFeatures(File root) throws IOException {
        FeatureFiles featureFiles = new FeatureFiles(root);

        Map<String, FeatureInfo> features = new LinkedHashMap<>(featureFiles.getAllChildren().size());

        for (File featureFile : featureFiles.getAllChildren()) {
            FeatureInfo feature = new FeatureInfo(featureFile);
            String featureName = feature.getName();

            // TODO: Return a list of duplicating features: We want to
            //       capture the entire list.

            // TODO: Feature file -> feature name mismatches should perhaps
            //       be detected here.

            FeatureInfo priorFeature = features.get(featureName);
            if (priorFeature != null) {
                String message = "Duplicate feature [ " + featureName + "]" +
                                 ": Loaded previously from [ " + priorFeature.getFeatureFile().getPath() + " ]" +
                                 ": Duplicate load from [ " + featureFile.getPath() + " ]";
                throw new IOException(message);

            } else {
                features.put(feature.getName(), feature);
            }
        }

        // TODO: These back-pointers are populated, but have no current uses.

        features.values().forEach((FeatureInfo feature) -> {
            feature.getAutoFeatures().forEach((String autoFeatureName) -> {
                FeatureInfo autoFeature = features.get(autoFeatureName);
                if (autoFeature != null) {
                    autoFeature.addActivatingAutoFeature(autoFeatureName);
                }
            });
        });

        return new FeatureRepo(featureFiles, features);
    }

    //

    public FeatureRepo(FeatureFiles featureFiles, Map<String, FeatureInfo> features) {
        this.featureFiles = featureFiles;
        this.features = features;

        this.visibilityPartitions = this.computeVisibilityPartitions();
        this.baseVisibilities = this.computeBaseVisibilities();
        this.cohorts = this.computeCohorts();
    }

    //

    private final FeatureFiles featureFiles;

    public FeatureFiles getFeatureFiles() {
        return featureFiles;
    }

    //

    private final Map<String, FeatureInfo> features;

    public Map<String, FeatureInfo> getFeatures() {
        return features;
    }

    public int getNumFeatures() {
        return getFeatures().size();
    }

    public FeatureInfo getFeature(String name) {
        return getFeatures().get(name);
    }

    public void forEach(BiConsumer<? super String, ? super FeatureInfo> action) {
        getFeatures().forEach(action);
    }

    public void forEach(Consumer<? super FeatureInfo> action) {
        getFeatures().values().forEach(action);
    }

    /**
     * Display the edition and product kind of a feature.
     *
     * Display "not found" text if the feature is not found.
     *
     * @param feature The name of a feature.
     *
     * @return A print string containing the feature name,
     *         the feature edition, and the feature product kind.
     */
    public String printFeature(String feature) {
        FeatureInfo featureInfo = getFeature(feature);
        if (featureInfo == null) {
            return "Feature: [ *** NOT FOUND: " + feature + " *** ]";
        } else {
            return featureInfo.printFeature();
        }
    }

    //

    // featureName -> visibility -> feature info

    private final Map<String, Map<String, List<FeatureInfo>>> visibilityPartitions;

    public Map<String, Map<String, List<FeatureInfo>>> getVisibilityPartitions() {
        return visibilityPartitions;
    }

    protected Map<String, Map<String, List<FeatureInfo>>> computeVisibilityPartitions() {
        Map<String, Map<String, List<FeatureInfo>>> usePartitions = new HashMap<>();

        forEach((FeatureInfo featureInfo) -> {
            if (featureInfo.isAutoFeature()) {
                return;
            }

            String feature = featureInfo.getName();

            // javaeePlatform and appSecurity are special because they have dependencies on each other.
            if (feature.startsWith("io.openliberty.jakartaeePlatform") ||
                feature.startsWith("com.ibm.websphere.appserver.javaeePlatform") ||
                feature.startsWith("com.ibm.websphere.appserver.appSecurity")) {
                return;
            }

            String baseName = featureInfo.getBaseName();
            String visibility = featureInfo.getVisibility();

            Map<String, List<FeatureInfo>> partition = usePartitions.computeIfAbsent(baseName, (String useName) -> new HashMap<>());
            List<FeatureInfo> partitionElement = partition.computeIfAbsent(visibility, (String useVisibility) -> new ArrayList<>());

            partitionElement.add(featureInfo);
        });

        return usePartitions;
    }

    private final Map<String, String> baseVisibilities;

    public Map<String, String> getBaseVisibilities() {
        return baseVisibilities;
    }

    /**
     * Build and return a table mapping base feature names to visibilities.
     *
     * Ignore auto-features and ignore features which do not have version.
     *
     * @return The visibility table.
     */
    protected Map<String, String> computeBaseVisibilities() {
        Map<String, String> useBaseVisibilities = new HashMap<>(getNumFeatures());

        forEach((FeatureInfo featureInfo) -> {
            if (featureInfo.isAutoFeature()) {
                return;
            }
            if (featureInfo.getVersion() == null) {
                return;
            }
            useBaseVisibilities.put(featureInfo.getBaseName(), featureInfo.getVisibility());
        });

        return useBaseVisibilities;
    }

    //

    public static <K, V> Map<K, V> merge(Map<K, V> m1, Map<K, V> m2) {
        if ((m1 == null) || m1.isEmpty()) {
            if ((m2 == null) || m2.isEmpty()) {
                return Collections.emptyMap();
            } else {
                return m2;
            }

        } else if ((m2 == null) || m2.isEmpty()) {
            return m1;

        } else {
            Map<K, V> merged = new HashMap<K, V>(m1.size() + m2.size());
            merged.putAll(m1);
            merged.putAll(m2);
            return merged;
        }
    }

    public static <E> Set<E> merge(Set<E> s1, Set<E> s2) {
        if ((s1 == null) || s1.isEmpty()) {
            if ((s2 == null) || s2.isEmpty()) {
                return Collections.emptySet();
            } else {
                return s2;
            }

        } else if ((s2 == null) || s2.isEmpty()) {
            return s1;

        } else {
            Set<E> merged = new HashSet<E>(s1.size() + s2.size());
            merged.addAll(s1);
            merged.addAll(s2);
            return merged;
        }
    }

    //

    private final Map<String, List<String>> cohorts;

    public Map<String, List<String>> getCohorts() {
        return cohorts;
    }

    public Map<String, List<String>> computeCohorts() {
        Map<String, List<String>> useCohorts = new LinkedHashMap<>();

        forEach((FeatureInfo featureInfo) -> {
            if (!featureInfo.isPublic()) {
                return;
            }

            String version = featureInfo.getVersion();
            if (version == null) {
                return;
            }

            String baseName = featureInfo.getBaseName();
            List<String> cohort = useCohorts.computeIfAbsent(baseName,
                                                             (String useBaseName) -> new ArrayList<>());
            cohort.add(version);
        });

        useCohorts.forEach((String baseName, List<String> useVersions) -> {
            useVersions.sort(FeatureRepo::compareVersions);
        });

        return useCohorts;
    }

    private static Map<String, int[]> versions = new HashMap<>();

    public static int[] parse(String version) {
        return versions.computeIfAbsent(version, (String useVersion) -> {
            int vOffset = useVersion.indexOf('.');
            if (vOffset == -1) {
                return new int[] { Integer.parseInt(useVersion) };
            } else {
                return new int[] { Integer.parseInt(useVersion.substring(0, vOffset)),
                                   Integer.parseInt(useVersion.substring(vOffset + 1, useVersion.length())) };
            }
        });
    }

    public static int compareVersions(String v0, String v1) {
        int[] fields0 = parse(v0);
        int len0 = fields0.length;

        int[] fields1 = parse(v1);
        int len1 = fields1.length;

        int fNo = 0;
        while ((fNo < len0) && (fNo < len1)) {
            int f0 = fields0[fNo];
            int f1 = fields1[fNo];
            if (f0 < f1) {
                return -1; // 8.x < 9.x
            } else if (f0 > f1) {
                return +1; // 9.x > 8.x
            } else {
                fNo++;
                // loop    // 8.x ? 8.y
            }
        }

        if (fNo == len0) {
            if (fNo == len1) {
                return 0; // 10.x == 10.x
            } else {
                return -1; // 10 < 10.x
            }
        } else if (fNo == len1) {
            return +1; // 8.x > 8
        } else {
            throw new IllegalStateException("Strange comparison of [ " + v0 + " ] with [ " + v0 + " ]");
        }
    }
}
