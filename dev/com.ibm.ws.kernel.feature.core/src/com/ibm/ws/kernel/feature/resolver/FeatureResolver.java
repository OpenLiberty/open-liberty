/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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
package com.ibm.ws.kernel.feature.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Version;

import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 * A resolver for liberty features. The resolver has two modes:
 *
 * First, the resolver accepts kernel and requested features and resolves
 * the constituents of these, selecting feature versions which mutually
 * satisfy the features. All <strong>auto</strong> features which are
 * satisfied as also selected. This first resolver mode is used when
 * starting the server, and is used when running minify on the server.
 *
 * Secondly, the resolver selects all constituents of requested features.
 * This second mode is used during installation.
 */
public interface FeatureResolver {
    /** Java7 predicate interface. */
    public static interface Selector<T> {
        boolean test(T input);
    }

    //

    /**
     * A collection of features which are available for feature resolution.
     */
    public static interface Repository {
        /**
         * Answer all feature within this repository.
         *
         * @return All features of this repository.
         */
        List<ProvisioningFeatureDefinition> getFeatures();

        /**
         * Answer all auto-features of the repository.
         *
         * See {@link ProvisioningFeatureDefinition#isAutoFeature()}.
         *
         * @return The auto-features of this repository.
         */
        Collection<ProvisioningFeatureDefinition> getAutoFeatures();

        /**
         * Answer the feature which matches a specified feature name.
         *
         * The feature name may be the short name of a public feature,
         * or a feature symbolic name.
         *
         * @param featureName A feature short name or symbolic name.
         *
         * @return The named feature. Null if no matching feature is found.
         */
        ProvisioningFeatureDefinition getFeature(String featureName);

        /**
         * Returns a list of versions that should always be tolerated for the
         * specified base symbolic name. For example, a feature named
         * com.ibm.ws.something_1.0 has a base symbolic name of
         * &quot;com.ibm.ws.something&quot;. This method could return
         * a list [1.0, 1.1, 1.2] to indicate that all features that include a
         * version of com.ibm.ws.something also can tolerate versions
         * 1.0, 1.1 and 1.2. This is a way to override the tolerates.
         *
         * @param baseSymbolicName a feature base symbolic name to allow more tolerations for.
         * @return The additional versions of the feature that can be tolerated.
         */
        List<String> getConfiguredTolerates(String baseSymbolicName);
    }

    /**
     * A feature resolution result.
     *
     * The result includes both resolved features, and includes feature resolution
     * errors.
     */
    public static interface Result {
        Set<String> getResolvedFeatures();

        //

        boolean hasErrors();

        Set<String> getMissing();

        Set<String> getNonPublicRoots();

        Map<String, Chain> getWrongProcessTypes();

        Map<String, Collection<Chain>> getConflicts();

        Set<String> getUnresolvedVersionless();
    }

    public static final List<String> EMPTY_STRINGS = Collections.emptyList();

    public static class Chain {
        private static Version parseVersion(String preferredVersion) {
            try {
                return Version.parseVersion(preferredVersion);
            } catch (IllegalArgumentException e) {
                return Version.emptyVersion;
            }
        }

        private static List<String> copyChain(Collection<String> chain) {
            return (chain.isEmpty() ? EMPTY_STRINGS : new ArrayList<String>(chain));
        }

        public Chain(String featureName, String preferredVersion) {
            this(EMPTY_STRINGS, Collections.singletonList(featureName), preferredVersion, featureName);
        }

        public Chain(Chain otherChain) {
            this._chain = copyChain(otherChain._chain);

            this._featureName = otherChain._featureName;
            this._preferredVersion = otherChain._preferredVersion;

            this._candidates = new ArrayList<String>(otherChain.getCandidates());

            this.toString = getString();
        }

        public Chain(Collection<String> chain, List<String> candidates, String preferredVersion, String featureName) {
            this._chain = copyChain(chain);

            this._featureName = featureName;
            this._preferredVersion = parseVersion(preferredVersion);

            this._candidates = candidates;

            this.toString = getString();
        }

