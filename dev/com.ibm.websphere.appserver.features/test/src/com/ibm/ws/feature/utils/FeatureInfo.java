/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import aQute.bnd.header.Attrs;

public class FeatureInfo {

    /**
     * Retrieve a property from a feature build. Trim the property value.
     *
     * @param builder A feature builder.
     * @param propertyName The name of the property which is to be retrieved.
     * @param defaultValue The property value, if the property was not found.
     *
     * @return The trimmed property value. The default value if the property was not found.
     */
    public static String getProperty(FeatureBuilder builder, String propertyName, String defaultValue) {
        String propertyValue = builder.getProperty(propertyName);
        return ((propertyValue == null) ? defaultValue : propertyValue.trim());
    }

    public static String getProperty(FeatureBuilder builder, String propertyName) {
        return getProperty(builder, propertyName, null);
    }

    public static boolean hasProperty(FeatureBuilder builder, String propertyName) {
        return (getProperty(builder, propertyName) != null);
    }

    public static boolean hasProperty(FeatureBuilder builder, String propertyName, String value) {
        String propertyValue = getProperty(builder, propertyName);
        return ((propertyValue != null) && propertyValue.equals(value));
    }

    public static boolean getProperty(FeatureBuilder builder, String propertyName, boolean defaultValue) {
        String propertyValue = getProperty(builder, propertyName, null);
        if (propertyValue == null) {
            return defaultValue;
        } else {
            return FeatureConstants.BND_TRUE.equals(propertyValue);
        }
    }

    //

    public static String getBaseName(String featureName) {
        int versionIndex = featureName.lastIndexOf('-');
        if (versionIndex != -1) {
            return featureName.substring(0, versionIndex);
        } else {
            return featureName;
        }
    }

    public static String getVersion(String featureName) {
        int versionIndex = featureName.lastIndexOf('-');
        if (versionIndex != -1) {
            return featureName.substring(versionIndex + 1, featureName.length());
        } else {
            return null;
        }
    }

    //

    //

    /**
     * Answer an integer level for an edition value.
     *
     * The order of these is significant: A dependent feature
     * cannot have an edition value which is less than its dependency
     * parent feature.
     *
     * @param edition An edition for which to obtain an integer value.
     * @return The integer value assigned to the edition.
     */
    public static int getEditionLevel(String edition) {
        if (edition == null) {
            return 99;
        }

        switch (edition.toLowerCase()) {

            case FeatureConstants.EDITION_FULL:
                return 0;
            case FeatureConstants.EDITION_UNSUPPORTED:
                return 1;
            case FeatureConstants.EDITION_ZOS:
                return 2;
            case FeatureConstants.EDITION_ND:
                return 3;
            case FeatureConstants.EDITION_BASE:
                return 4;
            case FeatureConstants.EDITION_CORE:
                return 5;
            default:
                // TODO: Is this correct?
                // This means that unknown edition values never generate
                // kind conflicts.
                return 99;
        }
    }

    /**
     * Answer an integer level for an product kind value.
     *
     * The order of these is significant: A dependent feature
     * cannot have an product kind value which is less than the
     * product kind value of its dependency parent feature.
     *
     * @param kind A kind for which to obtain an integer value.
     * @return The integer value assigned to the kind.
     */
    public static int getKindLevel(String kind) {
        if (kind == null) {
            return 99;
        }

        switch (kind.toLowerCase()) {
            case FeatureConstants.KIND_NOSHIP:
                return 0;
            case FeatureConstants.KIND_BETA:
                return 1;
            case FeatureConstants.KIND_GA:
                return 2;
            default:
                // TODO: Is this correct?
                // This means that unknown kind values never generate
                // kind conflicts.
                return 99;
        }
    }

    //

