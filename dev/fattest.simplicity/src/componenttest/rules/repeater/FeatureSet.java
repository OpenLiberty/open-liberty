/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.rules.repeater;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.rules.repeater.RepeatActions.SEVersion;

/**
 * An immutable set of features with an ID
 */
public class FeatureSet {
    private final String id;
    private final Set<String> features;
    private final EEVersion eeVersion;
    private final SEVersion minJavaLevel;

    /**
     * Create a new FeatureSet with the given ID and set of features
     *
     * @param id        The ID of the FeatureSet. Must be unique.
     * @param features  The features to include in the set
     * @param eeVersion The EE Version that the features are based on. May be null.
     */
    public FeatureSet(String id, Set<String> features, EEVersion eeVersion) {
        this(id, features, eeVersion, eeVersion.getMinJavaLevel());
    }

    /**
     * Create a new FeatureSet with the given ID and set of features
     *
     * @param id           The ID of the FeatureSet. Must be unique.
     * @param features     The features to include in the set
     * @param eeVersion    The EE Version that the features are based on. May be null.
     * @param minJavaLevel The minimum Java SE Version which this FeatureSet can run on.
     */
    public FeatureSet(String id, Set<String> features, EEVersion eeVersion, SEVersion minJavaLevel) {
        if (id == null)
            throw new NullPointerException();
        this.id = id;
        this.features = Collections.unmodifiableSet(new HashSet<>(features));
        this.eeVersion = eeVersion;
        this.minJavaLevel = minJavaLevel;
    }

    /**
     * Get the ID of this FeatureSet
     *
     * @return the id
     */
    public final String getID() {
        return this.id;
    }

    /**
     * Get an unmodifiable set of the features
     *
     * @return the features
     */
    public final Set<String> getFeatures() {
        return this.features;
    }

    /**
     * Get the EE Version upon which this set of features is based
     *
     * @return the EE version or null if not EE based
     */
    public final EEVersion getEEVersion() {
        return this.eeVersion;
    }

    /**
     * Get the minimum Java SE Version which this FeatureSet can run on.
     *
     * @return the minimum Java SE Version
     */
    public SEVersion getMinJavaLevel() {
        return this.minJavaLevel;
    }

    /**
     * Create a new FeatureSetBuilder based on this FeatureSet
     *
     * @param  feature the feature to add to the set
     * @return         a FeatureSetBuilder
     */
    public FeatureSetBuilder addFeature(String feature) {
        FeatureSetBuilder builder = new FeatureSetBuilder(this);
        builder.addFeature(feature);
        return builder;
    }

    /**
     * Create a new FeatureSetBuilder based on this FeatureSet
     *
     * @param  feature the feature to remove from the set
     * @return         a FeatureSetBuilder
     */
    public FeatureSetBuilder removeFeature(String feature) {
        FeatureSetBuilder builder = new FeatureSetBuilder(this);
        builder.removeFeature(feature);
        return builder;
    }

    /**
     * Create a new FeatureSetBuilder based on this FeatureSet but with a different min java level.
     *
     * @param  minJavaLevel the minimum Java SE Version
     * @return              a FeatureSetBuilder
     */
    public FeatureSetBuilder setMinJavaLevel(SEVersion javaLevel) {
        FeatureSetBuilder builder = new FeatureSetBuilder(this);
        builder.setMinJavaLevel(javaLevel);
        return builder;
    }

    /*
     * Since the id should be unique, we just use the hashCode of the id as the hashCode for this FeatureSet
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /*
     * Since the id should be unique, equality is based on the ids being equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        FeatureSet other = (FeatureSet) obj;

        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;

        return true;
    }

    /**
     * A FeatureSet builder. Since a FeatureSet is immutable, this builder must be used to modify the set of features.
     */
    public static class FeatureSetBuilder {

        private final HashSet<String> features;
        private final EEVersion eeVersion;
        private SEVersion minJavaLevel;

        /**
         * Create a new builder, starting with the same set of features from an existing FeatureSet
         *
         * @param featureSet a FeatureSet to copy the features from
         */
        public FeatureSetBuilder(FeatureSet featureSet) {
            this(featureSet.getFeatures(), featureSet.getEEVersion(), featureSet.getMinJavaLevel());
        }

        /**
         * Create a new builder, starting with the a given set of features
         *
         * @param features     a set of features to initially add
         * @param eeVersion    the EE version these features are based on
         * @param minJavaLevel the minimum Java SE Version which this FeatureSet can run on.
         */
        public FeatureSetBuilder(Set<String> features, EEVersion eeVersion, SEVersion minJavaLevel) {
            this.features = new HashSet<>(features);
            this.eeVersion = eeVersion;
            this.minJavaLevel = minJavaLevel;
        }

        /**
         * Add a feature to the set
         *
         * @param feature the feature to add
         */
        public FeatureSetBuilder addFeature(String feature) {
            this.features.add(feature);
            return this;
        }

        /**
         * Remove a feature from the set
         *
         * @param feature the feature to remove
         */
        public FeatureSetBuilder removeFeature(String feature) {
            this.features.remove(feature);
            return this;
        }

        /**
         * Set the minimum Java SE Version which this FeatureSet can run on.
         *
         * @param minJavaLevel the minimum Java SE Version
         */
        public FeatureSetBuilder setMinJavaLevel(SEVersion minJavaLevel) {
            this.minJavaLevel = minJavaLevel;
            return this;
        }

        /**
         * Create a new FeatureSet with the given ID and the features currently in the builder
         *
         * @param  id the ID of the new FeatureSet. Must be unique.
         * @return    the new FeatureSet
         */
        public FeatureSet build(String id) {
            return new FeatureSet(id, this.features, this.eeVersion, this.minJavaLevel);
        }
    }

}