        public Chain(String candidate, String preferredVersion, String featureName) {
            this._chain = EMPTY_STRINGS;

            this._featureName = featureName;
            this._preferredVersion = parseVersion(preferredVersion);

            this._candidates = Collections.singletonList(candidate);

            this.toString = getString();
        }

        //

        private final List<String> _chain;

        private final String _featureName;
        private final Version _preferredVersion;
        private final List<String> _candidates;

        public List<String> getChain() {
            return _chain;
        }

        public List<String> getCandidates() {
            return _candidates;
        }

        public Version getPreferredVersion() {
            return _preferredVersion;
        }

        public String getFeatureRequirement() {
            return _featureName;
        }

        private String getString() {
            StringBuilder builder = new StringBuilder();

            builder.append("ROOT->");

            for (String featureName : _chain) {
                builder.append(featureName);
                builder.append("->");
            }
            builder.append(_candidates);
            builder.append(" ");
            builder.append(_preferredVersion);

            return builder.toString();
        }

        private final String toString;

        @Override
        public String toString() {
            return toString;
        }
    }

    //

    public void setPreferredPlatforms(String preferredPlatformVersions);

    /**
     * Resolve with an empty collection of kernel features.
     */
    Result resolveFeatures(Repository repository,
                           Collection<String> rootFeatures,
                           Set<String> preResolved,
                           boolean allowMultipleVersions);

    /**
     * Resolve allowing all process types.
     */
    Result resolveFeatures(Repository repository,
                           Collection<ProvisioningFeatureDefinition> kernelFeatures,
                           Collection<String> rootFeatures,
                           Set<String> preResolved,
                           boolean allowMultipleVersions);

    /**
     * Main resolution API: Resolve features using the supplied parameters.
     */
    Result resolveFeatures(Repository repository,
                           Collection<ProvisioningFeatureDefinition> kernelFeatures,
                           Collection<String> rootFeatures,
                           Set<String> preResolved,
                           boolean allowMultipleVersions,
                           EnumSet<ProcessType> supportedProcessTypes);

    /**
     * Main resolution API: Resolve features using the supplied parameters.
     *
     * Specify features which multiple-versions are allowed using a set.
     */
    Result resolveFeatures(Repository repository,
                           Collection<ProvisioningFeatureDefinition> kernelFeatures,
                           Collection<String> rootFeatures,
                           Set<String> preResolved,
                           Set<String> allowedMultipleVersions,
                           EnumSet<ProcessType> supportedProcessTypes);

    // Updated, platform aware, feature resolution API.

    Result resolve(Repository repository,
                   Collection<String> rootFeatures,
                   Set<String> preResolved,
                   boolean allowMultipleVersions,
                   Collection<String> rootPlatforms);

    Result resolve(Repository repository,
                   Collection<ProvisioningFeatureDefinition> kernelFeatures,
                   Collection<String> rootFeatures,
                   Set<String> preResolved,
                   boolean allowMultipleVersions,
                   Collection<String> platforms);

    Result resolve(Repository repository,
                   Collection<ProvisioningFeatureDefinition> kernelFeatures,
                   Collection<String> rootFeatures,
                   Set<String> preResolved,
                   boolean allowMultipleVersions,
                   EnumSet<ProcessType> supportedProcessTypes,
                   Collection<String> platforms);

    /**
     * Fully parameterized feature resolution: Resolves a root features and root
     * platforms against a repository.
     *
     * @param repository              A feature repository.
     * @param kernelFeatures          Preset kernel features.
     * @param rootFeatures            Public features which are to be resolved.
     * @param preResolved             Optional fully resolved features collection.
     * @param allowedMultipleVersions Control parameter: Should resolution respect singleton
     *                                    specifications on features.
     * @param supportedProcessTypes   Control parameter: Limit resolution to features which support
     *                                    these process types.
     * @param rootPlatforms           Optional platforms which enable resolution of
     *                                    versionless features.
     *
     * @return The feature resolution result.
     */
    public Result resolve(Repository repository,
                          Collection<ProvisioningFeatureDefinition> kernelFeatures,
                          Collection<String> rootFeatures,
                          Set<String> preResolved,
                          Set<String> allowedMultipleVersions,
                          EnumSet<ProcessType> supportedProcessTypes,
                          Collection<String> rootPlatforms);
}