    /**
     * Read feature information from a feature file.
     *
     * @param featureFile A file containing feature information.
     *
     * @throws IOException Thrown if the feature information cannot be read.
     */
    public FeatureInfo(File featureFile) throws IOException {
        this.featureFile = featureFile;

        try (FeatureBuilder builder = new FeatureBuilder()) {
            builder.setProperties(this.featureFile);

            this.name = getProperty(builder, FeatureConstants.BND_SYMBOLIC_NAME);

            int versionIndex = this.name.lastIndexOf('-');
            if (versionIndex != -1) {
                this.baseName = this.name.substring(0, versionIndex);
                this.version = this.name.substring(versionIndex + 1, this.name.length());
            } else {
                this.baseName = this.name;
                this.version = null;
            }

            this.shortName = getProperty(builder, FeatureConstants.IBM_SHORT_NAME);

            this.edition = getProperty(builder, FeatureConstants.BND_PRODUCT_EDITION);
            this.kind = getProperty(builder, FeatureConstants.BND_PRODUCT_KIND);
            this.isSingleton = getProperty(builder, FeatureConstants.BND_SINGLETON,
                                           false);
            this.visibility = getProperty(builder, FeatureConstants.BND_VISIBILITY,
                                          FeatureConstants.VISIBILITY_PRIVATE);

            //

            this.isAutoFeature = hasProperty(builder, FeatureConstants.IBM_PROVISION_CAPABILITY);

            Set<String> useAutoFeatures = builder.getAutoFeatures();
            this.autoFeatures = new LinkedHashSet<String>(useAutoFeatures);

            // Inverse auto-features pointer.  Set after all features are created,
            // after the feature initialization.
            this.activatingAutoFeature = new LinkedHashSet<String>();

            Set<Map.Entry<String, Attrs>> useFeatures = builder.getFeatures();

            List<String> useDepNames = new ArrayList<>(useFeatures.size());
            Map<String, Attrs> useDeps = new LinkedHashMap<>(useFeatures.size());

            useFeatures.forEach((Map.Entry<String, Attrs> entry) -> {
                String depName = entry.getKey();
                useDepNames.add(depName);
                useDeps.put(depName, entry.getValue());

            });

            useDepNames.sort(Comparator.comparing(String::toString));

            this.sortedDependentNames = useDepNames;
            this.dependentFeatures = useDeps;

            //

            this.isParallelActivationEnabled = hasProperty(builder, FeatureConstants.WLP_ACTIVATION_TYPE,
                                                           FeatureConstants.WLP_ACTIVATION_TYPE_PARALLEL);

            this.isSetDisableOnConflict = hasProperty(builder, FeatureConstants.WLP_DISABLE_ALL_FEATURES_ON_CONFLICT);
            this.isDisableOnConflictEnabled = getProperty(builder, FeatureConstants.WLP_DISABLE_ALL_FEATURES_ON_CONFLICT,
                                                          true);

            this.isSetAlsoKnownAs = hasProperty(builder, FeatureConstants.WLP_ALSO_KNOWN_AS);
            this.alsoKnownAs = getProperty(builder, FeatureConstants.WLP_ALSO_KNOWN_AS);
        }
    }

    //

    /**
     * Display the edition and product kind of a feature.
     *
     * @return A print string containing the feature name,
     *         the feature edition, and the feature product kind.
     */
    public String printFeature() {
        StringBuilder builder = new StringBuilder();
        builder.append("Feature: [");
        builder.append(getName());
        builder.append("]");
        builder.append(" Edition: [");
        builder.append(getEdition());
        builder.append("]");
        builder.append(" Kind: [");
        builder.append(getKind());
        builder.append("]");

        builder.append(" File [ ");
        builder.append(getFeatureFile().getName());
        builder.append(" ]");

        return builder.toString();
    }

    //

    private final File featureFile;

    public File getFeatureFile() {
        return featureFile;
    }

    public String getFeatureFileName() {
        String fileName = getFeatureFile().getName();
        int index = fileName.lastIndexOf(FeatureFileConstants.FEATURE_FILE_EXT);
        return ((index == -1) ? fileName : fileName.substring(0, index));
    }

    //

    private final String name;
    private final String baseName;
    private final String version;

    private final String shortName;
    private final String edition;
    private final String kind;

    public String getName() {
        return name;
    }

    public String getBaseName() {
        return baseName;
    }

    public String getVersion() {
        return version;
    }

    public boolean isVersionless() {
        return (getVersion() == null);
    }

    public String getShortName() {
        return shortName;
    }

    public String getEdition() {
        return edition;
    }

    public int getEditionLevel() {
        return getEditionLevel(getEdition());
    }

    public boolean isBase() {
        return FeatureConstants.EDITION_BASE.equals(getEdition());
    }

    public boolean isFull() {
        return FeatureConstants.EDITION_FULL.equals(getEdition());
    }

    public boolean isND() {
        return FeatureConstants.EDITION_ND.equals(getEdition());
    }

    public boolean isCore() {
        return FeatureConstants.EDITION_CORE.equals(getEdition());
    }

    public boolean isZOS() {
        return FeatureConstants.EDITION_ZOS.equals(getEdition());
    }

    public boolean isUnsupported() {
        return FeatureConstants.EDITION_UNSUPPORTED.equals(getEdition());
    }

    public String getKind() {
        return kind;
    }

    public int getKindLevel() {
        return getKindLevel(getKind());
    }

    public boolean isNoShip() {
        return FeatureConstants.KIND_NOSHIP.equals(getKind());
    }

    public boolean isBeta() {
        return FeatureConstants.KIND_BETA.equals(getKind());
    }

    public boolean isGA() {
        return FeatureConstants.KIND_GA.equals(getKind());
    }

    //

    private final boolean isSingleton;
    private final String visibility;

    public boolean isSingleton() {
        return isSingleton;
    }

    public String getVisibility() {
        return visibility;
    }

    public boolean isPrivate() {
        return FeatureConstants.VISIBILITY_PRIVATE.equals(getVisibility());
    }

    public boolean isProtected() {
        return FeatureConstants.VISIBILITY_PROTECTED.equals(getVisibility());
    }

    public boolean isPublic() {
        return FeatureConstants.VISIBILITY_PUBLIC.equals(getVisibility());
    }

    //

    private final boolean isAutoFeature;

    public boolean isAutoFeature() {
        return isAutoFeature;
    }

    private final Set<String> autoFeatures;

    public Set<String> getAutoFeatures() {
        return autoFeatures;
    }

    private final Set<String> activatingAutoFeature;

    //Activating autofeature just means "I'm an autofeature, and i *might* activate this other feature
    //So it's like a "Sometimes" dependency, but is potentially useful for figuring out a superset of
    //potential provisioned features.

    protected void addActivatingAutoFeature(String featureName) {
        activatingAutoFeature.add(featureName);
    }

    public Set<String> getActivatingAutoFeatures() {
        return activatingAutoFeature;
    }

    //

    private final List<String> sortedDependentNames;
    private final Map<String, Attrs> dependentFeatures;

    public List<String> getSortedDependentNames() {
        return sortedDependentNames;
    }

    public void forEachSortedDepName(Consumer<? super String> consumer) {
        getSortedDependentNames().forEach(consumer);
    }

    //

    public Map<String, Attrs> getDependentFeatures() {
        return dependentFeatures;
    }

    public void forEachDep(FeatureRepo repo,
                           Consumer<FeatureInfo> consumer) {
        forEachDepName((String dep) -> consumer.accept(repo.getFeature(dep)));
    }

    public void forEachResolvedDep(FeatureRepo repo,
                                   Consumer<FeatureInfo> consumer) {

        forEachDepName((String dep) -> {
            FeatureInfo featureInfo = repo.getFeature(dep);
            if (featureInfo != null) {
                consumer.accept(featureInfo);
            }
        });
    }

    public void forEachDep(BiConsumer<? super String, ? super Attrs> consumer) {
        getDependentFeatures().forEach(consumer);
    }

    public void forEachDepName(Consumer<? super String> consumer) {
        getDependentFeatures().keySet().forEach(consumer);
    }

    //

    private final boolean isParallelActivationEnabled;

    private final boolean isSetDisableOnConflict;
    private final boolean isDisableOnConflictEnabled;

    private final boolean isSetAlsoKnownAs;
    private final String alsoKnownAs;

    public boolean isParallelActivationEnabled() {
        return isParallelActivationEnabled;
    }

    public boolean isDisableOnConflictEnabled() {
        return isDisableOnConflictEnabled;
    }

    public boolean isSetDisableOnConflict() {
        return isSetDisableOnConflict;
    }

    public boolean isSetAlsoKnownAs() {
        return isSetAlsoKnownAs;
    }

    public String getAlsoKnownAs() {
        return alsoKnownAs;
    }
}
